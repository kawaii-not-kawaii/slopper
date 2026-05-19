// spine-settings.jsx — Redesigned Settings for Spine
// Hub + drill-down pattern. Inline current values, compact controls,
// critical actions surfaced; cosmetic options nested.

// ─── Compact slider (thin track, value bubble) ─────────────────
const CSlider = ({ value, min = 0, max = 100, unit = '', step = 1, valueLabel }) => {
  const t = useTheme();
  const pct = Math.max(0, Math.min(1, (value - min) / (max - min)));
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '4px 0' }}>
      <div style={{ flex: 1, position: 'relative', height: 20, display: 'flex', alignItems: 'center' }}>
        <div style={{ width: '100%', height: 3, background: t.surfaceHigh, borderRadius: 1.5, position: 'relative' }}>
          <div style={{ position: 'absolute', left: 0, top: 0, bottom: 0, width: `${pct*100}%`, background: t.accent, borderRadius: 1.5 }}/>
          {/* tick marks at significant positions */}
          {[0.25, 0.5, 0.75].map(m => (
            <div key={m} style={{ position: 'absolute', left: `${m*100}%`, top: '50%', transform: 'translate(-50%, -50%)', width: 1, height: 7, background: t.textFaint }}/>
          ))}
        </div>
        <div style={{ position: 'absolute', left: `${pct*100}%`, transform: 'translateX(-50%)', width: 14, height: 14, borderRadius: '50%', background: t.accent, border: `2px solid ${t.bg}`, boxShadow: `0 0 0 1px ${t.accent}` }}/>
      </div>
      <div style={{
        minWidth: 60, padding: '4px 8px', textAlign: 'right',
        background: accentA(t, 0.08), border: `1px solid ${accentA(t, 0.25)}`, borderRadius: 4,
        fontFamily: t.fontMono, fontSize: 11, fontWeight: 600, color: t.accent, letterSpacing: 0.3,
      }}>{valueLabel || `${value}${unit}`}</div>
    </div>
  );
};

// ─── Range slider (two thumbs) ─────────────────────────────────
const CRange = ({ low, high, min = 0, max = 100, unit = '' }) => {
  const t = useTheme();
  const a = (low - min) / (max - min);
  const b = (high - min) / (max - min);
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <div style={{ flex: 1, position: 'relative', height: 20, display: 'flex', alignItems: 'center' }}>
        <div style={{ width: '100%', height: 3, background: t.surfaceHigh, borderRadius: 1.5, position: 'relative' }}>
          <div style={{ position: 'absolute', left: `${a*100}%`, right: `${(1-b)*100}%`, top: 0, bottom: 0, background: t.accent, borderRadius: 1.5 }}/>
        </div>
        {[a, b].map((p, i) => (
          <div key={i} style={{ position: 'absolute', left: `${p*100}%`, transform: 'translateX(-50%)', width: 14, height: 14, borderRadius: '50%', background: t.accent, border: `2px solid ${t.bg}`, boxShadow: `0 0 0 1px ${t.accent}` }}/>
        ))}
      </div>
      <div style={{
        minWidth: 72, padding: '4px 8px', textAlign: 'right',
        background: accentA(t, 0.08), border: `1px solid ${accentA(t, 0.25)}`, borderRadius: 4,
        fontFamily: t.fontMono, fontSize: 11, fontWeight: 600, color: t.accent,
      }}>{low}{unit}–{high}{unit}</div>
    </div>
  );
};

// ─── Switch ─────────────────────────────────────────────────
const CSwitch2 = ({ on }) => {
  const t = useTheme();
  return (
    <div style={{ width: 32, height: 18, borderRadius: 9, background: on ? t.accent : t.surfaceHigh, position: 'relative', border: `1px solid ${on ? t.accent : t.border}`, transition: '.15s', flexShrink: 0 }}>
      <div style={{ position: 'absolute', top: 1, left: on ? 14 : 1, width: 14, height: 14, borderRadius: '50%', background: on ? t.accentInk : t.textDim, transition: '.15s' }}/>
    </div>
  );
};

// ─── Chip row ───────────────────────────────────────────────
const CChips = ({ options, active = 0 }) => {
  const t = useTheme();
  return (
    <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
      {options.map((o, i) => (
        <div key={o} style={{
          padding: '6px 12px',
          background: i === active ? t.accent : 'transparent',
          color: i === active ? t.accentInk : t.text,
          border: `1px solid ${i === active ? t.accent : t.border}`,
          borderRadius: t.radius,
          fontFamily: t.fontMono, fontSize: 11, fontWeight: 500, letterSpacing: 0.3,
        }}>{o}</div>
      ))}
    </div>
  );
};

// ─── Top bar with optional back ─────────────────────────────
const SettingsTopBar = ({ title, back, action }) => {
  const t = useTheme();
  return (
    <div style={{ padding: '4px 18px 14px', display: 'flex', alignItems: 'center', gap: 10, borderBottom: `1px solid ${t.border}` }}>
      {back && (
        <div style={{ width: 30, height: 30, display: 'flex', alignItems: 'center', justifyContent: 'center', color: t.text, margin: '0 -4px 0 -6px' }}>
          <I.arrow_l size={18}/>
        </div>
      )}
      <div style={{ flex: 1, fontFamily: t.fontBody, fontSize: 18, fontWeight: 600, letterSpacing: -0.3, color: t.text }}>{title}</div>
      {action}
    </div>
  );
};

// ─── Settings HUB — landing page ────────────────────────────
const SpineSettingsHub = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <SettingsTopBar title="Settings" action={
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '5px 10px', background: t.surface, border: `1px solid ${t.border}`, borderRadius: 999, fontFamily: t.fontMono, fontSize: 10, color: t.textDim, letterSpacing: 0.5 }}>
          <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#5DBB63' }}/>
          Synced 2m ago
        </div>
      }/>
      <div style={{ flex: 1, overflowY: 'auto', paddingBottom: 100 }}>
        {/* Server status card — critical, always-visible */}
        <div style={{ padding: '14px 18px 6px' }}>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 14,
            padding: 14, background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radiusLg,
          }}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: accentA(t, 0.12), border: `1px solid ${accentA(t, 0.3)}`, display: 'flex', alignItems: 'center', justifyContent: 'center', color: t.accent }}>
              <I.cast size={18}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <div style={{ fontFamily: t.fontBody, fontSize: 14, color: t.text, fontWeight: 600 }}>media.local:9999</div>
                <div style={{ width: 6, height: 6, borderRadius: '50%', background: '#5DBB63' }}/>
              </div>
              <div style={{ ...SP_meta(t, t.textDim), marginTop: 2 }}>Stash v0.27.2 · 1,247 scenes · 8ms</div>
            </div>
            <I.chev_r size={14} color={t.textMuted}/>
          </div>
        </div>

        {/* Quick search */}
        <div style={{ padding: '14px 18px 0' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '9px 12px', background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius }}>
            <I.search size={14} color={t.textMuted}/>
            <div style={{ flex: 1, fontFamily: t.fontMono, fontSize: 12, color: t.textMuted }}>search settings…</div>
          </div>
        </div>

        {/* Category list */}
        <HubGroup>
          <HubRow
            icon={<I.play size={18}/>}
            label="Playback"
            value="1.0× · 10s seek · HW · Fit"
            hint="Speed, seek, resume, auto-play"
          />
          <HubRow
            icon={<I.film size={18}/>}
            label="Quality & codecs"
            value="Hardware · HEVC · 10-bit"
            hint="Decoder preference, buffer, codec capabilities"
            badge={{ label: 'OK', color: '#5DBB63' }}
          />
          <HubRow
            icon={<I.eye size={18}/>}
            label="Display"
            value="Sage · auto cols · AMOLED off"
            hint="Theme, grid, card chrome"
          />
          <HubRow
            icon={<I.bookmark size={18}/>}
            label="Library"
            value="Activity tracking · 256 MB cache"
            hint="Sync, history, downloads, cache"
          />
        </HubGroup>

        <HubGroup title="App">
          <HubRow icon={<I.grid size={18}/>}     label="Nav bar"        value="Home · Library · Browse · Settings"   hint="Customize bottom tabs"/>
          <HubRow icon={<I.cast size={18}/>}     label="Cast & Connect" value="No active session"                    hint="Chromecast, AirPlay, Stash sessions"/>
          <HubRow icon={<I.bolt size={18}/>}     label="About & diagnostics"  value="v1.0.4 · build 24f1b9c"        hint="Version, logs, licenses"/>
        </HubGroup>

        <HubGroup title="Danger zone">
          <HubRow
            icon={<I.unlock size={18}/>}
            label="Disconnect server"
            value=""
            hint="Clears credentials and returns to first-run"
            danger
          />
        </HubGroup>
      </div>
      <SBottomNav active="settings"/>
    </div>
  );
};

const HubGroup = ({ title, children }) => {
  const t = useTheme();
  return (
    <div style={{ padding: '20px 18px 0' }}>
      {title && (
        <div style={{ fontFamily: t.fontMono, fontSize: 10, color: t.textMuted, letterSpacing: 1.5, textTransform: 'uppercase', fontWeight: 600, marginBottom: 8 }}>{title}</div>
      )}
      <div style={{ background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius, overflow: 'hidden' }}>
        {children}
      </div>
    </div>
  );
};

const HubRow = ({ icon, label, value, hint, badge, danger }) => {
  const t = useTheme();
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12,
      padding: '12px 14px', borderBottom: `1px solid ${t.border}`,
      background: 'transparent',
    }}>
      <div style={{ width: 32, height: 32, borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center', background: danger ? 'rgba(255,88,96,0.08)' : t.surfaceHigh, color: danger ? t.err : t.textDim }}>{icon}</div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <div style={{ fontFamily: t.fontBody, fontSize: 13.5, color: danger ? t.err : t.text, fontWeight: 500 }}>{label}</div>
          {badge && <div style={{ padding: '0px 6px', background: 'transparent', border: `1px solid ${badge.color}`, color: badge.color, fontFamily: t.fontMono, fontSize: 9, letterSpacing: 0.8, borderRadius: 3, fontWeight: 600 }}>{badge.label}</div>}
        </div>
        {value && <div style={{ ...SP_meta(t, t.accent), marginTop: 2, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', fontFamily: t.fontMono }}>{value}</div>}
        {hint && <div style={{ fontFamily: t.fontBody, fontSize: 11, color: t.textMuted, marginTop: 3 }}>{hint}</div>}
      </div>
      <I.chev_r size={14} color={t.textMuted}/>
    </div>
  );
};

// ─── Playback detail ────────────────────────────────────────
const SpineSettingsPlayback = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <SettingsTopBar title="Playback" back/>
      <div style={{ flex: 1, overflowY: 'auto', padding: '4px 18px 100px' }}>
        <DetailGroup title="Defaults">
          <DRow k="Default speed" body={<CChips options={['0.5×','0.75×','1×','1.25×','1.5×','2×']} active={2}/>}/>
          <DRow k="Aspect ratio"  body={<CChips options={['Fit','Crop','Stretch']} active={0}/>}/>
          <DRow k="Auto-play next" sub="Advance to the next item in the queue" trailing={<CSwitch2 on/>}/>
          <DRow k="Auto-rotate on play" sub="Sensor landscape when entering player" trailing={<CSwitch2 on/>}/>
        </DetailGroup>

        <DetailGroup title="Seeking">
          <DRow k="Double-tap seek" sub="Step amount when double-tapping screen edges" trailing={null}
            body={<CSlider value={10} min={5} max={60} unit="s" valueLabel="10 sec"/>}/>
          <DRow k="Scrub sensitivity" sub="How far a horizontal drag moves the playhead" trailing={null}
            body={<CSlider value={120} min={50} max={300} unit=" ms/px" valueLabel="120 ms/px"/>}/>
          <DRow k="Show chapter thumbnails" sub="Preview chapter content while scrubbing" trailing={<CSwitch2 on/>}/>
        </DetailGroup>

        <DetailGroup title="Resume & skip">
          <DRow k="Resume threshold" sub="Resume only if more than this watched" trailing={null}
            body={<CSlider value={2} min={0} max={30} unit="s" valueLabel="2 sec"/>}/>
          <DRow k="Completion threshold" sub="Mark watched at this percentage" trailing={null}
            body={<CSlider value={85} min={50} max={100} unit="%" valueLabel="85%"/>}/>
          <DRow k="Skip intro" sub="Jump forward this many seconds at start of playback" trailing={null}
            body={<CSlider value={0} min={0} max={120} unit="s" valueLabel="Off"/>}/>
        </DetailGroup>

        <DetailGroup title="Player chrome" badge="Power user">
          <DRow k="Lock controls on idle" sub="Auto-hide UI after 3s" trailing={<CSwitch2 on/>}/>
          <DRow k="Show codec / bitrate badge" trailing={<CSwitch2 on/>}/>
          <DRow k="Show queue position" trailing={<CSwitch2 on/>}/>
          <DRow k="Haptics on seek" trailing={<CSwitch2/>}/>
        </DetailGroup>
      </div>
    </div>
  );
};

// ─── Quality & codecs detail ────────────────────────────────
const SpineSettingsCodecs = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <SettingsTopBar title="Quality & codecs" back/>
      <div style={{ flex: 1, overflowY: 'auto', padding: '4px 18px 100px' }}>
        {/* Status banner */}
        <div style={{ marginTop: 14, padding: 14, background: accentA(t, 0.06), border: `1px solid ${accentA(t, 0.25)}`, borderRadius: t.radius, display: 'flex', gap: 12, alignItems: 'flex-start' }}>
          <div style={{ width: 24, height: 24, borderRadius: 6, background: accentA(t, 0.2), display: 'flex', alignItems: 'center', justifyContent: 'center', color: t.accent }}>
            <I.check size={14}/>
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontFamily: t.fontBody, fontSize: 13, color: t.text, fontWeight: 600 }}>Full codec support</div>
            <div style={{ ...SP_meta(t, t.textDim), marginTop: 3 }}>HW · HEVC · AV1 · H264 · FFmpeg extension loaded · 10-bit YUV 4:2:0 OK</div>
          </div>
        </div>

        <DetailGroup title="Decoder">
          <DRow k="Preference" body={<CChips options={['Auto','Prefer HW','Prefer SW']} active={1}/>} sub="Auto chooses based on codec support"/>
          <DRow k="Fallback on failure" sub="Retry with software decoder if hardware can't play" trailing={<CSwitch2 on/>}/>
          <DRow k="Tunneling" sub="Direct decoder → display path · saves power" trailing={<CSwitch2 on/>}/>
        </DetailGroup>

        <DetailGroup title="Buffer">
          <DRow k="Buffer size" body={<CChips options={['Small · 15s','Medium · 50s','Large · 2min']} active={1}/>}/>
          <DRow k="Pre-buffer on hover" sub="Start fetching when you focus a card" trailing={<CSwitch2/>}/>
        </DetailGroup>

        <DetailGroup title="Display & HDR">
          <DRow k="HDR passthrough" sub="HDR10 · Dolby Vision · HLG" trailing={<CSwitch2 on/>}/>
          <DRow k="Match refresh rate" sub="Switch to source fps when available (23.976 / 24 / 30 / 60)" trailing={<CSwitch2 on/>}/>
          <DRow k="Match resolution" sub="Switch to 4K when source is 4K" trailing={<CSwitch2/>}/>
        </DetailGroup>

        <DetailGroup title="Diagnostics">
          <DRow k="Test codec" body={<TestBtn label="Run codec test"/>} sub="Plays a 5s sample at each format"/>
          <DRow k="Open codec details" trailing={<I.chev_r size={14} color={t.textMuted}/>}/>
        </DetailGroup>
      </div>
    </div>
  );
};

// ─── Display detail (with palette switch!) ──────────────────
const SpineSettingsDisplay = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <SettingsTopBar title="Display" back/>
      <div style={{ flex: 1, overflowY: 'auto', padding: '4px 18px 100px' }}>
        <DetailGroup title="Theme">
          {/* Palette picker — visual swatches */}
          <div style={{ padding: 12 }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8 }}>
              {[
                { id: 'sage', name: 'Sage', accent: '#9DC83C', desc: 'Moss · default' },
                { id: 'amber', name: 'Ember', accent: '#E5A742', desc: 'Warm projector' },
                { id: 'cyan', name: 'Signal', accent: '#4FD0E6', desc: 'Cool cyan' },
              ].map((p, i) => (
                <div key={p.id} style={{
                  padding: 10, borderRadius: t.radius,
                  background: i === 0 ? accentA(t, 0.08) : t.bg,
                  border: `1px solid ${i === 0 ? t.accent : t.border}`,
                  position: 'relative',
                }}>
                  <div style={{ width: 28, height: 28, borderRadius: 6, background: p.accent, marginBottom: 8 }}/>
                  <div style={{ fontFamily: t.fontBody, fontSize: 11, fontWeight: 600, color: t.text }}>{p.name}</div>
                  <div style={{ fontFamily: t.fontMono, fontSize: 9, color: t.textMuted, marginTop: 2, letterSpacing: 0.3 }}>{p.desc}</div>
                  {i === 0 && (
                    <div style={{ position: 'absolute', top: 6, right: 6, width: 14, height: 14, borderRadius: '50%', background: t.accent, color: t.accentInk, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <I.check size={9}/>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
          <DivLine/>
          <DRow k="AMOLED black mode" sub="Pure black scaffold for OLED panels" trailing={<CSwitch2/>}/>
          <DRow k="Reduce motion" sub="Disable card-hover and transition animations" trailing={<CSwitch2/>}/>
        </DetailGroup>

        <DetailGroup title="Library layout">
          <DRow k="Grid columns" body={<CChips options={['Auto','2','3','4']} active={0}/>}/>
          <DRow k="Card density" body={<CChips options={['Compact','Comfortable','Spacious']} active={1}/>}/>
          <DRow k="Long-press behavior" body={<CChips options={['Play queue','Quick menu','Off']} active={0}/>}/>
        </DetailGroup>

        <DetailGroup title="Card chrome">
          <DRow k="Rating on cards" trailing={<CSwitch2 on/>}/>
          <DRow k="Play count" trailing={<CSwitch2 on/>}/>
          <DRow k="Resolution badge" trailing={<CSwitch2 on/>}/>
          <DRow k="Resume bar"      sub="Thin accent bar showing watched progress" trailing={<CSwitch2 on/>}/>
          <DRow k="Studio caption"  sub="Studio name beneath the title" trailing={<CSwitch2 on/>}/>
        </DetailGroup>

        <DetailGroup title="Player">
          <DRow k="Show chapter strip" sub="Above the timeline · proportional segments" trailing={<CSwitch2 on/>}/>
          <DRow k="Tap to peek info" sub="Single-tap reveals title + meta for 3s" trailing={<CSwitch2/>}/>
        </DetailGroup>
      </div>
    </div>
  );
};

// ─── Library detail ─────────────────────────────────────────
const SpineSettingsLibrary = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <SettingsTopBar title="Library" back/>
      <div style={{ flex: 1, overflowY: 'auto', padding: '4px 18px 100px' }}>
        <DetailGroup title="Sync with Stash">
          <DRow k="Activity tracking"      sub="Report resume position + play count to Stash" trailing={<CSwitch2 on/>}/>
          <DRow k="Sync ratings"           sub="When you tap stars, write back to Stash" trailing={<CSwitch2 on/>}/>
          <DRow k="Sync O-counter"         trailing={<CSwitch2 on/>}/>
          <DRow k="Sync markers"           sub="Markers you add here appear in Stash web UI" trailing={<CSwitch2 on/>}/>
        </DetailGroup>

        <DetailGroup title="Cache" footer={<TestBtn label="Clear image cache · 184 MB used"/>}>
          <DRow k="Image cache" body={<CSlider value={256} min={64} max={512} unit=" MB" valueLabel="256 MB"/>}
            sub="Larger cache = fewer thumbnail fetches"/>
          <DRow k="Cache duration" body={<CChips options={['1 day','1 week','30 days','Forever']} active={2}/>}/>
        </DetailGroup>

        <DetailGroup title="History">
          <DRow k="Keep watch history"   sub="Remember scenes you've played" trailing={<CSwitch2 on/>}/>
          <DRow k="History on Home"      sub="Show 'Continue Watching' rail" trailing={<CSwitch2 on/>}/>
          <DRow k="Smart rails"          sub="Auto-generate Tonight / For You rails" trailing={<CSwitch2 on/>}/>
        </DetailGroup>

        <DetailGroup title="Downloads · offline" badge="Beta">
          <DRow k="Allow downloads"     sub="Save scenes for offline playback" trailing={<CSwitch2/>}/>
          <DRow k="Storage limit"       body={<CChips options={['2 GB','5 GB','10 GB','No limit']} active={1}/>}/>
          <DRow k="Auto-purge watched"  sub="Delete downloaded scenes after playback" trailing={<CSwitch2/>}/>
        </DetailGroup>
      </div>
    </div>
  );
};

// ─── Server detail ──────────────────────────────────────────
const SpineSettingsServer = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <SettingsTopBar title="Server" back action={
        <button style={{ padding: '6px 10px', background: t.surface, color: t.text, border: `1px solid ${t.border}`, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 11, fontWeight: 600 }}>Switch</button>
      }/>
      <div style={{ flex: 1, overflowY: 'auto', padding: '4px 18px 100px' }}>
        {/* Big status block */}
        <div style={{ marginTop: 14, padding: 16, background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radiusLg }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
            <div style={{ width: 8, height: 8, borderRadius: '50%', background: '#5DBB63' }}/>
            <div style={{ fontFamily: t.fontMono, fontSize: 10, color: '#5DBB63', letterSpacing: 1.4, textTransform: 'uppercase', fontWeight: 700 }}>Connected</div>
            <div style={{ flex: 1 }}/>
            <div style={{ ...SP_meta(t, t.textMuted) }}>last sync 2m ago</div>
          </div>
          <div style={{ fontFamily: t.fontMono, fontSize: 14, color: t.text, fontWeight: 500, marginBottom: 4, wordBreak: 'break-all' }}>http://media.local:9999</div>
          <div style={{ ...SP_meta(t, t.textDim) }}>Stash <span style={{ color: t.text }}>v0.27.2</span> · TLS off · API key configured</div>
          {/* Library stats */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 1, background: t.border, marginTop: 14, borderRadius: t.radius, overflow: 'hidden', border: `1px solid ${t.border}` }}>
            {[['Scenes','1,247'],['Studios','86'],['Performers','312'],['Tags','412']].map(([k,v]) => (
              <div key={k} style={{ padding: '8px 10px', background: t.bg }}>
                <div style={{ ...SP_meta(t, t.textMuted), letterSpacing: 0.5 }}>{k}</div>
                <div style={{ fontFamily: t.fontMono, fontSize: 13, color: t.text, fontWeight: 600, marginTop: 2 }}>{v}</div>
              </div>
            ))}
          </div>
        </div>

        <DetailGroup title="Network">
          <DRow k="Endpoint" v="media.local:9999"/>
          <DRow k="Latency"  v="8 ms" trailing={null}/>
          <DRow k="TLS"      v="Off · local network"/>
          <DRow k="API key"  v="Configured · sha1:7a8d…" trailing={<button style={{ padding: '4px 10px', background: 'transparent', color: t.accent, border: `1px solid ${accentA(t, 0.3)}`, borderRadius: 4, fontFamily: t.fontBody, fontSize: 11, fontWeight: 600 }}>Replace</button>}/>
        </DetailGroup>

        <DetailGroup title="Actions">
          <DRow k="Refresh library now"  trailing={<I.chev_r size={14} color={t.textMuted}/>} sub="Re-fetch metadata for all scenes"/>
          <DRow k="Trigger server scan"  trailing={<I.chev_r size={14} color={t.textMuted}/>} sub="Ask Stash to scan its watched folders"/>
          <DRow k="Edit connection"      trailing={<I.chev_r size={14} color={t.textMuted}/>}/>
        </DetailGroup>

        <DetailGroup title="Danger zone">
          <DRow k="Disconnect server" sub="Clears credentials and returns to first-run" danger
            trailing={<I.chev_r size={14} color={t.err}/>}/>
        </DetailGroup>
      </div>
    </div>
  );
};

// ─── About / diagnostics ────────────────────────────────────
const SpineSettingsAbout = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <SettingsTopBar title="About & diagnostics" back/>
      <div style={{ flex: 1, overflowY: 'auto', padding: '4px 18px 100px' }}>
        {/* Version block */}
        <div style={{ padding: '28px 0 18px', textAlign: 'center' }}>
          <div style={{ width: 56, height: 56, position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 14px' }}>
            <div style={{ width: 42, height: 42, background: t.accent, borderRadius: 6, transform: 'rotate(45deg)' }}/>
            <div style={{ position: 'absolute', width: 16, height: 16, background: t.bg, borderRadius: 2 }}/>
          </div>
          <div style={{ fontFamily: t.fontBody, fontSize: 22, fontWeight: 700, color: t.text, letterSpacing: -0.4 }}>Slopper</div>
          <div style={{ ...SP_meta(t, t.textDim), marginTop: 4, fontFamily: t.fontMono }}>v1.0.4 · build 24f1b9c · May 18 2026</div>
        </div>

        <DetailGroup title="Capabilities">
          <DRow k="Codec support"      v="Full · HW + FFmpeg" valueColor="#5DBB63"/>
          <DRow k="Hardware decoder"   v="Mediatek MTK MT6985"/>
          <DRow k="OpenGL"             v="ES 3.2 · Vulkan 1.3"/>
          <DRow k="HDR"                v="HDR10 · HLG · DolbyVision"/>
          <DRow k="Display"            v="120 Hz · 2400×1080"/>
        </DetailGroup>

        <DetailGroup title="Storage">
          <DRow k="Image cache"        v="184 MB / 256 MB"/>
          <DRow k="Downloads"          v="0 B"/>
          <DRow k="Database"           v="4.2 MB"/>
        </DetailGroup>

        <DetailGroup title="Diagnostics">
          <DRow k="View logs"          trailing={<I.chev_r size={14} color={t.textMuted}/>}/>
          <DRow k="Run network test"   body={<TestBtn label="Run"/>} sub="Latency, throughput, TLS handshake"/>
          <DRow k="Send debug report"  trailing={<I.chev_r size={14} color={t.textMuted}/>} sub="Anonymized · sent to maintainer"/>
        </DetailGroup>

        <DetailGroup title="Legal">
          <DRow k="Open-source licenses" trailing={<I.chev_r size={14} color={t.textMuted}/>}/>
          <DRow k="Privacy policy"       trailing={<I.chev_r size={14} color={t.textMuted}/>}/>
          <DRow k="Built on"             v="Stash · ExoPlayer · Compose"/>
        </DetailGroup>
      </div>
    </div>
  );
};

// ─── Reusable detail-page primitives ────────────────────────
const DetailGroup = ({ title, badge, footer, children }) => {
  const t = useTheme();
  return (
    <div style={{ marginTop: 18 }}>
      {title && (
        <div style={{ padding: '0 2px 8px', display: 'flex', alignItems: 'baseline', gap: 8 }}>
          <div style={{ fontFamily: t.fontMono, fontSize: 10, color: t.textMuted, letterSpacing: 1.5, textTransform: 'uppercase', fontWeight: 600 }}>{title}</div>
          {badge && <div style={{ ...SP_meta(t, t.accent), padding: '1px 5px', background: accentA(t, 0.1), border: `1px solid ${accentA(t, 0.25)}`, borderRadius: 3, textTransform: 'uppercase', fontWeight: 600 }}>{badge}</div>}
        </div>
      )}
      <div style={{ background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius, overflow: 'hidden' }}>
        {children}
        {footer && <div style={{ padding: 12, borderTop: `1px solid ${t.border}` }}>{footer}</div>}
      </div>
    </div>
  );
};

// Row used inside DetailGroup. Two modes:
//   trailing-only: simple "k → value/switch/chev" row
//   body:          k+sub headline on top, full-width body (slider, chips) below
const DRow = ({ k, sub, v, body, trailing, danger, valueColor }) => {
  const t = useTheme();
  const hasBody = !!body;
  return (
    <div style={{
      padding: hasBody ? '12px 14px' : '11px 14px',
      borderBottom: `1px solid ${t.border}`,
      display: 'flex', flexDirection: hasBody ? 'column' : 'row',
      gap: hasBody ? 10 : 12, alignItems: hasBody ? 'stretch' : 'center',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, width: '100%' }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontFamily: t.fontBody, fontSize: 13, color: danger ? t.err : t.text, fontWeight: 500 }}>{k}</div>
          {sub && <div style={{ fontFamily: t.fontBody, fontSize: 11, color: t.textMuted, marginTop: 2, lineHeight: 1.4 }}>{sub}</div>}
        </div>
        {v && <span style={{ fontFamily: t.fontMono, fontSize: 11, color: valueColor || t.accent, letterSpacing: 0.3 }}>{v}</span>}
        {trailing !== undefined ? trailing : (v ? null : <I.chev_r size={14} color={t.textMuted}/>)}
      </div>
      {body && <div>{body}</div>}
    </div>
  );
};

const DivLine = () => {
  const t = useTheme();
  return <div style={{ height: 1, background: t.border }}/>;
};

const TestBtn = ({ label }) => {
  const t = useTheme();
  return (
    <button style={{ width: '100%', padding: '10px', background: 'transparent', color: t.text, border: `1px solid ${t.border}`, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 12, fontWeight: 600, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}>
      <I.bolt size={13} color={t.accent}/>{label}
    </button>
  );
};

// ─── Search overlay on settings (the smart bit) ─────────────
const SpineSettingsSearch = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '4px 18px 14px', borderBottom: `1px solid ${t.border}` }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{ width: 30, height: 30, display: 'flex', alignItems: 'center', justifyContent: 'center', color: t.text }}><I.arrow_l size={18}/></div>
          <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 8, padding: '8px 12px', background: t.surface, border: `1px solid ${accentA(t, 0.35)}`, borderRadius: t.radius }}>
            <I.search size={14} color={t.accent}/>
            <div style={{ flex: 1, fontFamily: t.fontMono, fontSize: 13, color: t.text }}>resume<span style={{ background: t.accent, marginLeft: 1, width: 1.5, display: 'inline-block', height: 14, verticalAlign: -2 }}/></div>
          </div>
        </div>
      </div>
      <div style={{ flex: 1, overflowY: 'auto', padding: '14px 18px' }}>
        <div style={{ fontFamily: t.fontMono, fontSize: 10, color: t.textMuted, letterSpacing: 1.4, textTransform: 'uppercase', fontWeight: 600, marginBottom: 8 }}>3 matches</div>
        <div style={{ background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius, overflow: 'hidden' }}>
          <SearchHit
            crumb="Playback · Resume & skip"
            label={<>Re<span style={{ background: accentA(t, 0.25), color: t.accent }}>sume</span> threshold</>}
            value="2 sec"
            hint="Resume only if more than this watched"
          />
          <SearchHit
            crumb="Playback · Resume & skip"
            label={<>Completion threshold</>}
            value="85%"
            hint="Mark watched at this percentage"
          />
          <SearchHit
            crumb="Display · Card chrome"
            label={<>Re<span style={{ background: accentA(t, 0.25), color: t.accent }}>sume</span> bar</>}
            value="On"
            hint="Thin accent bar showing watched progress"
          />
        </div>
        <div style={{ marginTop: 18, ...SP_meta(t, t.textMuted), padding: 12, background: t.surface, borderRadius: t.radius, border: `1px dashed ${t.border}` }}>
          ▸ Tip · use the home indicator to swipe back, or tap a result to jump to the setting (it'll highlight briefly when you land).
        </div>
      </div>
    </div>
  );
};
const SearchHit = ({ crumb, label, value, hint }) => {
  const t = useTheme();
  return (
    <div style={{ padding: 12, borderBottom: `1px solid ${t.border}` }}>
      <div style={{ ...SP_meta(t, t.textMuted), letterSpacing: 0.4, marginBottom: 4 }}>{crumb}</div>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, marginBottom: 4 }}>
        <div style={{ flex: 1, fontFamily: t.fontBody, fontSize: 13, color: t.text, fontWeight: 500 }}>{label}</div>
        <span style={{ fontFamily: t.fontMono, fontSize: 11, color: t.accent }}>{value}</span>
      </div>
      <div style={{ fontFamily: t.fontBody, fontSize: 11, color: t.textMuted, lineHeight: 1.4 }}>{hint}</div>
    </div>
  );
};

Object.assign(window, {
  SpineSettingsHub, SpineSettingsPlayback, SpineSettingsCodecs,
  SpineSettingsDisplay, SpineSettingsLibrary, SpineSettingsServer,
  SpineSettingsAbout, SpineSettingsSearch,
});
