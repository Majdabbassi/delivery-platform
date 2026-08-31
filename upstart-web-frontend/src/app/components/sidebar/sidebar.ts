import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { SidebarService } from '../../services/sidebar.service';
import { AuthService, User, UserRole } from '../../services/auth.service';

interface MenuItem {
  icon: string;
  label: string;
  route: string;
  badge: number | null;
  roles: UserRole[] | null;
}

@Component({
  selector: 'app-sidebar',
  standalone: false,
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class Sidebar implements OnInit, OnDestroy {
  currentUser: User | null = null;
  userDisplayName = '';
  userRole = '';

  isCollapsed = false;
  private sidebarSubscription: Subscription = new Subscription();
  private userSubscription: Subscription = new Subscription();

  private fullMenuItems: MenuItem[] = [
    { icon: '🏠', label: 'Dashboard', route: '/dashboard', badge: null, roles: [UserRole.SUPER_ADMIN] },
    { icon: '📋', label: 'Orders', route: '/orders', badge: null, roles: null },
    { icon: '👥', label: 'Customers', route: '/customers', badge: null, roles: [UserRole.SUPER_ADMIN] },
    { icon: '🏢', label: 'Vendor Companies', route: '/vendorcompanies', badge: null, roles: [UserRole.SUPER_ADMIN, UserRole.VENDOR_OWNER] },
    { icon: '🏪', label: 'Vendor Owners', route: '/vendorowners', badge: null, roles: [UserRole.SUPER_ADMIN] },
    { icon: '🚛', label: 'Delivery Companies', route: '/deliverycompanies', badge: null, roles: [UserRole.SUPER_ADMIN, UserRole.DELIVERY_OWNER] },
    { icon: '🚚', label: 'Delivery Owners', route: '/deliveryowners', badge: null, roles: [UserRole.SUPER_ADMIN] },
    { icon: '🚗', label: 'Drivers', route: '/drivers', badge: null, roles: [UserRole.SUPER_ADMIN, UserRole.DELIVERY_OWNER] },
    { icon: '📦', label: 'Products', route: '/products', badge: null, roles: null },
    { icon: '🔐', label: 'Admins Management', route: '/admins', badge: null, roles: [UserRole.SUPER_ADMIN] },
    { icon: '⚙️', label: 'Settings', route: '/settings', badge: null, roles: null },
    { icon: '👤', label: 'Profile', route: '/profile', badge: null, roles: null },
  ];

  menuItems: MenuItem[] = [];

  quickActions = [
    { id: 'new-project', icon: '➕', label: 'New Project', tooltip: 'Create a new project' },
    { id: 'upload', icon: '📤', label: 'Upload', tooltip: 'Upload files' },
    { id: 'export', icon: '📥', label: 'Export', tooltip: 'Export data' },
    { id: 'backup', icon: '💾', label: 'Backup', tooltip: 'Create backup' }
  ];

  constructor(
    private sidebarService: SidebarService,
    private authService: AuthService
  ) {
    this.applyRoleMenu(this.authService.getCurrentUser());
  }

  executeAction(actionId: string) {
    console.log('Executing action:', actionId);
  }

  ngOnInit(): void {
    this.sidebarSubscription = this.sidebarService.isCollapsed$.subscribe(
      (collapsed) => {
        this.isCollapsed = collapsed;
      }
    );

    this.userSubscription = this.authService.currentUser$.subscribe(
      (user) => {
        this.currentUser = user;
        if (user) {
          this.userDisplayName = `${user.firstName} ${user.lastName}`;
          this.userRole = this.formatUserRole(user.role);
        } else {
          this.userDisplayName = '';
          this.userRole = '';
        }
        this.applyRoleMenu(user);
      }
    );
  }

  ngOnDestroy(): void {
    this.sidebarSubscription.unsubscribe();
    this.userSubscription.unsubscribe();
  }

  private applyRoleMenu(user: User | null): void {
    if (!user) {
      this.menuItems = this.fullMenuItems.filter(item => !item.roles);
      return;
    }
    if (user.role === UserRole.SUPER_ADMIN) {
      this.menuItems = this.fullMenuItems;
      return;
    }
    this.menuItems = this.fullMenuItems.filter(item => !item.roles || item.roles.includes(user.role));
  }

  private formatUserRole(role: string): string {
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

  getUserInitials(): string {
    if (!this.currentUser) return '??';
    const firstInitial = this.currentUser.firstName?.charAt(0) || '';
    const lastInitial = this.currentUser.lastName?.charAt(0) || '';
    return (firstInitial + lastInitial).toUpperCase() || '??';
  }

  toggleSidebar() {
    this.sidebarService.toggleSidebar();
  }
}