import React, { createContext, useContext, useState, useEffect } from 'react';
import { api, setStoredToken, getStoredToken } from '../services/api';

const AuthContext = createContext(null);

export const DEMO_ACCOUNTS = [
  { role: 'CUSTOMER', label: 'Rahul Sharma (Customer)', email: 'customer@swiggy.in', password: 'bobPass123' },
  { role: 'RESTAURANT_OWNER', label: 'Meghana Foods (Merchant)', email: 'owner@meghana.in', password: 'bobPass123' },
  { role: 'DELIVERY_PARTNER', label: 'Ramesh Rider (Fleet)', email: 'rider1@swiggy.in', password: 'bobPass123' },
];

export const SEEDED_ACCOUNTS = {
  CUSTOMERS: [
    {
      id: 101,
      name: 'Rahul Sharma',
      email: 'customer@swiggy.in',
      role: 'CUSTOMER',
      badge: 'Indiranagar Regular',
      avatar: '👨‍💼',
      zone: 'Indiranagar',
    },
    {
      id: 102,
      name: 'Priya Patel',
      email: 'priya@swiggy.in',
      role: 'CUSTOMER',
      badge: 'Koramangala Foodie',
      avatar: '👩‍💻',
      zone: 'Koramangala',
    },
    {
      id: 103,
      name: 'Amit Verma',
      email: 'amit@swiggy.in',
      role: 'CUSTOMER',
      badge: 'HSR Layout Gourmet',
      avatar: '🧑‍🍳',
      zone: 'HSR Layout',
    },
    {
      id: 104,
      name: 'Sneha Reddy',
      email: 'sneha@swiggy.in',
      role: 'CUSTOMER',
      badge: 'Whitefield Techie',
      avatar: '👩‍🎨',
      zone: 'Whitefield',
    },
  ],
  RESTAURANTS: [
    {
      id: 201,
      restaurantId: 1001,
      name: 'Meghana Foods',
      email: 'owner@meghana.in',
      role: 'RESTAURANT_OWNER',
      cuisine: 'Biryani, Andhra',
      icon: '🍗',
      location: 'Indiranagar',
    },
    {
      id: 202,
      restaurantId: 1002,
      name: 'Truffles',
      email: 'owner@truffles.in',
      role: 'RESTAURANT_OWNER',
      cuisine: 'Burgers, Cafe',
      icon: '🍔',
      location: 'Koramangala',
    },
    {
      id: 203,
      restaurantId: 1003,
      name: 'Empire Restaurant',
      email: 'owner@empire.in',
      role: 'RESTAURANT_OWNER',
      cuisine: 'North Indian, Kebabs',
      icon: '🍛',
      location: 'Indiranagar',
    },
    {
      id: 204,
      restaurantId: 1004,
      name: 'Corner House Ice Cream',
      email: 'owner@cornerhouse.in',
      role: 'RESTAURANT_OWNER',
      cuisine: 'Desserts, Ice Cream',
      icon: '🍨',
      location: 'Indiranagar',
    },
    {
      id: 205,
      restaurantId: 1005,
      name: 'A2B - Adyar Ananda Bhavan',
      email: 'owner@a2b.in',
      role: 'RESTAURANT_OWNER',
      cuisine: 'Pure Veg, South Indian',
      icon: '🥗',
      location: 'HSR Layout',
    },
    {
      id: 206,
      restaurantId: 1007,
      name: "Leon's Burgers & Wings",
      email: 'owner@leons.in',
      role: 'RESTAURANT_OWNER',
      cuisine: 'Fast Food, Wings',
      icon: '🍟',
      location: 'Indiranagar',
    },
    {
      id: 207,
      restaurantId: 1008,
      name: 'Beijing Bites',
      email: 'owner@beijing.in',
      role: 'RESTAURANT_OWNER',
      cuisine: 'Chinese, Asian',
      icon: '🥢',
      location: 'Koramangala',
    },
    {
      id: 208,
      restaurantId: 1006,
      name: 'The Pizza Bakery',
      email: 'owner@pizzabakery.in',
      role: 'RESTAURANT_OWNER',
      cuisine: 'Woodfired Pizza, Italian',
      icon: '🍕',
      location: 'Indiranagar',
    },
    {
      id: 212,
      restaurantId: 1012,
      name: 'Chai Point',
      email: 'owner@chaipoint.in',
      role: 'RESTAURANT_OWNER',
      cuisine: 'Chai, Fast Food, Snacks',
      icon: '☕',
      location: 'Indiranagar',
    },
    {
      id: 209,
      restaurantId: 1009,
      name: 'Rolls & Bowls by Faasos',
      email: 'owner@faasos.in',
      role: 'RESTAURANT_OWNER',
      cuisine: 'Wraps, Fast Food',
      icon: '🌯',
      location: 'Indiranagar',
    },
    {
      id: 210,
      restaurantId: 1010,
      name: 'Anand Sweets (Pure Veg)',
      email: 'owner@anand.in',
      role: 'RESTAURANT_OWNER',
      cuisine: 'Mithai, Pure Veg Snacks',
      icon: '🥮',
      location: 'Commercial St',
    },
    {
      id: 211,
      restaurantId: 1011,
      name: 'Al-Amanah Cafe',
      email: 'owner@alamanah.in',
      role: 'RESTAURANT_OWNER',
      cuisine: 'Rolls, Arabian, Fast Food',
      icon: '🥙',
      location: 'Kammanahalli',
    },
  ],
  RIDERS: [
    {
      id: 301,
      name: 'Ramesh Rider',
      email: 'rider1@swiggy.in',
      role: 'DELIVERY_PARTNER',
      badge: 'Super Fast Rider ⭐ 4.9',
      zone: 'Indiranagar Hub',
      vehicle: 'KA-03-EB-4829',
    },
    {
      id: 302,
      name: 'Suresh Rider',
      email: 'rider2@swiggy.in',
      role: 'DELIVERY_PARTNER',
      badge: 'Top Rated Rider ⭐ 4.8',
      zone: 'Koramangala Hub',
      vehicle: 'KA-05-MR-9182',
    },
    {
      id: 303,
      name: 'Manoj Rider',
      email: 'rider3@swiggy.in',
      role: 'DELIVERY_PARTNER',
      badge: 'Pro Delivery Hero ⭐ 4.9',
      zone: 'HSR Layout Hub',
      vehicle: 'KA-01-EE-3341',
    },
  ],
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(getStoredToken());
  const [loading, setLoading] = useState(true);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authMode, setAuthMode] = useState('login');
  const [error, setError] = useState(null);

  // Load tab-isolated user profile on startup; support ?portal= query parameter
  useEffect(() => {
    async function loadUser() {
      // 1. Check if this specific tab already has an active session token in sessionStorage
      const existingToken = getStoredToken();
      if (existingToken) {
        try {
          const profile = await api.getProfile();
          setUser(profile);
          setLoading(false);
          return;
        } catch (err) {
          console.warn('Tab session expired:', err.message);
          setStoredToken(null);
          setToken(null);
        }
      }

      // 2. Check if URL explicitly requests a dedicated portal role (?portal=customer|partner|rider)
      const params = new URLSearchParams(window.location.search);
      const portalParam = params.get('portal');

      if (portalParam === 'partner') {
        try {
          const res = await api.login({ email: 'owner@meghana.in', password: 'bobPass123' });
          setStoredToken(res.accessToken);
          setToken(res.accessToken);
          const profile = await api.getProfile();
          setUser(profile);
          setLoading(false);
          return;
        } catch (e) {}
      } else if (portalParam === 'rider') {
        try {
          const res = await api.login({ email: 'rider1@swiggy.in', password: 'bobPass123' });
          setStoredToken(res.accessToken);
          setToken(res.accessToken);
          const profile = await api.getProfile();
          setUser(profile);
          setLoading(false);
          return;
        } catch (e) {}
      } else if (portalParam === 'customer') {
        try {
          const res = await api.login({ email: 'customer@swiggy.in', password: 'bobPass123' });
          setStoredToken(res.accessToken);
          setToken(res.accessToken);
          const profile = await api.getProfile();
          setUser(profile);
          setLoading(false);
          return;
        } catch (e) {}
      }

      // 3. Otherwise no active session in this tab: show Portal Gateway so user picks their persona
      setUser(null);
      setLoading(false);
    }
    loadUser();
  }, []);

  const login = async (email, password) => {
    setError(null);
    try {
      const response = await api.login({ email, password });
      const jwt = response.accessToken;
      setStoredToken(jwt);
      setToken(jwt);
      const profile = await api.getProfile();
      setUser(profile);
      setAuthModalOpen(false);
      return profile;
    } catch (err) {
      setError(err.message || 'Login failed');
      throw err;
    }
  };

  const register = async (name, email, password, phone) => {
    setError(null);
    try {
      await api.register({ name, email, password, phone });
      return await login(email, password);
    } catch (err) {
      setError(err.message || 'Registration failed');
      throw err;
    }
  };

  const logout = () => {
    setStoredToken(null);
    setToken(null);
    setUser(null);
  };

  // Instant 1-click account switch without passwords
  const switchAccount = async (account) => {
    try {
      setLoading(true);
      setError(null);
      const res = await api.login({ email: account.email, password: 'bobPass123' });
      setStoredToken(res.accessToken);
      setToken(res.accessToken);
      const profile = await api.getProfile();
      setUser(profile);
      return profile;
    } catch (err) {
      console.error('Failed to switch account:', err);
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  // Ensure user has target role; auto-switch if different
  const ensureRole = async (targetRole) => {
    if (user && user.role === targetRole) {
      return user;
    }
    let defaultAccount;
    if (targetRole === 'RESTAURANT_OWNER') {
      defaultAccount = SEEDED_ACCOUNTS.RESTAURANTS[0]; // Meghana Foods
    } else if (targetRole === 'DELIVERY_PARTNER') {
      defaultAccount = SEEDED_ACCOUNTS.RIDERS[0]; // Ramesh Rider
    } else {
      defaultAccount = SEEDED_ACCOUNTS.CUSTOMERS[0]; // Rahul Sharma
    }
    return await switchAccount(defaultAccount);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        error,
        authModalOpen,
        authMode,
        setAuthMode,
        openAuthModal: (mode = 'login') => {
          setAuthMode(mode);
          setError(null);
          setAuthModalOpen(true);
        },
        closeAuthModal: () => setAuthModalOpen(false),
        login,
        register,
        logout,
        switchAccount,
        ensureRole,
        SEEDED_ACCOUNTS,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
