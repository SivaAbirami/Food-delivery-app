import React, { useState } from 'react';
import {
  ShoppingBag,
  ChefHat,
  Bike,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  CheckCircle2,
  Lock,
  Mail,
  Store,
  MapPin,
  ChevronRight,
} from 'lucide-react';
import { useAuth, SEEDED_ACCOUNTS } from '../context/AuthContext';

export default function PortalGatewayPage({ onLoginSuccess }) {
  const { login, error } = useAuth();
  const [selectedRole, setSelectedRole] = useState('CUSTOMER'); // 'CUSTOMER' | 'RESTAURANT_OWNER' | 'DELIVERY_PARTNER'
  const [customEmail, setCustomEmail] = useState('');
  const [customPassword, setCustomPassword] = useState('bobPass123');
  const [loading, setLoading] = useState(false);
  const [localError, setLocalError] = useState(null);

  const handleQuickLogin = async (email, password = 'bobPass123') => {
    setLoading(true);
    setLocalError(null);
    try {
      const user = await login(email, password);
      if (onLoginSuccess) {
        onLoginSuccess(user);
      }
    } catch (err) {
      setLocalError(err.message || 'Login failed. Please check credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleCustomSubmit = async (e) => {
    e.preventDefault();
    if (!customEmail) {
      setLocalError('Please enter an email address');
      return;
    }
    await handleQuickLogin(customEmail, customPassword);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 text-white flex flex-col justify-between py-10 px-4 sm:px-6 lg:px-8 relative overflow-hidden">
      {/* Background Decorative Rings */}
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-[#fc8019]/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl pointer-events-none" />

      {/* Main Container */}
      <div className="max-w-6xl mx-auto w-full relative z-10 flex-1 flex flex-col justify-center">
        {/* Swiggy Logo & Banner */}
        <div className="text-center mb-10">
          <div className="inline-flex items-center gap-2.5 px-4 py-1.5 rounded-full bg-white/10 backdrop-blur-md border border-white/10 text-orange-400 text-xs font-bold mb-4">
            <Sparkles className="w-3.5 h-3.5" />
            <span>SWIGGY ECOSYSTEM GATEWAY</span>
          </div>

          <div className="flex items-center justify-center gap-3 mb-3">
            <div className="w-12 h-12 rounded-2xl bg-[#fc8019] flex items-center justify-center shadow-lg shadow-orange-500/30">
              <span className="text-2xl font-black text-white">S</span>
            </div>
            <h1 className="text-4xl sm:text-5xl font-black tracking-tight text-white">
              SWIGGY
            </h1>
          </div>
          <p className="text-slate-400 text-sm max-w-lg mx-auto">
            Select your role to access your dedicated workspace. When you log in, the application will switch fully to your portal.
          </p>
        </div>

        {/* Global Error Banner */}
        {(localError || error) && (
          <div className="max-w-md mx-auto w-full mb-6 p-3.5 rounded-2xl bg-red-950/80 border border-red-500/50 text-red-200 text-xs font-medium text-center">
            {localError || error}
          </div>
        )}

        {/* 3 Portal Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
          {/* 1. CUSTOMER PORTAL */}
          <div
            className={`rounded-3xl p-6 transition-all border flex flex-col justify-between ${
              selectedRole === 'CUSTOMER'
                ? 'bg-slate-900/90 border-[#fc8019] shadow-xl shadow-orange-500/10 ring-2 ring-orange-500/30'
                : 'bg-slate-900/40 border-slate-800 hover:border-slate-700'
            }`}
            onClick={() => setSelectedRole('CUSTOMER')}
          >
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-12 h-12 rounded-2xl bg-orange-500/20 text-[#fc8019] flex items-center justify-center font-bold">
                  <ShoppingBag className="w-6 h-6" />
                </div>
                <span className="text-[10px] font-black uppercase tracking-wider px-2.5 py-1 rounded-full bg-orange-500/10 text-orange-400 border border-orange-500/20">
                  Consumer
                </span>
              </div>

              <h3 className="text-xl font-black text-white mb-1.5">Customer Portal</h3>
              <p className="text-xs text-slate-400 mb-5 leading-relaxed">
                Order meals from top restaurants, redeem offers, and track your delivery in real-time with your 4-digit OTP.
              </p>

              {/* Seeded Quick Selectors */}
              <div className="space-y-2 mb-6">
                <span className="text-[11px] font-bold text-slate-400 block mb-1">
                  1-Click Persona Sign-In:
                </span>
                {SEEDED_ACCOUNTS.CUSTOMERS.map((cust) => (
                  <button
                    key={cust.id}
                    type="button"
                    disabled={loading}
                    onClick={(e) => {
                      e.stopPropagation();
                      handleQuickLogin(cust.email);
                    }}
                    className="w-full flex items-center justify-between p-2.5 rounded-xl bg-slate-800/80 hover:bg-slate-800 border border-slate-700/60 hover:border-orange-500/40 transition-all text-left group cursor-pointer"
                  >
                    <div className="flex items-center gap-2.5 truncate">
                      <span className="text-base">{cust.avatar}</span>
                      <div className="truncate">
                        <div className="text-xs font-bold text-white group-hover:text-orange-400 transition-colors">
                          {cust.name}
                        </div>
                        <div className="text-[10px] text-slate-400 truncate">
                          {cust.badge} • {cust.zone}
                        </div>
                      </div>
                    </div>
                    <ChevronRight className="w-4 h-4 text-slate-500 group-hover:text-orange-400 group-hover:translate-x-0.5 transition-all shrink-0" />
                  </button>
                ))}
              </div>
            </div>

            <button
              type="button"
              disabled={loading}
              onClick={() => handleQuickLogin('customer@swiggy.in')}
              className="w-full py-3 px-4 rounded-xl bg-[#fc8019] hover:bg-[#e67210] text-white font-black text-xs flex items-center justify-center gap-2 shadow-lg shadow-orange-500/25 transition-all cursor-pointer"
            >
              <span>{loading ? 'Entering Portal...' : 'ENTER AS CUSTOMER'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>

          {/* 2. RESTAURANT PARTNER HUB */}
          <div
            className={`rounded-3xl p-6 transition-all border flex flex-col justify-between ${
              selectedRole === 'RESTAURANT_OWNER'
                ? 'bg-slate-900/90 border-amber-500 shadow-xl shadow-amber-500/10 ring-2 ring-amber-500/30'
                : 'bg-slate-900/40 border-slate-800 hover:border-slate-700'
            }`}
            onClick={() => setSelectedRole('RESTAURANT_OWNER')}
          >
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-12 h-12 rounded-2xl bg-amber-500/20 text-amber-400 flex items-center justify-center font-bold">
                  <ChefHat className="w-6 h-6" />
                </div>
                <span className="text-[10px] font-black uppercase tracking-wider px-2.5 py-1 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20">
                  Merchant
                </span>
              </div>

              <h3 className="text-xl font-black text-white mb-1.5">Restaurant Partner</h3>
              <p className="text-xs text-slate-400 mb-5 leading-relaxed">
                Kitchen Display System (KDS), manage incoming orders, accept & cook, and toggle menu item availability.
              </p>

              {/* Quick Select Outlets (First 4 shown directly, all 12 supported) */}
              <div className="space-y-2 mb-6">
                <span className="text-[11px] font-bold text-slate-400 block mb-1">
                  1-Click Outlet Sign-In:
                </span>
                {SEEDED_ACCOUNTS.RESTAURANTS.slice(0, 4).map((rest) => (
                  <button
                    key={rest.id}
                    type="button"
                    disabled={loading}
                    onClick={(e) => {
                      e.stopPropagation();
                      handleQuickLogin(rest.email);
                    }}
                    className="w-full flex items-center justify-between p-2.5 rounded-xl bg-slate-800/80 hover:bg-slate-800 border border-slate-700/60 hover:border-amber-500/40 transition-all text-left group cursor-pointer"
                  >
                    <div className="flex items-center gap-2.5 truncate">
                      <span className="text-base">{rest.icon}</span>
                      <div className="truncate">
                        <div className="text-xs font-bold text-white group-hover:text-amber-400 transition-colors">
                          {rest.name}
                        </div>
                        <div className="text-[10px] text-slate-400 truncate">
                          {rest.cuisine} • {rest.location}
                        </div>
                      </div>
                    </div>
                    <ChevronRight className="w-4 h-4 text-slate-500 group-hover:text-amber-400 group-hover:translate-x-0.5 transition-all shrink-0" />
                  </button>
                ))}

                {/* More Outlets Dropdown Quick Fill */}
                <select
                  disabled={loading}
                  onChange={(e) => {
                    if (e.target.value) handleQuickLogin(e.target.value);
                  }}
                  className="w-full py-2 px-3 rounded-xl bg-slate-800 text-xs font-bold text-slate-300 border border-slate-700 focus:outline-hidden focus:border-amber-400 cursor-pointer"
                  defaultValue=""
                >
                  <option value="" disabled>
                    More Outlets (Chai Point, Faasos, Anand Sweets...)...
                  </option>
                  {SEEDED_ACCOUNTS.RESTAURANTS.slice(4).map((r) => (
                    <option key={r.id} value={r.email}>
                      {r.icon} {r.name} ({r.location})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <button
              type="button"
              disabled={loading}
              onClick={() => handleQuickLogin('owner@meghana.in')}
              className="w-full py-3 px-4 rounded-xl bg-amber-500 hover:bg-amber-600 text-slate-950 font-black text-xs flex items-center justify-center gap-2 shadow-lg shadow-amber-500/25 transition-all cursor-pointer"
            >
              <span>{loading ? 'Entering Portal...' : 'ENTER AS RESTAURANT'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>

          {/* 3. DELIVERY FLEET PORTAL */}
          <div
            className={`rounded-3xl p-6 transition-all border flex flex-col justify-between ${
              selectedRole === 'DELIVERY_PARTNER'
                ? 'bg-slate-900/90 border-blue-500 shadow-xl shadow-blue-500/10 ring-2 ring-blue-500/30'
                : 'bg-slate-900/40 border-slate-800 hover:border-slate-700'
            }`}
            onClick={() => setSelectedRole('DELIVERY_PARTNER')}
          >
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-12 h-12 rounded-2xl bg-blue-500/20 text-blue-400 flex items-center justify-center font-bold">
                  <Bike className="w-6 h-6" />
                </div>
                <span className="text-[10px] font-black uppercase tracking-wider px-2.5 py-1 rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20">
                  Fleet
                </span>
              </div>

              <h3 className="text-xl font-black text-white mb-1.5">Delivery Fleet</h3>
              <p className="text-xs text-slate-400 mb-5 leading-relaxed">
                Claim ready orders, steer with the Dr. Driving GPS simulator, and verify customer 4-digit OTP at doorstep.
              </p>

              {/* Seeded Quick Selectors */}
              <div className="space-y-2 mb-6">
                <span className="text-[11px] font-bold text-slate-400 block mb-1">
                  1-Click Rider Sign-In:
                </span>
                {SEEDED_ACCOUNTS.RIDERS.map((rider) => (
                  <button
                    key={rider.id}
                    type="button"
                    disabled={loading}
                    onClick={(e) => {
                      e.stopPropagation();
                      handleQuickLogin(rider.email);
                    }}
                    className="w-full flex items-center justify-between p-2.5 rounded-xl bg-slate-800/80 hover:bg-slate-800 border border-slate-700/60 hover:border-blue-500/40 transition-all text-left group cursor-pointer"
                  >
                    <div className="flex items-center gap-2.5 truncate">
                      <span className="text-base">🛵</span>
                      <div className="truncate">
                        <div className="text-xs font-bold text-white group-hover:text-blue-400 transition-colors">
                          {rider.name}
                        </div>
                        <div className="text-[10px] text-slate-400 truncate">
                          {rider.zone} • {rider.vehicle}
                        </div>
                      </div>
                    </div>
                    <ChevronRight className="w-4 h-4 text-slate-500 group-hover:text-blue-400 group-hover:translate-x-0.5 transition-all shrink-0" />
                  </button>
                ))}
              </div>
            </div>

            <button
              type="button"
              disabled={loading}
              onClick={() => handleQuickLogin('rider1@swiggy.in')}
              className="w-full py-3 px-4 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-black text-xs flex items-center justify-center gap-2 shadow-lg shadow-blue-500/25 transition-all cursor-pointer"
            >
              <span>{loading ? 'Entering Portal...' : 'ENTER AS RIDER'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Custom Email / Password Accordion */}
        <div className="max-w-md mx-auto w-full bg-slate-900/60 border border-slate-800 rounded-2xl p-4">
          <form onSubmit={handleCustomSubmit} className="space-y-3">
            <div className="flex items-center justify-between text-xs text-slate-400 font-bold">
              <span>Or sign in with custom credentials:</span>
              <span className="text-[10px] font-mono text-slate-500">Default PWD: bobPass123</span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <input
                type="email"
                placeholder="email@swiggy.in"
                value={customEmail}
                onChange={(e) => setCustomEmail(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-slate-800 border border-slate-700 text-xs text-white placeholder-slate-500 focus:outline-hidden focus:border-orange-500"
              />
              <input
                type="password"
                placeholder="password"
                value={customPassword}
                onChange={(e) => setCustomPassword(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-slate-800 border border-slate-700 text-xs text-white placeholder-slate-500 focus:outline-hidden focus:border-orange-500"
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full py-2 bg-slate-800 hover:bg-slate-700 border border-slate-600 text-white text-xs font-bold rounded-xl transition-colors cursor-pointer"
            >
              {loading ? 'Authenticating...' : 'Sign In with Email'}
            </button>
          </form>
        </div>
      </div>

      {/* Footer */}
      <div className="text-center text-xs text-slate-500 mt-8">
        <span>© 2026 Swiggy Limited • Enterprise Food Delivery Ecosystem</span>
      </div>
    </div>
  );
}

