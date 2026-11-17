import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  Car, Shield, Sparkles, Navigation, Clock, 
  CheckCircle, ArrowRight, Star, Award, Compass,
  MapPin, Check, ChevronRight
} from 'lucide-react';
import api from '../../services/api';
import { AiExtractionResult } from '../../types';

export const LandingPage: React.FC = () => {
  const navigate = useNavigate();
  const [aiPrompt, setAiPrompt] = useState('');
  const [aiLoading, setAiLoading] = useState(false);
  const [aiResult, setAiResult] = useState<AiExtractionResult | null>(null);

  const handleAiTry = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!aiPrompt.trim()) return;

    setAiLoading(true);
    try {
      const res = await api.post<AiExtractionResult>('/ai/extract-requirements', { prompt: aiPrompt });
      setAiResult(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setAiLoading(false);
    }
  };

  const handleProceedToBooking = () => {
    if (aiResult) {
      navigate('/book', { state: { prefilled: aiResult } });
    } else {
      navigate('/book');
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 selection:bg-blue-600 selection:text-white">
      {/* Hero Section */}
      <section className="relative overflow-hidden pt-12 pb-20 lg:pt-20 lg:pb-28">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-blue-900/30 via-slate-900 to-slate-950 -z-10" />
        
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            
            {/* Left Column: Heading and CTAs */}
            <div className="lg:col-span-7 space-y-8 text-center lg:text-left">
              <div className="inline-flex items-center space-x-2 px-3 py-1.5 rounded-full bg-blue-500/10 border border-blue-500/20 text-blue-400 text-xs font-semibold">
                <Sparkles className="w-3.5 h-3.5 text-blue-400" />
                <span>Next-Gen AI Mobility Platform</span>
              </div>

              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight leading-tight">
                Hire a Professional Driver for <span className="bg-gradient-to-r from-blue-400 via-indigo-300 to-teal-300 bg-clip-text text-transparent">Your Own Vehicle</span>
              </h1>

              <p className="text-lg sm:text-xl text-slate-300 max-w-2xl font-light leading-relaxed">
                Connect on-demand with background-verified, high-rated drivers for city trips, daily commutes, hourly hires, and outstation travel. Transparent pricing and live GPS tracking.
              </p>

              <div className="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-4">
                <Link
                  to="/book"
                  className="w-full sm:w-auto px-8 py-4 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold shadow-lg shadow-blue-600/30 flex items-center justify-center space-x-2 transition-all group"
                >
                  <span>Book a Driver</span>
                  <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                </Link>

                <Link
                  to="/register"
                  className="w-full sm:w-auto px-8 py-4 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold border border-slate-700/80 flex items-center justify-center space-x-2 transition-all"
                >
                  <Shield className="w-5 h-5 text-emerald-400" />
                  <span>Join as Driver</span>
                </Link>
              </div>

              {/* Trust badges */}
              <div className="pt-6 grid grid-cols-3 gap-4 border-t border-slate-800 text-left">
                <div>
                  <div className="text-2xl font-bold text-white">100%</div>
                  <div className="text-xs text-slate-400">KYC Verified</div>
                </div>
                <div>
                  <div className="text-2xl font-bold text-white">4.9 ★</div>
                  <div className="text-xs text-slate-400">Average Rating</div>
                </div>
                <div>
                  <div className="text-2xl font-bold text-white">&lt; 15 min</div>
                  <div className="text-xs text-slate-400">Pickup Matching</div>
                </div>
              </div>
            </div>

            {/* Right Column: AI Requirement Assistant Demo */}
            <div className="lg:col-span-5">
              <div className="bg-slate-800/80 border border-slate-700/80 backdrop-blur-md rounded-2xl p-6 shadow-2xl relative">
                <div className="flex items-center space-x-2 mb-4 pb-3 border-b border-slate-700/80">
                  <div className="w-8 h-8 rounded-lg bg-indigo-600/20 border border-indigo-500/30 flex items-center justify-center">
                    <Sparkles className="w-4 h-4 text-indigo-400" />
                  </div>
                  <div>
                    <h3 className="text-sm font-semibold text-white">Drivio AI Booking Assistant</h3>
                    <p className="text-[11px] text-slate-400">Powered by Llama 3.1 8B</p>
                  </div>
                </div>

                <p className="text-xs text-slate-300 mb-3">
                  Type your trip requirement in plain English. Our AI will automatically parse the vehicle, service type, distance, and schedule:
                </p>

                <form onSubmit={handleAiTry} className="space-y-3">
                  <textarea
                    rows={3}
                    value={aiPrompt}
                    onChange={(e) => setAiPrompt(e.target.value)}
                    placeholder="e.g., I need an experienced driver tomorrow morning for my own SUV to travel 35 km from Connaught Place to Noida."
                    className="w-full bg-slate-900/90 border border-slate-700 rounded-xl p-3 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all resize-none"
                  />

                  <button
                    type="submit"
                    disabled={aiLoading || !aiPrompt.trim()}
                    className="w-full py-2.5 px-4 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 disabled:opacity-50 text-white text-xs font-semibold rounded-xl flex items-center justify-center space-x-2 transition-all shadow-md"
                  >
                    {aiLoading ? (
                      <>
                        <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                        <span>Extracting Parameters...</span>
                      </>
                    ) : (
                      <>
                        <Sparkles className="w-4 h-4" />
                        <span>Extract Requirements with AI</span>
                      </>
                    )}
                  </button>
                </form>

                {/* AI Extracted Result Preview */}
                {aiResult && (
                  <div className="mt-4 p-3 bg-slate-900/90 rounded-xl border border-blue-500/30 text-xs space-y-2 animate-in fade-in duration-300">
                    <div className="flex items-center justify-between text-blue-400 font-semibold pb-1 border-b border-slate-800">
                      <span>Structured Booking Parameters:</span>
                      <span className="text-[10px] bg-blue-500/20 px-2 py-0.5 rounded-full">
                        {aiResult.fallbackUsed ? 'NLP Rules' : 'Llama 3.1 8B'}
                      </span>
                    </div>
                    <div className="grid grid-cols-2 gap-2 text-[11px]">
                      <div>
                        <span className="text-slate-500">Service:</span>
                        <div className="font-medium text-slate-200">{aiResult.serviceType?.replace('_', ' ')}</div>
                      </div>
                      <div>
                        <span className="text-slate-500">Vehicle:</span>
                        <div className="font-medium text-slate-200">{aiResult.vehicleType}</div>
                      </div>
                      <div>
                        <span className="text-slate-500">Schedule:</span>
                        <div className="font-medium text-slate-200">{aiResult.pickupDate} ({aiResult.pickupTime})</div>
                      </div>
                      <div>
                        <span className="text-slate-500">Est. Distance:</span>
                        <div className="font-medium text-slate-200">{aiResult.estimatedDistanceKm || 30} km</div>
                      </div>
                    </div>

                    <button
                      onClick={handleProceedToBooking}
                      className="w-full mt-2 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-medium rounded-lg text-xs flex items-center justify-center space-x-1.5 transition-colors"
                    >
                      <span>Proceed to Book with These Details</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )}
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* Services Grid Section */}
      <section className="py-20 bg-slate-950 border-t border-slate-800/80">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-3xl mx-auto mb-16">
            <h2 className="text-xs font-bold text-blue-400 uppercase tracking-widest mb-2">Our Mobility Offerings</h2>
            <p className="text-3xl sm:text-4xl font-extrabold text-white">Comprehensive Driver Services</p>
            <p className="text-sm text-slate-400 mt-3">
              Whether you need a chauffeur for your personal car, a driver for late night rides, or an outstation specialist, Drivio has you covered.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-6">
            {[
              {
                title: 'Own-Car Driver',
                type: 'OWN_CAR_DRIVER',
                desc: 'Experienced chauffeur to drive your personal hatchback, sedan, or SUV safely.',
                fare: '₹150 base + ₹12/km',
                icon: Car,
              },
              {
                title: 'One-Way Drop',
                type: 'ONE_WAY',
                desc: 'Single destination driver service for airport drops, events, or parties.',
                fare: '₹200 base + ₹14/km',
                icon: Navigation,
              },
              {
                title: 'Round-Trip',
                type: 'ROUND_TRIP',
                desc: 'Two-way journey service with waiting options for shopping or business errands.',
                fare: '₹350 base + ₹11/km',
                icon: Compass,
              },
              {
                title: 'Hourly Rental',
                type: 'HOURLY',
                desc: 'Dedicated driver hired by the hour for multi-stop city travel and meetings.',
                fare: '₹250 base + ₹120/hr',
                icon: Clock,
              },
              {
                title: 'Outstation Trip',
                type: 'OUTSTATION',
                desc: 'Highway-certified drivers for weekend getaways and intercity interstate road trips.',
                fare: '₹800 base + ₹13/km',
                icon: MapPin,
              },
            ].map((s) => {
              const Icon = s.icon;
              return (
                <div
                  key={s.type}
                  className="bg-slate-900 border border-slate-800 rounded-2xl p-5 hover:border-blue-500/50 hover:shadow-xl hover:shadow-blue-500/10 transition-all flex flex-col justify-between group"
                >
                  <div>
                    <div className="w-12 h-12 rounded-xl bg-blue-600/10 border border-blue-500/20 text-blue-400 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                      <Icon className="w-6 h-6" />
                    </div>
                    <h3 className="text-base font-bold text-white mb-2">{s.title}</h3>
                    <p className="text-xs text-slate-400 leading-relaxed mb-4">{s.desc}</p>
                  </div>
                  <div className="pt-3 border-t border-slate-800">
                    <span className="text-xs font-semibold text-emerald-400 block">{s.fare}</span>
                    <Link
                      to={`/book?service=${s.type}`}
                      className="mt-2 text-xs font-medium text-blue-400 group-hover:text-blue-300 flex items-center space-x-1"
                    >
                      <span>Book now</span>
                      <ChevronRight className="w-3.5 h-3.5" />
                    </Link>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Driver Recommendation & Safety Features */}
      <section className="py-20 bg-slate-900 border-t border-slate-800/80">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            <div>
              <h2 className="text-xs font-bold text-blue-400 uppercase tracking-widest mb-2">Smart Driver Matching</h2>
              <h3 className="text-3xl font-extrabold text-white mb-4">
                Not Just Any Driver. <br />The Right Driver for Your Car.
              </h3>
              <p className="text-sm text-slate-400 leading-relaxed mb-6">
                Our rule-based recommendation algorithm analyzes pickup proximity using the Haversine formula, verified ratings, years of experience, and transmission/vehicle category compatibility so you get a top-tier chauffeur every single time.
              </p>

              <div className="space-y-3">
                {[
                  'Multi-criteria scoring evaluating proximity (25%), availability (25%), and rating (20%)',
                  'Vehicle category matching ensuring experience with Manual/Automatic & Hatchback/Sedan/SUV/Luxury',
                  'Zero unverified drivers: 100% license validation and administrative KYC checks',
                  'Live real-time driver tracking with Leaflet and WebSocket updates during trips',
                ].map((item, idx) => (
                  <div key={idx} className="flex items-start space-x-3 text-xs text-slate-300">
                    <div className="w-5 h-5 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center flex-shrink-0 mt-0.5">
                      <Check className="w-3 h-3" />
                    </div>
                    <span>{item}</span>
                  </div>
                ))}
              </div>

              <div className="mt-8">
                <Link
                  to="/book"
                  className="px-6 py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-sm font-semibold inline-flex items-center space-x-2 transition-colors"
                >
                  <span>Experience DRIVIO Today</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
              </div>
            </div>

            <div className="bg-slate-800/60 border border-slate-700 rounded-2xl p-6 shadow-xl space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-slate-700">
                <span className="text-xs font-semibold text-slate-300">Recommendation Algorithm Preview</span>
                <span className="text-[10px] font-mono text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full">
                  Score: 94.6 / 100
                </span>
              </div>

              <div className="flex items-center space-x-3">
                <img
                  src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
                  alt="Driver"
                  className="w-12 h-12 rounded-full object-cover border-2 border-blue-500"
                />
                <div>
                  <h4 className="text-sm font-bold text-white flex items-center space-x-1.5">
                    <span>Rajesh Kumar</span>
                    <Shield className="w-3.5 h-3.5 text-emerald-400" />
                  </h4>
                  <p className="text-xs text-slate-400">8 Years Experience • Central Delhi</p>
                </div>
              </div>

              {/* Match reasons breakdown */}
              <div className="space-y-1.5 text-xs">
                <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 flex items-center space-x-2 text-slate-300">
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-400 flex-shrink-0" />
                  <span>Proximity: 1.8 km away from pickup location</span>
                </div>
                <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 flex items-center space-x-2 text-slate-300">
                  <Star className="w-3.5 h-3.5 text-amber-400 flex-shrink-0" />
                  <span>Rating: 4.90 ★ with 48 completed verified reviews</span>
                </div>
                <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 flex items-center space-x-2 text-slate-300">
                  <Car className="w-3.5 h-3.5 text-blue-400 flex-shrink-0" />
                  <span>Compatible with SUV & Sedan luxury vehicles</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

