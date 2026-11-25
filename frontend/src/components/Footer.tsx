import React from 'react';
import { Link } from 'react-router-dom';
import { Car, Shield, Phone, Mail, MapPin } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const Footer: React.FC = () => {
  const { isAuthenticated, role } = useAuth();
  return (
    <footer className="bg-slate-950 text-slate-400 border-t border-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Brand Info */}
          <div className="space-y-4">
            <div className="flex items-center space-x-3">
              <div className="w-9 h-9 rounded-xl bg-blue-600 flex items-center justify-center text-white font-bold shadow-md">
                <Car className="w-5 h-5" />
              </div>
              <span className="text-xl font-bold tracking-tight text-white">DRIVIO</span>
            </div>
            <p className="text-sm text-slate-400 leading-relaxed">
              India's premier AI-powered mobility marketplace connecting vehicle owners with vetted, professional drivers for every journey.
            </p>
            <div className="flex items-center space-x-2 text-xs text-emerald-400">
              <Shield className="w-4 h-4" />
              <span>100% Background Verified Drivers</span>
            </div>
          </div>

          {/* Quick Links */}
          <div>
            <h3 className="text-sm font-semibold text-white uppercase tracking-wider mb-4">Marketplace</h3>
            <ul className="space-y-2 text-sm">
              <li><Link to="/services" className="hover:text-white transition-colors">Our Services</Link></li>
              {(!isAuthenticated || role === 'CUSTOMER') && (
                <>
                  <li><Link to="/ride" className="hover:text-white transition-colors">Book a Ride</Link></li>
                  <li><Link to="/book" className="hover:text-white transition-colors">Book a Driver</Link></li>
                </>
              )}
              {!isAuthenticated && (
                <li><Link to="/register" className="hover:text-white transition-colors">Drive with Us</Link></li>
              )}
              {isAuthenticated && role === 'CUSTOMER' && (
                <li><Link to="/customer/trips" className="hover:text-white transition-colors">My Trips</Link></li>
              )}
              {isAuthenticated && role === 'DRIVER' && (
                <>
                  <li><Link to="/driver/dashboard" className="hover:text-white transition-colors">Driver Dashboard</Link></li>
                  <li><Link to="/driver/trips" className="hover:text-white transition-colors">Assigned Trips</Link></li>
                </>
              )}
              {isAuthenticated && role === 'ADMIN' && (
                <>
                  <li><Link to="/admin/dashboard" className="hover:text-white transition-colors">Admin Console</Link></li>
                  <li><Link to="/admin/pricing" className="hover:text-white transition-colors">Pricing Engine</Link></li>
                </>
              )}
              <li><Link to="/about" className="hover:text-white transition-colors">Safety Standards</Link></li>
            </ul>
          </div>

          {/* Services */}
          <div>
            <h3 className="text-sm font-semibold text-white uppercase tracking-wider mb-4">Services</h3>
            <ul className="space-y-2 text-sm">
              <li><span className="text-slate-400">Own Car Driver</span></li>
              <li><span className="text-slate-400">One-Way City Trips</span></li>
              <li><span className="text-slate-400">Round-Trip Driver</span></li>
              <li><span className="text-slate-400">Hourly Driver Rental</span></li>
              <li><span className="text-slate-400">Outstation & Intercity</span></li>
            </ul>
          </div>

          {/* Contact info */}
          <div>
            <h3 className="text-sm font-semibold text-white uppercase tracking-wider mb-4">Contact & Support</h3>
            <ul className="space-y-3 text-sm">
              <li className="flex items-center space-x-2">
                <Phone className="w-4 h-4 text-blue-400" />
                <span>+91 1800-DRIVIO-24</span>
              </li>
              <li className="flex items-center space-x-2">
                <Mail className="w-4 h-4 text-blue-400" />
                <span>support@drivio.com</span>
              </li>
              <li className="flex items-center space-x-2">
                <MapPin className="w-4 h-4 text-blue-400" />
                <span>New Delhi, India</span>
              </li>
            </ul>
          </div>
        </div>

        <div className="mt-8 pt-8 border-t border-slate-800/80 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500">
          <p>© {new Date().getFullYear()} DRIVIO Marketplace Inc. All rights reserved.</p>
          <p className="mt-2 sm:mt-0 flex items-center space-x-1">
            <span>Built for Modern Mobility</span>
          </p>
        </div>
      </div>
    </footer>
  );
};

