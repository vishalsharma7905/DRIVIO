import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { 
  Car, Clock, MapPin, Navigation, Shield, 
  Sparkles, Star, ChevronRight, AlertCircle, 
  CheckCircle, ArrowUpRight, DollarSign 
} from 'lucide-react';
import api from '../../services/api';
import { Booking } from '../../types';

export const CustomerDashboard: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchBookings();
  }, []);

  const fetchBookings = async () => {
    try {
      const res = await api.get<Booking[]>('/customers/bookings');
      setBookings(res.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const activeBooking = bookings.find(
    (b) =>
      b.bookingStatus === 'DRIVER_ASSIGNED' ||
      b.bookingStatus === 'DRIVER_ACCEPTED' ||
      b.bookingStatus === 'DRIVER_ARRIVED' ||
      b.bookingStatus === 'TRIP_STARTED'
  );

  const completedTrips = bookings.filter((b) => b.bookingStatus === 'TRIP_COMPLETED');
  const totalSpent = completedTrips.reduce((acc, curr) => acc + (curr.totalFare || 0), 0);

  const getStatusBadge = (status: string = 'PENDING') => {
    switch (status) {
      case 'TRIP_COMPLETED':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">Completed</span>;
      case 'TRIP_STARTED':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-blue-500/10 text-blue-400 border border-blue-500/20 animate-pulse">Trip in Progress</span>;
      case 'DRIVER_ARRIVED':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">Driver Arrived</span>;
      case 'DRIVER_ACCEPTED':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">Driver En Route</span>;
      case 'DRIVER_ASSIGNED':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-purple-500/10 text-purple-400 border border-purple-500/20">Driver Assigned</span>;
      case 'CANCELLED':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">Cancelled</span>;
      default:
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-slate-700 text-slate-300">{status}</span>;
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
        
        {/* Welcome Banner */}
        <div className="bg-gradient-to-r from-blue-900/40 via-indigo-900/30 to-slate-900 border border-blue-500/20 rounded-3xl p-6 sm:p-8 flex flex-col md:flex-row items-start md:items-center justify-between gap-6 shadow-2xl">
          <div>
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-blue-500/20 text-blue-300 text-xs font-semibold mb-3">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Customer Portal</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white">
              Welcome back, {user?.fullName}!
            </h1>
            <p className="text-xs sm:text-sm text-slate-300 mt-1 max-w-xl">
              Book professional verified chauffeurs for your personal vehicles or track your active journeys in real-time.
            </p>
          </div>

          <div className="flex items-center space-x-3 w-full md:w-auto">
            <Link
              to="/book"
              className="flex-1 md:flex-initial px-6 py-3.5 bg-blue-600 hover:bg-blue-500 text-white text-xs sm:text-sm font-semibold rounded-xl flex items-center justify-center space-x-2 shadow-lg shadow-blue-600/30 transition-all"
            >
              <Car className="w-4 h-4" />
              <span>Book a Driver</span>
            </Link>
          </div>
        </div>

        {/* Active Trip Banner if currently on a trip */}
        {activeBooking && (
          <div className="bg-gradient-to-r from-blue-950/80 to-slate-900 border-2 border-blue-500/40 rounded-2xl p-6 shadow-2xl space-y-4 animate-in fade-in duration-300">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-800 gap-2">
              <div className="flex items-center space-x-2">
                <span className="w-2.5 h-2.5 rounded-full bg-blue-500 animate-ping"></span>
                <span className="text-xs font-bold text-blue-400 uppercase tracking-wider">
                  Active Ride: {activeBooking.bookingReference}
                </span>
              </div>
              <div className="flex items-center space-x-3">
                {getStatusBadge(activeBooking.bookingStatus)}
                <Link
                  to={`/track/${activeBooking.id}`}
                  className="px-4 py-1.5 bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold rounded-lg flex items-center space-x-1.5 shadow-md transition-colors"
                >
                  <Navigation className="w-3.5 h-3.5" />
                  <span>Live GPS Tracking</span>
                </Link>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
              <div className="space-y-1">
                <span className="text-slate-400">Assigned Driver:</span>
                <div className="text-sm font-bold text-white flex items-center space-x-1.5">
                  <span>{activeBooking.driverName || 'Finding Driver...'}</span>
                  {activeBooking.driverRating && (
                    <span className="text-amber-400 text-xs">★ {activeBooking.driverRating}</span>
                  )}
                </div>
                {activeBooking.driverPhone && (
                  <div className="text-slate-400 text-[11px]">📞 {activeBooking.driverPhone}</div>
                )}
              </div>

              <div className="space-y-1">
                <span className="text-slate-400">Route:</span>
                <div className="text-slate-200 line-clamp-1">📍 {activeBooking.pickupAddress}</div>
                <div className="text-slate-200 line-clamp-1">🏁 {activeBooking.destinationAddress}</div>
              </div>

              <div className="space-y-1 md:text-right">
                <span className="text-slate-400">Fare:</span>
                <div className="text-base font-bold text-emerald-400">₹{activeBooking.totalFare}</div>
                <div className="text-[11px] text-slate-400 capitalize">
                  {activeBooking.serviceType.replace('_', ' ')} • {activeBooking.vehicleType}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Metrics Row */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg">
            <div className="flex items-center justify-between text-slate-400 text-xs mb-2">
              <span>Completed Trips</span>
              <CheckCircle className="w-4 h-4 text-emerald-400" />
            </div>
            <div className="text-2xl font-bold text-white">{completedTrips.length}</div>
            <p className="text-[11px] text-slate-500 mt-1">Total journeys taken with DRIVIO</p>
          </div>

          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg">
            <div className="flex items-center justify-between text-slate-400 text-xs mb-2">
              <span>Total Spent</span>
              <DollarSign className="w-4 h-4 text-blue-400" />
            </div>
            <div className="text-2xl font-bold text-white">₹{totalSpent.toLocaleString()}</div>
            <p className="text-[11px] text-slate-500 mt-1">Transparent fare breakdown</p>
          </div>

          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg">
            <div className="flex items-center justify-between text-slate-400 text-xs mb-2">
              <span>All Bookings</span>
              <Clock className="w-4 h-4 text-purple-400" />
            </div>
            <div className="text-2xl font-bold text-white">{bookings.length}</div>
            <p className="text-[11px] text-slate-500 mt-1">Lifetime trip requests</p>
          </div>
        </div>

        {/* Recent Bookings List */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-800">
            <div>
              <h2 className="text-base font-bold text-white">Recent Journeys</h2>
              <p className="text-xs text-slate-400">View and track all your bookings</p>
            </div>
            <Link
              to="/customer/trips"
              className="text-xs text-blue-400 hover:text-blue-300 font-semibold flex items-center space-x-1"
            >
              <span>View All</span>
              <ChevronRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {loading ? (
            <div className="py-8 text-center text-xs text-slate-500">Loading trips...</div>
          ) : bookings.length === 0 ? (
            <div className="py-12 text-center space-y-3">
              <Car className="w-8 h-8 text-slate-600 mx-auto" />
              <p className="text-xs text-slate-400">You haven't booked any drivers yet.</p>
              <Link
                to="/book"
                className="inline-flex px-4 py-2 bg-blue-600 text-white rounded-lg text-xs font-semibold hover:bg-blue-500 transition-colors"
              >
                Book Your First Ride
              </Link>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-slate-800 text-slate-400 font-medium">
                    <th className="py-3 px-4">Booking Ref</th>
                    <th className="py-3 px-4">Service</th>
                    <th className="py-3 px-4">Driver</th>
                    <th className="py-3 px-4">Route</th>
                    <th className="py-3 px-4">Fare</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {bookings.slice(0, 5).map((b) => (
                    <tr key={b.id} className="hover:bg-slate-800/40 transition-colors">
                      <td className="py-3 px-4 font-mono font-medium text-slate-200">{b.bookingReference}</td>
                      <td className="py-3 px-4 capitalize text-slate-300">{b.serviceType.replace('_', ' ')}</td>
                      <td className="py-3 px-4 text-slate-200">
                        {b.driverName || <span className="text-slate-500 italic">Unassigned</span>}
                      </td>
                      <td className="py-3 px-4 max-w-xs truncate text-slate-400" title={`${b.pickupAddress} -> ${b.destinationAddress}`}>
                        {b.pickupAddress} → {b.destinationAddress}
                      </td>
                      <td className="py-3 px-4 font-semibold text-emerald-400">₹{b.totalFare}</td>
                      <td className="py-3 px-4">{getStatusBadge(b.bookingStatus)}</td>
                      <td className="py-3 px-4 text-right">
                        <Link
                          to={`/track/${b.id}`}
                          className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium inline-flex items-center space-x-1"
                        >
                          <span>Details</span>
                          <ArrowUpRight className="w-3.5 h-3.5" />
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

      </div>
    </div>
  );
};
