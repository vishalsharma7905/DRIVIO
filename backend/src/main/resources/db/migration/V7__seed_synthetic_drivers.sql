-- DRIVIO Flyway Migration V7: Rich Synthetic Delhi NCR Drivers for Recommendation Engine
-- Covers BIKE, AUTO, HATCHBACK, SEDAN, PREMIUM_SEDAN, SUV and all service types across Delhi NCR.

-- Password for all synthetic demo drivers: password123 (same BCrypt hash as V2)
INSERT INTO users (email, password_hash, full_name, phone_number, role, is_active)
VALUES
    ('arjun.mehta@drivio.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Arjun Mehta',   '+919810112233', 'DRIVER', TRUE),
    ('rohit.sharma@drivio.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Rohit Sharma',  '+919810223344', 'DRIVER', TRUE),
    ('manoj.tiwari@drivio.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Manoj Tiwari',  '+919810334455', 'DRIVER', TRUE),
    ('deepak.verma@drivio.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Deepak Verma',  '+919810445566', 'DRIVER', TRUE),
    ('karan.kapoor@drivio.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Karan Kapoor',  '+919810556677', 'DRIVER', TRUE),
    ('sanjay.gupta@drivio.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Sanjay Gupta',  '+919810667788', 'DRIVER', TRUE)
ON CONFLICT (email) DO NOTHING;

-- Insert driver profiles with realistic Delhi NCR coordinates
INSERT INTO driver_profiles (
    user_id, licence_number, experience_years, languages,
    vehicle_categories, service_preferences, verification_status,
    rating, total_ratings_count, total_trips, total_earnings,
    current_latitude, current_longitude, last_location_update
)
SELECT u.id, 'DL-04-2020-0011', 4, 'Hindi,English', 'BIKE', 'ONE_WAY,ROUND_TRIP', 'VERIFIED', 4.86, 142, 310, 48500.00, 28.6304, 77.2177, CURRENT_TIMESTAMP
FROM users u WHERE u.email = 'arjun.mehta@drivio.com'
  AND NOT EXISTS (SELECT 1 FROM driver_profiles dp WHERE dp.user_id = u.id);

INSERT INTO driver_profiles (
    user_id, licence_number, experience_years, languages,
    vehicle_categories, service_preferences, verification_status,
    rating, total_ratings_count, total_trips, total_earnings,
    current_latitude, current_longitude, last_location_update
)
SELECT u.id, 'DL-02-2019-0042', 6, 'Hindi,English', 'AUTO,BIKE', 'ONE_WAY,ROUND_TRIP,HOURLY', 'VERIFIED', 4.78, 215, 540, 92400.00, 28.6225, 77.2245, CURRENT_TIMESTAMP
FROM users u WHERE u.email = 'rohit.sharma@drivio.com'
  AND NOT EXISTS (SELECT 1 FROM driver_profiles dp WHERE dp.user_id = u.id);

INSERT INTO driver_profiles (
    user_id, licence_number, experience_years, languages,
    vehicle_categories, service_preferences, verification_status,
    rating, total_ratings_count, total_trips, total_earnings,
    current_latitude, current_longitude, last_location_update
)
SELECT u.id, 'UP-16-2017-0088', 9, 'Hindi,English', 'HATCHBACK,SEDAN,AUTO', 'ONE_WAY,ROUND_TRIP,HOURLY,OWN_CAR_DRIVER', 'VERIFIED', 4.91, 320, 780, 215000.00, 28.6279, 77.3649, CURRENT_TIMESTAMP
FROM users u WHERE u.email = 'manoj.tiwari@drivio.com'
  AND NOT EXISTS (SELECT 1 FROM driver_profiles dp WHERE dp.user_id = u.id);

INSERT INTO driver_profiles (
    user_id, licence_number, experience_years, languages,
    vehicle_categories, service_preferences, verification_status,
    rating, total_ratings_count, total_trips, total_earnings,
    current_latitude, current_longitude, last_location_update
)
SELECT u.id, 'HR-26-2015-0199', 11, 'Hindi,English,Punjabi', 'SEDAN,PREMIUM_SEDAN,SUV', 'ONE_WAY,ROUND_TRIP,OUTSTATION,OWN_CAR_DRIVER,CAR_LUGGAGE_DELIVERY', 'VERIFIED', 4.94, 410, 950, 385000.00, 28.4950, 77.0895, CURRENT_TIMESTAMP
FROM users u WHERE u.email = 'deepak.verma@drivio.com'
  AND NOT EXISTS (SELECT 1 FROM driver_profiles dp WHERE dp.user_id = u.id);

INSERT INTO driver_profiles (
    user_id, licence_number, experience_years, languages,
    vehicle_categories, service_preferences, verification_status,
    rating, total_ratings_count, total_trips, total_earnings,
    current_latitude, current_longitude, last_location_update
)
SELECT u.id, 'DL-09-2016-0301', 10, 'Hindi,English', 'PREMIUM_SEDAN,SUV,LUXURY', 'ONE_WAY,ROUND_TRIP,HOURLY,OUTSTATION,OWN_CAR_DRIVER', 'VERIFIED', 4.97, 290, 620, 410000.00, 28.5562, 77.1000, CURRENT_TIMESTAMP
FROM users u WHERE u.email = 'karan.kapoor@drivio.com'
  AND NOT EXISTS (SELECT 1 FROM driver_profiles dp WHERE dp.user_id = u.id);

INSERT INTO driver_profiles (
    user_id, licence_number, experience_years, languages,
    vehicle_categories, service_preferences, verification_status,
    rating, total_ratings_count, total_trips, total_earnings,
    current_latitude, current_longitude, last_location_update
)
SELECT u.id, 'DL-03-2018-0455', 7, 'Hindi,English', 'HATCHBACK,SEDAN,SUV,CAR_LUGGAGE_DELIVERY', 'ONE_WAY,HOURLY,OUTSTATION,CAR_LUGGAGE_DELIVERY', 'VERIFIED', 4.84, 175, 430, 165000.00, 28.5245, 77.2066, CURRENT_TIMESTAMP
FROM users u WHERE u.email = 'sanjay.gupta@drivio.com'
  AND NOT EXISTS (SELECT 1 FROM driver_profiles dp WHERE dp.user_id = u.id);

-- Ensure availability rows exist and are online for all synthetic drivers
INSERT INTO driver_availability (driver_id, is_available, shift_start, shift_end, status_note)
SELECT dp.id, TRUE, '06:00:00', '23:00:00', 'Online in Delhi NCR'
FROM driver_profiles dp
JOIN users u ON u.id = dp.user_id
WHERE u.email IN (
    'arjun.mehta@drivio.com', 'rohit.sharma@drivio.com', 'manoj.tiwari@drivio.com',
    'deepak.verma@drivio.com', 'karan.kapoor@drivio.com', 'sanjay.gupta@drivio.com'
)
ON CONFLICT (driver_id) DO UPDATE SET is_available = TRUE, status_note = 'Online in Delhi NCR';

-- Also bring Rajesh Kumar & Suresh Yadav online and expand Vikram Patel's categories
UPDATE driver_availability SET is_available = TRUE, status_note = 'Online in Delhi NCR'
WHERE driver_id IN (SELECT id FROM driver_profiles);

UPDATE driver_profiles
SET vehicle_categories = 'SEDAN,PREMIUM_SEDAN,SUV,LUXURY',
    service_preferences = 'OWN_CAR_DRIVER,ONE_WAY,ROUND_TRIP,HOURLY,OUTSTATION,CAR_LUGGAGE_DELIVERY'
WHERE user_id = (SELECT id FROM users WHERE email = 'vikram.patel@drivio.com');
