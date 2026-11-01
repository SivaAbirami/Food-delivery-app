import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { LocationProvider } from './context/LocationContext';
import Navbar from './components/Navbar';
import AuthModal from './components/AuthModal';
import CartConflictModal from './components/CartConflictModal';
import CartDrawer from './components/CartDrawer';
import SearchModal from './components/SearchModal';
import LocationDrawer from './components/LocationDrawer';

import HomePage from './pages/HomePage';
import RestaurantDetailPage from './pages/RestaurantDetailPage';
import CheckoutPage from './pages/CheckoutPage';
import OrderTrackingPage from './pages/OrderTrackingPage';
import OrdersHistoryPage from './pages/OrdersHistoryPage';
import OwnerDashboard from './pages/OwnerDashboard';
import DeliveryDashboard from './pages/DeliveryDashboard';
import PortalGatewayPage from './pages/PortalGatewayPage';

function AppContent() {
  const { user, loading: authLoading } = useAuth();

  // Read URL parameters on startup to restore view and orderId on refresh
  const getInitialView = () => {
    const params = new URLSearchParams(window.location.search);
    if (params.get('orderId') || params.get('track')) return 'tracking';
    if (params.get('restaurantId')) return 'restaurant';
    if (params.get('view') === 'orders') return 'orders';
    if (params.get('view') === 'checkout') return 'checkout';
    return 'home';
  };

  const getInitialTrackOrder = () => {
    const params = new URLSearchParams(window.location.search);
    const id = params.get('orderId') || params.get('track');
    return id ? { id: parseInt(id) } : null;
  };

  const [currentView, setCurrentView] = useState(getInitialView);
  const [selectedRestaurant, setSelectedRestaurant] = useState(null);
  const [checkoutInstructions, setCheckoutInstructions] = useState('');
  const [activeTrackOrder, setActiveTrackOrder] = useState(getInitialTrackOrder);
  const [searchModalOpen, setSearchModalOpen] = useState(false);

  // Sync browser URL whenever view or tracking order changes (so F5 never resets to home)
  useEffect(() => {
    if (user?.role !== 'CUSTOMER') return;

    if (currentView === 'tracking' && activeTrackOrder?.id) {
      window.history.replaceState(null, '', `/?orderId=${activeTrackOrder.id}`);
    } else if (currentView === 'restaurant' && selectedRestaurant?.id) {
      window.history.replaceState(null, '', `/?restaurantId=${selectedRestaurant.id}`);
    } else if (currentView === 'orders') {
      window.history.replaceState(null, '', `/?view=orders`);
    } else if (currentView === 'checkout') {
      window.history.replaceState(null, '', `/?view=checkout`);
    } else if (currentView === 'home') {
      window.history.replaceState(null, '', window.location.pathname);
    }
  }, [currentView, activeTrackOrder?.id, selectedRestaurant?.id, user?.role]);

  // Loading Screen
  if (authLoading) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center text-white">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-orange-500 border-t-transparent rounded-full animate-spin" />
          <span className="text-xs font-bold text-slate-400">Loading Swiggy Session...</span>
        </div>
      </div>
    );
  }

  // 1. If not logged in in this tab: show Portal Gateway to pick persona
  if (!user) {
    return <PortalGatewayPage onLoginSuccess={() => setCurrentView('home')} />;
  }

  // 2. If logged in as RESTAURANT_OWNER: Page changes FULLY to Partner Hub
  if (user.role === 'RESTAURANT_OWNER') {
    return <OwnerDashboard />;
  }

  // 3. If logged in as DELIVERY_PARTNER: Page changes FULLY to Delivery Fleet
  if (user.role === 'DELIVERY_PARTNER') {
    return <DeliveryDashboard />;
  }

  // 4. If logged in as CUSTOMER: Pure Customer Portal Experience
  const handleSelectRestaurant = (restaurant) => {
    setSelectedRestaurant(restaurant);
    setCurrentView('restaurant');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleProceedToCheckout = (instructions) => {
    setCheckoutInstructions(instructions);
    setCurrentView('checkout');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleOrderSuccess = (placedOrder) => {
    setActiveTrackOrder(placedOrder);
    setCurrentView('tracking');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleTrackOrderFromHistory = (order) => {
    setActiveTrackOrder(order);
    setCurrentView('tracking');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="min-h-screen flex flex-col bg-white text-[#282c3f]">
      {/* Signature Swiggy Customer Navbar */}
      <Navbar
        currentView={currentView}
        setCurrentView={setCurrentView}
        onOpenSearch={() => setSearchModalOpen(true)}
      />

      {/* Main Body Router */}
      <main className="flex-1">
        {/* 1. Customer Food Browsing View */}
        {currentView === 'home' && (
          <HomePage
            onSelectRestaurant={handleSelectRestaurant}
            onOpenSearch={() => setSearchModalOpen(true)}
          />
        )}

        {/* 2. Restaurant Menu Details */}
        {currentView === 'restaurant' && selectedRestaurant && (
          <RestaurantDetailPage
            restaurant={selectedRestaurant}
            onBack={() => setCurrentView('home')}
          />
        )}

        {/* 3. Swiggy Checkout Flow */}
        {currentView === 'checkout' && (
          <CheckoutPage
            initialInstructions={checkoutInstructions}
            onBack={() => setCurrentView(selectedRestaurant ? 'restaurant' : 'home')}
            onOrderSuccess={handleOrderSuccess}
          />
        )}

        {/* 4. Live Order Tracking */}
        {currentView === 'tracking' && activeTrackOrder && (
          <OrderTrackingPage
            orderId={activeTrackOrder.id}
            initialOrder={activeTrackOrder}
            onBackToHome={() => setCurrentView('home')}
          />
        )}

        {/* 5. Past Orders History */}
        {currentView === 'orders' && (
          <OrdersHistoryPage
            onTrackOrder={handleTrackOrderFromHistory}
            onBackToHome={() => setCurrentView('home')}
          />
        )}
      </main>

      {/* Global Modals & Drawers */}
      <CartDrawer onProceedToCheckout={handleProceedToCheckout} />
      <AuthModal />
      <CartConflictModal />
      <LocationDrawer />
      <SearchModal
        isOpen={searchModalOpen}
        onClose={() => setSearchModalOpen(false)}
        onSelectRestaurant={handleSelectRestaurant}
      />

      {/* Swiggy Customer Footer */}
      <footer className="bg-slate-950 text-white pt-12 pb-8 border-t border-slate-900 text-xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 mb-10">
            <div>
              <div className="flex items-center gap-2 mb-3">
                <div className="w-7 h-7 rounded-xl bg-[#fc8019] flex items-center justify-center text-white font-black text-sm">
                  S
                </div>
                <span className="font-black text-lg text-white tracking-tight">SWIGGY</span>
              </div>
              <p className="text-slate-400 leading-relaxed text-[11px]">
                © 2026 Swiggy Limited. All rights reserved. Full stack food delivery application powered by Spring Boot 3,
                MySQL, Redis, and the Grafana LGTM observability stack.
              </p>
            </div>

            <div>
              <h4 className="font-bold text-white text-sm mb-3">Multi-Tab Workspaces</h4>
              <ul className="space-y-2 text-slate-400">
                <li>
                  <a href="/?portal=partner" target="_blank" rel="noopener noreferrer" className="hover:text-white flex items-center gap-1 text-amber-400">
                    <span>Swiggy Partner Hub (Merchants)</span>
                    <span>↗</span>
                  </a>
                </li>
                <li>
                  <a href="/?portal=rider" target="_blank" rel="noopener noreferrer" className="hover:text-white flex items-center gap-1 text-blue-400">
                    <span>Delivery Fleet (Riders)</span>
                    <span>↗</span>
                  </a>
                </li>
                <li onClick={() => setCurrentView('home')} className="hover:text-white cursor-pointer transition-colors">
                  Order Food Online
                </li>
              </ul>
            </div>

            <div>
              <h4 className="font-bold text-white text-sm mb-3">Observability & Architecture</h4>
              <ul className="space-y-2 text-slate-400">
                <li className="hover:text-white cursor-pointer transition-colors">Spring Boot 3 + Java 21</li>
                <li className="hover:text-white cursor-pointer transition-colors">Distributed Traces (Tempo)</li>
                <li className="hover:text-white cursor-pointer transition-colors">Aggregated Logs (Loki)</li>
                <li className="hover:text-white cursor-pointer transition-colors">RED Metrics (Mimir)</li>
              </ul>
            </div>

            <div>
              <h4 className="font-bold text-white text-sm mb-3">Top Delivery Zones</h4>
              <ul className="space-y-2 text-slate-400">
                <li className="hover:text-white cursor-pointer transition-colors">Indiranagar, Bengaluru</li>
                <li className="hover:text-white cursor-pointer transition-colors">Koramangala, Bengaluru</li>
                <li className="hover:text-white cursor-pointer transition-colors">HSR Layout, Bengaluru</li>
                <li className="hover:text-white cursor-pointer transition-colors">Whitefield, Bengaluru</li>
              </ul>
            </div>
          </div>

          <div className="border-t border-slate-900 pt-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-slate-500 text-[11px]">
            <span>Crafted with Swiggy Design System • Independent Tab Sessions</span>
            <div className="flex gap-4">
              <span>Privacy Policy</span>
              <span>Terms & Conditions</span>
              <span>Security Invariants</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <LocationProvider>
      <AuthProvider>
        <CartProvider>
          <AppContent />
        </CartProvider>
      </AuthProvider>
    </LocationProvider>
  );
}
