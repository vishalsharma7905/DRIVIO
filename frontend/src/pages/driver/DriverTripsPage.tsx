import React, { useState, useEffect, useRef } from 'react';
import { bookingApi, trackingApi } from '../../services/api';
import { BookingResponse } from '../../types';
import { 
  MapPin, 
  Navigation, 
  CheckCircle2, 
  Play, 
  Flag, 
  XCircle, 
  Car, 
  Radio, 
  Clock, 
  Phone, 
  User, 
  AlertCircle
} from 'lucide-react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

export const DriverTripsPage: React.FC = () => {
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'ALL' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED'>('ALL');
  const [actionLoading, setActionLoading] = useState(false);
  const [isBroadcastingGps, setIsBroadcastingGps] = useState(false);
  const [currentCoords, setCurrentCoords] = useState<{ lat: number; lng: number }>({ lat: 28.6280, lng: 77.2180 });

  const mapRef = useRef<L.Map | null>(null);
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const driverMarkerRef = useRef<L.Marker | null>(null);
  const gpsTimerRef = useRef<any>(null);

  const fetchBookings = async () => {
    try {
      setLoading(true);
      const res = await bookingApi.getMyBookings();
      setBookings(res.data);
    } catch (err) {
      console.error('Failed to fetch bookings', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, []);

  const activeTrip = bookings.find(b => 
    ['DRIVER_ACCEPTED', 'DRIVER_ARRIVED', 'TRIP_STARTED'].includes(b.status)
  );

  // Initialize or update Map for active trip
  useEffect(() => {
    if (!activeTrip || !mapContainerRef.current) return;

    if (!mapRef.current) {
      const pickupLat = activeTrip.pickupLatitude || 28.6139;
      const pickupLng = activeTrip.pickupLongitude || 77.2090;

      const map = L.map(mapContainerRef.current).setView([pickupLat, pickupLng], 13);
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(map);

      // Pickup Marker
      L.marker([pickupLat, pickupLng], {
        icon: L.divIcon({
          className: 'custom-pin',
          html: `<div style="background-color: #10B981; color: white; width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; border: 2px solid white; box-shadow: 0 2px 4px rgba(0,0,0,0.3);">P</div>`,
          iconSize: [30, 30],
          iconAnchor: [15, 15]
        })
      }).addTo(map).bindPopup(`<b>Pickup:</b> ${activeTrip.pickupAddress}`);

      // Dropoff Marker
      if (activeTrip.dropoffLatitude && activeTrip.dropoffLongitude) {
        L.marker([activeTrip.dropoffLatitude, activeTrip.dropoffLongitude], {
          icon: L.divIcon({
            className: 'custom-pin',
            html: `<div style="background-color: #EF4444; color: white; width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; border: 2px solid white; box-shadow: 0 2px 4px rgba(0,0,0,0.3);">D</div>`,
            iconSize: [30, 30],
            iconAnchor: [15, 15]
          })
        }).addTo(map).bindPopup(`<b>Dropoff:</b> ${activeTrip.dropoffAddress}`);

        // Route line
        L.polyline([
          [pickupLat, pickupLng],
          [activeTrip.dropoffLatitude, activeTrip.dropoffLongitude]
        ], { color: '#3B82F6', weight: 4, dashArray: '6, 8' }).addTo(map);
      }

      // Driver position marker
      const driverMarker = L.marker([currentCoords.lat, currentCoords.lng], {
        icon: L.divIcon({
          className: 'driver-car-icon',
          html: `<div style="background-color: #1E293B; color: white; width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; border: 3px solid #10B981; box-shadow: 0 3px 6px rgba(0,0,0,0.4);">🚗</div>`,
          iconSize: [36, 36],
          iconAnchor: [18, 18]
        })
      }).addTo(map).bindPopup('<b>Your Current Location</b>');

      driverMarkerRef.current = driverMarker;
      mapRef.current = map;
    }

    return () => {
      // Map cleanup if unmounted
    };
  }, [activeTrip]);

  // GPS Simulation / Tracking Broadcast
  useEffect(() => {
    if (isBroadcastingGps && activeTrip) {
      gpsTimerRef.current = setInterval(async () => {
        // Move slightly towards dropoff or wiggle coordinates
        const targetLat = activeTrip.status === 'TRIP_STARTED' 
          ? (activeTrip.dropoffLatitude || 28.6139) 
          : (activeTrip.pickupLatitude || 28.6139);
        const targetLng = activeTrip.status === 'TRIP_STARTED' 
          ? (activeTrip.dropoffLongitude || 77.2090) 
          : (activeTrip.pickupLongitude || 77.2090);

        setCurrentCoords(prev => {
          const step = 0.0008; // Small increment
          const dLat = (targetLat - prev.lat) * 0.1;
          const dLng = (targetLng - prev.lng) * 0.1;
          const nextLat = prev.lat + dLat;
          const nextLng = prev.lng + dLng;

          // Update marker on map
          if (driverMarkerRef.current) {
            driverMarkerRef.current.setLatLng([nextLat, nextLng]);
          }

          // Send to backend via REST API
          trackingApi.updateLocation({
            bookingId: activeTrip.id,
            latitude: nextLat,
            longitude: nextLng,
            speedKmh: 35.5,
            headingDegrees: 45.0
          }).catch(e => console.error('GPS broadcast error', e));

          return { lat: nextLat, lng: nextLng };
        });
      }, 4000);
    } else {
      if (gpsTimerRef.current) {
        clearInterval(gpsTimerRef.current);
      }
    }

    return () => {
      if (gpsTimerRef.current) {
        clearInterval(gpsTimerRef.current);
      }
    };
  }, [isBroadcastingGps, activeTrip]);

  // Status transitions
  const handleMarkArrived = async (bookingId: number) => {
    try {
      setActionLoading(true);
      await bookingApi.driverArrived(bookingId);
      await fetchBookings();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update status');
    } finally {
      setActionLoading(false);
    }
  };

  const handleStartTrip = async (bookingId: number) => {
    try {
      setActionLoading(true);
      await bookingApi.startTrip(bookingId);
      setIsBroadcastingGps(true); // Automatically broadcast location when trip starts
      await fetchBookings();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to start trip');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCompleteTrip = async (bookingId: number) => {
    if (!window.confirm('Are you sure you want to finish this trip? Customer will proceed to payment.')) return;
    try {
      setActionLoading(true);
      setIsBroadcastingGps(false);
      await bookingApi.completeTrip(bookingId);
      await fetchBookings();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to complete trip');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCancelTrip = async (bookingId: number) => {
    const reason = window.prompt('Please provide a reason for cancelling this trip:');
    if (!reason) return;
    try {
      setActionLoading(true);
      setIsBroadcastingGps(false);
      await bookingApi.cancel(bookingId, { reason });
      await fetchBookings();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel trip');
    } finally {
      setActionLoading(false);
    }
  };

  // Filter bookings
  const filteredBookings = bookings.filter(b => {
    if (filter === 'ACTIVE') return ['DRIVER_ASSIGNED', 'DRIVER_ACCEPTED', 'DRIVER_ARRIVED', 'TRIP_STARTED'].includes(b.status);
    if (filter === 'COMPLETED') return b.status === 'TRIP_COMPLETED';
    if (filter === 'CANCELLED') return b.status === 'CANCELLED';
    return true;
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Driver Trips & Console</h1>
        <p className="text-sm text-slate-500 mt-1">
          Manage your ongoing trips, broadcast real-time GPS location, and review trip history.
        </p>
      </div>

      {/* Active Trip Console */}
      {activeTrip && (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
          <div className="bg-slate-900 text-white p-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
            <div>
              <div className="flex items-center space-x-2">
                <span className="bg-emerald-500 text-white text-xs px-2.5 py-0.5 rounded-full font-bold uppercase animate-pulse">
                  Active Mission
                </span>
                <span className="text-slate-400 text-xs font-mono">#{activeTrip.bookingReference || activeTrip.bookingNumber || `DRV-${activeTrip.id}`}</span>
              </div>
              <h2 className="text-xl font-bold mt-2">
                Status: {(activeTrip.bookingStatus || activeTrip.status || '').replace(/_/g, ' ')}
              </h2>
              <p className="text-sm text-slate-300 mt-1">
                {activeTrip.serviceType?.replace(/_/g, ' ')} • {activeTrip.vehicleType} • ₹{Number(activeTrip.totalFare ?? activeTrip.estimatedFare ?? 0).toFixed(2)}
              </p>
            </div>

            {/* GPS Broadcast Control */}
            <div className="flex items-center space-x-3 bg-slate-800 p-3 rounded-xl border border-slate-700">
              <div className="text-right">
                <span className="text-xs text-slate-400 block font-semibold uppercase">GPS Broadcast</span>
                <span className={`text-xs font-bold ${isBroadcastingGps ? 'text-emerald-400' : 'text-slate-400'}`}>
                  {isBroadcastingGps ? 'Transmitting Live' : 'Stopped'}
                </span>
              </div>
              <button
                onClick={() => setIsBroadcastingGps(!isBroadcastingGps)}
                className={`p-2.5 rounded-xl transition flex items-center justify-center ${
                  isBroadcastingGps ? 'bg-emerald-600 text-white' : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                }`}
                title="Toggle GPS broadcast"
              >
                <Radio className={`w-5 h-5 ${isBroadcastingGps ? 'animate-pulse' : ''}`} />
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3">
            {/* Map Container */}
            <div className="lg:col-span-2 h-96 relative bg-slate-100">
              <div ref={mapContainerRef} className="w-full h-full z-0" />
            </div>

            {/* Trip Action & Passenger Details */}
            <div className="p-6 space-y-6 flex flex-col justify-between border-t lg:border-t-0 lg:border-l border-slate-200">
              <div className="space-y-4">
                <h3 className="font-bold text-slate-900 text-base">Trip Details & Route</h3>

                <div className="space-y-3 text-sm">
                  <div className="flex items-start space-x-2">
                    <MapPin className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                    <div>
                      <span className="text-xs text-slate-400 block font-medium">PICKUP</span>
                      <span className="text-slate-800 font-semibold">{activeTrip.pickupAddress}</span>
                    </div>
                  </div>

                  <div className="flex items-start space-x-2">
                    <Navigation className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
                    <div>
                      <span className="text-xs text-slate-400 block font-medium">DESTINATION</span>
                      <span className="text-slate-800 font-semibold">{activeTrip.dropoffAddress}</span>
                    </div>
                  </div>

                  <div className="flex items-start space-x-2 pt-2 border-t border-slate-100">
                    <User className="w-4 h-4 text-purple-600 shrink-0 mt-0.5" />
                    <div>
                      <span className="text-xs text-slate-400 block font-medium">CUSTOMER</span>
                      <span className="text-slate-800 font-semibold">{activeTrip.customerName}</span>
                      <span className="text-xs text-slate-500 block">{activeTrip.customerPhone}</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Action Buttons based on status */}
              <div className="space-y-3 pt-4 border-t border-slate-200">
                {activeTrip.status === 'DRIVER_ACCEPTED' && (
                  <button
                    onClick={() => handleMarkArrived(activeTrip.id)}
                    disabled={actionLoading}
                    className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl shadow transition flex items-center justify-center space-x-2"
                  >
                    <CheckCircle2 className="w-5 h-5" />
                    <span>I Have Arrived at Pickup</span>
                  </button>
                )}

                {activeTrip.status === 'DRIVER_ARRIVED' && (
                  <button
                    onClick={() => handleStartTrip(activeTrip.id)}
                    disabled={actionLoading}
                    className="w-full py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl shadow transition flex items-center justify-center space-x-2"
                  >
                    <Play className="w-5 h-5" />
                    <span>Start Journey / Trip</span>
                  </button>
                )}

                {activeTrip.status === 'TRIP_STARTED' && (
                  <button
                    onClick={() => handleCompleteTrip(activeTrip.id)}
                    disabled={actionLoading}
                    className="w-full py-3 bg-emerald-700 hover:bg-emerald-800 text-white font-bold rounded-xl shadow transition flex items-center justify-center space-x-2"
                  >
                    <Flag className="w-5 h-5" />
                    <span>Complete Trip & Drop Passenger</span>
                  </button>
                )}

                <button
                  onClick={() => handleCancelTrip(activeTrip.id)}
                  disabled={actionLoading}
                  className="w-full py-2 bg-slate-100 hover:bg-rose-50 text-rose-600 font-semibold rounded-xl text-sm transition flex items-center justify-center space-x-1"
                >
                  <XCircle className="w-4 h-4" />
                  <span>Cancel Trip</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Trips History Table & Filter */}
      <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 space-y-6">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <h2 className="text-lg font-bold text-slate-900">Trip History</h2>

          {/* Filter Tabs */}
          <div className="flex items-center space-x-1 bg-slate-100 p-1 rounded-xl">
            {(['ALL', 'ACTIVE', 'COMPLETED', 'CANCELLED'] as const).map(tab => (
              <button
                key={tab}
                onClick={() => setFilter(tab)}
                className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition ${
                  filter === tab ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {tab}
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <div className="flex justify-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
          </div>
        ) : filteredBookings.length === 0 ? (
          <div className="text-center py-12 border-2 border-dashed border-slate-200 rounded-xl">
            <Car className="w-10 h-10 text-slate-300 mx-auto mb-2" />
            <p className="text-slate-600 font-medium">No trips found for this filter</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-slate-700 text-xs uppercase font-semibold">
                <tr>
                  <th className="py-3 px-4">Booking #</th>
                  <th className="py-3 px-4">Customer</th>
                  <th className="py-3 px-4">Route</th>
                  <th className="py-3 px-4">Service</th>
                  <th className="py-3 px-4">Fare</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredBookings.map(b => {
                  const bookingRef = b.bookingReference || b.bookingNumber || `DRV-${b.id}`;
                  const dropoff = b.destinationAddress || b.dropoffAddress || '';
                  const fare = b.totalFare ?? b.estimatedFare ?? 0;
                  const status = b.bookingStatus || b.status || 'PENDING';

                  return (
                    <tr key={b.id} className="hover:bg-slate-50/50">
                      <td className="py-3.5 px-4 font-mono font-medium text-slate-900">#{bookingRef}</td>
                      <td className="py-3.5 px-4">
                        <span className="font-semibold text-slate-900 block">{b.customerName}</span>
                        <span className="text-xs text-slate-400">{b.customerPhone}</span>
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="max-w-xs text-xs truncate">
                          <span className="text-emerald-700 font-medium">From:</span> {b.pickupAddress}
                        </div>
                        <div className="max-w-xs text-xs truncate mt-0.5">
                          <span className="text-rose-700 font-medium">To:</span> {dropoff}
                        </div>
                      </td>
                      <td className="py-3.5 px-4 text-xs">
                        <span className="font-medium text-slate-800">{b.serviceType?.replace(/_/g, ' ')}</span>
                        <span className="block text-slate-400">{b.vehicleType}</span>
                      </td>
                      <td className="py-3.5 px-4 font-bold text-slate-900">₹{Number(fare).toFixed(2)}</td>
                      <td className="py-3.5 px-4">
                        <span className={`px-2.5 py-1 text-xs rounded-full font-semibold ${
                          status === 'TRIP_COMPLETED' ? 'bg-emerald-100 text-emerald-800' :
                          status === 'CANCELLED' ? 'bg-rose-100 text-rose-800' :
                          'bg-blue-100 text-blue-800'
                        }`}>
                          {status.replace(/_/g, ' ')}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-xs text-slate-500">
                        {new Date(b.createdAt).toLocaleDateString()}
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

