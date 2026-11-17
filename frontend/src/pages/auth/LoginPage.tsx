import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Car, Lock, Mail, AlertCircle, ArrowRight, Shield, User } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const auth = await login(email, password);
      // Redirect based on role
      if (auth.role === 'ADMIN') {
        navigate('/admin/dashboard');
      } else if (auth.role === 'DRIVER') {
        navigate('/driver/dashboard');
      } else {
        navigate('/customer/dashboard');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickLogin = (demoEmail: string) => {
    setEmail(demoEmail);
    setPassword('password123');
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <Link to="/" className="inline-flex items-center space-x-2">
          <div className="w-10 h-10 rounded-xl bg-blue-600 flex items-center justify-center text-white shadow-lg">
            <Car className="w-6 h-6" />
          </div>
          <span className="text-2xl font-black tracking-tight text-white">DRIVIO</span>
        </Link>
        <h2 className="mt-4 text-2xl font-bold tracking-tight text-slate-100">
          Sign in to your account
        </h2>
        <p className="mt-1 text-xs text-slate-400">
          Access your personal mobility marketplace dashboard
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-slate-900 border border-slate-800 py-8 px-4 shadow-2xl rounded-2xl sm:px-10">
          {error && (
            <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 flex items-center space-x-2 text-rose-400 text-xs">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">
                Email address
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl pl-9 pr-3 py-2.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">
                Password
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl pl-9 pr-3 py-2.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 px-4 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white text-xs font-semibold rounded-xl flex items-center justify-center space-x-2 transition-all shadow-md shadow-blue-600/20"
            >
              {loading ? (
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Quick Demo Logins Pill Box */}
          <div className="mt-6 pt-6 border-t border-slate-800">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block mb-3 text-center">
              Quick Test Accounts (Click to Fill)
            </span>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => handleQuickLogin('rahul.verma@example.com')}
                className="p-2 rounded-lg bg-slate-800/80 hover:bg-slate-700 border border-slate-700 text-center transition-colors"
              >
                <User className="w-3.5 h-3.5 text-blue-400 mx-auto mb-1" />
                <span className="text-[10px] font-medium text-slate-200 block">Customer</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickLogin('rajesh.kumar@drivio.com')}
                className="p-2 rounded-lg bg-slate-800/80 hover:bg-slate-700 border border-slate-700 text-center transition-colors"
              >
                <Car className="w-3.5 h-3.5 text-emerald-400 mx-auto mb-1" />
                <span className="text-[10px] font-medium text-slate-200 block">Driver</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickLogin('admin@drivio.com')}
                className="p-2 rounded-lg bg-slate-800/80 hover:bg-slate-700 border border-slate-700 text-center transition-colors"
              >
                <Shield className="w-3.5 h-3.5 text-purple-400 mx-auto mb-1" />
                <span className="text-[10px] font-medium text-slate-200 block">Admin</span>
              </button>
            </div>
          </div>

          <div className="mt-6 text-center text-xs text-slate-400">
            Don't have an account yet?{' '}
            <Link to="/register" className="text-blue-400 hover:underline font-medium">
              Create an account
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

