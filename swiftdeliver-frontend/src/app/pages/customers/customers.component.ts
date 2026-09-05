import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { CustomerUserService, CustomerUser } from '../../services/customer-user.service';
import { PaginatedResponse as CustomerUserPaginatedResponse } from '../../models/paginated-response';
import { AuthService } from '../../services/auth.service';

// Customer interface is now imported from CustomerService

@Component({
  selector: 'app-customers',
  templateUrl: './customers.component.html',
  styleUrls: ['./customers.component.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule]
  })
export class CustomersComponent implements OnInit, OnDestroy {
  customers: CustomerUser[] = [];
  filteredCustomers: CustomerUser[] = [];
  searchTerm: string = '';
  statusFilter: string = 'all';
  roleFilter: string = 'all';
  premiumFilter: string = 'all';
  sortBy: string = 'firstName';
  sortDirection: 'asc' | 'desc' = 'asc';
  
  // Pagination
  currentPage: number = 0;
  pageSize: number = 10;
  totalElements: number = 0;
  totalPages: number = 0;
  
  // Loading states
  loading: boolean = false;
  error: string | null = null;
  
  // Subscriptions
  private subscriptions: Subscription[] = [];
  
  // Modal states
  showAddModal: boolean = false;
  showEditModal: boolean = false;
  showDeleteModal: boolean = false;
  showDetailsModal: boolean = false;
  showAnalyticsModal: boolean = false;
  selectedCustomer: CustomerUser | null = null;
  
  // Form data
  customerForm: Partial<CustomerUser> = {
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    phoneNumber: '',
    role: 'CLIENT',
    isEnabled: true,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    defaultAddress: '',
    loyaltyPoints: 0,
    preferredPaymentMethod: '',
    totalOrders: 0,
    totalSpent: 0,
    dateOfBirth: '',
    gender: undefined,
    notificationPreferences: JSON.stringify({
      email: true,
      sms: false,
      push: true,
      marketing: false
    }),
    isPremium: false
  };
  
  // Statistics
  stats = {
    total: 0,
    enabled: 0,
    disabled: 0,
    premium: 0,
    totalRevenue: 0,
    totalLoyaltyPoints: 0
  };
  
  roles: string[] = [];
  paymentMethods = ['CARD', 'CASH', 'WALLET', 'BANK_TRANSFER'];
  genders = ['MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'];

  constructor(
    private customerUserService: CustomerUserService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadCustomers();
    this.loadRoles();
    this.calculateStats();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  loadCustomers(): void {
    this.loading = true;
    this.error = null;
    const searchParams: any = {
      enabled: this.statusFilter === 'enabled' ? true : this.statusFilter === 'disabled' ? false : undefined,
      isPremium: this.premiumFilter === 'premium' ? true : this.premiumFilter === 'regular' ? false : undefined,
      page: this.currentPage,
      size: this.pageSize,
      sortBy: this.sortBy,
      sortDir: this.sortDirection
    };
    if (this.searchTerm && this.searchTerm.trim()) {
      searchParams.username = this.searchTerm.trim();
      searchParams.email = this.searchTerm.trim();
      searchParams.firstName = this.searchTerm.trim();
      searchParams.lastName = this.searchTerm.trim();
    }
    const subscription = this.customerUserService.searchCustomerUsers(searchParams).subscribe({
      next: (response: CustomerUserPaginatedResponse<CustomerUser>) => {
        this.customers = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.currentPage = response.number;
        this.applyFilters();
        this.calculateStats();
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Failed to load customers. Please try again.';
        this.loading = false;
        console.error('Error loading customers:', error);
      }
    });
    this.subscriptions.push(subscription);
  }

  loadRoles(): void {
    const subscription = this.customerUserService.getAllCustomerUsers(0, 500, 'firstName', 'asc').subscribe({
      next: (response: CustomerUserPaginatedResponse<CustomerUser>) => {
        const distinct = Array.from(new Set(
          response.content.map(c => c.role).filter((r): r is string => !!r)
        ));
        if (distinct.length) {
          this.roles = distinct.sort();
        }
      },
      error: (error) => {
        console.error('Error loading customer roles:', error);
        this.roles = ['CLIENT', 'VENDOR_OWNER', 'DELIVERY_OWNER', 'DRIVER'];
      }
    });
    this.subscriptions.push(subscription);
  }

  applyFilters(): void {
    this.filteredCustomers = this.customers.filter(customer => {
      const matchesSearch = customer.firstName.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
                           customer.lastName.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
                           customer.email.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
                           customer.username.toLowerCase().includes(this.searchTerm.toLowerCase());
      
      const matchesStatus = this.statusFilter === 'all' || 
                           (this.statusFilter === 'enabled' && customer.isEnabled) ||
                           (this.statusFilter === 'disabled' && !customer.isEnabled);
      
      const matchesRole = this.roleFilter === 'all' || customer.role === this.roleFilter;
      const matchesPremium = this.premiumFilter === 'all' || 
                            (this.premiumFilter === 'premium' && customer.isPremium) ||
                            (this.premiumFilter === 'regular' && !customer.isPremium);
      
      return matchesSearch && matchesStatus && matchesRole && matchesPremium;
    });
    
    this.sortCustomers();
  }

  sortCustomers(): void {
    this.filteredCustomers.sort((a, b) => {
      let aValue: any = a[this.sortBy as keyof CustomerUser];
      let bValue: any = b[this.sortBy as keyof CustomerUser];
      
      if (typeof aValue === 'string') {
        aValue = aValue.toLowerCase();
        bValue = bValue.toLowerCase();
      }
      
      if (this.sortDirection === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });
  }

  onSearch(term: string): void {
    this.searchTerm = term;
    this.currentPage = 0;
    this.loadCustomers();
  }

  onStatusFilter(status: string): void {
    this.statusFilter = status;
    this.currentPage = 0;
    this.loadCustomers();
  }

  onRoleFilter(role: string): void {
    this.roleFilter = role;
    this.currentPage = 0;
    this.loadCustomers();
  }

  onPremiumFilter(premium: string): void {
    this.premiumFilter = premium;
    this.currentPage = 0;
    this.loadCustomers();
  }

  onSort(field: string): void {
    if (this.sortBy === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortDirection = 'asc';
    }
    this.currentPage = 0;
    this.loadCustomers();
  }

  // Pagination methods
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadCustomers();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadCustomers();
  }

  goToFirstPage(): void {
    this.currentPage = 0;
    this.loadCustomers();
  }

  goToLastPage(): void {
    this.currentPage = this.totalPages - 1;
    this.loadCustomers();
  }

  goToPreviousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadCustomers();
    }
  }

  goToNextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadCustomers();
    }
  }

  calculateStats(): void {
    const subscription = this.customerUserService.getCustomerUserStatistics().subscribe({
      next: (counts) => {
        this.stats.total = counts.total || 0;
        this.stats.enabled = counts.active || 0;
        this.stats.disabled = (counts.total || 0) - (counts.active || 0);
        this.stats.premium = counts.premium || 0;
        this.stats.totalRevenue = this.customers.reduce((sum, c) => sum + (c.totalSpent || 0), 0);
        this.stats.totalLoyaltyPoints = this.customers.reduce((sum, c) => sum + (c.loyaltyPoints || 0), 0);
      },
      error: (error) => {
        this.stats.total = this.customers.length;
        this.stats.enabled = this.customers.filter(c => c.isEnabled).length;
        this.stats.disabled = this.customers.filter(c => !c.isEnabled).length;
        this.stats.premium = this.customers.filter(c => c.isPremium).length;
        this.stats.totalRevenue = this.customers.reduce((sum, c) => sum + (c.totalSpent || 0), 0);
        this.stats.totalLoyaltyPoints = this.customers.reduce((sum, c) => sum + (c.loyaltyPoints || 0), 0);
      }
    });
    this.subscriptions.push(subscription);
  }

  // Modal operations
  openAddModal(): void {
    this.customerForm = {
      username: '',
      email: '',
      password: '',
      firstName: '',
      lastName: '',
      phoneNumber: '',
      role: 'CLIENT',
      isEnabled: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      defaultAddress: '',
      loyaltyPoints: 0,
      preferredPaymentMethod: '',
      totalOrders: 0,
      totalSpent: 0,
      dateOfBirth: '',
      gender: undefined,
      notificationPreferences: JSON.stringify({
        email: true,
        sms: false,
        push: true,
        marketing: false
      }),
      isPremium: false
    };
    this.showAddModal = true;
  }

  openEditModal(customer: CustomerUser): void {
    this.selectedCustomer = customer;
    this.customerForm = { ...customer };
    this.showEditModal = true;
  }

  openDeleteModal(customer: CustomerUser): void {
    this.selectedCustomer = customer;
    this.showDeleteModal = true;
  }

  openDetailsModal(customer: CustomerUser): void {
    this.selectedCustomer = customer;
    this.showDetailsModal = true;
  }

  openAnalyticsModal(customer: CustomerUser): void {
    this.selectedCustomer = customer;
    this.showAnalyticsModal = true;
  }

  // Remove all references to customerService, use customerUserService only
  // Fix callback parameter types
  toggleCustomerStatus(customer: CustomerUser): void {
    if (!customer.id) {
      console.error('Customer ID is required for status update');
      return;
    }
    const newStatus = !customer.isEnabled;
    // Assuming updateEnabledStatus exists in CustomerUserService, otherwise implement accordingly
    const subscription = this.customerUserService.updateCustomerUser(customer.id, { ...customer, isEnabled: newStatus }).subscribe({
      next: (updatedCustomer: CustomerUser) => {
        const index = this.customers.findIndex(c => c.id === customer.id);
        if (index !== -1) {
          this.customers[index] = updatedCustomer;
          this.applyFilters();
          this.calculateStats();
        }
      },
      error: (error: any) => {
        console.error('Error updating customer status:', error);
        this.error = 'Failed to update customer status. Please try again.';
      }
    });
    this.subscriptions.push(subscription);
  }

  closeModals(): void {
    this.showAddModal = false;
    this.showEditModal = false;
    this.showDeleteModal = false;
    this.showDetailsModal = false;
    this.showAnalyticsModal = false;
    this.selectedCustomer = null;
  }

  saveCustomer(): void {
    if (this.showAddModal) {
      const customerData = this.customerForm as CustomerUser;
      const subscription = this.customerUserService.createCustomerUser(customerData).subscribe({
        next: (newCustomer) => {
          this.customers.push(newCustomer);
          this.applyFilters();
          this.calculateStats();
          this.closeModals();
        },
        error: (error) => {
          this.error = 'Failed to create customer. Please try again.';
        }
      });
      this.subscriptions.push(subscription);
    } else if (this.showEditModal && this.selectedCustomer) {
      const customerData = this.customerForm as CustomerUser;
      if (!this.selectedCustomer?.id) {
        return;
      }
      const subscription = this.customerUserService.updateCustomerUser(this.selectedCustomer.id, customerData).subscribe({
        next: (updatedCustomer) => {
          const index = this.customers.findIndex(c => c.id === this.selectedCustomer?.id);
          if (index !== -1) {
            this.customers[index] = updatedCustomer;
            this.applyFilters();
            this.calculateStats();
            this.closeModals();
          }
        },
        error: (error) => {
          this.error = 'Failed to update customer. Please try again.';
        }
      });
      this.subscriptions.push(subscription);
    }
  }

  deleteCustomer(): void {
    if (this.selectedCustomer?.id) {
      const subscription = this.customerUserService.deleteCustomerUser(this.selectedCustomer.id).subscribe({
        next: () => {
          this.customers = this.customers.filter(c => c.id !== this.selectedCustomer?.id);
          this.applyFilters();
          this.calculateStats();
          this.closeModals();
        },
        error: (error) => {
          this.error = 'Failed to delete customer. Please try again.';
        }
      });
      this.subscriptions.push(subscription);
    }
  }

  exportData(): void {
    const dataStr = JSON.stringify(this.filteredCustomers, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'customers-data.json';
    link.click();
    URL.revokeObjectURL(url);
  }

  formatCurrency(amount: number | undefined): string {
    if (amount === undefined || amount === null) {
      return '$0.00';
    }
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  }

  formatDate(date: Date | string | undefined): string {
    if (!date) {
      return 'N/A';
    }
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    }).format(new Date(date));
  }

  getStatusClass(isEnabled: boolean): string {
    return isEnabled ? 'status-enabled' : 'status-disabled';
  }

  getRoleIcon(role: string): string {
    switch(role) {
      case 'VENDOR_OWNER': return 'fa-solid fa-shop';
      case 'DELIVERY_OWNER': return 'fa-solid fa-truck';
      case 'DRIVER': return 'fa-solid fa-car';
      default: return 'fa-solid fa-user';
    }
  }

  getPremiumBadge(isPremium: boolean): string {
    return isPremium ? 'fa-solid fa-star' : '';
  }

  parseNotificationPreferences(preferences: string): any {
    try {
      return JSON.parse(preferences || '{}');
    } catch {
      return {};
    }
  }

  getFullName(customer: CustomerUser): string {
    return `${customer.firstName} ${customer.lastName}`;
  }
}