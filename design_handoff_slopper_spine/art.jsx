// art.jsx — procedural placeholder imagery (no real photos) + demo data

// Hash a string to a deterministic 0..1 number
const hash01 = (s) => {
  let h = 2166136261;
  for (let i = 0; i < s.length; i++) { h ^= s.charCodeAt(i); h = Math.imul(h, 16777619); }
  return ((h >>> 0) % 100000) / 100000;
};

// Two-tone gradient + abstract glyph per seed; intentionally reads as
// "placeholder", not as a fake photograph.
const SceneArt = ({ seed = 'x', w, h, label, palette = 'warm', radius = 0, style }) => {
  const r = hash01(seed);
  const r2 = hash01(seed + '_b');
  const palettes = {
    warm: [
      ['#3D2418', '#7A4523'], ['#2A1810', '#A1633C'], ['#4A2D1F', '#C97F4A'],
      ['#1F1410', '#6B3818'], ['#352014', '#8B4F2B'], ['#1E110A', '#A3623A'],
    ],
    cool: [
      ['#0F1A2B', '#2D5078'], ['#101822', '#3B5C7F'], ['#0A1320', '#1E4470'],
      ['#13202E', '#446A8B'], ['#0E1620', '#27537A'], ['#162130', '#3A618A'],
    ],
    mixed: [
      ['#1A1010', '#7A2D3D'], ['#0E1A1F', '#2E6A6E'], ['#1B1228', '#5A327A'],
      ['#22180A', '#A86A1F'], ['#0C1820', '#1B5C7D'], ['#1F0F1A', '#7A2D4B'],
      ['#15201A', '#37704A'], ['#1A1020', '#4A2E7A'],
    ],
  };
  const pool = palettes[palette] || palettes.warm;
  const [a, b] = pool[Math.floor(r * pool.length)];
  const ang = Math.floor(r2 * 360);
  // Shapes: shifting blob + grid lines
  const blobX = 20 + r * 60;
  const blobY = 30 + r2 * 40;
  const blobR = 30 + r * 30;
  return (
    <div style={{
      position: 'relative', width: w || '100%', height: h || '100%',
      borderRadius: radius, overflow: 'hidden',
      background: `linear-gradient(${ang}deg, ${a} 0%, ${b} 100%)`,
      ...style,
    }}>
      <svg viewBox="0 0 100 60" preserveAspectRatio="xMidYMid slice" width="100%" height="100%"
        style={{ position: 'absolute', inset: 0, mixBlendMode: 'screen', opacity: 0.35 }}>
        <defs>
          <radialGradient id={`g_${seed}`} cx={`${blobX}%`} cy={`${blobY}%`} r={`${blobR}%`}>
            <stop offset="0%" stopColor="#fff" stopOpacity="0.7"/>
            <stop offset="100%" stopColor="#fff" stopOpacity="0"/>
          </radialGradient>
        </defs>
        <rect width="100" height="60" fill={`url(#g_${seed})`}/>
      </svg>
      {/* very subtle film-grain hatch */}
      <div style={{
        position: 'absolute', inset: 0, opacity: 0.06, mixBlendMode: 'overlay',
        backgroundImage: 'repeating-linear-gradient(0deg, rgba(255,255,255,1) 0 1px, transparent 1px 3px)',
      }}/>
      {label && (
        <div style={{
          position: 'absolute', left: 10, bottom: 8,
          fontFamily: 'ui-monospace, monospace', fontSize: 9,
          color: 'rgba(255,255,255,0.6)', letterSpacing: 1, textTransform: 'uppercase',
        }}>{label}</div>
      )}
    </div>
  );
};

// Demo content. Names are intentionally generic / cinematic so the UI demo
// reads as a self-hosted personal video library, regardless of category.
const SCENES = [
  { id: 's1', title: 'The Tangerine Hour',         studio: 'Studio Aria',     date: '2024.11', dur: '42:18', res: '4K',   rating: 4.5, played: 7, progress: 0.62, codec: 'HEVC' },
  { id: 's2', title: 'Northbound, Empty',           studio: 'Pale Light Co.',  date: '2024.10', dur: '1:14:02', res: '1080', rating: 5.0, played: 12, progress: 0,    codec: 'AV1' },
  { id: 's3', title: 'Margins',                     studio: 'Cassette Bureau', date: '2024.09', dur: '28:45', res: '1080', rating: 3.5, played: 2, progress: 0.18, codec: 'H264' },
  { id: 's4', title: 'Quiet Engines',               studio: 'Studio Aria',     date: '2024.09', dur: '54:30', res: '4K',   rating: 4.0, played: 4, progress: 0,    codec: 'HEVC' },
  { id: 's5', title: 'Long Way to Pasadena',        studio: 'Iron & Salt',     date: '2024.08', dur: '1:48:11', res: '4K',   rating: 4.8, played: 3, progress: 0.91, codec: 'HEVC' },
  { id: 's6', title: 'Telephone Country',           studio: 'Pale Light Co.',  date: '2024.07', dur: '36:02', res: '1080', rating: 3.0, played: 1, progress: 0.42, codec: 'H264' },
  { id: 's7', title: 'Stations of the Year',        studio: 'Heron Films',     date: '2024.06', dur: '22:14', res: '1080', rating: 4.2, played: 9, progress: 0,    codec: 'AV1' },
  { id: 's8', title: 'Glass · Iron · Bread',        studio: 'Cassette Bureau', date: '2024.05', dur: '1:02:55', res: '4K',   rating: 4.6, played: 5, progress: 0.27, codec: 'HEVC' },
  { id: 's9', title: 'A Map for the Lake',          studio: 'Iron & Salt',     date: '2024.04', dur: '38:09', res: '4K',   rating: 3.8, played: 2, progress: 0,    codec: 'HEVC' },
  { id: 's10', title: 'Counting the Trains',        studio: 'Studio Aria',     date: '2024.03', dur: '47:33', res: '1080', rating: 5.0, played: 18, progress: 0.74, codec: 'AV1' },
  { id: 's11', title: 'Soft Architecture',          studio: 'Heron Films',     date: '2024.02', dur: '31:21', res: '4K',   rating: 4.1, played: 3, progress: 0,    codec: 'HEVC' },
  { id: 's12', title: 'Off-Season Pilgrim',         studio: 'Pale Light Co.',  date: '2024.01', dur: '1:23:00', res: '4K',   rating: 4.4, played: 6, progress: 0.05, codec: 'HEVC' },
];

const STUDIOS = [
  { id: 'st1', name: 'Studio Aria',     count: 84, palette: 'warm'  },
  { id: 'st2', name: 'Pale Light Co.',  count: 53, palette: 'cool'  },
  { id: 'st3', name: 'Cassette Bureau', count: 41, palette: 'mixed' },
  { id: 'st4', name: 'Iron & Salt',     count: 67, palette: 'warm'  },
  { id: 'st5', name: 'Heron Films',     count: 22, palette: 'cool'  },
  { id: 'st6', name: 'North Atlas',     count: 38, palette: 'mixed' },
];

const PERFORMERS = [
  { id: 'p1', name: 'Anya Korbel',    count: 28, fav: true  },
  { id: 'p2', name: 'Mateo Reyes',    count: 41, fav: false },
  { id: 'p3', name: 'Junko Hara',     count: 19, fav: true  },
  { id: 'p4', name: 'Eli Whitfield',  count: 34, fav: false },
  { id: 'p5', name: 'Priya Vasan',    count: 16, fav: true  },
  { id: 'p6', name: 'Theo Lindgren',  count: 22, fav: false },
];

const TAGS = [
  { id: 't1', name: 'Long take',     count: 142 },
  { id: 't2', name: '16mm',          count: 88  },
  { id: 't3', name: 'Studio shoot',  count: 213 },
  { id: 't4', name: 'On location',   count: 167 },
  { id: 't5', name: 'Interview',     count: 64  },
  { id: 't6', name: 'B-roll',        count: 305 },
  { id: 't7', name: 'Color graded',  count: 71  },
  { id: 't8', name: 'Available',     count: 12  },
];

const RAILS = [
  { id: 'r0', title: 'Continue',           ids: ['s1','s5','s10','s8','s6'] },
  { id: 'r1', title: 'Recently added',     ids: ['s2','s11','s9','s4','s7','s12'] },
  { id: 'r2', title: 'Tonight · under 45m', ids: ['s3','s7','s11','s1','s9'], badge: 'Smart' },
  { id: 'r3', title: 'From Studio Aria',   ids: ['s1','s4','s10','s11'] },
  { id: 'r4', title: 'Highly rated',       ids: ['s10','s2','s5','s8','s7'] },
];

const sceneById = (id) => SCENES.find(s => s.id === id);

// Helper: produce a paged slice for grids
const sceneList = (n = 12) => SCENES.slice(0, n);

Object.assign(window, {
  SceneArt, SCENES, STUDIOS, PERFORMERS, TAGS, RAILS, sceneById, sceneList, hash01,
});
