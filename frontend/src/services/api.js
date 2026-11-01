// API Service layer communicating with Spring Boot backend via /api

const BASE_URL = '/api';

// Use sessionStorage so multiple tabs in the same browser can each run different users (Customer, Restaurant Owner, Delivery Fleet) independently
export function getStoredToken() {
  return sessionStorage.getItem('food_delivery_token');
}

export function setStoredToken(token) {
  if (token) {
    sessionStorage.setItem('food_delivery_token', token);
  } else {
    sessionStorage.removeItem('food_delivery_token');
    localStorage.removeItem('food_delivery_token');
  }
}

async function request(endpoint, options = {}) {
  const token = getStoredToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const config = {
    ...options,
    headers,
  };

  if (options.body && typeof options.body === 'object') {
    config.body = JSON.stringify(options.body);
  }

  const response = await fetch(`${BASE_URL}${endpoint}`, config);

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type');
  const isJson = contentType && contentType.includes('application/json');
  const data = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    const errorMsg = data?.message || data?.error || (typeof data === 'string' ? data : `Error ${response.status}`);
    const err = new Error(errorMsg);
    err.status = response.status;
    err.data = data;
    throw err;
  }

  return data;
}

export const api = {
  // Auth
  login: (credentials) => request('/auth/login', { method: 'POST', body: credentials }),
  register: (payload) => request('/auth/register', { method: 'POST', body: payload }),
  getProfile: () => request('/auth/me', { method: 'GET' }),

  // Restaurants
  getRestaurants: (params = {}) => {
    const searchParams = new URLSearchParams();
    if (params.search) searchParams.set('search', params.search);
    if (params.isOpen !== undefined) searchParams.set('isOpen', params.isOpen);
    if (params.page !== undefined) searchParams.set('page', params.page);
    if (params.size !== undefined) searchParams.set('size', params.size);
    const qs = searchParams.toString();
    return request(`/restaurants${qs ? `?${qs}` : ''}`, { method: 'GET' });
  },
  getRestaurantById: (id) => request(`/restaurants/${id}`, { method: 'GET' }),
  getMyRestaurants: () => request('/restaurants/my', { method: 'GET' }),
  createRestaurant: (data) => request('/restaurants', { method: 'POST', body: data }),
  updateRestaurantStatus: (id, isOpen) => request(`/restaurants/${id}/status`, { method: 'PATCH', body: { isOpen } }),

  // Menu items
  getRestaurantMenu: (restaurantId, params = {}) => {
    const searchParams = new URLSearchParams();
    if (params.category) searchParams.set('category', params.category);
    if (params.available !== undefined) searchParams.set('available', params.available);
    if (params.search) searchParams.set('search', params.search);
    if (params.page !== undefined) searchParams.set('page', params.page);
    if (params.size !== undefined) searchParams.set('size', params.size);
    const qs = searchParams.toString();
    return request(`/restaurants/${restaurantId}/menu${qs ? `?${qs}` : ''}`, { method: 'GET' });
  },
  createMenuItem: (restaurantId, itemData) => request(`/restaurants/${restaurantId}/menu`, { method: 'POST', body: itemData }),
  updateMenuItemStatus: (itemId, isAvailable) => request(`/menu-items/${itemId}/status`, { method: 'PATCH', body: { isAvailable } }),

  // Cart
  getCart: () => request('/cart', { method: 'GET' }),
  addToCart: (menuItemId, quantity = 1) => request('/cart/items', { method: 'POST', body: { menuItemId, quantity } }),
  updateCartItem: (cartItemId, quantity) => request(`/cart/items/${cartItemId}`, { method: 'PUT', body: { quantity } }),
  removeCartItem: (cartItemId) => request(`/cart/items/${cartItemId}`, { method: 'DELETE' }),
  clearCart: () => request('/cart', { method: 'DELETE' }),

  // Orders (Customer)
  checkout: (checkoutData) => request('/orders/checkout', { method: 'POST', body: checkoutData }),
  getCustomerOrders: (page = 0, size = 10) => request(`/orders?page=${page}&size=${size}`, { method: 'GET' }),
  getOrderById: (orderId) => request(`/orders/${orderId}`, { method: 'GET' }),
  cancelOrder: (orderId) => request(`/orders/${orderId}/cancel`, { method: 'PATCH' }),

  // Owner Orders
  getOwnerRestaurantOrders: (restaurantId, status = null) => {
    const url = status
      ? `/owner/restaurants/${restaurantId}/orders?status=${status}`
      : `/owner/restaurants/${restaurantId}/orders`;
    return request(url, { method: 'GET' });
  },
  updateOwnerOrderStatus: (orderId, status) =>
    request(`/owner/orders/${orderId}/status`, { method: 'PATCH', body: { status } }),

  // Delivery Partner
  getAvailableDeliveries: () => request('/delivery/orders/available', { method: 'GET' }),
  claimDelivery: (orderId) => request(`/delivery/orders/${orderId}/claim`, { method: 'PATCH' }),
  getMyDeliveries: () => request('/delivery/orders/my', { method: 'GET' }),
  updateDeliveryStatus: (orderId, status) =>
    request(`/delivery/orders/${orderId}/status`, { method: 'PATCH', body: { status } }),

  // Dynamic Offers & Coupons
  getOffers: () => request('/offers', { method: 'GET' }),
  getRestaurantOffers: (restaurantId) => request(`/restaurants/${restaurantId}/offers`, { method: 'GET' }),
  validateCoupon: (code, restaurantId, subtotal) =>
    request('/offers/validate', { method: 'POST', body: { code, restaurantId, subtotal } }),
  createOffer: (restaurantId, offerData) =>
    request(`/owner/restaurants/${restaurantId}/offers`, { method: 'POST', body: offerData }),

  // Live GPS Telemetry & Driving Simulator
  updateDeliveryLocation: (orderId, telemetry) =>
    request(`/deliveries/${orderId}/location`, { method: 'POST', body: telemetry }),
  getDeliveryLocation: (orderId) =>
    request(`/orders/${orderId}/delivery-location`, { method: 'GET' }),
  verifyDeliveryOtp: (orderId, otp) =>
    request(`/deliveries/${orderId}/verify-otp`, { method: 'POST', body: { otp } }),
};

