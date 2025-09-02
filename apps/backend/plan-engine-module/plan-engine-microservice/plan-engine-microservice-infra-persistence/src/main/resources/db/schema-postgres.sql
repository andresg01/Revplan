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

  -- source permitido (tolerante con underscore/plus)
  IF LOWER(NEW.source) NOT IN ('rules','rules_plus_gpt','rules+gpt') THEN
    RAISE EXCEPTION 'source must be one of: rules, rules_plus_gpt';
  END IF;

  RETURN NEW;
END
$$;

-- Mantiene una única respuesta "is_latest = true" por (user_id, question_id)
CREATE OR REPLACE FUNCTION plan_engine.onboarding_answer_set_latest()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.is_latest THEN
    UPDATE plan_engine.onboarding_answer
       SET is_latest = FALSE,
           updated_at = now()
     WHERE user_id = NEW.user_id
       AND question_id = NEW.question_id
       AND id <> NEW.id
       AND is_latest = TRUE;
  END IF;
  RETURN NEW;
END
$$;

-- Construye un snapshot de perfil (JSON) a partir de las últimas respuestas
-- Convención: se esperan los IDs de pregunta AGE, RISK_SCORE, INCOME_NET, EXPENSES_FIXED, EXPENSES_VARIABLE,
-- STABILITY_INDEX, LIQUIDITY_NEED, GOALS, HORIZON, DEBT_SEVERITY, DEPENDENTS
CREATE OR REPLACE FUNCTION plan_engine.onb_profile_from_answers(_user_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
AS $$
DECLARE
  kv RECORD;
  h JSONB := '{}'::jsonb;
BEGIN
  FOR kv IN
    SELECT q.id AS qid, a.value
      FROM plan_engine.onboarding_answer a
      JOIN plan_engine.onboarding_question q ON q.id = a.question_id
     WHERE a.user_id = _user_id
       AND a.is_latest = TRUE
  LOOP
    IF kv.qid = 'AGE' THEN
      h := h || jsonb_build_object('age', (kv.value)::int);
    ELSIF kv.qid = 'RISK_SCORE' THEN
      h := h || jsonb_build_object('riskScore', (kv.value)::int);
    ELSIF kv.qid = 'INCOME_NET' THEN
      h := h || jsonb_build_object('incomeNet', (kv.value)::numeric);
    ELSIF kv.qid = 'EXPENSES_FIXED' THEN
      h := h || jsonb_build_object('expensesFixed', (kv.value)::numeric);
    ELSIF kv.qid = 'EXPENSES_VARIABLE' THEN
      h := h || jsonb_build_object('expensesVariable', (kv.value)::numeric);
    ELSIF kv.qid = 'STABILITY_INDEX' THEN
      h := h || jsonb_build_object('stabilityIndex', (kv.value)::numeric);
    ELSIF kv.qid = 'LIQUIDITY_NEED' THEN
      h := h || jsonb_build_object('liquidityNeed', (kv.value)::numeric);
    ELSIF kv.qid = 'GOALS' THEN
      h := h || jsonb_build_object('goals', kv.value);
    ELSIF kv.qid = 'HORIZON' THEN
      h := h || jsonb_build_object('horizon', kv.value);
    ELSIF kv.qid = 'DEBT_SEVERITY' THEN
      h := h || jsonb_build_object('debtSeverity', (kv.value)::numeric);
    ELSIF kv.qid = 'DEPENDENTS' THEN
      h := h || jsonb_build_object('dependents', (kv.value)::int);
    END IF;
  END LOOP;

  RETURN h;
END
$$;

-- ============================================
-- Tablas principales (PLAN)
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
  params           JSONB NOT NULL,          -- ajustes (saving_pct, emergency_months, envelopes..., ai_meta opcional)
  kpis             JSONB NOT NULL,          -- savingRate, runwayMonths, budgetCompliance (o traducciones)
  rationale        TEXT,
  alerts           JSONB NOT NULL DEFAULT '[]'::jsonb,
  source           TEXT NOT NULL,           -- 'rules' | 'rules_plus_gpt' | 'rules+gpt'
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
-- Ampliamos el CHECK para aceptar tanto los valores antiguos como los del OpenAPI actual:
-- 'balance_change','spend_drift','periodic_review','manual'
-- además de los históricos 'balance_update','overspend','sync'
CREATE TABLE IF NOT EXISTS plan_engine.recompute_event (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  plan_id          UUID NOT NULL REFERENCES plan_engine.plan(id) ON DELETE CASCADE,
  reason           TEXT NOT NULL,
  payload          JSONB NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  processed_at     TIMESTAMPTZ,
  CONSTRAINT ck_recompute_reason CHECK (
    reason IN ('balance_update','overspend','manual','sync','balance_change','spend_drift','periodic_review')
  )
);

CREATE INDEX IF NOT EXISTS idx_recompute_plan
  ON plan_engine.recompute_event (plan_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_recompute_pending
  ON plan_engine.recompute_event (plan_id)
  WHERE processed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_recompute_payload_gin
  ON plan_engine.recompute_event USING GIN (payload jsonb_path_ops);

-- ============================================
-- Tablas Onboarding (preguntas & respuestas)
-- ============================================

-- Bloques/páginas del cuestionario
CREATE TABLE IF NOT EXISTS plan_engine.onboarding_block (
  id           TEXT PRIMARY KEY CHECK (id ~ '^[A-Z0-9_]+$'),
  title        TEXT NOT NULL CHECK (char_length(title) BETWEEN 3 AND 200),
  description  TEXT,
  sort_order   INTEGER NOT NULL DEFAULT 0,
  active       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_onb_block_active_order
  ON plan_engine.onboarding_block (active, sort_order);

CREATE TRIGGER trg_onb_block_updated_at
BEFORE UPDATE ON plan_engine.onboarding_block
FOR EACH ROW EXECUTE FUNCTION plan_engine.tg_set_updated_at();

-- Preguntas
CREATE TABLE IF NOT EXISTS plan_engine.onboarding_question (
  id            TEXT PRIMARY KEY CHECK (id ~ '^[A-Z0-9_]+$'), -- e.g., AGE, RISK_SCORE...
  block_id      TEXT NOT NULL REFERENCES plan_engine.onboarding_block(id) ON DELETE CASCADE,
  qtype         TEXT NOT NULL CHECK (qtype IN ('integer','number','string','boolean','single_select','multi_select')),
  label         TEXT NOT NULL,
  help          TEXT,
  required      BOOLEAN NOT NULL DEFAULT TRUE,
  placeholder   TEXT,
  sort_order    INTEGER NOT NULL DEFAULT 0,
  visible_if    JSONB,      -- lógica condicional opcional
  validations   JSONB,      -- min/max/pattern/step...
  options_src   TEXT,       -- fuente externa (si aplica)
  active        BOOLEAN NOT NULL DEFAULT TRUE,
  meta          JSONB,      -- extensible
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_onb_question_block_order
  ON plan_engine.onboarding_question (block_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_onb_question_active
  ON plan_engine.onboarding_question (active);

CREATE TRIGGER trg_onb_question_updated_at
BEFORE UPDATE ON plan_engine.onboarding_question
FOR EACH ROW EXECUTE FUNCTION plan_engine.tg_set_updated_at();

-- Opciones para selects (single/multi)
CREATE TABLE IF NOT EXISTS plan_engine.onboarding_question_option (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  question_id  TEXT NOT NULL REFERENCES plan_engine.onboarding_question(id) ON DELETE CASCADE,
  value        JSONB NOT NULL,  -- puede ser string/number/objeto
  label        TEXT NOT NULL,
  sort_order   INTEGER NOT NULL DEFAULT 0,
  meta         JSONB,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_onb_option_question
  ON plan_engine.onboarding_question_option (question_id, sort_order);

CREATE TRIGGER trg_onb_option_updated_at
BEFORE UPDATE ON plan_engine.onboarding_question_option
FOR EACH ROW EXECUTE FUNCTION plan_engine.tg_set_updated_at();

-- Sesiones de onboarding
CREATE TABLE IF NOT EXISTS plan_engine.onboarding_session (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       UUID NOT NULL,
  status        TEXT NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS','COMPLETED','ABANDONED')),
  started_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  completed_at  TIMESTAMPTZ,
  last_seen_at  TIMESTAMPTZ,
  device_info   JSONB,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Sólo una sesión en curso por usuario
CREATE UNIQUE INDEX IF NOT EXISTS uq_onb_session_user_inprogress
  ON plan_engine.onboarding_session (user_id)
  WHERE status = 'IN_PROGRESS';

CREATE INDEX IF NOT EXISTS idx_onb_session_user_status
  ON plan_engine.onboarding_session (user_id, status);

CREATE TRIGGER trg_onb_session_updated_at
BEFORE UPDATE ON plan_engine.onboarding_session
FOR EACH ROW EXECUTE FUNCTION plan_engine.tg_set_updated_at();

-- Respuestas (histórico + marca latest)
CREATE TABLE IF NOT EXISTS plan_engine.onboarding_answer (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  session_id   UUID REFERENCES plan_engine.onboarding_session(id) ON DELETE SET NULL,
  user_id      UUID NOT NULL,
  question_id  TEXT NOT NULL REFERENCES plan_engine.onboarding_question(id) ON DELETE CASCADE,
  value        JSONB NOT NULL,
  is_latest    BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Asegura una única respuesta "vigente" por (user, question)
CREATE UNIQUE INDEX IF NOT EXISTS uq_onb_answer_latest
  ON plan_engine.onboarding_answer (user_id, question_id)
  WHERE is_latest = TRUE;

CREATE INDEX IF NOT EXISTS idx_onb_answer_user_question
  ON plan_engine.onboarding_answer (user_id, question_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_onb_answer_session
  ON plan_engine.onboarding_answer (session_id);

CREATE TRIGGER trg_onb_answer_set_latest_bi
BEFORE INSERT ON plan_engine.onboarding_answer
FOR EACH ROW EXECUTE FUNCTION plan_engine.onboarding_answer_set_latest();

CREATE TRIGGER trg_onb_answer_set_latest_bu
BEFORE UPDATE OF is_latest ON plan_engine.onboarding_answer
FOR EACH ROW EXECUTE FUNCTION plan_engine.onboarding_answer_set_latest();

CREATE TRIGGER trg_onb_answer_updated_at
BEFORE UPDATE ON plan_engine.onboarding_answer
FOR EACH ROW EXECUTE FUNCTION plan_engine.tg_set_updated_at();

-- Snapshot de perfil derivado de respuestas (opcional, cache)
CREATE TABLE IF NOT EXISTS plan_engine.onboarding_profile_snapshot (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID NOT NULL UNIQUE,
  snapshot    JSONB NOT NULL,  -- debe mapear con ProfileSnapshot del OpenAPI
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_onb_profile_snapshot_user
  ON plan_engine.onboarding_profile_snapshot (user_id);

CREATE TRIGGER trg_onb_profile_snapshot_updated_at
BEFORE UPDATE ON plan_engine.onboarding_profile_snapshot
FOR EACH ROW EXECUTE FUNCTION plan_engine.tg_set_updated_at();

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

-- Respuestas vigentes (últimas) por usuario y pregunta
CREATE OR REPLACE VIEW plan_engine.v_onboarding_answers_latest AS
SELECT a.user_id, a.question_id, a.value, a.updated_at
  FROM plan_engine.onboarding_answer a
 WHERE a.is_latest = TRUE;

-- Perfil derivado "al vuelo" (helper para debug/BI)
CREATE OR REPLACE VIEW plan_engine.v_onboarding_profile_preview AS
SELECT
  u.user_id,
  plan_engine.onb_profile_from_answers(u.user_id) AS profile_snapshot
FROM (
  SELECT DISTINCT user_id FROM plan_engine.onboarding_answer
) u;
