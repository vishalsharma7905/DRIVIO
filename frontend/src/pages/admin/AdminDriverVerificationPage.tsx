import React, { useState, useEffect } from 'react';
import { adminApi } from '../../services/api';
import { DriverProfile, VerificationStatus } from '../../types';
import { 
  ShieldCheck, 
  XCircle, 
  CheckCircle2, 
  Clock, 
  Search, 
  FileText, 
  AlertCircle,
  Car,
  User,
  Calendar
} from 'lucide-react';

export const AdminDriverVerificationPage: React.FC = () => {
  const [drivers, setDrivers] = useState<DriverProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedStatus, setSelectedStatus] = useState<VerificationStatus | 'ALL'>('PENDING');
  const [search, setSearch] = useState('');
  const [actionLoading, setActionLoading] = useState<number | null>(null);

  // Rejection modal state
  const [rejectingDriver, setRejectingDriver] = useState<DriverProfile | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');

  const fetchDrivers = async () => {
    try {
      setLoading(true);
      const res = await adminApi.getDrivers(selectedStatus === 'ALL' ? undefined : selectedStatus);
      setDrivers(res.data);
    } catch (err) {
      console.error('Failed to load drivers', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDrivers();
  }, [selectedStatus]);

  const handleApprove = async (driverId: number) => {
    if (!window.confirm('Are you sure you want to verify and authorize this driver?')) return;
    try {
      setActionLoading(driverId);
      await adminApi.verifyDriver(driverId, {
        status: 'VERIFIED',
        remarks: 'All documents and credentials verified by administration.'
      });
      await fetchDrivers();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to approve driver');
    } finally {
      setActionLoading(null);
    }
  };

  const handleConfirmReject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rejectingDriver || !rejectionReason.trim()) return;

    try {
      setActionLoading(rejectingDriver.id);
      await adminApi.verifyDriver(rejectingDriver.id, {
        status: 'REJECTED',
        remarks: rejectionReason.trim()
      });
      setRejectingDriver(null);
      setRejectionReason('');
      await fetchDrivers();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to reject driver');
    } finally {
      setActionLoading(null);
    }
  };

  const filteredDrivers = drivers.filter(d => {
    const q = search.toLowerCase();
    const name = d.user?.fullName?.toLowerCase() || '';
    const email = d.user?.email?.toLowerCase() || '';
    const lic = d.licenceNumber?.toLowerCase() || '';
    return name.includes(q) || email.includes(q) || lic.includes(q);
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Driver KYC Verification Portal</h1>
        <p className="text-sm text-slate-500 mt-1">
          Review commercial driving credentials, background qualifications, and approve or reject driver accounts.
        </p>
      </div>

      {/* Filter Tabs & Search Bar */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
        <div className="flex items-center space-x-1 bg-slate-100 p-1 rounded-xl">
          {(['PENDING', 'VERIFIED', 'REJECTED', 'ALL'] as const).map(tab => (
            <button
              key={tab}
              onClick={() => setSelectedStatus(tab)}
              className={`px-4 py-2 text-xs font-semibold rounded-lg transition ${
                selectedStatus === tab ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        <div className="relative w-full sm:w-64">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <input
            type="text"
            placeholder="Search driver or licence..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500"
          />
        </div>
      </div>

      {/* Drivers List */}
      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-emerald-600"></div>
        </div>
      ) : filteredDrivers.length === 0 ? (
        <div className="text-center py-16 bg-white border-2 border-dashed border-slate-200 rounded-2xl">
          <ShieldCheck className="w-12 h-12 text-slate-300 mx-auto mb-2" />
          <p className="text-slate-600 font-medium">No drivers found matching this filter</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredDrivers.map(d => (
            <div
              key={d.id}
              className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6"
            >
              <div className="space-y-3 flex-1">
                <div className="flex flex-wrap items-center gap-3">
                  <h3 className="text-lg font-bold text-slate-900">
                    {d.user?.fullName || `Driver #${d.id}`}
                  </h3>
                  <span className={`px-2.5 py-0.5 text-xs rounded-full font-bold uppercase ${
                    d.verificationStatus === 'VERIFIED' ? 'bg-emerald-100 text-emerald-800' :
                    d.verificationStatus === 'REJECTED' ? 'bg-rose-100 text-rose-800' :
                    'bg-amber-100 text-amber-800'
                  }`}>
                    {d.verificationStatus}
                  </span>
                  <span className="text-xs text-slate-400">{d.user?.email}</span>
                  <span className="text-xs text-slate-400">• {d.user?.phoneNumber}</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs text-slate-600 bg-slate-50 p-3.5 rounded-xl border border-slate-100">
                  <div>
                    <span className="text-slate-400 block font-semibold uppercase">Commercial License</span>
                    <span className="font-mono font-bold text-slate-800">{d.licenceNumber || 'Not submitted'}</span>
                    {d.licenceExpiryDate && (
                      <span className="text-[10px] text-slate-500 block">Expires: {d.licenceExpiryDate}</span>
                    )}
                  </div>
                  <div>
                    <span className="text-slate-400 block font-semibold uppercase">Experience & Rating</span>
                    <span className="font-bold text-slate-800">{d.experienceYears} Years</span>
                    <span className="text-[10px] text-amber-600 block font-semibold">★ {d.rating.toFixed(2)} ({d.totalRatingsCount} reviews)</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block font-semibold uppercase">Languages</span>
                    <span className="font-medium text-slate-800">{d.languages || 'English, Hindi'}</span>
                  </div>
                </div>

                <div className="flex flex-wrap gap-2 text-xs">
                  <span className="text-slate-500 font-medium">Categories:</span>
                  {d.vehicleCategories?.split(',').map((cat, i) => (
                    <span key={i} className="bg-slate-200 text-slate-700 px-2 py-0.5 rounded font-semibold text-[11px]">
                      {cat.trim()}
                    </span>
                  ))}
                  <span className="text-slate-300">|</span>
                  <span className="text-slate-500 font-medium">Services:</span>
                  {d.servicePreferences?.split(',').map((svc, i) => (
                    <span key={i} className="bg-blue-50 text-blue-700 px-2 py-0.5 rounded font-semibold text-[11px]">
                      {svc.trim().replace(/_/g, ' ')}
                    </span>
                  ))}
                </div>
              </div>

              {/* Action buttons */}
              <div className="flex items-center space-x-3 w-full lg:w-auto">
                {d.verificationStatus !== 'VERIFIED' && (
                  <button
                    onClick={() => handleApprove(d.id)}
                    disabled={actionLoading === d.id}
                    className="flex-1 lg:flex-none px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-xl text-sm transition shadow flex items-center justify-center space-x-1"
                  >
                    <CheckCircle2 className="w-4 h-4" />
                    <span>Approve & Verify</span>
                  </button>
                )}

                {d.verificationStatus !== 'REJECTED' && (
                  <button
                    onClick={() => {
                      setRejectingDriver(d);
                      setRejectionReason('');
                    }}
                    disabled={actionLoading === d.id}
                    className="flex-1 lg:flex-none px-4 py-2.5 bg-white border border-rose-300 text-rose-600 hover:bg-rose-50 font-semibold rounded-xl text-sm transition flex items-center justify-center space-x-1"
                  >
                    <XCircle className="w-4 h-4" />
                    <span>Reject</span>
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Reject Modal */}
      {rejectingDriver && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex items-center space-x-2 text-rose-600">
              <AlertCircle className="w-6 h-6" />
              <h3 className="text-lg font-bold text-slate-900">
                Reject Driver Verification
              </h3>
            </div>
            <p className="text-xs text-slate-500">
              Please enter official administrator comments explaining why this driver application is rejected. The driver will see this feedback.
            </p>

            <form onSubmit={handleConfirmReject} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                  Rejection Reason / Remarks
                </label>
                <textarea
                  rows={3}
                  value={rejectionReason}
                  onChange={e => setRejectionReason(e.target.value)}
                  placeholder="e.g. Expired driving license or illegible photo ID. Please submit a valid commercial license."
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-rose-500"
                  required
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setRejectingDriver(null)}
                  className="px-4 py-2 text-sm text-slate-600 hover:text-slate-900 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={!rejectionReason.trim()}
                  className="px-5 py-2 bg-rose-600 hover:bg-rose-700 text-white font-semibold rounded-xl text-sm transition shadow"
                >
                  Confirm Rejection
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

