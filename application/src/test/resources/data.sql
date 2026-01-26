-- Minimal model seed for tests (Hibernate create-drop + SQL init)
INSERT INTO ai_models (provider, model, enabled) VALUES ('GEMINI', 'gemini-2.5-flash-lite', 1);
INSERT INTO ai_models (provider, model, enabled) VALUES ('GPT', 'gpt-5.2-2025-12-11', 1);
INSERT INTO ai_models (provider, model, enabled) VALUES ('GPT', 'gpt-4.1-mini-2025-04-14', 1);
INSERT INTO ai_models (provider, model, enabled) VALUES ('GROK', 'grok-4-1-fast-non-reasoning', 1);
INSERT INTO ai_models (provider, model, enabled) VALUES ('GROK', 'grok-4-1-fast-reasoning', 1);
