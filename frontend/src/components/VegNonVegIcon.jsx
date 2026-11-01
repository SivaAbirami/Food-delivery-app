import React from 'react';

export default function VegNonVegIcon({ isVeg = true, size = 'md' }) {
  const dim = size === 'sm' ? 'w-3.5 h-3.5' : size === 'lg' ? 'w-5 h-5' : 'w-4 h-4';
  const innerDim = size === 'sm' ? 'w-1.5 h-1.5' : size === 'lg' ? 'w-2.5 h-2.5' : 'w-2 h-2';

  if (isVeg) {
    return (
      <span
        title="Vegetarian"
        className={`inline-flex items-center justify-center border border-emerald-600 rounded-[3px] p-[1.5px] bg-white ${dim} shrink-0`}
      >
        <span className={`bg-emerald-600 rounded-full ${innerDim}`}></span>
      </span>
    );
  }

  return (
    <span
      title="Non-Vegetarian"
      className={`inline-flex items-center justify-center border border-red-600 rounded-[3px] p-[1.5px] bg-white ${dim} shrink-0`}
    >
      <span
        className={`border-l-[4px] border-r-[4px] border-b-[7px] border-l-transparent border-r-transparent border-b-red-600`}
      ></span>
    </span>
  );
}

