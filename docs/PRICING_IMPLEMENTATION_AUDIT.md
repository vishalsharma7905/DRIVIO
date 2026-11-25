# DRIVIO Pricing & Financial Implementation Audit Report

## Phase 1: Audit & Correction Analysis

**Generated:** 2026-11-25  
**Specification Reference:** docs/PRICING_FINANCIAL_FINAL_SPEC.md  
**Scope:** PricingService, BookingService, PaymentService, Booking entity, DriverProfile

---

## 1. Executive Summary

This audit documents the gap between the current DRIVIO codebase and the unified pricing/financial architecture specified in `docs/PRICING_FINANCIAL_FINAL_SPEC.md`. The audit identifies specific code corrections, entity modifications, and migration requirements for Phases 2-4 implementation. All changes are additive or corrective — no V1-V4 migrations are modified, and legacy tables remain read-only.

**Key Finding:** The current codebase operates two separate pricing engines (legacy `pricing_configurations` + `vehicle_pricing_configurations`) without unified versioning, the PaymentService writes only to the `payments` table (missing the append-only `financial_transactions` ledger), and the Booking entity lacks the `fare_snapshot JSONB` column and `pricing_profile_version_id` needed for immutability.

---

## 2. PricingService Analysis

### Current State (backend/src/main/java/com/drivio/service/PricingService.java)

**`calculateEstimatedFare` (lines 106-139):**
- Dispatches to vehicle-based or service-based calculation
- **Bug:** Line 126-127: `isPersonalOrDelivery = OWN_CAR_DRIVER or CAR_LUGGAGE_DELIVERY` — vehicle-based pricing should only apply when `vehiclePricingConfigRepository != null` **AND** the booking is not a personal/delivery service type
- Current logic tries vehicle config first if available, then falls back to service-based. This is inverted from the spec: vehicle-based pricing must be gated behind both config availability AND service family compatibility.

**`calculateNormalRideVehicleFare` (lines 141-220):**
- 14-step fare calculation for vehicle categories ✓ (matches spec order)
- All BigDecimal with `setScale(2, HALF_UP)` ✓
- Line 217: `response.setPricingVersion(config.getVersion())` ✓
- Missing: `fare_snapshot` JSONB writing and `pricing_profile_version_id` assignment

**`calculateServiceBasedFare` (lines 222-275):**
- Service-based calculation with vehicle multipliers ✓
- **Bug:** Line 272: `response.setPricingVersion(1)` — hardcoded version 1 since service configs don't have versions. Per spec, service-based bookings should resolve to a `pricing_profile_version_id` from the profile version table, not a magic constant.
- OUTSTATION adds ₹100 additional charge ✓
- MinFare check ✓
- Tax on subtotal ✓

### Required Corrections

| Area | Current | Required | Priority |
|---|---|---|---|
| `calculateEstimatedFare` dispatch | Try vehicle first, then service | Vehicle-based only if `vehiclePricingConfigRepository != null` AND NOT personal/delivery service | Critical |
| `calculateServiceBasedFare` version | Hardcoded `1` | Resolve from `pricing_profile_versions` table | High |
| Fare snapshot writing | Not implemented | Write `fare_snapshot JSONB` + set `pricing_profile_version_id` | High |
| Audit log integration | Not implemented | Record pricing version changes in `pricing_audit_logs_v2` | Medium |

---

## 3. BookingService Analysis

### Current State (backend/src/main/java/com/drivio/service/BookingService.java)

**`createBooking` (lines 57-185):**
- Calls `pricingService.calculateEstimatedFare(fareRequest)` at line 68 ✓
- Sets fare details at lines 104-110: `booking.setBaseFare()`, `booking.setDistanceFare()`, `booking.setDurationFare()`, `booking.setAdditionalCharges()`, `booking.setTaxAmount()`, `booking.setTotalFare()`, `booking.setPricingVersion()` ✓
- **Bug:** Creates Payment record at lines 159-161: `new Payment(savedBooking, customer, savedBooking.getTotalFare(), PaymentMethod.CASH)` with taxAmount set — but does NOT write to `financial_transactions` ledger per spec
- Missing: Writing `fare_snapshot JSONB` column at booking creation
- Missing: Setting `pricing_profile_version_id` on the booking

**`completeTrip` (lines 319-380):**
- Line 349: `driverShare = booking.getTotalFare().multiply(new BigDecimal("0.75"));` — 75/25 split ✓ (matches spec commission baseline)
- Lines 350-351: `driver.setTotalTrips(driver.getTotalTrips() + 1)`, `driver.setTotalEarnings(driver.getTotalEarnings().add(driverShare))` — **Bug:** Updates `DriverProfile.total_earnings` but does NOT write to the new `driver_earnings` append-only ledger
- Missing: Driver earnings ledger entry with ACCRUED status
- Lines 371-377: Notifications to customer and driver with driverShare amount ✓

**`mapToDto` (lines 490-561):**
- Includes all fare components ✓ (baseFare, distanceFare, durationFare, additionalCharges, taxAmount, totalFare)
- pricingVersion at line 555 ✓
- Payment status at lines 530-535 ✓

### Required Corrections

| Area | Current | Required | Priority |
|---|---|---|---|
| `createBooking` financial ledger | Payment only | Also write `financial_transactions` entry (PAYMENT_INITIATED) | Critical |
| `createBooking` fare snapshot | Not written | Write `fare_snapshot JSONB` with all fare components | Critical |
| `createBooking` profile version | Not set | Set `pricing_profile_version_id` from pricing engine | High |
| `completeTrip` driver ledger | Updates `total_earnings` only | Also append `driver_earnings` row with ACCRUED status | Critical |
| `completeTrip` earnings status | Implicit 75/25 | Explicit ACCRUED → ADJUSTED → SETTLED lifecycle | Medium |

---

## 4. PaymentService Analysis

### Current State (backend/src/main/java/com/drivio/service/PaymentService.java)

**`processPayment` (lines 51-92):**
- Sets `paymentStatus=PAID`, `amount=booking.getTotalFare()`, `taxAmount=booking.getTaxAmount()` ✓
- Creates `transactionId` and `receiptNumber` ✓
- **Bug:** Currently only writes to `payments` table — **does NOT write to the new `financial_transactions` ledger** per spec
- `createRazorpayOrder` (lines 137-150): Creates Razorpay order, sets `gatewayOrderId` on payment ✓
- `verifyRazorpayPayment` (lines 152-186): Verifies Razorpay signature, sets payment status and gateway fields ✓

### Required Corrections

| Area | Current | Required | Priority |
|---|---|---|---|
| `processPayment` ledger | Payments table only | Also write `financial_transactions` entry (PAYMENT_INITIATED → PAYMENT_SUCCESS → PAYMENT_CAPTURED) | Critical |
| Idempotency key | Not handled | Generate and store `idempotency_key`; enforce UNIQUE constraint | High |
| Gateway event integration | Partial | Store `gateway_event_id`, `gateway_payment_id`, `gateway_signature`; support `reference_transaction_id` chain | High |
| Refund ledger | Not implemented | Support PARTIAL_REFUND / FULL_REFUND entries with proportional allocation | Medium |

---

## 5. Booking Entity Analysis

### Current State (backend/src/main/java/com/drivio/entity/Booking.java)

**Fare columns (lines 79-95):**
- `base_fare`, `distance_fare`, `duration_fare`, `additional_charges`, `tax_amount`, `total_fare` all `BigDecimal(10,2) DEFAULT 0.00` ✓

**`pricing_version` (lines 144-145):**
- `Integer DEFAULT 1` ✓

**Missing columns per spec:**
- ❌ `fare_snapshot JSONB` — immutable structured snapshot of all fare components at booking creation
- ❌ `pricing_profile_version_id` — foreign key to `pricing_profile_versions` table, tracks which profile version generated this fare

**`@PreUpdate` (lines 164-167):**
- Sets `updatedAt` ✓

**Immutability:**
- ❌ No DB trigger rejecting updates to fare columns post-insert
- ❌ No app-layer restriction (create-only endpoints should be enforced)

### Required Additions

| Column | Type | Purpose |
|---|---|---|
| `fare_snapshot JSONB` | JSONB | Immutable snapshot of all fare components at booking creation; used for historical audit, never recomputed |
| `pricing_profile_version_id BIGINT` | BIGINT | Links booking to the `pricing_profile_version` that generated its fare; NULL = legacy booking using stored columns (never recomputed) |

**Required DB Trigger:**
```sql
CREATE OR REPLACE FUNCTION reject_fare_updates()
RETURNS TRIGGER AS $$ BEGIN
  IF NEW.base_fare <> OLD.base_fare OR NEW.distance_fare <> OLD.distance_fare OR NEW.duration_fare <> OLD.duration_fare OR NEW.total_fare <> OLD.total_fare THEN
    RAISE EXCEPTION 'Booking fare components are immutable post-insert. Use fare_snapshot JSONB for historical changes.';
  END IF;
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER booking_fare_immutability_trigger
BEFORE UPDATE ON bookings
FOR EACH ROW EXECUTE FUNCTION reject_fare_updates();
```

---

## 6. DriverProfile Analysis

### Current State

**`total_earnings` (from V1 migration line 42):**
- `DECIMAL(12, 2) DEFAULT 0.00` ✓

**Current earnings model (from audit):**
- In `completeTrip()`: `driverShare = totalFare.multiply(new BigDecimal("0.75"))`, `driver.setTotalEarnings(driver.getTotalEarnings().add(driverShare))`
- This couples `DriverProfile.total_earnings` with the earnings accrual

### Required Changes Per Spec

| Change | Current | Required | Priority |
|---|---|---|---|
| Earnings status tracking | Implicit via `total_earnings` | Explicit ACCRUED/ADJUSTED/REVERSED/SETTLED statuses on `driver_earnings` ledger | Critical |
| Ledger decoupling | `total_earnings` updated directly | `DriverProfile.total_earnings` should be a **read-only summary** derived from `driver_earnings` ledger WHERE status='SETTLED' | Critical |
| New ledger entries | Not written | On trip complete: insert `driver_earnings` row with ACCRUED status, amount = 75% of totalFare | Critical |
| Settlement tracking | Not implemented | `driver_settlements` + `driver_settlement_items` + `driver_settlement_attempts` for partial/failed settlements | High |

**Decoupling Strategy:**
- `DriverProfile.total_earnings` = SUM(driver_earnings.amount) WHERE status = 'SETTLED' (materialized view or DAO query)
- On trip complete: insert `driver_earnings` with ACCRUED status, NOT update `DriverProfile.total_earnings` directly
- Settlement service moves ACCRUED → SETTLED and updates `DriverProfile.total_earnings` accordingly

---

## 7. Database Migration Requirements

### V5 Migration (new tables + additive booking columns)

**New Tables:**
1. `pricing_profiles` — service family + category key, active flag, metadata
2. `pricing_profile_versions` — generated `category_key = COALESCE(vehicle_category,'__ALL__')`, effective timestamps, `business_version`, state (DRAFT/PUBLISHED/RETIRED), exclusion constraint (btree_gist) preventing overlap
3. `pricing_audit_logs_v2` — audit trail for profile version changes (replaces/augments `VehiclePricingConfiguration` audit logs)
4. `financial_transactions` — append-only ledger (see §7 of spec): events PAYMENT_INITIATED, PAYMENT_SUCCESS, PAYMENT_CAPTURED, PAYMENT_FAILED, FULL_REFUND, PARTIAL_REFUND, REFUND_FAILED, PAYMENT_REVERSAL, COMMISSION_ADJUSTMENT, DRIVER_PAYOUT_ADJUSTMENT, GATEWAY_RECONCILIATION, CANCELLATION_FEE; signed amounts; UNIQUE idempotency_key; UNIQUE gateway_event_id; reference_transaction_id ON DELETE RESTRICT
5. `driver_earnings` — append-only earnings ledger: ACCRUED/ADJUSTED/REVERSED/SETTLED statuses; booking_id FK; amount; settled_at; attempt tracking
6. `driver_settlements` — settlement header: driver_id, total_amount, status, completed_at
7. `driver_settlement_items` — individual earning items per booking linking to driver_earnings
8. `driver_settlement_attempts` — attempt tracking for failed settlements: attempt number, status, error_message, tried_at

**Additive Booking Columns:**
- `fare_snapshot JSONB` — immutable fare snapshot (see entity changes above)
- `pricing_profile_version_id BIGINT NULL` — FK to pricing_profile_versions; NULL = legacy booking

**Indexing:**
- `pricing_profile_versions`: btree_gist on `(effective_from, effective_to)` for overlap exclusion
- `financial_transactions`: composite index on `(booking_id, event_type, created_at)`
- `driver_earnings`: index on `(driver_id, status, created_at)`
- `pricing_profiles`: index on `(profile_type, is_active)`

**V5 does NOT modify V1-V4 tables.** V5 creates new tables only. V6 (controlled backfill) links unambiguous legacy bookings to profile versions; leaves NULL otherwise.

### V6 Migration (controlled backfill)
- Link unambiguous bookings (those with service_type/vehicle_type combos matching profile versions)
- Leave NULL for ambiguous/legacy bookings
- Never recompute historical booking fares

---

## 8. Key Findings from Comparison

### 8.1 Pricing Engine Unification
- **Current:** Two separate engines (`pricing_configurations` for service-based, `vehicle_pricing_configurations` for vehicle-based) with no versioning or cross-referencing.
- **Spec:** Unified `pricing_profiles` with versioning, DRAFT/PUBLISHED/RETIRED states, effective-dating, resolved by `(service family, vehicle category, timestamp)`.
- **Correction:** Dispatch logic in `PricingService.calculateEstimatedFare` must gate vehicle-based pricing behind both config availability AND service family exclusion. Service-based pricing must resolve version from `pricing_profile_versions` table, not hardcode `1`.

### 8.2 Financial Ledger Gap
- **Current:** `PaymentService` writes only to `payments` table. No `financial_transactions` ledger exists.
- **Spec:** Append-only `financial_transactions` with 16 event types, idempotency keys, gateway event IDs, proportional refund allocation.
- **Correction:** `processPayment` must write to both `payments` and `financial_transactions`. Idempotency key generation and enforcement must be added.

### 8.3 Booking Immutability
- **Current:** Booking fare columns updated via application; no DB trigger; no `fare_snapshot` JSONB.
- **Spec:** `fare_snapshot JSONB` + DB trigger rejecting fare column updates + `pricing_profile_version_id` for version tracking.
- **Correction:** Add `fare_snapshot` column, `pricing_profile_version_id` column, and immutability trigger on `bookings` table.

### 8.4 Driver Earnings Decoupling
- **Current:** `DriverProfile.total_earnings` updated directly in `completeTrip()` via 75/25 split.
- **Spec:** Separate `driver_earnings` append-only ledger with ACCRUED/ADJUSTED/REVERSED/SETTLED statuses; `DriverProfile.total_earnings` is a derived summary from settled earnings only.
- **Correction:** Decouple `total_earnings` from direct updates; insert `driver_earnings` rows on trip complete; settlement service moves status forward.

### 8.5 Version Hardcoding
- **Current:** `calculateServiceBasedFare` line 272: `response.setPricingVersion(1)` — magic constant.
- **Spec:** Every fare estimate must carry the `pricing_profile_version_id` that generated it; version 1 is not a valid version in the new schema.
- **Correction:** Resolve version from the active profile version at calculation time; store on booking and response DTO.

---

## 9. Implementation Priority Order

1. **Critical — Phase 2 Foundation:**
   - Fix `PricingService.calculateEstimatedFare` dispatch logic (vehicle gating)
   - Add `fare_snapshot JSONB` and `pricing_profile_version_id` to Booking entity
   - Add DB trigger for fare immutability
   - `PaymentService.processPayment` writes to `financial_transactions` ledger
   - Add idempotency key handling in PaymentService

2. **Critical — Phase 2 Continuation:**
   - `BookingService.createBooking` writes `fare_snapshot` on creation
   - `BookingService.completeTrip` appends `driver_earnings` ACCRUED entry (not update `total_earnings`)
   - `DriverProfile` changes: `total_earnings` becomes derived summary

3. **High — Phase 3:**
   - Create V5 migration: all new tables (`pricing_profiles`, `pricing_profile_versions`, `financial_transactions`, `driver_earnings`, `driver_settlements`, `driver_settlement_items`, `driver_settlement_attempts`, `pricing_audit_logs_v2`)
   - Additive booking columns (`fare_snapshot`, `pricing_profile_version_id`)
   - btree_gist exclusion constraint on `pricing_profile_versions`

4. **Medium — Phase 3.1+:**
   - V6 controlled backfill from legacy configs
   - SettlementService: ACCRUED → ADJUSTED → SETTLED lifecycle
   - Partial refund proportional allocation with residual policy
   - Tax jurisdiction configuration and taxable components

---

## 10. Security & Constraints Compliance

- ✅ No real secrets exposure: JWT secret, OAuth credentials remain in `application.yml` env vars
- ✅ No destructive Git commands: All changes are additive/new files; no V1-V4 modifications
- ✅ No automatic commits: This report documents findings; implementation requires explicit approval
- ✅ BigDecimal only for money: All calculations already use BigDecimal with `setScale(2, HALF_UP)` ✓
- ✅ No GST compliance claims: Tax is `taxRate`/`taxMode` configurable; no GST-specific assertions
- ✅ Backend authoritative for fares: Frontend `CreateBookingPage` only displays estimates from `/api/pricing/estimate`
- ✅ Preserve historical records: `fare_snapshot JSONB` is immutable; legacy bookings with `pricing_profile_version_id IS NULL` use stored columns; never recomputed

---

## 11. Files to Create/Modify

### New Files:
- `docs/PRICING_IMPLEMENTATION_AUDIT.md` — this report (Phase 1 deliverable)
- V5 migration SQL: `backend/src/main/resources/db/migration/V5__new_tables_and_additive_columns.sql`
- V6 migration SQL: `backend/src/main/resources/db/migration/V6__controlled_backfill.sql`

### Modified Files:
- `backend/src/main/java/com/drivio/service/PricingService.java` — fix dispatch logic, version handling, audit logging
- `backend/src/main/java/com/drivio/service/BookingService.java` — fare_snapshot writing, driver earnings ledger, financial_transactions integration
- `backend/src/main/java/com/drivio/service/PaymentService.java` — financial_transactions ledger writing, idempotency keys, gateway event integration
- `backend/src/main/java/com/drivio/entity/Booking.java` — add `fare_snapshot JSONB`, `pricing_profile_version_id BIGINT`, immutability considerations
- `backend/src/main/java/com/drivio/entity/DriverProfile.java` — decouple `total_earnings` from direct updates; add documentation
- `backend/src/main/resources/application.yml` — may need profile-specific Flyway settings verification

### Frontend (no functional changes required, UI consistent):
- `frontend/src/services/api.ts` — already consumes unified estimate format; no changes needed
- `frontend/src/pages/customer/CreateBookingPage.tsx` — fare breakdown display consistent with new components
- `frontend/src/pages/admin/AdminPricingPage.tsx` — audit trail already present; version endpoints to be added

---

## 12. Verification Checklist

- [ ] `PricingService.calculateEstimatedFare` dispatches vehicle-based only when `vehiclePricingConfigRepository != null` AND service is NOT personal/delivery
- [ ] `PricingService.calculateServiceBasedFare` resolves version from `pricing_profile_versions` (not hardcoded 1)
- [ ] Booking entity has `fare_snapshot JSONB` column
- [ ] Booking entity has `pricing_profile_version_id BIGINT` column (FK to pricing_profile_versions)
- [ ] DB trigger rejects updates to fare columns on `bookings` table
- [ ] `PaymentService.processPayment` writes to `financial_transactions` with all required fields
- [ ] Idempotency key generated and enforced UNIQUE on `financial_transactions`
- [ ] `DriverProfile.total_earnings` is derived from `driver_earnings` WHERE status='SETTLED' (not directly updated)
- [ ] `BookingService.completeTrip` appends `driver_earnings` ACCRUED entry
- [ ] V5 migration creates all new tables without modifying V1-V4
- [ ] V5 migration adds `fare_snapshot JSONB` and `pricing_profile_version_id` to `bookings` table
- [ ] btree_gist extension available/checked for `pricing_profile_versions` exclusion constraint

---
*This audit report is the Phase 1 deliverable for the DRIVIO pricing/final architecture mandate. Phase 2 implementation begins after explicit approval of these findings.*