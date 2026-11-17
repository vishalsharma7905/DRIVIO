import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Car, Calendar, MapPin, IndianRupee, ArrowUpRight, Search, Filter } from 'lucide-react';
import api from '../../services/api';
import { Booking } from '../../types';

export const CustomerTripsPage: React.FC = () => {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('ALL');

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

  const filtered = bookings.filter((b) => {
    const ref = b.bookingReference || b.bookingNumber || '';
    const dest = b.destinationAddress || b.dropoffAddress || '';
    const matchesSearch =
      ref.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (b.pickupAddress && b.pickupAddress.toLowerCase().includes(searchQuery.toLowerCase())) ||
      dest.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (b.driverName && b.driverName.toLowerCase().includes(searchQuery.toLowerCase()));

    const currentStatus = b.bookingStatus || b.status;
    const matchesStatus = filterStatus === 'ALL' || currentStatus === filterStatus;
    return matchesSearch && matchesStatus;
  });

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 py-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-white tracking-tight">Booking History</h1>
            <p className="text-xs text-slate-400 mt-1">Review all your previous trips, driver receipts, and active bookings</p>
          </div>
          <Link
            to="/book"
            className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs rounded-xl shadow-md transition-colors inline-flex items-center space-x-2"
          >
            <Car className="w-4 h-4" />
            <span>New Booking</span>
          </Link>
        </div>

        {/* Filter / Search Bar */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow-xl flex flex-col sm:flex-row gap-4">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by reference, driver name, pickup, or destination..."
              className="w-full bg-slate-950 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="flex items-center space-x-2">
            <Filter className="w-4 h-4 text-slate-500" />
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="ALL">All Statuses</option>
              <option value="TRIP_COMPLETED">Completed</option>
              <option value="TRIP_STARTED">In Progress</option>
              <option value="DRIVER_ACCEPTED">Accepted</option>
              <option value="DRIVER_ASSIGNED">Assigned</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
        </div>

        {/* Bookings Table / List */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
          {loading ? (
            <div className="py-12 text-center text-xs text-slate-500">Loading trips...</div>
          ) : filtered.length === 0 ? (
            <div className="py-12 text-center text-xs text-slate-400">No journeys matching your filters.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-slate-800 text-slate-400 font-medium">
                    <th className="py-3 px-4">Booking Ref</th>
                    <th className="py-3 px-4">Service & Vehicle</th>
                    <th className="py-3 px-4">Driver</th>
                    <th className="py-3 px-4">Pickup / Destination</th>
                    <th className="py-3 px-4">Scheduled Date</th>
                    <th className="py-3 px-4">Fare</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {filtered.map((b) => (
                    <tr key={b.id} className="hover:bg-slate-800/40 transition-colors">
                      <td className="py-3 px-4 font-mono font-medium text-slate-200">{b.bookingReference}</td>
                      <td className="py-3 px-4 text-slate-300">
                        <div className="font-semibold text-white capitalize">{b.serviceType.replace('_', ' ')}</div>
                        <div className="text-[10px] text-slate-400">{b.vehicleType}</div>
                      </td>
                      <td className="py-3 px-4 text-slate-200">
                        {b.driverName || <span className="text-slate-500 italic">Unassigned</span>}
                      </td>
                      <td className="py-3 px-4 max-w-xs truncate text-slate-400">
                        <div>📍 {b.pickupAddress}</div>
                        <div>🏁 {b.destinationAddress}</div>
                      </td>
                      <td className="py-3 px-4 text-slate-300">
                        {new Date(b.scheduledPickupTime).toLocaleDateString([], {
                          month: 'short',
                          day: 'numeric',
                          year: 'numeric',
                        })}
                      </td>
                      <td className="py-3 px-4 font-semibold text-emerald-400">₹{b.totalFare}</td>
                      <td className="py-3 px-4">
                        <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-slate-800 border border-slate-700 text-slate-300">
                          {b.bookingStatus}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <Link
                          to={`/track/${b.id}`}
                          className="px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white font-medium inline-flex items-center space-x-1"
                        >
                          <span>Track</span>
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
