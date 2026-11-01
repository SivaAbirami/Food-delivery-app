import React, { useState } from 'react';
import { X, ShoppingBag, Plus, Minus, Trash2, Tag, Check, ArrowRight, ShieldCheck, MapPin, Loader2 } from 'lucide-react';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';
import VegNonVegIcon from './VegNonVegIcon';

const COUPONS = [
  { code: 'SWIGGY50', desc: '50% OFF up to ₹100', discount: '50%' },
  { code: 'FREEDEL', desc: 'FREE Delivery on all orders', discount: 'Free Delivery' },
  { code: 'FEAST150', desc: 'Flat ₹150 OFF on orders above ₹300', discount: '₹150 OFF' },
];

export default function CartDrawer({ onProceedToCheckout }) {
  const {
    cart,
    cartDrawerOpen,
    closeCart,
    updateQuantity,
    clearCart,
    totalItemCount,
    subtotal,
    deliveryFee,
    platformFee,
    taxes,
    discount,
    grandTotal,
    appliedCoupon,
    applyCoupon,
    removeCoupon,
  } = useCart();
  const { user } = useAuth();

  const [couponInput, setCouponInput] = useState('');
  const [couponLoading, setCouponLoading] = useState(false);
  const [couponMessage, setCouponMessage] = useState(null);
  const [instructions, setInstructions] = useState('');

  if (!cartDrawerOpen) return null;

  const handleApplyCustomCoupon = async () => {
    if (!couponInput.trim()) return;
    setCouponLoading(true);
    setCouponMessage(null);
    try {
      const res = await api.validateCoupon(couponInput.trim(), cart?.restaurantId, subtotal);
      if (res.isValid) {
        applyCoupon({
          code: res.code,
          discountAmount: res.discountAmount,
          message: res.message,
        });
        setCouponMessage({ type: 'success', text: res.message });
        setCouponInput('');
      } else {
        setCouponMessage({ type: 'error', text: res.message || 'Invalid coupon' });
      }
    } catch (err) {
      setCouponMessage({ type: 'error', text: err.message || 'Validation failed' });
    } finally {
      setCouponLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-xs transition-opacity"
        onClick={closeCart}
      />

      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md bg-white shadow-2xl flex flex-col animate-in slide-in-from-right duration-300">
          {/* Header */}
          <div className="p-4 border-b border-slate-100 flex items-center justify-between bg-white sticky top-0 z-10">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-orange-100 flex items-center justify-center text-[#fc8019]">
                <ShoppingBag className="w-4 h-4" />
              </div>
              <div>
                <h2 className="font-bold text-slate-800 text-base">Your Cart</h2>
                <p className="text-xs text-slate-400">
                  {cart?.restaurantName ? `From ${cart.restaurantName}` : 'No restaurant selected'}
                </p>
              </div>
            </div>
            <button
              onClick={closeCart}
              className="w-8 h-8 rounded-full hover:bg-slate-100 flex items-center justify-center text-slate-500 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Cart Contents */}
          {!cart || !cart.items || cart.items.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center p-8 text-center">
              <div className="w-24 h-24 rounded-full bg-orange-50 flex items-center justify-center text-[#fc8019] mb-4">
                <ShoppingBag className="w-12 h-12 stroke-[1.5]" />
              </div>
              <h3 className="font-bold text-lg text-slate-800 mb-1">Your cart is empty</h3>
              <p className="text-xs text-slate-500 max-w-xs mb-6">
                Good food is always just a few clicks away! Explore our top restaurants and add delicious meals.
              </p>
              <button
                onClick={closeCart}
                className="px-6 py-2.5 rounded-xl bg-[#fc8019] text-white font-bold text-sm hover:bg-[#e67210] transition-colors shadow-sm"
              >
                SEE RESTAURANTS NEAR YOU
              </button>
            </div>
          ) : (
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {/* Delivery Address Pill */}
              <div className="p-3 bg-slate-50 rounded-xl flex items-center gap-2.5 border border-slate-100 text-xs">
                <MapPin className="w-4 h-4 text-[#fc8019] shrink-0" />
                <div className="flex-1 truncate">
                  <span className="font-bold text-slate-800">Delivering to Home: </span>
                  <span className="text-slate-600 truncate">Indiranagar, Bengaluru, 560038</span>
                </div>
              </div>

              {/* Items List */}
              <div className="bg-white rounded-2xl border border-slate-100 divide-y divide-slate-100 shadow-2xs">
                {cart.items.map((item) => (
                  <div key={item.id} className="p-3.5 flex items-center justify-between gap-3">
                    <div className="flex items-start gap-2 flex-1 min-w-0">
                      <div className="mt-1">
                        <VegNonVegIcon isVeg={!item.menuItemName?.toLowerCase().includes('chicken') && !item.menuItemName?.toLowerCase().includes('meat')} />
                      </div>
                      <div className="min-w-0 flex-1">
                        <h4 className="font-bold text-sm text-slate-800 truncate">{item.menuItemName}</h4>
                        <p className="text-xs text-slate-500">₹{item.unitPrice}</p>
                      </div>
                    </div>

                    {/* Quantity Stepper */}
                    <div className="flex items-center border border-emerald-600 rounded-lg bg-white overflow-hidden shadow-2xs">
                      <button
                        onClick={() => updateQuantity(item.id, item.quantity - 1)}
                        className="w-7 h-7 flex items-center justify-center text-emerald-600 hover:bg-emerald-50 transition-colors"
                      >
                        {item.quantity === 1 ? <Trash2 className="w-3.5 h-3.5 text-red-500" /> : <Minus className="w-3.5 h-3.5" />}
                      </button>
                      <span className="w-6 text-center text-xs font-bold text-emerald-600">
                        {item.quantity}
                      </span>
                      <button
                        onClick={() => updateQuantity(item.id, item.quantity + 1)}
                        className="w-7 h-7 flex items-center justify-center text-emerald-600 hover:bg-emerald-50 transition-colors"
                      >
                        <Plus className="w-3.5 h-3.5" />
                      </button>
                    </div>

                    <div className="text-right min-w-[50px]">
                      <span className="font-bold text-sm text-slate-800">₹{item.subtotal}</span>
                    </div>
                  </div>
                ))}
              </div>

              {/* Special Instructions */}
              <div>
                <input
                  type="text"
                  placeholder="Any restaurant instructions? (e.g. less spicy, no onion)"
                  value={instructions}
                  onChange={(e) => setInstructions(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs text-slate-700 focus:outline-none focus:border-[#fc8019]"
                />
              </div>

              {/* Coupon Box */}
              <div className="p-3.5 bg-slate-50 rounded-2xl border border-slate-100">
                <div className="flex items-center gap-2 mb-2 text-xs font-bold text-slate-700">
                  <Tag className="w-4 h-4 text-[#fc8019]" />
                  <span>Coupons & Offers</span>
                </div>

                {appliedCoupon ? (
                  <div className="flex items-center justify-between bg-emerald-50 border border-emerald-200 p-2.5 rounded-xl">
                    <div className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-emerald-600" />
                      <div>
                        <span className="font-bold text-xs text-emerald-800">{appliedCoupon.code}</span>
                        <p className="text-[11px] text-emerald-600">{appliedCoupon.desc}</p>
                      </div>
                    </div>
                    <button
                      onClick={removeCoupon}
                      className="text-xs font-bold text-red-600 hover:underline px-2 py-1"
                    >
                      REMOVE
                    </button>
                  </div>
                ) : (
                  <div className="space-y-2">
                    <div className="flex gap-2">
                      <input
                        type="text"
                        placeholder="Enter coupon code"
                        value={couponInput}
                        onChange={(e) => setCouponInput(e.target.value.toUpperCase())}
                        className="flex-1 px-3 py-1.5 rounded-lg border border-slate-200 text-xs uppercase font-semibold focus:outline-none focus:border-[#fc8019]"
                      />
                      <button
                        onClick={handleApplyCustomCoupon}
                        className="px-3 py-1.5 rounded-lg bg-slate-800 text-white font-bold text-xs hover:bg-slate-700"
                      >
                        APPLY
                      </button>
                    </div>

                    <div className="flex flex-wrap gap-1.5 pt-1">
                      {COUPONS.map((c) => (
                        <button
                          key={c.code}
                          onClick={() => applyCoupon(c)}
                          className="text-[11px] font-bold px-2 py-1 bg-white border border-dashed border-orange-300 text-[#fc8019] rounded-md hover:bg-orange-50 transition-colors"
                        >
                          {c.code} ({c.discount})
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Bill Details */}
              <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100 text-xs space-y-2 text-slate-600">
                <h4 className="font-bold text-slate-800 text-xs uppercase tracking-wider mb-2">Bill Details</h4>
                <div className="flex justify-between">
                  <span>Item Total</span>
                  <span className="font-medium text-slate-800">₹{subtotal.toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Delivery Partner Fee</span>
                  <span className="font-medium text-slate-800">
                    {deliveryFee === 0 ? <span className="text-emerald-600 font-bold">FREE</span> : `₹${deliveryFee}`}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span>Platform Fee</span>
                  <span className="font-medium text-slate-800">₹{platformFee}</span>
                </div>
                <div className="flex justify-between">
                  <span>GST and Restaurant Charges</span>
                  <span className="font-medium text-slate-800">₹{taxes}</span>
                </div>
                {discount > 0 && (
                  <div className="flex justify-between text-emerald-600 font-bold">
                    <span>Discount Coupon ({appliedCoupon?.code})</span>
                    <span>-₹{discount}</span>
                  </div>
                )}
                <div className="border-t border-slate-200 pt-2 flex justify-between font-bold text-sm text-slate-900">
                  <span>TO PAY</span>
                  <span className="text-[#fc8019] text-base">₹{grandTotal.toFixed(2)}</span>
                </div>
              </div>

              {/* Invariant guarantee pill */}
              <div className="flex items-center justify-center gap-1.5 text-[11px] text-slate-400 py-1">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                <span>Live DB Price Guarantee & Idempotent Checkout</span>
              </div>
            </div>
          )}

          {/* Footer Checkout CTA */}
          {cart && cart.items && cart.items.length > 0 && (
            <div className="p-4 border-t border-slate-100 bg-white sticky bottom-0">
              <button
                onClick={() => {
                  closeCart();
                  if (onProceedToCheckout) onProceedToCheckout(instructions);
                }}
                className="w-full py-3.5 px-4 rounded-xl bg-[#fc8019] hover:bg-[#e67210] text-white font-black text-sm flex items-center justify-between shadow-lg shadow-orange-500/20 transition-all hover:scale-[1.01]"
              >
                <div className="text-left">
                  <span className="text-[11px] text-orange-100 block font-normal leading-tight">
                    {totalItemCount} {totalItemCount === 1 ? 'ITEM' : 'ITEMS'}
                  </span>
                  <span className="text-base font-black">₹{grandTotal.toFixed(2)}</span>
                </div>
                <div className="flex items-center gap-1 uppercase tracking-wider font-bold">
                  <span>Proceed to Pay</span>
                  <ArrowRight className="w-4 h-4" />
                </div>
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

