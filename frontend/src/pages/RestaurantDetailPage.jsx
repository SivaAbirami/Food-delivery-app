import React, { useState, useEffect } from 'react';
import {
  Star,
  Clock,
  MapPin,
  Search,
  ChevronDown,
  ChevronUp,
  Percent,
  Plus,
  Minus,
  Sparkles,
  ArrowLeft,
  Info,
} from 'lucide-react';
import { api } from '../services/api';
import { useCart } from '../context/CartContext';
import { getRestaurantImage, getDishImage } from '../data/swiggyData';
import VegNonVegIcon from '../components/VegNonVegIcon';

export default function RestaurantDetailPage({ restaurant, onBack }) {
  const { cart, addItem, updateQuantity } = useCart();
  const [menuItems, setMenuItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters
  const [vegOnly, setVegOnly] = useState(false);
  const [searchMenu, setSearchMenu] = useState('');
  const [collapsedCategories, setCollapsedCategories] = useState({});

  useEffect(() => {
    async function fetchMenu() {
      if (!restaurant?.id) return;
      try {
        setLoading(true);
        const data = await api.getRestaurantMenu(restaurant.id, { size: 50 });
        setMenuItems(data?.content || []);
      } catch (err) {
        setError(err.message || 'Failed to fetch menu items');
      } finally {
        setLoading(false);
      }
    }
    fetchMenu();
  }, [restaurant]);

  // Group menu items by category
  const categoriesMap = menuItems.reduce((acc, item) => {
    // Veg filter
    const isVeg =
      !item.name?.toLowerCase().includes('chicken') &&
      !item.name?.toLowerCase().includes('meat') &&
      !item.name?.toLowerCase().includes('mutton') &&
      !item.name?.toLowerCase().includes('fish');

    if (vegOnly && !isVeg) return acc;

    // Search filter
    if (searchMenu.trim()) {
      const q = searchMenu.toLowerCase();
      const match = item.name?.toLowerCase().includes(q) || item.description?.toLowerCase().includes(q);
      if (!match) return acc;
    }

    const cat = item.category || 'RECOMMENDED';
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(item);
    return acc;
  }, {});

  const toggleCategory = (cat) => {
    setCollapsedCategories((prev) => ({ ...prev, [cat]: !prev[cat] }));
  };

  // Helper to find quantity in cart
  const getItemQuantityInCart = (itemId) => {
    if (!cart || !cart.items) return 0;
    const found = cart.items.find((i) => i.menuItemId === itemId);
    return found ? found.quantity : 0;
  };

  const getCartItemId = (itemId) => {
    if (!cart || !cart.items) return null;
    const found = cart.items.find((i) => i.menuItemId === itemId);
    return found ? found.id : null;
  };

  const rating = (4.1 + ((restaurant?.id * 7) % 8) / 10).toFixed(1);
  const etaMinutes = 20 + ((restaurant?.id * 5) % 25);

  return (
    <div className="min-h-screen bg-slate-50/50 pb-20">
      {/* Breadcrumb Bar */}
      <div className="max-w-4xl mx-auto px-4 pt-4 pb-2">
        <button
          onClick={onBack}
          className="inline-flex items-center gap-1.5 text-xs font-bold text-slate-500 hover:text-[#fc8019] transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          Back to Restaurants
        </button>
      </div>

      {/* Restaurant Header Card */}
      <section className="max-w-4xl mx-auto px-4">
        <div className="bg-white rounded-3xl p-5 sm:p-6 shadow-sm border border-slate-100">
          <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
            <div>
              <h1 className="text-2xl sm:text-3xl font-black text-slate-800 tracking-tight">
                {restaurant?.name}
              </h1>
              <p className="text-xs sm:text-sm text-slate-500 mt-1">
                {restaurant?.description || 'North Indian, Biryani, Mughlai, Desserts'}
              </p>
              <p className="text-xs text-slate-400 mt-0.5 flex items-center gap-1">
                <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                {restaurant?.address}
              </p>
            </div>

            {/* Rating Box */}
            <div className="border border-slate-100 p-2.5 rounded-2xl text-center shrink-0 flex sm:flex-col items-center justify-between sm:justify-center gap-1 bg-slate-50/50">
              <div className="flex items-center gap-1 text-emerald-700 font-black text-sm">
                <Star className="w-4 h-4 fill-emerald-600 text-emerald-600" />
                <span>{rating}</span>
              </div>
              <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">
                1K+ ratings
              </span>
            </div>
          </div>

          <div className="border-t border-dashed border-slate-200 mt-4 pt-4 flex flex-wrap items-center gap-4 text-xs font-bold text-slate-700">
            <div className="flex items-center gap-1.5">
              <Clock className="w-4 h-4 text-slate-500" />
              <span>{etaMinutes}-{etaMinutes + 10} MINS</span>
            </div>
            <span className="text-slate-300">•</span>
            <div className="flex items-center gap-1">
              <span>₹350 for two</span>
            </div>
            <span className="text-slate-300">•</span>
            <div className="flex items-center gap-1 text-slate-500">
              <span>₹25 Delivery fee applies</span>
            </div>
          </div>
        </div>
      </section>

      {/* Menu Search and Veg Filter Bar */}
      <section className="max-w-4xl mx-auto px-4 mt-6 sticky top-18 z-20 bg-slate-50/90 backdrop-blur-xs py-2">
        <div className="bg-white p-3 rounded-2xl shadow-xs border border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-3">
          {/* Veg Only Toggle */}
          <div className="flex items-center gap-2 self-start sm:self-auto">
            <button
              onClick={() => setVegOnly(!vegOnly)}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold transition-all border ${
                vegOnly
                  ? 'bg-emerald-50 text-emerald-800 border-emerald-300 shadow-2xs'
                  : 'bg-white text-slate-600 border-slate-200 hover:border-slate-300'
              }`}
            >
              <VegNonVegIcon isVeg={true} size="sm" />
              <span>Veg Only</span>
            </button>
          </div>

          {/* Search inside menu */}
          <div className="relative w-full sm:w-64">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search for dishes..."
              value={searchMenu}
              onChange={(e) => setSearchMenu(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 bg-slate-50 rounded-xl text-xs text-slate-700 placeholder-slate-400 border border-slate-200 focus:outline-none focus:border-[#fc8019]"
            />
          </div>
        </div>
      </section>

      {/* Menu Categories Accordion */}
      <section className="max-w-4xl mx-auto px-4 mt-6 space-y-4">
        {loading && (
          <div className="py-12 text-center text-slate-400 text-sm">
            <div className="w-8 h-8 border-3 border-[#fc8019] border-t-transparent rounded-full animate-spin mx-auto mb-2" />
            Loading restaurant menu...
          </div>
        )}

        {!loading && Object.keys(categoriesMap).length === 0 && (
          <div className="bg-white rounded-3xl p-8 text-center text-slate-500 border border-slate-100">
            <p className="font-bold text-base text-slate-800">No dishes match your filters</p>
            <p className="text-xs text-slate-400 mt-1">Try turning off "Veg Only" or clearing the search query.</p>
          </div>
        )}

        {!loading &&
          Object.entries(categoriesMap).map(([category, items]) => {
            const isCollapsed = collapsedCategories[category];
            return (
              <div
                key={category}
                className="bg-white rounded-3xl p-5 shadow-xs border border-slate-100 overflow-hidden"
              >
                {/* Category Header */}
                <button
                  onClick={() => toggleCategory(category)}
                  className="w-full flex items-center justify-between pb-3 border-b border-slate-100 text-left font-black text-lg text-slate-800"
                >
                  <span className="capitalize tracking-tight">
                    {category.toLowerCase().replace('_', ' ')} ({items.length})
                  </span>
                  {isCollapsed ? (
                    <ChevronDown className="w-5 h-5 text-slate-400" />
                  ) : (
                    <ChevronUp className="w-5 h-5 text-slate-400" />
                  )}
                </button>

                {/* Items in Category */}
                {!isCollapsed && (
                  <div className="divide-y divide-slate-100">
                    {items.map((item) => {
                      const qty = getItemQuantityInCart(item.id);
                      const cartItemId = getCartItemId(item.id);
                      const isVeg =
                        !item.name?.toLowerCase().includes('chicken') &&
                        !item.name?.toLowerCase().includes('meat') &&
                        !item.name?.toLowerCase().includes('mutton') &&
                        !item.name?.toLowerCase().includes('fish');

                      return (
                        <div
                          key={item.id}
                          className="py-5 flex items-start justify-between gap-4 group"
                        >
                          {/* Dish Info */}
                          <div className="flex-1 min-w-0 pr-2">
                            <div className="flex items-center gap-2 mb-1">
                              <VegNonVegIcon isVeg={isVeg} />
                              {item.id % 2 === 0 && (
                                <span className="inline-flex items-center gap-0.5 text-[10px] font-black text-amber-800 bg-amber-50 px-1.5 py-0.5 rounded-sm">
                                  <Sparkles className="w-2.5 h-2.5 text-amber-500 fill-amber-500" />
                                  Bestseller
                                </span>
                              )}
                            </div>

                            <h3 className="font-bold text-base text-slate-800 group-hover:text-[#fc8019] transition-colors">
                              {item.name}
                            </h3>
                            <p className="font-bold text-sm text-slate-700 mt-0.5">₹{item.price}</p>
                            <p className="text-xs text-slate-400 line-clamp-2 mt-1 leading-relaxed">
                              {item.description || 'Delicious freshly prepared dish with premium authentic spices.'}
                            </p>
                          </div>

                          {/* Dish Image + Swiggy ADD Stepper */}
                          <div className="relative w-28 h-24 sm:w-36 sm:h-28 shrink-0 flex flex-col items-center">
                            <img
                              src={getDishImage(item)}
                              alt={item.name}
                              className="w-full h-full object-cover rounded-2xl bg-slate-100 shadow-2xs"
                              loading="lazy"
                            />

                            {/* Floating Swiggy ADD Button / Stepper */}
                            <div className="absolute -bottom-2.5">
                              {qty === 0 ? (
                                <button
                                  onClick={() => addItem(item, restaurant)}
                                  className="px-6 py-1.5 bg-white border border-slate-200 text-emerald-600 font-black text-xs uppercase rounded-lg shadow-md hover:bg-emerald-50 hover:border-emerald-300 transition-all hover:scale-105 active:scale-95"
                                >
                                  ADD
                                </button>
                              ) : (
                                <div className="flex items-center bg-white border border-emerald-600 rounded-lg shadow-md overflow-hidden">
                                  <button
                                    onClick={() => updateQuantity(cartItemId, qty - 1)}
                                    className="w-7 h-7 flex items-center justify-center text-emerald-600 hover:bg-emerald-50 transition-colors font-bold"
                                  >
                                    <Minus className="w-3.5 h-3.5" />
                                  </button>
                                  <span className="w-6 text-center text-xs font-black text-emerald-600">
                                    {qty}
                                  </span>
                                  <button
                                    onClick={() => updateQuantity(cartItemId, qty + 1)}
                                    className="w-7 h-7 flex items-center justify-center text-emerald-600 hover:bg-emerald-50 transition-colors font-bold"
                                  >
                                    <Plus className="w-3.5 h-3.5" />
                                  </button>
                                </div>
                              )}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
      </section>
    </div>
  );
}

