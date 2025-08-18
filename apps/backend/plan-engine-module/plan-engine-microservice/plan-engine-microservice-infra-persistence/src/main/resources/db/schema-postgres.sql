-- ============================================
-- Schema: plan_engine
-- ============================================
CREATE SCHEMA IF NOT EXISTS plan_engine;

-- Extensiones necesarias (UUID via gen_random_uuid)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================
-- Funciones utilitarias
-- ============================================

-- Timestamp automático en updated_at
CREATE OR REPLACE FUNCTION plan_engine.tg_set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END
$$;

-- Valida que los sobres (envelopes) sumen ~1 con tolerancia
CREATE OR REPLACE FUNCTION plan_engine.envelopes_sum_within(_envelopes JSONB, _tol NUMERIC DEFAULT 0.02)
RETURNS BOOLEAN
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
  s NUMERIC := 0;
  v NUMERIC;
  kv RECORD;
BEGIN
  IF _envelopes IS NULL THEN
    RETURN TRUE;
  END IF;

  FOR kv IN SELECT key, value FROM jsonb_each(_envelopes)
  LOOP
    BEGIN
      v := (kv.value)::text::numeric;
    EXCEPTION WHEN others THEN
      RETURN FALSE;
    END;
    IF v < 0 OR v > 1 THEN
      RETURN FALSE;
    END IF;
    s := s + v;
  END LOOP;

  RETURN (s BETWEEN (1 - _tol) AND (1 + _tol));
END
$$;

-- Trigger: valida params/kpis de plan_version en INSERT/UPDATE
CREATE OR REPLACE FUNCTION plan_engine.plan_version_validate()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
  sp NUMERIC;
  em INT;
  env JSONB;
BEGIN
  IF NEW.params IS NULL THEN
    RAISE EXCEPTION 'params cannot be null';
  END IF;

  -- saving_pct 0..1
  IF NOT NEW.params ? 'saving_pct' THEN
    RAISE EXCEPTION 'params.saving_pct is required';
  END IF;
  sp := (NEW.params->>'saving_pct')::numeric;
  IF sp < 0 OR sp > 1 THEN
    RAISE EXCEPTION 'params.saving_pct must be between 0 and 1';
  END IF;

  -- emergency_months 0..24
  IF NOT NEW.params ? 'emergency_months' THEN
    RAISE EXCEPTION 'params.emergency_months is required';
  END IF;
  em := (NEW.params->>'emergency_months')::int;
  IF em < 0 OR em > 24 THEN
    RAISE EXCEPTION 'params.emergency_months must be between 0 and 24';
  END IF;

  -- envelopes suma ≈ 1 si existe
  env := NEW.params->'envelopes';
  IF env IS NOT NULL AND NOT plan_engine.envelopes_sum_within(env, 0.02) THEN
    RAISE EXCEPTION 'params.envelopes must sum ~1 (±0.02)';
  END IF;

  -- kpis obligatorio como objeto (aunque pueda venir vacío)
  IF NEW.kpis IS NULL OR jsonb_typeof(NEW.kpis) <> 'object' THEN
    RAISE EXCEPTION 'kpis must be a JSON object';
  END IF;

  -- source permitido
  IF NEW.source NOT IN ('rules','rules+gpt') THEN
    RAISE EXCEPTION 'source must be one of: rules, rules+gpt';
  END IF;

  RETURN NEW;
END
$$;

-- ============================================
-- Tablas principales
-- ============================================

-- 1) Catálogo de plantillas
CREATE TABLE IF NOT EXISTS plan_engine.plan_template (
  id               TEXT PRIMARY KEY CHECK (id ~ '^[A-Z0-9_]+$'),
  name             TEXT NOT NULL CHECK (char_length(name) BETWEEN 3 AND 120),
  version          INTEGER NOT NULL DEFAULT 1 CHECK (version >= 1),
  params_schema    JSONB NOT NULL,          -- JSON Schema de parámetros/ajustes
  scoring_weights  JSONB NOT NULL,          -- pesos para el score
  constraints_doc  JSONB NOT NULL,          -- límites y reglas de la plantilla
  protected        BOOLEAN NOT NULL DEFAULT FALSE,
  checksum         TEXT NOT NULL,           -- hash/cambio
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_plan_template_name
  ON plan_engine.plan_template (name);

CREATE INDEX IF NOT EXISTS idx_plan_template_params_gin
  ON plan_engine.plan_template USING GIN (params_schema jsonb_path_ops);

CREATE TRIGGER trg_plan_template_updated_at
BEFORE UPDATE ON plan_engine.plan_template
FOR EACH ROW EXECUTE FUNCTION plan_engine.tg_set_updated_at();

-- 2) Plan lógico por usuario
CREATE TABLE IF NOT EXISTS plan_engine.plan (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id          UUID NOT NULL,
  active_version   INTEGER NOT NULL DEFAULT 0 CHECK (active_version >= 0),
  status           VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_plan_user UNIQUE (user_id),
  CONSTRAINT ck_plan_status CHECK (status IN ('DRAFT','ACTIVE','PAUSED','ARCHIVED'))
);

CREATE INDEX IF NOT EXISTS idx_plan_user
  ON plan_engine.plan (user_id);

-- para filtros por usuario + estado
CREATE INDEX IF NOT EXISTS idx_plan_user_status
  ON plan_engine.plan (user_id, status);

CREATE TRIGGER trg_plan_updated_at
BEFORE UPDATE ON plan_engine.plan
FOR EACH ROW EXECUTE FUNCTION plan_engine.tg_set_updated_at();

-- 3) Versiones del plan (histórico)
CREATE TABLE IF NOT EXISTS plan_engine.plan_version (
  plan_id          UUID NOT NULL REFERENCES plan_engine.plan(id) ON DELETE CASCADE,
  version          INTEGER NOT NULL CHECK (version >= 1),
  template_id      TEXT NOT NULL REFERENCES plan_engine.plan_template(id),
  params           JSONB NOT NULL,          -- ajustes (saving_pct, emergency_months, envelopes...)
  kpis             JSONB NOT NULL,          -- tasa_ahorro, runway, cumplimiento, etc.
  rationale        TEXT,
  alerts           JSONB NOT NULL DEFAULT '[]'::jsonb,
  source           TEXT NOT NULL,           -- 'rules' | 'rules+gpt' (validado en trigger)
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (plan_id, version)
);

CREATE INDEX IF NOT EXISTS idx_plan_version_plan
  ON plan_engine.plan_version (plan_id, version DESC);

CREATE INDEX IF NOT EXISTS idx_plan_version_template
  ON plan_engine.plan_version (template_id);

CREATE INDEX IF NOT EXISTS idx_plan_version_params_gin
  ON plan_engine.plan_version USING GIN (params jsonb_path_ops);

CREATE INDEX IF NOT EXISTS idx_plan_version_kpis_gin
  ON plan_engine.plan_version USING GIN (kpis jsonb_path_ops);

CREATE TRIGGER trg_plan_version_validate_bi
BEFORE INSERT ON plan_engine.plan_version
FOR EACH ROW EXECUTE FUNCTION plan_engine.plan_version_validate();

CREATE TRIGGER trg_plan_version_validate_bu
BEFORE UPDATE ON plan_engine.plan_version
FOR EACH ROW EXECUTE FUNCTION plan_engine.plan_version_validate();

-- 4) Eventos/Triggers de recompute (auditoría)
CREATE TABLE IF NOT EXISTS plan_engine.recompute_event (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  plan_id          UUID NOT NULL REFERENCES plan_engine.plan(id) ON DELETE CASCADE,
  reason           TEXT NOT NULL,           -- 'balance_update' | 'overspend' | 'manual' | 'sync'
  payload          JSONB NOT NULL,          -- dif de saldos, categoría fuera de límite, etc.
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  processed_at     TIMESTAMPTZ,
  CONSTRAINT ck_recompute_reason CHECK (reason IN ('balance_update','overspend','manual','sync'))
);

CREATE INDEX IF NOT EXISTS idx_recompute_plan
  ON plan_engine.recompute_event (plan_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_recompute_pending
  ON plan_engine.recompute_event (plan_id)
  WHERE processed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_recompute_payload_gin
  ON plan_engine.recompute_event USING GIN (payload jsonb_path_ops);

-- ============================================
-- Vistas útiles
-- ============================================

-- Última versión activa del plan (según active_version)
CREATE OR REPLACE VIEW plan_engine.v_plan_active AS
SELECT
  p.id               AS plan_id,
  p.user_id,
  p.active_version,
  p.status,
  pv.template_id,
  pv.params,
  pv.kpis,
  pv.rationale,
  pv.alerts,
  pv.source,
  pv.created_at      AS version_created_at
FROM plan_engine.plan p
JOIN plan_engine.plan_version pv
  ON pv.plan_id = p.id AND pv.version = p.active_version;
