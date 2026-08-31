import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, TrackByFunction } from '@angular/core';
import { ThemeService, ThemeType } from '../../services/theme.service';
import { Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { trigger, state, style, transition, animate } from '@angular/animations';

interface SettingsSection {
  id: string;
  title: string;
  icon: string;
  description: string;
}

interface NotificationSettings {
  email: boolean;
  push: boolean;
  sms: boolean;
  desktop: boolean;
}

interface PrivacySettings {
  profileVisibility: 'public' | 'private' | 'contacts';
  showEmail: boolean;
  showPhone: boolean;
  allowMessages: boolean;
  dataCollection: boolean;
}

interface SecuritySettings {
  twoFactorAuth: boolean;
  loginNotifications: boolean;
  sessionTimeout: number;
  passwordExpiry: number;
}

interface GeneralSettings {
  language: string;
  timezone: string;
  dateFormat: string;
  currency: string;
  autoSave: boolean;
}

@Component({
  selector: 'app-settings',
  standalone: false,
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    trigger('slideInOut', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(-10px)' }),
        animate('300ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ]),
      transition(':leave', [
        animate('200ms ease-in', style({ opacity: 0, transform: 'translateY(-10px)' }))
      ])
    ])
  ]
})
export class SettingsComponent implements OnInit, OnDestroy {
  activeSection: string = 'general';
  
  settingsSections: SettingsSection[] = [
    {
      id: 'general',
      title: 'General',
      icon: '⚙️',
      description: 'Basic application preferences'
    },
    {
      id: 'theme',
      title: 'Appearance',
      icon: '🎨',
      description: 'Theme and visual settings'
    },
    {
      id: 'notifications',
      title: 'Notifications',
      icon: '🔔',
      description: 'Manage notification preferences'
    },
    {
      id: 'privacy',
      title: 'Privacy',
      icon: '🔒',
      description: 'Privacy and visibility settings'
    },
    {
      id: 'security',
      title: 'Security',
      icon: '🛡️',
      description: 'Account security options'
    },
    {
      id: 'data',
      title: 'Data & Storage',
      icon: '💾',
      description: 'Data management and storage'
    }
  ];

  // Settings data
  notificationSettings: NotificationSettings = {
    email: true,
    push: true,
    sms: false,
    desktop: true
  };

  privacySettings: PrivacySettings = {
    profileVisibility: 'public',
    showEmail: false,
    showPhone: false,
    allowMessages: true,
    dataCollection: true
  };

  securitySettings: SecuritySettings = {
    twoFactorAuth: false,
    loginNotifications: true,
    sessionTimeout: 30,
    passwordExpiry: 90
  };

  generalSettings: GeneralSettings = {
    language: 'en',
    timezone: 'UTC',
    dateFormat: 'MM/DD/YYYY',
    currency: 'USD',
    autoSave: true
  };

  // Theme-related properties
  currentTheme: ThemeType = ThemeType.LIGHT;
  autoSwitchEnabled = false;
  lightStartTime = '06:00';
  darkStartTime = '18:00';
  
  // Theme configuration
  readonly themeOptions = [
    {
      type: ThemeType.LIGHT,
      name: 'Light',
      icon: '☀️',
      description: 'Always use light theme',
      previewClass: 'light-preview'
    },
    {
      type: ThemeType.DARK,
      name: 'Dark',
      icon: '🌙',
      description: 'Always use dark theme',
      previewClass: 'dark-preview'
    },
    {
      type: ThemeType.AUTO,
      name: 'Auto',
      icon: '🌓',
      description: 'Follow system preference or time-based switching',
      previewClass: 'auto-preview'
    }
  ];

  // Language options
  languageOptions = [
    { code: 'en', name: 'English' },
    { code: 'fr', name: 'Français' },
    { code: 'es', name: 'Español' },
    { code: 'de', name: 'Deutsch' }
  ];

  // Timezone options
  timezoneOptions = [
    { code: 'UTC', name: 'UTC (Coordinated Universal Time)' },
    { code: 'EST', name: 'EST (Eastern Standard Time)' },
    { code: 'PST', name: 'PST (Pacific Standard Time)' },
    { code: 'CET', name: 'CET (Central European Time)' }
  ];

  // Private properties
  private readonly destroy$ = new Subject<void>();
  private readonly timeValidationPattern = /^([01]?[0-9]|2[0-3]):[0-5][0-9]$/;
  
  constructor(private themeService: ThemeService) {}

  ngOnInit(): void {
    this.initializeThemeSubscription();
    this.loadInitialThemeSettings();
    this.loadSettings();
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  // Private methods for theme functionality
  private initializeThemeSubscription(): void {
    this.themeService.theme$
      .pipe(takeUntil(this.destroy$))
      .subscribe(theme => {
        this.currentTheme = theme;
      });
  }
  
  private loadInitialThemeSettings(): void {
    this.autoSwitchEnabled = this.themeService.isAutoSwitchEnabled ? this.themeService.isAutoSwitchEnabled() : false;
    if (this.themeService.getAutoSwitchTimes) {
      const times = this.themeService.getAutoSwitchTimes();
      this.lightStartTime = times.lightStart || '06:00';
      this.darkStartTime = times.darkStart || '18:00';
    }
  }
  
  private isValidTimeFormat(time: string): boolean {
    return this.timeValidationPattern.test(time);
  }

  setActiveSection(sectionId: string): void {
    this.activeSection = sectionId;
  }

  // Enhanced theme management methods
  setTheme(theme: ThemeType): void {
    if (this.currentTheme === theme) {
      return; // Avoid unnecessary operations
    }
    
    this.currentTheme = theme;
    this.themeService.setTheme(theme);
    
    // If switching away from auto, disable auto-switch
    if (theme !== ThemeType.AUTO && this.autoSwitchEnabled) {
      this.autoSwitchEnabled = false;
      if (this.themeService.setAutoSwitch) {
        this.themeService.setAutoSwitch(false);
      }
    }
    
    this.saveSettings();
  }
  
  toggleAutoSwitch(): void {
    this.autoSwitchEnabled = !this.autoSwitchEnabled;
    if (this.themeService.setAutoSwitch) {
      this.themeService.setAutoSwitch(this.autoSwitchEnabled);
    }
    
    if (this.autoSwitchEnabled) {
      this.setTheme(ThemeType.AUTO);
    }
  }
  
  updateAutoSwitchTimes(): void {
    if (this.isValidTimeFormat(this.lightStartTime) && this.isValidTimeFormat(this.darkStartTime)) {
      if (this.themeService.setAutoSwitchTimes) {
        this.themeService.setAutoSwitchTimes(this.lightStartTime, this.darkStartTime);
      }
    }
  }
  
  // Getter methods for template access
  get isAutoThemeSelected(): boolean {
    return this.currentTheme === ThemeType.AUTO;
  }
  
  get canShowTimeSettings(): boolean {
    return this.isAutoThemeSelected && this.autoSwitchEnabled;
  }
  
  // TrackBy function for performance optimization
  trackByThemeType: TrackByFunction<any> = (index: number, item: any) => item.type;
  
  // Public methods for theme functionality
  getThemeIcon(theme: ThemeType): string {
    const themeOption = this.themeOptions.find(option => option.type === theme);
    return themeOption?.icon || '☀️';
  }
  
  getThemeDescription(theme: ThemeType): string {
    const themeOption = this.themeOptions.find(option => option.type === theme);
    return themeOption?.description || '';
  }
  
  isThemeActive(theme: ThemeType): boolean {
    return this.currentTheme === theme;
  }
  
  onTimeInputChange(timeType: 'light' | 'dark', event: Event): void {
    const target = event.target as HTMLInputElement;
    const timeValue = target.value;
    
    if (this.isValidTimeFormat(timeValue)) {
      if (timeType === 'light') {
        this.lightStartTime = timeValue;
      } else {
        this.darkStartTime = timeValue;
      }
      this.updateAutoSwitchTimes();
    }
  }
  
  loadSettings(): void {
    // Implement load logic
    console.log('Loading settings...');
  }

  saveSettings(): void {
    // Implement save logic
    console.log('Settings saved:', {
      general: this.generalSettings,
      notifications: this.notificationSettings,
      privacy: this.privacySettings,
      security: this.securitySettings
    });
  }

  resetSettings(): void {
    // Implement reset logic
    if (confirm('Are you sure you want to reset all settings to default?')) {
      // Reset to defaults
      this.generalSettings = {
        language: 'en',
        timezone: 'UTC',
        dateFormat: 'MM/DD/YYYY',
        currency: 'USD',
        autoSave: true
      };
      
      this.notificationSettings = {
        email: true,
        push: true,
        sms: false,
        desktop: true
      };
      
      this.privacySettings = {
        profileVisibility: 'public',
        showEmail: false,
        showPhone: false,
        allowMessages: true,
        dataCollection: true
      };
      
      this.securitySettings = {
        twoFactorAuth: false,
        loginNotifications: true,
        sessionTimeout: 30,
        passwordExpiry: 90
      };
    }
  }

  exportSettings(): void {
    const settings = {
      general: this.generalSettings,
      notifications: this.notificationSettings,
      privacy: this.privacySettings,
      security: this.securitySettings,
      theme: this.currentTheme
    };
    
    const dataStr = JSON.stringify(settings, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    
    const link = document.createElement('a');
    link.href = url;
    link.download = 'settings-export.json';
    link.click();
    
    URL.revokeObjectURL(url);
  }

  get ThemeType() {
    return ThemeType;
  }
}