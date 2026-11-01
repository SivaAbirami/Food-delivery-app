import React from 'react';
import { Star, Clock, MapPin } from 'lucide-react';
import { getRestaurantImage } from '../data/swiggyData';

export default function RestaurantCard({ restaurant, onClick }) {
  const imageUrl = getRestaurantImage(restaurant.id);
  const rating = (4.1 + ((restaurant.id * 7) % 8) / 10).toFixed(1);
  const etaMinutes = 20 + ((restaurant.id * 5) % 25);
  const discountOffer = restaurant.id % 2 === 0 ? '60% OFF UPTO ₹120' : 'ITEMS AT ₹149';

  return (
    <div
      onClick={onClick}
      className="group flex flex-col cursor-pointer transition-transform duration-200 hover:scale-[0.97]"
    >
      {/* Thumbnail container with Swiggy discount banner */}
      <div className="relative w-full aspect-[16/10] overflow-hidden rounded-2xl shadow-sm bg-slate-200">
        <img
          src={imageUrl}
          alt={restaurant.name}
          className={`w-full h-full object-cover transition-transform duration-500 group-hover:scale-105 ${
            !restaurant.isOpen ? 'filter grayscale opacity-60' : ''
          }`}
          loading="lazy"
        />

        {/* Gradient shadow for offer text */}
        <div className="absolute inset-x-0 bottom-0 h-16 bg-gradient-to-t from-black/85 via-black/40 to-transparent flex items-end px-3.5 pb-2.5">
          <span className="text-white font-black text-sm md:text-base tracking-wide uppercase drop-shadow-sm">
            {discountOffer}
          </span>
        </div>

        {/* Closed overlay */}
        {!restaurant.isOpen && (
          <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
            <span className="bg-red-600 text-white font-bold text-xs px-3 py-1.5 rounded-full uppercase tracking-wider shadow">
              Currently Closed
            </span>
          </div>
        )}
      </div>

      {/* Restaurant Details */}
      <div className="mt-3 px-1">
        <h3 className="font-bold text-base md:text-lg text-slate-800 truncate group-hover:text-[#fc8019] transition-colors">
          {restaurant.name}
        </h3>

        {/* Rating and ETA */}
        <div className="flex items-center gap-2 mt-1 text-slate-700 font-semibold text-sm">
          <div className="inline-flex items-center gap-1 bg-emerald-600 text-white text-xs px-1.5 py-0.5 rounded-full font-bold">
            <Star className="w-3 h-3 fill-current text-white" />
            <span>{rating}</span>
          </div>
          <span className="text-slate-400 font-bold">•</span>
          <span className="flex items-center gap-1 text-slate-700 font-bold text-xs md:text-sm">
            <Clock className="w-3.5 h-3.5 text-slate-500" />
            {etaMinutes}-{etaMinutes + 10} mins
          </span>
        </div>

        {/* Cuisine & Description */}
        <p className="text-slate-500 text-xs md:text-sm truncate mt-1">
          {restaurant.description || 'Indian, Fast Food, Snacks, Beverages'}
        </p>

        {/* Locality */}
        <p className="text-slate-400 text-xs truncate mt-0.5 flex items-center gap-1">
          <MapPin className="w-3 h-3 shrink-0" />
          {restaurant.address}
        </p>
      </div>
    </div>
  );
}

