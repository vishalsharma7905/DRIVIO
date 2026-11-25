import axios from 'axios';
import {
  AuthResponse,
  CustomerProfile,
  DriverProfile,
  DriverDocument,
  DriverAvailability,
  DriverBalanceSummary,
  DriverSettlementRecord,
  BookingResponse,
  PricingConfiguration,
  VehiclePricingConfiguration,
  PricingAuditLog,
  FareEstimate,
  DriverRecommendation,
  AiExtractionResult,
  LiveTracking,
  PaymentReceipt,
  ReviewItem,
  NotificationItem,
  Complaint,
  AdminDashboardStats,
  User,
  VerificationStatus,
  ServiceType,
  VehicleType,
  PaymentMethod
} from '../types';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to automatically add Bearer token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('drivio_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle 401s
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      const currentPath = window.location.pathname;
      if (currentPath !== '/login' && currentPath !== '/register' && currentPath !== '/') {
        localStorage.removeItem('drivio_token');
        localStorage.removeItem('drivio_user');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// Auth Endpoints
export const authApi = {
  login: (data: any) => api.post<AuthResponse>('/auth/login', data),
  register: (data: any) => api.post<AuthResponse>('/auth/register', data),
  refreshToken: (refreshToken: string) => api.post<AuthResponse>('/auth/refresh', { refreshToken }),
};

// Customer Endpoints
export const customerApi = {
  getProfile: () => api.get<CustomerProfile>('/customers/me'),
  updateProfile: (data: Partial<CustomerProfile>) => api.put<CustomerProfile>('/customers/me', data),
};

// Driver Endpoints
export const driverApi = {
  getProfile: () => api.get<DriverProfile>('/drivers/me'),
  updateProfile: (data: any) => api.put<DriverProfile>('/drivers/me', data),
  updateAvailability: (data: any) => api.put<DriverAvailability>('/drivers/availability', data),
  uploadDocument: (documentType: string, file: File) => {
    const formData = new FormData();
    formData.append('type', documentType);
    formData.append('file', file);
    return api.post<DriverDocument>('/drivers/documents', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  getDocuments: () => api.get<DriverDocument[]>('/drivers/documents'),
};

export const driverEarningsApi = {
  getSummary: () => api.get<DriverBalanceSummary>('/drivers/earnings/summary'),
  getSettlements: () => api.get<DriverSettlementRecord[]>('/drivers/earnings/settlements'),
  requestSettlement: (amount: number) => api.post<DriverSettlementRecord>('/drivers/earnings/settlements', { amount }),
  retrySettlement: (id: number) => api.post<DriverSettlementRecord>(`/drivers/earnings/settlements/${id}/retry`),
};

// Booking Endpoints
export const bookingApi = {
  create: (data: any) => api.post<BookingResponse>('/bookings', data),
  getById: (id: number) => api.get<BookingResponse>(`/bookings/${id}`),
  getMyBookings: () => api.get<BookingResponse[]>('/bookings/my'),
  accept: (id: number) => api.post<BookingResponse>(`/bookings/${id}/accept`),
  reject: (id: number) => api.post<BookingResponse>(`/bookings/${id}/reject`),
  driverArrived: (id: number) => api.post<BookingResponse>(`/bookings/${id}/arrived`),
  startTrip: (id: number) => api.post<BookingResponse>(`/bookings/${id}/start`),
  completeTrip: (id: number) => api.post<BookingResponse>(`/bookings/${id}/complete`),
  cancel: (id: number, data: { reason: string }) =>
    api.post<BookingResponse>(`/bookings/${id}/cancel`, { cancellationReason: data.reason }),
};

// Pricing Endpoints
export const pricingApi = {
  estimate: (data: {
    serviceType: ServiceType;
    vehicleType: VehicleType;
    estimatedDistanceKm: number;
    estimatedDurationMinutes: number;
  }) => api.post<FareEstimate>('/pricing/estimate', data),
  getConfigurations: () => api.get<PricingConfiguration[]>('/pricing/configurations'),
  updateConfiguration: (id: number, data: Partial<PricingConfiguration>) =>
    api.put<PricingConfiguration>(`/pricing/configurations/${id}`, data),
  getVehicleConfigurations: () => api.get<VehiclePricingConfiguration[]>('/pricing/vehicles'),
  getAllVehicleEstimates: (data: {
    serviceType: ServiceType;
    vehicleType: VehicleType;
    estimatedDistanceKm: number;
    estimatedDurationMinutes: number;
  }) => api.post<FareEstimate[]>('/pricing/vehicles/estimates', data),
  updateVehicleConfiguration: (id: number, data: Partial<VehiclePricingConfiguration>) =>
    api.put<VehiclePricingConfiguration>(`/pricing/vehicles/${id}`, data),
  getAuditLogs: () => api.get<PricingAuditLog[]>('/pricing/audit-logs'),
  getProfileVersions: (filter?: string) => api.get<any[]>('/admin/pricing/versions', { params: filter ? { filter } : undefined }),
  publishProfileVersion: (versionId: number) => api.post(`/admin/pricing/versions/${versionId}/publish`),
  retireProfileVersion: (versionId: number) => api.post(`/admin/pricing/versions/${versionId}/retire`),
};

// Recommendation Endpoints
export const recommendationApi = {
  getRecommendations: (data: {
    pickupLatitude: number;
    pickupLongitude: number;
    serviceType: ServiceType;
    vehicleType: VehicleType;
    scheduledPickupTime?: string;
  }) => api.post<DriverRecommendation[]>('/drivers/recommendations', data),
};

// AI Requirement Extraction Endpoints
export const aiApi = {
  extractRequirements: (prompt: string) =>
    api.post<AiExtractionResult>('/ai/extract-requirements', { prompt }),
};

// Tracking Endpoints
export const trackingApi = {
  updateLocation: (data: {
    bookingId: number;
    latitude: number;
    longitude: number;
    speedKmh?: number;
    headingDegrees?: number;
  }) => api.post('/tracking/location', data),
  getLatestLocation: (bookingId: number) =>
    api.get<LiveTracking>(`/tracking/${bookingId}`),
  getLocationHistory: (bookingId: number) =>
    api.get<LiveTracking[]>(`/tracking/${bookingId}`),
};

// Payment Endpoints
export const paymentApi = {
  processPayment: (data: {
    bookingId: number;
    paymentMethod: PaymentMethod;
    cardLastFour?: string;
    upiId?: string;
  }) => api.post<PaymentReceipt>('/payments/process', data),
  getReceipt: (bookingId: number) => api.get<PaymentReceipt>(`/payments/booking/${bookingId}`),
};

// Review Endpoints
export const reviewApi = {
  submitReview: (data: { bookingId: number; rating: number; reviewText?: string }) =>
    api.post<ReviewItem>('/reviews', data),
  getDriverReviews: (driverId: number) => api.get<ReviewItem[]>(`/reviews/driver/${driverId}`),
};

// Notification Endpoints
export const notificationApi = {
  getMyNotifications: () => api.get<NotificationItem[]>('/notifications'),
  markAsRead: (id: number) => api.put(`/notifications/${id}/read`),
};

// Admin Endpoints
export const adminApi = {
  getDashboardStats: () => api.get<AdminDashboardStats>('/admin/dashboard'),
  getAllUsers: () => api.get<User[]>('/admin/users'),
  toggleUserStatus: (id: number, active: boolean) =>
    api.put<User>(`/admin/users/${id}/status?active=${active}`),
  getDrivers: (status?: VerificationStatus) =>
    api.get<DriverProfile[]>('/admin/drivers', { params: { status } }),
  verifyDriver: (id: number, data: { status: VerificationStatus; remarks?: string }) =>
    api.put<DriverProfile>(`/admin/drivers/${id}/verify`, data),
  getAllBookings: () => api.get<BookingResponse[]>('/admin/bookings'),
  getAllComplaints: () => api.get<Complaint[]>('/admin/complaints'),
  updateComplaint: (id: number, data: { status: string; adminNotes: string }) =>
    api.put<Complaint>(`/admin/complaints/${id}`, data),
};

export default api;
