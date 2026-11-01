import React, { useState, useEffect } from 'react';
import {
  ChefHat,
  Clock,
  CheckCircle2,
  PackageCheck,
  Plus,
  RefreshCw,
  Utensils,
  DollarSign,
  AlertCircle,
  Percent,
  Sparkles,
  Tag,
  ArrowRight,
  LogOut,
  ExternalLink,
} from 'lucide-react';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import VegNonVegIcon from '../components/VegNonVegIcon';

export default function OwnerDashboard() {
  const { user, logout } = useAuth();
  const [restaurants, setRestaurants] = useState([]);
  const [selectedRestaurant, setSelectedRestaurant] = useState(null);
  const [orders, setOrders] = useState([]);
  const [menuItems, setMenuItems] = useState([]);
  const [offers, setOffers] = useState([]);
  const [activeTab, setActiveTab] = useState('orders'); // 'orders' | 'menu' | 'offers'
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  // New menu item form state
  const [showAddMenuModal, setShowAddMenuModal] = useState(false);
  const [newItem, setNewItem] = useState({
    name: '',
    description: '',
    price: '',
    category: 'MAIN_COURSE',
    preparationTimeMinutes: 20,
    imageUrl: '',
  });

  // New offer form state
  const [showAddOfferModal, setShowAddOfferModal] = useState(false);
  const [newOffer, setNewOffer] = useState({
    code: '',
    title: '',
    description: '',
    discountPercentage: 20,
    maxDiscountAmount: 100,
    minOrderAmount: 199,
    bannerImageUrl: 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=800&auto=format&fit=crop&q=80',
    gradientColor: 'from-orange-600 to-amber-500',
  });

  // Fetch owner restaurants
  const fetchOwnerData = async () => {
    try {
      setLoading(true);
      const res = await api.getMyRestaurants();
      const list = res?.content || [];
      setRestaurants(list);
      if (list.length > 0) {
        // If current selected is not in list or null, default to first or matching outlet
        if (!selectedRestaurant || !list.some((r) => r.id === selectedRestaurant.id)) {
          setSelectedRestaurant(list[0]);
        }
      }
    } catch (err) {
      console.error('Failed to fetch owner restaurants:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOwnerData();
  }, [user?.email]);

  // Fetch orders, menu, and offers when selected restaurant changes
  const refreshRestaurantDetails = async () => {
    if (!selectedRestaurant?.id) return;
    try {
      const [orderRes, menuRes, offerRes] = await Promise.all([
        api.getOwnerRestaurantOrders(selectedRestaurant.id),
        api.getRestaurantMenu(selectedRestaurant.id, { size: 50 }),
        api.getRestaurantOffers(selectedRestaurant.id).catch(() => []),
      ]);
      setOrders(orderRes?.content || []);
      setMenuItems(menuRes?.content || []);
      setOffers(offerRes || []);
    } catch (err) {
      console.warn('Could not fetch restaurant details:', err.message);
    }
  };

  useEffect(() => {
    refreshRestaurantDetails();
    const interval = setInterval(refreshRestaurantDetails, 3500);
    return () => clearInterval(interval);
  }, [selectedRestaurant]);

  // Handle order status update
  const handleUpdateStatus = async (orderId, newStatus) => {
    try {
      setActionLoading(true);
      await api.updateOwnerOrderStatus(orderId, newStatus);
      await refreshRestaurantDetails();
    } catch (err) {
      alert(err.message || 'Failed to update order status');
    } finally {
      setActionLoading(false);
    }
  };

  // Handle add menu item
  const handleAddMenuItem = async (e) => {
    e.preventDefault();
    if (!selectedRestaurant?.id) return;
    try {
      setActionLoading(true);
      await api.createMenuItem(selectedRestaurant.id, {
        name: newItem.name,
        description: newItem.description,
        price: parseFloat(newItem.price),
        category: newItem.category,
        preparationTimeMinutes: parseInt(newItem.preparationTimeMinutes) || 15,
        imageUrl: newItem.imageUrl || null,
      });
      setShowAddMenuModal(false);
      setNewItem({
        name: '',
        description: '',
        price: '',
        category: 'MAIN_COURSE',
        preparationTimeMinutes: 20,
        imageUrl: '',
      });
      await refreshRestaurantDetails();
    } catch (err) {
      alert(err.message || 'Failed to add menu item');
    } finally {
      setActionLoading(false);
    }
  };

  // Handle create new offer in DB
  const handleCreateOffer = async (e) => {
    e.preventDefault();
    if (!selectedRestaurant?.id) return;
    try {
      setActionLoading(true);
      await api.createOffer(selectedRestaurant.id, {
        code: newOffer.code.trim().toUpperCase(),
        title: newOffer.title.trim(),
        description: newOffer.description,
        discountPercentage: parseInt(newOffer.discountPercentage),
        maxDiscountAmount: parseFloat(newOffer.maxDiscountAmount),
        minOrderAmount: parseFloat(newOffer.minOrderAmount) || 0,
        bannerImageUrl: newOffer.bannerImageUrl,
        gradientColor: newOffer.gradientColor,
      });
      setShowAddOfferModal(false);
      setNewOffer({
        code: '',
        title: '',
        description: '',
        discountPercentage: 20,
        maxDiscountAmount: 100,
        minOrderAmount: 199,
        bannerImageUrl: 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=800&auto=format&fit=crop&q=80',
        gradientColor: 'from-orange-600 to-amber-500',
      });
      await refreshRestaurantDetails();
    } catch (err) {
      alert(err.message || 'Failed to create promotional offer');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50/70 pb-20 pt-6">
      <div className="max-w-6xl mx-auto px-4">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-amber-500 text-slate-950 flex items-center justify-center shadow-md shadow-amber-500/20 font-black">
              <ChefHat className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-black text-slate-800 tracking-tight">
                Restaurant Kitchen Hub
              </h1>
              <p className="text-xs text-slate-500">
                Logged in as <strong className="text-amber-600 font-bold">{user?.name || user?.email}</strong> • Outlet: {selectedRestaurant?.name || 'Managing Kitchen'}
              </p>
            </div>
          </div>

          {/* Right Header Controls */}
          <div className="flex flex-wrap items-center gap-2">
            {/* Restaurant Outlet Selector */}
            {restaurants.length > 0 && (
              <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-2xl border border-slate-200 shadow-2xs">
                <span className="text-xs text-slate-400 font-bold">Outlet:</span>
                <select
                  value={selectedRestaurant?.id || ''}
                  onChange={(e) => {
                    const match = restaurants.find((r) => r.id === parseInt(e.target.value));
                    if (match) setSelectedRestaurant(match);
                  }}
                  className="text-xs font-bold text-slate-800 bg-transparent focus:outline-hidden cursor-pointer"
                >
                  {restaurants.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.name}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* Launch Other Roles in New Tab */}
            <div className="hidden sm:flex items-center gap-1 bg-white px-2 py-1 rounded-2xl border border-slate-200 text-xs">
              <a
                href="/?portal=customer"
                target="_blank"
                rel="noopener noreferrer"
                className="px-2 py-1 rounded-lg hover:bg-orange-50 text-orange-600 font-bold flex items-center gap-1"
                title="Open Customer Portal in new Chrome tab"
              >
                <span>Customer Tab</span>
                <ExternalLink className="w-3 h-3" />
              </a>
              <span className="text-slate-300">|</span>
              <a
                href="/?portal=rider"
                target="_blank"
                rel="noopener noreferrer"
                className="px-2 py-1 rounded-lg hover:bg-blue-50 text-blue-600 font-bold flex items-center gap-1"
                title="Open Delivery Fleet in new Chrome tab"
              >
                <span>Rider Tab</span>
                <ExternalLink className="w-3 h-3" />
              </a>
            </div>

            {/* Sign Out / Switch Portal in this Tab */}
            <button
              onClick={logout}
              className="flex items-center gap-1 px-3 py-1.5 rounded-2xl bg-white border border-red-200 text-red-600 hover:bg-red-50 text-xs font-bold shadow-2xs transition-colors cursor-pointer"
            >
              <LogOut className="w-3.5 h-3.5" />
              <span>Switch Portal</span>
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center gap-2 mb-6 border-b border-slate-200 pb-3">
          <button
            onClick={() => setActiveTab('orders')}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              activeTab === 'orders'
                ? 'bg-[#fc8019] text-white shadow-xs'
                : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
            }`}
          >
            <Clock className="w-3.5 h-3.5" />
            <span>Active Kitchen Orders ({orders.filter((o) => o.status !== 'DELIVERED').length})</span>
          </button>

          <button
            onClick={() => setActiveTab('menu')}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              activeTab === 'menu'
                ? 'bg-[#fc8019] text-white shadow-xs'
                : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
            }`}
          >
            <Utensils className="w-3.5 h-3.5" />
            <span>Menu Catalog ({menuItems.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('offers')}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              activeTab === 'offers'
                ? 'bg-amber-500 text-slate-950 font-black shadow-xs'
                : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
            }`}
          >
            <Percent className="w-3.5 h-3.5" />
            <span>Offers & Deals ({offers.length})</span>
          </button>
        </div>

        {/* ORDERS TAB */}
        {activeTab === 'orders' && (
          <div className="space-y-4">
            {orders.length === 0 ? (
              <div className="bg-white rounded-3xl p-12 text-center border border-slate-100">
                <Clock className="w-10 h-10 text-slate-300 mx-auto mb-2" />
                <h3 className="font-bold text-slate-700">No active kitchen orders right now</h3>
                <p className="text-xs text-slate-400 mt-1">
                  Place an order in the Customer view or run the simulation to see tickets arrive here in real-time!
                </p>
              </div>
            ) : (
              orders.map((order) => {
                return (
                  <div
                    key={order.id}
                    className="bg-white rounded-3xl p-5 sm:p-6 shadow-xs border border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-4"
                  >
                    <div className="space-y-2 flex-1">
                      <div className="flex items-center gap-3">
                        <span className="font-mono text-xs font-bold text-slate-500">
                          #{order.orderNumber || order.id}
                        </span>
                        <span
                          className={`text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full ${
                            order.status === 'PLACED'
                              ? 'bg-amber-100 text-amber-800 animate-pulse'
                              : order.status === 'ACCEPTED'
                              ? 'bg-blue-100 text-blue-800'
                              : order.status === 'PREPARING'
                              ? 'bg-purple-100 text-purple-800'
                              : order.status === 'READY_FOR_PICKUP'
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-slate-100 text-slate-600'
                          }`}
                        >
                          {order.status.replace(/_/g, ' ')}
                        </span>
                      </div>

                      {/* Items */}
                      <div className="flex flex-wrap gap-2 text-xs font-semibold text-slate-800 pt-1">
                        {order.items?.map((item) => (
                          <span
                            key={item.id}
                            className="bg-slate-50 border border-slate-200 px-2.5 py-1 rounded-lg"
                          >
                            {item.itemName} x{item.quantity}
                          </span>
                        ))}
                      </div>

                      <p className="text-xs text-slate-400">
                        Delivery to: {order.deliveryAddress} • Total Bill: ₹{order.totalAmount}
                      </p>
                    </div>

                    {/* Action Buttons to progress order */}
                    <div className="flex items-center gap-2 shrink-0">
                      {order.status === 'PLACED' && (
                        <button
                          onClick={() => handleUpdateStatus(order.id, 'ACCEPTED')}
                          disabled={actionLoading}
                          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-xs transition-colors cursor-pointer"
                        >
                          Accept Order
                        </button>
                      )}

                      {order.status === 'ACCEPTED' && (
                        <button
                          onClick={() => handleUpdateStatus(order.id, 'PREPARING')}
                          disabled={actionLoading}
                          className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white font-bold text-xs rounded-xl shadow-xs transition-colors cursor-pointer"
                        >
                          Start Kitchen Prep
                        </button>
                      )}

                      {order.status === 'PREPARING' && (
                        <button
                          onClick={() => handleUpdateStatus(order.id, 'READY_FOR_PICKUP')}
                          disabled={actionLoading}
                          className="px-4 py-2 bg-[#fc8019] hover:bg-[#e67210] text-white font-bold text-xs rounded-xl shadow-xs transition-colors cursor-pointer"
                        >
                          Ready for Rider Pickup
                        </button>
                      )}

                      {order.status === 'READY_FOR_PICKUP' && (
                        <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-3 py-1.5 rounded-xl border border-emerald-200 flex items-center gap-1">
                          <PackageCheck className="w-3.5 h-3.5" />
                          Awaiting Rider Dispatch
                        </span>
                      )}

                      {order.status === 'OUT_FOR_DELIVERY' && (
                        <span className="text-xs font-bold text-blue-700 bg-blue-50 px-3 py-1.5 rounded-xl border border-blue-200 animate-pulse">
                          🛵 Rider Out For Delivery
                        </span>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        )}

        {/* MENU CATALOG TAB */}
        {activeTab === 'menu' && (
          <div className="space-y-4">
            <div className="flex justify-between items-center mb-2">
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                {menuItems.length} Dishes in Catalog
              </span>
              <button
                onClick={() => setShowAddMenuModal(true)}
                className="flex items-center gap-1.5 px-4 py-2 bg-[#fc8019] text-white text-xs font-bold rounded-xl shadow-xs hover:bg-[#e67210] cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add New Dish</span>
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {menuItems.map((item) => (
                <div
                  key={item.id}
                  className="bg-white rounded-3xl p-4 shadow-xs border border-slate-100 flex gap-4 items-center"
                >
                  <img
                    src={item.imageUrl || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400'}
                    alt={item.name}
                    className="w-20 h-20 rounded-2xl object-cover shrink-0"
                  />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-1.5 mb-1">
                      <VegNonVegIcon isVeg={item.isVeg !== false} size="sm" />
                      <span className="text-xs font-black text-slate-800 truncate">{item.name}</span>
                    </div>
                    <div className="text-xs font-black text-slate-900">₹{parseFloat(item.price).toFixed(2)}</div>
                    <p className="text-[11px] text-slate-400 truncate mt-0.5">{item.description}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* OFFERS & DEALS TAB */}
        {activeTab === 'offers' && (
          <div className="space-y-4">
            <div className="flex justify-between items-center mb-2">
              <div>
                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block">
                  Promotional Offers & Discount Vouchers
                </span>
                <span className="text-[11px] text-slate-500">
                  Customers can enter these codes at checkout or click them in restaurant deals
                </span>
              </div>
              <button
                onClick={() => setShowAddOfferModal(true)}
                className="flex items-center gap-1.5 px-4 py-2 bg-amber-500 text-slate-950 text-xs font-black rounded-xl shadow-xs hover:bg-amber-400 cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Create New Promo Offer</span>
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {offers.map((offer) => (
                <div
                  key={offer.id}
                  className={`rounded-3xl p-5 text-white bg-gradient-to-r ${offer.gradientColor || 'from-orange-600 to-amber-500'} shadow-lg relative overflow-hidden flex flex-col justify-between`}
                >
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="bg-white/20 backdrop-blur-md px-3 py-1 rounded-full text-xs font-mono font-black tracking-wider uppercase border border-white/30">
                        CODE: {offer.code}
                      </span>
                      <span className="bg-white text-slate-900 px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase">
                        {offer.discountPercentage}% OFF
                      </span>
                    </div>
                    <h3 className="text-lg font-black tracking-tight">{offer.title}</h3>
                    <p className="text-xs text-white/80 mt-1">{offer.description}</p>
                  </div>

                  <div className="pt-4 border-t border-white/20 mt-4 flex items-center justify-between text-[11px] font-bold text-white/90">
                    <span>Max Discount: ₹{offer.maxDiscountAmount}</span>
                    <span>Min Order: ₹{offer.minOrderAmount}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Add New Dish Modal */}
      {showAddMenuModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-100">
            <h3 className="text-lg font-black text-slate-800 mb-4">Add New Menu Dish</h3>
            <form onSubmit={handleAddMenuItem} className="space-y-3">
              <div>
                <label className="block text-xs font-bold text-slate-600 mb-1">Dish Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Special Hyderabadi Dum Biryani"
                  value={newItem.name}
                  onChange={(e) => setNewItem({ ...newItem, name: e.target.value })}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none focus:border-[#fc8019]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-600 mb-1">Description</label>
                <textarea
                  rows={2}
                  placeholder="Fragrant basmati rice cooked with authentic spices..."
                  value={newItem.description}
                  onChange={(e) => setNewItem({ ...newItem, description: e.target.value })}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none focus:border-[#fc8019]"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-600 mb-1">Price (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    placeholder="250"
                    value={newItem.price}
                    onChange={(e) => setNewItem({ ...newItem, price: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none focus:border-[#fc8019]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-600 mb-1">Category</label>
                  <select
                    value={newItem.category}
                    onChange={(e) => setNewItem({ ...newItem, category: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none focus:border-[#fc8019]"
                  >
                    <option value="MAIN_COURSE">Main Course</option>
                    <option value="STARTER">Starter</option>
                    <option value="BREAD">Bread</option>
                    <option value="BEVERAGE">Beverage</option>
                    <option value="DESSERT">Dessert</option>
                  </select>
                </div>
              </div>

              <div className="flex gap-2 pt-3">
                <button
                  type="button"
                  onClick={() => setShowAddMenuModal(false)}
                  className="flex-1 py-2.5 rounded-xl border border-slate-200 font-bold text-xs text-slate-600 cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="flex-1 py-2.5 rounded-xl bg-[#fc8019] text-white font-bold text-xs hover:bg-[#e67210] cursor-pointer"
                >
                  {actionLoading ? 'Saving...' : 'Save Dish'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Create New Promo Offer Modal */}
      {showAddOfferModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-100">
            <h3 className="text-lg font-black text-slate-800 mb-4 flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-amber-500" />
              <span>Create Restaurant Promo Offer</span>
            </h3>

            <form onSubmit={handleCreateOffer} className="space-y-3">
              <div>
                <label className="block text-xs font-bold text-slate-600 mb-1">Coupon Code (e.g. MEGHANA30)</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. FEAST25"
                  value={newOffer.code}
                  onChange={(e) => setNewOffer({ ...newOffer, code: e.target.value.toUpperCase() })}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs font-mono font-bold focus:outline-none focus:border-amber-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-600 mb-1">Offer Title</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Biryani Bonanza 25% OFF"
                  value={newOffer.title}
                  onChange={(e) => setNewOffer({ ...newOffer, title: e.target.value })}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none focus:border-amber-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-600 mb-1">Description</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. 25% OFF on orders above ₹249"
                  value={newOffer.description}
                  onChange={(e) => setNewOffer({ ...newOffer, description: e.target.value })}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none focus:border-amber-500"
                />
              </div>

              <div className="grid grid-cols-3 gap-2">
                <div>
                  <label className="block text-[11px] font-bold text-slate-600 mb-1">Discount %</label>
                  <input
                    type="number"
                    min="5"
                    max="90"
                    required
                    value={newOffer.discountPercentage}
                    onChange={(e) => setNewOffer({ ...newOffer, discountPercentage: e.target.value })}
                    className="w-full px-2.5 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none focus:border-amber-500"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-slate-600 mb-1">Max Cap (₹)</label>
                  <input
                    type="number"
                    min="10"
                    required
                    value={newOffer.maxDiscountAmount}
                    onChange={(e) => setNewOffer({ ...newOffer, maxDiscountAmount: e.target.value })}
                    className="w-full px-2.5 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none focus:border-amber-500"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-slate-600 mb-1">Min Order (₹)</label>
                  <input
                    type="number"
                    min="0"
                    value={newOffer.minOrderAmount}
                    onChange={(e) => setNewOffer({ ...newOffer, minOrderAmount: e.target.value })}
                    className="w-full px-2.5 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none focus:border-amber-500"
                  />
                </div>
              </div>

              <div className="flex gap-2 pt-3">
                <button
                  type="button"
                  onClick={() => setShowAddOfferModal(false)}
                  className="flex-1 py-2.5 rounded-xl border border-slate-200 font-bold text-xs text-slate-600 cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="flex-1 py-2.5 rounded-xl bg-amber-500 text-slate-950 font-black text-xs hover:bg-amber-400 cursor-pointer shadow-md shadow-amber-500/20"
                >
                  {actionLoading ? 'Creating...' : 'Publish Offer to DB'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
