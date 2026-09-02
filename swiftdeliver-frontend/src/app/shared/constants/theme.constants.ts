import { ThemeType } from '../interfaces/theme.interface';

export const THEME_CONSTANTS = {
  STORAGE_KEYS: {
    THEME: 'app-theme',
    AUTO_SWITCH: 'auto-switch-enabled',
    AUTO_SWITCH_TIMES: 'auto-switch-times'
  },
  
  DEFAULT_VALUES: {
    THEME: ThemeType.LIGHT,
    AUTO_SWITCH_ENABLED: false,
    TRANSITION_DURATION: 300,
    LIGHT_START_TIME: '06:00',
    DARK_START_TIME: '18:00'
  },
  
  CSS_CLASSES: {
    NIGHT_MODE: 'night-mode',
    THEME_TRANSITIONING: 'theme-transitioning'
  },
  
  MEDIA_QUERIES: {
    PREFERS_DARK: '(prefers-color-scheme: dark)'
  }
} as const;

export const THEME_CSS_VARIABLES = {
  LIGHT: {
    '--bg-primary': '#ffffff',
    '--bg-secondary': '#f8fafc',
    '--text-primary': '#1a202c',
    '--text-secondary': '#4a5568',
    '--text-muted': '#718096',
    '--border-color': '#e2e8f0',
    '--accent-color': '#3182ce',
    '--accent-color-light': 'rgba(49, 130, 206, 0.1)',
    '--accent-rgb': '49, 130, 206',
    '--shadow-color': 'rgba(0, 0, 0, 0.1)'
  },
  DARK: {
    '--bg-primary': '#1a202c',
    '--bg-secondary': '#2d3748',
    '--text-primary': '#f7fafc',
    '--text-secondary': '#e2e8f0',
    '--text-muted': '#a0aec0',
    '--border-color': '#4a5568',
    '--accent-color': '#63b3ed',
    '--accent-color-light': 'rgba(99, 179, 237, 0.1)',
    '--accent-rgb': '99, 179, 237',
    '--shadow-color': 'rgba(0, 0, 0, 0.3)'
  }
} as const;