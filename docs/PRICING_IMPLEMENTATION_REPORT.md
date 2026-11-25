# DRIVIO — Unified Pricing & Financial Architecture Implementation Report

**Date:** 2026-11-25  
**Source of Truth:** `docs/PRICING_FINANCIAL_FINAL_SPEC.md` & `docs/PRICING_IMPLEMENTATION_AUDIT.md`

---

## A. Executive Summary

The unified pricing, immutable fare snapshot, append-only financial ledger, idempotent payment/webhook/refund pipeline, four-balance driver earnings ledger, and settlement lifecycle have been implemented and verified against the DRIVIO Spring Boot + PostgreSQL 16 + React/TypeScript codebase.

**Completed:**
- Fixed Phase 1 compilation blockers (`BookingService.java` missing closing brace & imports, `PricingService.resolvePricingVersionDynamic` signature, `PaymentService` entity field references, `DriverSettlement.java` entity, `CreateBookingRequest`/`BookingResponseDto` fields).
- Restored driver auto-assignment (`RecommendationService`), `requestedDriverId` validation, and `VehicleRepository` matching in `BookingService.createBooking()`.
- Fixed PostgreSQL DDL syntax issues in Flyway `V5__new_tables_and_additive_columns.sql` (`EXCLUDE USING gist` with `tstzrange`, `accrued_at` index, `RESERVED` earnings status, future-scheduled version support) and added `V6__controlled_backfill.sql`.
- Implemented unified `PricingProfileService` + `AdminPricingProfileController` (DRAFT/PUBLISHED/RETIRED versioning, future scheduling, overlap prevention, audit log V2).
- Implemented idempotent Razorpay webhook capture (`POST /api/payments/webhook/razorpay`) and proportional full/partial/failed refunds (`POST /api/payments/{bookingId}/refund`).
- Implemented `DriverEarningsService` (4 distinct balances: `ACCRUED`, `AVAILABLE`, `RESERVED`, `SETTLED`), `SettlementService` (partial settlements, failed settlement release, safe retries), and `DriverEarningsController`.
- Integrated the 4-balance wallet & settlement withdrawal UI into `DriverDashboard.tsx` and API bindings in `frontend/src/services/api.ts`.

---

## B. Files Changed

### Database Migrations
- `backend/src/main/resources/db/migration/V5__new_tables_and_additive_columns.sql` (corrected PostgreSQL DDL constraints & indexes)
- `backend/src/main/resources/db/migration/V6__controlled_backfill.sql` (new)

### Entities & Repositories
- `backend/src/main/java/com/drivio/entity/Booking.java` (`@JdbcTypeCode(SqlTypes.JSON)` on `fareSnapshot`)
- `backend/src/main/java/com/drivio/entity/DriverSettlement.java` (new)
- `backend/src/main/java/com/drivio/entity/DriverSettlementAttempt.java` (`gatewayReference`)
- `backend/src/main/java/com/drivio/entity/PricingAuditLogV2.java` (new)
- `backend/src/main/java/com/drivio/repository/PricingAuditLogV2Repository.java` (new)

### Services & Controllers
- `backend/src/main/java/com/drivio/service/BookingService.java`
- `backend/src/main/java/com/drivio/service/PricingService.java`
- `backend/src/main/java/com/drivio/service/PricingProfileService.java` (new)
- `backend/src/main/java/com/drivio/service/PaymentService.java`
- `backend/src/main/java/com/drivio/service/DriverEarningsService.java` (new)
- `backend/src/main/java/com/drivio/service/SettlementService.java` (new)
- `backend/src/main/java/com/drivio/controller/AdminPricingProfileController.java` (new)
- `backend/src/main/java/com/drivio/controller/DriverEarningsController.java` (new)
- `backend/src/main/java/com/drivio/controller/PaymentController.java`

### DTOs & Security
- `backend/src/main/java/com/drivio/dto/CreateBookingRequest.java`
- `backend/src/main/java/com/drivio/dto/BookingResponseDto.java`
- `backend/src/main/java/com/drivio/dto/FareEstimateResponse.java`
- `backend/src/main/java/com/drivio/security/SecurityConfig.java`

### Frontend
- `frontend/src/types/index.ts`
- `frontend/src/services/api.ts`
- `frontend/src/pages/driver/DriverDashboard.tsx`

### Tests & Documentation
- `backend/src/test/java/com/drivio/PricingFinancialArchitectureTest.java` (new — 17-scenario suite)
- `docs/PRICING_IMPLEMENTATION_REPORT.md` (this report)

---

## C. Pricing Architecture

- **Service-Family Isolation:** `ONE_WAY`, `ROUND_TRIP`, `HOURLY` resolve `VEHICLE_BASED` pricing (`BIKE`, `AUTO`, `HATCHBACK`, `SEDAN`, `PREMIUM_SEDAN`, `SUV`). `OWN_CAR_DRIVER`, `CAR_LUGGAGE_DELIVERY`, and `OUTSTATION` resolve `SERVICE_BASED` pricing. Missing or inactive configurations throw `BadRequestException` / `ResourceNotFoundException`.
- **Versioning & Scheduling:** `PricingProfileVersion` supports `DRAFT`, `PUBLISHED`, and `RETIRED` states. Future versions (`effectiveFrom > now()`) remain inactive until `effectiveFrom`. Published versions cannot be edited; updates require creating a new `DRAFT` version.
- **Snapshot Immutability:** `BookingService.createBooking()` writes `pricingProfileVersionId` and `fareSnapshot` (`JSONB`) at creation; PostgreSQL trigger `booking_fare_immutability_trigger` blocks post-insert mutations to fare columns.

---

## D. Financial Architecture

- **Append-Only Ledger (`financial_transactions`):** Records `PAYMENT_INITIATED`, `PAYMENT_SUCCESS`, `PAYMENT_CAPTURED`, `FULL_REFUND`, `PARTIAL_REFUND`, and `REFUND_FAILED` with unique `idempotency_key` and `gateway_event_id`.
- **Proportional Refunds:** Refunds allocate across `taxReversal`, `driverPayoutReversal`, and `commissionReversal` (residual absorbed into platform commission so `tax + driver + commission == refundAmount`). Failed refunds record `REFUND_FAILED` without altering payment status.
- **Four Driver Balances (`driver_earnings`):**
  - `ACCRUED`: Trip completed, awaiting payment capture.
  - `AVAILABLE`: Payment captured, eligible for settlement.
  - `RESERVED`: Allocated to a `PENDING` settlement (supports exact partial splits).
  - `SETTLED`: Transferred via `COMPLETED` settlement.
- **Settlement Retries:** Failed settlements transition `RESERVED` back to `AVAILABLE`; `retrySettlement()` prevents retrying `COMPLETED` settlements and increments `attempt_number`.

---

## E. API Documentation

| Method & Path | Authorization | Description |
|---|---|---|
| `GET/POST /api/admin/pricing/profiles` | `ROLE_ADMIN` | List/create unified pricing profiles |
| `GET /api/admin/pricing/versions` | `ROLE_ADMIN` | List versions (`?filter=active\|upcoming\|expired`) |
| `POST /api/admin/pricing/profiles/{id}/versions` | `ROLE_ADMIN` | Create `DRAFT` version |
| `PUT /api/admin/pricing/versions/{id}` | `ROLE_ADMIN` | Edit `DRAFT` version |
| `POST /api/admin/pricing/versions/{id}/publish` | `ROLE_ADMIN` | Publish or schedule version |
| `POST /api/admin/pricing/versions/{id}/retire` | `ROLE_ADMIN` | Retire version |
| `GET /api/admin/pricing/audit-logs` | `ROLE_ADMIN` | View V2 audit trail |
| `POST /api/payments/webhook/razorpay` | Public (signature-verified) | Idempotent Razorpay webhook capture |
| `POST /api/payments/{bookingId}/refund` | `ROLE_ADMIN` | Process full/partial refund |
| `GET /api/drivers/earnings/summary` | `ROLE_DRIVER` / `ROLE_ADMIN` | Four balances + ledger history |
| `GET/POST /api/drivers/earnings/settlements` | `ROLE_DRIVER` / `ROLE_ADMIN` | List/request settlements |
| `POST /api/drivers/earnings/settlements/{id}/complete\|fail\|retry` | `ADMIN` (`retry`: `DRIVER`/`ADMIN`) | Settlement lifecycle transitions |

---

## F. Database Changes

- **V5 (`V5__new_tables_and_additive_columns.sql`):** `pricing_profiles`, `pricing_profile_versions` (with `btree_gist` exclusion constraint), `pricing_audit_logs_v2`, `financial_transactions`, `driver_earnings` (with `uq_de_booking_accrual` unique partial index), `driver_settlements`, `driver_settlement_items`, `driver_settlement_attempts`, `bookings.fare_snapshot` (`JSONB`), `bookings.pricing_profile_version_id`, and `booking_fare_immutability_trigger`.
- **V6 (`V6__controlled_backfill.sql`):** `driver_settlement_attempts.gateway_reference` and controlled backfill of `pricing_profiles` + `pricing_profile_versions` from `vehicle_pricing_configurations` and `pricing_configurations`.

---

## G. Test Results

| Verification | Result | Evidence |
|---|---|---|
| Backend compilation (`mvn test-compile`) | **PASS** | 149 main classes + 7 test classes compiled with 0 errors |
| Backend full suite (`mvn clean test`) | **PASS** | **27 / 27 tests passed**, 0 failures, 0 errors, 0 skipped |
| Pricing & Financial 17-scenario suite | **PASS** | `PricingFinancialArchitectureTest`: 5/5 test methods covering scenarios 1–17 |
| Normal Ride & Boundary tests | **PASS** | `NormalRidePricingTest` (8/8) + `PricingBoundaryTest` (4/4) + `PricingServiceTest` (2/2) |
| PostgreSQL Flyway V1–V6 migrations | **PASS** | Executed against PostgreSQL 16.6; Hibernate `ddl-auto: validate` passed |
| Frontend production build (`npm run build`) | **PASS** | `tsc -b && vite build` succeeded with 0 errors in 588ms |
| Existing functionality E2E regression | **PASS** | `comprehensive_e2e_test.mjs`: 18/18 steps passed |

---

## H. Remaining Limitations

- **Live External Razorpay Payouts / Webhooks:** Verified using `RazorpayPaymentGateway` sandbox/HMAC signature mode in local tests; live production Razorpay webhook & Payouts X calls require `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` environment variables.
- **Advisory Lock on H2:** Integration tests run against H2 (`@ActiveProfiles("h2")`), while PostgreSQL `btree_gist` exclusion constraints, `JSONB`, and `booking_fare_immutability_trigger` were verified directly against PostgreSQL 16.6 during Flyway migration execution.
