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
    { icon: 'fa-solid fa-house', label: 'Dashboard', route: '/dashboard', badge: null, roles: null },
    { icon: 'fa-solid fa-clipboard-list', label: 'Orders', route: '/orders', badge: null, roles: null },
    { icon: 'fa-solid fa-compass', label: 'My Jobs', route: '/my-jobs', badge: null, roles: [UserRole.DRIVER] },
    { icon: 'fa-solid fa-users', label: 'Customers', route: '/customers', badge: null, roles: [UserRole.SUPER_ADMIN] },
    { icon: 'fa-solid fa-building', label: 'Vendor Companies', route: '/vendorcompanies', badge: null, roles: [UserRole.SUPER_ADMIN, UserRole.VENDOR_OWNER] },
    { icon: 'fa-solid fa-shop', label: 'Vendor Owners', route: '/vendorowners', badge: null, roles: [UserRole.SUPER_ADMIN] },
    { icon: 'fa-solid fa-truck-fast', label: 'Delivery Companies', route: '/deliverycompanies', badge: null, roles: [UserRole.SUPER_ADMIN, UserRole.DELIVERY_OWNER] },
    { icon: 'fa-solid fa-truck', label: 'Delivery Owners', route: '/deliveryowners', badge: null, roles: [UserRole.SUPER_ADMIN] },
    { icon: 'fa-solid fa-car', label: 'Drivers', route: '/drivers', badge: null, roles: [UserRole.SUPER_ADMIN, UserRole.DELIVERY_OWNER] },
    { icon: 'fa-solid fa-box', label: 'Products', route: '/products', badge: null, roles: [UserRole.SUPER_ADMIN, UserRole.VENDOR_OWNER] },
    { icon: 'fa-solid fa-handshake', label: 'Partnerships', route: '/partnerships', badge: null, roles: [UserRole.SUPER_ADMIN, UserRole.VENDOR_OWNER, UserRole.DELIVERY_OWNER] },
    { icon: 'fa-solid fa-cart-shopping', label: 'Marketplace', route: '/pool', badge: null, roles: [UserRole.SUPER_ADMIN, UserRole.DELIVERY_OWNER, UserRole.DRIVER] },
    { icon: 'fa-solid fa-briefcase', label: 'Bid Inbox', route: '/bids', badge: null, roles: [UserRole.SUPER_ADMIN, UserRole.VENDOR_OWNER, UserRole.CLIENT] },
    { icon: 'fa-solid fa-location-dot', label: 'Tracking', route: '/tracking', badge: null, roles: null },
    
    { icon: 'fa-solid fa-user', label: 'Profile', route: '/profile', badge: null, roles: null },
  ];

  menuItems: MenuItem[] = [];

  constructor(
    private sidebarService: SidebarService,
    private authService: AuthService
  ) {
    this.applyRoleMenu(this.authService.getCurrentUser());
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