import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { driverApi } from '../../services/api';
import { DriverProfile } from '../../types';
import { 
  User, 
  Car, 
  Award, 
  Languages, 
  CheckCircle2, 
  AlertCircle, 
  Save, 
  Clock, 
  ShieldCheck,
  CreditCard
} from 'lucide-react';

const ALL_LANGUAGES = ['English', 'Hindi', 'Punjabi', 'Bengali', 'Marathi', 'Gujarati', 'Tamil', 'Telugu', 'Kannada'];
const ALL_VEHICLES = [
  { id: 'HATCHBACK', label: 'Hatchback (Small/Compact Cars)' },
  { id: 'SEDAN', label: 'Sedan (Comfort/Mid-size Cars)' },
  { id: 'SUV', label: 'SUV (Large Family Cars)' },
  { id: 'LUXURY', label: 'Luxury (Premium Audi/BMW/Mercedes)' },
];
const ALL_SERVICES = [
  { id: 'OWN_CAR_DRIVER', label: 'Hire a Driver for My Own Car' },
  { id: 'ONE_WAY', label: 'One-Way Driver Service' },
  { id: 'ROUND_TRIP', label: 'Round-Trip Driver Service' },
  { id: 'HOURLY', label: 'Hourly Chauffeur Service' },
  { id: 'OUTSTATION', label: 'Outstation Long Distance' },
];

export const DriverProfilePage: React.FC = () => {
  const { user } = useAuth();
  const [profile, setProfile] = useState<DriverProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  // Form states
  const [experienceYears, setExperienceYears] = useState<number>(3);
  const [licenceNumber, setLicenceNumber] = useState('');
  const [licenceExpiryDate, setLicenceExpiryDate] = useState('');
  const [selectedLanguages, setSelectedLanguages] = useState<string[]>([]);
  const [selectedVehicles, setSelectedVehicles] = useState<string[]>([]);
  const [selectedServices, setSelectedServices] = useState<string[]>([]);

  useEffect(() => {
    const loadProfile = async () => {
      try {
        setLoading(true);
        const res = await driverApi.getProfile();
        const p = res.data;
        setProfile(p);
        setExperienceYears(p.experienceYears || 1);
        setLicenceNumber(p.licenceNumber || '');
        setLicenceExpiryDate(p.licenceExpiryDate || '');
        setSelectedLanguages(p.languages ? p.languages.split(',').map(s => s.trim()) : ['English', 'Hindi']);
        setSelectedVehicles(p.vehicleCategories ? p.vehicleCategories.split(',').map(s => s.trim()) : ['SEDAN']);
        setSelectedServices(p.servicePreferences ? p.servicePreferences.split(',').map(s => s.trim()) : ['OWN_CAR_DRIVER']);
      } catch (err) {
        console.error('Failed to load profile', err);
      } finally {
        setLoading(false);
      }
    };
    loadProfile();
  }, []);

  const toggleLanguage = (lang: string) => {
    setSelectedLanguages(prev => 
      prev.includes(lang) ? prev.filter(l => l !== lang) : [...prev, lang]
    );
  };

  const toggleVehicle = (vehicle: string) => {
    setSelectedVehicles(prev => 
      prev.includes(vehicle) ? prev.filter(v => v !== vehicle) : [...prev, vehicle]
    );
  };

  const toggleService = (svc: string) => {
    setSelectedServices(prev => 
      prev.includes(svc) ? prev.filter(s => s !== svc) : [...prev, svc]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedVehicles.length === 0) {
      setSaveError('Please select at least one vehicle category you can drive.');
      return;
    }
    if (selectedServices.length === 0) {
      setSaveError('Please select at least one service preference.');
      return;
    }

    try {
      setSaving(true);
      setSaveError(null);
      setSaveSuccess(false);

      const res = await driverApi.updateProfile({
        experienceYears,
        licenceNumber,
        licenceExpiryDate: licenceExpiryDate || undefined,
        languages: selectedLanguages.join(','),
        vehicleCategories: selectedVehicles.join(','),
        servicePreferences: selectedServices.join(',')
      });

      setProfile(res.data);
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 4000);
    } catch (err: any) {
      setSaveError(err.response?.data?.message || 'Failed to update profile settings');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Driver Professional Profile</h1>
        <p className="text-sm text-slate-500 mt-1">
          Configure your driving experience, vehicle categories, and service preferences for our recommendation algorithm.
        </p>
      </div>

      {saveSuccess && (
        <div className="p-4 bg-emerald-50 text-emerald-800 border border-emerald-200 rounded-xl text-sm flex items-center">
          <CheckCircle2 className="w-5 h-5 mr-2 shrink-0" />
          Profile settings updated successfully! Recommendation engine weights will reflect your preferences.
        </div>
      )}

      {saveError && (
        <div className="p-4 bg-rose-50 text-rose-800 border border-rose-200 rounded-xl text-sm flex items-center">
          <AlertCircle className="w-5 h-5 mr-2 shrink-0" />
          {saveError}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-8">
        {/* Account Info (Read-Only) */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 space-y-4">
          <h2 className="text-lg font-bold text-slate-900 flex items-center">
            <User className="w-5 h-5 mr-2 text-slate-700" />
            Account Information
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <span className="block text-xs font-semibold text-slate-500 uppercase">Full Name</span>
              <p className="text-sm font-semibold text-slate-900 mt-1">{user?.fullName}</p>
            </div>
            <div>
              <span className="block text-xs font-semibold text-slate-500 uppercase">Email</span>
              <p className="text-sm font-semibold text-slate-900 mt-1">{user?.email}</p>
            </div>
            <div>
              <span className="block text-xs font-semibold text-slate-500 uppercase">Verification</span>
              <span className="inline-block mt-1 px-2.5 py-0.5 text-xs font-bold rounded-full bg-emerald-100 text-emerald-800">
                {profile?.verificationStatus}
              </span>
            </div>
          </div>
        </div>

        {/* License & Experience */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 space-y-4">
          <h2 className="text-lg font-bold text-slate-900 flex items-center">
            <CreditCard className="w-5 h-5 mr-2 text-slate-700" />
            Commercial License & Experience
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Driving Experience (Years)
              </label>
              <input
                type="number"
                min="1"
                max="50"
                value={experienceYears}
                onChange={e => setExperienceYears(parseInt(e.target.value) || 1)}
                className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                License Number
              </label>
              <input
                type="text"
                value={licenceNumber}
                onChange={e => setLicenceNumber(e.target.value)}
                placeholder="DL-0420110023451"
                className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                License Expiry Date
              </label>
              <input
                type="date"
                value={licenceExpiryDate}
                onChange={e => setLicenceExpiryDate(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
              />
            </div>
          </div>
        </div>

        {/* Languages */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 space-y-4">
          <h2 className="text-lg font-bold text-slate-900 flex items-center">
            <Languages className="w-5 h-5 mr-2 text-slate-700" />
            Spoken Languages
          </h2>
          <p className="text-xs text-slate-500">Select all languages you are comfortable conversing in with customers.</p>

          <div className="flex flex-wrap gap-2.5">
            {ALL_LANGUAGES.map(lang => (
              <button
                type="button"
                key={lang}
                onClick={() => toggleLanguage(lang)}
                className={`px-4 py-2 rounded-xl text-sm font-semibold transition border ${
                  selectedLanguages.includes(lang)
                    ? 'bg-emerald-600 text-white border-emerald-600 shadow-sm'
                    : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                }`}
              >
                {lang}
              </button>
            ))}
          </div>
        </div>

        {/* Vehicle Categories */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 space-y-4">
          <h2 className="text-lg font-bold text-slate-900 flex items-center">
            <Car className="w-5 h-5 mr-2 text-slate-700" />
            Vehicle Categories You Can Drive
          </h2>
          <p className="text-xs text-slate-500">Select vehicle types you have verified experience driving.</p>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {ALL_VEHICLES.map(v => (
              <label
                key={v.id}
                className={`p-4 rounded-xl border cursor-pointer flex items-center space-x-3 transition ${
                  selectedVehicles.includes(v.id)
                    ? 'border-emerald-600 bg-emerald-50/50'
                    : 'border-slate-200 hover:bg-slate-50'
                }`}
              >
                <input
                  type="checkbox"
                  checked={selectedVehicles.includes(v.id)}
                  onChange={() => toggleVehicle(v.id)}
                  className="rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4"
                />
                <span className="text-sm font-semibold text-slate-800">{v.label}</span>
              </label>
            ))}
          </div>
        </div>

        {/* Service Preferences */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 space-y-4">
          <h2 className="text-lg font-bold text-slate-900 flex items-center">
            <Award className="w-5 h-5 mr-2 text-slate-700" />
            Service Preferences
          </h2>
          <p className="text-xs text-slate-500">Choose which types of driver jobs you are willing to fulfill.</p>

          <div className="space-y-2">
            {ALL_SERVICES.map(s => (
              <label
                key={s.id}
                className={`p-3.5 rounded-xl border cursor-pointer flex items-center space-x-3 transition ${
                  selectedServices.includes(s.id)
                    ? 'border-emerald-600 bg-emerald-50/50'
                    : 'border-slate-200 hover:bg-slate-50'
                }`}
              >
                <input
                  type="checkbox"
                  checked={selectedServices.includes(s.id)}
                  onChange={() => toggleService(s.id)}
                  className="rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4"
                />
                <span className="text-sm font-semibold text-slate-800">{s.label}</span>
              </label>
            ))}
          </div>
        </div>

        <button
          type="submit"
          disabled={saving}
          className="w-full sm:w-auto px-8 py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl shadow transition flex items-center justify-center space-x-2"
        >
          <Save className="w-5 h-5" />
          <span>{saving ? 'Saving Changes...' : 'Save Profile Changes'}</span>
        </button>
      </form>
    </div>
  );
};

