import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { Navbar } from './components/Navbar';
import { Footer } from './components/Footer';
import { ProtectedRoute } from './components/ProtectedRoute';
import { ErrorBoundary } from './components/ErrorBoundary';

// Public pages
import { LandingPage } from './pages/public/LandingPage';
import { AboutPage } from './pages/public/AboutPage';
import { ServicesPage } from './pages/public/ServicesPage';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';

// Customer pages
import { CustomerDashboard } from './pages/customer/CustomerDashboard';
import { CreateBookingPage } from './pages/customer/CreateBookingPage';
import { LiveTrackingPage } from './pages/customer/LiveTrackingPage';
import { CustomerTripsPage } from './pages/customer/CustomerTripsPage';
import { CustomerProfilePage } from './pages/customer/CustomerProfilePage';

// Driver pages
import { DriverDashboard } from './pages/driver/DriverDashboard';
import { DriverTripsPage } from './pages/driver/DriverTripsPage';
import { DriverVerificationPage } from './pages/driver/DriverVerificationPage';
import { DriverProfilePage } from './pages/driver/DriverProfilePage';

// Admin pages
import { AdminDashboard } from './pages/admin/AdminDashboard';
import { AdminDriverVerificationPage } from './pages/admin/AdminDriverVerificationPage';
import { AdminUserManagementPage } from './pages/admin/AdminUserManagementPage';
import { AdminBookingManagementPage } from './pages/admin/AdminBookingManagementPage';
import { AdminPricingPage } from './pages/admin/AdminPricingPage';
import { AdminComplaintsPage } from './pages/admin/AdminComplaintsPage';

export const App: React.FC = () => {
  return (
    <Router>
      <ThemeProvider>
        <AuthProvider>
          <div className="min-h-screen flex flex-col drivio-shell bg-[#141615] text-neutral-100 font-sans">
          <Navbar />
          <main className="flex-1">
            <ErrorBoundary>
              <Routes>
                {/* Public Routes */}
                <Route path="/" element={<LandingPage />} />
                <Route path="/about" element={<AboutPage />} />
                <Route path="/services" element={<ServicesPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />

                {/* Direct Booking & Tracking aliases */}
                <Route
                  path="/ride"
                  element={
                    <ProtectedRoute allowedRoles={['CUSTOMER', 'ADMIN']}>
                      <CreateBookingPage defaultMode="RIDE" />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/book"
                  element={
                    <ProtectedRoute allowedRoles={['CUSTOMER', 'ADMIN']}>
                      <CreateBookingPage defaultMode="DRIVER" />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/track/:id"
                  element={
                    <ProtectedRoute allowedRoles={['CUSTOMER', 'DRIVER', 'ADMIN']}>
                      <LiveTrackingPage />
                    </ProtectedRoute>
                  }
                />

                {/* Customer Routes */}
                <Route
                  path="/customer/dashboard"
                  element={
                    <ProtectedRoute allowedRoles={['CUSTOMER', 'ADMIN']}>
                      <CustomerDashboard />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/customer/ride"
                  element={
                    <ProtectedRoute allowedRoles={['CUSTOMER', 'ADMIN']}>
                      <CreateBookingPage defaultMode="RIDE" />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/customer/book"
                  element={
                    <ProtectedRoute allowedRoles={['CUSTOMER', 'ADMIN']}>
                      <CreateBookingPage defaultMode="DRIVER" />
                    </ProtectedRoute>
                  }
                />
              <Route
                path="/customer/tracking/:id"
                element={
                  <ProtectedRoute allowedRoles={['CUSTOMER', 'DRIVER', 'ADMIN']}>
                    <LiveTrackingPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/customer/trips"
                element={
                  <ProtectedRoute allowedRoles={['CUSTOMER', 'ADMIN']}>
                    <CustomerTripsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/customer/profile"
                element={
                  <ProtectedRoute allowedRoles={['CUSTOMER', 'ADMIN']}>
                    <CustomerProfilePage />
                  </ProtectedRoute>
                }
              />

              {/* Driver Routes */}
              <Route
                path="/driver/dashboard"
                element={
                  <ProtectedRoute allowedRoles={['DRIVER', 'ADMIN']}>
                    <DriverDashboard />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/driver/trips"
                element={
                  <ProtectedRoute allowedRoles={['DRIVER', 'ADMIN']}>
                    <DriverTripsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/driver/verification"
                element={
                  <ProtectedRoute allowedRoles={['DRIVER', 'ADMIN']}>
                    <DriverVerificationPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/driver/profile"
                element={
                  <ProtectedRoute allowedRoles={['DRIVER', 'ADMIN']}>
                    <DriverProfilePage />
                  </ProtectedRoute>
                }
              />

              {/* Admin Routes */}
              <Route
                path="/admin/dashboard"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <AdminDashboard />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/drivers"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <AdminDriverVerificationPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/users"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <AdminUserManagementPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/bookings"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <AdminBookingManagementPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/pricing"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <AdminPricingPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/complaints"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <AdminComplaintsPage />
                  </ProtectedRoute>
                }
              />

                {/* Fallback */}
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </ErrorBoundary>
          </main>
            <Footer />
          </div>
        </AuthProvider>
      </ThemeProvider>
    </Router>
  );
};

export default App;
