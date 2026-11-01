import React, { useState, useEffect } from 'react';
import { ShoppingBag, ArrowLeft, Clock, ArrowRight, CheckCircle2, ChevronRight } from 'lucide-react';
import { api } from '../services/api';
import VegNonVegIcon from '../components/VegNonVegIcon';

export default function OrdersHistoryPage({ onTrackOrder, onBackToHome }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function fetchOrders() {
      try {
        setLoading(true);
        const data = await api.getCustomerOrders(0, 20);
        setOrders(data?.content || []);
      } catch (err) {
        setError(err.message || 'Failed to fetch orders');
      } finally {
        setLoading(false);
      }
    }
    fetchOrders();
  }, []);

  return (
    <div className="min-h-screen bg-slate-50/70 pb-20 pt-6">
      <div className="max-w-4xl mx-auto px-4">
        {/* Navigation */}
        <div className="flex items-center justify-between mb-6">
          <button
            onClick={onBackToHome}
            className="inline-flex items-center gap-1.5 text-xs font-bold text-slate-600 hover:text-[#fc8019] transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            Back to Restaurants
          </button>
        </div>

        <h1 className="text-2xl font-black text-slate-800 tracking-tight mb-2">Past Orders</h1>
        <p className="text-xs text-slate-400 mb-6">Track live deliveries or view previous receipts</p>

        {loading && (
          <div className="py-16 text-center text-slate-400 text-sm">
            <div className="w-8 h-8 border-3 border-[#fc8019] border-t-transparent rounded-full animate-spin mx-auto mb-2" />
            Loading your orders...
          </div>
        )}

        {!loading && orders.length === 0 && (
          <div className="bg-white rounded-3xl p-10 text-center shadow-xs border border-slate-100 max-w-md mx-auto">
            <ShoppingBag className="w-12 h-12 text-[#fc8019] mx-auto mb-3" />
            <h3 className="font-bold text-lg text-slate-800">No orders placed yet</h3>
            <p className="text-xs text-slate-500 mt-1 mb-6">
              When you order from restaurants, your receipt and live status will appear here.
            </p>
            <button
              onClick={onBackToHome}
              className="px-6 py-2.5 rounded-xl bg-[#fc8019] text-white font-bold text-xs"
            >
              Start Ordering
            </button>
          </div>
        )}

        {!loading && orders.length > 0 && (
          <div className="space-y-4">
            {orders.map((order) => {
              const isDelivered = order.status === 'DELIVERED';
              const isCancelled = order.status === 'CANCELLED';

              return (
                <div
                  key={order.id}
                  className="bg-white rounded-3xl p-5 sm:p-6 shadow-xs border border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:shadow-md transition-shadow"
                >
                  <div className="space-y-2 flex-1">
                    <div className="flex items-center gap-3">
                      <h3 className="font-black text-base sm:text-lg text-slate-800">
                        {order.restaurantName || 'Restaurant'}
                      </h3>
                      <span
                        className={`text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full ${
                          isDelivered
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : isCancelled
                            ? 'bg-red-50 text-red-700 border border-red-200'
                            : 'bg-orange-50 text-[#fc8019] border border-orange-200 animate-pulse'
                        }`}
                      >
                        {order.status.replace(/_/g, ' ')}
                      </span>
                    </div>

                    <p className="text-xs text-slate-400 font-mono">
                      Order #{order.orderNumber || order.id} • {new Date(order.createdAt).toLocaleDateString()}
                    </p>

                    <div className="flex flex-wrap gap-2 text-xs text-slate-600 pt-1">
                      {order.items?.map((item) => (
                        <span key={item.id} className="bg-slate-50 px-2.5 py-1 rounded-lg border border-slate-100 font-medium">
                          {item.itemName} x{item.quantity}
                        </span>
                      ))}
                    </div>

                    <div className="pt-1 text-xs">
                      <span className="font-bold text-slate-900">Total: ₹{order.totalAmount}</span>
                      <span className="text-slate-400"> (includes taxes and delivery)</span>
                    </div>
                  </div>

                  <div className="flex sm:flex-col items-center sm:items-end justify-between gap-2 shrink-0 border-t sm:border-t-0 pt-3 sm:pt-0 border-slate-100">
                    <button
                      onClick={() => onTrackOrder(order)}
                      className="px-4 py-2 rounded-xl bg-slate-900 hover:bg-[#fc8019] text-white font-bold text-xs flex items-center gap-1.5 transition-colors shadow-2xs"
                    >
                      <span>Track Order</span>
                      <ChevronRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

