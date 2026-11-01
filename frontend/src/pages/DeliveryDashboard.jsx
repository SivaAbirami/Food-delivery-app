import React, { useState, useEffect } from 'react';
import {
  Bike,
  Package,
  MapPin,
  Clock,
  CheckCircle2,
  RefreshCw,
  Navigation,
  ShieldCheck,
  Phone,
  Flame,
  Gamepad2,
  LogOut,
  ExternalLink,
} from 'lucide-react';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import VegNonVegIcon from '../components/VegNonVegIcon';
import SwiggyLiveMap from '../components/SwiggyLiveMap';
import DrDrivingCockpit from '../components/DrDrivingCockpit';

export default function DeliveryDashboard() {
  const { user, logout } = useAuth();
  const [availableOrders, setAvailableOrders] = useState([]);
  const [myOrders, setMyOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('available');
  const [actionLoading, setActionLoading] = useState(false);

  // Active driving trip telemetry
  const [riderPositions, setRiderPositions] = useState({});

  const getSavedPosition = (id) => {
    if (riderPositions[id]) return riderPositions[id];
    try {
      const saved = localStorage.getItem(`swiggy_live_telemetry_${id}`);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (parsed.lat && parsed.lng) return parsed;
      }
    } catch (e) {}
    return {
      lat: 12.9784,
      lng: 77.6408,
      heading: 140,
      speed: 0,
      progress: 0,
    };
  };

  const fetchDeliveryData = async () => {
    try {
      setLoading(true);
      const [availRes, myRes] = await Promise.all([
        api.getAvailableDeliveries(),
        api.getMyDeliveries(),
      ]);
      setAvailableOrders(availRes?.content || []);
      setMyOrders(myRes?.content || []);
    } catch (err) {
      console.warn('Failed to fetch delivery orders:', err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDeliveryData();
    const interval = setInterval(fetchDeliveryData, 3500);
    return () => clearInterval(interval);
  }, []);

  const handleClaimOrder = async (orderId) => {
    try {
      setActionLoading(true);
      await api.claimDelivery(orderId);
      await fetchDeliveryData();
      setActiveTab('my');
    } catch (err) {
      alert(err.message || 'Failed to claim order. It may have already been claimed by another driver.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleUpdateStatus = async (orderId, newStatus) => {
    try {
      setActionLoading(true);
      await api.updateDeliveryStatus(orderId, newStatus);
      await fetchDeliveryData();
    } catch (err) {
      alert(err.message || 'Failed to update delivery status');
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
            <div className="w-12 h-12 rounded-2xl bg-blue-600 text-white flex items-center justify-center shadow-md shadow-blue-500/20">
              <Bike className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-black text-slate-800 tracking-tight">
                Delivery Partner Fleet
              </h1>
              <p className="text-xs text-slate-500">
                Claim ready orders, drive GPS route, and complete deliveries with Customer OTP
              </p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
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
                href="/?portal=partner"
                target="_blank"
                rel="noopener noreferrer"
                className="px-2 py-1 rounded-lg hover:bg-amber-50 text-amber-800 font-bold flex items-center gap-1"
                title="Open Restaurant Partner in new Chrome tab"
              >
                <span>Partner Tab</span>
                <ExternalLink className="w-3 h-3" />
              </a>
            </div>

            <button
              onClick={fetchDeliveryData}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-2xl bg-white border border-slate-200 text-xs font-bold text-slate-600 hover:text-slate-800 shadow-2xs cursor-pointer"
            >
              <RefreshCw className="w-3.5 h-3.5 text-blue-600" />
              <span>Refresh Pool</span>
            </button>

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

        {/* Tabs */}
        <div className="flex items-center gap-2 mb-6 border-b border-slate-200 pb-3">
          <button
            onClick={() => setActiveTab('available')}
            className={`px-4 py-2 rounded-xl text-xs font-black transition-all cursor-pointer ${
              activeTab === 'available'
                ? 'bg-blue-600 text-white shadow-xs'
                : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
            }`}
          >
            Available Orders ({availableOrders.length})
          </button>
          <button
            onClick={() => setActiveTab('my')}
            className={`px-4 py-2 rounded-xl text-xs font-black transition-all cursor-pointer ${
              activeTab === 'my'
                ? 'bg-blue-600 text-white shadow-xs'
                : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
            }`}
          >
            My Active Trips ({myOrders.length})
          </button>
        </div>

        {/* AVAILABLE ORDERS TAB */}
        {activeTab === 'available' && (
          <div className="space-y-4">
            {availableOrders.length === 0 ? (
              <div className="bg-white rounded-3xl p-12 text-center border border-slate-100">
                <Package className="w-10 h-10 text-slate-300 mx-auto mb-2" />
                <h3 className="font-bold text-slate-700">No deliveries ready for pickup right now</h3>
                <p className="text-xs text-slate-400 mt-1">
                  Place an order from customer portal and accept it in restaurant kitchen hub to test dispatch!
                </p>
              </div>
            ) : (
              availableOrders.map((order) => (
                <div
                  key={order.id}
                  className="bg-white rounded-3xl p-5 sm:p-6 shadow-xs border border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-4"
                >
                  <div className="space-y-2 flex-1">
                    <div className="flex items-center gap-3">
                      <span className="font-mono text-xs font-bold text-slate-500">
                        #{order.orderNumber || order.id}
                      </span>
                      <span className="bg-emerald-100 text-emerald-800 text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full">
                        READY FOR PICKUP
                      </span>
                    </div>

                    <h4 className="font-black text-base text-slate-800">
                      {order.restaurantName || 'Restaurant'}
                    </h4>

                    <p className="text-xs text-slate-500 flex items-center gap-1.5">
                      <MapPin className="w-3.5 h-3.5 text-red-500 shrink-0" />
                      Deliver to: {order.deliveryAddress}
                    </p>
                  </div>

                  <button
                    onClick={() => handleClaimOrder(order.id)}
                    disabled={actionLoading}
                    className="px-6 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-black text-xs rounded-xl shadow-xs transition-colors cursor-pointer"
                  >
                    {actionLoading ? 'Claiming...' : 'CLAIM DELIVERY'}
                  </button>
                </div>
              ))
            )}
          </div>
        )}

        {/* MY ACTIVE TRIPS TAB */}
        {activeTab === 'my' && (
          <div className="space-y-6">
            {myOrders.length === 0 ? (
              <div className="bg-white rounded-3xl p-12 text-center border border-slate-100">
                <Navigation className="w-10 h-10 text-slate-300 mx-auto mb-2" />
                <h3 className="font-bold text-slate-700">No active trips assigned to you</h3>
                <p className="text-xs text-slate-400 mt-1">
                  Go to the Available tab and claim a ready order to start driving!
                </p>
              </div>
            ) : (
              myOrders.map((order) => {
                const isClaimed = order.status === 'READY_FOR_PICKUP';
                const isEnRoute = order.status === 'OUT_FOR_DELIVERY';
                const isDelivered = order.status === 'DELIVERED';
                const currentPos = getSavedPosition(order.id);

                return (
                  <div
                    key={order.id}
                    className="bg-white rounded-3xl p-6 shadow-xs border border-slate-100 space-y-4"
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-bold text-slate-400">
                            Trip #{order.orderNumber || order.id}
                          </span>
                          <span
                            className={`text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full ${
                              isClaimed
                                ? 'bg-amber-100 text-amber-800'
                                : isEnRoute
                                ? 'bg-blue-100 text-blue-800 animate-pulse'
                                : 'bg-emerald-100 text-emerald-800'
                            }`}
                          >
                            {order.status.replace(/_/g, ' ')}
                          </span>
                        </div>
                        <h4 className="font-black text-lg text-slate-900 mt-1">
                          {order.restaurantName || 'Restaurant Outlet'}
                        </h4>
                        <p className="text-xs text-slate-500 flex items-center gap-1.5 mt-0.5">
                          <MapPin className="w-3.5 h-3.5 text-red-500 shrink-0" />
                          <span>Delivery Address: {order.deliveryAddress}</span>
                        </p>
                      </div>

                      {/* Pickup Transition Button */}
                      {isClaimed && (
                        <button
                          onClick={() => handleUpdateStatus(order.id, 'OUT_FOR_DELIVERY')}
                          disabled={actionLoading}
                          className="px-6 py-3 bg-[#fc8019] hover:bg-[#e67210] text-white font-black text-xs rounded-2xl shadow-md shadow-orange-500/20 transition-all cursor-pointer"
                        >
                          🛵 Pick Up from Kitchen & Start Driving
                        </button>
                      )}

                      {isDelivered && (
                        <span className="text-xs font-black text-emerald-700 bg-emerald-50 px-4 py-2 rounded-xl border border-emerald-200 flex items-center gap-1.5">
                          <CheckCircle2 className="w-4 h-4" />
                          <span>Trip Completed Successfully</span>
                        </span>
                      )}
                    </div>

                    {/* Active Live Map & Dr. Driving Cockpit */}
                    {isEnRoute && (
                      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 pt-2">
                        {/* Interactive Leaflet Map */}
                        <div className="lg:col-span-7 h-[360px] rounded-2xl overflow-hidden border border-slate-200">
                          <SwiggyLiveMap
                            restaurantLocation={{
                              lat: 12.9784,
                              lng: 77.6408,
                              name: order.restaurantName || 'Kitchen',
                            }}
                            customerLocation={{
                              lat: 12.9716,
                              lng: 77.6412,
                              name: 'Customer Address',
                            }}
                            riderPosition={{
                              lat: currentPos.lat,
                              lng: currentPos.lng,
                              heading: currentPos.heading,
                              speed: currentPos.speed,
                            }}
                            progressPercent={currentPos.progress || 0}
                            isOutForDelivery={isEnRoute}
                            orderStatus={order.status}
                          />
                        </div>

                        {/* Dr. Driving Cockpit with OTP Input */}
                        <div className="lg:col-span-5">
                          <DrDrivingCockpit
                            orderId={order.id}
                            customerOtp={order.deliveryOtp || '4829'}
                            onPositionUpdate={(pos) => {
                              setRiderPositions((prev) => ({ ...prev, [order.id]: pos }));
                            }}
                            onOrderDelivered={fetchDeliveryData}
                            isOutForDelivery={true}
                          />
                        </div>
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        )}
      </div>
    </div>
  );
}
