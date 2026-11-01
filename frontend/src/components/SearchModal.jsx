import React, { useState, useEffect } from 'react';
import { Search, X, Star, Clock, ArrowRight, Utensils } from 'lucide-react';
import { api } from '../services/api';
import { getRestaurantImage } from '../data/swiggyData';

const POPULAR_SEARCHES = ['Biryani', 'Pizza', 'Burger', 'Paneer', 'Butter Chicken', 'Naan', 'Lassi', 'Dosa'];

export default function SearchModal({ isOpen, onClose, onSelectRestaurant }) {
  const [searchTerm, setSearchTerm] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!searchTerm.trim()) {
      setResults([]);
      return;
    }

    const timer = setTimeout(async () => {
      setLoading(true);
      try {
        const response = await api.getRestaurants({ search: searchTerm.trim() });
        setResults(response?.content || []);
      } catch (err) {
        console.error('Search error:', err);
      } finally {
        setLoading(false);
      }
    }, 250);

    return () => clearTimeout(timer);
  }, [searchTerm]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center bg-black/60 backdrop-blur-xs pt-12 sm:pt-20 px-4">
      <div className="bg-white rounded-3xl max-w-2xl w-full shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200 border border-slate-100 flex flex-col max-h-[85vh]">
        {/* Search Input Bar */}
        <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center gap-3 bg-white sticky top-0">
          <Search className="w-5 h-5 text-slate-400 shrink-0" />
          <input
            type="text"
            autoFocus
            placeholder="Search for restaurants, cuisines, and dishes..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="flex-1 text-base sm:text-lg font-medium text-slate-800 placeholder-slate-400 focus:outline-none"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              className="text-xs font-bold text-slate-400 hover:text-slate-600 px-2 py-1"
            >
              CLEAR
            </button>
          )}
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full hover:bg-slate-100 flex items-center justify-center text-slate-500 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search Body */}
        <div className="overflow-y-auto p-5 space-y-6">
          {/* Popular Cuisines Pill suggestions */}
          {!searchTerm && (
            <div>
              <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3">
                Popular Cuisines & Dishes
              </h4>
              <div className="flex flex-wrap gap-2">
                {POPULAR_SEARCHES.map((query) => (
                  <button
                    key={query}
                    onClick={() => setSearchTerm(query)}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-slate-50 hover:bg-orange-50 hover:text-[#fc8019] border border-slate-200 text-xs font-semibold text-slate-700 transition-colors"
                  >
                    <Utensils className="w-3 h-3 text-[#fc8019]" />
                    <span>{query}</span>
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Loading indicator */}
          {loading && (
            <div className="py-8 text-center text-slate-400 text-sm">
              <div className="w-6 h-6 border-2 border-[#fc8019] border-t-transparent rounded-full animate-spin mx-auto mb-2" />
              Searching top restaurants...
            </div>
          )}

          {/* Results List */}
          {!loading && results.length > 0 && (
            <div className="space-y-3">
              <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">
                Restaurants Matching "{searchTerm}" ({results.length})
              </h4>
              <div className="divide-y divide-slate-100">
                {results.map((r) => (
                  <div
                    key={r.id}
                    onClick={() => {
                      onSelectRestaurant(r);
                      onClose();
                    }}
                    className="py-3 flex items-center gap-4 hover:bg-slate-50 rounded-xl px-2 cursor-pointer transition-colors group"
                  >
                    <img
                      src={getRestaurantImage(r.id)}
                      alt={r.name}
                      className="w-16 h-16 rounded-xl object-cover bg-slate-200 shrink-0"
                    />
                    <div className="flex-1 min-w-0">
                      <h5 className="font-bold text-sm text-slate-800 truncate group-hover:text-[#fc8019] transition-colors">
                        {r.name}
                      </h5>
                      <div className="flex items-center gap-2 text-xs text-slate-500 mt-0.5">
                        <span className="flex items-center gap-0.5 font-bold text-emerald-700">
                          <Star className="w-3 h-3 fill-emerald-600 text-emerald-600" />
                          4.3
                        </span>
                        <span>•</span>
                        <span>25-30 mins</span>
                        <span>•</span>
                        <span className="truncate">{r.address}</span>
                      </div>
                      <p className="text-xs text-slate-400 truncate mt-0.5">{r.description}</p>
                    </div>
                    <ArrowRight className="w-4 h-4 text-slate-300 group-hover:text-[#fc8019] group-hover:translate-x-1 transition-all" />
                  </div>
                ))}
              </div>
            </div>
          )}

          {!loading && searchTerm && results.length === 0 && (
            <div className="py-12 text-center text-slate-500 text-sm">
              <p className="font-bold text-slate-700">No restaurants found for "{searchTerm}"</p>
              <p className="text-xs text-slate-400 mt-1">Try checking for typos or searching another cuisine</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

