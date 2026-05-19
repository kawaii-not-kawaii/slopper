// theme.jsx — three design directions for Slopper redesign

const THEMES = {
  reel: {
    name: 'Reel',
    tagline: 'Editorial · warm · cinema-house',
    bg: '#0B0908',
    bgWarm: '#100C0A',
    surface: '#15110F',
    surfaceHigh: '#1F1A17',
    surfaceTop: '#2A2320',
    border: 'rgba(255,240,220,0.08)',
    borderStrong: 'rgba(255,240,220,0.16)',
    text: '#F2EBE2',
    textDim: '#A29B92',
    textMuted: '#6E665E',
    textFaint: '#403B36',
    accent: '#E5793A',     // tungsten ember
    accentDim: '#A4542A',
    accentInk: '#1A0A02',
    cool: '#9FB7C4',       // gentle slate, for studio names
    coolDim: '#5E727E',
    warn: '#E8C547',
    err: '#E85757',
    fontDisp: '"Newsreader", "Source Serif Pro", Georgia, serif',
    fontBody: '"Manrope", "Helvetica Neue", Helvetica, sans-serif',
    fontMono: '"JetBrains Mono", ui-monospace, monospace',
    radius: 4,
    radiusLg: 6,
  },
  spine: {
    name: 'Spine',
    tagline: 'Dense · typographic · Linear-tight',
    bg: '#0A0D12',
    bgWarm: '#0D1119',
    surface: '#11151C',
    surfaceHigh: '#1A2030',
    surfaceTop: '#232B3D',
    border: 'rgba(160,180,220,0.10)',
    borderStrong: 'rgba(160,180,220,0.22)',
    text: '#EAEEF6',
    textDim: '#8C95A8',
    textMuted: '#525B6E',
    textFaint: '#2F3645',
    accent: '#B6F23C',     // electric lime
    accentDim: '#7CB025',
    accentInk: '#0C1402',
    cool: '#7FB6FF',       // azure for studios/tags
    coolDim: '#4A75B6',
    warn: '#FFCC44',
    err: '#FF5860',
    fontDisp: '"Space Grotesk", "Helvetica Neue", sans-serif',
    fontBody: '"Space Grotesk", "Helvetica Neue", sans-serif',
    fontMono: '"JetBrains Mono", ui-monospace, monospace',
    radius: 6,
    radiusLg: 10,
  },
  cinema: {
    name: 'Cinema',
    tagline: 'Bold · immersive · Apple-TV scale',
    bg: '#000000',
    bgWarm: '#08080A',
    surface: '#0E0E10',
    surfaceHigh: '#1A1A1E',
    surfaceTop: '#26262C',
    border: 'rgba(255,255,255,0.08)',
    borderStrong: 'rgba(255,255,255,0.18)',
    text: '#FFFFFF',
    textDim: '#9E9EA8',
    textMuted: '#5C5C68',
    textFaint: '#34343C',
    accent: '#FF5E5B',     // electric coral
    accentDim: '#C84843',
    accentInk: '#160000',
    cool: '#A8B8FF',       // periwinkle
    coolDim: '#6478C8',
    warn: '#FFD23F',
    err: '#FF5E5B',
    fontDisp: '"Bricolage Grotesque", "Helvetica Neue", sans-serif',
    fontBody: '"Bricolage Grotesque", "Helvetica Neue", sans-serif',
    fontMono: '"JetBrains Mono", ui-monospace, monospace',
    radius: 14,
    radiusLg: 22,
  },
};

const ThemeContext = React.createContext(THEMES.reel);
const useTheme = () => React.useContext(ThemeContext);
const ThemeProvider = ({ theme, children }) => (
  <ThemeContext.Provider value={typeof theme === 'string' ? THEMES[theme] : theme}>
    {children}
  </ThemeContext.Provider>
);

Object.assign(window, { THEMES, ThemeContext, ThemeProvider, useTheme });
