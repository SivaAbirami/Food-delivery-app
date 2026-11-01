import React, { useState, useEffect } from 'react';
import {
  CheckCircle2,
  Clock,
  ChefHat,
  Bike,
  PackageCheck,
  MapPin,
  Phone,
  ArrowLeft,
  Sparkles,
  ShoppingBag,
  RefreshCw,
  AlertCircle,
  KeyRound,
  Gamepad2,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { api } from '../services/api';
import VegNonVegIcon from '../components/VegNonVegIcon';
import SwiggyLiveMap from '../components/SwiggyLiveMap';
import DrDrivingCockpit from '../components/DrDrivingCockpit';

const ORDER_STEPS = [
  { status: 'PLACED', title: 'Order Placed', desc: 'Sent to kitchen', icon: Clock },
  { status: 'ACCEPTED', title: 'Confirmed', desc: 'Kitchen accepted', icon: CheckCircle2 },
  { status: 'PREPARING', title: 'Cooking', desc: 'Chef preparing food', icon: ChefHat },
  { status: 'READY_FOR_PICKUP', title: 'Food Ready', desc: 'Awaiting rider', icon: PackageCheck },
  { status: 'OUT_FOR_DELIVERY', title: 'On the Way!', desc: 'Rider is driving', icon: Bike },
  { status: 'DELIVERED', title: 'Delivered', desc: 'Enjoy your meal!', icon: Sparkles },
];

export default function OrderTrackingPage({ orderId, initialOrder, onBackToHome }) {
  const [order, setOrder] = useState(initialOrder || null);
  const [loading, setLoading] = useState(!initialOrder);
  const [refreshing, setRefreshing] = useState(false);
  const [showSimulator, setShowSimulator] = useState(false);

  const targetOrderId = orderId || initialOrder?.id || order?.id;

  // Initial telemetry read from persistent storage or default
  const getInitialLocation = () => {
    if (targetOrderId) {
      try {
        const saved = localStorage.getItem(`swiggy_live_telemetry_${targetOrderId}`);
        if (saved) {
          const parsed = JSON.parse(saved);
          return {
            lat: parsed.lat || 12.9784,
            lng: parsed.lng || 77.6408,
            heading: parsed.heading || 140,
            speed: parsed.speed || 0,
            progress: parsed.progress || 0,
          };
        }
      } catch (e) {}
    }
    return {
      lat: 12.9784,
      lng: 77.6408,
      heading: 140,
      speed: 0,
      progress: 0,
    };
  };

  // Live telemetry state for map
  const [liveLocation, setLiveLocation] = useState(getInitialLocation);

  // Instant 0ms cross-tab listener for rider driving in another tab
  useEffect(() => {
    const id = targetOrderId;
    if (!id) return;

    const handleStorage = (e) => {
      if (e.key === `swiggy_live_telemetry_${id}` && e.newValue) {
        try {
          const telem = JSON.parse(e.newValue);
          setLiveLocation({
            lat: telem.lat,
            lng: telem.lng,
            heading: telem.heading || 180,
            speed: telem.speed || 0,
            progress: telem.progress || 0,
          });
        } catch (err) {}
      }
    };

    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, [targetOrderId]);

  // Reflect tracking order in URL so whole-page refresh (F5) never jumps back to home page
  useEffect(() => {
    if (targetOrderId) {
      window.history.replaceState(null, '', `/?orderId=${targetOrderId}`);
    }
  }, [targetOrderId]);

  // Fast 1000ms polling for live GPS telemetry (runs continuously when out for delivery)
  useEffect(() => {
    const id = targetOrderId;
    if (!id) return;

    let isMounted = true;
    async function pollTelemetry() {
      try {
        const telemetry = await api.getDeliveryLocation(id);
        if (isMounted && telemetry && telemetry.latitude) {
          const remaining = telemetry.distanceRemainingKm != null ? telemetry.distanceRemainingKm : 1.2;
          const calcProgress = Math.max(0, Math.min(100, Math.round((1 - (remaining / 2.2)) * 100)));

          setLiveLocation({
            lat: telemetry.latitude,
            lng: telemetry.longitude,
            heading: telemetry.heading || 180,
            speed: telemetry.speed != null ? telemetry.speed : 0,
            progress: calcProgress,
          });
        }
      } catch (err) {
        // Silently tolerate
      }
    }

    pollTelemetry();
    const interval = setInterval(pollTelemetry, 1000);
    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, [targetOrderId]);

  // Continuous 2-second Auto-Polling for order progression (PLACED -> PREPARING -> READY_FOR_PICKUP -> OUT_FOR_DELIVERY -> DELIVERED)
  useEffect(() => {
    const id = targetOrderId;
    if (!id) return;

    let isMounted = true;
    async function pollOrderStatus() {
      try {
        const data = await api.getOrderById(id);
        if (isMounted && data) {
          setOrder(data);
          setLoading(false);
          setRefreshing(false);
        }
      } catch (err) {
        console.warn('Auto-polling order status error:', err.message);
      }
    }

    pollOrderStatus();
    const interval = setInterval(pollOrderStatus, 2000);
    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, [targetOrderId]);

  const currentStepIndex = ORDER_STEPS.findIndex((s) => s.status === order?.status);
  const isDelivered = order?.status === 'DELIVERED';
  const isCancelled = order?.status === 'CANCELLED';
  const isOutForDelivery = order?.status === 'OUT_FOR_DELIVERY';

  return (
    <div className="min-h-screen bg-slate-50/70 pb-20 pt-6">
      <div className="max-w-4xl mx-auto px-4">
        {/* Navigation & Live Auto-Poll Indicator */}
        <div className="flex items-center justify-between mb-6">
          <button
            onClick={() => {
              window.history.replaceState(null, '', window.location.pathname);
              onBackToHome();
            }}
            className="inline-flex items-center gap-1.5 text-xs font-bold text-slate-600 hover:text-[#fc8019] transition-colors cursor-pointer"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            Back to Restaurants
          </button>

          {/* 2-Second Live Auto-Sync Status Badge (replaces manual refresh button) */}
          <div className="inline-flex items-center gap-2 text-xs font-bold text-emerald-700 bg-emerald-50 px-3.5 py-1.5 rounded-full border border-emerald-200 shadow-2xs">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
            <span>Live Auto-Sync (2s)</span>
          </div>
        </div>

        {/* Live Tracking Header Banner */}
        <div className="bg-gradient-to-r from-slate-900 to-slate-800 rounded-3xl p-6 sm:p-8 text-white shadow-xl relative overflow-hidden mb-6">
          <div className="relative z-10">
            <div className="flex flex-wrap items-center justify-between gap-3 mb-2">
              <span className="text-xs uppercase tracking-widest font-extrabold text-[#fc8019] bg-orange-950/60 border border-orange-500/30 px-3 py-1 rounded-full">
                LIVE ORDER TRACKING
              </span>
              <span className="text-xs text-slate-400 font-mono">
                Order #{order?.orderNumber || order?.id}
              </span>
            </div>

            <h1 className="text-2xl sm:text-3xl font-black mt-2 tracking-tight">
              {isCancelled
                ? 'Order Cancelled'
                : isDelivered
                ? 'Order Delivered!'
                : isOutForDelivery
                ? 'Rider is driving to your location!'
                : 'Your food is being prepared!'}
            </h1>
            <p className="text-xs sm:text-sm text-slate-300 mt-1 max-w-lg">
              {isDelivered
                ? 'Enjoy your delicious hot meal! Thank you for choosing Swiggy.'
                : `Delivering from ${order?.restaurantName || 'Restaurant'} to ${order?.deliveryAddress || 'Indiranagar, Bengaluru'}`}
            </p>
          </div>

          <div className="absolute right-4 bottom-2 text-white/5 pointer-events-none select-none">
            <Bike className="w-48 h-48 stroke-1" />
          </div>
        </div>

        {/* Swiggy 4-Digit Delivery OTP Card */}
        {!isCancelled && !isDelivered && (
          <div className="bg-gradient-to-r from-orange-50 to-amber-50 border border-orange-200 rounded-3xl p-4 sm:p-5 mb-6 flex flex-wrap items-center justify-between gap-4 shadow-xs">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-[#fc8019] text-white flex items-center justify-center font-black shadow-md shadow-orange-500/20">
                <KeyRound className="w-6 h-6" />
              </div>
              <div>
                <span className="text-[11px] font-black uppercase tracking-wider text-orange-900 block">
                  Swiggy Delivery OTP
                </span>
                <span className="text-xs text-slate-600">
                  Share this 4-digit code with your delivery partner upon arrival
                </span>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <div className="text-2xl sm:text-3xl font-black font-mono tracking-widest bg-white border-2 border-orange-400 px-5 py-2 rounded-2xl text-[#fc8019] shadow-sm">
                {order?.deliveryOtp || '4829'}
              </div>
            </div>
          </div>
        )}

        {/* Live GPS Swiggy Map View */}
        <div className="bg-white rounded-3xl p-5 shadow-xs border border-slate-100 mb-6">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-xl bg-orange-100 text-[#fc8019] flex items-center justify-center font-bold">
                🗺️
              </div>
              <div>
                <h3 className="font-black text-sm text-slate-900">Live Delivery Route Map</h3>
                <p className="text-[11px] text-slate-400">Bengaluru Street GPS Tracking & Radar</p>
              </div>
            </div>

            {/* Live GPS Telemetry Indicator */}
            <div className="flex items-center gap-2 bg-emerald-50 text-emerald-800 border border-emerald-200 px-3.5 py-1.5 rounded-full text-xs font-bold shadow-2xs">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
              <span>Live Rider Telemetry</span>
            </div>
          </div>

          {/* Leaflet Map Canvas */}
          <div className="h-[420px] w-full rounded-2xl overflow-hidden">
            <SwiggyLiveMap
              restaurantLocation={{
                lat: 12.9784,
                lng: 77.6408,
                name: order?.restaurantName || 'Kitchen',
              }}
              customerLocation={{
                lat: 12.9716,
                lng: 77.6412,
                name: 'Delivery Address',
              }}
              riderPosition={{
                lat: liveLocation.lat,
                lng: liveLocation.lng,
                heading: liveLocation.heading,
                speed: liveLocation.speed,
              }}
              progressPercent={liveLocation.progress || (isDelivered ? 100 : 0)}
              isOutForDelivery={isOutForDelivery}
              orderStatus={order?.status}
            />
          </div>
        </div>

        {/* Status Tracker Stepper */}
        {!isCancelled ? (
          <div className="bg-white rounded-3xl p-6 shadow-xs border border-slate-100 mb-6">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-6">
              Live Order Progression
            </h3>

            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-3">
              {ORDER_STEPS.map((step, idx) => {
                const Icon = step.icon;
                const isCompleted = currentStepIndex >= idx;
                const isCurrent = currentStepIndex === idx;

                return (
                  <div
                    key={step.status}
                    className={`flex flex-col items-center text-center p-3 rounded-2xl border transition-all ${
                      isCurrent
                        ? 'border-[#fc8019] bg-orange-50/50 shadow-xs scale-105'
                        : isCompleted
                        ? 'border-emerald-200 bg-emerald-50/30'
                        : 'border-slate-100 bg-slate-50/50 opacity-40'
                    }`}
                  >
                    <div
                      className={`w-10 h-10 rounded-full flex items-center justify-center mb-2 shadow-2xs ${
                        isCurrent
                          ? 'bg-[#fc8019] text-white animate-pulse'
                          : isCompleted
                          ? 'bg-emerald-600 text-white'
                          : 'bg-slate-200 text-slate-400'
                      }`}
                    >
                      <Icon className="w-5 h-5" />
                    </div>
                    <span className="font-bold text-xs text-slate-800 leading-tight">
                      {step.title}
                    </span>
                    <span className="text-[10px] text-slate-400 mt-0.5 leading-tight">
                      {step.desc}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        ) : (
          <div className="bg-red-50 p-6 rounded-3xl border border-red-200 text-red-700 flex items-center gap-3 mb-6">
            <AlertCircle className="w-8 h-8 text-red-600 shrink-0" />
            <div>
              <h4 className="font-bold text-sm">Order Cancelled</h4>
              <p className="text-xs text-red-600 mt-0.5">
                This order was cancelled. Any amount deducted will be refunded to original source.
              </p>
            </div>
          </div>
        )}

        {/* Order Details & Bill Breakdown */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
          <div className="md:col-span-7 space-y-4">
            <div className="bg-white rounded-3xl p-6 shadow-xs border border-slate-100">
              <h3 className="font-bold text-base text-slate-800 mb-3 flex items-center justify-between">
                <span>Items in this Order</span>
                <span className="text-xs text-slate-400 font-normal">
                  {order?.restaurantName || 'Restaurant'}
                </span>
              </h3>

              <div className="divide-y divide-slate-100">
                {order?.items?.map((item) => (
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
            </div>
          </div>

          <div className="md:col-span-5 space-y-4">
            <div className="bg-white rounded-3xl p-6 shadow-xs border border-slate-100">
              <h3 className="font-bold text-base text-slate-800 mb-4">Bill Summary</h3>

              <div className="space-y-2 text-xs text-slate-600">
                <div className="flex justify-between">
                  <span>Item Subtotal</span>
                  <span className="font-bold text-slate-800">₹{parseFloat(order?.subtotalAmount || 0).toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span>GST & Restaurant Taxes (5%)</span>
                  <span className="font-bold text-slate-800">₹{parseFloat(order?.taxAmount || 0).toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Delivery Partner Fee</span>
                  <span className="font-bold text-slate-800">₹{parseFloat(order?.deliveryFee || 0).toFixed(2)}</span>
                </div>

                <div className="border-t border-slate-100 pt-3 mt-3 flex justify-between font-black text-sm text-slate-900">
                  <span>Total Paid</span>
                  <span className="text-[#fc8019]">₹{parseFloat(order?.totalAmount || 0).toFixed(2)}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
