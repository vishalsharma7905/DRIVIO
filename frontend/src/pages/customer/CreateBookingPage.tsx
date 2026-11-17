import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { 
  Car, Sparkles, MapPin, Calendar, Clock, 
  Shield, CheckCircle, ArrowRight, IndianRupee, 
  Compass, Navigation, AlertCircle, Star, Info
} from 'lucide-react';
import api from '../../services/api';
import { 
  ServiceType, VehicleType, FareEstimate, 
  DriverRecommendation, AiExtractionResult 
} from '../../types';

export const CreateBookingPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  // AI assistant state
  const [aiPrompt, setAiPrompt] = useState('');
  const [aiLoading, setAiLoading] = useState(false);
  const [aiNotice, setAiNotice] = useState<string | null>(null);

  // Booking Form State
  const [serviceType, setServiceType] = useState<ServiceType>('OWN_CAR_DRIVER');
  const [vehicleType, setVehicleType] = useState<VehicleType>('SEDAN');
  const [pickupAddress, setPickupAddress] = useState('Connaught Place, New Delhi');
  const [pickupLat, setPickupLat] = useState<number>(28.6280);
  const [pickupLng, setPickupLng] = useState<number>(77.2180);

  const [destinationAddress, setDestinationAddress] = useState('Sector 62, Noida');
  const [destLat, setDestLat] = useState<number>(28.6250);
  const [destLng, setDestLng] = useState<number>(77.3750);

  const [pickupDate, setPickupDate] = useState<string>(
    new Date(Date.now() + 86400000).toISOString().split('T')[0]
  );
  const [pickupTime, setPickupTime] = useState<string>('09:00');
  const [estimatedDistance, setEstimatedDistance] = useState<number>(25.0);
  const [estimatedDuration, setEstimatedDuration] = useState<number>(50);
  const [specialInstructions, setSpecialInstructions] = useState('');

  // Fare and Driver Recommendation State
  const [fareEstimate, setFareEstimate] = useState<FareEstimate | null>(null);
  const [estimatingFare, setEstimatingFare] = useState(false);
  const [recommendations, setRecommendations] = useState<DriverRecommendation[]>([]);
  const [loadingDrivers, setLoadingDrivers] = useState(false);
  const [selectedDriverId, setSelectedDriverId] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Handle prefilled state from landing page AI or query params
  useEffect(() => {
    const prefilled = (location.state as any)?.prefilled as AiExtractionResult;
    if (prefilled) {
      if (prefilled.serviceType) setServiceType(prefilled.serviceType);
      if (prefilled.vehicleType) setVehicleType(prefilled.vehicleType);
      if (prefilled.pickupDate) setPickupDate(prefilled.pickupDate);
      if (prefilled.pickupTime) setPickupTime(prefilled.pickupTime);
      if (prefilled.estimatedDistanceKm) setEstimatedDistance(prefilled.estimatedDistanceKm);
      if (prefilled.pickupLocationHint) setPickupAddress(prefilled.pickupLocationHint);
      if (prefilled.destinationHint) setDestinationAddress(prefilled.destinationHint);
      setAiNotice('Form prefilled via AI requirement extraction. Please review and confirm your details.');
    }
  }, [location.state]);

  // Recalculate fare whenever parameters change
  useEffect(() => {
    fetchFareEstimate();
  }, [serviceType, vehicleType, estimatedDistance, estimatedDuration]);

  // Fetch driver recommendations when pickup location or service/vehicle changes
  useEffect(() => {
    fetchDriverRecommendations();
  }, [serviceType, vehicleType, pickupLat, pickupLng]);

  const fetchFareEstimate = async () => {
    setEstimatingFare(true);
    try {
      const res = await api.post<FareEstimate>('/pricing/estimate', {
        serviceType,
        vehicleType,
        estimatedDistanceKm: estimatedDistance,
        estimatedDurationMinutes: estimatedDuration,
      });
      setFareEstimate(res.data);
    } catch (e) {
      console.error('Fare estimate error:', e);
    } finally {
      setEstimatingFare(false);
    }
  };

  const fetchDriverRecommendations = async () => {
    setLoadingDrivers(true);
    try {
      const res = await api.post<DriverRecommendation[]>('/drivers/recommendations', {
        serviceType,
        vehicleType,
        pickupLatitude: pickupLat,
        pickupLongitude: pickupLng,
      });
      setRecommendations(res.data);
      if (res.data.length > 0 && !selectedDriverId) {
        setSelectedDriverId(res.data[0].driverId);
      }
    } catch (e) {
      console.error('Recommendations error:', e);
    } finally {
      setLoadingDrivers(false);
    }
  };

  const handleAiExtraction = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!aiPrompt.trim()) return;

    setAiLoading(true);
    setAiNotice(null);
    try {
      const res = await api.post<AiExtractionResult>('/ai/extract-requirements', { prompt: aiPrompt });
      const data = res.data;

      if (data.serviceType) setServiceType(data.serviceType);
      if (data.vehicleType) setVehicleType(data.vehicleType);
      if (data.pickupDate) setPickupDate(data.pickupDate);
      if (data.pickupTime) setPickupTime(data.pickupTime);
      if (data.estimatedDistanceKm) setEstimatedDistance(data.estimatedDistanceKm);
      if (data.pickupLocationHint) setPickupAddress(data.pickupLocationHint);
      if (data.destinationHint) setDestinationAddress(data.destinationHint);

      setAiNotice(
        `AI successfully structured your journey (${data.fallbackUsed ? 'NLP Rules' : 'Llama 3.1 8B'}). All fields updated below.`
      );
    } catch (err: any) {
      setAiNotice('Could not extract requirements via AI. You can enter details manually.');
    } finally {
      setAiLoading(false);
    }
  };

  const handleUseCurrentLocation = () => {
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setPickupLat(pos.coords.latitude);
          setPickupLng(pos.coords.longitude);
          setPickupAddress(`Current GPS (${pos.coords.latitude.toFixed(4)}, ${pos.coords.longitude.toFixed(4)})`);
        },
        (err) => {
          alert('Could not access current location. Please check browser permissions.');
        }
      );
    }
  };

  const handleSubmitBooking = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      const scheduledDateTime = `${pickupDate}T${pickupTime}:00`;

      const payload = {
        serviceType,
        vehicleType,
        pickupAddress,
        pickupLatitude: pickupLat,
        pickupLongitude: pickupLng,
        destinationAddress,
        destinationLatitude: destLat,
        destinationLongitude: destLng,
        scheduledPickupTime: scheduledDateTime,
        estimatedDistanceKm: estimatedDistance,
        estimatedDurationMinutes: estimatedDuration,
        specialInstructions,
        requestedDriverId: selectedDriverId,
      };

      const res = await api.post('/bookings', payload);
      const booking = res.data;
      navigate(`/track/${booking.id}`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to place booking. Please verify all inputs.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 py-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
        
        {/* Header */}
        <div>
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-blue-500/10 text-blue-400 text-xs font-semibold mb-2">
            <Car className="w-3.5 h-3.5" />
            <span>Driver Booking Wizard</span>
          </div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">
            Book a Professional Chauffeur
          </h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-1">
            Choose your service type, set your route, inspect the transparent fare breakdown, and select your top-matched driver.
          </p>
        </div>

        {/* AI Assistant Banner */}
        <div className="bg-slate-900 border border-indigo-500/30 rounded-2xl p-5 shadow-xl relative overflow-hidden">
          <div className="flex items-center space-x-2 mb-3">
            <Sparkles className="w-4 h-4 text-indigo-400" />
            <h2 className="text-xs font-bold text-white uppercase tracking-wider">
              Smart AI Prompt Assistant (Llama 3.1 8B)
            </h2>
          </div>

          <form onSubmit={handleAiExtraction} className="flex flex-col sm:flex-row gap-3">
            <input
              type="text"
              value={aiPrompt}
              onChange={(e) => setAiPrompt(e.target.value)}
              placeholder="e.g., I need an experienced driver tomorrow at 10 AM for my own SUV for a 40 km trip from Noida to Gurgaon."
              className="flex-1 bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <button
              type="submit"
              disabled={aiLoading || !aiPrompt.trim()}
              className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white text-xs font-semibold rounded-xl flex items-center justify-center space-x-2 transition-all shadow-md flex-shrink-0"
            >
              {aiLoading ? (
                <>
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  <span>Extracting...</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  <span>Auto-Fill with AI</span>
                </>
              )}
            </button>
          </form>

          {aiNotice && (
            <div className="mt-3 text-xs text-indigo-300 flex items-center space-x-2 bg-indigo-950/40 p-2.5 rounded-lg border border-indigo-500/20">
              <CheckCircle className="w-4 h-4 text-indigo-400 flex-shrink-0" />
              <span>{aiNotice}</span>
            </div>
          )}
        </div>

        {error && (
          <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 flex items-center space-x-3 text-rose-400 text-xs">
            <AlertCircle className="w-5 h-5 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Main Grid: Form Left, Fare & Driver Matching Right */}
        <form onSubmit={handleSubmitBooking} className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          
          {/* Left Column: Booking Form (7 cols) */}
          <div className="lg:col-span-7 space-y-6">
            
            {/* Step 1: Service Type Selection */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
              <div className="flex items-center space-x-2 pb-2 border-b border-slate-800">
                <span className="w-6 h-6 rounded-full bg-blue-600 text-white text-xs font-bold flex items-center justify-center">1</span>
                <h3 className="text-sm font-bold text-white">Select Service Type</h3>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {[
                  { type: 'OWN_CAR_DRIVER', label: 'Own-Car Chauffeur', desc: 'Driver for your personal vehicle' },
                  { type: 'ONE_WAY', label: 'One-Way Drop', desc: 'Single point-to-point drop-off' },
                  { type: 'ROUND_TRIP', label: 'Round-Trip Service', desc: 'Return trip with waiting time' },
                  { type: 'HOURLY', label: 'Hourly Chauffeur', desc: 'Hire by 4/8/12 hours block' },
                  { type: 'OUTSTATION', label: 'Outstation Roadtrip', desc: 'Intercity and highway trips' },
                ].map((s) => (
                  <button
                    key={s.type}
                    type="button"
                    onClick={() => setServiceType(s.type as ServiceType)}
                    className={`p-3 rounded-xl border text-left transition-all ${
                      serviceType === s.type
                        ? 'bg-blue-600/10 border-blue-500 text-white ring-1 ring-blue-500'
                        : 'bg-slate-950/60 border-slate-800 text-slate-300 hover:border-slate-700'
                    }`}
                  >
                    <div className="font-semibold text-xs text-white">{s.label}</div>
                    <div className="text-[11px] text-slate-400 mt-0.5">{s.desc}</div>
                  </button>
                ))}
              </div>
            </div>

            {/* Step 2: Vehicle Type */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
              <div className="flex items-center space-x-2 pb-2 border-b border-slate-800">
                <span className="w-6 h-6 rounded-full bg-blue-600 text-white text-xs font-bold flex items-center justify-center">2</span>
                <h3 className="text-sm font-bold text-white">Vehicle Category</h3>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                {[
                  { type: 'HATCHBACK', label: 'Hatchback', sub: 'Swift, i10, Baleno' },
                  { type: 'SEDAN', label: 'Sedan', sub: 'City, Dzire, Verna' },
                  { type: 'SUV', label: 'SUV', sub: 'Creta, Scorpio, XUV' },
                  { type: 'LUXURY', label: 'Luxury', sub: 'BMW, Audi, Mercedes' },
                ].map((v) => (
                  <button
                    key={v.type}
                    type="button"
                    onClick={() => setVehicleType(v.type as VehicleType)}
                    className={`p-3 rounded-xl border text-center transition-all ${
                      vehicleType === v.type
                        ? 'bg-blue-600/10 border-blue-500 text-white ring-1 ring-blue-500'
                        : 'bg-slate-950/60 border-slate-800 text-slate-400 hover:border-slate-700'
                    }`}
                  >
                    <div className="font-bold text-xs text-white">{v.label}</div>
                    <div className="text-[10px] text-slate-400 mt-1 truncate">{v.sub}</div>
                  </button>
                ))}
              </div>
            </div>

            {/* Step 3: Route & Schedule */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
              <div className="flex items-center space-x-2 pb-2 border-b border-slate-800">
                <span className="w-6 h-6 rounded-full bg-blue-600 text-white text-xs font-bold flex items-center justify-center">3</span>
                <h3 className="text-sm font-bold text-white">Pickup, Destination & Schedule</h3>
              </div>

              <div className="space-y-4">
                {/* Pickup address */}
                <div>
                  <div className="flex justify-between items-center mb-1">
                    <label className="text-xs font-medium text-slate-300">Pickup Location</label>
                    <button
                      type="button"
                      onClick={handleUseCurrentLocation}
                      className="text-[11px] text-blue-400 hover:underline flex items-center space-x-1"
                    >
                      <Navigation className="w-3 h-3" />
                      <span>Use Current Location</span>
                    </button>
                  </div>
                  <div className="relative">
                    <MapPin className="w-4 h-4 text-emerald-400 absolute left-3 top-3" />
                    <input
                      type="text"
                      required
                      value={pickupAddress}
                      onChange={(e) => setPickupAddress(e.target.value)}
                      placeholder="Enter pickup address"
                      className="w-full bg-slate-950 border border-slate-700 rounded-xl pl-9 pr-3 py-2.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                </div>

                {/* Destination address */}
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Destination Address</label>
                  <div className="relative">
                    <MapPin className="w-4 h-4 text-rose-400 absolute left-3 top-3" />
                    <input
                      type="text"
                      required
                      value={destinationAddress}
                      onChange={(e) => setDestinationAddress(e.target.value)}
                      placeholder="Enter destination address"
                      className="w-full bg-slate-950 border border-slate-700 rounded-xl pl-9 pr-3 py-2.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                </div>

                {/* Date & Time Row */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1">Pickup Date</label>
                    <div className="relative">
                      <Calendar className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
                      <input
                        type="date"
                        required
                        value={pickupDate}
                        onChange={(e) => setPickupDate(e.target.value)}
                        className="w-full bg-slate-950 border border-slate-700 rounded-xl pl-9 pr-3 py-2.5 text-xs text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1">Pickup Time</label>
                    <div className="relative">
                      <Clock className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
                      <input
                        type="time"
                        required
                        value={pickupTime}
                        onChange={(e) => setPickupTime(e.target.value)}
                        className="w-full bg-slate-950 border border-slate-700 rounded-xl pl-9 pr-3 py-2.5 text-xs text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                  </div>
                </div>

                {/* Distance & Duration slider / inputs */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
                  <div>
                    <div className="flex justify-between text-xs mb-1">
                      <span className="text-slate-300 font-medium">Est. Distance</span>
                      <span className="text-blue-400 font-semibold">{estimatedDistance} km</span>
                    </div>
                    <input
                      type="range"
                      min={1}
                      max={300}
                      step={1}
                      value={estimatedDistance}
                      onChange={(e) => setEstimatedDistance(Number(e.target.value))}
                      className="w-full accent-blue-600 bg-slate-800 rounded-lg cursor-pointer"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between text-xs mb-1">
                      <span className="text-slate-300 font-medium">Est. Duration</span>
                      <span className="text-blue-400 font-semibold">{estimatedDuration} mins</span>
                    </div>
                    <input
                      type="range"
                      min={15}
                      max={720}
                      step={15}
                      value={estimatedDuration}
                      onChange={(e) => setEstimatedDuration(Number(e.target.value))}
                      className="w-full accent-blue-600 bg-slate-800 rounded-lg cursor-pointer"
                    />
                  </div>
                </div>

                {/* Special instructions */}
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">
                    Special Instructions (Optional)
                  </label>
                  <textarea
                    rows={2}
                    value={specialInstructions}
                    onChange={(e) => setSpecialInstructions(e.target.value)}
                    placeholder="e.g. Please wear a formal uniform; vehicle is automatic transmission."
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl p-3 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                  />
                </div>
              </div>
            </div>

          </div>

          {/* Right Column: Pricing Engine Breakdown & Driver Matching (5 cols) */}
          <div className="lg:col-span-5 space-y-6">
            
            {/* Fare Breakdown Card */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                <div className="flex items-center space-x-2">
                  <IndianRupee className="w-5 h-5 text-emerald-400" />
                  <h3 className="text-sm font-bold text-white">Estimated Fare Breakdown</h3>
                </div>
                {estimatingFare && (
                  <div className="w-4 h-4 border-2 border-emerald-400 border-t-transparent rounded-full animate-spin"></div>
                )}
              </div>

              {fareEstimate ? (
                <div className="space-y-2 text-xs">
                  <div className="flex justify-between text-slate-400">
                    <span>Base Fare:</span>
                    <span className="text-slate-200">₹{fareEstimate.baseFare}</span>
                  </div>
                  <div className="flex justify-between text-slate-400">
                    <span>Distance ({fareEstimate.estimatedDistanceKm} km × ₹{fareEstimate.perKmRate}):</span>
                    <span className="text-slate-200">₹{fareEstimate.distanceFare}</span>
                  </div>
                  <div className="flex justify-between text-slate-400">
                    <span>Duration Charge ({fareEstimate.estimatedDurationMinutes} mins):</span>
                    <span className="text-slate-200">₹{fareEstimate.durationFare}</span>
                  </div>
                  {fareEstimate.additionalCharges > 0 && (
                    <div className="flex justify-between text-slate-400">
                      <span>Service Allowance:</span>
                      <span className="text-slate-200">₹{fareEstimate.additionalCharges}</span>
                    </div>
                  )}
                  <div className="flex justify-between text-slate-400">
                    <span>Applicable Taxes ({fareEstimate.taxPercentage}%):</span>
                    <span className="text-slate-200">₹{fareEstimate.taxAmount}</span>
                  </div>

                  <div className="pt-3 border-t border-slate-800 flex justify-between items-baseline">
                    <div>
                      <span className="text-sm font-bold text-white block">Total Estimated Fare:</span>
                      <span className="text-[10px] text-slate-500">Taxes and base fare included</span>
                    </div>
                    <span className="text-2xl font-black text-emerald-400">
                      ₹{fareEstimate.totalEstimatedFare}
                    </span>
                  </div>

                  <div className="mt-3 p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-[10px] text-slate-400 leading-normal flex items-start space-x-1.5">
                    <Info className="w-3.5 h-3.5 text-blue-400 flex-shrink-0 mt-0.5" />
                    <span>{fareEstimate.disclaimer}</span>
                  </div>
                </div>
              ) : (
                <div className="py-6 text-center text-xs text-slate-500">
                  Calculating transparent fare estimate...
                </div>
              )}
            </div>

            {/* Driver Recommendation Selection */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                <div className="flex items-center space-x-2">
                  <Shield className="w-5 h-5 text-blue-400" />
                  <h3 className="text-sm font-bold text-white">Recommended Drivers</h3>
                </div>
                <span className="text-[10px] text-indigo-400 font-semibold bg-indigo-500/10 border border-indigo-500/20 px-2 py-0.5 rounded-full flex items-center space-x-1">
                  <Sparkles className="w-3 h-3 text-indigo-400" />
                  <span>AI-Ranked Match</span>
                </span>
              </div>

              {loadingDrivers ? (
                <div className="py-8 text-center text-xs text-slate-500">
                  Running recommendation algorithm...
                </div>
              ) : recommendations.length === 0 ? (
                <div className="py-6 text-center text-xs text-slate-400">
                  No verified drivers currently in direct range. System will auto-dispatch upon booking confirmation.
                </div>
              ) : (
                <div className="space-y-3">
                  {recommendations.slice(0, 3).map((driver) => {
                    const isSelected = selectedDriverId === driver.driverId;
                    return (
                      <div
                        key={driver.driverId}
                        onClick={() => setSelectedDriverId(driver.driverId)}
                        className={`p-3.5 rounded-xl border cursor-pointer transition-all ${
                          isSelected
                            ? 'bg-blue-600/15 border-blue-500 ring-1 ring-blue-500 shadow-md'
                            : 'bg-slate-950/70 border-slate-800 hover:border-slate-700'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-3">
                            <img
                              src={driver.profilePhotoUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150'}
                              alt={driver.driverName}
                              className="w-10 h-10 rounded-full object-cover border border-slate-700"
                            />
                            <div>
                              <h4 className="text-xs font-bold text-white flex items-center space-x-1.5">
                                <span>{driver.driverName}</span>
                                <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
                              </h4>
                              <p className="text-[10px] text-slate-400">
                                {driver.experienceYears} Yrs Exp • {driver.distanceKm} km away
                              </p>
                            </div>
                          </div>

                          <div className="text-right">
                            <div className="text-xs font-mono font-bold text-emerald-400">
                              {driver.matchScore}% Match
                            </div>
                            <div className="text-[10px] text-amber-400 font-semibold">
                              ★ {driver.rating} ({driver.totalTrips} rides)
                            </div>
                          </div>
                        </div>

                        {/* Top Match reason tag */}
                        {driver.matchReasons && driver.matchReasons.length > 0 && (
                          <div className="mt-2 text-[10px] text-blue-300 bg-blue-950/60 px-2 py-1 rounded-md border border-blue-500/20 truncate">
                            ✓ {driver.matchReasons[0]}
                          </div>
                        )}

                        {/* AI Match Endorsement */}
                        {driver.aiEndorsement && (
                          <div className="mt-2 text-[10px] text-indigo-300 bg-indigo-950/60 p-2 rounded-md border border-indigo-500/30 flex items-start space-x-1.5 leading-relaxed">
                            <Sparkles className="w-3.5 h-3.5 text-indigo-400 flex-shrink-0 mt-0.5" />
                            <span><strong className="text-indigo-200">AI Endorsement:</strong> {driver.aiEndorsement}</span>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}

              {/* Confirm Booking CTA */}
              <button
                type="submit"
                disabled={submitting}
                className="w-full mt-4 py-3.5 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 disabled:opacity-50 text-white text-xs sm:text-sm font-bold rounded-xl flex items-center justify-center space-x-2 shadow-xl shadow-blue-600/30 transition-all"
              >
                {submitting ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Confirming Booking...</span>
                  </>
                ) : (
                  <>
                    <span>Confirm & Book Driver</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </div>

          </div>

        </form>

      </div>
    </div>
  );
};

