import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { SidebarService } from '../../services/sidebar.service';
import { ThemeService, ThemeType } from '../../services/theme.service';
import { AuthService } from '../../services/auth.service';
import { Subscription } from 'rxjs';

interface SearchResult {
  id: string;
  title: string;
  description: string;
  icon: string;
  route?: string;
  action?: () => void;
}

interface Message {
  id: number;
  sender: string;
  content: string;
  read: boolean;
  timestamp: Date;
  avatar?: string;
}

interface Notification {
  id: number;
  title: string;
  content: string;
  icon: string;
  read: boolean;
  timestamp: Date;
  type: 'info' | 'warning' | 'success' | 'error';
}

interface Language {
  code: string;
  name: string;
  flag: string;
}

import { User as AuthUser } from '../../services/auth.service';

interface User {
  name: string;
  email: string;
  role: string;
  avatar?: string;
}

@Component({
  selector: 'app-navbar',
  standalone: false,
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css'],
  animations: [
    trigger('fadeInOut', [
      transition(':enter', [
        style({ opacity: 0 }),
        animate('200ms ease-in', style({ opacity: 1 }))
      ]),
      transition(':leave', [
        animate('200ms ease-out', style({ opacity: 0 }))
      ])
    ])
  ]
})
export class Navbar implements OnInit, OnDestroy {
  constructor(
    private router: Router, 
    private sidebarService: SidebarService,
    private themeService: ThemeService,
    private authService: AuthService
  ) {}

  // Core properties
  isLoading = false;
  isNightMode = false;
  isMobileMenuOpen = false;
  companyName = 'Upstart';
  currentTheme: ThemeType = ThemeType.LIGHT;
  
  // Subscriptions
  private themeSubscription: Subscription = new Subscription();
  private userSubscription: Subscription = new Subscription();
  
  // Dropdown states
  isLanguageDropdownOpen = false;
  isMessagesDropdownOpen = false;
  isNotificationsDropdownOpen = false;
  isUserDropdownOpen = false;
  
  // User data
  user: User = {
    name: 'Guest User',
    email: '',
    role: 'Not Authenticated',
    avatar: ''
  };
  
  currentAuthUser: AuthUser | null = null;
  
  // Language settings
  currentLanguage = 'en';
  languages: Language[] = [
    { code: 'en', name: 'English', flag: '🇺🇸' },
    { code: 'fr', name: 'Français', flag: '🇫🇷' },
    { code: 'es', name: 'Español', flag: '🇪🇸' },
    { code: 'de', name: 'Deutsch', flag: '🇩🇪' },
    { code: 'zh', name: '中文', flag: '🇨🇳' },
    { code: 'ja', name: '日本語', flag: '🇯🇵' }
  ];
  
  // Navigation
  breadcrumbs = [
    { label: 'Home', link: '/' },
    { label: 'Dashboard', link: '/dashboard' },
    { label: 'Analytics', link: null }
  ];
  
  // Enhanced search
  searchQuery = '';
  searchPlaceholder = 'Search anything...';
  isSearchFocused = false;
  searchResults: SearchResult[] = [];
  selectedResultIndex = -1;
  private searchTimeout: any;
  
  // Messages with enhanced data
  messages: Message[] = [
    {
      id: 1,
      sender: 'Alice Johnson',
      content: 'The quarterly report is ready for review. Please check the analytics section.',
      read: false,
      timestamp: new Date(Date.now() - 1000 * 60 * 15) // 15 minutes ago
    },
    {
      id: 2,
      sender: 'Bob Smith',
      content: 'Team meeting scheduled for 3 PM today. Conference room B.',
      read: false,
      timestamp: new Date(Date.now() - 1000 * 60 * 30) // 30 minutes ago
    },
    {
      id: 3,
      sender: 'Carol Davis',
      content: 'New client onboarding process completed successfully.',
      read: true,
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 2) // 2 hours ago
    }
  ];
  
  // Enhanced notifications
  notifications: Notification[] = [
    {
      id: 1,
      title: 'New Order',
      content: 'Order #12345 has been received and is being processed.',
      icon: '📦',
      read: false,
      timestamp: new Date(Date.now() - 1000 * 60 * 5), // 5 minutes ago
      type: 'success'
    },
    {
      id: 2,
      title: 'System Maintenance',
      content: 'Scheduled maintenance will occur tonight at midnight.',
      icon: '🔧',
      read: false,
      timestamp: new Date(Date.now() - 1000 * 60 * 45), // 45 minutes ago
      type: 'warning'
    },
    {
      id: 3,
      title: 'Backup Complete',
      content: 'Daily backup completed successfully.',
      icon: '✅',
      read: true,
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 3), // 3 hours ago
      type: 'info'
    }
  ];
  
  // Computed properties
  get unreadMessagesCount() {
    return this.messages.filter(m => !m.read).length;
  }
  
  get unreadNotificationsCount() {
    return this.notifications.filter(n => !n.read).length;
  }
  ngOnInit() {
    // Subscribe to theme changes
    this.themeSubscription = this.themeService.theme$.subscribe(theme => {
      this.currentTheme = theme;
      this.isNightMode = this.themeService.getAppliedTheme() === 'dark';
    });
    
    // Subscribe to current user changes
    this.userSubscription = this.authService.currentUser$.subscribe(
      (authUser) => {
        this.currentAuthUser = authUser;
        if (authUser) {
          this.user = {
            name: `${authUser.firstName} ${authUser.lastName}`,
            email: authUser.email,
            role: this.formatUserRole(authUser.role),
            avatar: '' // Can be extended later if avatar field is added to User model
          };
        } else {
          this.user = {
            name: 'Guest User',
            email: '',
            role: 'Not Authenticated',
            avatar: ''
          };
        }
      }
    );
    
    // Initialize language from localStorage
    const savedLanguage = localStorage.getItem('language');
    if (savedLanguage && this.languages.find(l => l.code === savedLanguage)) {
      this.currentLanguage = savedLanguage;
    }
  }
  
  ngOnDestroy() {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    this.themeSubscription.unsubscribe();
    this.userSubscription.unsubscribe();
  }

  private formatUserRole(role: string): string {
    // Convert role enum to display-friendly format
    switch (role) {
      case 'SUPER_ADMIN':
        return 'Super Administrator';
      case 'VENDOR_OWNER':
        return 'Vendor Owner';
      case 'DELIVERY_OWNER':
        return 'Delivery Owner';
      case 'CLIENT':
        return 'Client';
      case 'DRIVER':
        return 'Driver';
      default:
        return role.replace('_', ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
    }
  }
  
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event) {
    const target = event.target as HTMLElement;
    
    // Close mobile menu when clicking outside
    if (this.isMobileMenuOpen) {
      if (!target.closest('.navbar-toggle') && !target.closest('.mobile-menu')) {
        this.isMobileMenuOpen = false;
      }
    }
    
    // Close dropdowns when clicking outside
    if (!target.closest('.language-dropdown')) {
      this.isLanguageDropdownOpen = false;
    }
    if (!target.closest('.messages-dropdown')) {
      this.isMessagesDropdownOpen = false;
    }
    if (!target.closest('.notifications-dropdown')) {
      this.isNotificationsDropdownOpen = false;
    }
    if (!target.closest('.user-dropdown')) {
      this.isUserDropdownOpen = false;
    }
  }
  
  @HostListener('window:resize', ['$event'])
  onWindowResize(event: Event) {
    // Close mobile menu on window resize
    if (this.isMobileMenuOpen) {
      this.isMobileMenuOpen = false;
    }
  }
  
  // Navigation methods
  navigateToHome() {
    this.router.navigate(['/dashboard']);
  }
  
  navigateTo(link: string | null) {
    if (link) {
      this.router.navigate([link]);
    }
  }
  
  navigateToProfile() {
    this.router.navigate(['/profile']);
    this.closeAllDropdowns();
  }
  
  navigateToSettings() {
    this.router.navigate(['/settings']);
    this.closeAllDropdowns();
  }
  
  navigateToHelp() {
    this.router.navigate(['/help']);
    this.closeAllDropdowns();
  }
  
  closeAllDropdowns() {
    this.isLanguageDropdownOpen = false;
    this.isMessagesDropdownOpen = false;
    this.isNotificationsDropdownOpen = false;
    this.isUserDropdownOpen = false;
  }
  
  // Sidebar toggle management
  toggleSidebar(): void {
    this.sidebarService.toggleSidebar();
  }

  // Mobile menu methods (kept for potential future use)
  toggleMobileMenu() {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }
  
  closeMobileMenu() {
    this.isMobileMenuOpen = false;
  }
  
  // Enhanced search methods
  onSearchFocus() {
    this.isSearchFocused = true;
  }
  
  onSearchBlur() {
    // Delay blur to allow for result selection
    setTimeout(() => {
      this.isSearchFocused = false;
      this.searchResults = [];
      this.selectedResultIndex = -1;
    }, 200);
  }
  
  onSearchInput() {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    
    this.searchTimeout = setTimeout(() => {
      this.performSearch();
    }, 300); // Debounce search
  }
  
  onSearchKeydown(event: KeyboardEvent) {
    if (this.searchResults.length === 0) return;
    
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.selectedResultIndex = Math.min(this.selectedResultIndex + 1, this.searchResults.length - 1);
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.selectedResultIndex = Math.max(this.selectedResultIndex - 1, -1);
        break;
      case 'Enter':
        event.preventDefault();
        if (this.selectedResultIndex >= 0) {
          this.selectSearchResult(this.searchResults[this.selectedResultIndex]);
        } else {
          this.onSearch();
        }
        break;
      case 'Escape':
        this.clearSearch();
        break;
    }
  }
  
  performSearch() {
    if (!this.searchQuery.trim()) {
      this.searchResults = [];
      return;
    }
    
    // Mock search results - replace with actual search logic
    const mockResults: SearchResult[] = [
      { id: '1', title: 'Dashboard', description: 'Main dashboard overview', icon: '📊', route: '/dashboard' },
      { id: '2', title: 'Orders', description: 'Track and manage delivery orders', icon: '📋', route: '/orders' },
      { id: '3', title: 'Customers', description: 'Manage customer accounts', icon: '👥', route: '/customers' },
      { id: '4', title: 'Vendor Companies', description: 'View vendor companies', icon: '🏢', route: '/vendorcompanies' },
      { id: '5', title: 'Settings', description: 'Application settings', icon: '⚙️', route: '/settings' }
    ];
    
    this.searchResults = mockResults.filter(result => 
      result.title.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
      result.description.toLowerCase().includes(this.searchQuery.toLowerCase())
    ).slice(0, 5); // Limit to 5 results
    
    this.selectedResultIndex = -1;
  }
  
  selectSearchResult(result: SearchResult) {
    if (result.action) {
      result.action();
    } else if (result.route) {
      this.navigateTo(result.route);
    }
    this.clearSearch();
  }
  
  onSearch() {
    if (this.searchQuery.trim()) {
      console.log('Executing search for:', this.searchQuery);
      // Implement full search functionality
      this.clearSearch();
    }
  }
  
  clearSearch() {
    this.searchQuery = '';
    this.searchResults = [];
    this.selectedResultIndex = -1;
    this.isSearchFocused = false;
  }
  
  // Quick actions
  createNew() {
    console.log('Creating new item');
    // Implement create new functionality
  }
  
  // Enhanced theme methods
  toggleNightMode() {
    this.themeService.toggleTheme();
  }
  
  getThemeIcon(): string {
    switch (this.currentTheme) {
      case 'light':
        return '☀️';
      case 'dark':
        return '🌙';
      case 'auto':
        return '🌓';
      default:
        return '☀️';
    }
  }
  
  getThemeTooltip(): string {
    switch (this.currentTheme) {
      case 'light':
        return 'Switch to Dark Mode';
      case 'dark':
        return 'Switch to Auto Mode';
      case 'auto':
        return 'Switch to Light Mode';
      default:
        return 'Toggle Theme';
    }
  }
  
  // Dropdown toggle methods
  toggleLanguageDropdown(event?: Event) {
    if (event) {
      event.stopPropagation();
    }
    this.isLanguageDropdownOpen = !this.isLanguageDropdownOpen;
    // Close other dropdowns
    this.isMessagesDropdownOpen = false;
    this.isNotificationsDropdownOpen = false;
    this.isUserDropdownOpen = false;
  }

  toggleMessagesDropdown(event?: Event) {
    if (event) {
      event.stopPropagation();
    }
    this.isMessagesDropdownOpen = !this.isMessagesDropdownOpen;
    // Close other dropdowns
    this.isLanguageDropdownOpen = false;
    this.isNotificationsDropdownOpen = false;
    this.isUserDropdownOpen = false;
  }

  toggleNotificationsDropdown(event?: Event) {
    if (event) {
      event.stopPropagation();
    }
    this.isNotificationsDropdownOpen = !this.isNotificationsDropdownOpen;
    // Close other dropdowns
    this.isLanguageDropdownOpen = false;
    this.isMessagesDropdownOpen = false;
    this.isUserDropdownOpen = false;
  }

  toggleUserDropdown(event?: Event) {
    if (event) {
      event.stopPropagation();
    }
    this.isUserDropdownOpen = !this.isUserDropdownOpen;
    // Close other dropdowns
    this.isLanguageDropdownOpen = false;
    this.isMessagesDropdownOpen = false;
    this.isNotificationsDropdownOpen = false;
  }

  // Language methods
  getLanguageName(code: string): string {
    const language = this.languages.find(l => l.code === code);
    return language ? language.name : code.toUpperCase();
  }

  changeLanguage(code: string) {
    this.currentLanguage = code;
    localStorage.setItem('language', code);
    console.log('Language changed to:', code);
    this.isLanguageDropdownOpen = false; // Close dropdown after selection
    // Implement actual language change logic
  }
  
  // Message methods
  openMessage(message: Message) {
    message.read = true;
    console.log('Opening message:', message.id);
    this.isMessagesDropdownOpen = false; // Close dropdown after selection
    // Implement message opening logic
  }

  markAllMessagesRead() {
    this.messages.forEach(msg => msg.read = true);
  }

  viewAllMessages() {
    console.log('Viewing all messages');
    this.isMessagesDropdownOpen = false; // Close dropdown after selection
    this.router.navigate(['/messagerie']);
  }
  
  // Notification methods
  openNotification(notification: Notification) {
    notification.read = true;
    console.log('Opening notification:', notification.id);
    this.isNotificationsDropdownOpen = false; // Close dropdown after selection
    // Implement notification opening logic
  }

  markAllNotificationsRead() {
    this.notifications.forEach(notif => notif.read = true);
  }

  viewAllNotifications() {
    console.log('Viewing all notifications');
    this.isNotificationsDropdownOpen = false; // Close dropdown after selection
    this.router.navigate(['/notifications']);
  }
  
  // User methods
  getUserInitials(): string {
    if (!this.currentAuthUser) return '??';
    const firstInitial = this.currentAuthUser.firstName?.charAt(0) || '';
    const lastInitial = this.currentAuthUser.lastName?.charAt(0) || '';
    return (firstInitial + lastInitial).toUpperCase() || '??';
  }
  
  logout() {
    console.log('Logging out');
    this.isUserDropdownOpen = false; // Close dropdown after selection
    
    // Call the auth service logout method and wait for completion
    this.authService.logoutUser().subscribe({
      next: () => {
        // Redirect to login page after successful logout
        this.router.navigate(['/login']);
      },
      error: (error) => {
        console.error('Logout error:', error);
        // Even if logout fails on server, clear local data and redirect
        this.authService.logout();
        this.router.navigate(['/login']);
      }
    });
  }
  
  // Utility methods
  formatTime(timestamp: Date): string {
    const now = new Date();
    const diff = now.getTime() - timestamp.getTime();
    const minutes = Math.floor(diff / (1000 * 60));
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7) return `${days}d ago`;
    
    return timestamp.toLocaleDateString();
  }
}
