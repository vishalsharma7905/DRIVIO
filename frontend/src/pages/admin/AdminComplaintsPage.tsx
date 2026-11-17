import React, { useState, useEffect } from 'react';
import { adminApi } from '../../services/api';
import { Complaint, ComplaintStatus } from '../../types';
import { 
  FileText, 
  Search, 
  AlertCircle, 
  CheckCircle2, 
  Clock, 
  MessageSquare, 
  Save, 
  X,
  User
} from 'lucide-react';

export const AdminComplaintsPage: React.FC = () => {
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<ComplaintStatus | 'ALL'>('ALL');
  const [search, setSearch] = useState('');
  const [editingComplaint, setEditingComplaint] = useState<Complaint | null>(null);
  const [adminNotes, setAdminNotes] = useState('');
  const [newStatus, setNewStatus] = useState<ComplaintStatus>('OPEN');
  const [saving, setSaving] = useState(false);

  const fetchComplaints = async () => {
    try {
      setLoading(true);
      const res = await adminApi.getAllComplaints();
      setComplaints(res.data);
    } catch (err) {
      console.error('Failed to load complaints', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchComplaints();
  }, []);

  const handleOpenEdit = (c: Complaint) => {
    setEditingComplaint(c);
    setNewStatus(c.status);
    setAdminNotes(c.adminNotes || '');
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingComplaint) return;

    try {
      setSaving(true);
      await adminApi.updateComplaint(editingComplaint.id, {
        status: newStatus,
        adminNotes: adminNotes.trim()
      });
      setEditingComplaint(null);
      await fetchComplaints();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update complaint');
    } finally {
      setSaving(false);
    }
  };

  const filteredComplaints = complaints.filter(c => {
    if (statusFilter !== 'ALL' && c.status !== statusFilter) return false;
    const q = search.toLowerCase();
    return (c.category?.toLowerCase() || '').includes(q) ||
           (c.description?.toLowerCase() || '').includes(q) ||
           (c.complainantName?.toLowerCase() || '').includes(q);
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Complaints & Dispute Resolution</h1>
        <p className="text-sm text-slate-500 mt-1">
          Review passenger feedback, driver dispute reports, and assign administrative resolution outcomes.
        </p>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
        <div className="flex flex-wrap items-center gap-1 bg-slate-100 p-1 rounded-xl">
          {(['ALL', 'OPEN', 'UNDER_INVESTIGATION', 'RESOLVED', 'DISMISSED'] as const).map(tab => (
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
            placeholder="Search complaint text..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500"
          />
        </div>
      </div>

      {/* Complaints List */}
      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-emerald-600"></div>
        </div>
      ) : filteredComplaints.length === 0 ? (
        <div className="text-center py-16 bg-white border-2 border-dashed border-slate-200 rounded-2xl">
          <CheckCircle2 className="w-12 h-12 text-slate-300 mx-auto mb-2" />
          <p className="text-slate-600 font-medium">No complaints found</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredComplaints.map(c => (
            <div
              key={c.id}
              className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 flex flex-col md:flex-row justify-between items-start md:items-center gap-6"
            >
              <div className="space-y-2 flex-1">
                <div className="flex items-center space-x-3">
                  <span className="font-mono text-xs font-bold text-slate-900">Ticket #{c.id}</span>
                  <span className={`px-2.5 py-0.5 text-xs rounded-full font-bold uppercase ${
                    c.status === 'RESOLVED' ? 'bg-emerald-100 text-emerald-800' :
                    c.status === 'DISMISSED' ? 'bg-slate-100 text-slate-600' :
                    c.status === 'UNDER_INVESTIGATION' ? 'bg-blue-100 text-blue-800' :
                    'bg-amber-100 text-amber-800'
                  }`}>
                    {c.status.replace(/_/g, ' ')}
                  </span>
                  <span className="text-xs bg-slate-100 text-slate-700 px-2 py-0.5 rounded font-semibold">
                    {c.category}
                  </span>
                  {c.bookingNumber && (
                    <span className="text-xs text-slate-400 font-mono">Trip #{c.bookingNumber}</span>
                  )}
                </div>

                <p className="text-sm font-medium text-slate-800">
                  {c.description}
                </p>

                <div className="flex items-center space-x-4 text-xs text-slate-500 pt-1">
                  <span>Complainant: <strong>{c.complainantName}</strong></span>
                  <span>•</span>
                  <span>Filed: {new Date(c.createdAt).toLocaleDateString()}</span>
                  {c.adminNotes && (
                    <>
                      <span>•</span>
                      <span className="text-emerald-700 italic">Resolution: {c.adminNotes}</span>
                    </>
                  )}
                </div>
              </div>

              <button
                onClick={() => handleOpenEdit(c)}
                className="px-4 py-2 bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200 rounded-xl text-xs font-semibold transition"
              >
                Resolve / Update
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Update Complaint Modal */}
      {editingComplaint && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl space-y-6">
            <div className="flex justify-between items-center pb-3 border-b border-slate-100">
              <div>
                <span className="text-xs font-bold text-slate-400 uppercase block">Investigation Ticket</span>
                <h3 className="text-lg font-bold text-slate-900">
                  Complaint #{editingComplaint.id} - {editingComplaint.category}
                </h3>
              </div>
              <button
                onClick={() => setEditingComplaint(null)}
                className="p-2 text-slate-400 hover:text-slate-700 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="bg-slate-50 p-3.5 rounded-xl border border-slate-100 text-xs text-slate-600">
              <p className="font-semibold text-slate-900 mb-1">Issue Description:</p>
              <p className="italic">"{editingComplaint.description}"</p>
            </div>

            <form onSubmit={handleUpdate} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                  Resolution Status
                </label>
                <select
                  value={newStatus}
                  onChange={e => setNewStatus(e.target.value as ComplaintStatus)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 font-semibold"
                >
                  <option value="OPEN">OPEN</option>
                  <option value="UNDER_INVESTIGATION">UNDER INVESTIGATION</option>
                  <option value="RESOLVED">RESOLVED</option>
                  <option value="DISMISSED">DISMISSED</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                  Administrative Resolution Notes
                </label>
                <textarea
                  rows={3}
                  value={adminNotes}
                  onChange={e => setAdminNotes(e.target.value)}
                  placeholder="Record customer communications, refund decisions, or driver disciplinary notices..."
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  required
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setEditingComplaint(null)}
                  className="px-4 py-2 text-sm text-slate-600 hover:text-slate-900 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-xl text-sm transition shadow flex items-center space-x-1"
                >
                  <Save className="w-4 h-4" />
                  <span>{saving ? 'Saving...' : 'Update Resolution'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

