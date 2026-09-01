import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { DriverPersonService, DriverPerson, PaginatedResponse, DriverPersonSearchParams } from '../../services/driver-person.service';
import { DeliveryCompanyService, DeliveryCompany } from '../../services/delivery-company.service';
import { AuthService } from '../../services/auth.service';

// DriverPerson and DeliveryCompany interfaces are now imported from services

@Component({
  selector: 'app-drivers',
  templateUrl: './drivers.component.html',
  styleUrls: ['./drivers.component.css'],
  standalone: false
})
export class DriversComponent implements OnInit, OnDestroy {
  drivers: DriverPerson[] = [];
  filteredDrivers: DriverPerson[] = [];
  searchTerm: string = '';
  statusFilter: string = 'all';
  availabilityFilter: string = 'all';
  vehicleTypeFilter: string = 'all';
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
  showPerformanceModal: boolean = false;
  selectedDriver: DriverPerson | null = null;
  expandedRows: Set<number> = new Set();
  
  // Form data
  driverForm: Partial<DriverPerson> = {
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    phoneNumber: '',
    nationalId: '',
    dateOfBirth: '',
    address: '',
    emergencyContact: '',
    licenseNumber: '',
    licenseExpiryDate: new Date().toISOString(),
    vehicleType: 'MOTORCYCLE',
    vehiclePlateNumber: '',
    vehicleModel: '',
    isAvailable: true,
    isVerified: false,
    rating: 5.0,
    totalDeliveries: 0,
    totalEarnings: 0,
    joinDate: new Date().toISOString(),
    insuranceNumber: '',
    bankAccountInfo: '',
    notes: ''
  };
  
  // Statistics
  stats = {
    total: 0,
    active: 0,
    available: 0,
    busy: 0,
    totalDeliveries: 0,
    avgRating: 0,
    totalEarnings: 0
  };
  
  vehicleTypes = [
    { label: 'All Vehicles', value: 'all' }
  ];

  get vehicleTypeFormOptions(): { label: string; value: string }[] {
    return this.vehicleTypes.filter(vt => vt.value !== 'all');
  }
  
  // deliveryCompanies removed since DriverPerson doesn't have deliveryCompanyId

  constructor(
    private driverPersonService: DriverPersonService,
    private deliveryCompanyService: DeliveryCompanyService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadDrivers();
    this.loadVehicleTypes();
    this.calculateStats();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  loadDrivers(): void {
    this.loading = true;
    this.error = null;
    
    const searchParams: DriverPersonSearchParams = {
      isAvailable: this.availabilityFilter === 'available' ? true : this.availabilityFilter === 'busy' ? false : undefined,
      vehicleType: this.vehicleTypeFilter !== 'all' ? this.vehicleTypeFilter : undefined,
      page: this.currentPage,
      size: this.pageSize,
      sortBy: this.sortBy,
      sortDir: this.sortDirection
    };
    
    // Add search term if provided
    if (this.searchTerm && this.searchTerm.trim()) {
      const term = this.searchTerm.trim();
      searchParams.firstName = term;
      searchParams.lastName = term;
      searchParams.email = term;
      searchParams.username = term;
      searchParams.vehiclePlateNumber = term;
    }
    
    const subscription = this.driverPersonService.searchDriverPersons(searchParams).subscribe({
      next: (response: PaginatedResponse<DriverPerson>) => {
        this.drivers = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.currentPage = response.number;
        this.applyFilters();
        this.calculateStats();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading drivers:', error);
        this.error = 'Failed to load drivers. Please try again.';
        this.loading = false;
      }
    });
    
    this.subscriptions.push(subscription);
  }
  
  // loadDeliveryCompanies method removed since DriverPerson doesn't have deliveryCompanyId

  loadVehicleTypes(): void {
    const params: DriverPersonSearchParams = {
      page: 0,
      size: 500,
      sortBy: 'firstName',
      sortDir: 'asc'
    };
    const subscription = this.driverPersonService.searchDriverPersons(params).subscribe({
      next: (response: PaginatedResponse<DriverPerson>) => {
        const existing = new Set(this.vehicleTypes.map(vt => vt.value));
        response.content.forEach(driver => {
          if (driver.vehicleType && !existing.has(driver.vehicleType)) {
            this.vehicleTypes.push({
              label: this.formatVehicleTypeLabel(driver.vehicleType),
              value: driver.vehicleType
            });
            existing.add(driver.vehicleType);
          }
        });
      },
      error: (error) => {
        console.error('Error loading vehicle types:', error);
      }
    });
    this.subscriptions.push(subscription);
  }

  formatVehicleTypeLabel(vehicleType: string): string {
    switch (vehicleType) {
      case 'MOTORCYCLE': return 'Motorcycle';
      case 'CAR': return 'Car';
      case 'VAN': return 'Van';
      case 'TRUCK': return 'Truck';
      default: return vehicleType.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
    }
  }

  calculateStats(): void {
    // Load stats from API using multiple endpoints
    const availableSubscription = this.driverPersonService.countAvailableDriverPersons().subscribe({
      next: (count) => {
        this.stats.available = count || 0;
      },
      error: (error) => {
        console.error('Error loading available drivers count:', error);
        this.stats.available = this.drivers.filter(d => d.isAvailable).length;
      }
    });
    this.subscriptions.push(availableSubscription);

    const verifiedSubscription = this.driverPersonService.countVerifiedDriverPersons().subscribe({
      next: (count) => {
        this.stats.active = count || 0;
      },
      error: (error) => {
        console.error('Error loading verified drivers count:', error);
        this.stats.active = this.drivers.filter(d => d.isVerified).length;
      }
    });
    this.subscriptions.push(verifiedSubscription);

    const totalDeliveriesSubscription = this.driverPersonService.getTotalDeliveries().subscribe({
      next: (total) => {
        this.stats.totalDeliveries = total || 0;
      },
      error: (error) => {
        console.error('Error loading total deliveries:', error);
        this.stats.totalDeliveries = this.drivers.reduce((sum, d) => sum + (d.totalDeliveries || 0), 0);
      }
    });
    this.subscriptions.push(totalDeliveriesSubscription);

    const avgRatingSubscription = this.driverPersonService.getAverageRating().subscribe({
      next: (rating) => {
        this.stats.avgRating = rating || 0;
      },
      error: (error) => {
        console.error('Error loading average rating:', error);
        this.stats.avgRating = this.drivers.length > 0 ? 
          this.drivers.reduce((sum, d) => sum + (d.rating || 0), 0) / this.drivers.length : 0;
      }
    });
    this.subscriptions.push(avgRatingSubscription);

    const avgEarningsSubscription = this.driverPersonService.getAverageEarnings().subscribe({
      next: (earnings) => {
        this.stats.totalEarnings = earnings || 0;
      },
      error: (error) => {
        console.error('Error loading average earnings:', error);
        this.stats.totalEarnings = this.drivers.reduce((sum, d) => sum + (d.totalEarnings || 0), 0);
      }
    });
    this.subscriptions.push(avgEarningsSubscription);

    // Calculate local stats
    this.stats.total = this.drivers.length;
    this.stats.busy = this.drivers.filter(d => !d.isAvailable && d.isVerified).length;
  }

  applyFilters(): void {
    this.filteredDrivers = this.drivers.filter(driver => {
      const matchesSearch = !this.searchTerm || 
        driver.firstName.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        driver.lastName.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        driver.email.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        driver.username.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        driver.vehiclePlateNumber?.toLowerCase().includes(this.searchTerm.toLowerCase());
      
      const matchesStatus = this.statusFilter === 'all' || 
        (this.statusFilter === 'active' && driver.isVerified) ||
        (this.statusFilter === 'inactive' && !driver.isVerified);
      
      const matchesAvailability = this.availabilityFilter === 'all' ||
        (this.availabilityFilter === 'available' && driver.isAvailable) ||
        (this.availabilityFilter === 'busy' && !driver.isAvailable);
      
      const matchesVehicleType = this.vehicleTypeFilter === 'all' || 
        driver.vehicleType === this.vehicleTypeFilter;
      
      // Remove company filter since DriverPerson doesn't have deliveryCompanyId
      
      return matchesSearch && matchesStatus && matchesAvailability && matchesVehicleType;
    });
    
    this.sortDrivers();
  }

  sortDrivers(): void {
    this.filteredDrivers.sort((a, b) => {
      let aValue: any = a[this.sortBy as keyof DriverPerson];
      let bValue: any = b[this.sortBy as keyof DriverPerson];
      
      if (typeof aValue === 'string') {
        aValue = aValue.toLowerCase();
        bValue = bValue.toLowerCase();
      }
      
      if (aValue < bValue) {
        return this.sortDirection === 'asc' ? -1 : 1;
      }
      if (aValue > bValue) {
        return this.sortDirection === 'asc' ? 1 : -1;
      }
      return 0;
    });
  }

  onSearch(searchTerm: string): void {
    this.searchTerm = searchTerm;
    this.currentPage = 0;
    this.loadDrivers();
  }

  onStatusFilter(status: string): void {
    this.statusFilter = status;
    this.currentPage = 0;
    this.loadDrivers();
  }

  onAvailabilityFilter(availability: string): void {
    this.availabilityFilter = availability;
    this.currentPage = 0;
    this.loadDrivers();
  }

  onVehicleTypeFilter(vehicleType: string): void {
    this.vehicleTypeFilter = vehicleType;
    this.currentPage = 0;
    this.loadDrivers();
  }

  // Company filter removed since DriverPerson doesn't have deliveryCompanyId

  onSort(field: string): void {
    if (this.sortBy === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortDirection = 'asc';
    }
    this.currentPage = 0;
    this.loadDrivers();
  }

  // Pagination methods
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadDrivers();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadDrivers();
  }

  goToFirstPage(): void {
    this.currentPage = 0;
    this.loadDrivers();
  }

  goToLastPage(): void {
    this.currentPage = this.totalPages - 1;
    this.loadDrivers();
  }

  goToPreviousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadDrivers();
    }
  }

  goToNextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadDrivers();
    }
  }

  openAddModal(): void {
    this.driverForm = {
      username: '',
      email: '',
      firstName: '',
      lastName: '',
      phoneNumber: '',
      nationalId: '',
      dateOfBirth: '',
      address: '',
      emergencyContact: '',
      licenseNumber: '',
      licenseExpiryDate: new Date().toISOString(),
      vehicleType: 'MOTORCYCLE',
      vehiclePlateNumber: '',
      vehicleModel: '',
      isAvailable: true,
      isVerified: false,
      rating: 5.0,
      totalDeliveries: 0,
      totalEarnings: 0,
      joinDate: new Date().toISOString(),
      insuranceNumber: '',
      bankAccountInfo: '',
      notes: ''
    };
    this.showAddModal = true;
  }

  openEditModal(driver: DriverPerson): void {
    this.selectedDriver = driver;
    this.driverForm = { ...driver };
    this.showEditModal = true;
  }

  openDetailsModal(driver: DriverPerson): void {
    this.selectedDriver = driver;
    this.showDetailsModal = true;
  }

  openPerformanceModal(driver: DriverPerson): void {
    this.selectedDriver = driver;
    this.showPerformanceModal = true;
  }

  openDeleteModal(driver: DriverPerson): void {
    this.selectedDriver = driver;
    this.showDeleteModal = true;
  }

  closeModals(): void {
    this.showAddModal = false;
    this.showEditModal = false;
    this.showDeleteModal = false;
    this.showDetailsModal = false;
    this.showPerformanceModal = false;
    this.selectedDriver = null;
    this.driverForm = {};
  }

  saveDriver(): void {
    if (this.showAddModal) {
      // Add new driver
      const driverData = this.driverForm as DriverPerson;
      const subscription = this.driverPersonService.createDriverPerson(driverData).subscribe({
        next: (newDriver) => {
          this.drivers.push(newDriver);
          this.applyFilters();
          this.calculateStats();
          this.closeModals();
        },
        error: (error) => {
          console.error('Error creating driver:', error);
          this.error = 'Failed to create driver. Please try again.';
        }
      });
      this.subscriptions.push(subscription);
    } else if (this.showEditModal && this.selectedDriver && this.selectedDriver.id) {
      // Update existing driver
      const driverData = this.driverForm as DriverPerson;
      const subscription = this.driverPersonService.updateDriverPerson(this.selectedDriver.id, driverData).subscribe({
        next: (updatedDriver) => {
          const index = this.drivers.findIndex(d => d.id === this.selectedDriver!.id);
          if (index !== -1) {
            this.drivers[index] = updatedDriver;
            this.applyFilters();
            this.calculateStats();
            this.closeModals();
          }
        },
        error: (error) => {
          console.error('Error updating driver:', error);
          this.error = 'Failed to update driver. Please try again.';
        }
      });
      this.subscriptions.push(subscription);
    }
  }

  deleteDriver(): void {
    if (this.selectedDriver && this.selectedDriver.id) {
      const subscription = this.driverPersonService.deleteDriverPerson(this.selectedDriver.id).subscribe({
        next: () => {
          this.drivers = this.drivers.filter(d => d.id !== this.selectedDriver!.id);
          this.applyFilters();
          this.calculateStats();
          this.closeModals();
        },
        error: (error) => {
          console.error('Error deleting driver:', error);
          this.error = 'Failed to delete driver. Please try again.';
        }
      });
      this.subscriptions.push(subscription);
    }
  }

  toggleDriverStatus(driver: DriverPerson): void {
    if (!driver.id) return;
    const newStatus = !driver.isVerified;
    const subscription = this.driverPersonService.updateVerificationStatus(driver.id, newStatus).subscribe({
      next: (updatedDriver) => {
        const index = this.drivers.findIndex(d => d.id === driver.id);
        if (index !== -1) {
          this.drivers[index] = updatedDriver;
          this.calculateStats();
        }
      },
      error: (error) => {
        console.error('Error updating driver status:', error);
        this.error = 'Failed to update driver status. Please try again.';
      }
    });
    this.subscriptions.push(subscription);
  }

  toggleDriverAvailability(driver: DriverPerson): void {
    if (!driver.id) return;
    const newAvailability = !driver.isAvailable;
    const subscription = this.driverPersonService.updateAvailabilityStatus(driver.id, newAvailability).subscribe({
      next: (updatedDriver) => {
        const index = this.drivers.findIndex(d => d.id === driver.id);
        if (index !== -1) {
          this.drivers[index] = updatedDriver;
          this.calculateStats();
        }
      },
      error: (error) => {
        console.error('Error updating driver availability:', error);
        this.error = 'Failed to update driver availability. Please try again.';
      }
    });
    this.subscriptions.push(subscription);
  }

  exportData(): void {
    // Implementation for exporting driver data
    console.log('Exporting drivers data...');
  }

  getStatusClass(driver: DriverPerson): string {
    if (!driver.isVerified) return 'inactive';
    if (driver.isAvailable) return 'available';
    return 'busy';
  }

  getVehicleIcon(vehicleType: string | undefined): string {
    if (!vehicleType) return 'fas fa-car';
    switch (vehicleType) {
      case 'MOTORCYCLE': return 'fas fa-motorcycle';
      case 'CAR': return 'fas fa-car';
      case 'VAN': return 'fas fa-shuttle-van';
      case 'TRUCK': return 'fas fa-truck';
      default: return 'fas fa-car';
    }
  }

  getRatingStars(rating: number | undefined): string[] {
    if (rating === undefined || rating === null) {
      return ['far fa-star', 'far fa-star', 'far fa-star', 'far fa-star', 'far fa-star'];
    }
    
    const stars = [];
    const fullStars = Math.floor(rating);
    const hasHalfStar = rating % 1 !== 0;
    
    for (let i = 0; i < fullStars; i++) {
      stars.push('fas fa-star');
    }
    
    if (hasHalfStar) {
      stars.push('fas fa-star-half-alt');
    }
    
    while (stars.length < 5) {
      stars.push('far fa-star');
    }
    
    return stars;
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
    if (!date) return 'N/A';
    const d = typeof date === 'string' ? new Date(date) : date;
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  getSuccessRate(driver: DriverPerson): number {
    if (!driver.totalDeliveries || driver.totalDeliveries === 0) return 0;
    // Since DriverPerson doesn't have completedDeliveries, we'll assume all deliveries are completed
    // This can be adjusted based on actual business logic
    return 100;
  }
}