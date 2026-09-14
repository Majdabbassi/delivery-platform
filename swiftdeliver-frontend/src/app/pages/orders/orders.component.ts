import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { OrderService, OrderDTO, OrderStatus, OrderPriority, CreateOrderDTO, OrderRatingDTO, OrderType, RoutingMode, PricingMode } from '../../services/order.service';
import { AuthService, User, UserRole } from '../../services/auth.service';
import { RealtimeService } from '../../services/realtime.service';
import { VendorCompanyService, VendorCompany } from '../../services/vendor-company.service';
import { CustomerUserService, CustomerUser } from '../../services/customer-user.service';

@Component({
  selector: 'app-orders',
  templateUrl: './orders.component.html',
  styleUrls: ['./orders.component.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule]
  })
export class OrdersComponent implements OnInit, OnDestroy {
  orders: OrderDTO[] = [];
  filteredOrders: OrderDTO[] = [];
  loading = false;
  error: string | null = null;
  
  // Filters and search
  searchTerm = '';
  statusFilter: OrderStatus | 'all' = 'all';
  priorityFilter: OrderPriority | 'all' = 'all';
  companyFilter = 'all';
  sortBy = 'orderDate';
  sortDirection: 'asc' | 'desc' = 'desc';
  
  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalPages = 0;
  totalElements = 0;
  
  // Statistics
  stats = {
    total: 0,
    pending: 0,
    inProgress: 0,
    completed: 0,
    cancelled: 0,
    overdue: 0,
    urgent: 0
  };
  
  // Modal states
  showCreateModal = false;
  showDetailsModal = false;
  showRatingModal = false;
  selectedOrder: OrderDTO | null = null;
  
  // Form data
  orderForm: CreateOrderDTO = {
    vendorCompanyId: 0,
    customerUserId: 0,
    orderType: OrderType.MARKETPLACE,
    routingMode: RoutingMode.OPEN_BID,
    pricingMode: PricingMode.FIXED,
    pickupAddress: '',
    deliveryAddress: '',
    orderValue: 0,
    priority: OrderPriority.NORMAL
  };

  // Dynamic dropdown options for the create-order form
  vendorCompanies: VendorCompany[] = [];
  customers: CustomerUser[] = [];
  
  ratingForm: OrderRatingDTO = {
    orderId: 0
  };
  
  // Enums for template
  OrderStatus = OrderStatus;
  OrderPriority = OrderPriority;
  OrderType = OrderType;
  RoutingMode = RoutingMode;
  PricingMode = PricingMode;
  UserRole = UserRole;
  
  private subscriptions = new Subscription();

  constructor(
    private orderService: OrderService,
    private authService: AuthService,
    private realtimeService: RealtimeService,
    private vendorCompanyService: VendorCompanyService,
    private customerUserService: CustomerUserService
  ) {}

  ngOnInit(): void {
    this.loadOrders();
    this.calculateStats();
    this.loadVendorCompanies();
    this.loadCustomers();
    
    // Subscribe to real-time updates
    this.subscriptions.add(
      this.orderService.orders$.subscribe(orders => {
        this.orders = orders;
        this.applyFilters();
        this.calculateStats();
      })
    );

    // Refresh the list automatically when orders change in realtime
    this.subscriptions.add(
      this.realtimeService.events$.subscribe(event => {
        if (event && ['ORDER_CREATED', 'ORDER_STATUS_CHANGED', 'DRIVER_ASSIGNED'].includes(event.type)) {
          this.loadOrders();
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  loadVendorCompanies(): void {
    const user = this.authService.getCurrentUser();
    // Only roles whose create-order flow needs vendor companies can access the list.
    // SUPER_ADMIN can list all; VENDOR_OWNER is limited to their own companies.
    if (!user) return;
    if (user.role === UserRole.CLIENT || user.role === UserRole.DELIVERY_OWNER || user.role === UserRole.DRIVER) {
      return;
    }
    if (user.role === UserRole.VENDOR_OWNER) {
      this.subscriptions.add(
        this.vendorCompanyService.getVendorCompaniesByOwner(user.id).subscribe({
          next: (companies) => {
            this.vendorCompanies = companies || [];
          },
          error: (error) => {
            console.error('Error loading vendor companies:', error);
          }
        })
      );
      return;
    }
    this.subscriptions.add(
      this.vendorCompanyService.getAllVendorCompanies(0, 500, 'name', 'asc').subscribe({
        next: (response) => {
          this.vendorCompanies = response.content;
        },
        error: (error) => {
          console.error('Error loading vendor companies:', error);
        }
      })
    );
  }

  loadCustomers(): void {
    const user = this.authService.getCurrentUser();
    // Only SUPER_ADMIN may list all customer users on the backend.
    if (!user || user.role === UserRole.CLIENT) {
      // Clients create orders for themselves; no list needed.
      const current = this.authService.getCurrentUser();
      if (current && current.role === UserRole.CLIENT) {
        this.orderForm.customerUserId = current.id;
      }
      return;
    }
    this.subscriptions.add(
      this.customerUserService.getAllCustomerUsers(0, 500, 'firstName', 'asc').subscribe({
        next: (response) => {
          this.customers = response.content;
        },
        error: (error) => {
          console.error('Error loading customers:', error);
        }
      })
    );
  }

  get currentUser(): User | null {
    return this.authService.getCurrentUser();
  }

  get isClient(): boolean {
    return this.currentUser?.role === UserRole.CLIENT;
  }

  get isMarketplace(): boolean {
    return this.orderForm.orderType === OrderType.MARKETPLACE;
  }

  get isGeneralDelivery(): boolean {
    return this.orderForm.orderType === OrderType.GENERAL_DELIVERY;
  }

  onOrderTypeChange(): void {
    if (this.isGeneralDelivery) {
      // General-delivery orders do not carry a vendor company or products.
      this.orderForm.vendorCompanyId = 0;
    }
  }

  // Data loading methods
  loadOrders(): void {
    this.loading = true;
    this.error = null;
    
    // Check if user is authenticated before making API call
    if (!this.authService.isAuthenticated()) {
      this.error = 'Please log in to view orders';
      this.loading = false;
      return;
    }
    
    const currentUser = this.authService.getCurrentUser();
    
    // SUPER_ADMIN uses the paginated global listing; all other roles use /orders/my
    if (currentUser?.role === UserRole.SUPER_ADMIN) {
      this.subscriptions.add(
        this.orderService.getAllOrders(this.currentPage, this.pageSize).subscribe({
          next: (response) => {
            this.orders = response.content;
            this.totalPages = response.totalPages;
            this.totalElements = response.totalElements;
            this.applyFilters();
            this.loading = false;
          },
          error: (error) => this.handleLoadError(error)
        })
      );
    } else {
      this.subscriptions.add(
        this.orderService.getMyOrders().subscribe({
          next: (orders) => {
            this.orders = orders;
            this.totalPages = 1;
            this.totalElements = orders.length;
            this.applyFilters();
            this.loading = false;
          },
          error: (error) => this.handleLoadError(error)
        })
      );
    }
  }

  private handleLoadError(error: any): void {
    console.error('Error loading orders:', error);
    if (error.status === 401 || error.status === 403) {
      this.error = 'Authentication failed. Please log in again.';
    } else if (error.status === 0) {
      this.error = 'Unable to connect to server. Please check your connection.';
    } else {
      this.error = `Failed to load orders: ${error.message || 'Unknown error'}`;
    }
    this.loading = false;
  }

  loadPendingUnassigned(): void {
    this.loading = true;
    
    if (!this.authService.isAuthenticated()) {
      this.error = 'Please log in to view orders';
      this.loading = false;
      return;
    }
    
    this.subscriptions.add(
      this.orderService.getPendingUnassignedOrders().subscribe({
        next: (orders) => {
          this.orders = orders;
          this.applyFilters();
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading pending orders:', error);
          if (error.status === 401 || error.status === 403) {
            this.error = 'Authentication failed. Please log in again.';
          } else if (error.status === 0) {
            this.error = 'Unable to connect to server. Please check your connection.';
          } else {
            this.error = `Failed to load pending orders: ${error.message || 'Unknown error'}`;
          }
          this.loading = false;
        }
      })
    );
  }

  loadOverdueOrders(): void {
    this.loading = true;
    
    if (!this.authService.isAuthenticated()) {
      this.error = 'Please log in to view orders';
      this.loading = false;
      return;
    }
    
    this.subscriptions.add(
      this.orderService.getOverdueOrders().subscribe({
        next: (orders) => {
          this.orders = orders;
          this.applyFilters();
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading overdue orders:', error);
          if (error.status === 401 || error.status === 403) {
            this.error = 'Authentication failed. Please log in again.';
          } else if (error.status === 0) {
            this.error = 'Unable to connect to server. Please check your connection.';
          } else {
            this.error = `Failed to load overdue orders: ${error.message || 'Unknown error'}`;
          }
          this.loading = false;
        }
      })
    );
  }

  loadUrgentOrders(): void {
    this.loading = true;
    
    if (!this.authService.isAuthenticated()) {
      this.error = 'Please log in to view orders';
      this.loading = false;
      return;
    }
    
    this.subscriptions.add(
      this.orderService.getUrgentOrders().subscribe({
        next: (orders) => {
          this.orders = orders;
          this.applyFilters();
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading urgent orders:', error);
          if (error.status === 401 || error.status === 403) {
            this.error = 'Authentication failed. Please log in again.';
          } else if (error.status === 0) {
            this.error = 'Unable to connect to server. Please check your connection.';
          } else {
            this.error = `Failed to load urgent orders: ${error.message || 'Unknown error'}`;
          }
          this.loading = false;
        }
      })
    );
  }

  // Filtering and sorting
  applyFilters(): void {
    let filtered = [...this.orders];
    
    // Search filter
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(order => 
        order.orderNumber.toLowerCase().includes(term) ||
        order.trackingNumber.toLowerCase().includes(term) ||
        order.customerName.toLowerCase().includes(term) ||
        order.pickupAddress.toLowerCase().includes(term) ||
        order.deliveryAddress.toLowerCase().includes(term)
      );
    }
    
    // Status filter
    if (this.statusFilter !== 'all') {
      filtered = filtered.filter(order => order.status === this.statusFilter);
    }
    
    // Priority filter
    if (this.priorityFilter !== 'all') {
      filtered = filtered.filter(order => order.priority === this.priorityFilter);
    }
    
    // Sort
    this.sortOrders(filtered);
    
    this.filteredOrders = filtered;
  }

  sortOrders(orders: OrderDTO[]): void {
    orders.sort((a, b) => {
      let aValue: string | number | Date = a[this.sortBy as keyof OrderDTO] as string | number | Date;
      let bValue: string | number | Date = b[this.sortBy as keyof OrderDTO] as string | number | Date;
      
      if (typeof aValue === 'string' && typeof bValue === 'string') {
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

  // Event handlers
  onSearch(term: string): void {
    this.searchTerm = term;
    this.applyFilters();
  }

  onStatusFilter(status: OrderStatus | 'all'): void {
    this.statusFilter = status;
    this.applyFilters();
  }

  onPriorityFilter(priority: OrderPriority | 'all'): void {
    this.priorityFilter = priority;
    this.applyFilters();
  }

  onSort(field: string): void {
    if (this.sortBy === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
  }

  // Pagination
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadOrders();
  }

  // Order actions
  createOrder(): void {
    if (!this.authService.canCreateOrder()) {
      this.error = 'You do not have permission to create orders';
      return;
    }
    
    this.loading = true;
    this.subscriptions.add(
      this.orderService.createOrder(this.orderForm).subscribe({
        next: (order) => {
          this.orders.unshift(order);
          this.applyFilters();
          this.closeCreateModal();
          this.loading = false;
        },
        error: (error) => {
          this.error = 'Failed to create order';
          this.loading = false;
          console.error('Error creating order:', error);
        }
      })
    );
  }

  updateOrderStatus(order: OrderDTO, status: OrderStatus): void {
    if (!this.authService.canUpdateOrderStatus()) {
      this.error = 'You do not have permission to update order status';
      return;
    }
    
    this.subscriptions.add(
      this.orderService.updateOrderStatus(order.id, status).subscribe({
        next: (updatedOrder) => {
          const index = this.orders.findIndex(o => o.id === order.id);
          if (index !== -1) {
            this.orders[index] = updatedOrder;
            this.applyFilters();
          }
        },
        error: (error) => {
          this.error = 'Failed to update order status';
          console.error('Error updating order status:', error);
        }
      })
    );
  }

  assignDeliveryCompany(order: OrderDTO, deliveryCompanyId: number): void {
    if (!this.authService.canAssignDeliveryCompany()) {
      this.error = 'You do not have permission to assign delivery companies';
      return;
    }
    
    this.subscriptions.add(
      this.orderService.assignDeliveryCompany(order.id, deliveryCompanyId).subscribe({
        next: (updatedOrder) => {
          const index = this.orders.findIndex(o => o.id === order.id);
          if (index !== -1) {
            this.orders[index] = updatedOrder;
            this.applyFilters();
          }
        },
        error: (error) => {
          this.error = 'Failed to assign delivery company';
          console.error('Error assigning delivery company:', error);
        }
      })
    );
  }

  cancelOrder(order: OrderDTO, reason?: string): void {
    if (!this.authService.canCancelOrder()) {
      this.error = 'You do not have permission to cancel orders';
      return;
    }
    
    this.subscriptions.add(
      this.orderService.cancelOrder(order.id, reason).subscribe({
        next: (updatedOrder) => {
          const index = this.orders.findIndex(o => o.id === order.id);
          if (index !== -1) {
            this.orders[index] = updatedOrder;
            this.applyFilters();
          }
        },
        error: (error) => {
          this.error = 'Failed to cancel order';
          console.error('Error cancelling order:', error);
        }
      })
    );
  }

  submitRating(): void {
    if (!this.authService.canRateOrder()) {
      this.error = 'You do not have permission to rate orders';
      return;
    }
    
    this.subscriptions.add(
      this.orderService.addOrderRating(
        this.ratingForm.orderId,
        this.ratingForm.customerRating || 1,
        this.ratingForm.customerReview
      ).subscribe({
        next: (updatedOrder) => {
          const index = this.orders.findIndex(o => o.id === updatedOrder.id);
          if (index !== -1) {
            this.orders[index] = updatedOrder;
            this.applyFilters();
          }
          this.closeRatingModal();
        },
        error: (error) => {
          this.error = 'Failed to submit rating';
          console.error('Error submitting rating:', error);
        }
      })
    );
  }

  // Modal methods
  openCreateModal(): void {
    this.showCreateModal = true;
    this.resetOrderForm();

    // Clients create orders for themselves
    const user = this.authService.getCurrentUser();
    if (user?.role === UserRole.CLIENT) {
      this.orderForm.customerUserId = user.id;
    }
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
    this.resetOrderForm();
  }

  openDetailsModal(order: OrderDTO): void {
    this.selectedOrder = order;
    this.showDetailsModal = true;
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.selectedOrder = null;
  }

  openRatingModal(order: OrderDTO): void {
    this.selectedOrder = order;
    this.ratingForm.orderId = order.id;
    this.showRatingModal = true;
  }

  closeRatingModal(): void {
    this.showRatingModal = false;
    this.selectedOrder = null;
    this.resetRatingForm();
  }

  // Helper methods
  resetOrderForm(): void {
    this.orderForm = {
      vendorCompanyId: 0,
      customerUserId: 0,
      orderType: OrderType.MARKETPLACE,
      routingMode: RoutingMode.OPEN_BID,
      pricingMode: PricingMode.FIXED,
      pickupAddress: '',
      deliveryAddress: '',
      orderValue: 0,
      priority: OrderPriority.NORMAL
    };
    const user = this.authService.getCurrentUser();
    if (user?.role === UserRole.CLIENT) {
      this.orderForm.customerUserId = user.id;
      this.orderForm.orderType = OrderType.GENERAL_DELIVERY;
    }
  }

  resetRatingForm(): void {
    this.ratingForm = {
      orderId: 0
    };
  }

  calculateStats(): void {
    this.stats = {
      total: this.orders.length,
      pending: this.orders.filter(o => o.status === OrderStatus.PENDING).length,
      inProgress: this.orders.filter(o => 
        [OrderStatus.ASSIGNED, OrderStatus.CONFIRMED, OrderStatus.IN_PROGRESS, 
         OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT].includes(o.status)
      ).length,
      completed: this.orders.filter(o => o.status === OrderStatus.DELIVERED).length,
      cancelled: this.orders.filter(o => o.status === OrderStatus.CANCELLED).length,
      overdue: this.orders.filter(o => o.isOverdue).length,
      urgent: this.orders.filter(o => o.priority === OrderPriority.URGENT).length
    };
  }

  // Utility methods for templates
  getStatusClass(status: OrderStatus): string {
    return this.orderService.getStatusClass(status);
  }

  getPriorityClass(priority: OrderPriority): string {
    return this.orderService.getPriorityClass(priority);
  }

  getStatusDisplayName(status: OrderStatus): string {
    return this.orderService.getStatusDisplayName(status);
  }

  getPriorityDisplayName(priority: OrderPriority): string {
    return this.orderService.getPriorityDisplayName(priority);
  }

  canUpdateStatus(): boolean {
    return this.authService.canUpdateOrderStatus();
  }

  canAssignCompany(): boolean {
    return this.authService.canAssignDeliveryCompany();
  }

  canCancel(): boolean {
    return this.authService.canCancelOrder();
  }

  canRate(): boolean {
    return this.authService.canRateOrder();
  }

  canCreate(): boolean {
    return this.authService.canCreateOrder();
  }

  exportData(): void {
    // Implement export functionality
    const dataStr = JSON.stringify(this.filteredOrders, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `orders_${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    URL.revokeObjectURL(url);
  }

  refreshOrders(): void {
    this.orderService.refreshOrders();
  }
}