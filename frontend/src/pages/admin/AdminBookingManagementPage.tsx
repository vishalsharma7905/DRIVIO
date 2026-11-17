import React, { useState, useEffect } from 'react';
import { adminApi } from '../../services/api';
import { BookingResponse, BookingStatus } from '../../types';
import { 
  Car, 
  Search, 
  MapPin, 
  Navigation, 
  Calendar, 
  IndianRupee, 
  User, 
  Eye, 
  X,
  CreditCard
} from 'lucide-react';

export const AdminBookingManagementPage: React.FC = () => {
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<BookingStatus | 'ALL'>('ALL');
  const [selectedBooking, setSelectedBooking] = useState<BookingResponse | null>(null);

  const fetchBookings = async () => {
    try {
      setLoading(true);
      const res = await adminApi.getAllBookings();
      setBookings(res.data);
    } catch (err) {
      console.error('Failed to load bookings', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, []);

  const filteredBookings = bookings.filter(b => {
    const status = b.bookingStatus || b.status;
    if (statusFilter !== 'ALL' && status !== statusFilter) return false;
    const q = search.toLowerCase();
    const ref = (b.bookingReference || b.bookingNumber || '').toLowerCase();
    const cust = (b.customerName || '').toLowerCase();
    const driver = (b.driverName || '').toLowerCase();
    const pickup = (b.pickupAddress || '').toLowerCase();
    const dropoff = (b.destinationAddress || b.dropoffAddress || '').toLowerCase();

    return ref.includes(q) ||
           cust.includes(q) ||
           driver.includes(q) ||
           pickup.includes(q) ||
           dropoff.includes(q);
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Platform Bookings Monitor</h1>
        <p className="text-sm text-slate-500 mt-1">
          Complete lifecycle surveillance of customer mobility bookings, driver dispatches, and fares.
        </p>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
        <div className="flex flex-wrap items-center gap-1 bg-slate-100 p-1 rounded-xl">
          {(['ALL', 'TRIP_STARTED', 'DRIVER_ASSIGNED', 'TRIP_COMPLETED', 'CANCELLED'] as const).map(tab => (
            <button
              key={tab}
              onClick={() => setStatusFilter(tab as any)}
              className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition ${
                statusFilter === tab ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {tab.replace(/_/g, ' ')}
            </button>
          ))}
        </div>

        <div className="relative w-full sm:w-64">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <input
            type="text"
            placeholder="Search booking #, customer, driver..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500"
          />
        </div>
      </div>

      {/* Bookings Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-16">
            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-emerald-600"></div>
          </div>
        ) : filteredBookings.length === 0 ? (
          <div className="text-center py-16 border-2 border-dashed border-slate-200 m-6 rounded-2xl">
            <Car className="w-12 h-12 text-slate-300 mx-auto mb-2" />
            <p className="text-slate-600 font-medium">No bookings found</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-slate-700 text-xs uppercase font-semibold">
                <tr>
                  <th className="py-3.5 px-6">Booking #</th>
                  <th className="py-3.5 px-6">Customer</th>
                  <th className="py-3.5 px-6">Driver Assigned</th>
                  <th className="py-3.5 px-6">Service & Vehicle</th>
                  <th className="py-3.5 px-6">Fare</th>
                  <th className="py-3.5 px-6">Status</th>
                  <th className="py-3.5 px-6 text-right">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredBookings.map(b => {
                  const bookingRef = b.bookingReference || b.bookingNumber || `DRV-${b.id}`;
                  const fare = b.totalFare ?? b.estimatedFare ?? 0;
                  const status = b.bookingStatus || b.status || 'PENDING';

                  return (
                    <tr key={b.id} className="hover:bg-slate-50/50">
                      <td className="py-4 px-6 font-mono font-semibold text-slate-900">
                        #{bookingRef}
                      </td>
                      <td className="py-4 px-6 font-medium text-slate-900">
                        {b.customerName}
                      </td>
                      <td className="py-4 px-6">
                        {b.driverName ? (
                          <span className="font-medium text-slate-900">{b.driverName}</span>
                        ) : (
                          <span className="text-xs text-slate-400 italic">Not assigned</span>
                        )}
                      </td>
                      <td className="py-4 px-6 text-xs">
                        <span className="font-semibold text-slate-800 block">{b.serviceType?.replace(/_/g, ' ')}</span>
                        <span className="text-slate-500">{b.vehicleType}</span>
                      </td>
                      <td className="py-4 px-6 font-bold text-slate-900">
                        ₹{Number(fare).toFixed(2)}
                      </td>
                      <td className="py-4 px-6">
                        <span className={`px-2.5 py-1 text-xs rounded-full font-semibold ${
                          status === 'TRIP_COMPLETED' ? 'bg-emerald-100 text-emerald-800' :
                          status === 'CANCELLED' ? 'bg-rose-100 text-rose-800' :
                          status === 'TRIP_STARTED' ? 'bg-blue-100 text-blue-800 font-bold' :
                          'bg-amber-100 text-amber-800'
                        }`}>
                          {status.replace(/_/g, ' ')}
                        </span>
                      </td>
                      <td className="py-4 px-6 text-right">
                        <button
                          onClick={() => setSelectedBooking(b)}
                          className="p-2 text-slate-400 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition"
                          title="View Full Booking Details"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Booking Details Modal */}
      {selectedBooking && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-xl w-full p-6 shadow-xl space-y-6 max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center pb-4 border-b border-slate-100">
              <div>
                <span className="text-xs font-mono font-bold text-emerald-600 block uppercase">Booking Details</span>
                <h3 className="text-xl font-bold text-slate-900">#{selectedBooking.bookingNumber}</h3>
              </div>
              <button
                onClick={() => setSelectedBooking(null)}
                className="p-2 text-slate-400 hover:text-slate-700 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4 text-sm">
              <div className="grid grid-cols-2 gap-4 bg-slate-50 p-4 rounded-xl border border-slate-100">
                <div>
                  <span className="text-xs text-slate-400 block font-semibold uppercase">Status</span>
                  <span className="font-bold text-slate-800">{selectedBooking.status.replace(/_/g, ' ')}</span>
                </div>
                <div>
                  <span className="text-xs text-slate-400 block font-semibold uppercase">Service Type</span>
                  <span className="font-bold text-slate-800">{selectedBooking.serviceType.replace(/_/g, ' ')}</span>
                </div>
                <div>
                  <span className="text-xs text-slate-400 block font-semibold uppercase">Vehicle Category</span>
                  <span className="font-bold text-slate-800">{selectedBooking.vehicleType}</span>
                </div>
                <div>
                  <span className="text-xs text-slate-400 block font-semibold uppercase">Distance</span>
                  <span className="font-bold text-slate-800">{selectedBooking.estimatedDistanceKm} km</span>
                </div>
              </div>

              <div className="space-y-3">
                <div className="flex items-start space-x-2">
                  <MapPin className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                  <div>
                    <span className="text-xs text-slate-400 block font-semibold uppercase">Pickup Address</span>
                    <span className="text-slate-800 font-medium">{selectedBooking.pickupAddress}</span>
                  </div>
                </div>

                <div className="flex items-start space-x-2">
                  <Navigation className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
                  <div>
                    <span className="text-xs text-slate-400 block font-semibold uppercase">Destination Address</span>
                    <span className="text-slate-800 font-medium">{selectedBooking.destinationAddress || selectedBooking.dropoffAddress}</span>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 pt-3 border-t border-slate-100">
                <div>
                  <span className="text-xs text-slate-400 block font-semibold uppercase">Customer</span>
                  <span className="text-slate-900 font-semibold block">{selectedBooking.customerName}</span>
                  <span className="text-xs text-slate-500">{selectedBooking.customerPhone}</span>
                </div>

                <div>
                  <span className="text-xs text-slate-400 block font-semibold uppercase">Assigned Driver</span>
                  <span className="text-slate-900 font-semibold block">
                    {selectedBooking.driverName || 'None assigned'}
                  </span>
                  <span className="text-xs text-slate-500">{selectedBooking.driverPhone}</span>
                </div>
              </div>

              <div className="bg-emerald-50 p-4 rounded-xl border border-emerald-100 flex justify-between items-center">
                <div>
                  <span className="text-xs text-emerald-800 font-semibold uppercase block">Total Estimated Fare</span>
                  <span className="text-xs text-emerald-600">Calculated per pricing engine</span>
                </div>
                <span className="text-2xl font-bold text-emerald-900">
                  ₹{Number(selectedBooking.totalFare ?? selectedBooking.estimatedFare ?? 0).toFixed(2)}
                </span>
              </div>
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setSelectedBooking(null)}
                className="px-5 py-2 bg-slate-900 text-white rounded-xl text-sm font-semibold hover:bg-slate-800"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

