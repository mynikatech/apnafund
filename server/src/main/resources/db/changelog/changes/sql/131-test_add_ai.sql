DROP TABLE IF EXISTS test_liquibase_validation;

CREATE TABLE IF NOT EXISTS test_liquibase_validation (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);