// icons.jsx — minimal stroke icons used across all 3 directions
// All accept size + color props; default stroke 1.75

const Ic = ({ d, size = 18, color = 'currentColor', sw = 1.75, fill = 'none', fillSolid, ...rest }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill={fill} stroke={color}
    strokeWidth={sw} strokeLinecap="round" strokeLinejoin="round" {...rest}>
    {typeof d === 'string' ? <path d={d}/> : d}
  </svg>
);

const I = {
  play:     (p) => <Ic {...p} fill={p?.fillSolid ? (p.color || 'currentColor') : 'none'} d="M7 5l13 7-13 7V5z"/>,
  pause:    (p) => <Ic {...p} d={<><rect x="6" y="5" width="4" height="14" rx="0.5"/><rect x="14" y="5" width="4" height="14" rx="0.5"/></>}/>,
  search:   (p) => <Ic {...p} d={<><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></>}/>,
  filter:   (p) => <Ic {...p} d="M3 5h18M6 12h12M10 19h4"/>,
  settings: (p) => <Ic {...p} d={<><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 00.3 1.8l.1.1a2 2 0 01-2.8 2.8l-.1-.1a1.7 1.7 0 00-1.8-.3 1.7 1.7 0 00-1 1.5V21a2 2 0 01-4 0v-.1a1.7 1.7 0 00-1.1-1.5 1.7 1.7 0 00-1.8.3l-.1.1A2 2 0 014.3 17l.1-.1a1.7 1.7 0 00.3-1.8 1.7 1.7 0 00-1.5-1H3a2 2 0 010-4h.1a1.7 1.7 0 001.5-1 1.7 1.7 0 00-.3-1.8l-.1-.1a2 2 0 012.8-2.8l.1.1a1.7 1.7 0 001.8.3H9a1.7 1.7 0 001-1.5V3a2 2 0 014 0v.1a1.7 1.7 0 001 1.5 1.7 1.7 0 001.8-.3l.1-.1a2 2 0 012.8 2.8l-.1.1a1.7 1.7 0 00-.3 1.8V9a1.7 1.7 0 001.5 1H21a2 2 0 010 4h-.1a1.7 1.7 0 00-1.5 1z"/></>}/>,
  home:     (p) => <Ic {...p} d="M3 10.5L12 3l9 7.5V20a1 1 0 01-1 1h-5v-7h-6v7H4a1 1 0 01-1-1v-9.5z"/>,
  grid:     (p) => <Ic {...p} d={<><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></>}/>,
  film:     (p) => <Ic {...p} d={<><rect x="3" y="4" width="18" height="16" rx="1.5"/><path d="M7 4v16M17 4v16M3 8h4M3 12h4M3 16h4M17 8h4M17 12h4M17 16h4"/></>}/>,
  user:     (p) => <Ic {...p} d={<><circle cx="12" cy="8" r="4"/><path d="M4 21c1-4 4-7 8-7s7 3 8 7"/></>}/>,
  tag:      (p) => <Ic {...p} d={<><path d="M3 12V4a1 1 0 011-1h8l9 9-9 9-9-9z"/><circle cx="8" cy="8" r="1.5"/></>}/>,
  store:    (p) => <Ic {...p} d={<><path d="M3 9l1-5h16l1 5"/><path d="M4 9v10a1 1 0 001 1h14a1 1 0 001-1V9"/><path d="M3 9c0 1.5 1.3 3 3 3s3-1.5 3-3M9 9c0 1.5 1.3 3 3 3s3-1.5 3-3M15 9c0 1.5 1.3 3 3 3s3-1.5 3-3"/></>}/>,
  more:     (p) => <Ic {...p} d={<><circle cx="5" cy="12" r="1.2" fill="currentColor"/><circle cx="12" cy="12" r="1.2" fill="currentColor"/><circle cx="19" cy="12" r="1.2" fill="currentColor"/></>}/>,
  refresh:  (p) => <Ic {...p} d="M3 12a9 9 0 0115.5-6.3L21 8M21 3v5h-5M21 12a9 9 0 01-15.5 6.3L3 16M3 21v-5h5"/>,
  star:     (p) => <Ic {...p} fill={p?.fillSolid ? (p.color || 'currentColor') : 'none'} d="M12 3l2.7 5.8 6.3.8-4.7 4.3 1.2 6.2L12 17l-5.5 3.1 1.2-6.2L3 9.6l6.3-.8L12 3z"/>,
  heart:    (p) => <Ic {...p} fill={p?.fillSolid ? (p.color || 'currentColor') : 'none'} d="M12 21s-7-4.5-9.3-9.2C1 8 3.5 4 7.2 4c2 0 3.6 1.1 4.8 2.9C13.2 5.1 14.8 4 16.8 4c3.7 0 6.2 4 4.5 7.8C19 16.5 12 21 12 21z"/>,
  check:    (p) => <Ic {...p} d="M5 12.5l4.5 4.5L19 7"/>,
  plus:     (p) => <Ic {...p} d="M12 5v14M5 12h14"/>,
  minus:    (p) => <Ic {...p} d="M5 12h14"/>,
  close:    (p) => <Ic {...p} d="M6 6l12 12M18 6L6 18"/>,
  chev_l:   (p) => <Ic {...p} d="M15 6l-6 6 6 6"/>,
  chev_r:   (p) => <Ic {...p} d="M9 6l6 6-6 6"/>,
  chev_d:   (p) => <Ic {...p} d="M6 9l6 6 6-6"/>,
  arrow_l:  (p) => <Ic {...p} d="M19 12H5M11 6l-6 6 6 6"/>,
  arrow_r:  (p) => <Ic {...p} d="M5 12h14M13 6l6 6-6 6"/>,
  bookmark: (p) => <Ic {...p} fill={p?.fillSolid ? (p.color || 'currentColor') : 'none'} d="M6 4h12v17l-6-4-6 4V4z"/>,
  clock:    (p) => <Ic {...p} d={<><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>}/>,
  hd:       (p) => <Ic {...p} d="M3 8v8M3 12h4M7 8v8M11 8h2a3 3 0 013 3v2a3 3 0 01-3 3h-2V8z"/>,
  shuffle:  (p) => <Ic {...p} d="M16 4h5v5M21 4l-7 7M3 5l6 7-6 7M16 20h5v-5M21 20L9 9"/>,
  repeat:   (p) => <Ic {...p} d="M17 2l4 4-4 4M21 6H7a4 4 0 00-4 4v2M7 22l-4-4 4-4M3 18h14a4 4 0 004-4v-2"/>,
  lock:     (p) => <Ic {...p} d={<><rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 018 0v4"/></>}/>,
  unlock:   (p) => <Ic {...p} d={<><rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 017-2.7"/></>}/>,
  rotate:   (p) => <Ic {...p} d={<><path d="M16.5 3.5l3 3-3 3"/><path d="M4 13a9 9 0 0015.5-6.5H8M7.5 20.5l-3-3 3-3"/><path d="M20 11a9 9 0 00-15.5 6.5H16"/></>}/>,
  pip:      (p) => <Ic {...p} d={<><rect x="3" y="5" width="18" height="14" rx="2"/><rect x="12" y="11" width="7" height="6" rx="1"/></>}/>,
  cast:     (p) => <Ic {...p} d="M2 8V6a2 2 0 012-2h16a2 2 0 012 2v12a2 2 0 01-2 2h-7M2 12a8 8 0 018 8M2 16a4 4 0 014 4M2 20h.01"/>,
  back10:   (p) => <Ic {...p} d={<><path d="M3 12a9 9 0 1018-2"/><path d="M3 4v6h6"/><text x="12" y="15" fontSize="7" fontFamily="ui-monospace" stroke="none" fill="currentColor" textAnchor="middle">10</text></>}/>,
  fwd10:    (p) => <Ic {...p} d={<><path d="M21 12a9 9 0 11-18-2"/><path d="M21 4v6h-6"/><text x="12" y="15" fontSize="7" fontFamily="ui-monospace" stroke="none" fill="currentColor" textAnchor="middle">10</text></>}/>,
  skip_n:   (p) => <Ic {...p} d={<><path d="M5 5l10 7-10 7V5z"/><path d="M19 5v14"/></>}/>,
  skip_p:   (p) => <Ic {...p} d={<><path d="M19 5L9 12l10 7V5z"/><path d="M5 5v14"/></>}/>,
  aspect:   (p) => <Ic {...p} d={<><rect x="3" y="5" width="18" height="14" rx="1"/><path d="M7 9v6M17 9v6"/></>}/>,
  camera:   (p) => <Ic {...p} d={<><path d="M3 8a2 2 0 012-2h2l2-2h6l2 2h2a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V8z"/><circle cx="12" cy="13" r="3.5"/></>}/>,
  speed:    (p) => <Ic {...p} d={<><path d="M12 4a8 8 0 018 8h-8V4z"/><circle cx="12" cy="12" r="8"/><path d="M12 12l3-3"/></>}/>,
  bolt:     (p) => <Ic {...p} fill="currentColor" stroke="none" d="M13 2L4 14h7l-1 8 9-12h-7l1-8z"/>,
  dot:      (p) => <Ic {...p} fill="currentColor" stroke="none" d="M12 8a4 4 0 100 8 4 4 0 000-8z"/>,
  organize: (p) => <Ic {...p} d="M3 6h18M6 12h12M10 18h4"/>,
  marker:   (p) => <Ic {...p} d={<><path d="M12 2l3 7h7l-5.5 4.5L18.5 21 12 16.5 5.5 21l2-7.5L2 9h7l3-7z"/></>}/>,
  eye:      (p) => <Ic {...p} d={<><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z"/><circle cx="12" cy="12" r="3"/></>}/>,
  eye_off:  (p) => <Ic {...p} d="M3 3l18 18M9.5 5.5A11 11 0 0112 5c7 0 11 7 11 7a18 18 0 01-2.7 3.7M6.6 6.6A18 18 0 001 12s4 7 11 7c1.8 0 3.5-.5 5-1.3M10 10a3 3 0 004 4"/>,
  download: (p) => <Ic {...p} d="M12 3v12M7 10l5 5 5-5M5 21h14"/>,
  pin:      (p) => <Ic {...p} d="M12 3l-4 4-3 1 7 7 1-3 4-4-5-5z M3 21l5-5"/>,
};

Object.assign(window, { Ic, I });
