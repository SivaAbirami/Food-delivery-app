import React, { useState } from 'react';
import { Bike, ArrowRight, ShieldCheck, Sparkles, Navigation, Package } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function RiderLoginPage({ onLoginSuccess, onSwitchPortal }) {
  const { login, error } = useAuth();
  const [formData, setFormData] = useState({
    email: 'rider1@swiggy.in',
    password: 'bobPass123',
  });
  const [loading, setLoading] = useState(false);
  const [localError, setLocalError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setLocalError(null);
    try {
      const user = await login(formData.email, formData.password);
      if (user.role !== 'DELIVERY_PARTNER') {
        setLocalError(`Logged in as ${user.role}. This portal is for DELIVERY_PARTNER accounts.`);
        return;
      }
      if (onLoginSuccess) onLoginSuccess();
    } catch (err) {
      setLocalError(err.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickFill = (email) => {
    setFormData({ email, password: 'bobPass123' });
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      {/* Top Portal Switcher Pill */}
      <div className="max-w-md mx-auto w-full mb-6 px-4">
        <div className="bg-slate-800 p-1 rounded-2xl border border-slate-700 flex text-xs font-bold shadow-2xs">
          <button
            onClick={() => onSwitchPortal('customer')}
            className="flex-1 py-2 rounded-xl text-slate-300 hover:text-white transition-colors"
          >
            <span>Customer</span>
          </button>
          <button
            onClick={() => onSwitchPortal('partner')}
            className="flex-1 py-2 rounded-xl text-slate-300 hover:text-white transition-colors"
          >
            <span>Restaurant Partner</span>
          </button>
          <button
            onClick={() => onSwitchPortal('rider')}
            className="flex-1 py-2 rounded-xl bg-blue-600 text-white font-black flex items-center justify-center gap-1.5 shadow-xs"
          >
            <Bike className="w-3.5 h-3.5" />
            <span>Delivery Fleet</span>
          </button>
        </div>
      </div>

      <div className="sm:mx-auto sm:w-full sm:max-w-md px-4">
        <div className="bg-white py-8 px-6 sm:px-10 shadow-2xl rounded-3xl border border-slate-100">
          {/* Logo & Portal Header */}
          <div className="text-center mb-6">
            <div className="w-14 h-14 rounded-2xl bg-blue-600 text-white flex items-center justify-center mx-auto mb-3 shadow-lg shadow-blue-500/25">
              <Bike className="w-8 h-8" />
            </div>
            <h2 className="text-2xl font-black text-slate-900 tracking-tight">
              Swiggy Delivery Partner
            </h2>
            <p className="text-xs text-slate-500 mt-1">
              Rider Fleet Portal: Real-time pickup & door-to-door dispatch
            </p>
          </div>

          {/* Seeded Riders Bar */}
          <div className="mb-6 p-3.5 bg-blue-50 rounded-2xl border border-blue-200">
            <div className="flex items-center justify-between text-[11px] font-bold text-blue-950 mb-2">
              <span className="flex items-center gap-1">
                <Sparkles className="w-3.5 h-3.5 text-blue-600" />
                Select Seeded Delivery Partner:
              </span>
              <span className="text-blue-800 font-mono">PWD: bobPass123</span>
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => handleQuickFill('rider1@swiggy.in')}
                className="flex-1 py-1.5 px-2 bg-white rounded-xl text-[11px] font-bold border border-blue-300 text-slate-800 hover:bg-blue-100/50 truncate"
              >
                🛵 Ramesh (Indiranagar)
              </button>
              <button
                type="button"
                onClick={() => handleQuickFill('rider2@swiggy.in')}
                className="flex-1 py-1.5 px-2 bg-white rounded-xl text-[11px] font-bold border border-blue-300 text-slate-800 hover:bg-blue-100/50 truncate"
              >
                🛵 Suresh (Koramangala)
              </button>
            </div>
          </div>

          {(localError || error) && (
            <div className="mb-4 p-3 rounded-xl bg-red-50 text-red-700 text-xs font-semibold border border-red-200">
              {localError || error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Rider Email Address</label>
              <input
                type="email"
                required
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                placeholder="rider1@swiggy.in"
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs sm:text-sm focus:outline-none focus:border-blue-600"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Password</label>
              <input
                type="password"
                required
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                placeholder="••••••••"
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs sm:text-sm focus:outline-none focus:border-blue-600"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-black text-sm uppercase tracking-wider shadow-md shadow-blue-500/20 transition-all flex items-center justify-center gap-2"
            >
              {loading ? (
                'Connecting Fleet...'
              ) : (
                <>
                  <span>Sign In to Rider Fleet</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          <div className="mt-6 text-center text-xs text-slate-400">
            <span>Rider emergency support & helpline active 24/7</span>
          </div>
        </div>
      </div>
    </div>
  );
}

