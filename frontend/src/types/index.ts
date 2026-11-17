export type Role = 'CUSTOMER' | 'DRIVER' | 'ADMIN';

export type ServiceType = 
  | 'OWN_CAR_DRIVER'
  | 'ONE_WAY'
  | 'ROUND_TRIP'
  | 'HOURLY'
  | 'OUTSTATION';

export type VehicleType = 'HATCHBACK' | 'SEDAN' | 'SUV' | 'LUXURY';

export type BookingStatus =
  | 'PENDING'
  | 'SEARCHING_DRIVER'
  | 'DRIVER_ASSIGNED'
  | 'DRIVER_ACCEPTED'
  | 'DRIVER_REJECTED'
  | 'DRIVER_ARRIVED'
  | 'TRIP_STARTED'
  | 'TRIP_COMPLETED'
  | 'CANCELLED';

export type VerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';

export type PaymentMethod = 'CASH' | 'MOCK_CARD' | 'MOCK_UPI';

export type ComplaintStatus = 'OPEN' | 'UNDER_INVESTIGATION' | 'RESOLVED' | 'DISMISSED';

export interface User {
  id: number;
  email: string;
  fullName: string;
  phoneNumber: string;
  role: Role;
  isActive: boolean;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  email: string;
  fullName: string;
  role: Role;
  verificationStatus?: VerificationStatus;
  profileId?: number;
}

export interface CustomerProfile {
  id: number;
  userId: number;
  email: string;
  fullName: string;
  phoneNumber?: string;
  preferredLanguage: string;
  emergencyContact?: string;
  homeAddress?: string;
  createdAt: string;
}

export interface DriverAvailability {
  isAvailable: boolean;
  shiftStart?: string;
  shiftEnd?: string;
  statusNote?: string;
}

export interface DriverProfile {
  id: number;
  userId: number;
  email?: string;
  fullName?: string;
  user?: User;
  phoneNumber?: string;
  profilePhotoUrl?: string;
  licenceNumber?: string;
  licenceExpiryDate?: string;
  licenceDocumentUrl?: string;
  experienceYears: number;
  languages: string;
  vehicleCategories: string;
  servicePreferences: string;
  verificationStatus: VerificationStatus;
  rejectionReason?: string;
  rating: number;
  totalRatingsCount: number;
  totalTrips: number;
  totalEarnings: number;
  currentLatitude?: number;
  currentLongitude?: number;
  lastLocationUpdate?: string;
  available?: boolean;
  availability?: DriverAvailability;
  statusNote?: string;
  createdAt: string;
}

export interface DriverDocument {
  id: number;
  driverId: number;
  documentType: string;
  documentUrl?: string;
  fileName?: string;
  verificationStatus: VerificationStatus;
  remarks?: string;
  createdAt: string;
}

export interface PricingConfiguration {
  id: number;
  serviceType: ServiceType;
  baseFare: number;
  perKmRate: number;
  perHourRate: number;
  minFare: number;
  cancellationFee: number;
  taxPercentage: number;
  active: boolean;
}

export interface FareEstimate {
  serviceType: ServiceType;
  vehicleType: VehicleType;
  estimatedDistanceKm: number;
  estimatedDurationMinutes: number;
  baseFare: number;
  perKmRate: number;
  distanceFare: number;
  durationFare: number;
  additionalCharges: number;
  subtotal: number;
  taxPercentage: number;
  taxAmount: number;
  totalEstimatedFare: number;
  disclaimer: string;
}

export interface ScoreBreakdown {
  availabilityScore: number;
  proximityScore: number;
  ratingScore: number;
  experienceScore: number;
  vehicleCompatibilityScore: number;
  serviceCompatibilityScore: number;
}

export interface DriverRecommendation {
  driverId: number;
  driverName: string;
  phoneNumber?: string;
  profilePhotoUrl?: string;
  rating: number;
  totalTrips: number;
  experienceYears: number;
  languages: string;
  currentLatitude?: number;
  currentLongitude?: number;
  distanceKm: number;
  matchScore: number;
  matchReasons: string[];
  breakdown: ScoreBreakdown;
}

export interface Booking {
  id: number;
  bookingNumber: string;
  bookingReference?: string;
  customerId: number;
  customerName: string;
  customerPhone?: string;
  driverId?: number;
  driverName?: string;
  driverPhone?: string;
  driverPhotoUrl?: string;
  driverRating?: number;
  driverLatitude?: number;
  driverLongitude?: number;
  serviceType: ServiceType;
  vehicleType: VehicleType;
  status: BookingStatus;
  bookingStatus?: BookingStatus;
  pickupAddress: string;
  pickupLatitude: number;
  pickupLongitude: number;
  dropoffAddress: string;
  dropoffLatitude: number;
  dropoffLongitude: number;
  destinationAddress?: string;
  destinationLatitude?: number;
  destinationLongitude?: number;
  scheduledPickupTime: string;
  estimatedDistanceKm: number;
  estimatedDurationMinutes: number;
  actualStartTime?: string;
  actualEndTime?: string;
  baseFare: number;
  distanceFare: number;
  durationFare: number;
  additionalCharges: number;
  taxAmount: number;
  estimatedFare: number;
  totalFare?: number;
  paymentStatus: PaymentStatus;
  cancellationReason?: string;
  specialInstructions?: string;
  rated: boolean;
  createdAt: string;
  updatedAt: string;
}

export type BookingResponse = Booking;
export type BookingResponseDto = Booking;

export interface LiveTracking {
  bookingId: number;
  bookingNumber?: string;
  bookingReference?: string;
  status?: BookingStatus;
  bookingStatus?: BookingStatus;
  driverId?: number;
  driverName?: string;
  driverPhone?: string;
  driverLatitude?: number;
  driverLongitude?: number;
  speed?: number;
  heading?: number;
  pickupLatitude: number;
  pickupLongitude: number;
  pickupAddress: string;
  dropoffLatitude?: number;
  dropoffLongitude?: number;
  dropoffAddress?: string;
  destinationLatitude?: number;
  destinationLongitude?: number;
  destinationAddress?: string;
  timestamp: string;
}

export interface PaymentReceipt {
  id: number;
  bookingId: number;
  bookingNumber?: string;
  bookingReference?: string;
  customerId: number;
  customerName: string;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  transactionId: string;
  amount: number;
  taxAmount: number;
  paidAt: string;
  receiptNumber: string;
  createdAt: string;
}

export interface NotificationItem {
  id: number;
  title: string;
  message: string;
  notificationType: string;
  read: boolean;
  referenceId?: number;
  createdAt: string;
}

export interface ReviewItem {
  id: number;
  bookingId: number;
  customerId: number;
  customerName: string;
  driverId: number;
  driverName: string;
  rating: number;
  reviewText?: string;
  createdAt: string;
}

export interface ComplaintItem {
  id: number;
  bookingId?: number;
  bookingNumber?: string;
  bookingReference?: string;
  userId: number;
  userName?: string;
  complainantName?: string;
  userEmail?: string;
  category: string;
  description: string;
  status: ComplaintStatus;
  adminNotes?: string;
  createdAt: string;
  updatedAt: string;
}

export type Complaint = ComplaintItem;

export interface AdminStats {
  totalCustomers: number;
  totalDrivers: number;
  verifiedDrivers: number;
  pendingDriverVerifications: number;
  pendingDrivers?: number;
  rejectedDrivers?: number;
  totalBookings: number;
  activeBookings: number;
  completedBookings: number;
  cancelledBookings: number;
  totalRevenue: number;
}

export type AdminDashboardStats = AdminStats;

export interface AiExtractionResult {
  serviceType: ServiceType;
  vehicleType: VehicleType;
  experiencePreference: string;
  pickupDate?: string;
  pickupTime?: string;
  estimatedDistanceKm?: number;
  pickupLocationHint?: string;
  destinationHint?: string;
  additionalNotes?: string;
  missingFields: string[];
  fallbackUsed: boolean;
  rawExtraction?: string;
}
