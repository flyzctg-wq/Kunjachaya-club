import React, { useEffect, useRef, useState } from 'react';

/**
 * Full-screen video/animated splash shown on first load.
 *
 * - If /splash_video.mp4 is present in public/, plays the video then calls onFinished.
 * - Otherwise (or on video error) falls back to an animated CSS logo splash.
 * - Always auto-dismisses after maxDurationMs (default 5 s) regardless.
 *
 * To use a video: copy splash_video.mp4 to web/public/splash_video.mp4 and rebuild/redeploy.
 */
export default function SplashScreen({ onFinished, maxDurationMs = 5000 }) {
  const [videoFailed, setVideoFailed] = useState(false);
  const [fadeOut, setFadeOut] = useState(false);
  const videoRef = useRef(null);
  const timerRef = useRef(null);

  const dismiss = () => {
    setFadeOut(true);
    setTimeout(onFinished, 600); // wait for CSS fade-out transition
  };

  useEffect(() => {
    // Hard cap: dismiss regardless of video state
    timerRef.current = setTimeout(dismiss, maxDurationMs);
    return () => clearTimeout(timerRef.current);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleVideoEnd = () => {
    clearTimeout(timerRef.current);
    dismiss();
  };

  const handleVideoError = () => {
    setVideoFailed(true);
    // The fallback CSS splash will auto-dismiss via the already-running timer.
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 9999,
        transition: 'opacity 0.6s ease',
        opacity: fadeOut ? 0 : 1,
        pointerEvents: fadeOut ? 'none' : 'auto',
      }}
    >
      {!videoFailed ? (
        /* ------------------------------------------------------------------ */
        /* Video splash — plays if public/splash_video.mp4 is present          */
        /* ------------------------------------------------------------------ */
        <div style={{ width: '100%', height: '100%', background: '#000', position: 'relative' }}>
          <video
            ref={videoRef}
            src="/splash_video.mp4"
            autoPlay
            muted
            playsInline
            onEnded={handleVideoEnd}
            onError={handleVideoError}
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              display: 'block',
            }}
          />
          {/* Skip button */}
          <button
            onClick={dismiss}
            aria-label="Skip splash screen"
            style={{
              position: 'absolute',
              bottom: 40,
              right: 40,
              background: 'rgba(255,255,255,0.15)',
              border: '1px solid rgba(255,255,255,0.3)',
              color: '#fff',
              padding: '8px 22px',
              borderRadius: 24,
              cursor: 'pointer',
              fontSize: 14,
              backdropFilter: 'blur(8px)',
              letterSpacing: '0.05em',
            }}
          >
            Skip ›
          </button>
        </div>
      ) : (
        /* ------------------------------------------------------------------ */
        /* Animated CSS fallback splash                                        */
        /* ------------------------------------------------------------------ */
        <AnimatedSplash onSkip={dismiss} />
      )}
    </div>
  );
}

/* -------------------------------------------------------------------------- */
/* Animated Compose-style logo splash (pure CSS, no dependencies)             */
/* -------------------------------------------------------------------------- */

function AnimatedSplash({ onSkip }) {
  return (
    <>
      <style>{`
        @keyframes kcPulse {
          0%, 100% { transform: scale(0.94); }
          50%       { transform: scale(1.06); }
        }
        @keyframes kcFadeUp {
          from { opacity: 0; transform: translateY(20px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @keyframes kcShimmer {
          0%   { opacity: 0.55; }
          50%  { opacity: 1;    }
          100% { opacity: 0.55; }
        }
        @keyframes kcDot {
          0%, 80%, 100% { transform: scale(0.6); opacity: 0.2; }
          40%            { transform: scale(1.0); opacity: 1;   }
        }
        .kc-pulse  { animation: kcPulse   1.5s ease-in-out infinite; }
        .kc-fadeup { animation: kcFadeUp  0.9s ease forwards; }
        .kc-delay1 { animation-delay: 0.3s; opacity: 0; }
        .kc-delay2 { animation-delay: 0.55s; opacity: 0; }
        .kc-shimmer { animation: kcShimmer 2s ease-in-out infinite; }
        .kc-dot1 { animation: kcDot 1.2s ease-in-out 0.0s infinite; }
        .kc-dot2 { animation: kcDot 1.2s ease-in-out 0.2s infinite; }
        .kc-dot3 { animation: kcDot 1.2s ease-in-out 0.4s infinite; }
      `}</style>

      <div
        style={{
          width: '100%',
          height: '100%',
          background: 'linear-gradient(160deg, #0A1628 0%, #1B2D4F 50%, #0D1F3C 100%)',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 20,
          position: 'relative',
        }}
      >
        {/* Logo orb */}
        <div
          className="kc-pulse"
          style={{
            width: 130,
            height: 130,
            borderRadius: '50%',
            background: 'radial-gradient(circle at 40% 35%, #4FC3F7, #0288D1, #01579B)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 0 48px 12px rgba(2, 136, 209, 0.45)',
          }}
        >
          <span
            style={{
              color: '#fff',
              fontSize: 34,
              fontWeight: 900,
              fontFamily: 'system-ui, sans-serif',
              letterSpacing: '-1px',
            }}
          >
            কুঞ্জ
          </span>
        </div>

        {/* Club name */}
        <div
          className="kc-fadeup kc-delay1"
          style={{
            color: '#E3F2FD',
            fontSize: 28,
            fontWeight: 700,
            letterSpacing: '1.5px',
            fontFamily: 'system-ui, sans-serif',
          }}
        >
          Kunjachaya Club
        </div>

        {/* Bangla tagline */}
        <div
          className="kc-fadeup kc-delay2 kc-shimmer"
          style={{
            color: '#90CAF9',
            fontSize: 15,
            fontWeight: 400,
            letterSpacing: '0.5px',
            fontFamily: 'system-ui, sans-serif',
          }}
        >
          কুঞ্জছায়া আবাসিক ক্লাব
        </div>

        {/* Loading dots */}
        <div style={{ display: 'flex', gap: 9, marginTop: 16, alignItems: 'center' }}>
          {['kc-dot1', 'kc-dot2', 'kc-dot3'].map((cls) => (
            <div
              key={cls}
              className={cls}
              style={{
                width: 9,
                height: 9,
                borderRadius: '50%',
                background: '#4FC3F7',
              }}
            />
          ))}
        </div>

        {/* Skip */}
        <button
          onClick={onSkip}
          style={{
            position: 'absolute',
            bottom: 40,
            right: 40,
            background: 'rgba(255,255,255,0.1)',
            border: '1px solid rgba(255,255,255,0.25)',
            color: '#90CAF9',
            padding: '7px 20px',
            borderRadius: 20,
            cursor: 'pointer',
            fontSize: 13,
            letterSpacing: '0.05em',
          }}
        >
          Skip ›
        </button>
      </div>
    </>
  );
}
