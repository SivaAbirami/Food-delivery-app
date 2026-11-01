import React, { useState } from 'react';
import { ShoppingBag, ArrowRight, ShieldCheck, Sparkles, MapPin, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function CustomerLoginPage({ onLoginSuccess, onSwitchPortal }) {
  const { login, register, error } = useAuth();
  const [isRegister, setIsRegister] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    email: 'customer@swiggy.in',
    password: 'bobPass123',
    phone: '9876543201',
  });
  const [loading, setLoading] = useState(false);
  const [localError, setLocalError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setLocalError(null);
    try {
      if (isRegister) {
        await register(formData.name, formData.email, formData.password, formData.phone);
      } else {
        await login(formData.email, formData.password);
      }
      if (onLoginSuccess) onLoginSuccess();
    } catch (err) {
      setLocalError(err.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickFill = (email, name, phone) => {
    setFormData({
      name: name || '',
      email,
      password: 'bobPass123',
      phone: phone || '9876543201',
    });
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      {/* Top Portal Switcher Pill */}
      <div className="max-w-md mx-auto w-full mb-6 px-4">
        <div className="bg-white p-1 rounded-2xl border border-slate-200 flex text-xs font-bold shadow-2xs">
          <button
            onClick={() => onSwitchPortal('customer')}
            className="flex-1 py-2 rounded-xl bg-[#fc8019] text-white flex items-center justify-center gap-1.5 shadow-xs"
          >
            <ShoppingBag className="w-3.5 h-3.5" />
            <span>Customer</span>
          </button>
          <button
            onClick={() => onSwitchPortal('partner')}
            className="flex-1 py-2 rounded-xl text-slate-600 hover:text-slate-900 transition-colors"
          >
            <span>Restaurant Partner</span>
          </button>
          <button
            onClick={() => onSwitchPortal('rider')}
            className="flex-1 py-2 rounded-xl text-slate-600 hover:text-slate-900 transition-colors"
          >
            <span>Delivery Fleet</span>
          </button>
        </div>
      </div>

      <div className="sm:mx-auto sm:w-full sm:max-w-md px-4">
        <div className="bg-white py-8 px-6 sm:px-10 shadow-xl rounded-3xl border border-slate-100">
          {/* Logo & Portal Header */}
          <div className="text-center mb-6">
            <div className="w-14 h-14 rounded-2xl bg-[#fc8019] text-white flex items-center justify-center mx-auto mb-3 shadow-lg shadow-orange-500/25">
              <ShoppingBag className="w-8 h-8" />
            </div>
            <h2 className="text-2xl font-black text-slate-900 tracking-tight">
              Swiggy Customer Portal
            </h2>
            <p className="text-xs text-slate-500 mt-1">
              Order delicious meals from top restaurants in Bengaluru
            </p>
          </div>

          {/* Seeded Quick Accounts Bar */}
          <div className="mb-6 p-3.5 bg-orange-50 rounded-2xl border border-orange-200">
            <div className="flex items-center justify-between text-[11px] font-bold text-orange-950 mb-2">
              <span className="flex items-center gap-1">
                <Sparkles className="w-3.5 h-3.5 text-[#fc8019]" />
                Seeded Customer Accounts:
              </span>
              <span className="text-orange-700 font-mono">PWD: bobPass123</span>
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => handleQuickFill('customer@swiggy.in', 'Rahul Sharma', '9876543201')}
                className="flex-1 py-1.5 px-2 bg-white rounded-xl text-[11px] font-bold border border-orange-300 text-slate-800 hover:bg-orange-100/50 truncate"
              >
                Rahul (Indiranagar)
              </button>
              <button
                type="button"
                onClick={() => handleQuickFill('priya@swiggy.in', 'Priya Patel', '9876543202')}
                className="flex-1 py-1.5 px-2 bg-white rounded-xl text-[11px] font-bold border border-orange-300 text-slate-800 hover:bg-orange-100/50 truncate"
              >
                Priya (Koramangala)
              </button>
            </div>
          </div>

          {(localError || error) && (
            <div className="mb-4 p-3 rounded-xl bg-red-50 text-red-700 text-xs font-semibold border border-red-200">
              {localError || error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {isRegister && (
              <>
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Full Name</label>
                  <input
                    type="text"
                    required
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    placeholder="e.g. Rahul Sharma"
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs sm:text-sm focus:outline-none focus:border-[#fc8019]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Phone Number</label>
                  <input
                    type="tel"
                    required
                    value={formData.phone}
                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                    placeholder="10-digit mobile number"
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs sm:text-sm focus:outline-none focus:border-[#fc8019]"
                  />
                </div>
              </>
            )}

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Email Address</label>
              <input
                type="email"
                required
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                placeholder="customer@swiggy.in"
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs sm:text-sm focus:outline-none focus:border-[#fc8019]"
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
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs sm:text-sm focus:outline-none focus:border-[#fc8019]"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 rounded-xl bg-[#fc8019] hover:bg-[#e67210] text-white font-black text-sm uppercase tracking-wider shadow-md shadow-orange-500/20 transition-all flex items-center justify-center gap-2"
            >
              {loading ? (
                'Connecting...'
              ) : isRegister ? (
                <>
                  <span>Create Customer Account</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              ) : (
                <>
                  <span>Sign In & Order Food</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          <div className="mt-6 text-center text-xs text-slate-500">
            {isRegister ? (
              <p>
                Already have an account?{' '}
                <button
                  type="button"
                  onClick={() => setIsRegister(false)}
                  className="font-bold text-[#fc8019] hover:underline"
                >
                  Sign In
                </button>
              </p>
            ) : (
              <p>
                New to Swiggy?{' '}
                <button
                  type="button"
                  onClick={() => setIsRegister(true)}
                  className="font-bold text-[#fc8019] hover:underline"
                >
                  Register here
                </button>
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

