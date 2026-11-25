-- DRIVIO Flyway Migration V4: Normal Ride Vehicle Pricing Engine (Delhi NCR)
-- Adds vehicle_pricing_configurations, pricing_audit_logs, and booking pricing_version snapshot.

CREATE TABLE IF NOT EXISTS vehicle_pricing_configurations (
    id BIGSERIAL PRIMARY KEY,
    vehicle_category VARCHAR(50) NOT NULL UNIQUE,
    base_fare DECIMAL(10, 2) NOT NULL,
    per_km_rate DECIMAL(10, 2) NOT NULL,
    per_minute_rate DECIMAL(10, 2) NOT NULL,
    min_fare DECIMAL(10, 2) NOT NULL,
    waiting_free_minutes INT NOT NULL DEFAULT 3,
    waiting_per_minute_rate DECIMAL(10, 2) NOT NULL DEFAULT 1.00,
    night_surcharge_percent DECIMAL(5, 2) NOT NULL DEFAULT 10.00,
    peak_surcharge_percent DECIMAL(5, 2) NOT NULL DEFAULT 20.00,
    cancellation_fee DECIMAL(10, 2) NOT NULL DEFAULT 35.00,
    tax_percentage DECIMAL(5, 2) NOT NULL DEFAULT 5.00,
    version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pricing_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    vehicle_category VARCHAR(50) NOT NULL,
    version INT NOT NULL,
    changed_by_admin_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    old_values TEXT,
    new_values TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS pricing_version INT DEFAULT 1;

INSERT INTO vehicle_pricing_configurations
    (vehicle_category, base_fare, per_km_rate, per_minute_rate, min_fare, waiting_free_minutes, waiting_per_minute_rate, night_surcharge_percent, peak_surcharge_percent, cancellation_fee, tax_percentage, version, is_active)
VALUES
    ('BIKE',          15.00,  5.00, 0.50,  25.00, 3, 1.00, 10.00, 20.00, 25.00, 5.00, 1, TRUE),
    ('AUTO',          25.00, 10.00, 0.75,  40.00, 3, 1.00, 10.00, 20.00, 30.00, 5.00, 1, TRUE),
    ('HATCHBACK',     50.00, 13.00, 1.00,  60.00, 3, 1.00, 10.00, 20.00, 35.00, 5.00, 1, TRUE),
    ('SEDAN',         50.00, 15.00, 1.00,  70.00, 3, 1.00, 10.00, 20.00, 40.00, 5.00, 1, TRUE),
    ('PREMIUM_SEDAN', 60.00, 17.00, 1.25,  85.00, 3, 1.00, 10.00, 20.00, 45.00, 5.00, 1, TRUE),
    ('SUV',           60.00, 20.00, 1.50, 100.00, 3, 1.00, 10.00, 20.00, 50.00, 5.00, 1, TRUE)
ON CONFLICT (vehicle_category) DO NOTHING;
