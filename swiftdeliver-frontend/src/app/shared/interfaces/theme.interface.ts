export enum ThemeType {
  LIGHT = 'light',
  DARK = 'dark',
  AUTO = 'auto'
}

export interface ThemeConfig {
  theme: ThemeType;
  systemPreference: 'light' | 'dark';
  autoSwitchEnabled: boolean;
  transitionDuration: number;
}

export interface AutoSwitchTimes {
  lightStart: string;
  darkStart: string;
}

export interface ThemeState {
  currentTheme: ThemeType;
  appliedTheme: 'light' | 'dark';
  isAutoSwitchEnabled: boolean;
  autoSwitchTimes: AutoSwitchTimes;
}