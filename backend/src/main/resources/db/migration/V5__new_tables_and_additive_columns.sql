-- DRIVIO Flyway Migration V5: New Tables and Additive Booking Columns
-- Creates all new tables specified in the unified pricing/financial architecture
-- V5 does NOT modify V1-V4 migrations. All changes are additive.

-- ============================================================
-- Enable btree_gist extension for exclusion constraints
-- ============================================================
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ============================================================
-- New Table: pricing_profiles
-- Service family + category key, active flag, metadata
-- ============================================================
CREATE TABLE IF NOT EXISTS pricing_profiles (
    id BIGSERIAL PRIMARY KEY,
    profile_type VARCHAR(50) NOT NULL,          -- 'VEHICLE_BASED' or 'SERVICE_BASED'
    category_key VARCHAR(50) NOT NULL,         -- COALESCE(vehicle_category,'__ALL__') or service_mode key
    vehicle_category VARCHAR(50),              -- NULL for SERVICE_BASED; vehicle type for VEHICLE_BASED
    service_type VARCHAR(50) NOT NULL,          -- service type string (e.g. OWN_CAR_DRIVER)
    base_fare DECIMAL(12, 2) NOT NULL,
    per_km_rate DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    per_minute_rate DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    per_hour_rate DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    min_fare DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    waiting_free_minutes INT NOT NULL DEFAULT 3,
    waiting_per_minute_rate DECIMAL(12, 2) NOT NULL DEFAULT 1.00,
    night_surcharge_percent DECIMAL(5, 2) NOT NULL DEFAULT 10.00,
    peak_surcharge_percent DECIMAL(5, 2) NOT NULL DEFAULT 20.00,
    stack_peak_and_night BOOLEAN NOT NULL DEFAULT FALSE,
    cancellation_fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    platform_commission_percent DECIMAL(5, 2) NOT NULL DEFAULT 25.00,
    tax_rate DECIMAL(5, 2) NOT NULL DEFAULT 5.00,
    tax_mode VARCHAR(20) NOT NULL DEFAULT 'EXCLUSIVE',  -- 'EXCLUSIVE' or 'INCLUSIVE'
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    jurisdiction VARCHAR(100) NOT NULL DEFAULT 'India',
    gateway_fee_percent DECIMAL(5, 2) NOT NULL DEFAULT 5.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    state VARCHAR(20) NOT NULL DEFAULT 'DRAFT',  -- DRAFT/PUBLISHED/RETIRED
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,
    business_version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_prof_state_effective CHECK (
        (state = 'DRAFT' AND effective_to IS NULL) OR
        (state IN ('PUBLISHED', 'RETIRED') AND (effective_to IS NULL OR effective_from <= effective_to))
    ),
    CONSTRAINT chk_prof_version_monotonic CHECK (business_version > 0)
);

-- ============================================================
-- New Table: pricing_profile_versions
-- Generated category_key, effective timestamps, business_version, state
-- Exclusion constraint (btree_gist) preventing overlap
-- ============================================================
CREATE TABLE IF NOT EXISTS pricing_profile_versions (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES pricing_profiles(id) ON DELETE CASCADE,
    category_key VARCHAR(50) NOT NULL,          -- COALESCE(vehicle_category,'__ALL__') or service_mode key
    vehicle_category VARCHAR(50),               -- NULL for SERVICE_BASED; vehicle type for VEHICLE_BASED
    service_type VARCHAR(50),                   -- NULL for VEHICLE_BASED; service type for SERVICE_BASED
    base_fare DECIMAL(12, 2) NOT NULL,
    per_km_rate DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    per_minute_rate DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    per_hour_rate DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    min_fare DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    waiting_free_minutes INT NOT NULL DEFAULT 3,
    waiting_per_minute_rate DECIMAL(12, 2) NOT NULL DEFAULT 1.00,
    night_surcharge_percent DECIMAL(5, 2) NOT NULL DEFAULT 10.00,
    peak_surcharge_percent DECIMAL(5, 2) NOT NULL DEFAULT 20.00,
    stack_peak_and_night BOOLEAN NOT NULL DEFAULT FALSE,
    cancellation_fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    platform_commission_percent DECIMAL(5, 2) NOT NULL DEFAULT 25.00,
    tax_rate DECIMAL(5, 2) NOT NULL DEFAULT 5.00,
    tax_mode VARCHAR(20) NOT NULL DEFAULT 'EXCLUSIVE',
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    jurisdiction VARCHAR(100) NOT NULL DEFAULT 'India',
    gateway_fee_percent DECIMAL(5, 2) NOT NULL DEFAULT 5.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    state VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,
    business_version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Exclusion constraint: prevents overlapping effective periods for same category_key among published versions
    EXCLUDE USING gist (
        category_key WITH =,
        tstzrange(effective_from, effective_to, '[)') WITH &&
    ) WHERE (state = 'PUBLISHED'),
    CONSTRAINT chk_pv_state_effective CHECK (
        (state = 'DRAFT' AND effective_to IS NULL) OR
        (state IN ('PUBLISHED', 'RETIRED') AND (effective_to IS NULL OR effective_from <= effective_to))
    ),
    CONSTRAINT chk_pv_version_monotonic CHECK (business_version > 0)
);

-- Index for fast profile version resolution
CREATE INDEX IF NOT EXISTS idx_pv_category_effective ON pricing_profile_versions (category_key, state, effective_from DESC, business_version DESC);

-- ============================================================
-- New Table: pricing_audit_logs_v2
-- Audit trail for profile version changes (replaces/augments VehiclePricingConfiguration audit logs)
-- ============================================================
CREATE TABLE IF NOT EXISTS pricing_audit_logs_v2 (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT REFERENCES pricing_profiles(id) ON DELETE SET NULL,
    profile_version_id BIGINT REFERENCES pricing_profile_versions(id) ON DELETE SET NULL,
    changed_by_admin_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    old_values JSONB,
    new_values JSONB,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    change_type VARCHAR(50) NOT NULL,  -- 'CREATE', 'UPDATE', 'STATE_CHANGE', 'PUBLISH', 'RETIRE'
    category_key VARCHAR(50)
);

-- ============================================================
-- New Table: financial_transactions
-- Append-only financial event ledger (spec §7)
-- 16 event types with signed amounts, idempotency keys, gateway event IDs
-- ============================================================
CREATE TABLE IF NOT EXISTS financial_transactions (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT REFERENCES bookings(id) ON DELETE RESTRICT,
    driver_profile_id BIGINT REFERENCES driver_profiles(id) ON DELETE SET NULL,
    transaction_id VARCHAR(100) UNIQUE,         -- Unique transaction identifier
    gateway_event_id VARCHAR(100) UNIQUE,       -- Gateway's event identifier (null if not gateway-pushed)
    gateway_payment_id VARCHAR(100),            -- Gateway's payment identifier
    gateway_signature VARCHAR(512),             -- Verification signature
    event_type VARCHAR(50) NOT NULL,            -- PAYMENT_INITIATED, PAYMENT_SUCCESS, etc.
    amount NUMERIC(12, 2) NOT NULL,             -- Signed amount (+ in, - out); can be negative for refunds
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING/COMPLETED/FAILED/RESERVED
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    reference_transaction_id BIGINT REFERENCES financial_transactions(id) ON DELETE SET NULL,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL, -- Enforces idempotency; unique across all events
    CONSTRAINT chk_ft_event_type CHECK (event_type IN (
        'PAYMENT_INITIATED', 'PAYMENT_SUCCESS', 'PAYMENT_CAPTURED', 'PAYMENT_FAILED',
        'FULL_REFUND', 'PARTIAL_REFUND', 'REFUND_FAILED', 'PAYMENT_REVERSAL',
        'COMMISSION_ADJUSTMENT', 'DRIVER_PAYOUT_ADJUSTMENT', 'GATEWAY_RECONCILIATION', 'CANCELLATION_FEE'
    )),
    CONSTRAINT chk_ft_amount_sign CHECK (amount != 0)
);

-- Composite index for booking-related queries
CREATE INDEX IF NOT EXISTS idx_fin_booking_event ON financial_transactions (booking_id, event_type, created_at);

-- ============================================================
-- New Table: driver_earnings
-- Append-only earnings ledger with ACCRUED/ADJUSTED/REVERSED/SETTLED statuses
-- ============================================================
CREATE TABLE IF NOT EXISTS driver_earnings (
    id BIGSERIAL PRIMARY KEY,
    driver_profile_id BIGINT NOT NULL REFERENCES driver_profiles(id) ON DELETE CASCADE,
    booking_id BIGINT REFERENCES bookings(id) ON DELETE SET NULL,
    amount NUMERIC(12, 2) NOT NULL,              -- Earned amount (positive = payout to driver)
    status VARCHAR(20) NOT NULL DEFAULT 'ACCRUED', -- ACCRUED/ADJUSTED/REVERSED/SETTLED
    settled_at TIMESTAMP,
    accrued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description TEXT,                           -- Description of the earning event
    CONSTRAINT chk_de_status CHECK (status IN ('ACCRUED', 'AVAILABLE', 'RESERVED', 'ADJUSTED', 'REVERSED', 'SETTLED')),
    CONSTRAINT chk_de_amount CHECK (amount != 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_de_booking_accrual
    ON driver_earnings (booking_id)
    WHERE booking_id IS NOT NULL AND amount > 0 AND status <> 'REVERSED';

-- Index for driver earnings queries
CREATE INDEX IF NOT EXISTS idx_de_driver_status ON driver_earnings (driver_profile_id, status, accrued_at);

-- ============================================================
-- New Table: driver_settlements
-- Settlement header: driver_id, total_amount, status, completed_at
-- ============================================================
CREATE TABLE IF NOT EXISTS driver_settlements (
    id BIGSERIAL PRIMARY KEY,
    driver_profile_id BIGINT NOT NULL REFERENCES driver_profiles(id) ON DELETE CASCADE,
    total_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING/COMPLETED/FAILED
    completed_at TIMESTAMP,
    initiated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_ds_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

-- ============================================================
-- New Table: driver_settlement_items
-- Individual earning items per booking linking to driver_earnings
-- ============================================================
CREATE TABLE IF NOT EXISTS driver_settlement_items (
    id BIGSERIAL PRIMARY KEY,
    settlement_id BIGINT NOT NULL REFERENCES driver_settlements(id) ON DELETE CASCADE,
    driver_earnings_id BIGINT REFERENCES driver_earnings(id) ON DELETE SET NULL,
    booking_id BIGINT REFERENCES bookings(id) ON DELETE SET NULL,
    amount NUMERIC(12, 2) NOT NULL,              -- Amount from this earning item
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING/COMPLETED/FAILED
    settled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT cis_item CHECK (amount != 0)
);

-- Index for settlement item queries
CREATE INDEX IF NOT EXISTS idx_dsi_settlement ON driver_settlement_items (settlement_id, status);

-- ============================================================
-- New Table: driver_settlement_attempts
-- Attempt tracking for failed settlements: attempt number, status, error_message
-- ============================================================
CREATE TABLE IF NOT EXISTS driver_settlement_attempts (
    id BIGSERIAL PRIMARY KEY,
    settlement_id BIGINT NOT NULL REFERENCES driver_settlements(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL,                 -- 1-based attempt counter
    status VARCHAR(50) NOT NULL,                 -- 'PENDING','SUCCESS','FAILED'
    error_message TEXT,
    tried_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT dsa_attempt_uniq UNIQUE (settlement_id, attempt_number)
);

-- Index for settlement attempt queries
CREATE INDEX IF NOT EXISTS idx_dsa_settlement ON driver_settlement_attempts (settlement_id, tried_at);

-- ============================================================
-- Additive Booking Columns
-- fare_snapshot JSONB: immutable structured snapshot of all fare components at booking creation
-- pricing_profile_version_id BIGINT: FK to pricing_profile_versions; NULL = legacy booking using stored columns, never recomputed
-- ============================================================
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS fare_snapshot JSONB,
    ADD COLUMN IF NOT EXISTS pricing_profile_version_id BIGINT NULL;

-- Comment: fare_snapshot JSONB preserves the exact fare calculation used at booking time
-- Comment: pricing_profile_version_id NULL means legacy booking; never recompute historical fares

-- ============================================================
-- DB Trigger: reject_fare_updates
-- Rejects updates to fare columns post-insert to preserve immutability
-- ============================================================
CREATE OR REPLACE FUNCTION reject_fare_updates()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.base_fare <> OLD.base_fare THEN
        RAISE EXCEPTION 'Booking fare components are immutable post-insert. Use fare_snapshot JSONB for historical changes.';
    END IF;
    IF NEW.distance_fare <> OLD.distance_fare THEN
        RAISE EXCEPTION 'Booking fare components are immutable post-insert. Use fare_snapshot JSONB for historical changes.';
    END IF;
    IF NEW.duration_fare <> OLD.duration_fare THEN
        RAISE EXCEPTION 'Booking fare components are immutable post-insert. Use fare_snapshot JSONB for historical changes.';
    END IF;
    IF NEW.total_fare <> OLD.total_fare THEN
        RAISE EXCEPTION 'Booking fare components are immutable post-insert. Use fare_snapshot JSONB for historical changes.';
    END IF;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER booking_fare_immutability_trigger
BEFORE UPDATE ON bookings
FOR EACH ROW EXECUTE FUNCTION reject_fare_updates();

-- ============================================================
-- V5 Migration Complete
-- ============================================================
COMMENT ON TABLE pricing_profiles IS 'Unified pricing profiles with versioning, effective-dating, and state management';
COMMENT ON TABLE pricing_profile_versions IS 'Generated pricing profile versions with exclusion constraints preventing overlap';
COMMENT ON TABLE financial_transactions IS 'Append-only financial event ledger with idempotency keys and gateway event tracking';
COMMENT ON TABLE driver_earnings IS 'Append-only earnings ledger with ACCRUED/ADJUSTED/REVERSED/SETTLED statuses';
COMMENT ON TABLE driver_settlements IS 'Driver settlement headers';
COMMENT ON TABLE driver_settlement_items IS 'Individual earning items per booking linking to driver_earnings';
COMMENT ON TABLE driver_settlement_attempts IS 'Attempt tracking for failed settlements';
COMMENT ON COLUMN bookings.fare_snapshot IS 'Immutable JSONB snapshot of all fare components at booking creation; never recomputed for historical bookings';
COMMENT ON COLUMN bookings.pricing_profile_version_id IS 'FK to pricing_profile_versions; NULL = legacy booking using stored columns, never recomputed';