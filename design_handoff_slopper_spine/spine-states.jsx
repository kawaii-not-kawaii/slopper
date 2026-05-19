// spine-states.jsx — real-content states for Spine: loading, error, empty,
// no-results, offline / connection error.

// ─── Skeleton primitives ────────────────────────────────────────
const Skel = ({ w, h, r = 4, style }) => {
  const t = useTheme();
  return (
    <div style={{
      width: w, height: h, borderRadius: r,
      background: `linear-gradient(90deg, ${t.surface} 0%, ${t.surfaceHigh} 50%, ${t.surface} 100%)`,
      backgroundSize: '200% 100%',
      ...style,
    }}/>
  );
};

// ─── Home · loading state ──────────────────────────────────────
const SpineHomeLoading = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '4px 18px 12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <SpineLogo/>
          <div style={{ fontFamily: t.fontBody, fontSize: 16, fontWeight: 600 }}>Slopper</div>
          <Skel w={64} h={14}/>
        </div>
        <div style={{ display: 'flex', gap: 6 }}><Skel w={28} h={28}/><Skel w={28} h={28}/></div>
      </div>
      <div style={{ flex: 1, overflow: 'hidden', paddingBottom: 90 }}>
        <div style={{ padding: '0 18px 24px' }}>
          <Skel w="100%" h={88} r={8}/>
        </div>
        {[1, 2, 3].map(i => (
          <div key={i} style={{ padding: '0 18px 24px' }}>
            <div style={{ display: 'flex', gap: 10, marginBottom: 12 }}>
              <Skel w={140} h={16}/>
              <Skel w={28} h={16}/>
            </div>
            <div style={{ display: 'flex', gap: 10, overflow: 'hidden' }}>
              {[0,1,2,3].map(j => <Skel key={j} w={180} h={101} r={6}/>)}
            </div>
          </div>
        ))}
      </div>
      {/* Subtle loading bar across top */}
      <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 2, background: `linear-gradient(90deg, transparent, ${t.accent}, transparent)`, backgroundSize: '200% 100%', animation: 'shimmer 1.4s linear infinite' }}/>
      <style>{`@keyframes shimmer { 0%{background-position: -100% 0} 100%{background-position: 100% 0} }`}</style>
      <SBottomNav active="home"/>
    </div>
  );
};

// ─── Home · empty state (no rails yet) ─────────────────────────
const SpineHomeEmpty = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '4px 18px 12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <SpineLogo/>
          <div style={{ fontFamily: t.fontBody, fontSize: 16, fontWeight: 600 }}>Slopper</div>
        </div>
      </div>
      <div style={{ flex: 1, padding: '8px 24px', display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'flex-start' }}>
        <div style={{ width: 56, height: 56, borderRadius: 12, background: t.surface, border: `1px solid ${t.border}`, display: 'flex', alignItems: 'center', justifyContent: 'center', color: t.textMuted, marginBottom: 22 }}>
          <I.film size={26}/>
        </div>
        <div style={{ fontFamily: t.fontBody, fontSize: 22, fontWeight: 600, color: t.text, letterSpacing: -0.5, lineHeight: 1.2, marginBottom: 14, textWrap: 'balance' }}>Library's empty — for&nbsp;now.</div>
        <div style={{ fontFamily: t.fontBody, fontSize: 13, color: t.textDim, lineHeight: 1.55, marginBottom: 24 }}>Once your Stash server finishes its first scan, rails will populate here automatically.</div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, width: '100%' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 14px', background: accentA(t, 0.08), border: `1px solid ${accentA(t, 0.25)}`, borderRadius: t.radius }}>
            <div style={{ width: 6, height: 6, borderRadius: '50%', background: t.accent, animation: 'pulse 1.5s ease-in-out infinite' }}/>
            <div style={{ flex: 1, fontFamily: t.fontBody, fontSize: 12, color: t.text }}>Scanning · 412 / 1,247 files</div>
            <div style={{ fontFamily: t.fontMono, fontSize: 11, color: t.accent }}>33%</div>
          </div>
        </div>
        <button style={{ marginTop: 18, padding: '10px 18px', background: 'transparent', color: t.accent, border: `1px solid ${accentA(t, 0.3)}`, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 12, fontWeight: 600, alignSelf: 'flex-start' }}>Trigger scan now</button>
      </div>
      <style>{`@keyframes pulse { 0%, 100% { opacity: 1 } 50% { opacity: 0.3 } }`}</style>
      <SBottomNav active="home"/>
    </div>
  );
};

// ─── Library · no results ──────────────────────────────────────
const SpineLibraryNoResults = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '4px 18px 10px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <div style={{ fontFamily: t.fontBody, fontSize: 18, fontWeight: 600 }}>Library</div>
          <div style={{ display: 'flex', gap: 4 }}><div style={{ width: 32, height: 32 }}/><div style={{ width: 32, height: 32 }}/></div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 12px', background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius, marginBottom: 10 }}>
          <I.search size={14} color={t.accent}/>
          <div style={{ flex: 1, fontFamily: t.fontMono, fontSize: 12, color: t.text }}>obscure search query</div>
          <I.close size={14} color={t.textMuted}/>
        </div>
        <div style={{ display: 'flex', gap: 6 }}>
          {['4K only', '≥ 60 min', 'Has captions'].map(f => (
            <div key={f} style={{ padding: '5px 10px', background: accentA(t, 0.1), color: t.accent, border: `1px solid ${accentA(t, 0.3)}`, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 11, fontWeight: 500, display: 'flex', alignItems: 'center', gap: 4 }}>
              {f} <I.close size={9}/>
            </div>
          ))}
        </div>
      </div>
      <div style={{ flex: 1, padding: '40px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', justifyContent: 'flex-start' }}>
        <div style={{ width: 48, height: 48, borderRadius: '50%', background: t.surface, display: 'flex', alignItems: 'center', justifyContent: 'center', color: t.textMuted, marginBottom: 18 }}>
          <I.search size={22}/>
        </div>
        <div style={{ fontFamily: t.fontBody, fontSize: 18, fontWeight: 600, color: t.text, marginBottom: 6 }}>Nothing matches.</div>
        <div style={{ fontFamily: t.fontBody, fontSize: 13, color: t.textDim, lineHeight: 1.5, maxWidth: 280, marginBottom: 22 }}>
          Try widening the date range, dropping a filter, or clearing search.
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6, width: '100%', maxWidth: 280 }}>
          <button style={{ padding: '10px 14px', background: t.accent, color: t.accentInk, border: 0, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 12, fontWeight: 700 }}>Clear all filters · keep search</button>
          <button style={{ padding: '10px 14px', background: 'transparent', color: t.text, border: `1px solid ${t.border}`, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 12, fontWeight: 600 }}>Reset to defaults</button>
        </div>
        {/* "did you mean" suggestion */}
        <div style={{ marginTop: 30, padding: 14, background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius, width: '100%' }}>
          <div style={{ ...SP_meta(t, t.textMuted), letterSpacing: 1.2, textTransform: 'uppercase', fontWeight: 600, marginBottom: 8, fontFamily: t.fontMono }}>Did you mean</div>
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
            {['Studio Aria', 'Anya Korbel', 'Long take', '16mm'].map(s => (
              <div key={s} style={{ padding: '5px 10px', background: t.bg, border: `1px solid ${t.border}`, borderRadius: 999, fontFamily: t.fontBody, fontSize: 12, color: t.accent }}>{s}</div>
            ))}
          </div>
        </div>
      </div>
      <SBottomNav active="library"/>
    </div>
  );
};

// ─── Library · error state ─────────────────────────────────────
const SpineLibraryError = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '4px 18px 10px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <div style={{ fontFamily: t.fontBody, fontSize: 18, fontWeight: 600 }}>Library</div>
        </div>
      </div>
      <div style={{ flex: 1, padding: '40px 24px', display: 'flex', flexDirection: 'column', alignItems: 'flex-start', justifyContent: 'flex-start' }}>
        <div style={{ width: 48, height: 48, borderRadius: 12, background: 'rgba(255,88,96,0.1)', border: `1px solid rgba(255,88,96,0.3)`, display: 'flex', alignItems: 'center', justifyContent: 'center', color: t.err, marginBottom: 22 }}>
          <I.bolt size={22}/>
        </div>
        <div style={{ fontFamily: t.fontBody, fontSize: 22, fontWeight: 600, color: t.text, letterSpacing: -0.5, marginBottom: 10 }}>Can't reach the server.</div>
        <div style={{ fontFamily: t.fontBody, fontSize: 13, color: t.textDim, lineHeight: 1.55, marginBottom: 18 }}>
          Server <span style={{ color: t.text, fontFamily: t.fontMono, fontSize: 12 }}>media.local:9999</span> didn't respond. Last successful sync was 12 minutes ago.
        </div>
        {/* error detail */}
        <div style={{ width: '100%', padding: 14, background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius, marginBottom: 18 }}>
          <div style={{ fontFamily: t.fontMono, fontSize: 10, color: t.err, letterSpacing: 1.2, textTransform: 'uppercase', fontWeight: 600, marginBottom: 6 }}>Error · GRAPHQL_NETWORK</div>
          <div style={{ fontFamily: t.fontMono, fontSize: 11, color: t.textDim, lineHeight: 1.5 }}>fetch failed: ETIMEDOUT 192.168.1.10:9999 after 8.0s</div>
        </div>
        <div style={{ display: 'flex', gap: 8, width: '100%' }}>
          <button style={{ flex: 1, padding: '12px', background: t.accent, color: t.accentInk, border: 0, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 13, fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}><I.refresh size={14}/> Retry</button>
          <button style={{ padding: '12px 16px', background: 'transparent', color: t.text, border: `1px solid ${t.border}`, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 13, fontWeight: 600 }}>Server settings</button>
        </div>
        <div style={{ marginTop: 24, fontFamily: t.fontBody, fontSize: 12, color: t.textMuted }}>
          You can still browse <span style={{ color: t.accent }}>5 cached scenes</span> from your recent activity.
        </div>
      </div>
      <SBottomNav active="library"/>
    </div>
  );
};

// ─── Connection · failed state ────────────────────────────────
const SpineConnectError = () => {
  const t = useTheme();
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, padding: 24, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 24 }}>
        <SpineLogo/>
        <div style={{ fontFamily: t.fontBody, fontSize: 16, fontWeight: 600, letterSpacing: -0.3 }}>Slopper</div>
      </div>
      <div style={{ fontFamily: t.fontBody, fontSize: 26, fontWeight: 600, letterSpacing: -0.6, lineHeight: 1.05 }}>Couldn't connect.</div>
      <div style={{ fontFamily: t.fontBody, fontSize: 13, color: t.textDim, marginTop: 10, marginBottom: 24, lineHeight: 1.5 }}>The URL responded, but it doesn't look like a Stash server. Check your URL and try again.</div>
      <SpineConnectInput label="Server URL" value="http://10.0.0.99:9999"/>
      <SpineConnectInput label="API key" value="•••••" trailing={<I.eye size={14}/>}/>
      <div style={{ marginTop: 6, padding: 12, background: 'rgba(255,88,96,0.08)', border: `1px solid rgba(255,88,96,0.3)`, borderRadius: 6, display: 'flex', gap: 10, alignItems: 'flex-start' }}>
        <I.close size={16} color={t.err}/>
        <div style={{ flex: 1 }}>
          <div style={{ fontFamily: t.fontBody, fontSize: 13, color: t.text }}>Unexpected response · 200 OK</div>
          <div style={{ ...SP_meta(t, t.textMuted), marginTop: 4, fontFamily: t.fontMono }}>expected JSON · got text/html "&lt;!doctype html…&gt;"</div>
        </div>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 18 }}>
        <button style={{ flex: 1, padding: 11, background: 'transparent', color: t.text, border: `1px solid ${t.border}`, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 12, fontWeight: 600 }}>Help · find my server</button>
        <button style={{ flex: 2, padding: 11, background: t.accent, color: t.accentInk, border: 0, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 12, fontWeight: 700 }}>Retry test</button>
      </div>
    </div>
  );
};

// ─── Detail · scene not yet scraped ────────────────────────────
const SpineDetailUnscraped = () => {
  const t = useTheme();
  const s = sceneById('s6');
  return (
    <div style={{ position: 'absolute', inset: 0, background: t.bg, display: 'flex', flexDirection: 'column' }}>
      <div style={{ flex: 1, overflowY: 'auto', paddingBottom: 24 }}>
        <div style={{ padding: '8px 18px 0' }}>
          <div style={{ position: 'relative', width: '100%', aspectRatio: '16/10', borderRadius: t.radiusLg, overflow: 'hidden', border: `1px solid ${t.border}`, background: t.surface }}>
            {/* No proper artwork — show file-icon placeholder */}
            <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 10, color: t.textMuted }}>
              <I.film size={48}/>
              <div style={{ fontFamily: t.fontMono, fontSize: 10, letterSpacing: 1.2, textTransform: 'uppercase' }}>No thumbnail · run scrape</div>
            </div>
            <div style={{ position: 'absolute', top: 10, left: 10, width: 30, height: 30, borderRadius: '50%', background: 'rgba(0,0,0,0.55)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff' }}>
              <I.chev_l size={16}/>
            </div>
          </div>
        </div>
        <div style={{ padding: '16px 18px 0' }}>
          <div style={{ ...SP_meta(t, t.textMuted), fontWeight: 700, letterSpacing: 1, textTransform: 'uppercase' }}>Not scraped</div>
          <div style={{ fontFamily: t.fontMono, fontSize: 14, color: t.text, marginTop: 4, wordBreak: 'break-all' }}>20231012_drive_oregon_001_FINAL.mp4</div>
          <div style={{ display: 'flex', gap: 8, marginTop: 8, ...SP_meta(t, t.textDim) }}>
            <span>added 11 days ago</span><span style={{ color: t.textFaint }}>·</span>
            <span>2.14 GB</span>
          </div>
          <button style={{ marginTop: 16, width: '100%', padding: 12, background: t.accent, color: t.accentInk, border: 0, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 13, fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}>
            <I.play size={14} fillSolid sw={0}/> Play raw
          </button>
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            <button style={{ flex: 1, padding: 11, background: t.surface, color: t.text, border: `1px solid ${accentA(t, 0.3)}`, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 12, fontWeight: 600 }}>Scrape now</button>
            <button style={{ flex: 1, padding: 11, background: t.surface, color: t.text, border: `1px solid ${t.border}`, borderRadius: t.radius, fontFamily: t.fontBody, fontSize: 12, fontWeight: 600 }}>Edit manually</button>
          </div>
          <div style={{ marginTop: 22, padding: 14, background: t.surface, border: `1px solid ${t.border}`, borderRadius: t.radius }}>
            <div style={{ fontFamily: t.fontMono, fontSize: 10, color: t.textMuted, letterSpacing: 1.2, textTransform: 'uppercase', fontWeight: 600, marginBottom: 10 }}>File · only known data</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, fontFamily: t.fontMono, fontSize: 12 }}>
              {[
                ['Path', '/library/2023/10/'],
                ['Codec', 'HEVC'],
                ['Resolution', '3840 × 2160'],
                ['Bitrate', '14.2 Mbps'],
                ['Duration', '36:02'],
                ['Hash', 'sha1:7a8d…3f12'],
              ].map(([k, v]) => (
                <div key={k} style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: t.textMuted }}>{k}</span>
                  <span style={{ color: t.text }}>{v}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

Object.assign(window, {
  SpineHomeLoading, SpineHomeEmpty,
  SpineLibraryNoResults, SpineLibraryError,
  SpineConnectError, SpineDetailUnscraped,
  Skel,
});
