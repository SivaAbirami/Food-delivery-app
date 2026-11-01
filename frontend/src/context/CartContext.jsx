import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { api } from '../services/api';
import { useAuth } from './AuthContext';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const { user, token, openAuthModal } = useAuth();
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(false);
  const [cartDrawerOpen, setCartDrawerOpen] = useState(false);
  const [conflictModal, setConflictModal] = useState({
    isOpen: false,
    existingRestaurant: '',
    newRestaurant: '',
    pendingItem: null,
  });
  const [appliedCoupon, setAppliedCoupon] = useState(null);

  // Load cart when user logs in as CUSTOMER
  const refreshCart = useCallback(async () => {
    if (!token || user?.role !== 'CUSTOMER') {
      setCart(null);
      return;
    }
    try {
      setLoading(true);
      const data = await api.getCart();
      setCart(data);
    } catch (err) {
      console.warn('Could not fetch cart:', err.message);
    } finally {
      setLoading(false);
    }
  }, [token, user]);

  useEffect(() => {
    refreshCart();
  }, [refreshCart]);

  const addItem = async (menuItem, targetRestaurant) => {
    if (!token) {
      openAuthModal('login');
      return;
    }

    // Check if cart already has items from another restaurant
    if (cart && cart.items && cart.items.length > 0 && cart.restaurantId) {
      if (targetRestaurant && cart.restaurantId !== targetRestaurant.id) {
        setConflictModal({
          isOpen: true,
          existingRestaurant: cart.restaurantName || 'Another restaurant',
          newRestaurant: targetRestaurant.name || 'New restaurant',
          pendingItem: { menuItem, targetRestaurant },
        });
        return;
      }
    }

    try {
      setLoading(true);
      const updated = await api.addToCart(menuItem.id, 1);
      setCart(updated);
      setCartDrawerOpen(true);
    } catch (err) {
      // Check if backend responded with multiple restaurant error
      if (err.message && err.message.toLowerCase().includes('restaurant')) {
        setConflictModal({
          isOpen: true,
          existingRestaurant: cart?.restaurantName || 'Current restaurant',
          newRestaurant: targetRestaurant?.name || 'Selected restaurant',
          pendingItem: { menuItem, targetRestaurant },
        });
      } else {
        alert(err.message || 'Failed to add item to cart');
      }
    } finally {
      setLoading(false);
    }
  };

  const resolveConflictAndAdd = async () => {
    const { pendingItem } = conflictModal;
    setConflictModal({ isOpen: false, existingRestaurant: '', newRestaurant: '', pendingItem: null });
    if (!pendingItem) return;

    try {
      setLoading(true);
      await api.clearCart();
      const updated = await api.addToCart(pendingItem.menuItem.id, 1);
      setCart(updated);
      setCartDrawerOpen(true);
    } catch (err) {
      alert(err.message || 'Failed to reset cart');
    } finally {
      setLoading(false);
    }
  };

  const updateQuantity = async (cartItemId, newQuantity) => {
    try {
      setLoading(true);
      if (newQuantity <= 0) {
        const updated = await api.removeCartItem(cartItemId);
        setCart(updated);
      } else {
        const updated = await api.updateCartItem(cartItemId, newQuantity);
        setCart(updated);
      }
    } catch (err) {
      alert(err.message || 'Failed to update quantity');
    } finally {
      setLoading(false);
    }
  };

  const clearCart = async () => {
    try {
      setLoading(true);
      await api.clearCart();
      setCart(null);
      setAppliedCoupon(null);
    } catch (err) {
      alert(err.message || 'Failed to clear cart');
    } finally {
      setLoading(false);
    }
  };

  // Helper calculations
  const totalItemCount = cart?.items ? cart.items.reduce((sum, item) => sum + item.quantity, 0) : 0;
  const subtotal = cart?.totalAmount ? parseFloat(cart.totalAmount) : 0;
  
  // Delivery Fee: ₹30, free if > ₹250 or coupon FREEDEL
  let deliveryFee = subtotal > 0 ? (subtotal >= 250 || appliedCoupon?.code === 'FREEDEL' ? 0 : 30) : 0;
  const platformFee = subtotal > 0 ? 6 : 0;
  const taxes = subtotal > 0 ? Math.round(subtotal * 0.05) : 0;

  // Coupon discount
  let discount = 0;
  if (appliedCoupon?.discountAmount) {
    discount = parseFloat(appliedCoupon.discountAmount);
  } else if (appliedCoupon?.code === 'SWIGGY50' && subtotal > 0) {
    discount = Math.min(Math.round(subtotal * 0.5), 120);
  } else if (appliedCoupon?.code === 'FEAST150' && subtotal >= 300) {
    discount = 150;
  } else if (appliedCoupon?.discountPercentage && subtotal > 0) {
    discount = Math.min(
      Math.round(subtotal * (appliedCoupon.discountPercentage / 100)),
      appliedCoupon.maxDiscountAmount || 150
    );
  }

  const grandTotal = Math.max(0, subtotal + deliveryFee + platformFee + taxes - discount);

  return (
    <CartContext.Provider
      value={{
        cart,
        loading,
        cartDrawerOpen,
        openCart: () => setCartDrawerOpen(true),
        closeCart: () => setCartDrawerOpen(false),
        addItem,
        updateQuantity,
        clearCart,
        refreshCart,
        conflictModal,
        closeConflictModal: () => setConflictModal({ isOpen: false, existingRestaurant: '', newRestaurant: '', pendingItem: null }),
        resolveConflictAndAdd,
        totalItemCount,
        subtotal,
        deliveryFee,
        platformFee,
        taxes,
        discount,
        grandTotal,
        appliedCoupon,
        applyCoupon: (coupon) => setAppliedCoupon(coupon),
        removeCoupon: () => setAppliedCoupon(null),
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
}

