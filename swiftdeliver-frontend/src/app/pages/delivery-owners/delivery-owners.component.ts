import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { DeliveryOwner, DeliveryOwnerService, DeliveryOwnerSearchParams } from '../../services/delivery-owner.service';
import { AuthService } from '../../services/auth.service';

// Extended DeliveryOwner interface for component use
interface DeliveryOwnerForm extends Partial<DeliveryOwner> {
  password?: string;
}

interface DeliveryCompany {
  id: number;
  companyName: string;
  companyAddress: string;
  operatingLicense: string;
  contactPhone: string;
  contactEmail: string;
  serviceRegion: string;
  isLicensed: boolean;
  isActive: boolean;
  maxDrivers: number;
  activeDriversCount: number;
  totalDeliveriesManaged: number;
  totalRevenue: number;
  commissionRate: number;
  rating: number;
  registrationDate: Date;
  lastDeliveryDate?: Date;
}

@Component({
  selector: 'app-delivery-owners',
  templateUrl: './delivery-owners.component.html',
  styleUrls: ['./delivery-owners.component.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule]
  })
export class DeliveryOwnersComponent implements OnInit, OnDestroy {
  deliveryOwners: DeliveryOwner[] = [];
  filteredDeliveryOwners: DeliveryOwner[] = [];
  searchTerm: string = '';
  statusFilter: string = 'all';
  verificationFilter: string = 'all';
  experienceFilter: string = 'all';
  sortBy: string = 'firstName';
  sortDirection: 'asc' | 'desc' = 'asc';
  
  // Pagination
  currentPage: number = 0;
  pageSize: number = 10;
  totalElements: number = 0;
  totalPages: number = 0;
  
  // Loading and error states
  loading: boolean = false;
  error: string = '';
  
  // Subscriptions
  private subscriptions: Subscription[] = [];
  
  // Modal states
  showAddModal: boolean = false;
  showEditModal: boolean = false;
  showDeleteModal: boolean = false;
  showDetailsModal: boolean = false;
  showCompaniesModal: boolean = false;
  selectedOwner: DeliveryOwner | null = null;
  expandedRows: Set<number> = new Set();
  
  // Form data
  ownerForm: DeliveryOwnerForm = {
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    phoneNumber: '',
    enabled: true,
    nationalId: '',
    dateOfBirth: '',
    address: '',
    emergencyContact: '',
    businessExperience: 0,
    preferredBusinessCategory: '',
    verified: false,
    businessLicense: '',
    isActive: true
  };
  
  // Statistics
  stats = {
    total: 0,
    verified: 0,
    unverified: 0,
    active: 0,
    totalCompanies: 0,
    totalRevenue: 0,
    avgExperience: 0
  };
  
  experienceRanges = [
    { label: 'All Experience', value: 'all' },
    { label: '0-2 years', value: '0-2' },
    { label: '3-5 years', value: '3-5' },
    { label: '6-10 years', value: '6-10' },
    { label: '10+ years', value: '10+' }
  ];

  constructor(
    private deliveryOwnerService: DeliveryOwnerService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadDeliveryOwners();
    this.calculateStats();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  loadDeliveryOwners(): void {
    this.loading = true;
    this.error = '';
    
    const searchParams: DeliveryOwnerSearchParams = {
      page: this.currentPage,
      size: this.pageSize,
      sortBy: this.sortBy,
      sortDir: this.sortDirection
    };
    
    // Add filters if they are set
    if (this.searchTerm) {
      searchParams.firstName = this.searchTerm;
      searchParams.lastName = this.searchTerm;
      searchParams.username = this.searchTerm;
      searchParams.email = this.searchTerm;
    }
    
    if (this.statusFilter !== 'all') {
      searchParams.enabled = this.statusFilter === 'enabled';
    }
    
    if (this.verificationFilter !== 'all') {
      searchParams.verified = this.verificationFilter === 'verified';
    }
    
    if (this.experienceFilter !== 'all') {
      const [min, max] = this.experienceFilter.split('-').map(Number);
      if (!isNaN(min)) {
        searchParams.minBusinessExperience = min;
      }
    }
    
    const subscription = this.deliveryOwnerService.searchDeliveryOwners(searchParams).subscribe({
      next: (response) => {
        this.deliveryOwners = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.filteredDeliveryOwners = [...this.deliveryOwners];
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading delivery owners:', error);
        this.error = 'Failed to load delivery owners. Please try again.';
        this.loading = false;
      }
    });
    
    this.subscriptions.push(subscription);
  }

  // Pagination methods
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadDeliveryOwners();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadDeliveryOwners();
  }

  getPaginationInfo(): string {
    const start = this.currentPage * this.pageSize + 1;
    const end = Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
    return `${start}-${end} of ${this.totalElements}`;
  }

  calculateStats(): void {
    // Load stats from API
    const subscription = this.deliveryOwnerService.getDeliveryOwnerCounts().subscribe({
      next: (stats: any) => {
        this.stats.total = stats.total || 0;
        this.stats.active = stats.active || 0;
        this.stats.verified = stats.verified || 0;
        this.stats.unverified = (stats.total || 0) - (stats.verified || 0);
        this.stats.totalCompanies = stats.withMultipleCompanies || 0;
        this.stats.totalRevenue = 0; // Not available in stats interface
        this.stats.avgExperience = 0; // Not available in stats interface
      },
      error: (error: any) => {
        console.error('Error loading delivery owner stats:', error);
      }
    });
    
    this.subscriptions.push(subscription);
  }

  onSearch(term: string): void {
    this.searchTerm = term;
    this.currentPage = 0;
    this.loadDeliveryOwners();
  }

  onStatusFilter(status: string): void {
    this.statusFilter = status;
    this.currentPage = 0;
    this.loadDeliveryOwners();
  }

  onVerificationFilter(verification: string): void {
    this.verificationFilter = verification;
    this.currentPage = 0;
    this.loadDeliveryOwners();
  }

  onExperienceFilter(experience: string): void {
    this.experienceFilter = experience;
    this.currentPage = 0;
    this.loadDeliveryOwners();
  }

  onSort(field: string): void {
    if (this.sortBy === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortDirection = 'asc';
    }
    this.loadDeliveryOwners();
  }

  toggleRowExpansion(ownerId: number): void {
    if (this.expandedRows.has(ownerId)) {
      this.expandedRows.delete(ownerId);
    } else {
      this.expandedRows.add(ownerId);
    }
  }

  isRowExpanded(ownerId: number): boolean {
    return this.expandedRows.has(ownerId);
  }

  openAddModal(): void {
    this.ownerForm = {
      username: '',
      email: '',
      firstName: '',
      lastName: '',
      phoneNumber: '',
      enabled: true,
      nationalId: '',
      dateOfBirth: '',
      address: '',
      emergencyContact: '',
      businessExperience: 0,
      preferredBusinessCategory: '',
      verified: false,
      businessLicense: '',
      isActive: true
    };
    this.showAddModal = true;
  }

  openEditModal(owner: DeliveryOwner): void {
    this.selectedOwner = owner;
    this.ownerForm = { ...owner };
    this.showEditModal = true;
  }

  openDeleteModal(owner: DeliveryOwner): void {
    this.selectedOwner = owner;
    this.showDeleteModal = true;
  }

  openDetailsModal(owner: DeliveryOwner): void {
    this.selectedOwner = owner;
    this.showDetailsModal = true;
  }

  openCompaniesModal(owner: DeliveryOwner): void {
    this.selectedOwner = owner;
    this.showCompaniesModal = true;
  }

  closeModals(): void {
    this.showAddModal = false;
    this.showEditModal = false;
    this.showDeleteModal = false;
    this.showDetailsModal = false;
    this.showCompaniesModal = false;
    this.selectedOwner = null;
  }

  saveOwner(): void {
    if (this.selectedOwner) {
      // Update existing owner
      const subscription = this.deliveryOwnerService.updateDeliveryOwner(this.selectedOwner.id!, this.ownerForm as DeliveryOwner).subscribe({
        next: (updatedOwner) => {
          this.loadDeliveryOwners();
          this.calculateStats();
          this.closeModals();
        },
        error: (error: any) => {
          console.error('Error updating delivery owner:', error);
          this.error = 'Failed to update delivery owner. Please try again.';
        }
      });
      this.subscriptions.push(subscription);
    } else {
      // Add new owner
      const subscription = this.deliveryOwnerService.createDeliveryOwner(this.ownerForm as DeliveryOwner).subscribe({
        next: (newOwner) => {
          this.loadDeliveryOwners();
          this.calculateStats();
          this.closeModals();
        },
        error: (error: any) => {
          console.error('Error creating delivery owner:', error);
          this.error = 'Failed to create delivery owner. Please try again.';
        }
      });
      this.subscriptions.push(subscription);
    }
  }

  deleteOwner(): void {
    if (this.selectedOwner) {
      const subscription = this.deliveryOwnerService.deleteDeliveryOwner(this.selectedOwner.id!).subscribe({
        next: () => {
          this.loadDeliveryOwners();
          this.calculateStats();
          this.closeModals();
        },
        error: (error: any) => {
          console.error('Error deleting delivery owner:', error);
          this.error = 'Failed to delete delivery owner. Please try again.';
        }
      });
      this.subscriptions.push(subscription);
    }
  }

  exportData(): void {
    const csvContent = this.generateCSV();
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'delivery-owners.csv';
    a.click();
    window.URL.revokeObjectURL(url);
  }

  private generateCSV(): string {
    const headers = ['ID', 'Username', 'Name', 'Email', 'Phone', 'National ID', 'Experience (Years)', 'Verified', 'Status', 'Companies', 'Revenue'];
    const rows = this.filteredDeliveryOwners.map(owner => [
      owner.id,
      owner.username,
      `${owner.firstName} ${owner.lastName}`,
      owner.email,
      owner.phoneNumber || '',
      owner.nationalId,
      owner.businessExperience || 0,
      owner.verified ? 'Yes' : 'No',
      owner.enabled ? 'Active' : 'Inactive',
      owner.totalCompanies || 0,
      this.formatCurrency(owner.totalRevenue || 0)
    ]);
    
    return [headers, ...rows].map(row => row.join(',')).join('\n');
  }

  getFullName(owner: DeliveryOwner): string {
    return `${owner.firstName} ${owner.lastName}`;
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  }

  formatDate(date: string | Date): string {
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    }).format(new Date(date));
  }

  getStatusClass(owner: DeliveryOwner): string {
    return owner.enabled ? 'active' : 'inactive';
  }

  getVerificationBadge(owner: DeliveryOwner): string {
    return owner.verified ? 'verified' : 'unverified';
  }

  getExperienceLevel(years: number): string {
    if (years <= 2) return 'Beginner';
    if (years <= 5) return 'Intermediate';
    if (years <= 10) return 'Advanced';
    return 'Expert';
  }

  getExperienceBadgeClass(years: number): string {
    if (years <= 2) return 'beginner';
    if (years <= 5) return 'intermediate';
    if (years <= 10) return 'experienced';
    return 'expert';
  }
}