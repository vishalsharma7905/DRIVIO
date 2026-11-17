import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { driverApi, bookingApi } from '../../services/api';
import { DriverProfile, BookingResponse } from '../../types';
import { 
  Car, 
  DollarSign, 
  Star, 
  Clock, 
  MapPin, 
  CheckCircle2, 
  XCircle, 
  AlertTriangle, 
  ArrowRight, 
  Power,
  Navigation,
  ShieldCheck,
  Calendar
} from 'lucide-react';

export const DriverDashboard: React.FC = () => {
  const { user } = useAuth();
  const [profile, setProfile] = useState<DriverProfile | null>(null);
  const [isAvailable, setIsAvailable] = useState<boolean>(false);
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [togglingStatus, setTogglingStatus] = useState(false);
  const [actionInProgress, setActionInProgress] = useState<number | null>(null);

  const fetchDriverData = async () => {
    try {
      setLoading(true);
      const [profileRes, bookingsRes] = await Promise.all([
        driverApi.getProfile(),
        bookingApi.getMyBookings()
      ]);
      setProfile(profileRes.data);
      if (profileRes.data.availability) {
        setIsAvailable(profileRes.data.availability.isAvailable);
      }
      setBookings(bookingsRes.data);
    } catch (err) {
      console.error('Failed to load driver dashboard data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDriverData();
    const interval = setInterval(fetchDriverData, 15000); // Polling for new assigned requests
    return () => clearInterval(interval);
  }, []);

  const handleToggleAvailability = async () => {
    try {
      setTogglingStatus(true);
      const newStatus = !isAvailable;
      await driverApi.updateAvailability({
        isAvailable: newStatus,
        shiftStart: profile?.availability?.shiftStart || '08:00:00',
        shiftEnd: profile?.availability?.shiftEnd || '20:00:00',
        statusNote: newStatus ? 'Available for bookings' : 'Offline'
      });
      setIsAvailable(newStatus);
    } catch (err) {
      console.error('Failed to update availability', err);
      alert('Could not update status. Please ensure your profile is verified.');
    } finally {
      setTogglingStatus(false);
    }
  };

  const handleAccept = async (bookingId: number) => {
    try {
      setActionInProgress(bookingId);
      await bookingApi.accept(bookingId);
      await fetchDriverData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to accept booking');
    } finally {
      setActionInProgress(null);
    }
  };

  const handleReject = async (bookingId: number) => {
    if (!window.confirm('Are you sure you want to decline this booking?')) return;
    try {
      setActionInProgress(bookingId);
      await bookingApi.reject(bookingId);
      await fetchDriverData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to decline booking');
    } finally {
      setActionInProgress(null);
    }
  };

  if (loading && !profile) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-600"></div>
      </div>
    );
  }

  // Active / ongoing trip
  const activeTrip = bookings.find(b => 
    ['DRIVER_ACCEPTED', 'DRIVER_ARRIVED', 'TRIP_STARTED'].includes(b.status)
  );

  // Incoming booking requests waiting for driver acceptance
  const pendingRequests = bookings.filter(b => b.status === 'DRIVER_ASSIGNED');

  // Past completed bookings
  const completedTrips = bookings.filter(b => b.status === 'TRIP_COMPLETED');

  const verificationStatus = profile?.verificationStatus || 'PENDING';

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header & Status Control */}
      <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
        <div>
          <div className="flex items-center space-x-3">
            <h1 className="text-2xl font-bold text-slate-900">Welcome, {user?.fullName}</h1>
            <span className={`px-3 py-1 text-xs font-semibold rounded-full flex items-center space-x-1 ${
              verificationStatus === 'VERIFIED'
                ? 'bg-emerald-100 text-emerald-800'
                : verificationStatus === 'REJECTED'
                ? 'bg-rose-100 text-rose-800'
                : 'bg-amber-100 text-amber-800'
            }`}>
              <ShieldCheck className="w-3.5 h-3.5 mr-1" />
              {verificationStatus}
            </span>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            {verificationStatus === 'VERIFIED'
              ? 'You are fully verified and eligible to receive customer booking requests.'
              : 'Complete your KYC document verification to become eligible for customer bookings.'}
          </p>
        </div>

        {/* Availability Toggle */}
        <div className="flex items-center space-x-4 bg-slate-50 p-3 rounded-xl border border-slate-200">
          <div className="text-right">
            <span className="text-xs font-semibold text-slate-500 block uppercase">Duty Status</span>
            <span className={`text-sm font-bold ${isAvailable ? 'text-emerald-600' : 'text-slate-400'}`}>
              {isAvailable ? 'Online & Available' : 'Offline'}
            </span>
          </div>
          <button
            onClick={handleToggleAvailability}
            disabled={togglingStatus || verificationStatus !== 'VERIFIED'}
            className={`p-3 rounded-xl transition flex items-center justify-center ${
              isAvailable
                ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                : 'bg-slate-200 hover:bg-slate-300 text-slate-700'
            } ${verificationStatus !== 'VERIFIED' ? 'opacity-50 cursor-not-allowed' : ''}`}
            title={verificationStatus !== 'VERIFIED' ? 'Account must be verified by admin first' : 'Toggle online status'}
          >
            <Power className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* KYC Warning Banner if not verified */}
      {verificationStatus !== 'VERIFIED' && (
        <div className="bg-amber-50 border border-amber-200 rounded-2xl p-5 flex items-start space-x-4">
          <AlertTriangle className="w-6 h-6 text-amber-600 shrink-0 mt-0.5" />
          <div className="flex-1">
            <h3 className="font-semibold text-amber-900">KYC Verification {verificationStatus}</h3>
            <p className="text-sm text-amber-700 mt-1">
              {verificationStatus === 'PENDING'
                ? 'Your driving license and documents are currently under review by our operations team. You can view or upload additional documents anytime.'
                : 'Your documents were rejected. Please review administrator comments and re-upload your driving license.'}
            </p>
            <Link
              to="/driver/verification"
              className="inline-flex items-center text-sm font-semibold text-amber-900 underline mt-2 hover:text-amber-800"
            >
              Manage KYC Documents <ArrowRight className="w-4 h-4 ml-1" />
            </Link>
          </div>
        </div>
      )}

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Earnings</span>
            <div className="p-2.5 bg-emerald-50 text-emerald-600 rounded-xl">
              <DollarSign className="w-5 h-5" />
            </div>
          </div>
          <p className="text-2xl font-bold text-slate-900 mt-4">
            ₹{profile?.totalEarnings ? profile.totalEarnings.toFixed(2) : '0.00'}
          </p>
          <span className="text-xs text-slate-500 mt-1 block">Lifetime platform earnings</span>
        </div>

        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Trips Completed</span>
            <div className="p-2.5 bg-blue-50 text-blue-600 rounded-xl">
              <Car className="w-5 h-5" />
            </div>
          </div>
          <p className="text-2xl font-bold text-slate-900 mt-4">
            {profile?.totalTrips || 0}
          </p>
          <span className="text-xs text-slate-500 mt-1 block">Successful trips delivered</span>
        </div>

        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Driver Rating</span>
            <div className="p-2.5 bg-amber-50 text-amber-600 rounded-xl">
              <Star className="w-5 h-5" />
            </div>
          </div>
          <p className="text-2xl font-bold text-slate-900 mt-4 flex items-center">
            {profile?.rating ? profile.rating.toFixed(2) : '5.00'}
            <span className="text-xs font-normal text-slate-500 ml-2">
              ({profile?.totalRatingsCount || 0} reviews)
            </span>
          </p>
          <span className="text-xs text-slate-500 mt-1 block">Customer satisfaction score</span>
        </div>

        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Experience</span>
            <div className="p-2.5 bg-purple-50 text-purple-600 rounded-xl">
              <Clock className="w-5 h-5" />
            </div>
          </div>
          <p className="text-2xl font-bold text-slate-900 mt-4">
            {profile?.experienceYears || 0} Years
          </p>
          <span className="text-xs text-slate-500 mt-1 block">Verified commercial driving</span>
        </div>
      </div>

      {/* Active Trip Alert if in progress */}
      {activeTrip && (
        <div className="bg-gradient-to-r from-blue-600 to-indigo-700 text-white rounded-2xl p-6 shadow-md">
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
            <div>
              <span className="inline-block bg-white/20 text-white text-xs px-2.5 py-1 rounded-full font-semibold mb-2">
                ACTIVE TRIP IN PROGRESS • #{activeTrip.bookingNumber}
              </span>
              <h3 className="text-xl font-bold">
                {activeTrip.serviceType.replace(/_/g, ' ')} ({activeTrip.vehicleType})
              </h3>
              <p className="text-blue-100 text-sm mt-1 flex items-center">
                <MapPin className="w-4 h-4 mr-1 shrink-0" />
                From: {activeTrip.pickupAddress}
              </p>
              <p className="text-blue-100 text-sm mt-0.5 flex items-center">
                <Navigation className="w-4 h-4 mr-1 shrink-0" />
                To: {activeTrip.dropoffAddress}
              </p>
            </div>
            <Link
              to="/driver/trips"
              className="bg-white text-blue-700 hover:bg-blue-50 font-semibold px-6 py-3 rounded-xl transition shadow flex items-center"
            >
              Open Trip Navigation & Controls <ArrowRight className="w-4 h-4 ml-2" />
            </Link>
          </div>
        </div>
      )}

      {/* Incoming Requests Section */}
      <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
        <div className="flex justify-between items-center mb-6">
          <div className="flex items-center space-x-2">
            <h2 className="text-lg font-bold text-slate-900">Incoming Booking Requests</h2>
            {pendingRequests.length > 0 && (
              <span className="bg-rose-100 text-rose-700 text-xs px-2.5 py-0.5 rounded-full font-semibold animate-pulse">
                {pendingRequests.length} New
              </span>
            )}
          </div>
          <span className="text-xs text-slate-500">Auto-refreshes every 15s</span>
        </div>

        {pendingRequests.length === 0 ? (
          <div className="text-center py-10 border-2 border-dashed border-slate-200 rounded-xl">
            <Car className="w-10 h-10 text-slate-300 mx-auto mb-2" />
            <p className="text-slate-600 font-medium">No pending requests right now</p>
            <p className="text-xs text-slate-400 mt-1">
              {isAvailable
                ? 'Keep your status Online. You will be matched when customers request trips in your area.'
                : 'Turn your status Online to receive incoming customer trips.'}
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {pendingRequests.map(req => (
              <div
                key={req.id}
                className="border border-emerald-200 bg-emerald-50/40 rounded-xl p-5 flex flex-col md:flex-row justify-between items-start md:items-center gap-4 transition hover:shadow-sm"
              >
                <div className="space-y-2 flex-1">
                  <div className="flex items-center space-x-2">
                    <span className="font-bold text-slate-900 text-base">
                      {req.serviceType.replace(/_/g, ' ')}
                    </span>
                    <span className="bg-slate-200 text-slate-700 text-xs font-semibold px-2 py-0.5 rounded">
                      {req.vehicleType}
                    </span>
                    <span className="text-xs text-slate-500">#{req.bookingNumber}</span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-sm text-slate-600">
                    <div className="flex items-start space-x-1.5">
                      <MapPin className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                      <span><strong>Pickup:</strong> {req.pickupAddress}</span>
                    </div>
                    <div className="flex items-start space-x-1.5">
                      <Navigation className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
                      <span><strong>Destination:</strong> {req.dropoffAddress}</span>
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-4 text-xs text-slate-500 pt-1">
                    <span className="flex items-center">
                      <Calendar className="w-3.5 h-3.5 mr-1" />
                      {new Date(req.scheduledPickupTime).toLocaleString()}
                    </span>
                    <span>Distance: {req.estimatedDistanceKm} km</span>
                    <span className="font-bold text-slate-900 text-sm text-emerald-700">
                      Estimated Payout: ₹{Number(req.totalFare ?? req.estimatedFare ?? 0).toFixed(2)}
                    </span>
                  </div>
                </div>

                <div className="flex items-center space-x-3 w-full md:w-auto">
                  <button
                    onClick={() => handleReject(req.id)}
                    disabled={actionInProgress === req.id}
                    className="flex-1 md:flex-none px-4 py-2.5 bg-white border border-rose-200 text-rose-600 hover:bg-rose-50 rounded-xl text-sm font-semibold transition flex items-center justify-center space-x-1"
                  >
                    <XCircle className="w-4 h-4" />
                    <span>Decline</span>
                  </button>
                  <button
                    onClick={() => handleAccept(req.id)}
                    disabled={actionInProgress === req.id}
                    className="flex-1 md:flex-none px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-sm font-semibold transition flex items-center justify-center space-x-1 shadow"
                  >
                    <CheckCircle2 className="w-4 h-4" />
                    <span>Accept Request</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Completed Trips Overview */}
      <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-lg font-bold text-slate-900">Recent Completed Trips</h2>
          <Link to="/driver/trips" className="text-sm font-semibold text-emerald-600 hover:text-emerald-700">
            View All Trips →
          </Link>
        </div>

        {completedTrips.length === 0 ? (
          <p className="text-sm text-slate-500 py-4 text-center">No completed trips yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-slate-700 text-xs uppercase font-semibold">
                <tr>
                  <th className="py-3 px-4">Booking</th>
                  <th className="py-3 px-4">Route</th>
                  <th className="py-3 px-4">Service</th>
                  <th className="py-3 px-4">Date</th>
                  <th className="py-3 px-4">Fare</th>
                  <th className="py-3 px-4">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {completedTrips.slice(0, 5).map(trip => {
                  const bookingRef = trip.bookingReference || trip.bookingNumber || `DRV-${trip.id}`;
                  const dropoff = trip.destinationAddress || trip.dropoffAddress || '';
                  const fare = trip.totalFare ?? trip.estimatedFare ?? 0;
                  const status = trip.bookingStatus || trip.status || 'COMPLETED';

                  return (
                    <tr key={trip.id} className="hover:bg-slate-50/50">
                      <td className="py-3.5 px-4 font-mono font-medium text-slate-900">#{bookingRef}</td>
                      <td className="py-3.5 px-4">
                        <span className="block truncate max-w-xs">{trip.pickupAddress} → {dropoff}</span>
                      </td>
                      <td className="py-3.5 px-4">{trip.serviceType?.replace(/_/g, ' ')}</td>
                      <td className="py-3.5 px-4 text-xs">{new Date(trip.createdAt).toLocaleDateString()}</td>
                      <td className="py-3.5 px-4 font-semibold text-slate-900">₹{Number(fare).toFixed(2)}</td>
                      <td className="py-3.5 px-4">
                        <span className="px-2.5 py-1 text-xs rounded-full font-semibold bg-emerald-100 text-emerald-800">
                          {status.replace(/_/g, ' ')}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

