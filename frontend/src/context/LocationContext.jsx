import React, { createContext, useContext, useState, useEffect } from 'react';

const LocationContext = createContext(null);

export const BENGALURU_LOCATIONS = [
  {
    id: 'indiranagar',
    name: 'Indiranagar',
    area: '100 Feet Road, CMH Road, HAL 2nd Stage',
    fullAddress: 'Flat 402, Sunshine Towers, 100ft Road, Indiranagar, Bengaluru, 560038',
    zone: 'East Bengaluru',
    tag: 'Indiranagar',
    lat: 12.9784,
    lng: 77.6408,
    deliveryTimeBonus: 0,
  },
  {
    id: 'koramangala',
    name: 'Koramangala',
    area: '5th Block, 7th Block, Sony World Signal',
    fullAddress: 'No. 88, 1st Cross, 5th Block, Koramangala, Bengaluru, 560095',
    zone: 'South Bengaluru',
    tag: 'Koramangala',
    lat: 12.9352,
    lng: 77.6245,
    deliveryTimeBonus: 5,
  },
  {
    id: 'hsr',
    name: 'HSR Layout',
    area: 'Sector 1, Sector 6, 27th Main Road',
    fullAddress: 'Villa 12, Green Glen Layout, Sector 6, HSR Layout, Bengaluru, 560102',
    zone: 'South-East Bengaluru',
    tag: 'HSR',
    lat: 12.9121,
    lng: 77.6446,
    deliveryTimeBonus: 5,
  },
  {
    id: 'whitefield',
    name: 'Whitefield',
    area: 'ITPL Main Road, EPIP Zone, Hope Farm',
    fullAddress: 'Prestige Shantiniketan, Tower 4, ITPL Road, Whitefield, Bengaluru, 560066',
    zone: 'East Bengaluru',
    tag: 'Whitefield',
    lat: 12.9698,
    lng: 77.7499,
    deliveryTimeBonus: 10,
  },
  {
    id: 'mgroad',
    name: 'MG Road / CBD',
    area: 'Brigade Road, Church Street, Residency Road',
    fullAddress: 'Apt 3B, Regency Enclave, Magrath Road, Bengaluru, 560001',
    zone: 'Central Bengaluru',
    tag: 'Central',
    lat: 12.9756,
    lng: 77.6066,
    deliveryTimeBonus: 0,
  },
  {
    id: 'jayanagar',
    name: 'Jayanagar',
    area: '4th Block, 9th Block, Cool Joint Circle',
    fullAddress: 'No. 14, 11th Main, 4th Block, Jayanagar, Bengaluru, 560011',
    zone: 'South Bengaluru',
    tag: 'Jayanagar',
    lat: 12.9299,
    lng: 77.5838,
    deliveryTimeBonus: 5,
  },
];

export function LocationProvider({ children }) {
  const [currentLocation, setCurrentLocation] = useState(() => {
    const saved = localStorage.getItem('swiggy_user_location');
    return saved ? JSON.parse(saved) : BENGALURU_LOCATIONS[0];
  });
  const [isLocationModalOpen, setIsLocationModalOpen] = useState(false);
  const [gpsLoading, setGpsLoading] = useState(false);

  const selectLocation = (loc) => {
    setCurrentLocation(loc);
    localStorage.setItem('swiggy_user_location', JSON.stringify(loc));
    setIsLocationModalOpen(false);
  };

  // Real HTML5 Geolocation API with fallback
  const detectCurrentLocation = () => {
    setGpsLoading(true);
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const { latitude, longitude } = position.coords;
          // Find nearest predefined Bengaluru location or create dynamic coordinate entry
          const nearest = BENGALURU_LOCATIONS.reduce((prev, curr) => {
            const prevDist = Math.hypot(prev.lat - latitude, prev.lng - longitude);
            const currDist = Math.hypot(curr.lat - latitude, curr.lng - longitude);
            return currDist < prevDist ? curr : prev;
          });

          const liveLoc = {
            id: 'gps_detected',
            name: nearest ? `${nearest.name} (Live GPS)` : 'Current Location',
            area: nearest ? nearest.area : 'Bengaluru, Karnataka',
            fullAddress: `GPS: ${latitude.toFixed(4)}° N, ${longitude.toFixed(4)}° E, Bengaluru`,
            zone: nearest ? nearest.zone : 'Bengaluru Metropolitan',
            tag: nearest ? nearest.tag : 'GPS',
            lat: latitude,
            lng: longitude,
            deliveryTimeBonus: 0,
          };
          selectLocation(liveLoc);
          setGpsLoading(false);
        },
        (error) => {
          console.warn('Geolocation denied or unavailable, using Indiranagar default:', error.message);
          selectLocation(BENGALURU_LOCATIONS[0]);
          setGpsLoading(false);
        },
        { timeout: 8000 }
      );
    } else {
      selectLocation(BENGALURU_LOCATIONS[0]);
      setGpsLoading(false);
    }
  };

  return (
    <LocationContext.Provider
      value={{
        currentLocation,
        selectLocation,
        detectCurrentLocation,
        gpsLoading,
        isLocationModalOpen,
        openLocationModal: () => setIsLocationModalOpen(true),
        closeLocationModal: () => setIsLocationModalOpen(false),
        availableLocations: BENGALURU_LOCATIONS,
      }}
    >
      {children}
    </LocationContext.Provider>
  );
}

export function useLocation() {
  const context = useContext(LocationContext);
  if (!context) {
    throw new Error('useLocation must be used within a LocationProvider');
  }
  return context;
}
