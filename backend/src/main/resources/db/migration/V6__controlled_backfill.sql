-- DRIVIO Flyway Migration V6: Controlled Backfill & Settlement Attempt Enhancements
-- Backfills pricing_profiles & pricing_profile_versions from legacy configs without mutating V1-V4.

ALTER TABLE driver_settlement_attempts
    ADD COLUMN IF NOT EXISTS gateway_reference VARCHAR(100);

-- 1. Backfill VEHICLE_BASED profiles from vehicle_pricing_configurations
INSERT INTO pricing_profiles (
    profile_type, category_key, vehicle_category, service_type,
    base_fare, per_km_rate, per_minute_rate, per_hour_rate, min_fare,
    waiting_free_minutes, waiting_per_minute_rate,
    night_surcharge_percent, peak_surcharge_percent, stack_peak_and_night,
    cancellation_fee, platform_commission_percent, tax_rate, tax_mode,
    currency, jurisdiction, gateway_fee_percent, is_active, state,
    effective_from, effective_to, business_version
)
SELECT
    'VEHICLE_BASED',
    LOWER(vehicle_category),
    vehicle_category,
    'ONE_WAY',
    base_fare, per_km_rate, per_minute_rate, 0.00, min_fare,
    waiting_free_minutes, waiting_per_minute_rate,
    night_surcharge_percent, peak_surcharge_percent, FALSE,
    cancellation_fee, 25.00, tax_percentage, 'EXCLUSIVE',
    'INR', 'Delhi NCR', 2.00, is_active, 'PUBLISHED',
    '2026-01-01T00:00:00Z'::timestamptz, NULL, version
FROM vehicle_pricing_configurations vpc
WHERE NOT EXISTS (
    SELECT 1 FROM pricing_profiles pp
    WHERE pp.profile_type = 'VEHICLE_BASED' AND pp.category_key = LOWER(vpc.vehicle_category)
);

-- 2. Backfill SERVICE_BASED profiles for OWN_CAR_DRIVER and CAR_LUGGAGE_DELIVERY
INSERT INTO pricing_profiles (
    profile_type, category_key, vehicle_category, service_type,
    base_fare, per_km_rate, per_minute_rate, per_hour_rate, min_fare,
    waiting_free_minutes, waiting_per_minute_rate,
    night_surcharge_percent, peak_surcharge_percent, stack_peak_and_night,
    cancellation_fee, platform_commission_percent, tax_rate, tax_mode,
    currency, jurisdiction, gateway_fee_percent, is_active, state,
    effective_from, effective_to, business_version
)
SELECT
    'SERVICE_BASED',
    LOWER(service_type),
    NULL,
    service_type,
    base_fare, per_km_rate, 0.00, per_hour_rate, min_fare,
    3, 1.00,
    10.00, 20.00, FALSE,
    cancellation_fee, 25.00, tax_percentage, 'EXCLUSIVE',
    'INR', 'Delhi NCR', 2.00, is_active, 'PUBLISHED',
    '2026-01-01T00:00:00Z'::timestamptz, NULL, 1
FROM pricing_configurations pc
WHERE pc.service_type IN ('OWN_CAR_DRIVER', 'CAR_LUGGAGE_DELIVERY', 'OUTSTATION')
  AND NOT EXISTS (
    SELECT 1 FROM pricing_profiles pp
    WHERE pp.profile_type = 'SERVICE_BASED' AND pp.category_key = LOWER(pc.service_type)
);

-- 3. Backfill initial PUBLISHED versions into pricing_profile_versions
INSERT INTO pricing_profile_versions (
    profile_id, category_key, vehicle_category, service_type,
    base_fare, per_km_rate, per_minute_rate, per_hour_rate, min_fare,
    waiting_free_minutes, waiting_per_minute_rate,
    night_surcharge_percent, peak_surcharge_percent, stack_peak_and_night,
    cancellation_fee, platform_commission_percent, tax_rate, tax_mode,
    currency, jurisdiction, gateway_fee_percent, is_active, state,
    effective_from, effective_to, business_version
)
SELECT
    pp.id, pp.category_key, pp.vehicle_category,
    CASE WHEN pp.profile_type = 'SERVICE_BASED' THEN pp.service_type ELSE NULL END,
    pp.base_fare, pp.per_km_rate, pp.per_minute_rate, pp.per_hour_rate, pp.min_fare,
    pp.waiting_free_minutes, pp.waiting_per_minute_rate,
    pp.night_surcharge_percent, pp.peak_surcharge_percent, pp.stack_peak_and_night,
    pp.cancellation_fee, pp.platform_commission_percent, pp.tax_rate, pp.tax_mode,
    pp.currency, pp.jurisdiction, pp.gateway_fee_percent, pp.is_active, pp.state,
    pp.effective_from, pp.effective_to, pp.business_version
FROM pricing_profiles pp
WHERE NOT EXISTS (
    SELECT 1 FROM pricing_profile_versions pv WHERE pv.profile_id = pp.id
);
