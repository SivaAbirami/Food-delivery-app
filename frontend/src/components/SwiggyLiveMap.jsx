import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import { ChefHat } from 'lucide-react';

export default function SwiggyLiveMap({
  restaurantLocation = { lat: 12.9784, lng: 77.6408, name: 'Meghana Foods' },
  customerLocation = { lat: 12.9716, lng: 77.6412, name: 'Customer Address' },
  riderPosition = { lat: 12.9750, lng: 77.6410, heading: 180, speed: 32 },
  routeCoordinates = [],
  progressPercent = 0,
  isOutForDelivery = false,
  orderStatus = 'PLACED',
}) {
  const mapContainerRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const riderMarkerRef = useRef(null);
  const routePolylineRef = useRef(null);

  useEffect(() => {
    if (!mapContainerRef.current) return;

    // Initialize Leaflet Map once
    if (!mapInstanceRef.current) {
      const map = L.map(mapContainerRef.current, {
        center: [restaurantLocation.lat, restaurantLocation.lng],
        zoom: 14,
        zoomControl: false,
        attributionControl: false,
      });

      // Standard OpenStreetMap public tiles (100% free, no API key, works globally)
      L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors',
      }).addTo(map);

      // Force recalculation of container size after mounting to avoid blank/grey tiles
      setTimeout(() => {
        map.invalidateSize();
      }, 250);

      // Restaurant Icon Marker
      const restIcon = L.divIcon({
        className: 'custom-rest-marker',
        html: `
          <div style="background: #282c3f; color: white; padding: 6px 10px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.3); display: flex; align-items: center; gap: 5px; border: 2px solid #fc8019;">
            <span style="font-size: 14px;">🏪</span>
            <span style="font-size: 11px; font-weight: 900; white-space: nowrap;">${restaurantLocation.name || 'Kitchen'}</span>
          </div>
        `,
        iconSize: [110, 32],
        iconAnchor: [55, 16],
      });
      L.marker([restaurantLocation.lat, restaurantLocation.lng], { icon: restIcon }).addTo(map);

      // Customer Destination Marker
      const custIcon = L.divIcon({
        className: 'custom-cust-marker',
        html: `
          <div style="background: #10b981; color: white; padding: 6px 10px; border-radius: 12px; box-shadow: 0 4px 12px rgba(16,185,129,0.4); display: flex; align-items: center; gap: 5px; border: 2px solid white;">
            <span style="font-size: 14px;">🏠</span>
            <span style="font-size: 11px; font-weight: 900; white-space: nowrap;">Your Location</span>
          </div>
        `,
        iconSize: [115, 32],
        iconAnchor: [57, 16],
      });
      L.marker([customerLocation.lat, customerLocation.lng], { icon: custIcon }).addTo(map);

      // Route Polyline
      const polyline = L.polyline(
        routeCoordinates.length > 0
          ? routeCoordinates
          : [
              [restaurantLocation.lat, restaurantLocation.lng],
              [customerLocation.lat, customerLocation.lng],
            ],
        {
          color: '#fc8019',
          weight: 5,
          opacity: 0.9,
          dashArray: '8, 8',
          lineCap: 'round',
        }
      ).addTo(map);
      routePolylineRef.current = polyline;

      // Fit bounds to restaurant & customer
      try {
        const bounds = L.latLngBounds([
          [restaurantLocation.lat, restaurantLocation.lng],
          [customerLocation.lat, customerLocation.lng],
        ]);
        map.fitBounds(bounds, { padding: [60, 60] });
      } catch (e) {
        // bounds fallback
      }

      mapInstanceRef.current = map;
    }

    const map = mapInstanceRef.current;
    if (!map) return;

    // Show Rider Marker ONLY when order is OUT_FOR_DELIVERY
    if (isOutForDelivery) {
      if (!riderMarkerRef.current) {
        const bikeIcon = L.divIcon({
          className: 'custom-bike-marker',
          html: `
            <div style="position: relative; display: flex; flex-direction: column; align-items: center;">
              <div class="swiggy-bike-rotator" style="transform: rotate(${riderPosition.heading || 0}deg); transition: transform 0.3s ease; position: relative;">
                <div style="position: absolute; width: 44px; height: 44px; top: -6px; left: -6px; border-radius: 50%; background: rgba(252,128,25,0.3); animation: ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite;"></div>
                <div style="width: 34px; height: 34px; background: #fc8019; border-radius: 50%; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 14px rgba(252,128,25,0.6); border: 2.5px solid white; color: white; font-size: 17px;">
                  🛵
                </div>
              </div>
              <div class="swiggy-bike-speed-badge" style="margin-top: 4px; background: #282c3f; color: white; font-size: 9px; font-weight: 900; padding: 2px 7px; border-radius: 6px; white-space: nowrap; box-shadow: 0 2px 6px rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.2);">
                SWIGGY • ${Math.round(riderPosition.speed || 0)} km/h
              </div>
            </div>
          `,
          iconSize: [80, 56],
          iconAnchor: [40, 20],
        });

        riderMarkerRef.current = L.marker([riderPosition.lat, riderPosition.lng], { icon: bikeIcon }).addTo(map);
      } else {
        riderMarkerRef.current.setLatLng([riderPosition.lat, riderPosition.lng]);
        const markerEl = riderMarkerRef.current.getElement();
        if (markerEl) {
          const rotator = markerEl.querySelector('.swiggy-bike-rotator');
          if (rotator) {
            rotator.style.transform = `rotate(${riderPosition.heading || 0}deg)`;
          }
          const badge = markerEl.querySelector('.swiggy-bike-speed-badge');
          if (badge) {
            badge.textContent = `SWIGGY • ${Math.round(riderPosition.speed || 0)} km/h`;
          }
        }
      }
    } else {
      // Remove rider marker if not out for delivery
      if (riderMarkerRef.current) {
        map.removeLayer(riderMarkerRef.current);
        riderMarkerRef.current = null;
      }
    }
  }, [riderPosition.lat, riderPosition.lng, riderPosition.heading, riderPosition.speed, isOutForDelivery, restaurantLocation, customerLocation]);

  return (
    <div className="relative w-full h-[400px] min-h-[400px] rounded-3xl overflow-hidden shadow-inner border border-slate-200 bg-slate-100">
      <div ref={mapContainerRef} className="w-full h-full min-h-[400px] z-0" />

      {/* Floating Status HUD Overlay */}
      <div className="absolute top-4 left-4 z-10 bg-white/95 backdrop-blur-md px-4 py-2.5 rounded-2xl shadow-lg border border-slate-100 flex items-center gap-3">
        {isOutForDelivery ? (
          <>
            <div className="w-9 h-9 rounded-xl bg-orange-100 text-[#fc8019] flex items-center justify-center font-black text-lg">
              🛵
            </div>
            <div>
              <div className="text-xs font-black text-slate-900 flex items-center gap-1.5">
                <span>Rider Driving to Your Door</span>
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              </div>
              <div className="text-[10px] text-slate-500 font-bold">
                Speed: {Math.round(riderPosition.speed || 30)} km/h • GPS Broadcasting
              </div>
            </div>
          </>
        ) : (
          <>
            <div className="w-9 h-9 rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center font-black">
              <ChefHat className="w-5 h-5" />
            </div>
            <div>
              <div className="text-xs font-black text-slate-900 flex items-center gap-1.5">
                <span>Cooking in Kitchen</span>
                <span className="w-2 h-2 rounded-full bg-purple-500 animate-pulse"></span>
              </div>
              <div className="text-[10px] text-slate-500 font-bold">
                Rider will be dispatched once food is packed
              </div>
            </div>
          </>
        )}
      </div>

      {/* Journey Progress Bar (Only when Out For Delivery) */}
      {isOutForDelivery && (
        <div className="absolute bottom-4 inset-x-4 z-10 bg-slate-900/95 backdrop-blur-md text-white p-3.5 rounded-2xl shadow-xl flex items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-slate-300">🏪 Kitchen</span>
            <div className="w-28 sm:w-48 h-2 bg-slate-800 rounded-full overflow-hidden">
              <div
                className="h-full bg-gradient-to-r from-orange-500 to-amber-400 rounded-full transition-all duration-300"
                style={{ width: `${Math.min(100, Math.max(0, progressPercent))}%` }}
              />
            </div>
            <span className="text-xs font-bold text-emerald-400">🏠 Delivery</span>
          </div>
          <div className="text-right">
            <span className="text-xs font-black text-orange-400">
              {progressPercent >= 100 ? 'Rider at Doorstep!' : `${Math.round(progressPercent)}% Route Traversed`}
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
