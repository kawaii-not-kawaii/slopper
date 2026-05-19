// spine-flows.jsx — modal flows for Spine: filter sheet, search,
// more sheet, customize-nav, marker editor, player settings sheet

// ─── Generic sheet shell ───────────────────────────────────────
const SheetShell = ({ title, children, height = '78%', onClose, footer }) => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.55)', backdropFilter: 'blur(4px)', display: 'flex', alignItems: 'flex-end' }}>
      <div style={{
        width: '100%', height,
        background: t.bg, borderTop: `1px solid ${t.borderStrong}`,
        borderTopLeftRadius: t.radiusLg, borderTopRightRadius: t.radiusLg,
        boxShadow: '0 -20px 60px rgba(0,0,0,0.5)',
        display: 'flex', flexDirection: 'column',
      }}>
        {/* drag handle */}
        <div style={{ display: 'flex', justifyContent: 'center', padding: '8px 0 4px' }}>
          <div style={{ width: 40, height: 4, borderRadius: 2, background: t.borderStrong }}/>
        </div>
        {/* header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '6px 18px 14px' }}>
          <div style={{ fontFamily: t.fontBody, fontSize: 16, fontWeight: 600, letterSpacing: -0.3, color: t.text }}>{title}</div>
          <div onClick={onClose} style={{ width: 30, height: 30, borderRadius: '50%', background: t.surface, display: 'flex', alignItems: 'center', justifyContent: 'center', color: t.text }}>
            <I.close size={14}/>
          </div>
        </div>
        <div style={{ flex: 1, overflowY: 'auto', padding: '0 18px 16px' }}>{children}</div>
        {footer && <div style={{ borderTop: `1px solid ${t.border}`, padding: 14 }}>{footer}</div>}
      </div>
    </div>
  );
};

// ─── Filter sheet (the big one) ───────────────────────────────
const SpineFilterSheet = () => {
  const t = useTheme();
  return (
    <SheetShell
      title="Filters"
      height="84%"
      footer={
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <button style={{ padding: '10px 12px', background: 'transparent', color: t.textDim, border: 'none', fontFamily: t.fontBody, fontSize: 12, fontWeight: 600 }}>Reset</button>
          <button style={{ marginLeft: 'auto', padding: '10px 14px', background: 'transparent', color: t.text, border: `1px solid ${t.border}`, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 12, fontWeight: 600 }}>Save view</button>
          <button style={{ padding: '10px 18px', background: t.accent, color: t.accentInk, border: 0, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 12, fontWeight: 700 }}>Apply · 218 results</button>
        </div>
      }>
      {/* active-filter strip */}
      <div style={{ display: 'flex', gap: 6, marginBottom: 18, flexWrap: 'wrap' }}>
        {['Rating ≥ 4', '≥ 1080p', 'Has markers', '< 1h'].map(c => (
          <div key={c} style={{
            display: 'flex', alignItems: 'center', gap: 4,
            padding: '5px 8px', background: accentA(t, 0.1),
            border: `1px solid ${accentA(t, 0.3)}`, color: t.accent,
            borderRadius: t.radius,
            fontFamily: t.fontBody, fontSize: 11, fontWeight: 500,
          }}>{c} <I.close size={10}/></div>
        ))}
      </div>
      {/* Sort */}
      <FSect title="Sort by">
        <FSelect value="Date · newest" />
      </FSect>
      {/* Duration */}
      <FSect title="Duration">
        <FChipRow opts={[
          ['Any', false],
          ['< 10 min', false],
          ['10–30 min', false],
          ['30–60 min', false],
          ['1–2 h', true],
          ['> 2 h', false],
        ]}/>
      </FSect>
      {/* Release date */}
      <FSect title="Released">
        <FChipRow opts={[['Any',false],['Last week',false],['Last month',true],['Last year',false],['2024',false]]}/>
      </FSect>
      {/* Resolution */}
      <FSect title="Minimum resolution">
        <FChipRow opts={[['Any',false],['720p',false],['1080p',true],['1440p',false],['4K',false]]}/>
      </FSect>
      {/* Orientation */}
      <FSect title="Orientation">
        <FChipRow opts={[['Any',true],['Landscape',false],['Portrait',false],['Square',false]]}/>
      </FSect>
      {/* Rating range */}
      <FSect title="Rating" right={<span style={{ ...SP_meta(t, t.accent) }}>4.0 ★ – 5.0 ★</span>}>
        <SpineRange/>
      </FSect>
      {/* Play count */}
      <FSect title="Play count" right={<span style={{ ...SP_meta(t, t.accent) }}>any</span>}>
        <SpineSlider value={0} max={50}/>
      </FSect>
      {/* O-counter */}
      <FSect title="O-counter" right={<span style={{ ...SP_meta(t, t.accent) }}>any</span>}>
        <SpineSlider value={0} max={20}/>
      </FSect>
      {/* Flags — tri-state */}
      <FSect title="Flags">
        <FChipRow opts={[
          ['Organized: yes', 'yes'],
          ['Has markers: yes', 'yes'],
          ['Interactive', false],
          ['In progress: no', 'no'],
          ['Has captions', false],
        ]} tri/>
      </FSect>
    </SheetShell>
  );
};

const FSect = ({ title, right, children }) => {
  const t = useTheme();
  return (
    <div style={{ marginBottom: 22 }}>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 10 }}>
        <div style={{ fontFamily: t.fontBody, fontSize: 13, fontWeight: 600, color: t.text }}>{title}</div>
        {right}
      </div>
      {children}
    </div>
  );
};
const FSelect = ({ value }) => {
  const t = useTheme();
  return (
    <div style={{ display: 'flex', alignItems: 'center', padding: '10px 12px', background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 13, color: t.text }}>
      <span style={{ flex: 1 }}>{value}</span>
      <I.chev_d size={14} color={t.textMuted}/>
    </div>
  );
};
const FChipRow = ({ opts, tri }) => {
  const t = useTheme();
  return (
    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
      {opts.map(([label, state]) => {
        const active = state === true || state === 'yes';
        const neg = state === 'no';
        return (
          <div key={label} style={{
            padding: '6px 11px',
            background: active ? accentA(t, 0.12) : (neg ? 'rgba(255,88,96,0.1)' : 'transparent'),
            color: active ? t.accent : (neg ? t.err : t.text),
            border: `1px solid ${active ? accentA(t, 0.3) : (neg ? 'rgba(255,88,96,0.3)' : t.border)}`,
            borderRadius: t.radius,
            fontFamily: t.fontBody, fontSize: 11.5, fontWeight: 500, letterSpacing: -0.1,
          }}>{label}</div>
        );
      })}
    </div>
  );
};
const SpineSlider = ({ value = 0, max = 100 }) => {
  const t = useTheme();
  const pct = value / max;
  return (
    <div style={{ height: 24, display: 'flex', alignItems: 'center' }}>
      <div style={{ flex: 1, height: 4, background: t.surfaceHigh, borderRadius: 2, position: 'relative' }}>
        <div style={{ width: `${pct*100}%`, height: '100%', background: t.accent, borderRadius: 2 }}/>
        <div style={{ position: 'absolute', left: `${pct*100}%`, top: '50%', transform: 'translate(-50%,-50%)', width: 14, height: 14, borderRadius: '50%', background: t.accent, border: `2px solid ${t.bg}` }}/>
      </div>
    </div>
  );
};
const SpineRange = () => {
  const t = useTheme();
  return (
    <div style={{ height: 24, display: 'flex', alignItems: 'center' }}>
      <div style={{ flex: 1, height: 4, background: t.surfaceHigh, borderRadius: 2, position: 'relative' }}>
        <div style={{ position: 'absolute', left: '80%', right: 0, height: '100%', background: t.accent, borderRadius: 2 }}/>
        <div style={{ position: 'absolute', left: '80%', top: '50%', transform: 'translate(-50%,-50%)', width: 14, height: 14, borderRadius: '50%', background: t.accent, border: `2px solid ${t.bg}` }}/>
        <div style={{ position: 'absolute', left: '100%', top: '50%', transform: 'translate(-50%,-50%)', width: 14, height: 14, borderRadius: '50%', background: t.accent, border: `2px solid ${t.bg}` }}/>
      </div>
    </div>
  );
};

// ─── Search expanded with results ──────────────────────────────
const SpineSearchExpanded = () => {
  const t = useTheme();
  const recent = ['Studio Aria', 'long take', 'Anya Korbel', '4K · 2024'];
  const sceneHits = sceneList(3);
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      {/* search bar */}
      <div style={{ padding: '6px 14px 12px', borderBottom: `1px solid ${t.border}` }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{ color: t.textMuted }}><I.arrow_l size={20}/></div>
          <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 8, padding: '8px 12px', background: t.surface, border: `1px solid ${accentA(t, 0.35)}`, borderRadius: t.radius }}>
            <I.search size={14} color={t.accent}/>
            <div style={{ flex: 1, fontFamily: t.fontMono, fontSize: 13, color: t.text }}>Studio Aria<span style={{ background: t.accent, marginLeft: 1, width: 1.5, display: 'inline-block', height: 14, verticalAlign: -2 }}/></div>
            <I.close size={14} color={t.textMuted}/>
          </div>
        </div>
        {/* filter scopes */}
        <div style={{ display: 'flex', gap: 4, marginTop: 10 }}>
          {[['All',218],['Scenes',164],['Studios',1],['Performers',8],['Tags',12]].map(([l, n], i) => (
            <div key={l} style={{
              padding: '5px 10px',
              background: i === 0 ? accentA(t, 0.12) : 'transparent',
              border: `1px solid ${i === 0 ? accentA(t, 0.3) : t.border}`,
              color: i === 0 ? t.accent : t.textDim,
              borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 11, fontWeight: 500,
              display: 'flex', alignItems: 'center', gap: 6,
            }}>{l}<span style={{ fontFamily: t.fontMono, fontSize: 9, color: t.textMuted }}>{n}</span></div>
          ))}
        </div>
      </div>
      {/* results */}
      <div style={{ flex: 1, overflowY: 'auto' }}>
        {/* Studio hit — top result */}
        <div style={{ padding: '14px 18px 6px' }}>
          <div style={{ fontFamily: t.fontMono, fontSize: 10, color: t.textMuted, letterSpacing: 1.4, textTransform: 'uppercase', fontWeight: 600, marginBottom: 8 }}>Top result</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: 12, background: t.surface, border: `1px solid ${accentA(t, 0.3)}`, borderRadius: t.radius }}>
            <div style={{ width: 56, height: 56, borderRadius: t.radius, overflow: 'hidden', position: 'relative' }}>
              <SceneArt seed="st1" palette="cool"/>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontFamily: t.fontBody, fontSize: 14, fontWeight: 600, color: t.text }}>
                <span style={{ background: accentA(t, 0.25), color: t.accent, padding: '0 2px', borderRadius: 2 }}>Studio Aria</span>
              </div>
              <div style={{ ...SP_meta(t, t.textMuted), marginTop: 3 }}>Studio · 84 scenes · added 2y ago</div>
            </div>
            <I.chev_r size={14} color={t.textMuted}/>
          </div>
        </div>
        {/* Scene hits */}
        <div style={{ padding: '18px 18px 6px' }}>
          <div style={{ fontFamily: t.fontMono, fontSize: 10, color: t.textMuted, letterSpacing: 1.4, textTransform: 'uppercase', fontWeight: 600, marginBottom: 8 }}>Scenes · 164</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {sceneHits.map(s => (
              <div key={s.id} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: 8, background: t.surface, borderRadius: t.radius, border: `1px solid ${t.border}` }}>
                <div style={{ width: 86, height: 50, borderRadius: 4, overflow: 'hidden', position: 'relative', flexShrink: 0 }}>
                  <SceneArt seed={s.id} palette="cool"/>
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontFamily: t.fontBody, fontSize: 13, color: t.text, fontWeight: 500 }}>{s.title}</div>
                  <div style={{ ...SP_meta(t, t.textMuted), marginTop: 2 }}>
                    <span style={{ background: accentA(t, 0.25), color: t.accent, padding: '0 2px', borderRadius: 2 }}>Studio Aria</span>
                    {' · '}{s.dur} · {s.res}
                  </div>
                </div>
              </div>
            ))}
            <div style={{ padding: '8px 12px', fontFamily: t.fontBody, fontSize: 12, color: t.accent, textAlign: 'center' }}>See all 164 scenes →</div>
          </div>
        </div>
        {/* Performers */}
        <div style={{ padding: '18px 18px 16px' }}>
          <div style={{ fontFamily: t.fontMono, fontSize: 10, color: t.textMuted, letterSpacing: 1.4, textTransform: 'uppercase', fontWeight: 600, marginBottom: 8 }}>Performers · 8</div>
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            {PERFORMERS.slice(0, 3).map(p => (
              <div key={p.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 10px 4px 4px', background: t.surface, borderRadius: 999, border: `1px solid ${t.border}` }}>
                <div style={{ width: 24, height: 24, borderRadius: '50%', overflow: 'hidden' }}>
                  <SceneArt seed={p.id} palette="cool"/>
                </div>
                <div style={{ fontFamily: t.fontBody, fontSize: 12, color: t.text }}>{p.name}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

// ─── More sheet ────────────────────────────────────────────────
const SpineMoreSheet = () => {
  const t = useTheme();
  return (
    <SheetShell title="More" height="62%">
      <div style={{ fontFamily: t.fontMono, fontSize: 10, color: t.textMuted, letterSpacing: 1.4, textTransform: 'uppercase', fontWeight: 600, margin: '6px 0 8px' }}>Browse</div>
      <div style={{ background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius, overflow: 'hidden', marginBottom: 18 }}>
        <MoreRow icon={<I.tag size={16}/>} label="Tags" sub="412 tags"/>
        <MoreRow icon={<I.marker size={16}/>} label="Markers" sub="2,108 markers across scenes"/>
        <MoreRow icon={<I.clock size={16}/>} label="History" sub="last 7 days · 38 plays"/>
      </div>
      <div style={{ fontFamily: t.fontMono, fontSize: 10, color: t.textMuted, letterSpacing: 1.4, textTransform: 'uppercase', fontWeight: 600, margin: '6px 0 8px' }}>App</div>
      <div style={{ background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius, overflow: 'hidden' }}>
        <MoreRow icon={<I.settings size={16}/>} label="Settings"/>
        <MoreRow icon={<I.grid size={16}/>} label="Customize nav bar" sub="Home, Library, Browse, Settings"/>
        <MoreRow icon={<I.cast size={16}/>} label="Cast" sub="Not connected"/>
      </div>
    </SheetShell>
  );
};
const MoreRow = ({ icon, label, sub }) => {
  const t = useTheme();
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '12px 14px', borderBottom: `1px solid ${t.border}` }}>
      <div style={{ color: t.accent }}>{icon}</div>
      <div style={{ flex: 1 }}>
        <div style={{ fontFamily: t.fontBody, fontSize: 13, color: t.text, fontWeight: 500 }}>{label}</div>
        {sub && <div style={{ ...SP_meta(t, t.textMuted), marginTop: 2 }}>{sub}</div>}
      </div>
      <I.chev_r size={14} color={t.textMuted}/>
    </div>
  );
};

// ─── Customize nav sheet ───────────────────────────────────────
const SpineCustomizeNavSheet = () => {
  const t = useTheme();
  const items = [
    { id: 'home', label: 'Home', icon: <I.home size={16}/>, checked: true, locked: false },
    { id: 'lib',  label: 'Library', icon: <I.film size={16}/>, checked: true },
    { id: 'browse', label: 'Browse', icon: <I.grid size={16}/>, checked: true },
    { id: 'settings', label: 'Settings', icon: <I.settings size={16}/>, checked: true },
    { id: 'studios', label: 'Studios', icon: <I.store size={16}/>, checked: false },
    { id: 'people', label: 'People', icon: <I.user size={16}/>, checked: false },
    { id: 'tags', label: 'Tags', icon: <I.tag size={16}/>, checked: false },
    { id: 'markers', label: 'Markers', icon: <I.marker size={16}/>, checked: false },
  ];
  return (
    <SheetShell title="Customize nav bar"
      height="76%"
      footer={
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button style={{ padding: '10px 18px', background: 'transparent', color: t.textDim, border: 'none', fontFamily: t.fontBody, fontSize: 12, fontWeight: 600 }}>Cancel</button>
          <button style={{ padding: '10px 20px', background: t.accent, color: t.accentInk, border: 0, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 12, fontWeight: 700 }}>Apply</button>
        </div>
      }>
      <div style={{ ...SP_meta(t, t.textDim), marginBottom: 14, padding: 12, background: accentA(t, 0.08), border: `1px solid ${accentA(t, 0.2)}`, borderRadius: t.radius }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
          <I.dot size={10} color={t.accent}/>
          <span>Pick up to 4 tabs. The rest live in the More sheet on the right.</span>
        </div>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
        {items.map(it => {
          const disabled = !it.checked && items.filter(i => i.checked).length >= 4;
          return (
            <div key={it.id} style={{
              display: 'flex', alignItems: 'center', gap: 14, padding: '11px 14px',
              background: t.surface, border: `1px solid ${it.checked ? accentA(t, 0.25) : t.border}`,
              borderRadius: t.radius, opacity: disabled ? 0.4 : 1,
            }}>
              <div style={{ color: it.checked ? t.accent : t.textMuted, display: 'flex' }}>{it.icon}</div>
              <div style={{ flex: 1, fontFamily: t.fontBody, fontSize: 13, color: t.text, fontWeight: 500 }}>{it.label}</div>
              <div style={{
                width: 18, height: 18, borderRadius: 4,
                background: it.checked ? t.accent : 'transparent',
                border: `1.5px solid ${it.checked ? t.accent : t.border}`,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                {it.checked && <I.check size={12} color={t.accentInk}/>}
              </div>
              <div style={{ color: t.textFaint, display: 'flex' }}><I.organize size={14}/></div>
            </div>
          );
        })}
      </div>
    </SheetShell>
  );
};

// ─── Marker editor sheet ───────────────────────────────────────
const SpineMarkerSheet = () => {
  const t = useTheme();
  const markers = [
    { time: '04:12', title: 'Cold open', tag: 'Intro', primary: 'Talking head' },
    { time: '18:40', title: 'Studio interview', tag: 'Talk' },
    { time: '42:55', title: 'B-roll · port', tag: 'Cutaway' },
    { time: '1:14:02', title: 'Closing monologue', tag: 'Outro' },
  ];
  return (
    <SheetShell title="Markers"
      height="84%"
      footer={
        <div style={{ display: 'flex', gap: 8 }}>
          <button style={{ flex: 1, padding: '11px', background: 'transparent', color: t.text, border: `1px solid ${t.border}`, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 12, fontWeight: 600, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}>
            <I.plus size={14}/> Add marker at current time (1:38:12)
          </button>
        </div>
      }>
      <div style={{ marginBottom: 16, padding: 12, background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius }}>
        {/* mini timeline */}
        <div style={{ position: 'relative', height: 24 }}>
          <div style={{ position: 'absolute', top: '50%', left: 0, right: 0, height: 3, background: t.surfaceHigh, borderRadius: 1.5, transform: 'translateY(-50%)' }}/>
          <div style={{ position: 'absolute', top: '50%', left: 0, width: '91%', height: 3, background: t.accent, borderRadius: 1.5, transform: 'translateY(-50%)' }}/>
          {[0.05, 0.21, 0.47, 0.78].map((m, i) => (
            <div key={i} style={{ position: 'absolute', left: `${m*100}%`, top: 0, bottom: 0, display: 'flex', alignItems: 'center', transform: 'translateX(-50%)' }}>
              <div style={{ width: 10, height: 10, borderRadius: '50%', background: t.warn, border: `2px solid ${t.bg}` }}/>
            </div>
          ))}
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4, ...SP_meta(t, t.textMuted) }}>
          <span>00:00</span><span>1:48:11</span>
        </div>
      </div>
      <div style={{ fontFamily: t.fontMono, fontSize: 10, color: t.textMuted, letterSpacing: 1.4, textTransform: 'uppercase', fontWeight: 600, marginBottom: 10 }}>4 markers · sorted by time</div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {markers.map((m, i) => (
          <div key={i} style={{
            display: 'flex', alignItems: 'center', gap: 10, padding: 10,
            background: i === 0 ? accentA(t, 0.06) : t.surface,
            border: `1px solid ${i === 0 ? accentA(t, 0.3) : t.border}`,
            borderRadius: t.radius,
          }}>
            <div style={{ width: 64, height: 36, borderRadius: 4, overflow: 'hidden', position: 'relative', flexShrink: 0 }}>
              <SceneArt seed={'mk'+i} palette="cool"/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontFamily: t.fontBody, fontSize: 13, color: t.text, fontWeight: 500 }}>{m.title}</div>
              <div style={{ ...SP_meta(t, t.accent), letterSpacing: 0.4 }}>{m.tag}{m.primary && <span style={{ color: t.textMuted }}> · {m.primary}</span>}</div>
            </div>
            <div style={{ ...SP_meta(t, t.text), fontFamily: t.fontMono, fontSize: 12 }}>{m.time}</div>
            <div style={{ color: t.textMuted, display: 'flex' }}><I.more size={14}/></div>
          </div>
        ))}
      </div>
    </SheetShell>
  );
};

// ─── Player settings sheet ─────────────────────────────────────
const SpinePlayerSettings = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', top: 0, right: 0, bottom: 0, width: 360, background: 'rgba(11,15,22,0.95)', backdropFilter: 'blur(20px)', borderLeft: `1px solid ${t.borderStrong}`, color: '#fff', fontFamily: t.fontBody, display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 16px', borderBottom: `1px solid ${t.border}` }}>
        <div style={{ fontFamily: t.fontBody, fontSize: 14, fontWeight: 600, letterSpacing: -0.2 }}>Playback</div>
        <div style={{ color: 'rgba(255,255,255,0.6)' }}><I.close size={14}/></div>
      </div>
      <div style={{ flex: 1, overflowY: 'auto', padding: '12px 16px' }}>
        <PSGroup title="Speed">
          <PSChips opts={['0.5×','0.75×','1×','1.25×','1.5×','2×']} active={2}/>
        </PSGroup>
        <PSGroup title="Audio · 2 tracks">
          <PSRadio opts={[
            { label: 'English · DTS-HD 5.1', sub: 'default', on: true },
            { label: 'Director\'s commentary · AAC stereo', sub: '', on: false },
          ]}/>
        </PSGroup>
        <PSGroup title="Subtitles · 3 tracks">
          <PSRadio opts={[
            { label: 'Off', on: false },
            { label: 'English · SDH', sub: 'embedded', on: true },
            { label: 'English · forced', sub: 'embedded', on: false },
            { label: 'Spanish', sub: 'external · .srt', on: false },
          ]}/>
        </PSGroup>
        <PSGroup title="Video">
          <PSRow k="Aspect ratio" v="Fit"/>
          <PSRow k="Decoder" v="HW · HEVC"/>
          <PSRow k="Pixel format" v="10-bit · YUV 4:2:0"/>
          <PSRow k="Frame rate" v="23.976 fps"/>
          <PSRow k="Color space" v="Rec. 709"/>
        </PSGroup>
        <PSGroup title="Up next · queue · 12 items">
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {[3,4,5].map((idx, i) => {
              const s = sceneById('s'+idx);
              return (
                <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: 6, borderRadius: t.radius, background: i === 0 ? accentA(t, 0.1) : 'rgba(255,255,255,0.04)', border: `1px solid ${i === 0 ? accentA(t, 0.3) : 'rgba(255,255,255,0.08)'}` }}>
                  <div style={{ width: 50, height: 28, borderRadius: 3, overflow: 'hidden', position: 'relative', flexShrink: 0 }}>
                    <SceneArt seed={s.id} palette="cool"/>
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontFamily: t.fontBody, fontSize: 11, color: '#fff', fontWeight: 500, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{s.title}</div>
                    <div style={{ fontFamily: t.fontMono, fontSize: 9, color: 'rgba(255,255,255,0.5)', letterSpacing: 0.4, marginTop: 1 }}>{s.studio} · {s.dur}</div>
                  </div>
                  {i === 0 && <div style={{ fontFamily: t.fontMono, fontSize: 9, color: t.accent, letterSpacing: 0.5 }}>NEXT</div>}
                </div>
              );
            })}
          </div>
        </PSGroup>
      </div>
    </div>
  );
};
const PSGroup = ({ title, children }) => {
  const t = useTheme();
  return (
    <div style={{ marginBottom: 18 }}>
      <div style={{ fontFamily: t.fontMono, fontSize: 9, color: 'rgba(255,255,255,0.5)', letterSpacing: 1.4, textTransform: 'uppercase', fontWeight: 600, marginBottom: 8 }}>{title}</div>
      {children}
    </div>
  );
};
const PSChips = ({ opts, active }) => {
  const t = useTheme();
  return (
    <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
      {opts.map((o, i) => (
        <div key={o} style={{
          padding: '5px 9px',
          background: i === active ? t.accent : 'rgba(255,255,255,0.06)',
          color: i === active ? t.accentInk : '#fff',
          border: `1px solid ${i === active ? t.accent : 'rgba(255,255,255,0.12)'}`,
          borderRadius: t.radius, fontFamily: t.fontMono, fontSize: 11, fontWeight: 500,
        }}>{o}</div>
      ))}
    </div>
  );
};
const PSRadio = ({ opts }) => {
  const t = useTheme();
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
      {opts.map((o, i) => (
        <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 10px', borderRadius: t.radius, background: o.on ? accentA(t, 0.12) : 'rgba(255,255,255,0.04)', border: `1px solid ${o.on ? accentA(t, 0.3) : 'rgba(255,255,255,0.08)'}` }}>
          <div style={{ width: 14, height: 14, borderRadius: '50%', border: `1.5px solid ${o.on ? t.accent : 'rgba(255,255,255,0.3)'}`, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            {o.on && <div style={{ width: 6, height: 6, borderRadius: '50%', background: t.accent }}/>}
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontFamily: t.fontBody, fontSize: 12, color: '#fff' }}>{o.label}</div>
            {o.sub && <div style={{ fontFamily: t.fontMono, fontSize: 9, color: 'rgba(255,255,255,0.5)', letterSpacing: 0.4, marginTop: 1 }}>{o.sub}</div>}
          </div>
        </div>
      ))}
    </div>
  );
};
const PSRow = ({ k, v }) => {
  const t = useTheme();
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: `1px solid rgba(255,255,255,0.05)`, fontFamily: t.fontBody, fontSize: 12 }}>
      <span style={{ color: 'rgba(255,255,255,0.6)' }}>{k}</span>
      <span style={{ color: t.accent, fontFamily: t.fontMono, fontSize: 11 }}>{v}</span>
    </div>
  );
};

// ─── Player wrapper with settings sheet open ───────────────────
const SpinePlayerWithSettings = ({ w = 820, h = 390 }) => {
  return (
    <div style={{ position: 'relative', width: w, height: h }}>
      <SpinePlayer w={w} h={h}/>
      {/* dim overlay */}
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.4)', borderRadius: 28, overflow: 'hidden' }}/>
      <div style={{ position: 'absolute', top: 6, right: 6, bottom: 6, width: 360, borderTopRightRadius: 24, borderBottomRightRadius: 24, overflow: 'hidden' }}>
        <SpinePlayerSettings/>
      </div>
    </div>
  );
};

Object.assign(window, {
  SpineFilterSheet, SpineSearchExpanded, SpineMoreSheet,
  SpineCustomizeNavSheet, SpineMarkerSheet, SpinePlayerSettings,
  SpinePlayerWithSettings,
});
