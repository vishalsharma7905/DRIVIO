import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { driverApi, bookingApi, driverEarningsApi } from '../../services/api';
import { DriverProfile, BookingResponse, DriverBalanceSummary, DriverSettlementRecord } from '../../types';
import { 
  Car, 
  IndianRupee, 
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
  const [balances, setBalances] = useState<DriverBalanceSummary | null>(null);
  const [settlements, setSettlements] = useState<DriverSettlementRecord[]>([]);
  const [withdrawAmount, setWithdrawAmount] = useState<string>('');
  const [settling, setSettling] = useState(false);
  const [loading, setLoading] = useState(true);
  const [togglingStatus, setTogglingStatus] = useState(false);
  const [actionInProgress, setActionInProgress] = useState<number | null>(null);

  const fetchDriverData = async () => {
    try {
      setLoading(true);
      const [profileRes, bookingsRes, balRes, stlRes] = await Promise.all([
        driverApi.getProfile(),
        bookingApi.getMyBookings(),
        driverEarningsApi.getSummary().catch(() => ({ data: null })),
        driverEarningsApi.getSettlements().catch(() => ({ data: [] })),
      ]);
      setProfile(profileRes.data);
      if (profileRes.data.availability) {
        setIsAvailable(profileRes.data.availability.isAvailable);
      }
      setBookings(bookingsRes.data);
      if (balRes.data) setBalances(balRes.data);
      if (stlRes.data) setSettlements(stlRes.data);
    } catch (err) {
      console.error('Failed to load driver dashboard data', err);
    } finally {
      setLoading(false);
    }
  };

  const handleRequestSettlement = async (e: React.FormEvent) => {
    e.preventDefault();
    const amt = parseFloat(withdrawAmount);
    if (!amt || amt <= 0) return;
    try {
      setSettling(true);
      await driverEarningsApi.requestSettlement(amt);
      setWithdrawAmount('');
      await fetchDriverData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Settlement request failed');
    } finally {
      setSettling(false);
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
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6 text-neutral-100">
      {/* Centered Driver Hub Card (Screen 2 Aesthetic) */}
      <div className="bg-[#1C1F1D] rounded-3xl p-6 sm:p-8 shadow-xl border border-[#2B2F2C] flex flex-col items-center text-center space-y-4">
        <div className="w-20 h-20 rounded-full bg-emerald-500/20 border-2 border-emerald-500 flex items-center justify-center text-2xl font-bold text-emerald-400">
          {user?.fullName?.charAt(0) || 'D'}
        </div>
        <div>
          <div className="flex items-center justify-center space-x-2">
            <h1 className="text-2xl font-bold text-white">{user?.fullName}</h1>
            <span className={`px-2.5 py-0.5 text-[11px] font-semibold rounded-full flex items-center ${
              verificationStatus === 'VERIFIED'
                ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                : verificationStatus === 'REJECTED'
                ? 'bg-rose-500/15 text-rose-400 border border-rose-500/30'
                : 'bg-amber-500/15 text-amber-400 border border-amber-500/30'
            }`}>
              <ShieldCheck className="w-3 h-3 mr-1" />
              {verificationStatus}
            </span>
          </div>
          <p className="text-sm text-neutral-400 mt-1 flex items-center justify-center space-x-2">
            <span className="text-amber-400 font-semibold flex items-center">
              <Star className="w-4 h-4 fill-amber-400 mr-1" />
              {(profile?.rating ?? 4.95).toFixed(2)}
            </span>
            <span>·</span>
            <span>{profile?.totalTrips ?? completedTrips.length} trips</span>
          </p>
        </div>

        {/* Full-Width Vibrant Green Go Online / Go Offline Button */}
        <button
          onClick={handleToggleAvailability}
          disabled={togglingStatus || verificationStatus !== 'VERIFIED'}
          className={`w-full max-w-md py-3.5 rounded-2xl font-bold text-sm transition flex items-center justify-center space-x-2 shadow-lg ${
            isAvailable
              ? 'bg-emerald-500 hover:bg-emerald-400 text-[#09130C] shadow-emerald-500/20'
              : 'bg-[#262A27] hover:bg-[#2F3431] text-white border border-[#383E3A]'
          } ${verificationStatus !== 'VERIFIED' ? 'opacity-50 cursor-not-allowed' : ''}`}
          title={verificationStatus !== 'VERIFIED' ? 'Account must be verified by admin first' : 'Toggle online status'}
        >
          <Power className="w-4 h-4" />
          <span>{isAvailable ? 'Online — Tap to Go Offline' : 'Go online'}</span>
        </button>

        {/* 3 Quick Action Tiles */}
        <div className="grid grid-cols-3 gap-3 w-full max-w-md pt-2">
          <Link to="/driver/trips" className="bg-[#242825] hover:bg-[#2C312D] border border-[#323733] rounded-2xl p-3 flex flex-col items-center space-y-1.5 transition">
            <Navigation className="w-5 h-5 text-white" />
            <span className="text-xs font-medium text-neutral-300">My Trips</span>
          </Link>
          <div className="bg-[#242825] border border-[#323733] rounded-2xl p-3 flex flex-col items-center space-y-1.5">
            <IndianRupee className="w-5 h-5 text-amber-400" />
            <span className="text-xs font-medium text-neutral-300">₹{Number(profile?.totalEarnings ?? 0).toFixed(0)}</span>
          </div>
          <Link to="/driver/verification" className="bg-[#242825] hover:bg-[#2C312D] border border-[#323733] rounded-2xl p-3 flex flex-col items-center space-y-1.5 transition">
            <Star className="w-5 h-5 text-emerald-400" />
            <span className="text-xs font-medium text-neutral-300">KYC & Docs</span>
          </Link>
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

      {/* Four Distinct Financial Balances (Phase 4) */}
      {balances && (
        <div className="bg-[#1C1F1D] rounded-3xl p-6 border border-[#2B2F2C] space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h2 className="text-lg font-bold text-white">Driver Wallet & Four Balances</h2>
              <p className="text-xs text-neutral-400">
                Only Available earnings (from captured trips) are eligible for settlement withdrawal.
              </p>
            </div>
            <form onSubmit={handleRequestSettlement} className="flex items-center space-x-2">
              <input
                type="number"
                min="1"
                step="0.01"
                max={balances.available}
                value={withdrawAmount}
                onChange={(e) => setWithdrawAmount(e.target.value)}
                placeholder="Amount (₹)"
                className="w-32 px-3 py-2 bg-[#242825] border border-[#323733] rounded-xl text-xs text-white"
              />
              <button
                type="submit"
                disabled={settling || balances.available <= 0}
                className="px-4 py-2 bg-emerald-500 hover:bg-emerald-400 disabled:opacity-50 text-[#09130C] font-bold text-xs rounded-xl transition"
              >
                {settling ? 'Requesting...' : 'Withdraw'}
              </button>
            </form>
          </div>

          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-[#242825] rounded-2xl p-4 border border-[#323733]">
              <span className="text-[11px] font-semibold text-neutral-400 uppercase">Accrued (Pending Capture)</span>
              <p className="text-xl font-bold text-amber-400 mt-1">₹{Number(balances.accrued).toFixed(2)}</p>
            </div>
            <div className="bg-[#242825] rounded-2xl p-4 border border-emerald-500/30">
              <span className="text-[11px] font-semibold text-neutral-400 uppercase">Available (Withdrawable)</span>
              <p className="text-xl font-bold text-emerald-400 mt-1">₹{Number(balances.available).toFixed(2)}</p>
            </div>
            <div className="bg-[#242825] rounded-2xl p-4 border border-[#323733]">
              <span className="text-[11px] font-semibold text-neutral-400 uppercase">Reserved (In Settlement)</span>
              <p className="text-xl font-bold text-blue-400 mt-1">₹{Number(balances.reserved).toFixed(2)}</p>
            </div>
            <div className="bg-[#242825] rounded-2xl p-4 border border-[#323733]">
              <span className="text-[11px] font-semibold text-neutral-400 uppercase">Settled (Transferred)</span>
              <p className="text-xl font-bold text-white mt-1">₹{Number(balances.settled).toFixed(2)}</p>
            </div>
          </div>

          {settlements.length > 0 && (
            <div className="pt-2 border-t border-[#2B2F2C] space-y-2">
              <span className="text-xs font-semibold text-neutral-400 uppercase">Recent Settlements</span>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
                {settlements.slice(0, 3).map((s) => (
                  <div key={s.id} className="bg-[#242825] rounded-xl p-3 text-xs flex items-center justify-between border border-[#323733]">
                    <div>
                      <span className="font-bold text-white">₹{Number(s.totalAmount).toFixed(2)}</span>
                      <span className="block text-[10px] text-neutral-400">{new Date(s.initiatedAt).toLocaleDateString()}</span>
                    </div>
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                      s.status === 'COMPLETED' ? 'bg-emerald-500/15 text-emerald-400' :
                      s.status === 'FAILED' ? 'bg-rose-500/15 text-rose-400' : 'bg-amber-500/15 text-amber-400'
                    }`}>{s.status}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Earnings</span>
            <div className="p-2.5 bg-emerald-50 text-emerald-600 rounded-xl">
              <IndianRupee className="w-5 h-5" />
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

