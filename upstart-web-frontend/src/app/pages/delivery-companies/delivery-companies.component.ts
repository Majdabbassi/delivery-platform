import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { DeliveryCompanyService, DeliveryCompany, DeliveryCompanySearchParams, DeliveryCompanyStats } from '../../services/delivery-company.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-delivery-companies',
  templateUrl: './delivery-companies.component.html',
  styleUrls: ['./delivery-companies.component.css'],
  standalone: false
})
export class DeliveryCompaniesComponent implements OnInit, OnDestroy {
  deliveryCompanies: DeliveryCompany[] = [];
  filteredDeliveryCompanies: DeliveryCompany[] = [];
  searchTerm: string = '';
  statusFilter: string = 'all';
  serviceTypeFilter: string = 'all';
  sortBy: string = 'companyName';
  sortDirection: 'asc' | 'desc' = 'asc';
  
  // Pagination
  currentPage: number = 0;
  pageSize: number = 10;
  totalElements: number = 0;
  totalPages: number = 0;
  
  // Loading states
  loading: boolean = false;
  error: string | null = null;
  
  // Modal states
  showAddModal: boolean = false;
  showEditModal: boolean = false;
  showDeleteModal: boolean = false;
  showDetailsModal: boolean = false;
  showAnalyticsModal: boolean = false;
  selectedDeliveryCompany: DeliveryCompany | null = null;
  
  // Form data
  deliveryCompanyForm: Partial<DeliveryCompany> = {
    name: '',
    email: '',
    phone: '',
    address: '',
    website: '',
    serviceType: '',
    status: 'ACTIVE',
    contactPerson: '',
    contractStartDate: new Date().toISOString().split('T')[0],
    coverageAreas: [],
    vehicleTypes: [],
    rating: 5
  };
  
  // Statistics
  stats: DeliveryCompanyStats = {
    total: 0,
    active: 0,
    inactive: 0,
    verified: 0,
    activeAndVerified: 0,
    highRated: 0,
    withMultipleVehicles: 0,
    pending: 0,
    totalRevenue: 0
  };
  
  private subscriptions: Subscription[] = [];
  
  serviceTypes = [
    'Express Delivery',
    'Standard Delivery',
    'Same Day Delivery',
    'International Shipping',
    'Freight Transport',
    'Last Mile Delivery',
    'Cold Chain Logistics',
    'E-commerce Fulfillment'
  ];

  vehicleTypeOptions = [
    'Van',
    'Truck',
    'Motorcycle',
    'Bicycle',
    'Drone',
    'Ship',
    'Airplane'
  ];

  constructor(
    private deliveryCompanyService: DeliveryCompanyService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadDeliveryCompanies();
    this.calculateStats();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  loadDeliveryCompanies(): void {
    this.loading = true;
    this.error = null;
    
    const searchParams: DeliveryCompanySearchParams = {
      page: this.currentPage,
      size: this.pageSize,
      sortBy: this.sortBy,
      sortDir: this.sortDirection
    };
    
    // Add filters to search params
    if (this.searchTerm) {
      searchParams.name = this.searchTerm;
      searchParams.email = this.searchTerm;
      searchParams.contactPerson = this.searchTerm;
    }
    
    if (this.statusFilter !== 'all') {
      searchParams.status = this.statusFilter.toUpperCase() as any;
    }
    
    if (this.serviceTypeFilter !== 'all') {
      searchParams.serviceType = this.serviceTypeFilter;
    }
    
    const subscription = this.deliveryCompanyService.searchDeliveryCompanies(searchParams).subscribe({
      next: (response) => {
        this.deliveryCompanies = response.content;
        this.filteredDeliveryCompanies = [...this.deliveryCompanies];
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading delivery companies:', error);
        this.error = 'Failed to load delivery companies. Please try again.';
        this.loading = false;
      }
    });
    
    this.subscriptions.push(subscription);
  }



  onSearch(term: string): void {
    this.searchTerm = term;
    this.currentPage = 0;
    this.loadDeliveryCompanies();
  }

  onStatusFilter(status: string): void {
    this.statusFilter = status;
    this.currentPage = 0;
    this.loadDeliveryCompanies();
  }

  onServiceTypeFilter(serviceType: string): void {
    this.serviceTypeFilter = serviceType;
    this.currentPage = 0;
    this.loadDeliveryCompanies();
  }

  onSort(field: string): void {
    if (this.sortBy === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortDirection = 'asc';
    }
    this.currentPage = 0;
    this.loadDeliveryCompanies();
  }

  calculateStats(): void {
    const subscription = this.deliveryCompanyService.getDeliveryCompanyStats().subscribe({
      next: (stats) => {
        this.stats = stats;
      },
      error: (error) => {
        console.error('Error loading delivery company statistics:', error);
        // Fallback to local calculation if API fails
        this.stats = {
          total: this.deliveryCompanies.length,
          active: this.deliveryCompanies.filter(c => c.status === 'ACTIVE').length,
          inactive: this.deliveryCompanies.filter(c => c.status === 'INACTIVE').length,
          verified: this.deliveryCompanies.filter(c => c.isVerified === true).length,
          activeAndVerified: this.deliveryCompanies.filter(c => c.status === 'ACTIVE' && c.isVerified === true).length,
          highRated: this.deliveryCompanies.filter(c => (c.rating || 0) >= 4.5).length,
          withMultipleVehicles: this.deliveryCompanies.filter(c => (c.vehicleTypes?.length || 0) > 1).length,
          pending: this.deliveryCompanies.filter(c => c.status === 'PENDING').length,
          totalRevenue: this.deliveryCompanies.reduce((sum, c) => sum + (c.totalRevenue || 0), 0)
        };
      }
    });
    
    this.subscriptions.push(subscription);
  }

  // Pagination methods
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadDeliveryCompanies();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadDeliveryCompanies();
  }

  goToFirstPage(): void {
    this.currentPage = 0;
    this.loadDeliveryCompanies();
  }

  goToLastPage(): void {
    this.currentPage = this.totalPages - 1;
    this.loadDeliveryCompanies();
  }

  goToPreviousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadDeliveryCompanies();
    }
  }

  goToNextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadDeliveryCompanies();
    }
  }

  // Modal operations
  openAddModal(): void {
    this.deliveryCompanyForm = {
      name: '',
      email: '',
      phone: '',
      address: '',
      website: '',
      serviceType: '',
      status: 'ACTIVE' as 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'SUSPENDED',
      contactPerson: '',
      contractStartDate: new Date().toISOString().split('T')[0],
      coverageAreas: [],
      vehicleTypes: [],
      rating: 5,
      notes: ''
    };
    this.showAddModal = true;
  }

  openEditModal(company: DeliveryCompany): void {
    this.selectedDeliveryCompany = company;
    this.deliveryCompanyForm = { ...company };
    this.showEditModal = true;
  }

  openDeleteModal(company: DeliveryCompany): void {
    this.selectedDeliveryCompany = company;
    this.showDeleteModal = true;
  }

  openDetailsModal(company: DeliveryCompany): void {
    this.selectedDeliveryCompany = company;
    this.showDetailsModal = true;
  }

  openAnalyticsModal(company: DeliveryCompany): void {
    this.selectedDeliveryCompany = company;
    this.showAnalyticsModal = true;
  }

  toggleCompanyStatus(company: DeliveryCompany): void {
    const index = this.deliveryCompanies.findIndex(c => c.id === company.id);
    if (index !== -1) {
      // Toggle between active and inactive
      if (this.deliveryCompanies[index].status === 'ACTIVE') {
        this.deliveryCompanies[index].status = 'INACTIVE';
      } else {
        this.deliveryCompanies[index].status = 'ACTIVE';
      }
      this.loadDeliveryCompanies();
      this.calculateStats();
    }
  }

  closeModals(): void {
    this.showAddModal = false;
    this.showEditModal = false;
    this.showDeleteModal = false;
    this.showDetailsModal = false;
    this.showAnalyticsModal = false;
    this.selectedDeliveryCompany = null;
  }

  saveDeliveryCompany(): void {
    if (this.showAddModal) {
      // Add new delivery company
      const newCompany = {
        ...this.deliveryCompanyForm,
        status: 'PENDING' as 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'SUSPENDED'
      };
      
      const subscription = this.deliveryCompanyService.createDeliveryCompany(newCompany as DeliveryCompany).subscribe({
        next: (createdCompany) => {
          this.closeModals();
          this.loadDeliveryCompanies();
          this.calculateStats();
        },
        error: (error) => {
          console.error('Error creating delivery company:', error);
          this.error = 'Failed to create delivery company. Please try again.';
        }
      });
      
      this.subscriptions.push(subscription);
    } else if (this.showEditModal && this.selectedDeliveryCompany) {
      // Update existing delivery company
      const updateData = {
        ...this.selectedDeliveryCompany,
        ...this.deliveryCompanyForm
      };
      
      const subscription = this.deliveryCompanyService.updateDeliveryCompany(this.selectedDeliveryCompany.id!, updateData as DeliveryCompany).subscribe({
        next: (updatedCompany) => {
          this.closeModals();
          this.loadDeliveryCompanies();
          this.calculateStats();
        },
        error: (error) => {
          console.error('Error updating delivery company:', error);
          this.error = 'Failed to update delivery company. Please try again.';
        }
      });
      
      this.subscriptions.push(subscription);
    }
  }

  deleteDeliveryCompany(): void {
    if (this.selectedDeliveryCompany) {
      const subscription = this.deliveryCompanyService.hardDeleteDeliveryCompany(this.selectedDeliveryCompany.id!).subscribe({
        next: () => {
          this.closeModals();
          this.loadDeliveryCompanies();
          this.calculateStats();
        },
        error: (error) => {
          console.error('Error deleting delivery company:', error);
          this.error = 'Failed to delete delivery company. Please try again.';
        }
      });
      
      this.subscriptions.push(subscription);
    }
  }

  exportData(): void {
    const dataStr = JSON.stringify(this.filteredDeliveryCompanies, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'delivery-companies-data.json';
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

  getStatusClass(status: string): string {
    switch (status?.toLowerCase()) {
      case 'active': return 'status-active';
      case 'inactive': return 'status-inactive';
      case 'pending': return 'status-pending';
      case 'suspended': return 'status-suspended';
      default: return '';
    }
  }

  getServiceTypeIcon(serviceType: string): string {
    const icons: { [key: string]: string } = {
      'Express Delivery': '⚡',
      'Standard Delivery': '📦',
      'Same Day Delivery': '🚀',
      'International Shipping': '🌍',
      'Freight Transport': '🚛',
      'Last Mile Delivery': '🏠',
      'Cold Chain Logistics': '❄️',
      'E-commerce Fulfillment': '🛒'
    };
    return icons[serviceType] || '🚚';
  }

  getRatingStars(rating: number | undefined): string {
    if (rating === undefined || rating === null) {
      return '☆☆☆☆☆';
    }
    const fullStars = Math.floor(rating);
    const hasHalfStar = rating % 1 !== 0;
    let stars = '★'.repeat(fullStars);
    if (hasHalfStar) stars += '☆';
    return stars + '☆'.repeat(5 - Math.ceil(rating));
  }

  updateCoverageAreas(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.deliveryCompanyForm.coverageAreas = target.value.split(',').map((area: string) => area.trim()).filter(area => area.length > 0);
  }

  updateVehicleTypes(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.deliveryCompanyForm.vehicleTypes = target.value.split(',').map((vehicle: string) => vehicle.trim()).filter(vehicle => vehicle.length > 0);
  }
}