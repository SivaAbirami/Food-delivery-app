import React from 'react';
import { AlertCircle } from 'lucide-react';
import { useCart } from '../context/CartContext';

export default function CartConflictModal() {
  const { conflictModal, closeConflictModal, resolveConflictAndAdd } = useCart();

  if (!conflictModal.isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
      <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-200 border border-slate-100">
        <div className="flex items-center gap-3 text-amber-600 mb-3">
          <div className="w-10 h-10 rounded-full bg-amber-100 flex items-center justify-center shrink-0">
            <AlertCircle className="w-6 h-6 text-amber-600" />
          </div>
          <h3 className="text-lg font-bold text-slate-800">Items already in cart</h3>
        </div>

        <p className="text-sm text-slate-600 leading-relaxed">
          Your cart contains items from{' '}
          <strong className="text-slate-800 font-semibold">{conflictModal.existingRestaurant}</strong>.
          Would you like to reset your cart for items from{' '}
          <strong className="text-slate-800 font-semibold">{conflictModal.newRestaurant}</strong>?
        </p>

        <div className="mt-6 flex gap-3">
          <button
            onClick={closeConflictModal}
            className="flex-1 py-2.5 px-4 rounded-xl border border-slate-300 font-semibold text-slate-700 hover:bg-slate-50 transition-colors text-sm"
          >
            NO, KEEP CART
          </button>
          <button
            onClick={resolveConflictAndAdd}
            className="flex-1 py-2.5 px-4 rounded-xl bg-[#fc8019] text-white font-bold hover:bg-[#e67210] transition-colors text-sm shadow-sm"
          >
            YES, START AFRESH
          </button>
        </div>
      </div>
    </div>
  );
}

