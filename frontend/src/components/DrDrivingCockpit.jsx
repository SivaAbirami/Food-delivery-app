import React, { useState, useEffect, useRef } from 'react';
import {
  Play,
  Pause,
  RotateCcw,
  Gauge,
  KeyRound,
  CheckCircle,
  Navigation,
  Sparkles,
  Zap,
} from 'lucide-react';
import { api } from '../services/api';

export default function DrDrivingCockpit({
  orderId,
  customerOtp = '4829',
  onPositionUpdate,
  onOrderDelivered,
  isOutForDelivery = true,
}) {
  const storageKey = orderId ? `swiggy_trip_progress_${orderId}` : null;
  const initialSavedProgress = () => {
    if (!storageKey) return 0;
    const saved = localStorage.getItem(storageKey);
    return saved !== null && !isNaN(parseFloat(saved)) ? Math.min(100, Math.max(0, parseFloat(saved))) : 0;
  };

  const [progress, setProgress] = useState(initialSavedProgress);
  const [isAutoDriving, setIsAutoDriving] = useState(false);
  const [speed, setSpeed] = useState(0);
  const [otpInput, setOtpInput] = useState('');
  const [otpError, setOtpError] = useState(null);
  const [isVerifying, setIsVerifying] = useState(false);
  const [deliveryDone, setDeliveryDone] = useState(false);
  const autoDriveTimerRef = useRef(null);

  // Bengaluru Indiranagar realistic street route waypoints
  const waypoints = [
    { lat: 12.9784, lng: 77.6408, heading: 140 }, // Meghana Foods Indiranagar
    { lat: 12.9778, lng: 77.6412, heading: 145 }, // 100ft Road intersection
    { lat: 12.9770, lng: 77.6415, heading: 160 }, // CMH Road signal
    { lat: 12.9762, lng: 77.6417, heading: 175 }, // Towards 12th Main
    { lat: 12.9755, lng: 77.6418, heading: 180 }, // Domlur flyover turn
    { lat: 12.9745, lng: 77.6416, heading: 185 }, // Defense Colony
    { lat: 12.9735, lng: 77.6414, heading: 190 }, // HAL 2nd Stage
    { lat: 12.9725, lng: 77.6413, heading: 180 }, // Near customer society
    { lat: 12.9716, lng: 77.6412, heading: 170 }, // Customer Doorstep
  ];

  // Interpolate coordinates based on progress percentage (0 - 100)
  const getInterpolatedPoint = (pct) => {
    const totalSegments = waypoints.length - 1;
    const progressFraction = Math.max(0, Math.min(100, pct)) / 100;
    const segmentIndex = Math.min(
      Math.floor(progressFraction * totalSegments),
      totalSegments - 1
    );
    const segmentFraction = (progressFraction * totalSegments) - segmentIndex;

    const p1 = waypoints[segmentIndex];
    const p2 = waypoints[segmentIndex + 1];

    const lat = p1.lat + (p2.lat - p1.lat) * segmentFraction;
    const lng = p1.lng + (p2.lng - p1.lng) * segmentFraction;
    const heading = p1.heading + (p2.heading - p1.heading) * segmentFraction;

    const remainingDistanceKm = Math.max(0, ((100 - pct) / 100) * 2.2).toFixed(1);
    const estimatedMinutes = Math.max(1, Math.round(((100 - pct) / 100) * 12));

    return { lat, lng, heading, remainingDistanceKm, estimatedMinutes };
  };

  // On mount: restore previous position from server or localStorage so page refresh never starts from hotel
  useEffect(() => {
    let isMounted = true;
    async function restorePosition() {
      // First update parent with initial saved progress
      const initialPct = initialSavedProgress();
      if (initialPct > 0) {
        const pt = getInterpolatedPoint(initialPct);
        if (onPositionUpdate) {
          onPositionUpdate({
            lat: pt.lat,
            lng: pt.lng,
            heading: pt.heading,
            speed: 0,
            progress: initialPct,
            remainingDistanceKm: pt.remainingDistanceKm,
            estimatedMinutes: pt.estimatedMinutes,
          });
        }
      }

      if (!orderId) return;
      try {
        const telemetry = await api.getDeliveryLocation(orderId);
        if (isMounted && telemetry && telemetry.distanceRemainingKm != null) {
          const calcPct = Math.min(
            100,
            Math.max(0, Math.round((1 - (telemetry.distanceRemainingKm / 2.2)) * 100))
          );
          if (calcPct > 0) {
            setProgress(calcPct);
            if (storageKey) localStorage.setItem(storageKey, calcPct.toString());
            const pt = getInterpolatedPoint(calcPct);
            if (onPositionUpdate) {
              onPositionUpdate({
                lat: pt.lat,
                lng: pt.lng,
                heading: pt.heading,
                speed: 0,
                progress: calcPct,
                remainingDistanceKm: pt.remainingDistanceKm,
                estimatedMinutes: pt.estimatedMinutes,
              });
            }
          }
        }
      } catch (err) {
        // Silently tolerate if offline
      }
    }

    restorePosition();
    return () => {
      isMounted = false;
    };
  }, [orderId]);

  // Broadcast current location to backend and cross-tab localStorage
  const broadcastLocation = async (pct, currentSpeed) => {
    const point = getInterpolatedPoint(pct);

    // Save in localStorage for persistent trip recovery across reloads
    if (storageKey) {
      localStorage.setItem(storageKey, pct.toString());
    }

    // Broadcast cross-tab instant telemetry sync for customer map
    if (orderId) {
      try {
        localStorage.setItem(
          `swiggy_live_telemetry_${orderId}`,
          JSON.stringify({
            lat: point.lat,
            lng: point.lng,
            heading: point.heading,
            speed: currentSpeed,
            progress: pct,
            remainingDistanceKm: point.remainingDistanceKm,
            estimatedMinutes: point.estimatedMinutes,
            timestamp: Date.now(),
          })
        );
      } catch (e) {}
    }

    if (onPositionUpdate) {
      onPositionUpdate({
        lat: point.lat,
        lng: point.lng,
        heading: point.heading,
        speed: currentSpeed,
        progress: pct,
        remainingDistanceKm: point.remainingDistanceKm,
        estimatedMinutes: point.estimatedMinutes,
      });
    }

    if (orderId) {
      try {
        await api.updateDeliveryLocation(orderId, {
          latitude: point.lat,
          longitude: point.lng,
          heading: point.heading,
          speed: currentSpeed,
          distanceRemainingKm: parseFloat(point.remainingDistanceKm),
          estimatedMinutes: point.estimatedMinutes,
        });
      } catch (err) {
        // Silently tolerate if mock or offline
      }
    }
  };

  // Auto-drive simulation loop
  useEffect(() => {
    if (isAutoDriving) {
      setSpeed(36);
      autoDriveTimerRef.current = setInterval(() => {
        setProgress((prev) => {
          if (prev >= 100) {
            clearInterval(autoDriveTimerRef.current);
            setIsAutoDriving(false);
            setSpeed(0);
            broadcastLocation(100, 0);
            return 100;
          }
          const next = Math.min(100, prev + 2.5);
          broadcastLocation(next, 38);
          return next;
        });
      }, 500);
    } else {
      clearInterval(autoDriveTimerRef.current);
    }
    return () => clearInterval(autoDriveTimerRef.current);
  }, [isAutoDriving]);

  // Accelerate / Drive Step
  const handleAccelerate = () => {
    setSpeed(42);
    setProgress((prev) => {
      const next = Math.min(100, prev + 5);
      broadcastLocation(next, 42);
      return next;
    });
    setTimeout(() => setSpeed(20), 400);
  };

  // Brake / Stop
  const handleBrake = () => {
    setIsAutoDriving(false);
    setSpeed(0);
    broadcastLocation(progress, 0);
  };

  // Manual Progress Slider
  const handleSliderChange = (e) => {
    const val = parseFloat(e.target.value);
    setProgress(val);
    setSpeed(val > progress ? 35 : 10);
    broadcastLocation(val, 30);
  };

  // Reset to Restaurant
  const handleReset = () => {
    setIsAutoDriving(false);
    setProgress(0);
    setSpeed(0);
    setOtpInput('');
    setOtpError(null);
    setDeliveryDone(false);
    if (storageKey) localStorage.removeItem(storageKey);
    if (orderId) localStorage.removeItem(`swiggy_live_telemetry_${orderId}`);
    broadcastLocation(0, 0);
  };

  // Verify OTP Handshake upon Arrival
  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setIsVerifying(true);
    setOtpError(null);
    try {
      if (orderId) {
        await api.verifyDeliveryOtp(orderId, otpInput);
      }
      setDeliveryDone(true);
      if (storageKey) localStorage.removeItem(storageKey);
      if (orderId) localStorage.removeItem(`swiggy_live_telemetry_${orderId}`);
      if (onOrderDelivered) onOrderDelivered();
    } catch (err) {
      setOtpError(err.message || 'Invalid OTP code. Please check Customer screen.');
    } finally {
      setIsVerifying(false);
    }
  };

  const point = getInterpolatedPoint(progress);

  return (
    <div className="bg-slate-900 text-white rounded-3xl p-5 shadow-2xl border border-slate-800">
      {/* Header */}
      <div className="flex items-center justify-between pb-3 border-b border-slate-800">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-xl bg-orange-500/20 text-[#fc8019] flex items-center justify-center font-bold">
            🎮
          </div>
          <div>
            <h3 className="font-black text-sm tracking-tight text-white flex items-center gap-1.5">
              <span>Dr. Driving Rider Simulator</span>
              <span className="text-[10px] font-mono bg-orange-500/30 text-orange-400 px-1.5 py-0.5 rounded-full">
                Active Telemetry
              </span>
            </h3>
            <p className="text-[11px] text-slate-400">
              Steer the Swiggy Bike or Auto-Drive to Customer
            </p>
          </div>
        </div>

        {/* Live Gauges */}
        <div className="flex items-center gap-3">
          <div className="bg-slate-800 px-3 py-1.5 rounded-xl text-center border border-slate-700">
            <div className="text-[9px] text-slate-400 uppercase font-bold">Speed</div>
            <div className="text-xs font-black text-orange-400 font-mono">{speed} km/h</div>
          </div>
          <div className="bg-slate-800 px-3 py-1.5 rounded-xl text-center border border-slate-700">
            <div className="text-[9px] text-slate-400 uppercase font-bold">ETA</div>
            <div className="text-xs font-black text-emerald-400 font-mono">{point.estimatedMinutes}m</div>
          </div>
        </div>
      </div>

      {/* Main Driving Controls */}
      <div className="py-4 space-y-4">
        {/* Route Progress Slider */}
        <div>
          <div className="flex items-center justify-between text-xs font-bold text-slate-400 mb-1.5">
            <span>🏪 Kitchen ({progress.toFixed(0)}%)</span>
            <span>📍 {point.remainingDistanceKm} km left</span>
            <span>🏠 Customer</span>
          </div>
          <input
            type="range"
            min="0"
            max="100"
            value={progress}
            onChange={handleSliderChange}
            className="w-full h-2.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-[#fc8019]"
          />
        </div>

        {/* Dr. Driving Action Buttons */}
        <div className="grid grid-cols-3 gap-2.5">
          <button
            type="button"
            onClick={() => setIsAutoDriving(!isAutoDriving)}
            className={`py-3 px-3 rounded-2xl font-black text-xs flex items-center justify-center gap-1.5 transition-all cursor-pointer shadow-lg ${
              isAutoDriving
                ? 'bg-amber-500 text-slate-950 shadow-amber-500/20 animate-pulse'
                : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-emerald-600/20'
            }`}
          >
            {isAutoDriving ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
            <span>{isAutoDriving ? 'Pause Auto-Drive' : 'Auto-Drive'}</span>
          </button>

          <button
            type="button"
            onClick={handleAccelerate}
            disabled={progress >= 100}
            className="py-3 px-3 rounded-2xl bg-[#fc8019] hover:bg-[#e67210] disabled:opacity-40 text-white font-black text-xs flex items-center justify-center gap-1.5 transition-all cursor-pointer shadow-lg shadow-orange-500/25 active:scale-95"
          >
            <Zap className="w-4 h-4" />
            <span>Accelerate ⚡</span>
          </button>

          <button
            type="button"
            onClick={handleBrake}
            className="py-3 px-3 rounded-2xl bg-rose-600 hover:bg-rose-500 text-white font-black text-xs flex items-center justify-center gap-1.5 transition-all cursor-pointer shadow-lg shadow-rose-600/20 active:scale-95"
          >
            <span>Brake 🛑</span>
          </button>
        </div>

        {/* Reset button */}
        <div className="flex justify-end">
          <button
            type="button"
            onClick={handleReset}
            className="text-[11px] text-slate-400 hover:text-slate-200 flex items-center gap-1 font-bold cursor-pointer"
          >
            <RotateCcw className="w-3 h-3" />
            <span>Reset Journey to Start</span>
          </button>
        </div>
      </div>

      {/* Arrival & 4-Digit Delivery OTP Handshake */}
      {progress >= 100 && !deliveryDone && (
        <div className="mt-2 p-4 bg-gradient-to-r from-emerald-950/80 to-slate-900 border border-emerald-500/40 rounded-2xl animate-in zoom-in-95">
          <div className="flex items-center gap-2 mb-2 text-emerald-400 font-black text-xs">
            <CheckCircle className="w-4 h-4" />
            <span>ARRIVED AT CUSTOMER DOORSTEP!</span>
          </div>
          <p className="text-[11px] text-slate-300 mb-3">
            Ask customer for the <strong className="text-orange-400">4-digit Delivery OTP</strong> shown on their tracking screen to complete order.
          </p>

          <form onSubmit={handleVerifyOtp} className="flex gap-2">
            <div className="relative flex-1">
              <KeyRound className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                maxLength="4"
                placeholder="e.g. 4829"
                value={otpInput}
                onChange={(e) => setOtpInput(e.target.value)}
                className="w-full pl-9 pr-3 py-2 rounded-xl bg-slate-800 border border-slate-700 text-white font-mono text-center tracking-widest text-sm font-black focus:outline-none focus:border-emerald-500"
              />
            </div>
            <button
              type="submit"
              disabled={otpInput.length < 4 || isVerifying}
              className="py-2 px-4 rounded-xl bg-emerald-500 hover:bg-emerald-400 disabled:opacity-40 text-slate-950 font-black text-xs flex items-center gap-1 transition-all cursor-pointer shadow-md shadow-emerald-500/20"
            >
              <span>{isVerifying ? 'Verifying...' : 'Complete Delivery'}</span>
            </button>
          </form>

          {otpError && (
            <div className="text-[11px] text-rose-400 mt-2 font-bold">
              ⚠️ {otpError}
            </div>
          )}

          {/* Quick Demo Hint */}
          <div className="text-[10px] text-slate-500 mt-2">
            Demo Helper: Customer OTP is <span className="font-mono text-orange-400 font-bold">{customerOtp || '4829'}</span>
          </div>
        </div>
      )}

      {deliveryDone && (
        <div className="mt-2 p-3 bg-emerald-500/20 border border-emerald-500 rounded-2xl flex items-center gap-2 text-emerald-400 text-xs font-black">
          <CheckCircle className="w-4 h-4 shrink-0" />
          <span>Delivery Successfully Completed! Thank you for delivering with Swiggy!</span>
        </div>
      )}
    </div>
  );
}

