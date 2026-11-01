import React, { useState } from 'react';
import { ChefHat, ArrowRight, ShieldCheck, Sparkles, Utensils, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function PartnerLoginPage({ onLoginSuccess, onSwitchPortal }) {
  const { login, error } = useAuth();
  const [formData, setFormData] = useState({
    email: 'owner@meghana.in',
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
      if (user.role !== 'RESTAURANT_OWNER') {
        setLocalError(`Logged in as ${user.role}. This portal is for RESTAURANT_OWNER accounts.`);
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
            className="flex-1 py-2 rounded-xl bg-amber-500 text-slate-950 font-black flex items-center justify-center gap-1.5 shadow-xs"
          >
            <ChefHat className="w-3.5 h-3.5" />
            <span>Restaurant Partner</span>
          </button>
          <button
            onClick={() => onSwitchPortal('rider')}
            className="flex-1 py-2 rounded-xl text-slate-300 hover:text-white transition-colors"
          >
            <span>Delivery Fleet</span>
          </button>
        </div>
      </div>

      <div className="sm:mx-auto sm:w-full sm:max-w-md px-4">
        <div className="bg-white py-8 px-6 sm:px-10 shadow-2xl rounded-3xl border border-slate-100">
          {/* Logo & Portal Header */}
          <div className="text-center mb-6">
            <div className="w-14 h-14 rounded-2xl bg-amber-500 text-slate-950 flex items-center justify-center mx-auto mb-3 shadow-lg shadow-amber-500/25">
              <ChefHat className="w-8 h-8" />
            </div>
            <h2 className="text-2xl font-black text-slate-900 tracking-tight">
              Swiggy Partner Hub
            </h2>
            <p className="text-xs text-slate-500 mt-1">
              Merchant Kitchen Display System & Menu Catalog Manager
            </p>
          </div>

          {/* Seeded Outlets Bar */}
          <div className="mb-6 p-3.5 bg-amber-50 rounded-2xl border border-amber-200">
            <div className="flex items-center justify-between text-[11px] font-bold text-amber-950 mb-2">
              <span className="flex items-center gap-1">
                <Sparkles className="w-3.5 h-3.5 text-amber-600" />
                Select Seeded Restaurant Owner:
              </span>
              <span className="text-amber-800 font-mono">PWD: bobPass123</span>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => handleQuickFill('owner@meghana.in')}
                className="py-1.5 px-2 bg-white rounded-xl text-[11px] font-bold border border-amber-300 text-slate-800 hover:bg-amber-100/50 truncate"
              >
                🍗 Meghana Foods
              </button>
              <button
                type="button"
                onClick={() => handleQuickFill('owner@truffles.in')}
                className="py-1.5 px-2 bg-white rounded-xl text-[11px] font-bold border border-amber-300 text-slate-800 hover:bg-amber-100/50 truncate"
              >
                🍔 Truffles
              </button>
              <button
                type="button"
                onClick={() => handleQuickFill('owner@empire.in')}
                className="py-1.5 px-2 bg-white rounded-xl text-[11px] font-bold border border-amber-300 text-slate-800 hover:bg-amber-100/50 truncate"
              >
                🥘 Empire Restaurant
              </button>
              <button
                type="button"
                onClick={() => handleQuickFill('owner@cornerhouse.in')}
                className="py-1.5 px-2 bg-white rounded-xl text-[11px] font-bold border border-amber-300 text-slate-800 hover:bg-amber-100/50 truncate"
              >
                🍨 Corner House
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
              <label className="block text-xs font-bold text-slate-700 mb-1">Partner Email Address</label>
              <input
                type="email"
                required
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                placeholder="owner@meghana.in"
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs sm:text-sm focus:outline-none focus:border-amber-500"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Partner Password</label>
              <input
                type="password"
                required
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                placeholder="••••••••"
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs sm:text-sm focus:outline-none focus:border-amber-500"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-slate-950 font-black text-sm uppercase tracking-wider shadow-md shadow-amber-500/20 transition-all flex items-center justify-center gap-2"
            >
              {loading ? (
                'Verifying Credentials...'
              ) : (
                <>
                  <span>Sign In to Kitchen Hub</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          <div className="mt-6 text-center text-xs text-slate-400">
            <span>Need merchant assistance? Contact Partner Support</span>
          </div>
        </div>
      </div>
    </div>
  );
}

