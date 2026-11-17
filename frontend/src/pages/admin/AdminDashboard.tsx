import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { adminApi, bookingApi } from '../../services/api';
import { AdminDashboardStats, BookingResponse, DriverProfile } from '../../types';
import { 
  Users, 
  Car, 
  ShieldCheck, 
  Clock, 
  DollarSign, 
  CheckCircle2, 
  TrendingUp, 
  ArrowRight,
  AlertCircle,
  FileText,
  Sliders
} from 'lucide-react';

export const AdminDashboard: React.FC = () => {
  const [stats, setStats] = useState<AdminDashboardStats | null>(null);
  const [recentBookings, setRecentBookings] = useState<BookingResponse[]>([]);
  const [pendingDrivers, setPendingDrivers] = useState<DriverProfile[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAdminData = async () => {
      try {
        setLoading(true);
        const [statsRes, bookingsRes, driversRes] = await Promise.all([
          adminApi.getDashboardStats(),
          adminApi.getAllBookings(),
          adminApi.getDrivers('PENDING')
        ]);
        setStats(statsRes.data);
        setRecentBookings(bookingsRes.data.slice(0, 6));
        setPendingDrivers(driversRes.data);
      } catch (err) {
        console.error('Failed to load admin stats', err);
      } finally {
        setLoading(false);
      }
    };
    fetchAdminData();
  }, []);

  if (loading && !stats) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Admin Operations Dashboard</h1>
          <p className="text-sm text-slate-500 mt-1">
            Real-time platform overview, driver KYC authorizations, and operational analytics.
          </p>
        </div>
        <div className="flex items-center space-x-3">
          <Link
            to="/admin/drivers"
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-sm font-semibold transition shadow flex items-center"
          >
            <ShieldCheck className="w-4 h-4 mr-1.5" />
            Verify Drivers
          </Link>
          <Link
            to="/admin/pricing"
            className="px-4 py-2 bg-slate-800 hover:bg-slate-900 text-white rounded-xl text-sm font-semibold transition flex items-center"
          >
            <Sliders className="w-4 h-4 mr-1.5" />
            Fare Rules
          </Link>
        </div>
      </div>

      {/* Primary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Revenue</span>
            <div className="p-2.5 bg-emerald-50 text-emerald-600 rounded-xl">
              <DollarSign className="w-5 h-5" />
            </div>
          </div>
          <p className="text-2xl font-bold text-slate-900 mt-4">
            ₹{stats?.totalRevenue ? stats.totalRevenue.toFixed(2) : '0.00'}
          </p>
          <span className="text-xs text-emerald-600 font-medium mt-1 block flex items-center">
            <TrendingUp className="w-3.5 h-3.5 mr-1" /> Verified bookings completed
          </span>
        </div>

        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Bookings</span>
            <div className="p-2.5 bg-blue-50 text-blue-600 rounded-xl">
              <Car className="w-5 h-5" />
            </div>
          </div>
          <p className="text-2xl font-bold text-slate-900 mt-4">
            {stats?.totalBookings || 0}
          </p>
          <span className="text-xs text-slate-500 mt-1 block">
            {stats?.activeBookings || 0} active • {stats?.completedBookings || 0} completed
          </span>
        </div>

        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Active Drivers</span>
            <div className="p-2.5 bg-purple-50 text-purple-600 rounded-xl">
              <ShieldCheck className="w-5 h-5" />
            </div>
          </div>
          <p className="text-2xl font-bold text-slate-900 mt-4">
            {stats?.verifiedDrivers || 0}
          </p>
          <span className="text-xs text-slate-500 mt-1 block">
            Out of {stats?.totalDrivers || 0} registered drivers
          </span>
        </div>

        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Pending KYC</span>
            <div className="p-2.5 bg-amber-50 text-amber-600 rounded-xl">
              <Clock className="w-5 h-5" />
            </div>
          </div>
          <p className="text-2xl font-bold text-amber-600 mt-4">
            {stats?.pendingDriverVerifications || 0}
          </p>
          <span className="text-xs text-amber-700 font-medium mt-1 block">
            Awaiting manual admin review
          </span>
        </div>
      </div>

      {/* Pending KYC Action Alert */}
      {pendingDrivers.length > 0 && (
        <div className="bg-amber-50 border border-amber-200 rounded-2xl p-5 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div className="flex items-start space-x-3">
            <AlertCircle className="w-6 h-6 text-amber-600 shrink-0 mt-0.5" />
            <div>
              <h3 className="font-semibold text-amber-900">
                {pendingDrivers.length} Driver KYC {pendingDrivers.length === 1 ? 'Application' : 'Applications'} Awaiting Verification
              </h3>
              <p className="text-sm text-amber-700 mt-0.5">
                Review uploaded driving licenses, experience credentials, and vehicle authorizations before granting platform access.
              </p>
            </div>
          </div>
          <Link
            to="/admin/drivers"
            className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-xl text-xs font-bold uppercase tracking-wider transition shadow whitespace-nowrap"
          >
            Review Applications →
          </Link>
        </div>
      )}

      {/* Recent Platform Bookings & Quick Links */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Bookings Feed */}
        <div className="lg:col-span-2 bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-lg font-bold text-slate-900">Recent Platform Trips</h2>
            <Link to="/admin/bookings" className="text-sm font-semibold text-emerald-600 hover:text-emerald-700">
              View All →
            </Link>
          </div>

          {recentBookings.length === 0 ? (
            <p className="text-sm text-slate-500 py-6 text-center">No platform bookings yet.</p>
          ) : (
            <div className="divide-y divide-slate-100">
              {recentBookings.map(b => (
                <div key={b.id} className="py-3.5 flex items-center justify-between">
                  <div className="space-y-0.5">
                    <div className="flex items-center space-x-2">
                      <span className="font-mono text-xs font-bold text-slate-900">#{b.bookingNumber}</span>
                      <span className="text-xs text-slate-500">• {b.serviceType.replace(/_/g, ' ')}</span>
                      <span className="text-xs bg-slate-100 text-slate-700 px-2 py-0.5 rounded font-medium">
                        {b.vehicleType}
                      </span>
                    </div>
                    <p className="text-xs text-slate-600 truncate max-w-sm">
                      {b.pickupAddress} → {b.dropoffAddress}
                    </p>
                  </div>

                  <div className="text-right">
                    <span className="font-bold text-slate-900 text-sm block">₹{b.estimatedFare.toFixed(2)}</span>
                    <span className={`px-2 py-0.5 text-[10px] rounded-full font-semibold ${
                      b.status === 'TRIP_COMPLETED' ? 'bg-emerald-100 text-emerald-800' :
                      b.status === 'CANCELLED' ? 'bg-rose-100 text-rose-800' :
                      'bg-blue-100 text-blue-800'
                    }`}>
                      {b.status.replace(/_/g, ' ')}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Quick Management Shortcuts */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 space-y-4">
          <h2 className="text-lg font-bold text-slate-900 mb-2">Platform Administration</h2>

          <Link
            to="/admin/drivers"
            className="p-4 bg-slate-50 hover:bg-emerald-50/50 border border-slate-200 hover:border-emerald-200 rounded-xl transition flex items-center justify-between group"
          >
            <div className="flex items-center space-x-3">
              <div className="p-2.5 bg-white text-emerald-600 rounded-lg shadow-sm">
                <ShieldCheck className="w-5 h-5" />
              </div>
              <div>
                <span className="text-sm font-bold text-slate-900 group-hover:text-emerald-700 block">
                  Driver KYC Verification
                </span>
                <span className="text-xs text-slate-500">Approve or reject commercial licenses</span>
              </div>
            </div>
            <ArrowRight className="w-4 h-4 text-slate-400 group-hover:text-emerald-600 transition" />
          </Link>

          <Link
            to="/admin/users"
            className="p-4 bg-slate-50 hover:bg-blue-50/50 border border-slate-200 hover:border-blue-200 rounded-xl transition flex items-center justify-between group"
          >
            <div className="flex items-center space-x-3">
              <div className="p-2.5 bg-white text-blue-600 rounded-lg shadow-sm">
                <Users className="w-5 h-5" />
              </div>
              <div>
                <span className="text-sm font-bold text-slate-900 group-hover:text-blue-700 block">
                  User Management
                </span>
                <span className="text-xs text-slate-500">Suspend or activate platform accounts</span>
              </div>
            </div>
            <ArrowRight className="w-4 h-4 text-slate-400 group-hover:text-blue-600 transition" />
          </Link>

          <Link
            to="/admin/pricing"
            className="p-4 bg-slate-50 hover:bg-purple-50/50 border border-slate-200 hover:border-purple-200 rounded-xl transition flex items-center justify-between group"
          >
            <div className="flex items-center space-x-3">
              <div className="p-2.5 bg-white text-purple-600 rounded-lg shadow-sm">
                <Sliders className="w-5 h-5" />
              </div>
              <div>
                <span className="text-sm font-bold text-slate-900 group-hover:text-purple-700 block">
                  Fare Configuration Engine
                </span>
                <span className="text-xs text-slate-500">Adjust base fares & per-km rates</span>
              </div>
            </div>
            <ArrowRight className="w-4 h-4 text-slate-400 group-hover:text-purple-600 transition" />
          </Link>

          <Link
            to="/admin/complaints"
            className="p-4 bg-slate-50 hover:bg-amber-50/50 border border-slate-200 hover:border-amber-200 rounded-xl transition flex items-center justify-between group"
          >
            <div className="flex items-center space-x-3">
              <div className="p-2.5 bg-white text-amber-600 rounded-lg shadow-sm">
                <FileText className="w-5 h-5" />
              </div>
              <div>
                <span className="text-sm font-bold text-slate-900 group-hover:text-amber-700 block">
                  Complaints & Feedback
                </span>
                <span className="text-xs text-slate-500">Investigate customer & driver tickets</span>
              </div>
            </div>
            <ArrowRight className="w-4 h-4 text-slate-400 group-hover:text-amber-600 transition" />
          </Link>
        </div>
      </div>
    </div>
  );
};

