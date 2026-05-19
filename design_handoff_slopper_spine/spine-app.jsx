// spine-app.jsx — Slopper Spine page: palette switcher + every screen, flow, state

// Bump on every published revision. Surface in the header badge + handoff CHANGELOG.
const SLOPPER_VERSION = '3.0';
const SLOPPER_DATE    = 'May 19, 2026';

const CHANGELOG = [
  {
    v: '3.0', date: 'May 19, 2026', headline: 'Settings redesign',
    items: [
      'Replaced single-strip Settings with hub + drill-down pattern',
      '7 focused detail pages: Playback, Quality & codecs, Display, Library, Server, About, Search',
      'Compact horizontal sliders w/ mono value bubble (was: dot-row sliders ~80dp tall)',
      'Server status surfaced as a card at top of hub (was: bottom of strip)',
      '"Disconnect server" moved to its own Danger zone group',
      'Palette picker on Display as visual swatch cards (Sage / Ember / Signal)',
      'Settings search w/ breadcrumb results from anywhere in hierarchy',
      'New: Quality & codecs page with HDR / refresh-match / codec test',
      'New: About & diagnostics page with build hash, logs, debug-report',
    ],
  },
  {
    v: '2.0', date: 'May 18, 2026', headline: 'Spine deep-dive',
    items: [
      'Committed Spine as the chosen direction',
      'Toned-down "Sage" lime replaces electric lime',
      'Two alternate palettes: Ember (amber) + Signal (cyan)',
      'Filter sheet · Search expanded · More sheet · Customize-nav sheet · Marker editor sheet · Player settings panel',
      'Real-content states: loading skeleton, empty/scanning, no-results, server error, wrong-server, unscraped',
    ],
  },
  {
    v: '1.0', date: 'May 18, 2026', headline: 'Three-direction exploration',
    items: [
      'Reel (editorial cinema-house) · Spine (Linear-tight) · Cinema (Apple-TV scale)',
      'All 6 core screens per direction + landscape Player',
      'TV (10-foot) layouts for each direction',
      'Side-by-side comparison views',
    ],
  },
];

const PHONE_W = 390;
const PHONE_H = 820;
const PLAYER_W = 820;
const PLAYER_H = 390;

// PhoneShell wrapped in the active Spine theme
const Phone = ({ paletteKey, children }) => {
  const theme = makeSpineTheme(paletteKey);
  return (
    <ThemeProvider theme={theme}>
      <PhoneShell w={PHONE_W} h={PHONE_H} theme={theme}>
        {children}
      </PhoneShell>
    </ThemeProvider>
  );
};
const PlayerBoard = ({ paletteKey, children }) => {
  const theme = makeSpineTheme(paletteKey);
  return <ThemeProvider theme={theme}>{children}</ThemeProvider>;
};

// ─── Page-level state: which palette is active ──────────────────
const PaletteCtx = React.createContext('sage');
const usePalette = () => React.useContext(PaletteCtx);

// ─── Top header with palette switcher ───────────────────────────
const Header = ({ palette, onPalette }) => {
  const theme = makeSpineTheme(palette);
  const p = SPINE_PALETTES[palette];
  return (
    <div style={{
      position: 'sticky', top: 0, zIndex: 50,
      background: 'rgba(10,13,18,0.85)', backdropFilter: 'blur(20px)',
      borderBottom: `1px solid ${theme.border}`,
      padding: '14px 32px',
      display: 'flex', alignItems: 'center', gap: 24,
    }}>
      {/* Logo */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <div style={{ width: 24, height: 24, position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ width: 18, height: 18, background: theme.accent, borderRadius: 3, transform: 'rotate(45deg)' }}/>
          <div style={{ position: 'absolute', width: 7, height: 7, background: theme.bg, borderRadius: 1.5 }}/>
        </div>
        <div style={{ fontSize: 17, fontWeight: 700, letterSpacing: -0.4 }}>Slopper</div>
        <a href="#changelog" style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 10, fontFamily: 'JetBrains Mono, monospace', letterSpacing: 1.5, color: theme.accent, textTransform: 'uppercase', padding: '2px 8px', background: theme.surface, border: `1px solid ${theme.border}`, borderRadius: 999, fontWeight: 700, textDecoration: 'none' }}>
          <span style={{ width: 5, height: 5, borderRadius: '50%', background: theme.accent }}/>
          SPINE · v{SLOPPER_VERSION}
        </a>
      </div>
      <div style={{ flex: 1 }}/>
      {/* Section anchors */}
      <div style={{ display: 'flex', gap: 4 }}>
        {[
          { id: 'core', label: 'Core screens' },
          { id: 'flows', label: 'Flows' },
          { id: 'states', label: 'States' },
          { id: 'settings', label: 'Settings ◆ new' },
          { id: 'changelog', label: 'Changelog' },
        ].map(a => (
          <a key={a.id} href={`#${a.id}`} style={{ padding: '6px 12px', fontFamily: 'JetBrains Mono, monospace', fontSize: 10, fontWeight: 600, letterSpacing: 1.4, textTransform: 'uppercase', color: theme.textDim, textDecoration: 'none', borderRadius: 4, border: '1px solid transparent' }}>{a.label}</a>
        ))}
      </div>
      {/* Palette switcher */}
      <div style={{ display: 'flex', gap: 6, padding: 4, background: theme.surface, border: `1px solid ${theme.border}`, borderRadius: 8 }}>
        {Object.entries(SPINE_PALETTES).map(([key, info]) => {
          const active = palette === key;
          return (
            <button key={key} onClick={() => onPalette(key)} style={{
              display: 'flex', alignItems: 'center', gap: 8,
              padding: '6px 11px', border: 'none', cursor: 'pointer',
              background: active ? theme.surfaceHigh : 'transparent',
              borderRadius: 5,
              fontFamily: 'Space Grotesk', fontSize: 12, fontWeight: 600,
              color: active ? theme.text : theme.textDim,
              letterSpacing: -0.1,
            }}>
              <div style={{ width: 14, height: 14, borderRadius: 3, background: info.accent }}/>
              {info.name}
            </button>
          );
        })}
      </div>
    </div>
  );
};

// ─── Section helper ─────────────────────────────────────────────
const Section = ({ id, num, title, sub, palette, children }) => {
  const theme = makeSpineTheme(palette);
  return (
    <div id={id} style={{ padding: '64px 32px 24px', borderTop: `1px solid ${theme.border}` }}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 18, marginBottom: 28, maxWidth: 980 }}>
        <div style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: 12, color: theme.textMuted, letterSpacing: 2 }}>{String(num).padStart(2, '0')}</div>
        <div>
          <div style={{ fontSize: 32, fontWeight: 600, letterSpacing: -0.8, lineHeight: 1.1 }}>{title}</div>
          {sub && <div style={{ marginTop: 6, fontSize: 14, color: theme.textDim, lineHeight: 1.5, maxWidth: 720 }}>{sub}</div>}
        </div>
      </div>
      {children}
    </div>
  );
};

// ─── Single artboard with caption ──────────────────────────────
const Board = ({ w, h, label, caption, children }) => {
  const palette = usePalette();
  const theme = makeSpineTheme(palette);
  return (
    <div style={{ flexShrink: 0 }}>
      <div style={{ width: w, height: h, position: 'relative', isolation: 'isolate', borderRadius: 36, overflow: 'visible' }}>
        {children}
      </div>
      <div style={{ marginTop: 14, maxWidth: w }}>
        <div style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: 10, color: theme.accent, letterSpacing: 1.6, textTransform: 'uppercase', fontWeight: 700, marginBottom: 4 }}>{label}</div>
        {caption && <div style={{ fontSize: 12, color: theme.textDim, lineHeight: 1.5 }}>{caption}</div>}
      </div>
    </div>
  );
};

// ─── Brief paragraph at top of page ─────────────────────────────
const Brief = ({ palette }) => {
  const theme = makeSpineTheme(palette);
  return (
    <div style={{ padding: '48px 32px 24px', maxWidth: 1080 }}>
      <div style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: 11, color: theme.accent, letterSpacing: 2, textTransform: 'uppercase', fontWeight: 600, marginBottom: 14 }}>Direction · committed</div>
      <div style={{ fontSize: 48, fontWeight: 600, letterSpacing: -1.4, lineHeight: 1, marginBottom: 18 }}>
        Spine.
      </div>
      <div style={{ fontSize: 16, lineHeight: 1.6, color: theme.textDim, maxWidth: 720, marginBottom: 28 }}>
        Linear-tight. Space Grotesk throughout, JetBrains Mono for metadata. Cool charcoal surfaces, info-rich rails, side-panel detail. The accent does heavy lifting — switch it above to try three calibrations.
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 14, maxWidth: 880 }}>
        {Object.entries(SPINE_PALETTES).map(([key, p]) => (
          <div key={key} style={{ padding: 16, background: theme.surface, border: `1px solid ${palette === key ? p.accent : theme.border}`, borderRadius: 6, transition: '.2s' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
              <div style={{ width: 28, height: 28, borderRadius: 4, background: p.accent, boxShadow: `0 0 0 1px ${theme.border}` }}/>
              <div>
                <div style={{ fontSize: 14, fontWeight: 700, letterSpacing: -0.2 }}>{p.name}</div>
                <div style={{ fontFamily: 'JetBrains Mono', fontSize: 10, color: theme.textMuted, letterSpacing: 0.8 }}>{p.accent.toUpperCase()}</div>
              </div>
            </div>
            <div style={{ fontSize: 12, color: theme.textDim, lineHeight: 1.5 }}>{p.note}</div>
          </div>
        ))}
      </div>
    </div>
  );
};

// ─── Row component (horizontal scroll) ──────────────────────────
const Row = ({ children, gap = 32 }) => (
  <div style={{ display: 'flex', gap, overflowX: 'auto', padding: '0 0 32px', alignItems: 'flex-start', flexWrap: 'wrap' }}>
    {children}
  </div>
);
const ScrollRow = ({ children, gap = 32 }) => (
  <div style={{ display: 'flex', gap, overflowX: 'auto', padding: '0 0 32px', alignItems: 'flex-start' }}>
    {children}
  </div>
);

// ─── Composite: phone with sheet overlay ────────────────────────
const PhoneWithSheet = ({ paletteKey, base, sheet }) => (
  <Phone paletteKey={paletteKey}>
    {base}
    {sheet}
  </Phone>
);

// ─── App ─────────────────────────────────────────────────────────
const App = () => {
  const [palette, setPalette] = React.useState('sage');
  return (
    <PaletteCtx.Provider value={palette}>
      <Header palette={palette} onPalette={setPalette}/>
      <Brief palette={palette}/>

      {/* CORE SCREENS */}
      <Section id="core" num={1} title="Core screens" sub="Six primary surfaces — Home, Library, Scene Detail, Browse, Settings, Connection — and the landscape Player." palette={palette}>
        <ScrollRow>
          <Board w={PHONE_W} h={PHONE_H} label="Home"
            caption="Resume card up top with a strong inline progress bar. Continue / Recently added / Tonight (smart) / From a studio. Compact rails so two rows fit above the fold.">
            <Phone paletteKey={palette}><SpineHome/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Library"
            caption="Search → filter → grid. Inline filter chips show active state. Grid uses an Adaptive(180dp) layout — 2-up on phone, 3-up on tablet.">
            <Phone paletteKey={palette}><SpineLibrary/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Scene Detail"
            caption="Card-shaped hero (not full-bleed) keeps the title block legible. Tech metadata in a 2-column key/value grid. Markers are clickable chapters.">
            <Phone paletteKey={palette}><SpineDetail/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Browse · People"
            caption="Segmented control switches between Studios / Performers / Tags / Markers. The list shows mini-thumbnails of recent scenes per entity.">
            <Phone paletteKey={palette}><SpineBrowse/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Settings"
            caption="Grouped surfaces, JetBrains Mono for values on the right. Server URL in the section header, not buried under Account.">
            <Phone paletteKey={palette}><SpineSettings/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Connection · first run"
            caption="One screen, optimistic. Field labels above inputs in faint copy; the success card appears below before you commit.">
            <Phone paletteKey={palette}><SpineConnect/></Phone>
          </Board>
        </ScrollRow>
        <Row>
          <Board w={PLAYER_W} h={PLAYER_H} label="Player · landscape"
            caption="Full chapter strip above the timeline — each chapter labelled and proportional. Big rounded play, info dock top-right. Cleaner than the MX-Player original.">
            <PlayerBoard paletteKey={palette}><SpinePlayer w={PLAYER_W} h={PLAYER_H}/></PlayerBoard>
          </Board>
        </Row>
      </Section>

      {/* FLOWS */}
      <Section id="flows" num={2} title="Flows" sub="Modal flows and overlays that complete the IA — filter sheet, expanded search, More & Customize-nav, marker editor, player settings panel." palette={palette}>
        <ScrollRow>
          <Board w={PHONE_W} h={PHONE_H} label="Filter sheet · open"
            caption="Tri-state flag chips (default / yes / no). Active filters surface as removable chips at the top of the sheet itself. Apply button shows live result count.">
            <PhoneWithSheet paletteKey={palette} base={<SpineLibrary/>} sheet={<SpineFilterSheet/>}/>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Search · expanded with results"
            caption="Top result is type-aware (a Studio match outranks scene matches). Scope chips show counts. Performer hits land in pill-shaped chips below.">
            <Phone paletteKey={palette}><SpineSearchExpanded/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="More sheet"
            caption="Reached via the More dot in the nav. Two groups: Browse spillover (Tags / Markers / History) and App utilities (Settings, Customize, Cast).">
            <PhoneWithSheet paletteKey={palette} base={<SpineHome/>} sheet={<SpineMoreSheet/>}/>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Customize nav bar"
            caption="Drag-handles per row, max-4 selection enforced visually (over-cap rows fade). Active items get an accent-tinted border so the cap is obvious.">
            <PhoneWithSheet paletteKey={palette} base={<SpineHome/>} sheet={<SpineCustomizeNavSheet/>}/>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Marker editor"
            caption="A mini timeline at the top with marker dots — scrub to find a spot, then 'Add marker at current time'. Per-marker overflow lets you rename or jump.">
            <PhoneWithSheet paletteKey={palette} base={<SpineDetail/>} sheet={<SpineMarkerSheet/>}/>
          </Board>
        </ScrollRow>
        <Row>
          <Board w={PLAYER_W} h={PLAYER_H} label="Player · settings panel"
            caption="Slides in from the right (40% of screen at this size). Speed · audio tracks · subtitles · video info · Up next queue. Stays inside the chrome — no full-screen takeover.">
            <PlayerBoard paletteKey={palette}><SpinePlayerWithSettings w={PLAYER_W} h={PLAYER_H}/></PlayerBoard>
          </Board>
        </Row>
      </Section>

      {/* STATES */}
      <Section id="states" num={3} title="Real-content states" sub="What you actually see day-to-day: loading, empty, error, no-results, offline, unscraped. The boring stuff — usually the part that's missing." palette={palette}>
        <ScrollRow>
          <Board w={PHONE_W} h={PHONE_H} label="Home · loading"
            caption="Shimmer skeletons match the final rail shapes. A thin accent bar slides across the very top — subtle, doesn't dominate.">
            <Phone paletteKey={palette}><SpineHomeLoading/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Home · empty · scanning"
            caption="First-run, post-Connection. Library is empty but the server is actively scanning. Pulsing dot + progress turns the wait into a moment of progress.">
            <Phone paletteKey={palette}><SpineHomeEmpty/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Library · no results"
            caption="Doesn't just say 'no scenes' — offers real escape hatches. Clearing all filters but keeping search is the most common recovery; surface it.">
            <Phone paletteKey={palette}><SpineLibraryNoResults/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Library · server error"
            caption="Honest error block with the actual error code visible. Cached fallback is offered inline — you can still see something useful.">
            <Phone paletteKey={palette}><SpineLibraryError/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Connection · wrong server"
            caption="The URL responded but it's not Stash (e.g. the user pasted their router admin URL). Error explains what came back, in mono.">
            <Phone paletteKey={palette}><SpineConnectError/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Detail · scene not yet scraped"
            caption="No title, no thumbnail — just a filename and the file metadata Stash already knows. Still playable. Scrape now / Edit manually as next steps.">
            <Phone paletteKey={palette}><SpineDetailUnscraped/></Phone>
          </Board>
        </ScrollRow>
      </Section>

      {/* SETTINGS — REDESIGN */}
      <Section id="settings" num={4} title="Settings — redesigned" sub="Was: one giant strip of switches and dot-sliders. Now: a hub with grouped categories that show their current values inline, and focused drill-down pages. Critical actions (Disconnect, Server status) are surfaced; cosmetic toggles are nested." palette={palette}>
        <ScrollRow>
          <Board w={PHONE_W} h={PHONE_H} label="Settings · hub"
            caption="Landing page. Server status card at the very top — it's critical, you always want to see it. Search field. Grouped category list — each row shows its current values inline so you don't have to drill in just to check.">
            <Phone paletteKey={palette}><SpineSettingsHub/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Playback"
            caption="Defaults / Seeking / Resume & skip / Player chrome. Sliders are now compact horizontal tracks with an inline value bubble in mono — the previous dot-rows used ~80dp of vertical space each.">
            <Phone paletteKey={palette}><SpineSettingsPlayback/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Quality & codecs"
            caption="A success banner at the top calls out 'Full codec support' with FFmpeg detail. Then Decoder / Buffer / Display & HDR / Diagnostics. The codec test that didn't exist before lives here.">
            <Phone paletteKey={palette}><SpineSettingsCodecs/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Display"
            caption="Theme up top — palette picker is now a 3-card swatch row (Sage/Ember/Signal) with a check on the active one. Then Library layout / Card chrome / Player chrome.">
            <Phone paletteKey={palette}><SpineSettingsDisplay/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Library"
            caption="What syncs to Stash, image cache (compact slider), watch history, and a beta Downloads / offline section. The cache 'Clear' button sits in the group footer.">
            <Phone paletteKey={palette}><SpineSettingsLibrary/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Server"
            caption="Big status panel: green dot + endpoint + Stash version + a 4-column counts grid. 'Disconnect server' moved to a 'Danger zone' group at the bottom — separated visually.">
            <Phone paletteKey={palette}><SpineSettingsServer/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="About & diagnostics"
            caption="Version block, capabilities (with green codec status), storage, diagnostics (logs / network test / send debug report), legal. The build hash is included for bug reporting.">
            <Phone paletteKey={palette}><SpineSettingsAbout/></Phone>
          </Board>
          <Board w={PHONE_W} h={PHONE_H} label="Settings · search"
            caption="The hub's search field expands to this. Type 'resume' and matching settings appear from anywhere in the hierarchy, with a breadcrumb showing where they live. Tap a result to jump.">
            <Phone paletteKey={palette}><SpineSettingsSearch/></Phone>
          </Board>
        </ScrollRow>
      </Section>

      <div id="changelog" style={{ padding: '60px 32px 24px', borderTop: '1px solid rgba(160,180,220,0.10)' }}>
        <div style={{ maxWidth: 820 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 14, marginBottom: 28 }}>
            <div style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: 12, color: 'rgba(140,149,168,0.7)', letterSpacing: 2 }}>05</div>
            <div>
              <div style={{ fontSize: 32, fontWeight: 600, letterSpacing: -0.8, lineHeight: 1.1, color: '#EAEEF6' }}>Changelog</div>
              <div style={{ marginTop: 6, fontSize: 14, color: 'rgba(140,149,168,0.9)', lineHeight: 1.5 }}>UI revisions for Slopper · Spine direction.</div>
            </div>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
            {CHANGELOG.map((rev, i) => {
              const isFirst = i === 0;
              return (
                <div key={rev.v} style={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 28, paddingBottom: 28, marginBottom: 28, borderBottom: i < CHANGELOG.length - 1 ? '1px solid rgba(160,180,220,0.10)' : 'none' }}>
                  <div>
                    <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6, padding: '4px 10px', background: isFirst ? '#9DC83C' : 'rgba(255,255,255,0.04)', color: isFirst ? '#0B1402' : '#EAEEF6', border: isFirst ? 'none' : '1px solid rgba(160,180,220,0.16)', borderRadius: 999, fontFamily: 'JetBrains Mono', fontSize: 11, fontWeight: 700, letterSpacing: 0.6 }}>
                      v{rev.v}{isFirst && <span style={{ fontWeight: 600 }}>· current</span>}
                    </div>
                    <div style={{ marginTop: 8, fontFamily: 'JetBrains Mono', fontSize: 10, color: 'rgba(140,149,168,0.7)', letterSpacing: 1 }}>{rev.date.toUpperCase()}</div>
                  </div>
                  <div>
                    <div style={{ fontSize: 18, fontWeight: 600, letterSpacing: -0.3, color: '#EAEEF6', marginBottom: 12 }}>{rev.headline}</div>
                    <ul style={{ margin: 0, padding: 0, listStyle: 'none', display: 'flex', flexDirection: 'column', gap: 6 }}>
                      {rev.items.map((it, j) => (
                        <li key={j} style={{ fontSize: 13, color: 'rgba(234,238,246,0.78)', lineHeight: 1.55, display: 'flex', gap: 10 }}>
                          <span style={{ color: 'rgba(140,149,168,0.5)', flexShrink: 0 }}>—</span>
                          <span>{it}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      <div style={{ padding: '20px 32px 60px', textAlign: 'center', fontFamily: 'JetBrains Mono', fontSize: 11, color: 'rgba(234,238,246,0.3)', letterSpacing: 1.5, textTransform: 'uppercase' }}>
        End · Slopper Spine · v{SLOPPER_VERSION} · {SLOPPER_DATE}
      </div>
    </PaletteCtx.Provider>
  );
};

ReactDOM.createRoot(document.getElementById('root')).render(<App/>);
