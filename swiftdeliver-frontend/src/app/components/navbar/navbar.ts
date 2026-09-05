import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { SidebarService } from '../../services/sidebar.service';
import { ThemeService, ThemeType } from '../../services/theme.service';
import { AuthService } from '../../services/auth.service';
import { RealtimeService, OrderRealtimeEvent } from '../../services/realtime.service';
import { Subscription } from 'rxjs';

interface SearchResult {
  id: string;
  title: string;
  description: string;
  icon: string;
  route?: string;
  action?: () => void;
  roles?: string[];
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
    private authService: AuthService,
    private realtimeService: RealtimeService
  ) {}

  // Core properties
  isLoading = false;
  isNightMode = false;
  isMobileMenuOpen = false;
  companyName = 'SwiftDeliver';
  currentTheme: ThemeType = ThemeType.LIGHT;
  
  // Subscriptions
  private themeSubscription: Subscription = new Subscription();
  private userSubscription: Subscription = new Subscription();
  private eventSubscription: Subscription = new Subscription();
  
  // Dropdown states
  isLanguageDropdownOpen = false;
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
    { code: 'en', name: 'English', flag: 'fa-solid fa-globe' },
    { code: 'fr', name: 'Français', flag: 'fa-solid fa-globe' },
    { code: 'es', name: 'Español', flag: 'fa-solid fa-globe' },
    { code: 'de', name: 'Deutsch', flag: 'fa-solid fa-globe' },
    { code: 'zh', name: '中文', flag: 'fa-solid fa-globe' },
    { code: 'ja', name: '日本語', flag: 'fa-solid fa-globe' }
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
  
  // Enhanced notifications (dynamic, driven by realtime events)
  notifications: Notification[] = [];
  
  // Computed properties
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
    
    // Subscribe to realtime order events to build dynamic notifications
    this.eventSubscription = this.realtimeService.events$.subscribe(event => {
      if (event) {
        this.addRealtimeNotification(event);
      }
    });
  }
  
  ngOnDestroy() {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    this.themeSubscription.unsubscribe();
    this.userSubscription.unsubscribe();
    if (this.eventSubscription) {
      this.eventSubscription.unsubscribe();
    }
  }

  private addRealtimeNotification(event: OrderRealtimeEvent) {
    const titleMap: Record<string, string> = {
      'ORDER_CREATED': 'New Order',
      'ORDER_STATUS_CHANGED': 'Order Update',
      'DRIVER_ASSIGNED': 'Driver Assigned',
      'DRIVER_LOCATION_UPDATE': 'Location Update',
      'ORDER_DELIVERED': 'Order Delivered',
      'BID_SUBMITTED': 'New Bid',
      'BID_ACCEPTED': 'Bid Accepted',
      'BID_REJECTED': 'Bid Rejected',
      'ORDER_CANCELLED': 'Order Cancelled'
    };
    const iconMap: Record<string, string> = {
      'ORDER_CREATED': 'fa-solid fa-box',
      'ORDER_STATUS_CHANGED': 'fa-solid fa-arrows-rotate',
      'DRIVER_ASSIGNED': 'fa-solid fa-truck',
      'DRIVER_LOCATION_UPDATE': 'fa-solid fa-location-dot',
      'ORDER_DELIVERED': 'fa-solid fa-circle-check',
      'BID_SUBMITTED': 'fa-solid fa-briefcase',
      'BID_ACCEPTED': 'fa-solid fa-gift',
      'BID_REJECTED': 'fa-solid fa-ban',
      'ORDER_CANCELLED': 'fa-solid fa-ban'
    };
    const typeMap: Record<string, Notification['type']> = {
      'ORDER_CREATED': 'success',
      'ORDER_STATUS_CHANGED': 'info',
      'DRIVER_ASSIGNED': 'info',
      'DRIVER_LOCATION_UPDATE': 'info',
      'ORDER_DELIVERED': 'success',
      'BID_SUBMITTED': 'info',
      'BID_ACCEPTED': 'success',
      'BID_REJECTED': 'error',
      'ORDER_CANCELLED': 'error'
    };
    const title = titleMap[event.type] || 'Order Update';
    const content = event.orderNumber
      ? `Order ${event.orderNumber} ${this.describeEvent(event)}`
      : this.describeEvent(event);
    this.notifications.unshift({
      id: Date.now() + Math.random(),
      title,
      content,
      icon: iconMap[event.type] || 'fa-solid fa-bell',
      read: false,
      timestamp: new Date(event.timestamp || Date.now()),
      type: typeMap[event.type] || 'info'
    });
    if (this.notifications.length > 20) {
      this.notifications.length = 20;
    }
  }

  private describeEvent(event: OrderRealtimeEvent): string {
    switch (event.type) {
      case 'ORDER_CREATED': return 'has been created.';
      case 'ORDER_STATUS_CHANGED': return `status changed${event.status ? ` to ${event.status}` : ''}.`;
      case 'DRIVER_ASSIGNED': return `assigned to ${event.driverName || 'a driver'}.`;
      case 'DRIVER_LOCATION_UPDATE': return 'driver location updated.';
      case 'ORDER_DELIVERED': return 'has been delivered.';
      case 'BID_SUBMITTED': return 'received a new bid.';
      case 'BID_ACCEPTED': return 'bid was accepted.';
      case 'BID_REJECTED': return 'bid was rejected.';
      case 'ORDER_CANCELLED': return 'was cancelled.';
      default: return 'received an update.';
    }
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
  
  navigateToHelp() {
    this.router.navigate(['/help']);
    this.closeAllDropdowns();
  }
  
  closeAllDropdowns() {
    this.isLanguageDropdownOpen = false;
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

    // Search index built from the real, role-restricted app navigation pages
    const navIndex: SearchResult[] = [
      { id: 'dashboard', title: 'Dashboard', description: 'Overview of your activity', icon: 'fa-solid fa-house', route: '/dashboard' },
      { id: 'orders', title: 'Orders', description: 'Track and manage delivery orders', icon: 'fa-solid fa-clipboard-list', route: '/orders' },
      { id: 'customers', title: 'Customers', description: 'Manage customer accounts', icon: 'fa-solid fa-users', route: '/customers', roles: ['SUPER_ADMIN'] },
      { id: 'vendorcompanies', title: 'Vendor Companies', description: 'View vendor companies', icon: 'fa-solid fa-building', route: '/vendorcompanies', roles: ['SUPER_ADMIN', 'VENDOR_OWNER'] },
      { id: 'deliverycompanies', title: 'Delivery Companies', description: 'View delivery companies', icon: 'fa-solid fa-truck-fast', route: '/deliverycompanies', roles: ['SUPER_ADMIN', 'DELIVERY_OWNER'] },
      { id: 'drivers', title: 'Drivers', description: 'Manage delivery drivers', icon: 'fa-solid fa-car', route: '/drivers', roles: ['SUPER_ADMIN', 'DELIVERY_OWNER'] },
      { id: 'products', title: 'Products', description: 'Browse available products', icon: 'fa-solid fa-box', route: '/products', roles: ['SUPER_ADMIN', 'VENDOR_OWNER'] },
      { id: 'partnerships', title: 'Partnerships', description: 'Vendor & delivery partnerships', icon: 'fa-solid fa-handshake', route: '/partnerships', roles: ['SUPER_ADMIN', 'VENDOR_OWNER', 'DELIVERY_OWNER'] },
      { id: 'pool', title: 'Marketplace', description: 'Browse and bid on available orders', icon: 'fa-solid fa-cart-shopping', route: '/pool', roles: ['SUPER_ADMIN', 'DELIVERY_OWNER', 'DRIVER'] },
      { id: 'bids', title: 'Bid Inbox', description: 'Review and manage received bids', icon: 'fa-solid fa-briefcase', route: '/bids', roles: ['SUPER_ADMIN', 'VENDOR_OWNER', 'CLIENT'] },
      { id: 'tracking', title: 'Tracking', description: 'Live order & driver tracking', icon: 'fa-solid fa-location-dot', route: '/tracking' },
      { id: 'profile', title: 'Profile', description: 'View your account information', icon: 'fa-solid fa-user', route: '/profile' },
      { id: 'notifications', title: 'Notifications', description: 'View your notifications', icon: 'fa-solid fa-bell', route: '/notifications' }
    ];

    const query = this.searchQuery.toLowerCase();
    const role = this.currentAuthUser?.role;
    this.searchResults = navIndex.filter(result => {
      if (result.roles && role && !result.roles.includes(role)) {
        return false;
      }
      return result.title.toLowerCase().includes(query) ||
        result.description.toLowerCase().includes(query);
    }).slice(0, 5);

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
        return 'fa-solid fa-sun';
      case 'dark':
        return 'fa-solid fa-moon';
      case 'auto':
        return 'fa-solid fa-circle-half-stroke';
      default:
        return 'fa-solid fa-sun';
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
    this.isUserDropdownOpen = false;
  }

  toggleUserDropdown(event?: Event) {
    if (event) {
      event.stopPropagation();
    }
    this.isUserDropdownOpen = !this.isUserDropdownOpen;
    // Close other dropdowns
    this.isLanguageDropdownOpen = false;
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
  
  // Notification methods
  openNotification(notification: Notification) {
    notification.read = true;
    this.isNotificationsDropdownOpen = false; // Close dropdown after selection
    // Navigate based on notification type
    if (notification.title === 'New Order' || notification.title === 'Order Update') {
      this.router.navigate(['/orders']);
    }
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
