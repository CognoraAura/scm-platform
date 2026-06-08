export const tokens = {
  // Layout
  headerHeight: 64,
  sidebarWidth: 240,
  sidebarCollapsedWidth: 80,
  contentPadding: 24,

  // Spacing (8px grid)
  spacing: {
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32,
    xxl: 48,
    xxxl: 64,
  },

  // Typography
  fontSize: {
    xs: 12,
    sm: 13,
    base: 14,
    lg: 16,
    xl: 20,
    xxl: 24,
    xxxl: 32,
  },

  // Border radius
  radius: {
    xs: 4,
    sm: 6,
    base: 8,
    lg: 12,
    xl: 16,
  },

  // Colors (light theme)
  colors: {
    primary: '#1677ff',
    success: '#52c41a',
    warning: '#faad14',
    error: '#ff4d4f',
    info: '#1677ff',
  },

  // Shadows
  shadow: {
    xs: '0 1px 2px 0 rgba(0,0,0,0.03), 0 1px 6px -1px rgba(0,0,0,0.02)',
    sm: '0 1px 2px 0 rgba(0,0,0,0.03), 0 2px 4px -1px rgba(0,0,0,0.02)',
    base: '0 6px 16px 0 rgba(0,0,0,0.08), 0 3px 6px -4px rgba(0,0,0,0.12)',
    lg: '0 12px 40px 0 rgba(0,0,0,0.12), 0 8px 20px -6px rgba(0,0,0,0.16)',
  },

  // Breakpoints
  breakpoint: {
    xs: 480,
    sm: 576,
    md: 768,
    lg: 992,
    xl: 1200,
    xxl: 1600,
  },

  // Animation
  duration: {
    fast: 100,
    normal: 200,
    slow: 300,
  },

  // Z-index layers
  zIndex: {
    dropdown: 1050,
    sticky: 1100,
    fixed: 1200,
    modalBackdrop: 1300,
    modal: 1400,
    popover: 1500,
    tooltip: 1600,
  },
} as const

export type Tokens = typeof tokens
