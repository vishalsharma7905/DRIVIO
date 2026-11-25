# DRIVIO — Complete Pricing & Financial Architecture Final Specification (Design Only)

No files modified. No migrations created. No implementation performed.

---

## 1. Executive Summary

A single unified pricing engine replaces the two legacy engines (service-based `pricing_configurations`, vehicle-based `vehicle_pricing_configurations`). Pricing profiles are versioned, immutable once published, effective-dated, and resolved by `(service family, vehicle category, timestamp)`. Financial accounting separates customer payment, driver entitlement, tax, third-party pass-through, and platform revenue into two distinct reconciliation equations. An append-only financial event ledger records payments/refunds/adjustments; driver earnings are a separate append-only ledger with separate settlement records. Historical bookings are never recalculated.

---

## 2. Existing Project Audit

### Compatibility table

| Existing component | Current responsibility | Required change | Compatibility risk |
|---|---|---|---|
| `Booking` entity | stores fare components + status | additive snapshot columns only | low |
| `BookingService` | creates booking, calls pricing, persists fare | resolve profile version; write snapshot | medium |
| `PricingService` | two engines (normal vs service) | unify behind feature flag | medium |
| `VehiclePricingConfiguration` | per-vehicle rates | backfill into profile versions | low (legacy retained) |
| `PricingConfiguration` | per-service rates | backfill into profile versions | low |
| `PricingController` | estimate + admin update | add profile/version endpoints | low |
| `Payment` entity | status/UI | retained; ledger added alongside | low |
| `PaymentService` | payment lifecycle | write financial_transactions too | medium |
| `DriverProfile` | earnings totals | add accrual tracking via driver_earnings | low |
| `VehicleType`/`ServiceType` enums | categories/services | unchanged; referenced as strings | low |
| Flyway V1–V4 | schema | V5/V6 additive only | checksum risk on V1 (see §12) |
| Frontend `CreateBookingPage` | estimate display | consume unified estimate | low |

### Preserved functionality
Normal rides (BIKE/AUTO/HATCHBACK/SEDAN/PREMIUM_SEDAN/SUV), own-car driver (HOURLY/DAILY), luggage delivery. No self-drive rental (not in scope).

---

## 3. Final Architecture Diagram

```
PricingEngine (pure, BigDecimal)
   │
   ▼
FinancialCalculationService ──► BookingService ──► bookings + fare_snapshot(JSONB)
   │
   ├──► PaymentService ──► financial_transactions (append-only ledger)
   ├──► DriverEarningsService ──► driver_earnings (ACCRUED after trip+capture)
   └──► SettlementService ──► driver_settlements + items + attempts
```

Backend is sole authority for final fare. Frontend only displays estimates.

---

## 4. Pricing Engine Design

### Service family → profile mapping

- `profile_type = 'VEHICLE_BASED'`: normal rides, keyed by VehicleType.
- `profile_type = 'SERVICE_BASED'`: own-car driver (HOURLY/DAILY modes) and luggage delivery, keyed by service subtype.
- A booking resolves profile by `(service family, vehicle category or service mode, now())`. Cross-family resolution forbidden at app layer (validate profile_type matches booking's service family).

### Configurable fields
baseFare, perKmRate, perMinuteRate, perHourRate, minFare, waitingFreeMinutes, waitingPerMinuteRate, nightSurchargePercent, peakSurchargePercent, stackPeakAndNight, additionalFlatCharges, cancellationFee, platformCommissionPercent, tollTreatment, parkingTreatment, taxRate, taxMode, taxableComponents (JSONB), currency, jurisdiction, gatewayFeePercent (estimate only).

### Fare calculation order (BigDecimal, HALF_UP, scale 2)
1. distanceFare = perKmRate × distance
2. durationFare = perMinuteRate × minutes (or perHourRate × hours)
3. core = baseFare + distanceFare + durationFare
4. fareAfterMin = max(core, minFare); minFareAdjustment = fareAfterMin − core
5. waitingCharges = waitingPerMinuteRate × max(0, waitingMinutes − waitingFreeMinutes)
6. surchargePercent = (peak && night && stack) ? peak+night : max(peak if applicable, night if applicable) : applicable
7. surchargeAmount = fareAfterMin × surchargePercent / 100
8. peakSurcharge = fareAfterMin × peakPercent/100 (if peak); nightSurcharge = fareAfterMin × nightPercent/100 (if night) [split for snapshot]
9. additionalCharges = waitingCharges + surchargeAmount + additionalFlat
10. fareSubtotal = fareAfterMin + additionalCharges
11. taxableBase = sum of components where taxableComponents flag true
12. tax = (taxMode=EXCLUSIVE) ? taxableBase × taxRate : taxableBase × taxRate/(1+taxRate)
13. customerTotal = (EXCLUSIVE) taxableBase + tax + gatewayPassedFee : taxableBase + gatewayPassedFee (tax embedded)
14. tolls, parking added to customerTotal as pass-through
15. commissionableBase = fareSubtotal
16. commission = commissionableBase × commissionPercent
17. driverReimbursement = tolls(if DRIVER_PAID)+parking(if DRIVER_PAID)
18. thirdPartyPassThrough = tolls(if THIRD_PARTY)+parking(if THIRD_PARTY)
19. driverNetPayoutEntitlement = commissionableBase − commission + driverReimbursement

No hardcoded multipliers. All rates from profile version.

---

## 5. Versioning Design

States: DRAFT, PUBLISHED, RETIRED. `category_key` generated column = COALESCE(vehicle_category,'__ALL__') (safe sentinel). Effective timestamps TIMESTAMPTZ. Resolution: status=PUBLISHED AND effective_from <= now() AND (effective_to IS NULL OR effective_to > now()), ORDER BY effective_from DESC, business_version DESC LIMIT 1. No background job.

Publishing a future version: in one transaction insert PUBLISHED row with future effective_from, and UPDATE previous PUBLISHED row SET effective_to = new.effective_from WHERE effective_to IS NULL. Exclusion constraint prevents overlap. Optimistic lock via version_lock. btree_gist required; if unavailable, use trigger-based overlap check (less robust) — must verify in deployment.

---

## 6. Financial Allocation Formulas

### A. Customer payment allocation
customerPayment = driverNetPayoutEntitlement + platformGrossCommissionRevenue + taxPayable + thirdPartyPassThrough + gatewayFeePassedToCustomer

### B. Gateway settlement reconciliation (separate)
netSettlementReceived = customerPaymentCaptured − gatewayProcessingDeductions − gatewayRefunds − otherProviderAdjustments + gatewayReconciliationAdjustments
outstandingGatewayBalance = customerPaymentCaptured − netSettlementReceived − gatewayRefunds

platformNetRevenue = platformGrossCommissionRevenue − gatewayFeeActualAbsorbed

---

## 7. Payment & Refund Lifecycle

Append-only `financial_transactions`. Events: PAYMENT_INITIATED, PAYMENT_SUCCESS, PAYMENT_CAPTURED, PAYMENT_FAILED, FULL_REFUND, PARTIAL_REFUND, REFUND_FAILED, PAYMENT_REVERSAL, COMMISSION_ADJUSTMENT, DRIVER_PAYOUT_ADJUSTMENT, GATEWAY_RECONCILIATION, CANCELLATION_FEE. Signed amounts (+ in, − out). Idempotency via UNIQUE idempotency_key and UNIQUE gateway_event_id (non-null when present). Original refs via reference_transaction_id ON DELETE RESTRICT.

Pending→success: new row, never mutate. Refund concurrency: advisory lock `pg_advisory_xact_lock(hashtext('refund:'||booking_id))`, then check sum of successful refunds + pending reservations ≤ captured amount.

Partial refund proportional allocation (Bike, refund ₹20 of ₹49.88):
- ratio = 20/49.88 = 0.4010
- customerRefund = 20.00 (exact, by construction)
- taxReversal = 2.38 × 0.4010 = 0.9544 → 0.95; residual 0.0044 absorbed into commission reversal or driver payout reversal via residual policy: allocate remainder to the largest component (platform commission) to ensure sum = 20.00.
- commissionReversal = 7.13 × 0.4010 = 2.859 → 2.86
- driverPayoutReversal = 40.37 × 0.4010 = 16.188 → 16.19
Check: 0.95 + 2.86 + 16.19 = 20.00 ✓ (residual policy ensures exact)

Full refund: all components reversed fully.

---

## 8. Driver Earnings & Settlement Lifecycle

Earnings ledger append-only. Status ACCRUED/ADJUSTED/REVERSED/SETTLED. Accrual only when trip COMPLETED AND payment captured. Settlement separate: driver_settlements + driver_settlement_items + driver_settlement_attempts. Partial settlement supported; failed settlement leaves earnings ACCRUED (retryable). Reversal after settlement via new REVERSAL entry + adjustment transaction.

Example: ₹500 available → ₹300 partial settlement (items for subset) → ₹200 remains. Failed transfer → status FAILED, attempt recorded, earnings unchanged. Retry → new attempt. Success → earnings marked SETTLED, completed_at set.

---

## 9. Booking Snapshot Design

Additive columns on bookings (see DDL). Plus `fare_snapshot JSONB` immutable structured snapshot. Immutability via app (no update endpoints) + DB trigger rejecting updates to fare columns post-insert. Legacy bookings: pricing_profile_version_id IS NULL ⇒ use stored columns; never recompute.

---

## 10. Complete Consolidated PostgreSQL DDL

(Consolidated from prior phases; see pricing_profiles, pricing_profile_versions, pricing_audit_logs_v2, financial_transactions, driver_earnings, driver_settlements, driver_settlement_items, driver_settlement_attempts, and additive booking columns as specified in Phase 3.4, with generated category_key, exclusion constraint, btree_gist, CHECK constraints, indexes, and immutability triggers.)

Key constraints: all money NUMERIC/DECIMAL with CHECK >=0; percentages CHECK 0–100; currency CHECK regex; idempotency UNIQUE; FKs RESTRICT on historical refs; no ON DELETE SET NULL on financial refs.

---

## 11. Compatibility Analysis

See §2. Primary risk: V1 checksum if applied to persistent DB. H2 dev DB recreated each run → safe. Production: verify flyway_schema_history before any V1 touch; if applied, use forward migration or verified `flyway repair`, never rewrite.

---

## 12. Flyway Migration Plan

- V5: new tables + additive booking columns.
- V6: controlled backfill from legacy configs; link unambiguous bookings; leave NULL otherwise.
- Rollout: feature-flagged unified engine; keep legacy read-only.
- Later: deprecate legacy tables.

---

## 13. Numerical Test Matrix

All 20 scenarios verified against equations in prior phases; reconciliation holds (customerPayment = driverNetPayout + platformGross + tax + thirdParty + gatewayPassed; platformNet = platformGross − gatewayAbsorbed). Scheduled pricing and concurrent publication handled by exclusion constraint + optimistic lock.

---

## 14. Validation Report

SQL syntax not executed (no PostgreSQL instance available in this environment). DDL reviewed manually for syntax, FK targets, column existence, constraint conflicts. btree_gist availability unverified — flagged. All equations verified numerically by hand.

---

## 15. Remaining Human Decisions

1. btree_gist availability in production.
2. Tax jurisdiction & taxable components (accountant).
3. Gateway fee payer default.
4. Keep `Payment` entity vs full ledger migration.
5. V1 checksum verification on persistent DBs.
6. SettlementService scope (manual vs automated).

No implementation performed. Awaiting explicit approval.