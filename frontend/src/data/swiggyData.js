// Real Swiggy-themed visual assets, banners, cuisine icons, and presets

export const SWIGGY_ORANGE = '#fc8019';
export const SWIGGY_DARK = '#282c3f';

export const CUISINES_LIST = [
  {
    id: 'biryani',
    name: 'Biryani',
    image: 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=300&auto=format&fit=crop&q=80',
    tag: 'INDIAN',
  },
  {
    id: 'pizza',
    name: 'Pizzas',
    image: 'https://images.unsplash.com/photo-1513104890138-7c749659a591?w=300&auto=format&fit=crop&q=80',
    tag: 'ITALIAN',
  },
  {
    id: 'burger',
    name: 'Burgers',
    image: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=300&auto=format&fit=crop&q=80',
    tag: 'FAST_FOOD',
  },
  {
    id: 'north_indian',
    name: 'North Indian',
    image: 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=300&auto=format&fit=crop&q=80',
    tag: 'INDIAN',
  },
  {
    id: 'chinese',
    name: 'Chinese',
    image: 'https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=300&auto=format&fit=crop&q=80',
    tag: 'CHINESE',
  },
  {
    id: 'rolls',
    name: 'Rolls',
    image: 'https://images.unsplash.com/photo-1626777552726-4a6b54c97e46?w=300&auto=format&fit=crop&q=80',
    tag: 'FAST_FOOD',
  },
  {
    id: 'desserts',
    name: 'Desserts',
    image: 'https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=300&auto=format&fit=crop&q=80',
    tag: 'DESSERT',
  },
  {
    id: 'south_indian',
    name: 'South Indian',
    image: 'https://images.unsplash.com/photo-1610192244261-3f33de3f55e4?w=300&auto=format&fit=crop&q=80',
    tag: 'SOUTH_INDIAN',
  },
  {
    id: 'beverages',
    name: 'Shakes & Drinks',
    image: 'https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=300&auto=format&fit=crop&q=80',
    tag: 'BEVERAGE',
  },
];

export const PROMO_BANNERS = [
  {
    id: 1,
    title: 'Gourmet Delights',
    subtitle: 'FLAT 50% OFF',
    code: 'SWIGGY50',
    color: 'from-orange-600 to-amber-500',
    image: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800&auto=format&fit=crop&q=80',
  },
  {
    id: 2,
    title: 'Fast & Hot Delivery',
    subtitle: 'FREE DELIVERY on Orders Above ₹199',
    code: 'FREEDEL',
    color: 'from-rose-600 to-orange-500',
    image: 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800&auto=format&fit=crop&q=80',
  },
  {
    id: 3,
    title: 'Weekend Biryani Feast',
    subtitle: 'UPTO ₹150 CASHBACK',
    code: 'FEAST150',
    color: 'from-amber-600 to-yellow-500',
    image: 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=800&auto=format&fit=crop&q=80',
  },
];

export const RESTAURANT_IMAGES = [
  'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=700&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1552566626-52f8b828add9?w=700&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=700&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=700&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1544025162-d76694265947?w=700&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1550547660-d9450f859349?w=700&auto=format&fit=crop&q=80',
];

export const DISH_FALLBACK_IMAGES = {
  MAIN_COURSE: 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=400&auto=format&fit=crop&q=80',
  STARTER: 'https://images.unsplash.com/photo-1567188040759-fb8a883dc6d8?w=400&auto=format&fit=crop&q=80',
  BREAD: 'https://images.unsplash.com/photo-1601050690597-df0568f70950?w=400&auto=format&fit=crop&q=80',
  BEVERAGE: 'https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=400&auto=format&fit=crop&q=80',
  DESSERT: 'https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=400&auto=format&fit=crop&q=80',
  DEFAULT: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&auto=format&fit=crop&q=80',
};

export function getRestaurantImage(id) {
  const index = (Math.abs(id || 0)) % RESTAURANT_IMAGES.length;
  return RESTAURANT_IMAGES[index];
}

export function getDishImage(dish) {
  if (dish?.imageUrl && dish.imageUrl.startsWith('http')) return dish.imageUrl;
  const cat = dish?.category || 'DEFAULT';
  return DISH_FALLBACK_IMAGES[cat] || DISH_FALLBACK_IMAGES.DEFAULT;
}

