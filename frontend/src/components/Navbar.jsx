import React, { useState } from 'react';
import {
  MapPin,
  Search,
  Percent,
  User,
  ShoppingBag,
  ChevronDown,
  ChefHat,
  Bike,
  Sparkles,
  LogOut,
  Clock,
  Check,
  ShieldAlert,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useLocation } from '../context/LocationContext';

export default function Navbar({ currentView, setCurrentView, onOpenSearch }) {
  const { user, logout, switchAccount, ensureRole, SEEDED_ACCOUNTS } = useAuth();
  const { totalItemCount, openCart } = useCart();
  const { currentLocation, openLocationModal } = useLocation();
  const [accountDropdownOpen, setAccountDropdownOpen] = useState(false);

  // Instant seamless portal switching without login prompts
  const handlePortalSwitch = async (targetRole, targetView) => {
    try {
      await ensureRole(targetRole);
      setCurrentView(targetView);
    } catch (err) {
      console.error('Error switching portal:', err);
    }
  };

  const handleAccountSelect = async (account, targetView) => {
    try {
      await switchAccount(account);
      setCurrentView(targetView);
      setAccountDropdownOpen(false);
    } catch (err) {
      console.error('Failed to select account:', err);
    }
  };

  return (
    <header className="sticky top-0 z-40 bg-white shadow-xs border-b border-slate-100">
      {/* Main Swiggy Navigation */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-18 flex items-center justify-between">
        {/* Left: Logo & Dynamic Location Selector */}
        <div className="flex items-center gap-4 sm:gap-8">
          {/* Swiggy Logo */}
          <div
            onClick={() => {
              if (user?.role === 'RESTAURANT_OWNER') setCurrentView('owner');
              else if (user?.role === 'DELIVERY_PARTNER') setCurrentView('delivery');
              else setCurrentView('home');
            }}
            className="flex items-center gap-2 cursor-pointer group"
          >
            <div className="w-9 h-9 rounded-2xl bg-[#fc8019] flex items-center justify-center shadow-md shadow-orange-500/20 group-hover:scale-105 transition-transform">
              <svg viewBox="0 0 24 24" fill="none" className="w-6 h-6 text-white" stroke="currentColor">
                <path
                  d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"
                  fill="white"
                />
              </svg>
            </div>
            <div className="flex flex-col">
              <span className="font-black text-xl md:text-2xl tracking-tighter text-[#fc8019] leading-none">
                SWIGGY
              </span>
              <span className="text-[9px] font-bold text-slate-400 tracking-widest uppercase">FOOD</span>
            </div>
          </div>

          {/* Interactive Real Location Selector */}
          <div
            onClick={openLocationModal}
            className="flex items-center gap-1.5 cursor-pointer text-xs group py-1.5 px-3 rounded-xl hover:bg-slate-50 transition-colors border border-transparent hover:border-slate-200"
            title="Click to change delivery location in Bengaluru"
          >
            <MapPin className="w-4 h-4 text-[#fc8019] shrink-0" />
            <span className="font-black text-slate-800 border-b-2 border-slate-800 pb-0.5 group-hover:text-[#fc8019] group-hover:border-[#fc8019] transition-colors">
              {currentLocation?.name || 'Indiranagar'}
            </span>
            <span className="text-slate-400 truncate max-w-[120px] md:max-w-[200px] hidden sm:inline">
              {currentLocation?.area || '100 Feet Rd, Bengaluru'}
            </span>
            <ChevronDown className="w-3.5 h-3.5 text-[#fc8019]" />
          </div>
        </div>

        {/* Right Navigation Items */}
        <nav className="flex items-center gap-3 sm:gap-5">
          {/* Search */}
          <button
            onClick={onOpenSearch}
            className="flex items-center gap-1.5 text-slate-700 hover:text-[#fc8019] font-bold text-xs sm:text-sm transition-colors cursor-pointer"
          >
            <Search className="w-4 h-4 text-slate-500" />
            <span className="hidden md:inline">Search</span>
          </button>

          {/* Offers */}
          <button
            onClick={() => {
              if (user?.role !== 'CUSTOMER') ensureRole('CUSTOMER');
              setCurrentView('home');
            }}
            className="hidden sm:flex items-center gap-1.5 text-slate-700 hover:text-[#fc8019] font-bold text-xs sm:text-sm transition-colors cursor-pointer"
          >
            <Percent className="w-4 h-4 text-slate-500" />
            <span className="hidden md:inline">Offers</span>
            <span className="bg-orange-100 text-[#fc8019] text-[10px] font-black px-1.5 py-0.5 rounded-full">
              50% OFF
            </span>
          </button>

          {/* Orders History (for Customers) */}
          {user?.role === 'CUSTOMER' && (
            <button
              onClick={() => setCurrentView('orders')}
              className={`flex items-center gap-1.5 font-bold text-xs sm:text-sm transition-colors cursor-pointer ${
                currentView === 'orders' ? 'text-[#fc8019]' : 'text-slate-700 hover:text-[#fc8019]'
              }`}
            >
              <Clock className="w-4 h-4 text-slate-500" />
              <span className="hidden md:inline">Orders</span>
            </button>
          )}

          {/* Unified Persona & Account Switcher Dropdown */}
          <div className="relative">
            <button
              onClick={() => setAccountDropdownOpen(!accountDropdownOpen)}
              className="flex items-center gap-2 py-1.5 px-3 rounded-2xl bg-slate-100 hover:bg-slate-200/80 transition-colors cursor-pointer border border-slate-200"
            >
              <div className="w-6 h-6 rounded-full bg-[#fc8019] text-white flex items-center justify-center text-xs font-black shadow-xs">
                {user?.name ? user.name[0].toUpperCase() : 'U'}
              </div>
              <div className="flex flex-col text-left">
                <span className="text-xs font-black text-slate-800 max-w-[100px] sm:max-w-[130px] truncate leading-tight">
                  {user?.name || user?.email || 'Select Account'}
                </span>
                <span className="text-[10px] font-bold text-slate-400 capitalize leading-tight">
                  {user?.role === 'RESTAURANT_OWNER'
                    ? '🍳 Merchant'
                    : user?.role === 'DELIVERY_PARTNER'
                    ? '🛵 Fleet Rider'
                    : '🛍️ Customer'}
                </span>
              </div>
              <ChevronDown className="w-3.5 h-3.5 text-slate-500" />
            </button>

            {/* Account Switcher Modal / Dropdown */}
            {accountDropdownOpen && (
              <div
                onMouseLeave={() => setAccountDropdownOpen(false)}
                className="absolute right-0 mt-2 w-80 bg-white rounded-3xl shadow-2xl border border-slate-100 py-3 z-50 animate-in fade-in zoom-in-95"
              >
                <div className="px-4 pb-2 border-b border-slate-100 flex items-center justify-between">
                  <span className="text-xs font-black text-slate-900 tracking-tight">
                    Switch Demo Account
                  </span>
                  <span className="text-[10px] font-mono text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full font-bold">
                    Zero Password
                  </span>
                </div>

                <div className="max-h-[380px] overflow-y-auto px-2 py-2 space-y-3">
                  {/* 1. Restaurant Owners (All 8 Seeded Outlets) */}
                  <div>
                    <div className="px-3 py-1 text-[11px] font-black text-amber-700 uppercase tracking-wider flex items-center gap-1.5">
                      <ChefHat className="w-3.5 h-3.5" />
                      <span>Restaurant Owners (8 Outlets)</span>
                    </div>
                    <div className="space-y-1 mt-1">
                      {SEEDED_ACCOUNTS.RESTAURANTS.map((outlet) => {
                        const isCurrent = user?.email === outlet.email;
                        return (
                          <button
                            key={outlet.id}
                            onClick={() => handleAccountSelect(outlet, 'owner')}
                            className={`w-full px-3 py-2 rounded-xl text-left flex items-center justify-between transition-colors cursor-pointer ${
                              isCurrent
                                ? 'bg-amber-100/70 text-amber-950 font-black'
                                : 'hover:bg-amber-50/60 text-slate-700'
                            }`}
                          >
                            <div className="flex items-center gap-2 truncate">
                              <span className="text-base">{outlet.icon}</span>
                              <div className="truncate">
                                <div className="text-xs font-bold truncate leading-tight">{outlet.name}</div>
                                <div className="text-[10px] text-slate-400 truncate">{outlet.cuisine} • {outlet.location}</div>
                              </div>
                            </div>
                            {isCurrent && <Check className="w-4 h-4 text-amber-600 shrink-0" />}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  {/* 2. Customer Accounts */}
                  <div className="border-t border-slate-100 pt-2">
                    <div className="px-3 py-1 text-[11px] font-black text-orange-600 uppercase tracking-wider flex items-center gap-1.5">
                      <ShoppingBag className="w-3.5 h-3.5" />
                      <span>Customer Personas</span>
                    </div>
                    <div className="space-y-1 mt-1">
                      {SEEDED_ACCOUNTS.CUSTOMERS.map((cust) => {
                        const isCurrent = user?.email === cust.email;
                        return (
                          <button
                            key={cust.id}
                            onClick={() => handleAccountSelect(cust, 'home')}
                            className={`w-full px-3 py-2 rounded-xl text-left flex items-center justify-between transition-colors cursor-pointer ${
                              isCurrent
                                ? 'bg-orange-100/70 text-[#fc8019] font-black'
                                : 'hover:bg-orange-50/60 text-slate-700'
                            }`}
                          >
                            <div className="flex items-center gap-2 truncate">
                              <span className="text-base">{cust.avatar}</span>
                              <div className="truncate">
                                <div className="text-xs font-bold truncate leading-tight">{cust.name}</div>
                                <div className="text-[10px] text-slate-400 truncate">{cust.badge}</div>
                              </div>
                            </div>
                            {isCurrent && <Check className="w-4 h-4 text-[#fc8019] shrink-0" />}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  {/* 3. Delivery Fleet Riders */}
                  <div className="border-t border-slate-100 pt-2">
                    <div className="px-3 py-1 text-[11px] font-black text-blue-600 uppercase tracking-wider flex items-center gap-1.5">
                      <Bike className="w-3.5 h-3.5" />
                      <span>Delivery Fleet (Riders)</span>
                    </div>
                    <div className="space-y-1 mt-1">
                      {SEEDED_ACCOUNTS.RIDERS.map((rider) => {
                        const isCurrent = user?.email === rider.email;
                        return (
                          <button
                            key={rider.id}
                            onClick={() => handleAccountSelect(rider, 'delivery')}
                            className={`w-full px-3 py-2 rounded-xl text-left flex items-center justify-between transition-colors cursor-pointer ${
                              isCurrent
                                ? 'bg-blue-100/70 text-blue-900 font-black'
                                : 'hover:bg-blue-50/60 text-slate-700'
                            }`}
                          >
                            <div className="flex items-center gap-2 truncate">
                              <span className="text-base">🛵</span>
                              <div className="truncate">
                                <div className="text-xs font-bold truncate leading-tight">{rider.name}</div>
                                <div className="text-[10px] text-slate-400 truncate">{rider.zone}</div>
                              </div>
                            </div>
                            {isCurrent && <Check className="w-4 h-4 text-blue-600 shrink-0" />}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                </div>

                <div className="px-3 pt-2.5 pb-1 border-t border-slate-100 flex flex-col gap-2">
                  <div className="flex items-center justify-between text-[11px]">
                    <span className="text-[10px] text-slate-400">Multi-Tab Session</span>
                    <button
                      onClick={logout}
                      className="font-bold text-red-600 hover:text-red-700 flex items-center gap-1 cursor-pointer"
                    >
                      <LogOut className="w-3 h-3" />
                      <span>Switch Portal / Sign Out</span>
                    </button>
                  </div>
                  <div className="grid grid-cols-2 gap-1.5 pt-1 border-t border-slate-50 text-[10px]">
                    <a
                      href="/?portal=partner"
                      target="_blank"
                      rel="noopener noreferrer"
                      className="px-2 py-1 rounded-lg bg-amber-50 text-amber-900 font-bold hover:bg-amber-100 flex items-center justify-center gap-1"
                    >
                      <span>Partner Tab</span>
                      <span>↗</span>
                    </a>
                    <a
                      href="/?portal=rider"
                      target="_blank"
                      rel="noopener noreferrer"
                      className="px-2 py-1 rounded-lg bg-blue-50 text-blue-900 font-bold hover:bg-blue-100 flex items-center justify-center gap-1"
                    >
                      <span>Rider Tab</span>
                      <span>↗</span>
                    </a>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Cart Icon with Counter */}
          <button
            onClick={openCart}
            className={`flex items-center gap-2 py-1.5 px-3 rounded-2xl font-bold text-xs sm:text-sm transition-all cursor-pointer ${
              totalItemCount > 0
                ? 'bg-emerald-600 text-white shadow-md shadow-emerald-600/20 hover:bg-emerald-700 scale-105'
                : 'text-slate-700 hover:text-[#fc8019]'
            }`}
          >
            <ShoppingBag className="w-4 h-4" />
            <span className="font-black">{totalItemCount}</span>
            <span className="hidden md:inline">Cart</span>
          </button>
        </nav>
      </div>
    </header>
  );
}
