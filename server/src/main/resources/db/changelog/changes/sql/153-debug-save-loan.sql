DROP TABLE IF EXISTS debug_function_log ;


CREATE TABLE debug_function_log (
    id          BIGSERIAL PRIMARY KEY,
    log_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    step        TEXT,
    variable    TEXT,
    value       TEXT
);
