// phone-shell.jsx — minimal phone bezel + status bar; theme-aware

const PhoneShell = ({ children, w = 390, h = 820, statusBar = true, dark = true, theme }) => {
  const t = theme || useTheme();
  return (
    <div style={{
      width: w, height: h,
      background: t.bg,
      borderRadius: 36,
      border: `1px solid ${t.borderStrong}`,
      boxShadow: '0 30px 80px rgba(0,0,0,0.4), inset 0 0 0 6px #000',
      overflow: 'hidden',
      position: 'relative',
      color: t.text,
      fontFamily: t.fontBody,
      display: 'flex', flexDirection: 'column',
      WebkitFontSmoothing: 'antialiased',
    }}>
      {statusBar && <PhoneStatus theme={t}/>}
      <div style={{ flex: 1, minHeight: 0, position: 'relative', overflow: 'hidden' }}>
        {children}
      </div>
    </div>
  );
};

const PhoneStatus = ({ theme, time = '9:41', tint }) => {
  const t = theme || useTheme();
  const c = tint || t.text;
  return (
    <div style={{
      height: 38,
      display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between',
      padding: '6px 22px 4px',
      fontFamily: t.fontBody, fontSize: 14, fontWeight: 600,
      color: c, letterSpacing: -0.1,
      flexShrink: 0,
      position: 'relative', zIndex: 5,
    }}>
      <div>{time}</div>
      {/* dynamic island */}
      <div style={{
        position: 'absolute', top: 8, left: '50%', transform: 'translateX(-50%)',
        width: 90, height: 24, background: '#000', borderRadius: 14,
      }}/>
      <div style={{ display: 'flex', gap: 6, alignItems: 'center', opacity: 0.95 }}>
        {/* signal */}
        <svg width="17" height="11" viewBox="0 0 17 11"><g fill={c}>
          <rect x="0" y="7" width="3" height="4" rx="0.5"/>
          <rect x="4.5" y="5" width="3" height="6" rx="0.5"/>
          <rect x="9" y="2.5" width="3" height="8.5" rx="0.5"/>
          <rect x="13.5" y="0" width="3" height="11" rx="0.5"/>
        </g></svg>
        {/* wifi */}
        <svg width="15" height="11" viewBox="0 0 15 11"><path fill={c} d="M7.5 0a13 13 0 017.4 2.3l-1.4 1.5a11 11 0 00-12 0L0 2.3A13 13 0 017.5 0zm0 3.5a9 9 0 015.2 1.6l-1.4 1.5a7 7 0 00-7.6 0L2.3 5.1A9 9 0 017.5 3.5zm0 3.5a5 5 0 013 1l-1.5 1.5a3 3 0 00-3 0L4.5 8a5 5 0 013-1z"/></svg>
        {/* battery */}
        <svg width="26" height="12" viewBox="0 0 26 12"><rect x="0.5" y="0.5" width="22" height="11" rx="2.5" stroke={c} fill="none" opacity="0.5"/><rect x="23.5" y="3.5" width="2" height="5" rx="0.5" fill={c} opacity="0.5"/><rect x="2" y="2" width="18" height="8" rx="1.5" fill={c}/></svg>
      </div>
    </div>
  );
};

// Reusable artboard background for design canvas
const Artboard = ({ children, theme, w, h, label, dark = true }) => (
  <ThemeProvider theme={theme}>
    <div style={{ width: w, height: h, position: 'relative' }}>
      {children}
    </div>
  </ThemeProvider>
);

// A bottom-of-content fade so scrollable areas blend into the bottom nav
const ScrollFade = ({ color, h = 56 }) => (
  <div style={{
    position: 'absolute', bottom: 0, left: 0, right: 0, height: h,
    background: `linear-gradient(to bottom, transparent, ${color})`,
    pointerEvents: 'none',
  }}/>
);

Object.assign(window, { PhoneShell, PhoneStatus, Artboard, ScrollFade });
