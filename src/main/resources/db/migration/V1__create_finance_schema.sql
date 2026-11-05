CREATE TABLE transactions (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              customer_id VARCHAR(64) NOT NULL,
                              source VARCHAR(16) NOT NULL,
                              external_id VARCHAR(128) NOT NULL,
                              occurred_at TIMESTAMP NOT NULL,
                              amount DECIMAL(19,2) NOT NULL,
                              currency VARCHAR(3) NOT NULL,
                              direction VARCHAR(8) NOT NULL,
                              description VARCHAR(255) NOT NULL,
                              merchant_name VARCHAR(128) NOT NULL,
                              mcc VARCHAR(4),
                              category VARCHAR(32) NOT NULL,
                              category_source VARCHAR(16) NOT NULL,
                              budget_status VARCHAR(32) NOT NULL,
                              status VARCHAR(16) NOT NULL,
                              duplicate_of_id BIGINT,
                              ingested_at TIMESTAMP NOT NULL,
                              updated_at TIMESTAMP NOT NULL,

                              CONSTRAINT uq_transactions_source_external
                                  UNIQUE (customer_id, source, external_id),

                              CONSTRAINT fk_transactions_duplicate
                                  FOREIGN KEY (duplicate_of_id)
                                      REFERENCES transactions(id)
);

CREATE INDEX idx_tx_customer_time
    ON transactions (customer_id, occurred_at);

CREATE INDEX idx_tx_customer_status_time
    ON transactions (customer_id, status, occurred_at);

CREATE INDEX idx_tx_duplicate_lookup
    ON transactions (customer_id, amount, occurred_at);


CREATE TABLE budgets (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         customer_id VARCHAR(64) NOT NULL,
                         budget_month VARCHAR(7) NOT NULL,
                         total_limit DECIMAL(19,2),
                         created_at TIMESTAMP NOT NULL,
                         updated_at TIMESTAMP NOT NULL,

                         CONSTRAINT uq_budgets_customer_month
                             UNIQUE (customer_id, budget_month),

                         CONSTRAINT ck_budgets_total_positive
                             CHECK (total_limit IS NULL OR total_limit > 0)
);


CREATE TABLE budget_limits (
                               budget_id BIGINT NOT NULL,
                               category VARCHAR(32) NOT NULL,
                               limit_amount DECIMAL(19,2) NOT NULL,

                               PRIMARY KEY (budget_id, category),

                               CONSTRAINT fk_budget_limits_budget
                                   FOREIGN KEY (budget_id)
                                       REFERENCES budgets(id)
                                       ON DELETE CASCADE,

                               CONSTRAINT ck_budget_limit_positive
                                   CHECK (limit_amount > 0)
);


CREATE TABLE sync_runs (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           customer_id VARCHAR(64) NOT NULL,
                           started_at TIMESTAMP NOT NULL,
                           completed_at TIMESTAMP NULL,
                           status VARCHAR(16),
                           fetched_count INT NOT NULL DEFAULT 0,
                           inserted_count INT NOT NULL DEFAULT 0,
                           duplicate_count INT NOT NULL DEFAULT 0,
                           already_known_count INT NOT NULL DEFAULT 0,
                           error_summary VARCHAR(1000)
);

CREATE INDEX idx_sync_runs_customer
    ON sync_runs (customer_id, started_at);
