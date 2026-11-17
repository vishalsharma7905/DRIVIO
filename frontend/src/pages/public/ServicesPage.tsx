import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Car, Navigation, Compass, Clock, MapPin, ArrowRight, Shield, Check } from 'lucide-react';
import api from '../../services/api';
import { PricingConfiguration } from '../../types';

export const ServicesPage: React.FC = () => {
  const [configs, setConfigs] = useState<PricingConfiguration[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchPricing();
  }, []);

  const fetchPricing = async () => {
    try {
      const res = await api.get<PricingConfiguration[]>('/pricing/configurations');
      setConfigs(res.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const serviceMeta = {
    OWN_CAR_DRIVER: {
      title: 'Own-Car Driver Service',
      desc: 'Hire a skilled chauffeur to drive your personal car. Ideal for city errands, medical appointments, social functions, or relaxing while commuting.',
      icon: Car,
      features: ['Drive your own Hatchback, Sedan or SUV', 'No mileage restriction', 'Professional attire & etiquette'],
    },
    ONE_WAY: {
      title: 'One-Way Driver Service',
      desc: 'Single drop-off service for airport departures, dinner outings, or one-way travel across town.',
      icon: Navigation,
      features: ['Direct point-to-point drop', 'Real-time GPS trip tracking', 'Instant driver dispatch'],
    },
    ROUND_TRIP: {
      title: 'Round-Trip Driver Service',
      desc: 'Two-way travel with flexible waiting time. Great for meetings, day shopping, or family functions.',
      icon: Compass,
      features: ['Both ways in the same booking', 'Reasonable waiting hourly rates', 'Same driver for the whole journey'],
    },
    HOURLY: {
      title: 'Hourly Rental Driver',
      desc: 'Hire a driver for 4, 8, or 12 hours. Multiple stops, errands, and meetings as per your schedule.',
      icon: Clock,
      features: ['Unlimited stops within duration', 'Full schedule flexibility', 'Budget friendly hourly packages'],
    },
    OUTSTATION: {
      title: 'Outstation & Intercity Driver',
      desc: 'Expert highway drivers for intercity road trips, weekend vacations, and long-distance driving.',
      icon: MapPin,
      features: ['Highway driving certified', 'Knowledge of toll & expressways', 'Day & multi-day trip coverage'],
    },
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 py-16">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
        <div className="text-center space-y-4">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-blue-500/10 text-blue-400 text-xs font-semibold">
            <span>Transparent Mobility Solutions</span>
          </div>
          <h1 className="text-3xl sm:text-5xl font-extrabold text-white tracking-tight">
            Our Driver Mobility Services & Transparent Rates
          </h1>
          <p className="text-base text-slate-300 max-w-2xl mx-auto font-light leading-relaxed">
            Every service features upfront pricing based on verified formulas. No hidden surprises.
          </p>
        </div>

        {/* Pricing & Service Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {configs.map((config) => {
            const meta = serviceMeta[config.serviceType] || {
              title: config.serviceType.replace('_', ' '),
              desc: 'Custom driver mobility service.',
              icon: Car,
              features: ['Verified Driver', 'Transparent Rates', 'Live Tracking'],
            };
            const Icon = meta.icon;

            return (
              <div
                key={config.id}
                className="bg-slate-800/80 border border-slate-700 rounded-2xl p-6 shadow-xl flex flex-col justify-between hover:border-blue-500/50 transition-all group"
              >
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <div className="w-12 h-12 rounded-xl bg-blue-600/20 text-blue-400 flex items-center justify-center group-hover:scale-105 transition-transform">
                      <Icon className="w-6 h-6" />
                    </div>
                    <span className="text-[10px] font-semibold tracking-wider text-emerald-400 bg-emerald-500/10 px-2.5 py-1 rounded-full uppercase">
                      Active
                    </span>
                  </div>

                  <h3 className="text-xl font-bold text-white mb-2">{meta.title}</h3>
                  <p className="text-xs text-slate-300 leading-relaxed mb-6">{meta.desc}</p>

                  {/* Pricing Breakdown Card */}
                  <div className="bg-slate-900/90 rounded-xl p-4 border border-slate-700/60 mb-6 space-y-2 text-xs">
                    <div className="flex justify-between text-slate-400">
                      <span>Base Fare:</span>
                      <span className="font-semibold text-white">₹{config.baseFare}</span>
                    </div>
                    <div className="flex justify-between text-slate-400">
                      <span>Rate per KM:</span>
                      <span className="font-semibold text-white">₹{config.perKmRate} / km</span>
                    </div>
                    <div className="flex justify-between text-slate-400">
                      <span>Rate per Hour:</span>
                      <span className="font-semibold text-white">₹{config.perHourRate} / hr</span>
                    </div>
                    <div className="flex justify-between text-slate-400 pt-2 border-t border-slate-800">
                      <span>Minimum Trip Fare:</span>
                      <span className="font-semibold text-emerald-400">₹{config.minFare}</span>
                    </div>
                  </div>

                  {/* Feature Checklist */}
                  <div className="space-y-2 mb-6">
                    {meta.features.map((feat, idx) => (
                      <div key={idx} className="flex items-center space-x-2 text-xs text-slate-300">
                        <Check className="w-3.5 h-3.5 text-blue-400 flex-shrink-0" />
                        <span>{feat}</span>
                      </div>
                    ))}
                  </div>
                </div>

                <Link
                  to={`/book?service=${config.serviceType}`}
                  className="w-full py-3 bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-xl text-xs flex items-center justify-center space-x-2 shadow-md transition-colors"
                >
                  <span>Book {meta.title}</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

