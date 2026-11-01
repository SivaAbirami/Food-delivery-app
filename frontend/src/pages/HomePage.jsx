import React, { useState, useEffect } from 'react';
import {
  Sparkles,
  SlidersHorizontal,
  ArrowUpDown,
  Flame,
  Star,
  Clock,
  ChevronRight,
  ShieldCheck,
  Search,
  MapPin,
} from 'lucide-react';
import { api } from '../services/api';
import { CUISINES_LIST, PROMO_BANNERS } from '../data/swiggyData';
import { useLocation } from '../context/LocationContext';
import RestaurantCard from '../components/RestaurantCard';

export default function HomePage({ onSelectRestaurant, onOpenSearch }) {
  const { currentLocation, openLocationModal } = useLocation();
  const [restaurants, setRestaurants] = useState([]);
  const [offers, setOffers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters & sorting
  const [selectedCuisine, setSelectedCuisine] = useState(null);
  const [filterRating4Plus, setFilterRating4Plus] = useState(false);
  const [filterFastDelivery, setFilterFastDelivery] = useState(false);
  const [filterPureVeg, setFilterPureVeg] = useState(false);
  const [sortBy, setSortBy] = useState('relevance');

  useEffect(() => {
    async function fetchPageData() {
      try {
        setLoading(true);
        const [restData, offerData] = await Promise.all([
          api.getRestaurants({ size: 50 }),
          api.getOffers().catch(() => []),
        ]);
        setRestaurants(restData?.content || []);
        setOffers(offerData && offerData.length > 0 ? offerData : PROMO_BANNERS);
      } catch (err) {
        setError(err.message || 'Failed to load data from database');
      } finally {
        setLoading(false);
      }
    }
    fetchPageData();
  }, []);

  // Filter and sort computation
  const filteredRestaurants = restaurants.filter((r) => {
    // Pure Veg filter
    if (filterPureVeg) {
      const isVeg =
        r.name?.toLowerCase().includes('pure veg') ||
        r.description?.toLowerCase().includes('pure veg') ||
        r.name?.toLowerCase().includes('anand') ||
        r.name?.toLowerCase().includes('a2b');
      if (!isVeg) return false;
    }

    // Fast Delivery filter (< 30 mins)
    if (filterFastDelivery) {
      const isNearby = r.address?.toLowerCase().includes(currentLocation.name.toLowerCase());
      if (!isNearby && r.id % 2 !== 0) return false;
    }

    // Rating filter (4.0+)
    if (filterRating4Plus) {
      const simulatedRating = 4.1 + ((r.id * 7) % 8) / 10;
      if (simulatedRating < 4.3) return false;
    }

    // Cuisine Filter
    if (selectedCuisine) {
      const matchesName = r.name?.toLowerCase().includes(selectedCuisine.toLowerCase());
      const matchesDesc = r.description?.toLowerCase().includes(selectedCuisine.toLowerCase());
      if (!matchesName && !matchesDesc) return false;
    }

    return true;
  });

  return (
    <div className="min-h-screen bg-white pb-16">
      {/* Promotional Swiggy Hero Banners */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-6 pb-4">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {offers.slice(0, 3).map((banner) => (
            <div
              key={banner.id || banner.code}
              className={`relative h-44 rounded-3xl overflow-hidden shadow-sm bg-gradient-to-r ${banner.gradientColor || banner.color || 'from-orange-600 to-amber-500'} p-5 flex flex-col justify-between text-white cursor-pointer group hover:shadow-md transition-shadow`}
            >
              <div className="absolute right-0 bottom-0 w-44 h-44 rounded-full overflow-hidden opacity-90 group-hover:scale-105 transition-transform">
                <img
                  src={banner.bannerImageUrl || banner.image || 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800'}
                  alt={banner.title}
                  className="w-full h-full object-cover"
                />
              </div>
              <div className="relative z-10 max-w-[200px]">
                <span className="text-[10px] uppercase tracking-wider font-extrabold bg-white/25 backdrop-blur-xs px-2.5 py-0.5 rounded-full border border-white/30">
                  CODE: {banner.code}
                </span>
                <h3 className="text-xl font-black mt-2 leading-tight drop-shadow-xs">{banner.title}</h3>
                <p className="text-xs font-bold text-white/90 drop-shadow-xs mt-0.5">{banner.description || banner.subtitle}</p>
              </div>
              <div className="relative z-10">
                <span className="text-xs font-black underline underline-offset-4 flex items-center gap-1">
                  Order with {banner.discountPercentage || 50}% OFF <ChevronRight className="w-3.5 h-3.5" />
                </span>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* "What's on your mind?" - Swiggy's Signature Cuisine Carousel */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 border-b border-slate-100">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl md:text-2xl font-black text-slate-800 tracking-tight flex items-center gap-2">
            What's on your mind?
          </h2>
          {selectedCuisine && (
            <button
              onClick={() => setSelectedCuisine(null)}
              className="text-xs font-bold text-[#fc8019] hover:underline"
            >
              CLEAR FILTER
            </button>
          )}
        </div>

        <div className="flex gap-4 sm:gap-6 overflow-x-auto no-scrollbar pb-2">
          {CUISINES_LIST.map((cuisine) => {
            const isSelected = selectedCuisine === cuisine.name;
            return (
              <div
                key={cuisine.id}
                onClick={() => setSelectedCuisine(isSelected ? null : cuisine.name)}
                className="flex flex-col items-center shrink-0 cursor-pointer group"
              >
                <div
                  className={`w-20 h-20 sm:w-28 sm:h-28 rounded-full overflow-hidden p-0.5 transition-all duration-200 group-hover:scale-105 ${
                    isSelected ? 'ring-3 ring-[#fc8019] ring-offset-2' : 'ring-1 ring-slate-200'
                  }`}
                >
                  <img
                    src={cuisine.image}
                    alt={cuisine.name}
                    className="w-full h-full object-cover rounded-full"
                    loading="lazy"
                  />
                </div>
                <span
                  className={`mt-2 text-xs sm:text-sm font-bold transition-colors ${
                    isSelected ? 'text-[#fc8019]' : 'text-slate-700 group-hover:text-slate-900'
                  }`}
                >
                  {cuisine.name}
                </span>
              </div>
            );
          })}
        </div>
      </section>

      {/* Top Restaurant Chains in Active Location */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
          <div>
            <h2 className="text-xl md:text-2xl font-black text-slate-800 tracking-tight flex items-center gap-2">
              <span>Restaurants with online food delivery in {currentLocation.name}</span>
              <button
                onClick={openLocationModal}
                className="text-xs font-bold text-[#fc8019] underline underline-offset-2 hover:text-[#e67210]"
              >
                (Change)
              </button>
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">
              Serving {currentLocation.area} • {filteredRestaurants.length} restaurants live from database
            </p>
          </div>

          {/* Quick Filter Chips */}
          <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pb-1">
            {/* Rating 4.0+ */}
            <button
              onClick={() => setFilterRating4Plus(!filterRating4Plus)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-bold shrink-0 transition-colors border ${
                filterRating4Plus
                  ? 'bg-slate-900 text-white border-slate-900'
                  : 'bg-white text-slate-700 border-slate-300 hover:border-slate-400'
              }`}
            >
              Ratings 4.0+
            </button>

            {/* Fast Delivery */}
            <button
              onClick={() => setFilterFastDelivery(!filterFastDelivery)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-bold shrink-0 transition-colors border ${
                filterFastDelivery
                  ? 'bg-slate-900 text-white border-slate-900'
                  : 'bg-white text-slate-700 border-slate-300 hover:border-slate-400'
              }`}
            >
              Fast Delivery (&lt;30m)
            </button>

            {/* Pure Veg */}
            <button
              onClick={() => setFilterPureVeg(!filterPureVeg)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-bold shrink-0 transition-colors border ${
                filterPureVeg
                  ? 'bg-emerald-700 text-white border-emerald-700'
                  : 'bg-white text-slate-700 border-slate-300 hover:border-slate-400'
              }`}
            >
              Pure Veg
            </button>

            {/* Offers */}
            <button
              onClick={() => alert('All listed restaurants have live offers active!')}
              className="px-3.5 py-1.5 rounded-full text-xs font-bold shrink-0 border border-slate-300 text-slate-700 hover:border-slate-400 bg-white"
            >
              Offers
            </button>
          </div>
        </div>

        {/* Loading state */}
        {loading && (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {[...Array(8)].map((_, i) => (
              <div key={i} className="animate-pulse space-y-3">
                <div className="w-full aspect-[16/10] bg-slate-200 rounded-2xl" />
                <div className="h-4 bg-slate-200 rounded w-3/4" />
                <div className="h-3 bg-slate-200 rounded w-1/2" />
              </div>
            ))}
          </div>
        )}

        {/* Error state */}
        {!loading && error && (
          <div className="p-8 text-center bg-red-50 rounded-2xl border border-red-200 text-red-700">
            <p className="font-bold">Could not connect to Spring Boot backend</p>
            <p className="text-xs mt-1 text-red-600">{error}</p>
          </div>
        )}

        {/* Restaurants Grid */}
        {!loading && !error && (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-x-6 gap-y-9">
            {filteredRestaurants.map((restaurant) => (
              <RestaurantCard
                key={restaurant.id}
                restaurant={restaurant}
                onClick={() => onSelectRestaurant(restaurant)}
              />
            ))}
          </div>
        )}

        {!loading && !error && filteredRestaurants.length === 0 && (
          <div className="py-16 text-center text-slate-500">
            <p className="font-bold text-lg text-slate-800">No restaurants match your filters</p>
            <p className="text-xs text-slate-400 mt-1">Try clearing some filter chips to view all restaurants.</p>
            <button
              onClick={() => {
                setSelectedCuisine(null);
                setFilterRating4Plus(false);
                setFilterFastDelivery(false);
                setFilterPureVeg(false);
              }}
              className="mt-4 px-4 py-2 rounded-xl bg-[#fc8019] text-white font-bold text-xs"
            >
              Reset All Filters
            </button>
          </div>
        )}
      </section>
    </div>
  );
}
