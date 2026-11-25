import React, { useState, useEffect } from 'react';
import { pricingApi } from '../../services/api';
import { PricingConfiguration, VehiclePricingConfiguration, PricingAuditLog } from '../../types';
import { 
  Sliders, 
  IndianRupee, 
  Edit3, 
  CheckCircle2, 
  AlertCircle, 
  Save, 
  X,
  ShieldAlert
} from 'lucide-react';

export const AdminPricingPage: React.FC = () => {
  const [configurations, setConfigurations] = useState<PricingConfiguration[]>([]);
  const [vehicleConfigs, setVehicleConfigs] = useState<VehiclePricingConfiguration[]>([]);
  const [auditLogs, setAuditLogs] = useState<PricingAuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingConfig, setEditingConfig] = useState<PricingConfiguration | null>(null);
  const [editingVehicleConfig, setEditingVehicleConfig] = useState<VehiclePricingConfiguration | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  const fetchConfigurations = async () => {
    try {
      setLoading(true);
      const [serviceRes, vehicleRes, auditRes] = await Promise.all([
        pricingApi.getConfigurations(),
        pricingApi.getVehicleConfigurations(),
        pricingApi.getAuditLogs(),
      ]);
      setConfigurations(serviceRes.data);
      setVehicleConfigs(vehicleRes.data);
      setAuditLogs(auditRes.data);
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

  const handleVehicleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingVehicleConfig) return;
    try {
      setSaving(true);
      setSaveError(null);
      await pricingApi.updateVehicleConfiguration(editingVehicleConfig.id, editingVehicleConfig);
      setSaveSuccess(true);
      setEditingVehicleConfig(null);
      await fetchConfigurations();
      setTimeout(() => setSaveSuccess(false), 4000);
    } catch (err: any) {
      setSaveError(err.response?.data?.message || 'Failed to update vehicle pricing rules');
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

      {/* Normal Ride Vehicle Pricing (Delhi NCR) */}
      <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Normal Ride Vehicle Pricing (Delhi NCR)</h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Changes increment the pricing version and do not alter historical or already-quoted trips.
            </p>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-200 text-slate-500">
                <th className="py-2.5 px-3">Category</th>
                <th className="py-2.5 px-3">Base</th>
                <th className="py-2.5 px-3">Per KM</th>
                <th className="py-2.5 px-3">Per Min</th>
                <th className="py-2.5 px-3">Min Fare</th>
                <th className="py-2.5 px-3">Wait (Free / Rate)</th>
                <th className="py-2.5 px-3">Night / Peak</th>
                <th className="py-2.5 px-3">Cancel</th>
                <th className="py-2.5 px-3">Ver</th>
                <th className="py-2.5 px-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {vehicleConfigs.map((vc) => (
                <tr key={vc.id} className="hover:bg-slate-50">
                  <td className="py-2.5 px-3 font-bold text-slate-900">{vc.vehicleCategory}</td>
                  <td className="py-2.5 px-3">₹{vc.baseFare.toFixed(2)}</td>
                  <td className="py-2.5 px-3">₹{vc.perKmRate.toFixed(2)}</td>
                  <td className="py-2.5 px-3">₹{vc.perMinuteRate.toFixed(2)}</td>
                  <td className="py-2.5 px-3 font-semibold text-emerald-700">₹{vc.minFare.toFixed(2)}</td>
                  <td className="py-2.5 px-3">{vc.waitingFreeMinutes}m / ₹{vc.waitingPerMinuteRate}</td>
                  <td className="py-2.5 px-3">{vc.nightSurchargePercent}% / {vc.peakSurchargePercent}%</td>
                  <td className="py-2.5 px-3">₹{vc.cancellationFee}</td>
                  <td className="py-2.5 px-3 font-mono">v{vc.version}</td>
                  <td className="py-2.5 px-3 text-right">
                    <button
                      onClick={() => setEditingVehicleConfig({ ...vc })}
                      className="px-2.5 py-1 bg-slate-100 hover:bg-emerald-50 text-slate-700 rounded-lg font-semibold"
                    >
                      Edit
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pricing Audit Trail */}
      {auditLogs.length > 0 && (
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 space-y-3">
          <h2 className="text-base font-bold text-slate-900">Pricing Configuration Audit Trail</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500">
                  <th className="py-2 px-3">Category</th>
                  <th className="py-2 px-3">Ver</th>
                  <th className="py-2 px-3">Previous Values</th>
                  <th className="py-2 px-3">Updated Values</th>
                  <th className="py-2 px-3">Changed At</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {auditLogs.slice(0, 10).map((log) => (
                  <tr key={log.id}>
                    <td className="py-2 px-3 font-bold">{log.vehicleCategory}</td>
                    <td className="py-2 px-3 font-mono">v{log.version}</td>
                    <td className="py-2 px-3 text-slate-500">{log.oldValues}</td>
                    <td className="py-2 px-3 text-emerald-700">{log.newValues}</td>
                    <td className="py-2 px-3 text-slate-400">{new Date(log.changedAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Edit Vehicle Pricing Modal */}
      {editingVehicleConfig && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl space-y-4">
            <div className="flex justify-between items-center pb-2 border-b border-slate-100">
              <h3 className="text-lg font-bold text-slate-900">
                Edit {editingVehicleConfig.vehicleCategory} Pricing (v{editingVehicleConfig.version})
              </h3>
              <button onClick={() => setEditingVehicleConfig(null)} className="p-1.5 text-slate-400 hover:text-slate-700">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form onSubmit={handleVehicleUpdate} className="grid grid-cols-2 gap-3 text-xs">
              <div>
                <label className="block font-semibold mb-1">Base Fare (₹)</label>
                <input type="number" step="0.5" value={editingVehicleConfig.baseFare}
                  onChange={(e) => setEditingVehicleConfig({ ...editingVehicleConfig, baseFare: parseFloat(e.target.value) || 0 })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl" required />
              </div>
              <div>
                <label className="block font-semibold mb-1">Per KM Rate (₹)</label>
                <input type="number" step="0.5" value={editingVehicleConfig.perKmRate}
                  onChange={(e) => setEditingVehicleConfig({ ...editingVehicleConfig, perKmRate: parseFloat(e.target.value) || 0 })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl" required />
              </div>
              <div>
                <label className="block font-semibold mb-1">Per Minute Rate (₹)</label>
                <input type="number" step="0.25" value={editingVehicleConfig.perMinuteRate}
                  onChange={(e) => setEditingVehicleConfig({ ...editingVehicleConfig, perMinuteRate: parseFloat(e.target.value) || 0 })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl" required />
              </div>
              <div>
                <label className="block font-semibold mb-1">Minimum Fare (₹)</label>
                <input type="number" step="1" value={editingVehicleConfig.minFare}
                  onChange={(e) => setEditingVehicleConfig({ ...editingVehicleConfig, minFare: parseFloat(e.target.value) || 0 })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl" required />
              </div>
              <div>
                <label className="block font-semibold mb-1">Wait Rate (₹/min)</label>
                <input type="number" step="0.5" value={editingVehicleConfig.waitingPerMinuteRate}
                  onChange={(e) => setEditingVehicleConfig({ ...editingVehicleConfig, waitingPerMinuteRate: parseFloat(e.target.value) || 0 })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl" required />
              </div>
              <div>
                <label className="block font-semibold mb-1">Night Surcharge (%)</label>
                <input type="number" step="1" value={editingVehicleConfig.nightSurchargePercent}
                  onChange={(e) => setEditingVehicleConfig({ ...editingVehicleConfig, nightSurchargePercent: parseFloat(e.target.value) || 0 })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl" required />
              </div>
              <div>
                <label className="block font-semibold mb-1">Peak Surcharge (10-30%)</label>
                <input type="number" step="1" min="10" max="30" value={editingVehicleConfig.peakSurchargePercent}
                  onChange={(e) => setEditingVehicleConfig({ ...editingVehicleConfig, peakSurchargePercent: parseFloat(e.target.value) || 10 })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl" required />
              </div>
              <div>
                <label className="block font-semibold mb-1">Cancellation Fee (₹25-50)</label>
                <input type="number" step="1" min="25" max="50" value={editingVehicleConfig.cancellationFee}
                  onChange={(e) => setEditingVehicleConfig({ ...editingVehicleConfig, cancellationFee: parseFloat(e.target.value) || 25 })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl" required />
              </div>
              <div className="col-span-2 flex justify-end space-x-2 pt-3">
                <button type="button" onClick={() => setEditingVehicleConfig(null)} className="px-4 py-2 text-slate-600 font-semibold">Cancel</button>
                <button type="submit" disabled={saving} className="px-4 py-2 bg-emerald-600 text-white rounded-xl font-semibold">
                  {saving ? 'Saving...' : 'Save & Increment Version'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

