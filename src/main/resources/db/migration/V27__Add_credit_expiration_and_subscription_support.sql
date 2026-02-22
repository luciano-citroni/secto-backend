-- Add expiration tracking to credit transactions (each purchase = a "lot" with its own expiry)
ALTER TABLE secto.credit_transaction ADD COLUMN expires_at TIMESTAMP;
ALTER TABLE secto.credit_transaction ADD COLUMN remaining_amount NUMERIC(19,2);
ALTER TABLE secto.credit_transaction ADD COLUMN source_type VARCHAR(20);
ALTER TABLE secto.credit_transaction ADD COLUMN interval_type VARCHAR(20);

-- Add stripe_customer_id to company for subscription management
ALTER TABLE secto.company ADD COLUMN stripe_customer_id VARCHAR(255);

-- Backfill existing positive transactions (purchases): 30-day expiry, remaining = amount
UPDATE secto.credit_transaction
SET remaining_amount = amount,
    expires_at = created_at + INTERVAL '30 days',
    source_type = 'ONE_TIME'
WHERE amount > 0 AND remaining_amount IS NULL;

-- Backfill existing negative transactions (usage): remaining = 0, no expiry
UPDATE secto.credit_transaction
SET remaining_amount = 0,
    source_type = 'USAGE'
WHERE amount <= 0 AND remaining_amount IS NULL;
