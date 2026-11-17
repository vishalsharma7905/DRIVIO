import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Car, Shield, Phone, MapPin, Navigation, 
  CheckCircle, Clock, AlertCircle, Star, DollarSign,
  ChevronRight, RefreshCw, XCircle
} from 'lucide-react';
import api from '../../services/api';
import { socketService } from '../../services/socket';
import { Booking, LiveTracking, PaymentMethod } from '../../types';

export const LiveTrackingPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [booking, setBooking] = useState<Booking | null>(null);
  const [tracking, setTracking] = useState<LiveTracking | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Payment modal state
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CASH');
  const [paying, setPaying] = useState(false);

  // Review modal state
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [rating, setRating] = useState<number>(5);
  const [reviewText, setReviewText] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);

  // Leaflet map container ref
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<any>(null);
  const driverMarkerRef = useRef<any>(null);

  useEffect(() => {
    if (id) {
      fetchBookingDetails();
      fetchTrackingInfo();

      // Subscribe to real-time WebSocket driver updates
      socketService.connect(() => {
        socketService.subscribe(`/topic/tracking/${id}`, (data: LiveTracking) => {
          setTracking(data);
          const newStatus = data.bookingStatus || data.status;
          if (newStatus) {
            setBooking((prev) => prev ? { ...prev, bookingStatus: newStatus, status: newStatus } : null);
            if (newStatus === 'TRIP_COMPLETED') {
              setShowPaymentModal(true);
            }
          }
          if (data.driverLatitude != null && data.driverLongitude != null) {
            updateMapDriverPosition(data.driverLatitude, data.driverLongitude);
          }
        });
      });
    }

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, [id]);

  const fetchBookingDetails = async () => {
    try {
      const res = await api.get<Booking>(`/bookings/${id}`);
      setBooking(res.data);
      if (res.data.bookingStatus === 'TRIP_COMPLETED' && res.data.paymentStatus === 'PENDING') {
        setShowPaymentModal(true);
      }
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to load booking');
    } finally {
      setLoading(false);
    }
  };

  const fetchTrackingInfo = async () => {
    try {
      const res = await api.get<LiveTracking>(`/tracking/${id}`);
      setTracking(res.data);
      initMap(res.data);
    } catch (e) {
      console.error(e);
    }
  };

  // Initialize Leaflet Map dynamically
  const initMap = async (trackData: LiveTracking) => {
    if (!mapContainerRef.current || mapInstanceRef.current) return;

    try {
      const L = await import('leaflet');
      await import('leaflet/dist/leaflet.css');

      const pickupLat = trackData.pickupLatitude || 28.6280;
      const pickupLng = trackData.pickupLongitude || 77.2180;
      const destLat = trackData.destinationLatitude || 28.6250;
      const destLng = trackData.destinationLongitude || 77.3750;

      const map = L.map(mapContainerRef.current).setView([pickupLat, pickupLng], 12);
      mapInstanceRef.current = map;

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
      }).addTo(map);

      // Pickup Marker (Green)
      const pickupIcon = L.divIcon({
        className: 'custom-pin',
        html: `<div style="background-color: #10B981; width: 14px; height: 14px; border-radius: 50%; border: 3px solid white; box-shadow: 0 0 10px rgba(0,0,0,0.5);"></div>`,
        iconSize: [14, 14],
      });
      L.marker([pickupLat, pickupLng], { icon: pickupIcon })
        .addTo(map)
        .bindPopup(`<b>Pickup:</b> ${trackData.pickupAddress || 'Pickup Point'}`)
        .openPopup();

      // Destination Marker (Red)
      const destIcon = L.divIcon({
        className: 'custom-pin',
        html: `<div style="background-color: #EF4444; width: 14px; height: 14px; border-radius: 50%; border: 3px solid white; box-shadow: 0 0 10px rgba(0,0,0,0.5);"></div>`,
        iconSize: [14, 14],
      });
      L.marker([destLat, destLng], { icon: destIcon })
        .addTo(map)
        .bindPopup(`<b>Destination:</b> ${trackData.destinationAddress || 'Destination'}`);

      // Driver Position Marker (Blue with pulsing effect)
      const driverLat = trackData.driverLatitude || pickupLat;
      const driverLng = trackData.driverLongitude || pickupLng;

      const driverIcon = L.divIcon({
        className: 'driver-marker',
        html: `<div style="background-color: #3B82F6; width: 18px; height: 18px; border-radius: 50%; border: 3px solid white; box-shadow: 0 0 12px #3B82F6;"></div>`,
        iconSize: [18, 18],
      });

      const driverMarker = L.marker([driverLat, driverLng], { icon: driverIcon }).addTo(map);
      driverMarker.bindPopup(`<b>${trackData.driverName || 'Driver'}</b><br/>Speed: ${trackData.speed || 30} km/h`);
      driverMarkerRef.current = driverMarker;

      // Draw Route Polyline connecting points
      const routeLine = L.polyline(
        [
          [pickupLat, pickupLng],
          [driverLat, driverLng],
          [destLat, destLng],
        ],
        { color: '#3B82F6', weight: 4, dashArray: '6, 8', opacity: 0.8 }
      ).addTo(map);

      map.fitBounds(routeLine.getBounds(), { padding: [50, 50] });
    } catch (e) {
      console.error('Error initializing Leaflet map:', e);
    }
  };

  const updateMapDriverPosition = (lat?: number, lng?: number) => {
    if (!lat || !lng || !driverMarkerRef.current) return;
    driverMarkerRef.current.setLatLng([lat, lng]);
  };

  const handleProcessPayment = async () => {
    if (!booking) return;
    setPaying(true);
    try {
      await api.post('/payments/process', {
        bookingId: booking.id,
        paymentMethod,
      });
      setShowPaymentModal(false);
      setShowReviewModal(true);
      fetchBookingDetails();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Payment processing failed');
    } finally {
      setPaying(false);
    }
  };

  const handleSubmitReview = async () => {
    if (!booking) return;
    setSubmittingReview(true);
    try {
      await api.post('/reviews', {
        bookingId: booking.id,
        rating,
        reviewText,
      });
      setShowReviewModal(false);
      fetchBookingDetails();
      alert('Thank you for rating your driver!');
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to submit review');
    } finally {
      setSubmittingReview(false);
    }
  };

  const handleCancelBooking = async () => {
    if (!booking) return;
    const reason = prompt('Please enter your reason for cancelling:');
    if (!reason) return;

    try {
      await api.post(`/bookings/${booking.id}/cancel`, { cancellationReason: reason });
      fetchBookingDetails();
    } catch (e: any) {
      alert(e.response?.data?.message || 'Failed to cancel booking');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center text-slate-400 text-xs">
        <div className="flex flex-col items-center space-y-2">
          <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
          <span>Loading trip tracking data...</span>
        </div>
      </div>
    );
  }

  if (error || !booking) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center p-4">
        <div className="max-w-md w-full bg-slate-900 border border-slate-800 rounded-2xl p-6 text-center space-y-4">
          <AlertCircle className="w-10 h-10 text-rose-500 mx-auto" />
          <h2 className="text-lg font-bold text-white">Booking Not Found</h2>
          <p className="text-xs text-slate-400">{error || 'Could not locate the requested trip details.'}</p>
          <button
            onClick={() => navigate('/customer/dashboard')}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold rounded-lg"
          >
            Return to Dashboard
          </button>
        </div>
      </div>
    );
  }

  const steps = [
    { key: 'DRIVER_ASSIGNED', label: 'Assigned' },
    { key: 'DRIVER_ACCEPTED', label: 'Accepted' },
    { key: 'DRIVER_ARRIVED', label: 'Arrived' },
    { key: 'TRIP_STARTED', label: 'Trip Started' },
    { key: 'TRIP_COMPLETED', label: 'Completed' },
  ];

  const currentStepIndex = steps.findIndex((s) => s.key === booking.bookingStatus);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
        
        {/* Top Header Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center space-x-2">
              <span className="text-xs font-mono font-bold text-blue-400">
                {booking.bookingReference}
              </span>
              <span className="text-[10px] uppercase font-bold px-2 py-0.5 rounded-full bg-slate-800 border border-slate-700 text-slate-300">
                {booking.serviceType.replace('_', ' ')} • {booking.vehicleType}
              </span>
            </div>
            <h1 className="text-xl font-bold text-white mt-1">Live Trip Tracking & Status</h1>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={() => {
                fetchBookingDetails();
                fetchTrackingInfo();
              }}
              className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
              title="Refresh"
            >
              <RefreshCw className="w-4 h-4" />
            </button>

            {booking.bookingStatus !== 'TRIP_COMPLETED' && booking.bookingStatus !== 'CANCELLED' && (
              <button
                onClick={handleCancelBooking}
                className="px-3 py-1.5 rounded-lg bg-rose-600/10 hover:bg-rose-600/20 text-rose-400 border border-rose-500/30 text-xs font-semibold transition-colors"
              >
                Cancel Ride
              </button>
            )}

            {booking.bookingStatus === 'TRIP_COMPLETED' && booking.paymentStatus === 'PENDING' && (
              <button
                onClick={() => setShowPaymentModal(true)}
                className="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-md transition-colors"
              >
                Pay ₹{booking.totalFare}
              </button>
            )}

            {booking.bookingStatus === 'TRIP_COMPLETED' && !booking.rated && (
              <button
                onClick={() => setShowReviewModal(true)}
                className="px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-500 text-white text-xs font-bold shadow-md transition-colors"
              >
                Rate Driver ★
              </button>
            )}
          </div>
        </div>

        {/* Step Progress Tracker */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl">
          <div className="flex items-center justify-between max-w-3xl mx-auto relative">
            {steps.map((step, idx) => {
              const isPastOrCurrent = currentStepIndex >= idx;
              const isCurrent = currentStepIndex === idx;

              return (
                <div key={step.key} className="flex flex-col items-center relative z-10">
                  <div
                    className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs transition-all ${
                      isPastOrCurrent
                        ? 'bg-blue-600 text-white shadow-lg shadow-blue-500/30'
                        : 'bg-slate-800 text-slate-500'
                    } ${isCurrent ? 'ring-4 ring-blue-500/20 animate-pulse' : ''}`}
                  >
                    {idx + 1}
                  </div>
                  <span
                    className={`text-[11px] font-medium mt-1.5 ${
                      isPastOrCurrent ? 'text-slate-200' : 'text-slate-500'
                    }`}
                  >
                    {step.label}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Main Grid: Leaflet Map Left, Details Card Right */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          
          {/* Map Column (8 cols) */}
          <div className="lg:col-span-8 bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow-xl space-y-3">
            <div className="flex items-center justify-between text-xs text-slate-400 px-2">
              <div className="flex items-center space-x-2">
                <Navigation className="w-4 h-4 text-blue-400" />
                <span>Interactive Route & GPS Tracking</span>
              </div>
              <span className="flex items-center space-x-1 text-emerald-400">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
                <span>Live Feed</span>
              </span>
            </div>

            {/* Map Container */}
            <div
              ref={mapContainerRef}
              className="w-full h-[450px] rounded-xl overflow-hidden shadow-inner bg-slate-950 border border-slate-800"
            />
          </div>

          {/* Details Column (4 cols) */}
          <div className="lg:col-span-4 space-y-6">
            
            {/* Driver Profile Card */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl space-y-4">
              <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                Assigned Driver Information
              </h3>

              {booking.driverName ? (
                <div className="space-y-4">
                  <div className="flex items-center space-x-3">
                    <img
                      src={booking.driverPhotoUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150'}
                      alt={booking.driverName}
                      className="w-14 h-14 rounded-full object-cover border-2 border-blue-500"
                    />
                    <div>
                      <h4 className="text-sm font-bold text-white flex items-center space-x-1.5">
                        <span>{booking.driverName}</span>
                        <CheckCircle className="w-4 h-4 text-emerald-400" />
                      </h4>
                      <div className="flex items-center space-x-2 text-xs text-slate-400 mt-0.5">
                        <span className="text-amber-400 font-semibold flex items-center">
                          ★ {booking.driverRating || '5.0'}
                        </span>
                        <span>•</span>
                        <span>Verified Chauffeur</span>
                      </div>
                    </div>
                  </div>

                  {booking.driverPhone && (
                    <a
                      href={`tel:${booking.driverPhone}`}
                      className="w-full py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-xs font-semibold flex items-center justify-center space-x-2 border border-slate-700 transition-colors"
                    >
                      <Phone className="w-3.5 h-3.5 text-emerald-400" />
                      <span>Call Driver ({booking.driverPhone})</span>
                    </a>
                  )}
                </div>
              ) : (
                <div className="py-6 text-center text-xs text-slate-400">
                  <Car className="w-8 h-8 text-slate-600 mx-auto mb-2 animate-bounce" />
                  <span>Matching optimal driver via recommendation engine...</span>
                </div>
              )}
            </div>

            {/* Route & Billing Card */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl space-y-4 text-xs">
              <h3 className="font-bold text-slate-400 uppercase tracking-wider text-[11px]">
                Trip Coordinates & Summary
              </h3>

              <div className="space-y-3">
                <div className="flex items-start space-x-2">
                  <div className="w-2.5 h-2.5 rounded-full bg-emerald-500 mt-1 flex-shrink-0"></div>
                  <div>
                    <span className="text-slate-400 text-[10px] block">PICKUP</span>
                    <span className="text-slate-200 font-medium">{booking.pickupAddress}</span>
                  </div>
                </div>

                <div className="flex items-start space-x-2">
                  <div className="w-2.5 h-2.5 rounded-full bg-rose-500 mt-1 flex-shrink-0"></div>
                  <div>
                    <span className="text-slate-400 text-[10px] block">DESTINATION</span>
                    <span className="text-slate-200 font-medium">{booking.destinationAddress}</span>
                  </div>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-800 space-y-1.5">
                <div className="flex justify-between text-slate-400">
                  <span>Scheduled Time:</span>
                  <span className="text-slate-200 font-medium">
                    {new Date(booking.scheduledPickupTime).toLocaleString([], {
                      month: 'short',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Estimated Distance:</span>
                  <span className="text-slate-200 font-medium">{booking.estimatedDistanceKm} km</span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Payment Status:</span>
                  <span className="font-semibold text-emerald-400">{booking.paymentStatus}</span>
                </div>
                <div className="flex justify-between text-slate-400 pt-2 border-t border-slate-800/80">
                  <span className="font-bold text-white">Final Total Fare:</span>
                  <span className="text-base font-extrabold text-emerald-400">₹{booking.totalFare}</span>
                </div>
              </div>
            </div>

          </div>

        </div>

      </div>

      {/* Mock Payment Modal */}
      {showPaymentModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex justify-between items-center pb-3 border-b border-slate-800">
              <h3 className="text-base font-bold text-white flex items-center space-x-2">
                <DollarSign className="w-5 h-5 text-emerald-400" />
                <span>Complete Trip Payment</span>
              </h3>
              <button
                onClick={() => setShowPaymentModal(false)}
                className="text-slate-500 hover:text-white"
              >
                ✕
              </button>
            </div>

            <div className="text-center py-2">
              <span className="text-xs text-slate-400">Total Payable:</span>
              <div className="text-3xl font-extrabold text-emerald-400">₹{booking.totalFare}</div>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-300 block">Select Payment Mode:</label>
              {(['CASH', 'MOCK_UPI', 'MOCK_CARD'] as PaymentMethod[]).map((mode) => (
                <button
                  key={mode}
                  type="button"
                  onClick={() => setPaymentMethod(mode)}
                  className={`w-full p-3 rounded-xl border text-xs font-semibold flex items-center justify-between transition-colors ${
                    paymentMethod === mode
                      ? 'bg-blue-600/20 border-blue-500 text-blue-300 ring-1 ring-blue-500'
                      : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                  }`}
                >
                  <span>
                    {mode === 'CASH' && '💵 Cash to Driver'}
                    {mode === 'MOCK_UPI' && '📱 Instant UPI / QR (Mock)'}
                    {mode === 'MOCK_CARD' && '💳 Credit / Debit Card (Mock)'}
                  </span>
                  {paymentMethod === mode && <CheckCircle className="w-4 h-4 text-blue-400" />}
                </button>
              ))}
            </div>

            <button
              onClick={handleProcessPayment}
              disabled={paying}
              className="w-full py-3 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-xl text-xs flex items-center justify-center space-x-2 shadow-lg transition-colors"
            >
              {paying ? (
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              ) : (
                <span>Confirm Payment of ₹{booking.totalFare}</span>
              )}
            </button>
          </div>
        </div>
      )}

      {/* Review Modal */}
      {showReviewModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex justify-between items-center pb-3 border-b border-slate-800">
              <h3 className="text-base font-bold text-white flex items-center space-x-2">
                <Star className="w-5 h-5 text-amber-400" />
                <span>Rate Your Chauffeur</span>
              </h3>
              <button
                onClick={() => setShowReviewModal(false)}
                className="text-slate-500 hover:text-white"
              >
                ✕
              </button>
            </div>

            <div className="text-center py-2 space-y-2">
              <p className="text-xs text-slate-300">
                How was your journey with <span className="font-semibold text-white">{booking.driverName}</span>?
              </p>
              {/* Star Rating input */}
              <div className="flex justify-center space-x-2">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRating(star)}
                    className="p-1 focus:outline-none"
                  >
                    <Star
                      className={`w-7 h-7 transition-colors ${
                        star <= rating ? 'text-amber-400 fill-amber-400' : 'text-slate-700'
                      }`}
                    />
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="text-xs font-medium text-slate-300 block mb-1">
                Feedback Comments (Optional)
              </label>
              <textarea
                rows={3}
                value={reviewText}
                onChange={(e) => setReviewText(e.target.value)}
                placeholder="Punctual, professional driving, smooth car handling..."
                className="w-full bg-slate-950 border border-slate-700 rounded-xl p-3 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-amber-500 resize-none"
              />
            </div>

            <button
              onClick={handleSubmitReview}
              disabled={submittingReview}
              className="w-full py-3 bg-amber-600 hover:bg-amber-500 text-white font-bold rounded-xl text-xs flex items-center justify-center space-x-2 shadow-lg transition-colors"
            >
              {submittingReview ? (
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              ) : (
                <span>Submit Driver Review</span>
              )}
            </button>
          </div>
        </div>
      )}

    </div>
  );
};

