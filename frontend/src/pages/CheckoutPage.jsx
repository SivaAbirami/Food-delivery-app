import React, { useState } from 'react';
import {
  MapPin,
  CreditCard,
  Banknote,
  ShieldCheck,
  ArrowLeft,
  CheckCircle2,
  Clock,
  Sparkles,
  ShoppingBag,
  Heart,
  Tag,
} from 'lucide-react';
import { api } from '../services/api';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { useLocation } from '../context/LocationContext';
import VegNonVegIcon from '../components/VegNonVegIcon';

export default function CheckoutPage({ initialInstructions, onBack, onOrderSuccess }) {
  const { cart, refreshCart, clearCart, subtotal, deliveryFee, platformFee, taxes, discount, grandTotal, appliedCoupon } = useCart();
  const { user } = useAuth();
  const { currentLocation } = useLocation();

  const [address, setAddress] = useState(
    currentLocation?.fullAddress || 'Flat 402, Sunshine Towers, 100ft Road, Indiranagar, Bengaluru, 560038'
  );
  const [instructions, setInstructions] = useState(initialInstructions || '');
  const [paymentMethod, setPaymentMethod] = useState('UPI'); // 'COD' | 'UPI'
  const [tip, setTip] = useState(20); // Delivery partner tip (₹20 default)
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const finalPayable = Math.max(0, grandTotal + tip);

  const handlePlaceOrder = async () => {
    if (!address.trim()) {
      alert('Please enter a delivery address');
      return;
    }

    setLoading(true);
    setError(null);

    const idempotencyKey = `idemp_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;

    try {
      const order = await api.checkout({
        deliveryAddress: address,
        specialInstructions: `${instructions ? instructions + ' | ' : ''}Tip: ₹${tip} | Payment: ${paymentMethod}`,
        idempotencyKey,
      });

      // Clear cart locally
      await clearCart();

      // Callback to show order tracking
      if (onOrderSuccess) {
        onOrderSuccess(order);
      }
    } catch (err) {
      setError(err.message || 'Failed to place order');
    } finally {
      setLoading(false);
    }
  };

  if (!cart || !cart.items || cart.items.length === 0) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-3xl p-8 max-w-md w-full text-center shadow-sm border border-slate-100">
          <ShoppingBag className="w-12 h-12 text-[#fc8019] mx-auto mb-3" />
          <h2 className="text-xl font-bold text-slate-800">Your cart is empty</h2>
          <p className="text-xs text-slate-500 mt-1 mb-6">Add dishes to proceed with checkout.</p>
          <button
            onClick={onBack}
            className="w-full py-3 bg-[#fc8019] text-white font-bold rounded-xl text-sm hover:bg-[#e67210] cursor-pointer"
          >
            Explore Restaurants
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-100/70 pb-20 pt-6">
      <div className="max-w-5xl mx-auto px-4">
        {/* Back Link */}
        <button
          onClick={onBack}
          className="inline-flex items-center gap-1.5 text-xs font-bold text-slate-600 hover:text-[#fc8019] mb-4 transition-colors cursor-pointer"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          Back to Cart & Menu
        </button>

        <h1 className="text-2xl font-black text-slate-800 mb-6 tracking-tight">Checkout</h1>

        {error && (
          <div className="mb-6 p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-sm font-semibold">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Left Column: Address & Payment */}
          <div className="lg:col-span-7 space-y-6">
            {/* Delivery Address Card */}
            <div className="bg-white p-6 rounded-3xl shadow-xs border border-slate-100">
              <div className="flex items-center gap-2 mb-4">
                <div className="w-8 h-8 rounded-full bg-orange-100 flex items-center justify-center text-[#fc8019]">
                  <MapPin className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-bold text-base text-slate-800">Delivery Address</h3>
                  <p className="text-xs text-slate-400">Selected via {currentLocation?.name || 'Indiranagar'}</p>
                </div>
              </div>

              <textarea
                rows={3}
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                placeholder="Enter complete delivery address..."
                className="w-full p-3 rounded-2xl border border-slate-200 text-xs sm:text-sm text-slate-700 focus:outline-none focus:border-[#fc8019] focus:ring-1 focus:ring-[#fc8019]"
              />

              <div className="mt-4">
                <label className="block text-xs font-bold text-slate-600 mb-1">
                  Delivery Instructions (Optional)
                </label>
                <input
                  type="text"
                  value={instructions}
                  onChange={(e) => setInstructions(e.target.value)}
                  placeholder="e.g. Ring the bell twice, leave with guard"
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs text-slate-700 focus:outline-none focus:border-[#fc8019]"
                />
              </div>
            </div>

            {/* Delivery Partner Tip Card (Authentic Swiggy Feature) */}
            <div className="bg-white p-6 rounded-3xl shadow-xs border border-slate-100">
              <div className="flex items-center gap-2 mb-3">
                <div className="w-8 h-8 rounded-full bg-pink-100 flex items-center justify-center text-pink-600">
                  <Heart className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-slate-800">Tip your delivery partner</h3>
                  <p className="text-[11px] text-slate-400">
                    100% of your tip goes directly to your rider
                  </p>
                </div>
              </div>

              <div className="flex gap-2">
                {[0, 20, 30, 50].map((amt) => (
                  <button
                    key={amt}
                    type="button"
                    onClick={() => setTip(amt)}
                    className={`flex-1 py-2 rounded-xl text-xs font-black transition-all cursor-pointer border ${
                      tip === amt
                        ? 'bg-[#fc8019] text-white border-[#fc8019] shadow-xs'
                        : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                    }`}
                  >
                    {amt === 0 ? 'No Tip' : `₹${amt}`}
                  </button>
                ))}
              </div>
            </div>

            {/* Payment Method Card */}
            <div className="bg-white p-6 rounded-3xl shadow-xs border border-slate-100">
              <div className="flex items-center gap-2 mb-4">
                <div className="w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600">
                  <CreditCard className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-bold text-base text-slate-800">Payment Option</h3>
                  <p className="text-xs text-slate-400">Select payment method</p>
                </div>
              </div>

              <div className="space-y-3">
                <label
                  onClick={() => setPaymentMethod('UPI')}
                  className={`flex items-center justify-between p-3.5 rounded-2xl border cursor-pointer transition-all ${
                    paymentMethod === 'UPI'
                      ? 'border-[#fc8019] bg-orange-50/40'
                      : 'border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <CreditCard className="w-5 h-5 text-blue-600" />
                    <div>
                      <span className="font-bold text-sm text-slate-800 block">UPI / Google Pay / PhonePe</span>
                      <span className="text-xs text-slate-500">Fast 1-click digital checkout</span>
                    </div>
                  </div>
                  <input
                    type="radio"
                    name="payment"
                    checked={paymentMethod === 'UPI'}
                    onChange={() => setPaymentMethod('UPI')}
                    className="accent-[#fc8019]"
                  />
                </label>

                <label
                  onClick={() => setPaymentMethod('COD')}
                  className={`flex items-center justify-between p-3.5 rounded-2xl border cursor-pointer transition-all ${
                    paymentMethod === 'COD'
                      ? 'border-[#fc8019] bg-orange-50/40'
                      : 'border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <Banknote className="w-5 h-5 text-emerald-600" />
                    <div>
                      <span className="font-bold text-sm text-slate-800 block">Cash on Delivery</span>
                      <span className="text-xs text-slate-500">Pay cash or UPI upon delivery</span>
                    </div>
                  </div>
                  <input
                    type="radio"
                    name="payment"
                    checked={paymentMethod === 'COD'}
                    onChange={() => setPaymentMethod('COD')}
                    className="accent-[#fc8019]"
                  />
                </label>
              </div>
            </div>
          </div>

          {/* Right Column: Order Summary & Pay */}
          <div className="lg:col-span-5 space-y-6">
            <div className="bg-white p-6 rounded-3xl shadow-xs border border-slate-100">
              <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-100">
                <div>
                  <h3 className="font-black text-slate-800 text-base">{cart.restaurantName}</h3>
                  <span className="text-xs text-slate-400">Order Summary</span>
                </div>
              </div>

              {/* Items List */}
              <div className="divide-y divide-slate-100 max-h-48 overflow-y-auto mb-4">
                {cart.items.map((item) => (
                  <div key={item.id} className="py-2.5 flex items-center justify-between text-xs">
                    <div className="flex items-center gap-2">
                      <VegNonVegIcon isVeg={true} size="sm" />
                      <span className="font-bold text-slate-800">{item.menuItemName}</span>
                      <span className="text-slate-400">× {item.quantity}</span>
                    </div>
                    <span className="font-bold text-slate-700">₹{parseFloat(item.subtotal || 0).toFixed(2)}</span>
                  </div>
                ))}
              </div>

              {/* Applied Coupon Pill */}
              {appliedCoupon && (
                <div className="mb-4 p-2.5 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center justify-between text-xs">
                  <div className="flex items-center gap-1.5 text-emerald-700 font-bold">
                    <Tag className="w-3.5 h-3.5" />
                    <span>'{appliedCoupon.code}' Applied!</span>
                  </div>
                  <span className="font-bold text-emerald-700">-₹{discount.toFixed(2)}</span>
                </div>
              )}

              {/* Bill Details */}
              <div className="space-y-2 text-xs text-slate-600 border-t border-slate-100 pt-3">
                <div className="flex justify-between">
                  <span>Item Subtotal</span>
                  <span className="font-bold text-slate-800">₹{subtotal.toFixed(2)}</span>
                </div>

                {discount > 0 && (
                  <div className="flex justify-between text-emerald-600 font-bold">
                    <span>Coupon Discount</span>
                    <span>-₹{discount.toFixed(2)}</span>
                  </div>
                )}

                <div className="flex justify-between">
                  <span>Delivery Fee</span>
                  <span className="font-bold text-slate-800">
                    {deliveryFee === 0 ? <span className="text-emerald-600">FREE</span> : `₹${deliveryFee.toFixed(2)}`}
                  </span>
                </div>

                <div className="flex justify-between">
                  <span>Platform Fee</span>
                  <span className="font-bold text-slate-800">₹{platformFee.toFixed(2)}</span>
                </div>

                <div className="flex justify-between">
                  <span>GST & Restaurant Taxes (5%)</span>
                  <span className="font-bold text-slate-800">₹{taxes.toFixed(2)}</span>
                </div>

                {tip > 0 && (
                  <div className="flex justify-between text-pink-600 font-bold">
                    <span>Delivery Partner Tip</span>
                    <span>₹{tip.toFixed(2)}</span>
                  </div>
                )}

                <div className="border-t border-slate-100 pt-3 mt-3 flex justify-between font-black text-base text-slate-900">
                  <span>TO PAY</span>
                  <span className="text-[#fc8019]">₹{finalPayable.toFixed(2)}</span>
                </div>
              </div>

              {/* Pay Button */}
              <button
                onClick={handlePlaceOrder}
                disabled={loading}
                className="w-full mt-6 py-3.5 px-4 rounded-2xl bg-[#fc8019] hover:bg-[#e67210] disabled:opacity-50 text-white font-black text-sm flex items-center justify-center gap-2 shadow-lg shadow-orange-500/25 transition-all cursor-pointer active:scale-98"
              >
                <span>{loading ? 'Processing Order...' : `PAY ₹${finalPayable.toFixed(2)}`}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
