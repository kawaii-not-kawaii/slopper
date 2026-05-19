// spine-palettes.jsx — Spine direction with 3 toggleable accent palettes

const SPINE_PALETTES = {
  sage: {
    name: 'Sage',
    note: 'Toned-down lime — less neon, more moss',
    accent: '#9DC83C',
    accentDim: '#6E9028',
    accentInk: '#0B1402',
    cool: '#7FB6FF',
    coolDim: '#4A75B6',
  },
  amber: {
    name: 'Ember',
    note: 'Warm cinema projector — ties to original Slopper amber',
    accent: '#E5A742',
    accentDim: '#A57628',
    accentInk: '#1A0F00',
    cool: '#7FB6FF',
    coolDim: '#4A75B6',
  },
  cyan: {
    name: 'Signal',
    note: 'Cool cyan — Slopper teal evolved, easy on OLED',
    accent: '#4FD0E6',
    accentDim: '#298FA1',
    accentInk: '#001218',
    cool: '#C7A0FF',     // pair with violet for contrast
    coolDim: '#7050B0',
  },
};

const makeSpineTheme = (paletteKey) => {
  const p = SPINE_PALETTES[paletteKey];
  return {
    ...THEMES.spine,
    accent: p.accent,
    accentDim: p.accentDim,
    accentInk: p.accentInk,
    cool: p.cool,
    coolDim: p.coolDim,
  };
};

// CSS overrides so static color references in screens-spine.jsx don't dominate
// (they shouldn't — every color comes from `useTheme()` — but this is a
// belt-and-suspenders against any leaked rgba(182,242,60,...) string.)
const PaletteCSSPatch = ({ palette }) => {
  const p = SPINE_PALETTES[palette];
  const css = `
    [data-palette="${palette}"] .accent-bg { background: ${p.accent}; }
    [data-palette="${palette}"] .accent-fg { color: ${p.accent}; }
  `;
  return <style>{css}</style>;
};

Object.assign(window, { SPINE_PALETTES, makeSpineTheme, PaletteCSSPatch });
