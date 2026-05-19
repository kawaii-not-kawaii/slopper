# Changelog

UI revisions for Slopper · Spine direction. The version shown in the prototype header always matches the latest entry here. Bump the version in `spine-app.jsx` (`SLOPPER_VERSION`) whenever you ship a new revision.

---

## v3.0 — May 19, 2026 · Settings redesign

The previous Settings screen was a single vertically-scrolling strip of switches, dot-sliders, and chip-rows with no hierarchy. Disconnect-server sat next to "Show play count on cards." Sliders for one numeric value consumed ~80 dp of vertical space each. The version-1 design.

**Replaced with a hub + drill-down pattern:**

- **Hub landing page** with grouped categories. Every row shows its current values inline ("Playback · 1.0× · 10s seek · HW · Fit") so you can audit settings at a glance without drilling in.
- **Server status surfaced as a card** at the top of the hub — green dot, endpoint, version, latency. Always visible.
- **Quick-search field** that matches settings from anywhere in the hierarchy, with breadcrumb chips on each result.
- **7 focused detail pages:**
  - **Playback** — defaults / seeking / resume & skip / player chrome
  - **Quality & codecs** — success banner + decoder / buffer / HDR / diagnostics
  - **Display** — theme picker (3-palette visual swatches) / library layout / card chrome / player chrome
  - **Library** — Stash sync / cache (with size slider + clear button in group footer) / history / downloads (beta)
  - **Server** — big status panel with 4-column counts grid, network info, actions, danger zone
  - **About & diagnostics** — version block with build hash, capabilities, storage, logs, debug-report, legal
  - **Search overlay** — full-text search across settings with breadcrumbs

**New components added:**

- `CSlider` — compact horizontal slider with inline mono value bubble (replaces dot-row sliders)
- `CRange` — two-thumb range slider
- `HubGroup` / `HubRow` — grouped landing rows with leading icon, inline values, optional badge, danger styling
- `DetailGroup` / `DRow` — detail-page row primitive supporting both inline (k/v/switch) and stacked (k/sub/body) modes
- `TestBtn` — diagnostic action button used in codec/network test sections

**Other:**

- "Disconnect server" moved to a dedicated Danger zone group with red error styling — no longer competing visually with cosmetic toggles
- Codec test functionality surfaced (was previously not in the design at all)
- Build hash visible in About for bug reports

---

## v2.0 — May 18, 2026 · Spine deep-dive

Committed Spine as the chosen direction. Built out the full phone surface and the modal flows the original brief didn't cover.

**Palette additions:**
- "Sage" `#9DC83C` — the lime, toned down (less neon, more moss). Default.
- "Ember" `#E5A742` — warm projector amber, recalling original Slopper.
- "Signal" `#4FD0E6` — cool cyan, evolved from original teal.
- Live switcher in the prototype header.

**Modal flows added:**
- Full Filter sheet (tri-state flag chips, range slider for ratings, live result count on Apply)
- Search expanded with type-aware results (Studio match top, scene hits, performer pills)
- More sheet (Browse spillover + App utilities)
- Customize nav bar (max-4 enforcement)
- Marker editor (mini timeline + add-at-current-time)
- Player settings side-panel (speed / audio tracks / subtitles / video info / up next queue)

**Real-content states added:**
- Home loading skeletons (shimmer + accent bar)
- Home empty + scanning (pulsing progress)
- Library no-results (did-you-mean recovery)
- Library server error (with real error code, cached-fallback hint)
- Connection wrong-server (the "pasted router admin URL" case)
- Detail · unscraped scene (filename + file metadata, still playable)

---

## v1.0 — May 18, 2026 · Three-direction exploration

Initial brief response. Three complete points of view explored side-by-side.

- **Reel** — editorial cinema-house. Warm near-black, tungsten ember accent, Newsreader serif + Manrope. Sharp 4 dp corners.
- **Spine** — Linear-tight library. Cool charcoal, electric lime accent, Space Grotesk. Info-dense rails.
- **Cinema** — Apple-TV scale. True black, electric coral, Bricolage Grotesque. Full-bleed hero everything.

Each direction shipped with: Home / Library / Detail / Browse / Settings / Connection / Player. TV (10-foot) layouts for all three. Side-by-side comparison views.

User chose Spine. Reel and Cinema preserved in `Slopper Redesign.html` for reference only.

---

## Versioning policy

- **Major bump** (v1 → v2) when a whole screen pattern is reworked or the direction commits change.
- **Minor bump** (v3.0 → v3.1) for screen additions and significant component changes.
- **Patch bump** (v3.0.0 → v3.0.1) for copy / spacing / color tweaks.
- Bump `SLOPPER_VERSION` in `spine-app.jsx` and add a section here in the same edit. The prototype header badge surfaces the version live.
