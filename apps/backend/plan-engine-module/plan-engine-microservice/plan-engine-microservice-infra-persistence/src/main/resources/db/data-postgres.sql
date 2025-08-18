-- ============================================
-- Semillas iniciales (plantillas de plan)
-- Ejecuta esto después de schema-postgres.sql
-- ============================================

-- Limpia posibles datos previos de prueba (orden por FKs)
DELETE FROM plan_engine.recompute_event;
DELETE FROM plan_engine.plan_version;
DELETE FROM plan_engine.plan;
DELETE FROM plan_engine.plan_template;

-- Plantilla: EMERGENCIA_RAPIDA
INSERT INTO plan_engine.plan_template (
  id, name, version, params_schema, scoring_weights, constraints_doc, protected, checksum
) VALUES (
  'EMERGENCIA_RAPIDA',
  'Emergencia Rapida',
  1,
  '{
    "type":"object",
    "properties":{
      "saving_pct":{"type":"number","minimum":0,"maximum":1},
      "emergency_months":{"type":"integer","minimum":0,"maximum":24},
      "envelopes":{"type":"object","additionalProperties":{"type":"number","minimum":0,"maximum":1}}
    },
    "required":["saving_pct","emergency_months"]
  }'::jsonb,
  '{
    "risk":0.15,"liquidity":0.30,"debt":0.25,"gap":0.20,"age":0.05,"stability":0.05
  }'::jsonb,
  '{
    "min_emergency_months":3,
    "max_emergency_months":12,
    "saving_pct_range":[0.20,0.50],
    "envelope_bounds":{"fijos":[0.40,0.60],"variables":[0.15,0.40],"objetivos":[0.10,0.40]}
  }'::jsonb,
  TRUE,
  'chk-emer-v1'
);

-- Plantilla: EQUILIBRADO_50_30_20
INSERT INTO plan_engine.plan_template (
  id, name, version, params_schema, scoring_weights, constraints_doc, protected, checksum
) VALUES (
  'EQUILIBRADO_50_30_20',
  'Equilibrado 50-30-20',
  1,
  '{
    "type":"object",
    "properties":{
      "saving_pct":{"type":"number","minimum":0,"maximum":1},
      "emergency_months":{"type":"integer","minimum":0,"maximum":24},
      "envelopes":{"type":"object","additionalProperties":{"type":"number","minimum":0,"maximum":1}}
    },
    "required":["saving_pct","emergency_months"]
  }'::jsonb,
  '{
    "risk":0.20,"liquidity":0.20,"debt":0.20,"gap":0.20,"age":0.10,"stability":0.10
  }'::jsonb,
  '{
    "saving_pct_range":[0.20,0.35],
    "target_split":{"fijos":0.50,"variables":0.30,"objetivos":0.20},
    "tolerance":0.05
  }'::jsonb,
  TRUE,
  'chk-eq-502020-v1'
);

-- Plantilla: ANTIDEUDA_AVALANCHE
INSERT INTO plan_engine.plan_template (
  id, name, version, params_schema, scoring_weights, constraints_doc, protected, checksum
) VALUES (
  'ANTIDEUDA_AVALANCHE',
  'Antideuda Avalanche',
  1,
  '{
    "type":"object",
    "properties":{
      "saving_pct":{"type":"number","minimum":0,"maximum":1},
      "emergency_months":{"type":"integer","minimum":0,"maximum":24},
      "envelopes":{"type":"object","additionalProperties":{"type":"number","minimum":0,"maximum":1}}
    },
    "required":["saving_pct","emergency_months"]
  }'::jsonb,
  '{
    "risk":0.10,"liquidity":0.15,"debt":0.45,"gap":0.20,"age":0.05,"stability":0.05
  }'::jsonb,
  '{
    "debt_strategy":"avalanche",
    "threshold_apr_high":0.12,
    "saving_pct_range":[0.10,0.30]
  }'::jsonb,
  TRUE,
  'chk-debt-ava-v1'
);

-- Plantilla: INVERSION_PROGRESIVA
INSERT INTO plan_engine.plan_template (
  id, name, version, params_schema, scoring_weights, constraints_doc, protected, checksum
) VALUES (
  'INVERSION_PROGRESIVA',
  'Inversion Progresiva',
  1,
  '{
    "type":"object",
    "properties":{
      "saving_pct":{"type":"number","minimum":0,"maximum":1},
      "emergency_months":{"type":"integer","minimum":0,"maximum":24},
      "envelopes":{"type":"object","additionalProperties":{"type":"number","minimum":0,"maximum":1}}
    },
    "required":["saving_pct","emergency_months"]
  }'::jsonb,
  '{
    "risk":0.25,"liquidity":0.10,"debt":0.15,"gap":0.20,"age":0.15,"stability":0.15
  }'::jsonb,
  '{
    "min_emergency_months":3,
    "saving_pct_range":[0.20,0.40],
    "ramp_up_months":6
  }'::jsonb,
  TRUE,
  'chk-inv-prog-v1'
);

-- ============================================
-- Datos de prueba (opcional)
-- ============================================

-- Crea un plan de ejemplo para un usuario (status por defecto = 'ACTIVE')
WITH new_plan AS (
  INSERT INTO plan_engine.plan (user_id, active_version, status)
  VALUES ('00000000-0000-0000-0000-000000000001', 1, 'ACTIVE')
  RETURNING id
)
INSERT INTO plan_engine.plan_version (
  plan_id, version, template_id, params, kpis, rationale, alerts, source
)
SELECT
  id, 1, 'EQUILIBRADO_50_30_20',
  '{
    "saving_pct":0.27,
    "emergency_months":7,
    "envelopes":{"fijos":0.45,"variables":0.28,"objetivos":0.27}
  }'::jsonb,
  '{
    "tasa_ahorro":0.27,
    "runway_meses":5.8,
    "cumplimiento_global":0.92
  }'::jsonb,
  'Plan inicial basado en perfil equilibrado.',
  '["Ajusta ocio si superas 10% del limite"]'::jsonb,
  'rules+gpt'
FROM new_plan;
