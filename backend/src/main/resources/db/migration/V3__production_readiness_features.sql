-- DRIVIO Flyway Migration V3: Production Readiness Features
-- Adds Service C (Car + Luggage Delivery) fields, Service B subtypes,
-- OTP verification, Handover inspection, SOS emergency, Quotations, and Payment Gateway metadata.

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS service_subtype VARCHAR(50),
    ADD COLUMN IF NOT EXISTS vehicle_id BIGINT REFERENCES vehicles(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS luggage_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS pickup_otp VARCHAR(10),
    ADD COLUMN IF NOT EXISTS destination_otp VARCHAR(10),
    ADD COLUMN IF NOT EXISTS odometer_start INT,
    ADD COLUMN IF NOT EXISTS odometer_end INT,
    ADD COLUMN IF NOT EXISTS fuel_level_start VARCHAR(50),
    ADD COLUMN IF NOT EXISTS fuel_level_end VARCHAR(50),
    ADD COLUMN IF NOT EXISTS damage_notes_start TEXT,
    ADD COLUMN IF NOT EXISTS damage_notes_end TEXT,
    ADD COLUMN IF NOT EXISTS sos_triggered BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS sos_triggered_at TIMESTAMP;

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS gateway_order_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS gateway_payment_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS gateway_signature VARCHAR(255);

CREATE TABLE IF NOT EXISTS booking_quotations (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    driver_id BIGINT NOT NULL REFERENCES driver_profiles(id) ON DELETE CASCADE,
    proposed_fare DECIMAL(10, 2) NOT NULL,
    customer_counter_fare DECIMAL(10, 2),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_booking_quotations_booking ON booking_quotations(booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_quotations_driver ON booking_quotations(driver_id);
CREATE INDEX IF NOT EXISTS idx_vehicles_user ON vehicles(user_id);

INSERT INTO pricing_configurations (service_type, base_fare, per_km_rate, per_hour_rate, min_fare, cancellation_fee, tax_percentage, is_active)
VALUES ('CAR_LUGGAGE_DELIVERY', 299.00, 16.00, 120.00, 499.00, 100.00, 5.00, TRUE)
ON CONFLICT (service_type) DO NOTHING;
