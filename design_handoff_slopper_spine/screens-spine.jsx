// screens-spine.jsx — Direction B "Spine"
// Linear-tight library: cool charcoal, electric lime accent, Space Grotesk,
// info-dense rails with crisp typographic hierarchy.

const SP_meta = (t, color) => ({ fontFamily: t.fontMono, fontSize: 10, color: color || t.textDim, letterSpacing: 0.6 });

// Convert hex accent into rgba(r,g,b,a) for tinted backgrounds/borders.
// Keeps subtle accent fills theme-bound across all 3 Spine palettes.
const hexToRgba = (hex, a) => {
  const h = hex.replace('#', '');
  const r = parseInt(h.slice(0, 2), 16), g = parseInt(h.slice(2, 4), 16), b = parseInt(h.slice(4, 6), 16);
  return `rgba(${r}, ${g}, ${b}, ${a})`;
};
const accentA = (t, a) => hexToRgba(t.accent, a);

// ─── Scene card variant — flatter, info overlay always on ────────
const SSceneCard = ({ s, w = 200 }) => {
  const t = useTheme();
  return (
    <div style={{ width: w, flexShrink: 0 }}>
      <div style={{ position: 'relative', width: w, height: w * 9/16, borderRadius: t.radius, overflow: 'hidden', background: t.surface }}>
        <SceneArt seed={s.id} palette="cool"/>
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(180deg, transparent 45%, rgba(10,13,18,0.92))' }}/>
        {/* top row */}
        <div style={{ position: 'absolute', top: 6, left: 6, right: 6, display: 'flex', justifyContent: 'space-between' }}>
          <div style={{ ...SP_meta(t, '#fff'), padding: '2px 5px', background: 'rgba(0,0,0,0.65)', borderRadius: 3, backdropFilter: 'blur(4px)' }}>{s.res}</div>
          {s.rating >= 4 && (
            <div style={{ ...SP_meta(t, t.warn), padding: '2px 5px', background: 'rgba(0,0,0,0.65)', borderRadius: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
              <I.star size={9} fillSolid color={t.warn} sw={0}/>{s.rating.toFixed(1)}
            </div>
          )}
        </div>
        {/* bottom row */}
        <div style={{ position: 'absolute', left: 8, right: 8, bottom: 8 }}>
          <div style={{ fontFamily: t.fontBody, fontSize: 12, color: '#fff', fontWeight: 500, lineHeight: 1.15, marginBottom: 4, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>{s.title}</div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontFamily: t.fontMono, fontSize: 9, color: 'rgba(255,255,255,0.7)', letterSpacing: 0.5 }}>
            <span>{s.dur}</span>
            <span style={{ color: t.accent }}>{s.studio}</span>
          </div>
        </div>
        {/* resume */}
        {s.progress > 0 && (
          <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, height: 2, background: 'rgba(255,255,255,0.1)' }}>
            <div style={{ width: `${s.progress*100}%`, height: '100%', background: t.accent }}/>
          </div>
        )}
      </div>
    </div>
  );
};

// ─── Floating pill bottom nav ───────────────────────────────────
const SBottomNav = ({ active = 'home' }) => {
  const t = useTheme();
  const tabs = [
    { id: 'home', icon: <I.home size={18}/>, label: 'Home' },
    { id: 'library', icon: <I.film size={18}/>, label: 'Library' },
    { id: 'browse', icon: <I.grid size={18}/>, label: 'Browse' },
    { id: 'settings', icon: <I.settings size={18}/>, label: 'Settings' },
  ];
  return (
    <div style={{ position: 'absolute', left: 12, right: 12, bottom: 14, display: 'flex', justifyContent: 'center' }}>
      <div style={{
        display: 'flex', gap: 2, padding: 4,
        background: 'rgba(17,21,28,0.92)', backdropFilter: 'blur(20px)',
        border: `1px solid ${t.border}`, borderRadius: 16,
        boxShadow: '0 10px 30px rgba(0,0,0,0.4)',
      }}>
        {tabs.map(tab => (
          <div key={tab.id} style={{
            padding: '9px 12px', borderRadius: 12,
            background: tab.id === active ? t.accent : 'transparent',
            color: tab.id === active ? t.accentInk : t.textDim,
            display: 'flex', alignItems: 'center', gap: 6,
            fontFamily: t.fontBody, fontSize: 11, fontWeight: 600, letterSpacing: -0.1,
          }}>
            {tab.icon}
            {tab.id === active && tab.label}
          </div>
        ))}
      </div>
    </div>
  );
};

// ─── Home (Spine) — dense rails ──────────────────────────────────
const SpineHome = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column', color: t.text }}>
      {/* Top bar */}
      <div style={{ padding: '4px 18px 12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <SpineLogo/>
          <div style={{ fontFamily: t.fontBody, fontSize: 16, fontWeight: 600, letterSpacing: -0.3 }}>Slopper</div>
          <div style={{ ...SP_meta(t), padding: '2px 6px', background: t.surface, border: `1px solid ${t.border}`, borderRadius: 4, marginLeft: 4 }}>Living Room</div>
        </div>
        <div style={{ display: 'flex', gap: 4 }}>
          <SpineIconBtn icon={<I.search size={16}/>}/>
          <SpineIconBtn icon={<I.refresh size={16}/>}/>
        </div>
      </div>
      <div style={{ flex: 1, overflowY: 'auto', paddingBottom: 90 }}>
        {/* Big resume card */}
        <div style={{ padding: '0 18px 24px' }}>
          <SpineResumeCard/>
        </div>
        {/* Rails */}
        {RAILS.slice(1).map((rail, i) => (
          <div key={rail.id} style={{ marginBottom: 24 }}>
            <div style={{ padding: '0 18px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <div style={{ fontFamily: t.fontBody, fontSize: 14, fontWeight: 600, color: t.text, letterSpacing: -0.2 }}>{rail.title}</div>
                <div style={{ ...SP_meta(t, t.textMuted), padding: '1px 5px', background: t.surface, borderRadius: 3 }}>{rail.ids.length}</div>
                {rail.badge && (
                  <div style={{ ...SP_meta(t, t.accent), padding: '1px 5px', background: accentA(t, 0.1), border: `1px solid ${accentA(t, 0.25)}`, borderRadius: 3, textTransform: 'uppercase', fontWeight: 600 }}>{rail.badge}</div>
                )}
              </div>
              <I.chev_r size={14} color={t.textMuted}/>
            </div>
            <div style={{ display: 'flex', gap: 10, padding: '0 18px', overflowX: 'auto' }}>
              {rail.ids.map(id => <SSceneCard key={id} s={sceneById(id)} w={180}/>)}
            </div>
          </div>
        ))}
      </div>
      <SBottomNav active="home"/>
    </div>
  );
};

const SpineLogo = () => {
  const t = useTheme();
  return (
    <div style={{ width: 22, height: 22, position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ width: 16, height: 16, background: t.accent, borderRadius: 2, transform: 'rotate(45deg)' }}/>
      <div style={{ position: 'absolute', width: 6, height: 6, background: t.bg, borderRadius: 1 }}/>
    </div>
  );
};
const SpineIconBtn = ({ icon }) => {
  const t = useTheme();
  return <div style={{ width: 32, height: 32, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 6, color: t.textDim, background: 'transparent' }}>{icon}</div>;
};

const SpineResumeCard = () => {
  const t = useTheme();
  const s = sceneById('s1');
  return (
    <div style={{ background: t.surface, borderRadius: t.radiusLg, border: `1px solid ${t.border}`, overflow: 'hidden' }}>
      <div style={{ display: 'flex', gap: 0 }}>
        <div style={{ position: 'relative', width: 130, height: 88, flexShrink: 0 }}>
          <SceneArt seed={s.id} palette="cool"/>
          <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <div style={{ width: 32, height: 32, borderRadius: '50%', background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(8px)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff' }}>
              <I.play size={14} fillSolid sw={0}/>
            </div>
          </div>
        </div>
        <div style={{ flex: 1, padding: '10px 14px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', minWidth: 0 }}>
          <div>
            <div style={{ ...SP_meta(t, t.accent), textTransform: 'uppercase', fontWeight: 700, letterSpacing: 1, marginBottom: 2 }}>Resume</div>
            <div style={{ fontFamily: t.fontBody, fontSize: 14, color: t.text, fontWeight: 600, letterSpacing: -0.2, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{s.title}</div>
            <div style={{ ...SP_meta(t) }}>{s.studio} · 26:12 left</div>
          </div>
          <div style={{ height: 3, background: 'rgba(255,255,255,0.08)', borderRadius: 1.5 }}>
            <div style={{ width: `${s.progress*100}%`, height: '100%', background: t.accent, borderRadius: 1.5 }}/>
          </div>
        </div>
      </div>
    </div>
  );
};

// ─── Library (Spine) — info-dense grid with inline filters ─────────
const SpineLibrary = () => {
  const t = useTheme();
  const scenes = sceneList(10);
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '4px 18px 10px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <div style={{ fontFamily: t.fontBody, fontSize: 18, fontWeight: 600, letterSpacing: -0.3, color: t.text }}>Library</div>
          <div style={{ display: 'flex', gap: 4 }}>
            <SpineIconBtn icon={<I.search size={16}/>}/>
            <SpineIconBtn icon={<I.filter size={16}/>}/>
          </div>
        </div>
        {/* search-like inline */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 12px', background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius, marginBottom: 10 }}>
          <I.search size={14} color={t.textMuted}/>
          <div style={{ flex: 1, fontFamily: t.fontMono, fontSize: 12, color: t.textMuted }}>search · scene title, performer, tag…</div>
          <div style={{ ...SP_meta(t, t.textMuted), padding: '1px 4px', background: t.surfaceHigh, borderRadius: 3 }}>⌘K</div>
        </div>
        {/* Filter row */}
        <div style={{ display: 'flex', gap: 6, overflowX: 'auto' }}>
          <SpineFilterChip label="Sort: Date ↓" active/>
          <SpineFilterChip label="Rating ≥ 4" active/>
          <SpineFilterChip label="≥ 1080p"  active/>
          <SpineFilterChip label="Has markers" active/>
          <SpineFilterChip label="+ filter" dashed/>
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 10, ...SP_meta(t, t.textMuted) }}>
          <span>1,247 results · 286 organized</span>
          <span>Grid · auto</span>
        </div>
      </div>
      <div style={{ flex: 1, overflowY: 'auto', padding: '0 18px 100px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          {scenes.map(s => <SSceneCard key={s.id} s={s} w={166}/>)}
        </div>
      </div>
      <SBottomNav active="library"/>
    </div>
  );
};
const SpineFilterChip = ({ label, active, dashed }) => {
  const t = useTheme();
  return (
    <div style={{
      flexShrink: 0,
      padding: '5px 10px',
      background: active ? accentA(t, 0.1) : 'transparent',
      color: active ? t.accent : (dashed ? t.textMuted : t.text),
      border: `1px ${dashed ? 'dashed' : 'solid'} ${active ? accentA(t, 0.35) : t.border}`,
      borderRadius: t.radius,
      fontFamily: t.fontBody, fontSize: 11, fontWeight: 500, letterSpacing: -0.1,
      display: 'flex', alignItems: 'center', gap: 4,
    }}>{label}</div>
  );
};

// ─── Detail (Spine) — side-info dense ─────────────────────────────
const SpineDetail = () => {
  const t = useTheme();
  const s = sceneById('s5');
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <div style={{ flex: 1, overflowY: 'auto', paddingBottom: 30 }}>
        {/* Hero — smaller, more like a card */}
        <div style={{ padding: '8px 18px 0' }}>
          <div style={{ position: 'relative', width: '100%', aspectRatio: '16/10', borderRadius: t.radiusLg, overflow: 'hidden', border: `1px solid ${t.border}` }}>
            <SceneArt seed={s.id} palette="cool"/>
            <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to bottom, transparent 40%, rgba(0,0,0,0.85))' }}/>
            <div style={{ position: 'absolute', top: 10, left: 10, width: 30, height: 30, borderRadius: '50%', background: 'rgba(0,0,0,0.55)', backdropFilter: 'blur(8px)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff' }}>
              <I.chev_l size={16}/>
            </div>
            <div style={{ position: 'absolute', bottom: 12, left: 12, right: 12, display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', gap: 5 }}>
                <SpineMetaPill label="1:48"/>
                <SpineMetaPill label="4K"/>
                <SpineMetaPill label="HEVC"/>
                <SpineMetaPill label="12.4Mbps"/>
              </div>
              <div style={{ width: 44, height: 44, borderRadius: '50%', background: t.accent, color: t.accentInk, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <I.play size={20} fillSolid sw={0}/>
              </div>
            </div>
          </div>
        </div>
        <div style={{ padding: '16px 18px 0' }}>
          <div style={{ ...SP_meta(t, t.accent), fontWeight: 700, letterSpacing: 1, textTransform: 'uppercase' }}>{s.studio}</div>
          <div style={{ fontFamily: t.fontBody, fontSize: 24, fontWeight: 600, color: t.text, letterSpacing: -0.6, lineHeight: 1.1, marginTop: 4 }}>{s.title}</div>
          <div style={{ display: 'flex', gap: 8, marginTop: 8, ...SP_meta(t, t.textDim) }}>
            <span>{s.date}</span><span style={{ color: t.textFaint }}>·</span>
            <span style={{ display: 'flex', alignItems: 'center', gap: 3, color: t.warn }}><I.star size={11} fillSolid sw={0}/>{s.rating} (4 votes)</span>
            <span style={{ color: t.textFaint }}>·</span>
            <span>{s.played} plays</span>
          </div>
          {/* primary CTA + action row */}
          <button style={{
            marginTop: 16, width: '100%', padding: '12px',
            background: t.accent, color: t.accentInk, border: 0, borderRadius: t.radius,
            fontFamily: t.fontBody, fontSize: 13, fontWeight: 700, letterSpacing: -0.1,
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
          }}>
            <I.play size={14} fillSolid sw={0}/> Resume · 1:38:12 left 9:59
          </button>
          {/* meta grid */}
          <div style={{ marginTop: 18, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1, background: t.border, border: `1px solid ${t.border}`, borderRadius: t.radius, overflow: 'hidden' }}>
            <SpineMeta k="Codec"      v="HEVC + AAC"/>
            <SpineMeta k="Bitrate"    v="12.4 Mbps"/>
            <SpineMeta k="Resolution" v="3840 × 2160"/>
            <SpineMeta k="Framerate"  v="23.976 fps"/>
            <SpineMeta k="Size"       v="9.83 GB"/>
            <SpineMeta k="Added"      v="11 days ago"/>
          </div>
          {/* performers */}
          <div style={{ marginTop: 24 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 10 }}>
              <div style={{ fontFamily: t.fontBody, fontSize: 14, fontWeight: 600, color: t.text }}>Cast & crew</div>
              <div style={{ ...SP_meta(t, t.textMuted) }}>5 people</div>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {PERFORMERS.slice(0, 4).map(p => (
                <div key={p.id} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: 6, background: t.surface, borderRadius: t.radius, border: `1px solid ${t.border}` }}>
                  <div style={{ width: 36, height: 36, borderRadius: '50%', overflow: 'hidden', flexShrink: 0 }}>
                    <SceneArt seed={p.id} palette="cool"/>
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontFamily: t.fontBody, fontSize: 13, color: t.text, fontWeight: 500 }}>{p.name}</div>
                    <div style={{ ...SP_meta(t, t.textMuted) }}>{p.count} scenes</div>
                  </div>
                  {p.fav && <I.heart size={14} color={t.err} fillSolid sw={0}/>}
                  <I.chev_r size={14} color={t.textMuted}/>
                </div>
              ))}
            </div>
          </div>
          {/* Markers — chapter-like */}
          <div style={{ marginTop: 22 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 10 }}>
              <div style={{ fontFamily: t.fontBody, fontSize: 14, fontWeight: 600 }}>Chapters</div>
              <div style={{ ...SP_meta(t, t.textMuted) }}>4 markers</div>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {[
                { time: '04:12', title: 'Cold open',         tag: 'Intro' },
                { time: '18:40', title: 'Studio interview',  tag: 'Talk' },
                { time: '42:55', title: 'B-roll · port',     tag: 'Cutaway' },
                { time: '1:14:02', title: 'Closing monologue', tag: 'Outro' },
              ].map((m, i) => (
                <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 10px', background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius }}>
                  <div style={{ width: 56, height: 32, borderRadius: 4, overflow: 'hidden', flexShrink: 0 }}>
                    <SceneArt seed={'c'+i} palette="cool"/>
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontFamily: t.fontBody, fontSize: 12, color: t.text, fontWeight: 500 }}>{m.title}</div>
                    <div style={{ ...SP_meta(t, t.accent), letterSpacing: 0.4 }}>{m.tag}</div>
                  </div>
                  <div style={{ ...SP_meta(t, t.textDim) }}>{m.time}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
const SpineMetaPill = ({ label }) => {
  const t = useTheme();
  return <div style={{ padding: '3px 6px', background: 'rgba(0,0,0,0.55)', backdropFilter: 'blur(6px)', borderRadius: 4, fontFamily: t.fontMono, fontSize: 10, color: '#fff', letterSpacing: 0.4 }}>{label}</div>;
};
const SpineMeta = ({ k, v }) => {
  const t = useTheme();
  return (
    <div style={{ padding: '8px 12px', background: t.surface, display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
      <div style={{ ...SP_meta(t, t.textMuted) }}>{k}</div>
      <div style={{ fontFamily: t.fontMono, fontSize: 11, color: t.text }}>{v}</div>
    </div>
  );
};

// ─── Player (Spine) — clean, columns of info ───────────────────────
const SpinePlayer = ({ w = 820, h = 390 }) => {
  const t = useTheme();
  return (
    <div style={{
      width: w, height: h, background: '#000',
      borderRadius: 28, border: `1px solid ${t.borderStrong}`,
      boxShadow: '0 30px 80px rgba(0,0,0,0.5), inset 0 0 0 6px #000',
      position: 'relative', overflow: 'hidden', color: '#fff', fontFamily: t.fontBody,
    }}>
      <SceneArt seed="s5" palette="cool" style={{ position: 'absolute', inset: 0 }}/>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.25)' }}/>
      <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 90, background: 'linear-gradient(to bottom, rgba(0,0,0,0.85), transparent)' }}/>
      <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, height: 160, background: 'linear-gradient(to top, rgba(0,0,0,0.92), transparent)' }}/>
      {/* Top */}
      <div style={{ position: 'absolute', top: 14, left: 22, right: 22, display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ width: 32, height: 32, borderRadius: 6, background: 'rgba(255,255,255,0.06)', border: `1px solid rgba(255,255,255,0.15)`, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <I.chev_d size={16}/>
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ fontFamily: t.fontBody, fontSize: 14, fontWeight: 600, letterSpacing: -0.2 }}>Long Way to Pasadena <span style={{ color: t.accent, fontFamily: t.fontMono, fontSize: 11, marginLeft: 6, fontWeight: 400 }}>3/12</span></div>
          <div style={{ fontFamily: t.fontMono, fontSize: 10, color: 'rgba(255,255,255,0.6)', letterSpacing: 0.5, marginTop: 1 }}>Iron & Salt · 4K HEVC · 23.976 fps</div>
        </div>
        <div style={{ display: 'flex', gap: 6 }}>
          <SpinePlayerPill label="HW · HEVC"/>
          <SpinePlayerPill label="1.0×"/>
          <SpinePlayerPill icon={<I.pip size={12}/>}/>
          <SpinePlayerPill icon={<I.aspect size={12}/>}/>
          <SpinePlayerPill icon={<I.camera size={12}/>}/>
          <SpinePlayerPill icon={<I.shuffle size={12}/>}/>
          <SpinePlayerPill icon={<I.repeat size={12}/>} active/>
        </div>
      </div>
      {/* Bottom transport */}
      <div style={{ position: 'absolute', bottom: 18, left: 22, right: 22 }}>
        {/* chapter strip */}
        <div style={{ display: 'flex', gap: 2, marginBottom: 10 }}>
          {[0.05, 0.16, 0.27, 0.16, 0.36].map((w, i) => (
            <div key={i} style={{ flex: w, position: 'relative' }}>
              <div style={{ height: 4, background: i < 2 ? t.accent : (i === 2 ? `linear-gradient(to right, ${t.accent} 60%, rgba(255,255,255,0.18) 60%)` : 'rgba(255,255,255,0.18)'), borderRadius: 1 }}/>
              <div style={{ fontFamily: t.fontMono, fontSize: 9, color: i <= 2 ? '#fff' : 'rgba(255,255,255,0.45)', letterSpacing: 0.4, marginTop: 5, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {['Cold open','Interview','B-roll · port','Closing'][i] || ''}
              </div>
            </div>
          ))}
        </div>
        {/* Timeline + transport row */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginTop: 14 }}>
          <div style={{ fontFamily: t.fontMono, fontSize: 12, color: t.accent, minWidth: 60, letterSpacing: 0.5 }}>1:38:12</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 18, flex: 1, justifyContent: 'center' }}>
            <I.lock size={18} color="rgba(255,255,255,0.7)"/>
            <I.skip_p size={20} color="#fff"/>
            <I.back10 size={24} color="#fff"/>
            <div style={{ width: 52, height: 52, borderRadius: t.radiusLg, background: t.accent, color: t.accentInk, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <I.pause size={22} sw={0} fill={t.accentInk}/>
            </div>
            <I.fwd10 size={24} color="#fff"/>
            <I.skip_n size={20} color="#fff"/>
          </div>
          <div style={{ fontFamily: t.fontMono, fontSize: 12, color: 'rgba(255,255,255,0.7)', minWidth: 60, textAlign: 'right' }}>-9:59</div>
        </div>
      </div>
    </div>
  );
};

const SpinePlayerPill = ({ label, icon, active }) => {
  const t = useTheme();
  return (
    <div style={{
      height: 26, padding: '0 8px', display: 'flex', alignItems: 'center', gap: 5,
      background: active ? accentA(t, 0.15) : 'rgba(255,255,255,0.06)',
      border: `1px solid ${active ? accentA(t, 0.4) : 'rgba(255,255,255,0.15)'}`,
      color: active ? t.accent : '#fff',
      borderRadius: t.radius, fontFamily: t.fontMono, fontSize: 10, letterSpacing: 0.5,
    }}>{icon}{label}</div>
  );
};

// ─── Browse (Spine) — segmented + lists ───────────────────────────
const SpineBrowse = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '4px 18px 10px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ fontFamily: t.fontBody, fontSize: 18, fontWeight: 600, letterSpacing: -0.3 }}>Browse</div>
          <SpineIconBtn icon={<I.search size={16}/>}/>
        </div>
        {/* segmented */}
        <div style={{ display: 'flex', background: t.surface, borderRadius: t.radius, padding: 3, marginTop: 10, border: `1px solid ${t.border}` }}>
          {['Studios','Performers','Tags','Markers'].map((tab, i) => (
            <div key={tab} style={{
              flex: 1, padding: '7px', textAlign: 'center',
              background: i === 1 ? t.surfaceHigh : 'transparent',
              border: i === 1 ? `1px solid ${t.borderStrong}` : '1px solid transparent',
              borderRadius: t.radius - 1,
              fontFamily: t.fontBody, fontSize: 11, fontWeight: 500,
              color: i === 1 ? t.text : t.textDim,
            }}>{tab}</div>
          ))}
        </div>
      </div>
      <div style={{ flex: 1, overflowY: 'auto', padding: '4px 18px 100px' }}>
        {/* Performer list */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          {PERFORMERS.map((p, i) => (
            <div key={p.id} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 4px', borderBottom: `1px solid ${t.border}` }}>
              <div style={{ width: 44, height: 44, borderRadius: '50%', overflow: 'hidden', position: 'relative' }}>
                <SceneArt seed={p.id} palette="cool"/>
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <div style={{ fontFamily: t.fontBody, fontSize: 14, color: t.text, fontWeight: 500 }}>{p.name}</div>
                  {p.fav && <I.heart size={11} color={t.err} fillSolid sw={0}/>}
                </div>
                <div style={{ ...SP_meta(t, t.textMuted), marginTop: 2 }}>{p.count} scenes · last seen 3d ago</div>
              </div>
              <div style={{ display: 'flex', gap: -4 }}>
                {[0,1,2].map(j => (
                  <div key={j} style={{ width: 30, height: 18, borderRadius: 2, marginLeft: j === 0 ? 0 : -8, border: `1px solid ${t.bg}`, overflow: 'hidden' }}>
                    <SceneArt seed={p.id + j} palette="cool"/>
                  </div>
                ))}
              </div>
              <I.chev_r size={14} color={t.textMuted}/>
            </div>
          ))}
        </div>
      </div>
      <SBottomNav active="browse"/>
    </div>
  );
};

// ─── Settings (Spine) ───────────────────────────────────────────
const SpineSettings = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '4px 18px 10px' }}>
        <div style={{ fontFamily: t.fontBody, fontSize: 18, fontWeight: 600, letterSpacing: -0.3, color: t.text }}>Settings</div>
      </div>
      <div style={{ flex: 1, overflowY: 'auto', paddingBottom: 100 }}>
        <SpineGroup title="Player">
          <SpineRow k="Auto-play next" v={<SpineSwitch on/>}/>
          <SpineRow k="Auto-rotate"    v={<SpineSwitch on/>}/>
          <SpineRow k="Double-tap seek" v={<span style={{ ...SP_meta(t, t.accent) }}>10 seconds</span>}/>
          <SpineRow k="Default speed"   v={<span style={{ ...SP_meta(t, t.accent) }}>1.0×</span>}/>
          <SpineRow k="Decoder"         v={<span style={{ ...SP_meta(t, t.accent) }}>Hardware</span>}/>
          <SpineRow k="Aspect ratio"    v={<span style={{ ...SP_meta(t, t.accent) }}>Fit</span>}/>
          <SpineRow k="Buffer"          v={<span style={{ ...SP_meta(t, t.accent) }}>Medium · 50s</span>}/>
        </SpineGroup>
        <SpineGroup title="Display">
          <SpineRow k="Grid columns" v={<span style={{ ...SP_meta(t, t.accent) }}>Auto</span>}/>
          <SpineRow k="AMOLED black mode" v={<SpineSwitch/>}/>
          <SpineRow k="Show rating on cards" v={<SpineSwitch on/>}/>
          <SpineRow k="Show play count" v={<SpineSwitch on/>}/>
        </SpineGroup>
        <SpineGroup title="Server" badge="media.local:9999">
          <SpineRow k="Activity tracking" sub="Resume position + plays sync to Stash" v={<SpineSwitch on/>}/>
          <SpineRow k="Cache size" v={<span style={{ ...SP_meta(t, t.accent) }}>256 MB</span>}/>
          <SpineRow k="Codec status" v={<span style={{ ...SP_meta(t, t.accent) }}>● Full support</span>}/>
        </SpineGroup>
      </div>
      <SBottomNav active="settings"/>
    </div>
  );
};
const SpineGroup = ({ title, children, badge }) => {
  const t = useTheme();
  return (
    <div style={{ marginBottom: 16 }}>
      <div style={{ padding: '8px 18px 6px', display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
        <div style={{ fontFamily: t.fontMono, fontSize: 10, color: t.textMuted, letterSpacing: 1.5, textTransform: 'uppercase', fontWeight: 600 }}>{title}</div>
        {badge && <div style={{ ...SP_meta(t, t.cool) }}>{badge}</div>}
      </div>
      <div style={{ margin: '0 12px', background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius, overflow: 'hidden' }}>
        {children}
      </div>
    </div>
  );
};
const SpineRow = ({ k, sub, v }) => {
  const t = useTheme();
  return (
    <div style={{ padding: '10px 14px', display: 'flex', alignItems: 'center', gap: 12, borderBottom: `1px solid ${t.border}` }}>
      <div style={{ flex: 1 }}>
        <div style={{ fontFamily: t.fontBody, fontSize: 13, color: t.text }}>{k}</div>
        {sub && <div style={{ ...SP_meta(t, t.textMuted), marginTop: 2 }}>{sub}</div>}
      </div>
      {v}
    </div>
  );
};
const SpineSwitch = ({ on }) => {
  const t = useTheme();
  return (
    <div style={{ width: 30, height: 18, borderRadius: 9, background: on ? t.accent : t.surfaceHigh, position: 'relative', border: `1px solid ${on ? t.accent : t.border}` }}>
      <div style={{ position: 'absolute', top: 2, left: on ? 14 : 2, width: 12, height: 12, borderRadius: '50%', background: on ? t.accentInk : t.textDim }}/>
    </div>
  );
};

// ─── Connection (Spine) ─────────────────────────────────────────
const SpineConnect = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, padding: 24, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 24 }}>
        <SpineLogo/>
        <div style={{ fontFamily: t.fontBody, fontSize: 16, fontWeight: 600, letterSpacing: -0.3 }}>Slopper</div>
      </div>
      <div style={{ fontFamily: t.fontBody, fontSize: 26, fontWeight: 600, letterSpacing: -0.6, lineHeight: 1.05, color: t.text }}>Connect your Stash server.</div>
      <div style={{ fontFamily: t.fontBody, fontSize: 13, color: t.textDim, marginTop: 10, marginBottom: 24, lineHeight: 1.5 }}>Server URL, API key (if auth is on), and a friendly name for this device.</div>
      <SpineConnectInput label="Server URL" value="http://media.local:9999"/>
      <SpineConnectInput label="API key" value="••••••••••••" trailing={<I.eye size={14}/>}/>
      <SpineConnectInput label="Display name" value="Living Room"/>
      <div style={{ marginTop: 6, padding: 12, background: accentA(t, 0.08), border: `1px solid ${accentA(t, 0.25)}`, borderRadius: t.radius, display: 'flex', gap: 10, alignItems: 'flex-start' }}>
        <I.check size={16} color={t.accent}/>
        <div>
          <div style={{ fontFamily: t.fontBody, fontSize: 12, color: t.text }}>Stash v0.27.2 · 1,247 scenes</div>
          <div style={{ ...SP_meta(t, t.textMuted), marginTop: 2 }}>round-trip 8 ms · TLS off (local)</div>
        </div>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 18 }}>
        <button style={{ flex: 1, padding: 11, background: 'transparent', color: t.text, border: `1px solid ${t.border}`, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 12, fontWeight: 600 }}>Test</button>
        <button style={{ flex: 2, padding: 11, background: t.accent, color: t.accentInk, border: 0, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 12, fontWeight: 700 }}>Connect & continue</button>
      </div>
    </div>
  );
};
const SpineConnectInput = ({ label, value, trailing }) => {
  const t = useTheme();
  return (
    <div style={{ marginBottom: 10 }}>
      <div style={{ fontFamily: t.fontBody, fontSize: 11, color: t.textDim, marginBottom: 4 }}>{label}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '10px 12px', background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius }}>
        <div style={{ flex: 1, fontFamily: t.fontMono, fontSize: 12, color: t.text }}>{value}</div>
        {trailing}
      </div>
    </div>
  );
};

Object.assign(window, {
  SpineHome, SpineLibrary, SpineDetail, SpinePlayer, SpineBrowse, SpineSettings, SpineConnect,
});
