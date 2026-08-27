-- payment-service schema.
--
-- Note what is absent: there is no column for a card number, an expiry, a CVV or
-- a cardholder name, because the service never accepts them. Storing card data
-- would place this application in PCI-DSS scope. A demonstration project should
-- design that requirement away rather than pretend to meet it.

CREATE TABLE payments (
    id                    VARCHAR(36)           NOT NULL,
    order_id              VARCHAR(36)           NOT NULL,
    user_id               VARCHAR(36)           NOT NULL,
    amount                NUMERIC(12, 2) NOT NULL,
    currency              VARCHAR(3)     NOT NULL,
    method                VARCHAR(30)    NOT NULL,
    status                VARCHAR(20)    NOT NULL,
    transaction_reference VARCHAR(40)    NOT NULL,
    failure_reason        VARCHAR(300),
    created_at            TIMESTAMP      NOT NULL,
    settled_at            TIMESTAMP,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uq_payments_transaction_ref UNIQUE (transaction_reference),
    -- One payment per order. This is what stops a retried checkout, or a
    -- double-clicked pay button, from opening a second payment for the same
    -- order; the database enforces it rather than relying on the check in the
    -- service being reached first.
    CONSTRAINT uq_payments_order UNIQUE (order_id),
    CONSTRAINT ck_payments_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_payments_order ON payments (order_id);
CREATE INDEX idx_payments_user ON payments (user_id);
CREATE INDEX idx_payments_status ON payments (status);

-- Append-only trail. Rows are never updated, so the history of a disputed
-- payment stays intact.
CREATE TABLE payment_transactions (
    id                VARCHAR(36)           NOT NULL,
    payment_id        VARCHAR(36)           NOT NULL,
    type              VARCHAR(20)    NOT NULL,
    amount            NUMERIC(12, 2) NOT NULL,
    gateway_reference VARCHAR(60)    NOT NULL,
    message           VARCHAR(300),
    occurred_at       TIMESTAMP      NOT NULL,
    CONSTRAINT pk_payment_transactions PRIMARY KEY (id),
    CONSTRAINT fk_payment_transactions_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE CASCADE
);

CREATE INDEX idx_payment_transactions_payment ON payment_transactions (payment_id);
