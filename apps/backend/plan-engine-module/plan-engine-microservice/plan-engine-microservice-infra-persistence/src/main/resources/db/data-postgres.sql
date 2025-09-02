-- ============================================
-- Semillas iniciales (plantillas y onboarding)
-- Ejecuta esto después del schema
-- ============================================

-- Limpia posibles datos previos de prueba (orden por FKs)

-- Onboarding primero (por dependencias)
DELETE FROM plan_engine.onboarding_profile_snapshot;
DELETE FROM plan_engine.onboarding_answer;
DELETE FROM plan_engine.onboarding_session;
DELETE FROM plan_engine.onboarding_question_option;
DELETE FROM plan_engine.onboarding_question;
DELETE FROM plan_engine.onboarding_block;

-- Plan tables
DELETE FROM plan_engine.recompute_event;
DELETE FROM plan_engine.plan_version;
DELETE FROM plan_engine.plan;
DELETE FROM plan_engine.plan_template;

-- ============================================
-- Plantillas de plan (catálogo)
-- ============================================

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
    "envelope_bounds":{"fixed":[0.40,0.60],"variable":[0.15,0.40],"goals":[0.10,0.40]}
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
    "target_split":{"fixed":0.50,"variable":0.30,"goals":0.20},
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
-- Onboarding (bloques, preguntas, opciones)
-- ============================================

-- Bloques
INSERT INTO plan_engine.onboarding_block (id, title, description, sort_order, active)
VALUES
  ('BASICS', 'Datos básicos', 'Edad, dependientes, horizonte temporal.', 10, TRUE),
  ('RISK_LIQUIDITY', 'Riesgo y liquidez', 'Preferencias de riesgo y necesidad de liquidez.', 20, TRUE),
  ('INCOME_EXPENSES', 'Ingresos y gastos', 'Ingresos netos y gasto fijo/variable.', 30, TRUE),
  ('GOALS', 'Objetivos', 'Objetivos financieros prioritarios.', 40, TRUE);

-- Preguntas
INSERT INTO plan_engine.onboarding_question
(id, block_id, qtype, label, help, required, placeholder, sort_order, validations, active, meta)
VALUES
  ('AGE', 'BASICS', 'integer', '¿Cuál es tu edad?', 'Introduce tu edad en años.', TRUE, 'p. ej. 32', 10,
    '{"min":16,"max":100}'::jsonb, TRUE, '{"profileKey":"age"}'::jsonb),
  ('DEPENDENTS', 'BASICS', 'integer', 'Personas a tu cargo', 'Número de dependientes económicos.', TRUE, '0', 20,
    '{"min":0,"max":20}'::jsonb, TRUE, '{"profileKey":"dependents"}'::jsonb),
  ('HORIZON', 'BASICS', 'single_select', 'Horizonte principal', 'Plazo que tienes en mente.', TRUE, NULL, 30,
    NULL, TRUE, '{"profileKey":"horizon"}'::jsonb),

  ('RISK_SCORE', 'RISK_LIQUIDITY', 'integer', 'Tolerancia al riesgo (1-5)', '1=baja, 5=alta.', TRUE, NULL, 10,
    '{"min":1,"max":5}'::jsonb, TRUE, '{"profileKey":"riskScore"}'::jsonb),
  ('STABILITY_INDEX', 'RISK_LIQUIDITY', 'number', 'Estabilidad de ingresos (0-1)', '0=volátil, 1=estable.', TRUE, NULL, 20,
    '{"min":0,"max":1,"step":0.01}'::jsonb, TRUE, '{"profileKey":"stabilityIndex"}'::jsonb),
  ('LIQUIDITY_NEED', 'RISK_LIQUIDITY', 'number', 'Necesidad de liquidez (0-1)', '0=baja, 1=alta.', TRUE, NULL, 30,
    '{"min":0,"max":1,"step":0.01}'::jsonb, TRUE, '{"profileKey":"liquidityNeed"}'::jsonb),
  ('DEBT_SEVERITY', 'RISK_LIQUIDITY', 'number', 'Severidad de deuda (0-1)', '0=sin deuda, 1=crítica.', FALSE, NULL, 40,
    '{"min":0,"max":1,"step":0.01}'::jsonb, TRUE, '{"profileKey":"debtSeverity"}'::jsonb),

  ('INCOME_NET', 'INCOME_EXPENSES', 'number', 'Ingreso neto mensual (€)', 'Ingreso neto promedio.', TRUE, 'p. ej. 2500', 10,
    '{"min":0,"step":0.01}'::jsonb, TRUE, '{"profileKey":"incomeNet"}'::jsonb),
  ('EXPENSES_FIXED', 'INCOME_EXPENSES', 'number', 'Gasto fijo mensual (€)', NULL, TRUE, 'p. ej. 1200', 20,
    '{"min":0,"step":0.01}'::jsonb, TRUE, '{"profileKey":"expensesFixed"}'::jsonb),
  ('EXPENSES_VARIABLE', 'INCOME_EXPENSES', 'number', 'Gasto variable mensual (€)', NULL, TRUE, 'p. ej. 600', 30,
    '{"min":0,"step":0.01}'::jsonb, TRUE, '{"profileKey":"expensesVariable"}'::jsonb),

  ('GOALS', 'GOALS', 'multi_select', '¿Qué objetivos te importan ahora?', 'Puedes elegir varios.', TRUE, NULL, 10,
    NULL, TRUE, '{"profileKey":"goals"}'::jsonb);

-- Opciones para HORIZON
INSERT INTO plan_engine.onboarding_question_option (question_id, value, label, sort_order)
VALUES
  ('HORIZON','"short"'::jsonb,'Corto (≤ 1 año)',10),
  ('HORIZON','"medium"'::jsonb,'Medio (1-3 años)',20),
  ('HORIZON','"long"'::jsonb,'Largo (≥ 3 años)',30);

-- Opciones para GOALS
INSERT INTO plan_engine.onboarding_question_option (question_id, value, label, sort_order)
VALUES
  ('GOALS','"emergency"'::jsonb,'Fondo de emergencia',10),
  ('GOALS','"investment"'::jsonb,'Inversión',20),
  ('GOALS','"housing"'::jsonb,'Vivienda',30),
  ('GOALS','"travel"'::jsonb,'Viajes',40),
  ('GOALS','"debt_reduction"'::jsonb,'Reducir deudas',50);

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
    "envelopes":{"fixed":0.45,"variable":0.28,"goals":0.27},
    "ai_meta": {
      "provider":"openai",
      "model":"gpt-4o-mini",
      "promptVersion":"2025-08-19",
      "tokensPrompt": 1111,
      "tokensOutput": 333,
      "latencyMs": 720
    }
  }'::jsonb,
  '{
    "savingRate":0.27,
    "runwayMonths":5.8,
    "budgetCompliance":1.00
  }'::jsonb,
  'Plan inicial basado en perfil equilibrado.',
  '["Ajusta ocio si superas 10% del limite"]'::jsonb,
  'rules+gpt'
FROM new_plan;

-- Sesión de onboarding de ejemplo
INSERT INTO plan_engine.onboarding_session (user_id, status, started_at, last_seen_at, device_info)
VALUES ('00000000-0000-0000-0000-000000000001','IN_PROGRESS', now(), now(), '{"platform":"ios"}');

-- Usa la última sesión creada
WITH s AS (
  SELECT id, user_id FROM plan_engine.onboarding_session
   WHERE user_id='00000000-0000-0000-0000-000000000001'
   ORDER BY started_at DESC LIMIT 1
)
-- Respuestas de ejemplo (coinciden con tu cURL de generación)
INSERT INTO plan_engine.onboarding_answer (session_id, user_id, question_id, value) VALUES
  ((SELECT id FROM s), (SELECT user_id FROM s), 'AGE',                '32'::jsonb),
  ((SELECT id FROM s), (SELECT user_id FROM s), 'RISK_SCORE',         '3'::jsonb),
  ((SELECT id FROM s), (SELECT user_id FROM s), 'INCOME_NET',         '2500'::jsonb),
  ((SELECT id FROM s), (SELECT user_id FROM s), 'EXPENSES_FIXED',     '1200'::jsonb),
  ((SELECT id FROM s), (SELECT user_id FROM s), 'EXPENSES_VARIABLE',  '600'::jsonb),
  ((SELECT id FROM s), (SELECT user_id FROM s), 'STABILITY_INDEX',    '0.7'::jsonb),
  ((SELECT id FROM s), (SELECT user_id FROM s), 'LIQUIDITY_NEED',     '0.4'::jsonb),
  ((SELECT id FROM s), (SELECT user_id FROM s), 'GOALS',              '["emergency","investment"]'::jsonb),
  ((SELECT id FROM s), (SELECT user_id FROM s), 'DEBT_SEVERITY',      '0.2'::jsonb),
  ((SELECT id FROM s), (SELECT user_id FROM s), 'HORIZON',            '"medium"'::jsonb);

-- Marca la sesión como completada (opcional en seeds)
UPDATE plan_engine.onboarding_session
   SET status='COMPLETED', completed_at=now(), updated_at=now()
 WHERE user_id='00000000-0000-0000-0000-000000000001'
 ORDER BY started_at DESC LIMIT 1;

-- Genera y guarda un snapshot de perfil a partir de las respuestas
INSERT INTO plan_engine.onboarding_profile_snapshot (user_id, snapshot)
VALUES (
  '00000000-0000-0000-0000-000000000001',
  plan_engine.onb_profile_from_answers('00000000-0000-0000-0000-000000000001')
)
ON CONFLICT (user_id) DO
UPDATE SET snapshot = EXCLUDED.snapshot, updated_at = now();
