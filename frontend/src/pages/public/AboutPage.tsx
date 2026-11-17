import React from 'react';
import { Shield, Award, Users, CheckCircle, Car } from 'lucide-react';

export const AboutPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 py-16">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
        <div className="text-center space-y-4">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-blue-500/10 text-blue-400 text-xs font-semibold">
            <span>About DRIVIO</span>
          </div>
          <h1 className="text-3xl sm:text-5xl font-extrabold text-white tracking-tight">
            Pioneering Safe, Smart, AI-Assisted Mobility
          </h1>
          <p className="text-base text-slate-300 max-w-2xl mx-auto font-light leading-relaxed">
            DRIVIO was founded to solve a fundamental urban challenge: car owners who want to enjoy the comfort of their personal vehicles without the stress, fatigue, and friction of traffic, parking, and long highway journeys.
          </p>
        </div>

        {/* 3 Core Pillars */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-6 space-y-3">
            <div className="w-12 h-12 rounded-xl bg-blue-600/20 text-blue-400 flex items-center justify-center">
              <Shield className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-white">Rigorous Vetting</h3>
            <p className="text-xs text-slate-300 leading-relaxed">
              Every driver on the DRIVIO platform undergoes in-depth background checks, official driving license verification with transport authorities, and thorough driving skill tests.
            </p>
          </div>

          <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-6 space-y-3">
            <div className="w-12 h-12 rounded-xl bg-emerald-600/20 text-emerald-400 flex items-center justify-center">
              <Award className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-white">Rule-Based Matching</h3>
            <p className="text-xs text-slate-300 leading-relaxed">
              Our multi-criteria recommendation algorithm evaluates geographic distance, ratings, driving experience, vehicle category, and schedule compatibility to match you with the best chauffeur.
            </p>
          </div>

          <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-6 space-y-3">
            <div className="w-12 h-12 rounded-xl bg-purple-600/20 text-purple-400 flex items-center justify-center">
              <Users className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-white">Driver Empowerment</h3>
            <p className="text-xs text-slate-300 leading-relaxed">
              We empower professional drivers with transparent 75% trip earnings, fair shift control, reliable customer ratings, and immediate digital payment settlement.
            </p>
          </div>
        </div>

        {/* Verification standards */}
        <div className="bg-slate-800/50 border border-slate-800 rounded-2xl p-8 space-y-4">
          <h2 className="text-xl font-bold text-white">Driver Verification Workflow</h2>
          <p className="text-xs text-slate-300 leading-relaxed">
            Unlike informal hiring, DRIVIO enforces an administrative verification state machine. When drivers register, their status is set to PENDING. An administrator reviews their submitted government identification, commercial badge or license authenticity, and driving experience before granting VERIFIED status. Only verified drivers are eligible to accept bookings.
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
            {[
              'Commercial & Transport License Authentication',
              'Zero Criminal Record & Identity KYC',
              'Vehicle Category Clearance (Hatchback / Sedan / SUV / Luxury)',
              'Customer Feedback & Rolling Star Rating System',
            ].map((v, i) => (
              <div key={i} className="flex items-center space-x-2 text-xs text-slate-300">
                <CheckCircle className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                <span>{v}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

