-- DRIVIO Flyway Migration V2: Seed Data
-- Default password for all seeded users: password123
-- BCrypt: $2a$10$30u9p7kf9L53LCFCK3m4vugNvWLb.C6SkSSGUlc58GWMH/aqljaay

-- 1. Insert Pricing Configurations
INSERT INTO pricing_configurations (service_type, base_fare, per_km_rate, per_hour_rate, min_fare, cancellation_fee, tax_percentage, is_active)
VALUES
('OWN_CAR_DRIVER', 150.00, 12.00, 60.00, 250.00, 50.00, 5.00, TRUE),
('ONE_WAY', 200.00, 14.00, 70.00, 300.00, 60.00, 5.00, TRUE),
('ROUND_TRIP', 350.00, 11.00, 50.00, 500.00, 80.00, 5.00, TRUE),
('HOURLY', 250.00, 8.00, 120.00, 400.00, 75.00, 5.00, TRUE),
('OUTSTATION', 800.00, 13.00, 40.00, 1200.00, 150.00, 5.00, TRUE)
ON CONFLICT (service_type) DO NOTHING;

-- 2. Insert Admin User
INSERT INTO users (id, email, password_hash, full_name, phone_number, role, is_active)
VALUES
(1, 'admin@drivio.com', '$2a$10$30u9p7kf9L53LCFCK3m4vugNvWLb.C6SkSSGUlc58GWMH/aqljaay', 'System Administrator', '+919876543210', 'ADMIN', TRUE)
ON CONFLICT (email) DO NOTHING;

-- 3. Insert Customers
INSERT INTO users (id, email, password_hash, full_name, phone_number, role, is_active)
VALUES
(2, 'rahul.verma@example.com', '$2a$10$30u9p7kf9L53LCFCK3m4vugNvWLb.C6SkSSGUlc58GWMH/aqljaay', 'Rahul Verma', '+919812345678', 'CUSTOMER', TRUE),
(3, 'priya.sharma@example.com', '$2a$10$30u9p7kf9L53LCFCK3m4vugNvWLb.C6SkSSGUlc58GWMH/aqljaay', 'Priya Sharma', '+919823456789', 'CUSTOMER', TRUE)
ON CONFLICT (email) DO NOTHING;

INSERT INTO customer_profiles (user_id, preferred_language, emergency_contact, home_address)
VALUES
(2, 'English', '+919811122233', 'A-42, Connaught Place, New Delhi'),
(3, 'Hindi', '+919844455566', 'Flat 304, Green Park, South Delhi')
ON CONFLICT (user_id) DO NOTHING;

-- 4. Insert Drivers
INSERT INTO users (id, email, password_hash, full_name, phone_number, role, is_active)
VALUES
(4, 'rajesh.kumar@drivio.com', '$2a$10$30u9p7kf9L53LCFCK3m4vugNvWLb.C6SkSSGUlc58GWMH/aqljaay', 'Rajesh Kumar', '+919891001122', 'DRIVER', TRUE),
(5, 'amit.singh@drivio.com', '$2a$10$30u9p7kf9L53LCFCK3m4vugNvWLb.C6SkSSGUlc58GWMH/aqljaay', 'Amit Singh', '+919892002233', 'DRIVER', TRUE),
(6, 'vikram.patel@drivio.com', '$2a$10$30u9p7kf9L53LCFCK3m4vugNvWLb.C6SkSSGUlc58GWMH/aqljaay', 'Vikram Patel', '+919893003344', 'DRIVER', TRUE),
(7, 'suresh.yadav@drivio.com', '$2a$10$30u9p7kf9L53LCFCK3m4vugNvWLb.C6SkSSGUlc58GWMH/aqljaay', 'Suresh Yadav', '+919894004455', 'DRIVER', TRUE)
ON CONFLICT (email) DO NOTHING;

-- Driver Profiles:
-- Rajesh: 8 yrs exp, 4.9 rating, verified, available, near Connaught Place (28.6280, 77.2180)
INSERT INTO driver_profiles (id, user_id, profile_photo_url, licence_number, licence_expiry_date, experience_years, languages, vehicle_categories, service_preferences, verification_status, rating, total_ratings_count, total_trips, total_earnings, current_latitude, current_longitude, last_location_update)
VALUES
(1, 4, 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150', 'DL-0420110023451', '2030-05-15', 8, 'English,Hindi,Punjabi', 'HATCHBACK,SEDAN,SUV,LUXURY', 'OWN_CAR_DRIVER,ONE_WAY,ROUND_TRIP,HOURLY,OUTSTATION', 'VERIFIED', 4.90, 48, 112, 34500.00, 28.6280, 77.2180, CURRENT_TIMESTAMP),
(2, 5, 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150', 'DL-0720160087612', '2032-11-20', 5, 'Hindi,English', 'HATCHBACK,SEDAN,SUV', 'OWN_CAR_DRIVER,ONE_WAY,ROUND_TRIP,HOURLY', 'VERIFIED', 4.82, 32, 74, 21800.00, 28.6139, 77.2090, CURRENT_TIMESTAMP),
(3, 6, 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150', 'DL-1020080011993', '2028-09-10', 12, 'English,Hindi,Gujarati', 'SEDAN,SUV,LUXURY', 'OWN_CAR_DRIVER,ROUND_TRIP,HOURLY,OUTSTATION', 'VERIFIED', 4.95, 86, 215, 68900.00, 28.6350, 77.2250, CURRENT_TIMESTAMP),
(4, 7, 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=150', 'DL-1220200055441', '2035-01-25', 3, 'Hindi', 'HATCHBACK,SEDAN', 'OWN_CAR_DRIVER,ONE_WAY', 'PENDING', 5.00, 0, 0, 0.00, 28.5800, 77.2300, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Driver Availability
INSERT INTO driver_availability (driver_id, is_available, shift_start, shift_end, status_note)
VALUES
(1, TRUE, '08:00:00', '22:00:00', 'Active and ready for bookings in Central Delhi'),
(2, TRUE, '09:00:00', '20:00:00', 'On duty around India Gate'),
(3, TRUE, '07:00:00', '21:00:00', 'Available for luxury and long-distance trips'),
(4, FALSE, '10:00:00', '18:00:00', 'Pending admin KYC verification')
ON CONFLICT (driver_id) DO NOTHING;

-- Driver Documents
INSERT INTO driver_documents (driver_id, document_type, document_url, verification_status, verified_by_admin_id, remarks)
VALUES
(1, 'DRIVING_LICENCE', 'https://example.com/docs/dl_rajesh.pdf', 'VERIFIED', 1, 'Valid commercial & transport endorsement verified.'),
(2, 'DRIVING_LICENCE', 'https://example.com/docs/dl_amit.pdf', 'VERIFIED', 1, 'Verified with transport authority.'),
(3, 'DRIVING_LICENCE', 'https://example.com/docs/dl_vikram.pdf', 'VERIFIED', 1, 'Flawless background check and valid luxury license.'),
(4, 'DRIVING_LICENCE', 'https://example.com/docs/dl_suresh.pdf', 'PENDING', NULL, 'Submitted awaiting administrator review.')
ON CONFLICT DO NOTHING;

-- Update sequences to prevent primary key collision
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('driver_profiles_id_seq', (SELECT MAX(id) FROM driver_profiles));
SELECT setval('pricing_configurations_id_seq', (SELECT MAX(id) FROM pricing_configurations));
