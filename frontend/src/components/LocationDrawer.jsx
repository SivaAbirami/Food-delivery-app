import React, { useState } from 'react';
import { X, MapPin, Search, Navigation, CheckCircle2, ChevronRight, Home, Briefcase, Compass, Loader2 } from 'lucide-react';
import { useLocation } from '../context/LocationContext';

export default function LocationDrawer() {
  const {
    currentLocation,
    selectLocation,
    detectCurrentLocation,
    gpsLoading,
    isLocationModalOpen,
    closeLocationModal,
    availableLocations,
  } = useLocation();
  const [searchTerm, setSearchTerm] = useState('');

  if (!isLocationModalOpen) return null;

  const filteredLocations = availableLocations.filter(
    (loc) =>
      loc.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      loc.area.toLowerCase().includes(searchTerm.toLowerCase()) ||
      loc.zone.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-xs transition-opacity"
        onClick={closeLocationModal}
      />

      <div className="fixed inset-y-0 left-0 max-w-full flex pr-10">
        <div className="w-screen max-w-md bg-white shadow-2xl flex flex-col animate-in slide-in-from-left duration-300">
          {/* Header */}
          <div className="p-5 border-b border-slate-100 flex items-center justify-between bg-white sticky top-0 z-10">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-orange-100 flex items-center justify-center text-[#fc8019]">
                <MapPin className="w-4 h-4" />
              </div>
              <div>
                <h2 className="font-black text-slate-800 text-base leading-tight">Select Delivery Location</h2>
                <p className="text-[11px] text-slate-400">Bengaluru Delivery Network</p>
              </div>
            </div>
            <button
              onClick={closeLocationModal}
              className="w-8 h-8 rounded-full hover:bg-slate-100 flex items-center justify-center text-slate-500 transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <div className="p-5 flex-1 overflow-y-auto space-y-5">
            {/* Real Search Input */}
            <div className="relative">
              <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search for area, street name..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-200 text-xs sm:text-sm text-slate-700 placeholder-slate-400 focus:outline-none focus:border-[#fc8019]"
                autoFocus
              />
            </div>

            {/* GPS Live Geolocation Button */}
            <button
              onClick={detectCurrentLocation}
              disabled={gpsLoading}
              className="w-full p-4 rounded-2xl border border-orange-200 hover:border-[#fc8019] bg-gradient-to-r from-orange-50/60 to-amber-50/40 flex items-center gap-3 transition-colors text-left group cursor-pointer shadow-2xs"
            >
              <div className="w-10 h-10 rounded-2xl bg-[#fc8019] text-white flex items-center justify-center shrink-0 shadow-md shadow-orange-500/20 group-hover:scale-105 transition-transform">
                {gpsLoading ? (
                  <Loader2 className="w-5 h-5 animate-spin" />
                ) : (
                  <Navigation className="w-5 h-5" />
                )}
              </div>
              <div className="flex-1">
                <span className="font-black text-sm text-slate-900 group-hover:text-[#fc8019] transition-colors block">
                  {gpsLoading ? 'Detecting GPS Coordinates...' : 'Use Current Location'}
                </span>
                <span className="text-xs text-slate-500">
                  Using Device GPS / Browser Geolocation
                </span>
              </div>
            </button>

            {/* Quick Bengaluru Delivery Hubs */}
            <div>
              <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                <Compass className="w-3.5 h-3.5 text-slate-400" />
                <span>Popular Delivery Zones</span>
              </h4>

              <div className="flex flex-wrap gap-1.5 mb-4">
                {availableLocations.slice(0, 5).map((loc) => (
                  <button
                    key={loc.id}
                    onClick={() => selectLocation(loc)}
                    className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                      currentLocation.id === loc.id
                        ? 'bg-[#fc8019] text-white shadow-xs'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    {loc.name}
                  </button>
                ))}
              </div>

              {/* Detailed Location List */}
              <div className="divide-y divide-slate-100 border border-slate-100 rounded-2xl overflow-hidden bg-white shadow-2xs">
                {filteredLocations.map((loc) => {
                  const isSelected = currentLocation.id === loc.id;
                  return (
                    <div
                      key={loc.id}
                      onClick={() => selectLocation(loc)}
                      className={`p-3.5 flex items-start justify-between gap-3 cursor-pointer transition-colors ${
                        isSelected ? 'bg-orange-50/70' : 'hover:bg-slate-50'
                      }`}
                    >
                      <div className="flex items-start gap-3">
                        <div
                          className={`w-7 h-7 rounded-xl flex items-center justify-center shrink-0 mt-0.5 ${
                            isSelected
                              ? 'bg-orange-200/60 text-[#fc8019]'
                              : 'bg-slate-100 text-slate-500'
                          }`}
                        >
                          <MapPin className="w-4 h-4" />
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <span
                              className={`font-black text-sm block leading-tight ${
                                isSelected ? 'text-[#fc8019]' : 'text-slate-900'
                              }`}
                            >
                              {loc.name}
                            </span>
                            <span className="text-[10px] font-bold text-slate-400 bg-slate-100 px-1.5 py-0.5 rounded-md">
                              {loc.zone}
                            </span>
                          </div>
                          <span className="text-xs text-slate-500 block mt-1 leading-snug">
                            {loc.fullAddress}
                          </span>
                        </div>
                      </div>

                      {isSelected ? (
                        <CheckCircle2 className="w-5 h-5 text-[#fc8019] shrink-0 mt-1" />
                      ) : (
                        <ChevronRight className="w-4 h-4 text-slate-300 shrink-0 mt-1" />
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
