import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, fromEvent } from 'rxjs';
import { map } from 'rxjs/operators';
import { ThemeType, ThemeConfig, AutoSwitchTimes, ThemeState } from '../shared/interfaces/theme.interface';
import { THEME_CONSTANTS, THEME_CSS_VARIABLES } from '../shared/constants/theme.constants';

// Export for backward compatibility
export type Theme = ThemeType;
export { ThemeType };

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly STORAGE_KEY = THEME_CONSTANTS.STORAGE_KEYS.THEME;
  private readonly AUTO_SWITCH_KEY = THEME_CONSTANTS.STORAGE_KEYS.AUTO_SWITCH;
  private readonly AUTO_SWITCH_TIMES_KEY = THEME_CONSTANTS.STORAGE_KEYS.AUTO_SWITCH_TIMES;
  
  private themeSubject = new BehaviorSubject<ThemeType>(THEME_CONSTANTS.DEFAULT_VALUES.THEME);
  private systemPreferenceSubject = new BehaviorSubject<'light' | 'dark'>('light');
  private autoSwitchSubject = new BehaviorSubject<boolean>(THEME_CONSTANTS.DEFAULT_VALUES.AUTO_SWITCH_ENABLED);
  
  public theme$: Observable<ThemeType> = this.themeSubject.asObservable();
  public systemPreference$: Observable<'light' | 'dark'> = this.systemPreferenceSubject.asObservable();
  public autoSwitch$: Observable<boolean> = this.autoSwitchSubject.asObservable();
  
  private mediaQuery: MediaQueryList | null = null;
  private autoSwitchTimer: number | null = null;
  
  constructor() {
    this.initializeTheme();
    this.setupSystemPreferenceListener();
    this.setupAutoSwitch();
  }
  
  private initializeTheme(): void {
    try {
      // Get saved theme from localStorage with fallback
      const savedTheme = this.getFromStorage(this.STORAGE_KEY) as ThemeType;
      const autoSwitchEnabled = this.getFromStorage(this.AUTO_SWITCH_KEY) === 'true';
      
      // Detect system preference
      this.mediaQuery = window.matchMedia(THEME_CONSTANTS.MEDIA_QUERIES.PREFERS_DARK);
      const systemPreference = this.mediaQuery.matches ? 'dark' : 'light';
      this.systemPreferenceSubject.next(systemPreference);
      
      // Set initial theme
      let initialTheme: ThemeType = savedTheme || THEME_CONSTANTS.DEFAULT_VALUES.THEME;
      
      if (autoSwitchEnabled) {
        this.autoSwitchSubject.next(true);
        initialTheme = ThemeType.AUTO;
      }
      
      this.setTheme(initialTheme, false);
    } catch (error) {
      console.warn('Failed to initialize theme from storage, using defaults:', error);
      this.setTheme(THEME_CONSTANTS.DEFAULT_VALUES.THEME, false);
    }
  }
  
  /**
   * Safely get item from localStorage with error handling
   */
  private getFromStorage(key: string): string | null {
    try {
      return localStorage.getItem(key);
    } catch (error) {
      return null;
    }
  }
  
  /**
   * Safely set item to localStorage with error handling
   */
  private setToStorage(key: string, value: string): void {
    try {
      localStorage.setItem(key, value);
    } catch (error) {}
  }
  
  private setupSystemPreferenceListener(): void {
    if (this.mediaQuery) {
      this.mediaQuery.addEventListener('change', (e) => {
        const systemPreference = e.matches ? 'dark' : 'light';
        this.systemPreferenceSubject.next(systemPreference);
        
        // If auto theme is enabled and auto-switch is disabled, use system preference
        if (this.themeSubject.value === ThemeType.AUTO && !this.autoSwitchSubject.value) {
          this.applyTheme(systemPreference);
        }
      });
    }
  }
  
  private setupAutoSwitch(): void {
    // Clear existing timer
    if (this.autoSwitchTimer) {
      clearInterval(this.autoSwitchTimer);
    }
    
    // Auto switch between light and dark based on time of day
    const checkTime = () => {
      if (this.autoSwitchSubject.value && this.themeSubject.value === ThemeType.AUTO) {
        const times = this.getAutoSwitchTimes();
        const now = new Date();
        const currentTime = now.getHours() * 60 + now.getMinutes();
        
        const [lightHour, lightMin] = times.lightStart.split(':').map(Number);
        const [darkHour, darkMin] = times.darkStart.split(':').map(Number);
        
        const lightTime = lightHour * 60 + lightMin;
        const darkTime = darkHour * 60 + darkMin;
        
        const shouldBeDark = currentTime >= darkTime || currentTime < lightTime;
        const timeBasedTheme = shouldBeDark ? 'dark' : 'light';
        this.applyTheme(timeBasedTheme);
      }
    };
    
    // Check every minute
    this.autoSwitchTimer = setInterval(checkTime, 60000);
    checkTime(); // Initial check
  }
  
  /**
   * Set the current theme
   * @param theme - The theme to set
   * @param saveToStorage - Whether to persist the theme to localStorage
   */
  public setTheme(theme: ThemeType, saveToStorage: boolean = true): void {
    this.themeSubject.next(theme);
    
    if (saveToStorage) {
      this.setToStorage(this.STORAGE_KEY, theme);
    }
    
    // Apply the actual theme
    this.applyTheme(this.getAppliedTheme());
  }
  
  /**
   * Apply the theme to the DOM
   * @param appliedTheme - The actual theme to apply ('light' or 'dark')
   */
  private applyTheme(appliedTheme: 'light' | 'dark'): void {
    const body = document.body;
    const isDark = appliedTheme === 'dark';
    
    // Add transition class for smooth theme changes
    body.classList.add(THEME_CONSTANTS.CSS_CLASSES.THEME_TRANSITIONING);
    
    // Apply theme
    body.classList.toggle(THEME_CONSTANTS.CSS_CLASSES.NIGHT_MODE, isDark);
    
    // Update meta theme-color for mobile browsers
    const metaThemeColor = document.querySelector('meta[name="theme-color"]');
    if (metaThemeColor) {
      metaThemeColor.setAttribute('content', isDark ? '#1a1a1a' : '#ffffff');
    }
    
    // Remove transition class after animation completes
    setTimeout(() => {
      body.classList.remove(THEME_CONSTANTS.CSS_CLASSES.THEME_TRANSITIONING);
    }, THEME_CONSTANTS.DEFAULT_VALUES.TRANSITION_DURATION);
  }
  
  /**
   * Get the currently applied theme (resolves 'auto' to actual theme)
   * @returns The actual theme being applied
   */
  public getAppliedTheme(): 'light' | 'dark' {
    const currentTheme = this.themeSubject.value;
    let appliedTheme: 'light' | 'dark';
    
    if (currentTheme === ThemeType.AUTO) {
      if (this.autoSwitchSubject.value) {
        // Use time-based switching
        const times = this.getAutoSwitchTimes();
        const now = new Date();
        const currentTime = now.getHours() * 60 + now.getMinutes();
        
        const [lightHour, lightMin] = times.lightStart.split(':').map(Number);
        const [darkHour, darkMin] = times.darkStart.split(':').map(Number);
        
        const lightTime = lightHour * 60 + lightMin;
        const darkTime = darkHour * 60 + darkMin;
        
        const shouldBeDark = currentTime >= darkTime || currentTime < lightTime;
        appliedTheme = shouldBeDark ? 'dark' : 'light';
      } else {
        // Use system preference
        appliedTheme = this.systemPreferenceSubject.value;
      }
    } else {
      appliedTheme = currentTheme === ThemeType.DARK ? 'dark' : 'light';
    }
    
    return appliedTheme;
  }
  

  
  public toggleTheme(): void {
    const currentTheme = this.themeSubject.value;
    
    switch (currentTheme) {
      case ThemeType.LIGHT:
        this.setTheme(ThemeType.DARK);
        break;
      case ThemeType.DARK:
        this.setTheme(ThemeType.AUTO);
        break;
      case ThemeType.AUTO:
        this.setTheme(ThemeType.LIGHT);
        break;
    }
  }
  
  /**
   * Enable or disable automatic theme switching
   * @param enabled - Whether to enable auto-switching
   */
  public setAutoSwitch(enabled: boolean): void {
    this.autoSwitchSubject.next(enabled);
    this.setToStorage(this.AUTO_SWITCH_KEY, enabled.toString());
    
    if (enabled && this.themeSubject.value === ThemeType.AUTO) {
      // Immediately apply time-based theme
      const appliedTheme = this.getAppliedTheme();
      this.applyTheme(appliedTheme);
    }
  }
  
  /**
   * Check if auto-switching is currently enabled
   * @returns True if auto-switching is enabled
   */
  public isAutoSwitchEnabled(): boolean {
    return this.autoSwitchSubject.value;
  }
  
  /**
   * Set custom auto-switch times
   * @param lightStart - Time to switch to light theme (HH:MM format)
   * @param darkStart - Time to switch to dark theme (HH:MM format)
   */
  public setAutoSwitchTimes(lightStart: string, darkStart: string): void {
    const times: AutoSwitchTimes = { lightStart, darkStart };
    this.setToStorage(this.AUTO_SWITCH_TIMES_KEY, JSON.stringify(times));
    
    // Restart auto-switch with new times
    if (this.autoSwitchSubject.value) {
      this.setupAutoSwitch();
    }
  }
  
  /**
   * Get the current auto-switch times
   * @returns The auto-switch times configuration
   */
  public getAutoSwitchTimes(): AutoSwitchTimes {
    const saved = this.getFromStorage(this.AUTO_SWITCH_TIMES_KEY);
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        // Validate the parsed data
        if (parsed.lightStart && parsed.darkStart) {
          return parsed;
        }
      } catch (error) {
        console.warn('Failed to parse auto-switch times from storage:', error);
      }
    }
    return {
      lightStart: THEME_CONSTANTS.DEFAULT_VALUES.LIGHT_START_TIME,
      darkStart: THEME_CONSTANTS.DEFAULT_VALUES.DARK_START_TIME
    };
  }
  
  public getCurrentTheme(): Theme {
    return this.themeSubject.value;
  }
  
  public getThemeConfig(): ThemeConfig {
    return {
      theme: this.themeSubject.value,
      systemPreference: this.systemPreferenceSubject.value,
      autoSwitchEnabled: this.autoSwitchSubject.value,
      transitionDuration: 300
    };
  }
  
  public destroy(): void {
    if (this.autoSwitchTimer) {
      clearInterval(this.autoSwitchTimer);
    }
    this.mediaQuery?.removeEventListener('change', () => {});
  }
}