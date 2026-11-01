import React, { useState } from 'react';
import { X, Sparkles, UserCheck, ShieldCheck, Bike, ChefHat, ArrowRight } from 'lucide-react';
import { useAuth, DEMO_ACCOUNTS } from '../context/AuthContext';

export default function AuthModal() {
  const { authModalOpen, closeAuthModal, authMode, setAuthMode, login, register, switchRole, error } = useAuth();
  
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    phone: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [localError, setLocalError] = useState(null);

  if (!authModalOpen) return null;

  const handleChange = (e) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setLocalError(null);
    try {
      if (authMode === 'login') {
        await login(formData.email, formData.password);
      } else {
        await register(formData.name, formData.email, formData.password, formData.phone);
      }
    } catch (err) {
      setLocalError(err.message || 'Authentication error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
      <div className="relative bg-white rounded-3xl max-w-lg w-full overflow-hidden shadow-2xl animate-in fade-in zoom-in-95 duration-200 border border-slate-100">
        {/* Top Header with Swiggy Orange Accent */}
        <div className="bg-gradient-to-r from-[#fc8019] to-orange-500 p-6 text-white relative">
          <button
            onClick={closeAuthModal}
            className="absolute top-5 right-5 w-8 h-8 rounded-full bg-black/20 hover:bg-black/30 flex items-center justify-center transition-colors text-white"
          >
            <X className="w-5 h-5" />
          </button>

          <div className="flex items-center gap-2 mb-1">
            <span className="text-2xl font-black tracking-tight">SWIGGY</span>
            <span className="text-xs bg-white/20 font-bold px-2 py-0.5 rounded-full uppercase">Food Delivery</span>
          </div>
          <h2 className="text-xl font-bold mt-1">
            {authMode === 'login' ? 'Welcome back!' : 'Create your account'}
          </h2>
          <p className="text-orange-100 text-xs mt-0.5">
            {authMode === 'login'
              ? 'Sign in to order food, view order history, or switch roles'
              : 'Sign up to start ordering delicious food delivered fast'}
          </p>
        </div>

        <div className="p-6">
          {/* 1-Click Role Switcher Demo Cards */}
          <div className="mb-6 p-4 rounded-2xl bg-orange-50/70 border border-orange-100">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-bold text-orange-950 uppercase tracking-wider flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-[#fc8019]" />
                1-Click Demo Logins
              </span>
              <span className="text-[11px] text-orange-800/80">No password needed</span>
            </div>

            <div className="grid grid-cols-3 gap-2">
              {DEMO_ACCOUNTS.map((acc) => {
                const Icon = acc.role === 'CUSTOMER' ? UserCheck : acc.role === 'RESTAURANT_OWNER' ? ChefHat : Bike;
                return (
                  <button
                    key={acc.role}
                    onClick={() => {
                      switchRole(acc);
                      closeAuthModal();
                    }}
                    className="flex flex-col items-center text-center p-2 rounded-xl bg-white hover:bg-orange-100/50 border border-orange-200 transition-all hover:scale-105 shadow-2xs group"
                  >
                    <Icon className="w-5 h-5 text-[#fc8019] mb-1 group-hover:scale-110 transition-transform" />
                    <span className="text-xs font-bold text-slate-800">{acc.label}</span>
                    <span className="text-[10px] text-slate-500 truncate w-full">{acc.email.split('@')[0]}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Form error */}
          {(localError || error) && (
            <div className="mb-4 p-3 rounded-xl bg-red-50 text-red-700 text-xs font-medium border border-red-200">
              {localError || error}
            </div>
          )}

          {/* Standard Form */}
          <form onSubmit={handleSubmit} className="space-y-3.5">
            {authMode === 'register' && (
              <div>
                <label className="block text-xs font-bold text-slate-600 mb-1">Full Name</label>
                <input
                  type="text"
                  name="name"
                  required
                  placeholder="e.g. Rahul Sharma"
                  value={formData.name}
                  onChange={handleChange}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:border-[#fc8019] focus:ring-1 focus:ring-[#fc8019]"
                />
              </div>
            )}

            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1">Email Address</label>
              <input
                type="email"
                name="email"
                required
                placeholder="name@example.com"
                value={formData.email}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:border-[#fc8019] focus:ring-1 focus:ring-[#fc8019]"
              />
            </div>

            {authMode === 'register' && (
              <div>
                <label className="block text-xs font-bold text-slate-600 mb-1">Phone Number</label>
                <input
                  type="tel"
                  name="phone"
                  required
                  placeholder="10-digit phone number"
                  value={formData.phone}
                  onChange={handleChange}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:border-[#fc8019] focus:ring-1 focus:ring-[#fc8019]"
                />
              </div>
            )}

            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1">Password</label>
              <input
                type="password"
                name="password"
                required
                placeholder="••••••••"
                value={formData.password}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:border-[#fc8019] focus:ring-1 focus:ring-[#fc8019]"
              />
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full mt-2 py-3 px-4 rounded-xl bg-[#fc8019] hover:bg-[#e67210] text-white font-bold text-sm transition-colors shadow-md disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {submitting ? 'Please wait...' : authMode === 'login' ? 'LOGIN' : 'SIGN UP'}
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          {/* Mode toggle */}
          <div className="mt-4 text-center">
            {authMode === 'login' ? (
              <p className="text-xs text-slate-500">
                Don't have an account?{' '}
                <button
                  type="button"
                  onClick={() => setAuthMode('register')}
                  className="font-bold text-[#fc8019] hover:underline"
                >
                  Sign up now
                </button>
              </p>
            ) : (
              <p className="text-xs text-slate-500">
                Already have an account?{' '}
                <button
                  type="button"
                  onClick={() => setAuthMode('login')}
                  className="font-bold text-[#fc8019] hover:underline"
                >
                  Log in
                </button>
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

