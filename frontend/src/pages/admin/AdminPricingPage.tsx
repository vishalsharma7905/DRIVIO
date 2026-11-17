import React, { useState, useEffect } from 'react';
import { pricingApi } from '../../services/api';
import { PricingConfiguration } from '../../types';
import { 
  Sliders, 
  DollarSign, 
  Edit3, 
  CheckCircle2, 
  AlertCircle, 
  Save, 
  X,
  ShieldAlert
} from 'lucide-react';

export const AdminPricingPage: React.FC = () => {
  const [configurations, setConfigurations] = useState<PricingConfiguration[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingConfig, setEditingConfig] = useState<PricingConfiguration | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  const fetchConfigurations = async () => {
    try {
      setLoading(true);
      const res = await pricingApi.getConfigurations();
      setConfigurations(res.data);
    } catch (err) {
      console.error('Failed to load pricing configurations', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConfigurations();
  }, []);

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingConfig) return;

    try {
      setSaving(true);
      setSaveError(null);
      await pricingApi.updateConfiguration(editingConfig.id, editingConfig);
      setSaveSuccess(true);
      setEditingConfig(null);
      await fetchConfigurations();
      setTimeout(() => setSaveSuccess(false), 4000);
    } catch (err: any) {
      setSaveError(err.response?.data?.message || 'Failed to update pricing rules');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Configurable Fare Engine</h1>
        <p className="text-sm text-slate-500 mt-1">
          Adjust base fares, distance per-km rates, hourly rates, and minimum guarantees across all 5 mobility services.
        </p>
      </div>

      {saveSuccess && (
        <div className="p-4 bg-emerald-50 text-emerald-800 border border-emerald-200 rounded-xl text-sm flex items-center">
          <CheckCircle2 className="w-5 h-5 mr-2 shrink-0" />
          Pricing configuration updated successfully! Future fare estimates and booking bills will use these new rates.
        </div>
      )}

      {saveError && (
        <div className="p-4 bg-rose-50 text-rose-800 border border-rose-200 rounded-xl text-sm flex items-center">
          <AlertCircle className="w-5 h-5 mr-2 shrink-0" />
          {saveError}
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-emerald-600"></div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {configurations.map(c => (
            <div
              key={c.id}
              className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 space-y-6 flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between">
                  <h3 className="text-lg font-bold text-slate-900">
                    {c.serviceType.replace(/_/g, ' ')}
                  </h3>
                  <span className={`px-2.5 py-0.5 text-xs font-bold rounded-full ${
                    c.active ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'
                  }`}>
                    {c.active ? 'ACTIVE' : 'INACTIVE'}
                  </span>
                </div>
                <span className="text-xs text-slate-400 font-mono block mt-0.5">Rule ID: #{c.id}</span>

                <div className="mt-6 space-y-3 text-sm">
                  <div className="flex justify-between py-1.5 border-b border-slate-100">
                    <span className="text-slate-500">Base Fare:</span>
                    <span className="font-bold text-slate-900">₹{c.baseFare.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between py-1.5 border-b border-slate-100">
                    <span className="text-slate-500">Per KM Rate:</span>
                    <span className="font-bold text-slate-900">₹{c.perKmRate.toFixed(2)}/km</span>
                  </div>
                  <div className="flex justify-between py-1.5 border-b border-slate-100">
                    <span className="text-slate-500">Per Hour Rate:</span>
                    <span className="font-bold text-slate-900">₹{c.perHourRate.toFixed(2)}/hr</span>
                  </div>
                  <div className="flex justify-between py-1.5 border-b border-slate-100">
                    <span className="text-slate-500">Minimum Trip Fare:</span>
                    <span className="font-bold text-emerald-700">₹{c.minFare.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between py-1.5 border-b border-slate-100">
                    <span className="text-slate-500">Cancellation Fee:</span>
                    <span className="font-semibold text-rose-600">₹{c.cancellationFee.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between py-1.5">
                    <span className="text-slate-500">GST / Tax:</span>
                    <span className="font-semibold text-slate-800">{c.taxPercentage}%</span>
                  </div>
                </div>
              </div>

              <button
                onClick={() => setEditingConfig({ ...c })}
                className="w-full py-2.5 bg-slate-50 hover:bg-emerald-50 text-slate-700 hover:text-emerald-700 border border-slate-200 hover:border-emerald-200 rounded-xl font-semibold text-sm transition flex items-center justify-center space-x-1"
              >
                <Edit3 className="w-4 h-4" />
                <span>Configure Rates</span>
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Edit Pricing Modal */}
      {editingConfig && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl space-y-6">
            <div className="flex justify-between items-center pb-3 border-b border-slate-100">
              <div>
                <span className="text-xs font-bold text-emerald-600 uppercase block">Modify Pricing Rules</span>
                <h3 className="text-lg font-bold text-slate-900">
                  {editingConfig.serviceType.replace(/_/g, ' ')}
                </h3>
              </div>
              <button
                onClick={() => setEditingConfig(null)}
                className="p-2 text-slate-400 hover:text-slate-700 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleUpdate} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                    Base Fare (₹)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={editingConfig.baseFare}
                    onChange={e => setEditingConfig({ ...editingConfig, baseFare: parseFloat(e.target.value) || 0 })}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 font-bold"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                    Per KM Rate (₹)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={editingConfig.perKmRate}
                    onChange={e => setEditingConfig({ ...editingConfig, perKmRate: parseFloat(e.target.value) || 0 })}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 font-bold"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                    Per Hour Rate (₹)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={editingConfig.perHourRate}
                    onChange={e => setEditingConfig({ ...editingConfig, perHourRate: parseFloat(e.target.value) || 0 })}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 font-bold"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                    Minimum Fare (₹)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={editingConfig.minFare}
                    onChange={e => setEditingConfig({ ...editingConfig, minFare: parseFloat(e.target.value) || 0 })}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 font-bold"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                    Cancellation Fee (₹)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={editingConfig.cancellationFee}
                    onChange={e => setEditingConfig({ ...editingConfig, cancellationFee: parseFloat(e.target.value) || 0 })}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 font-bold"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                    Tax / GST (%)
                  </label>
                  <input
                    type="number"
                    step="0.1"
                    min="0"
                    max="100"
                    value={editingConfig.taxPercentage}
                    onChange={e => setEditingConfig({ ...editingConfig, taxPercentage: parseFloat(e.target.value) || 0 })}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 font-bold"
                    required
                  />
                </div>
              </div>

              <div className="flex items-center space-x-2 pt-2">
                <input
                  type="checkbox"
                  id="activeToggle"
                  checked={editingConfig.active}
                  onChange={e => setEditingConfig({ ...editingConfig, active: e.target.checked })}
                  className="rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4"
                />
                <label htmlFor="activeToggle" className="text-sm font-semibold text-slate-700">
                  Service active & available for customer bookings
                </label>
              </div>

              <div className="flex items-center justify-end space-x-3 pt-4 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setEditingConfig(null)}
                  className="px-4 py-2 text-sm text-slate-600 hover:text-slate-900 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-xl text-sm transition shadow flex items-center space-x-1"
                >
                  <Save className="w-4 h-4" />
                  <span>{saving ? 'Saving...' : 'Apply New Rates'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

