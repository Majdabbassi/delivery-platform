import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { VendorOwner, VendorOwnerService, VendorOwnerSearchParams } from '../../services/vendor-owner.service';
import { AuthService } from '../../services/auth.service';

// Extended VendorOwner interface for component use


interface VendorCompany {
  id: number;
  companyName: string;
  companyAddress: string;
  businessLicense: string;
  contactPhone: string;
  contactEmail: string;
  businessCategory: string;
  isLicensed: boolean;
  isActive: boolean;
  maxProducts: number;
  activeProductsCount: number;
  totalOrdersManaged: number;
  totalRevenue: number;
  commissionRate: number;
  rating: number;
  registrationDate: Date;
  lastOrderDate?: Date;
}

@Component({
  selector: 'app-vendor-owners',
  templateUrl: './vendor-owners.component.html',
  styleUrls: ['./vendor-owners.component.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule]
  })
export class VendorOwnersComponent implements OnInit, OnDestroy {
  vendorOwners: VendorOwner[] = [];
  filteredVendorOwners: VendorOwner[] = [];
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
  selectedOwner: VendorOwner | null = null;
  expandedRows: Set<number> = new Set();
  
  // Form data
  ownerForm: Partial<VendorOwner> & {
    businessExperienceYears?: number;
    businessLicenseNumber?: string;
    maxCompaniesAllowed?: number;
    preferredBusinessCategories?: string;
    password?: string;
    isEnabled?: boolean;
    isVerifiedOwner?: boolean;
    totalProductsManaged?: number;
    totalRevenue?: number;
  } = {
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
    businessExperienceYears: 0,
    businessLicenseNumber: '',
    maxCompaniesAllowed: 1,
    preferredBusinessCategories: '',
    password: '',
    isEnabled: true,
    isVerifiedOwner: false,
    totalProductsManaged: 0,
    totalRevenue: 0
  };
  
  // Statistics
  stats = {
    total: 0,
    verified: 0,
    unverified: 0,
    active: 0,
    activeAndVerified: 0,
    experienced: 0,
    withMultipleCompanies: 0,
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
    private vendorOwnerService: VendorOwnerService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadVendorOwners();
    this.calculateStats();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  loadVendorOwners(): void {
    this.loading = true;
    this.error = '';
    
    const searchParams: VendorOwnerSearchParams = {
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
      searchParams.enabled = this.statusFilter === 'active';
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
    
    const subscription = this.vendorOwnerService.searchVendorOwners(searchParams).subscribe({
      next: (response) => {
        this.vendorOwners = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.filteredVendorOwners = [...this.vendorOwners];
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading vendor owners:', error);
        this.error = 'Failed to load vendor owners. Please try again.';
        this.loading = false;
      }
    });
    
    this.subscriptions.push(subscription);
  }

  calculateStats(): void {
    // Load stats from API
    const subscription = this.vendorOwnerService.getVendorOwnerCounts().subscribe({
      next: (counts) => {
        this.stats.total = counts.total || 0;
        this.stats.active = counts.active || 0;
        this.stats.verified = counts.verified || 0;
        this.stats.unverified = (counts.total || 0) - (counts.verified || 0);
        this.stats.activeAndVerified = counts.activeAndVerified || 0;
        this.stats.experienced = counts.experienced || 0;
        this.stats.withMultipleCompanies = counts.withMultipleCompanies || 0;
        this.stats.totalCompanies = counts.totalCompanies || 0;
        this.stats.totalRevenue = counts.totalRevenue || 0;
        this.stats.avgExperience = counts.avgExperience || 0;
      },
      error: (error) => {
        console.error('Error loading vendor owner statistics:', error);
        // Fallback to local calculation
        this.stats.total = this.vendorOwners.length;
        this.stats.active = this.vendorOwners.filter(vo => vo.enabled).length;
        this.stats.verified = this.vendorOwners.filter(vo => vo.verified).length;
        this.stats.unverified = this.vendorOwners.filter(vo => !vo.verified).length;
        this.stats.activeAndVerified = this.vendorOwners.filter(vo => vo.enabled && vo.verified).length;
        this.stats.experienced = this.vendorOwners.filter(vo => (vo.businessExperience || 0) >= 5).length;
        this.stats.withMultipleCompanies = 0; // Cannot calculate locally
        this.stats.totalCompanies = this.vendorOwners.reduce((sum, vo) => sum + (vo.totalCompanies || 0), 0);
        this.stats.totalRevenue = this.vendorOwners.reduce((sum, vo) => sum + (vo.totalRevenue || 0), 0);
        const totalExperience = this.vendorOwners.reduce((sum, vo) => sum + (vo.businessExperienceYears || 0), 0);
        this.stats.avgExperience = this.vendorOwners.length > 0 ? Math.round(totalExperience / this.vendorOwners.length * 10) / 10 : 0;
      }
    });
    this.subscriptions.push(subscription);
  }

  // Search and Filter Methods
  onSearch(term: string): void {
    this.currentPage = 0;
    this.loadVendorOwners();
  }

  onStatusFilter(status: string): void {
    this.currentPage = 0;
    this.loadVendorOwners();
  }

  onVerificationFilter(verification: string): void {
    this.currentPage = 0;
    this.loadVendorOwners();
  }

  onExperienceFilter(experience: string): void {
    this.currentPage = 0;
    this.loadVendorOwners();
  }

  // Sorting Methods
  onSort(column: string): void {
    if (this.sortBy === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = column;
      this.sortDirection = 'asc';
    }
    this.currentPage = 0;
    this.loadVendorOwners();
  }

  // Pagination Methods
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadVendorOwners();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadVendorOwners();
  }

  goToFirstPage(): void {
    this.currentPage = 0;
    this.loadVendorOwners();
  }

  goToLastPage(): void {
    this.currentPage = this.totalPages - 1;
    this.loadVendorOwners();
  }

  goToPreviousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadVendorOwners();
    }
  }

  goToNextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadVendorOwners();
    }
  }

  // Row Expansion
  toggleRowExpansion(ownerId: number | undefined): void {
    if (ownerId !== undefined) {
      if (this.expandedRows.has(ownerId)) {
        this.expandedRows.delete(ownerId);
      } else {
        this.expandedRows.add(ownerId);
      }
    }
  }

  isRowExpanded(ownerId: number | undefined): boolean {
    return ownerId !== undefined && this.expandedRows.has(ownerId);
  }

  // Modal Methods
  openAddModal(): void {
    this.resetForm();
    this.showAddModal = true;
  }

  openEditModal(owner: VendorOwner): void {
    // Ensure details modal is closed before opening edit modal
    this.showDetailsModal = false;
    this.selectedOwner = owner;
    // Exclude password from the form when editing unless explicitly changed
    const { password, ...ownerWithoutPassword } = owner;
    this.ownerForm = { ...ownerWithoutPassword };
    this.showEditModal = true;
  }

  openDeleteModal(owner: VendorOwner): void {
    this.selectedOwner = owner;
    this.showDeleteModal = true;
  }

  openDetailsModal(owner: VendorOwner): void {
    this.selectedOwner = owner;
    this.showDetailsModal = true;
  }

  openCompaniesModal(owner: VendorOwner): void {
    this.selectedOwner = owner;
    this.showCompaniesModal = true;
  }

  closeModal(): void {
    this.showAddModal = false;
    this.showEditModal = false;
    this.showDeleteModal = false;
    this.showDetailsModal = false;
    this.showCompaniesModal = false;
    this.selectedOwner = null;
    this.resetForm();
  }

  resetForm(): void {
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
      businessExperienceYears: 0,
      businessLicenseNumber: '',
      maxCompaniesAllowed: 1,
      preferredBusinessCategories: '',
      password: '',
      isEnabled: true,
      isVerifiedOwner: false,
      totalProductsManaged: 0,
      totalRevenue: 0
    };
  }

  // CRUD Operations
  saveOwner(): void {
    if (this.showAddModal) {
      this.addOwner();
    } else if (this.showEditModal) {
      this.updateOwner();
    }
  }

  addOwner(): void {
    const ownerData = this.ownerForm as VendorOwner;
    const subscription = this.vendorOwnerService.createVendorOwner(ownerData).subscribe({
      next: (newOwner) => {
        this.vendorOwners.push(newOwner);
        this.loadVendorOwners();
        this.calculateStats();
        this.closeModal();
      },
      error: (error) => {
        console.error('Error creating vendor owner:', error);
        this.error = 'Failed to create vendor owner. Please try again.';
      }
    });
    this.subscriptions.push(subscription);
  }

  updateOwner(): void {
    if (this.selectedOwner) {
      // Exclude password from payload if not set (to avoid sending empty string)
      const ownerData = { ...this.ownerForm };
      if (!ownerData.password) {
        delete ownerData.password;
      }
      const subscription = this.vendorOwnerService.updateVendorOwner(this.selectedOwner.id!, ownerData as VendorOwner).subscribe({
        next: (updatedOwner) => {
          const index = this.vendorOwners.findIndex(o => o.id === this.selectedOwner!.id);
          if (index !== -1) {
            this.vendorOwners[index] = updatedOwner;
            this.loadVendorOwners();
            this.calculateStats();
            this.closeModal();
          }
        },
        error: (error) => {
          console.error('Error updating vendor owner:', error);
          this.error = 'Failed to update vendor owner. Please try again.';
        }
      });
      this.subscriptions.push(subscription);
    }
  }

  deleteOwner(): void {
    if (this.selectedOwner) {
      const subscription = this.vendorOwnerService.deleteVendorOwner(this.selectedOwner.id!).subscribe({
        next: () => {
          this.vendorOwners = this.vendorOwners.filter(o => o.id !== this.selectedOwner!.id);
          this.loadVendorOwners();
          this.calculateStats();
          this.closeModal();
        },
        error: (error) => {
          console.error('Error deleting vendor owner:', error);
          this.error = 'Failed to delete vendor owner. Please try again.';
        }
      });
      this.subscriptions.push(subscription);
    }
  }

  toggleOwnerStatus(owner: VendorOwner): void {
    const newStatus = !owner.enabled;
    const subscription = this.vendorOwnerService.updateEnabledStatus(owner.id!, newStatus).subscribe({
      next: (updatedOwner) => {
        const index = this.vendorOwners.findIndex(o => o.id === owner.id);
        if (index !== -1) {
          this.vendorOwners[index] = updatedOwner;
          this.calculateStats();
        }
      },
      error: (error) => {
        console.error('Error updating vendor owner status:', error);
        this.error = 'Failed to update vendor owner status. Please try again.';
      }
    });
    this.subscriptions.push(subscription);
  }

  toggleOwnerVerification(owner: VendorOwner): void {
    const newVerification = !owner.verified;
    const subscription = this.vendorOwnerService.updateVerifiedStatus(owner.id!, newVerification).subscribe({
      next: (updatedOwner) => {
        const index = this.vendorOwners.findIndex(o => o.id === owner.id);
        if (index !== -1) {
          this.vendorOwners[index] = updatedOwner;
          this.calculateStats();
        }
      },
      error: (error) => {
        console.error('Error updating vendor owner verification:', error);
        this.error = 'Failed to update vendor owner verification. Please try again.';
      }
    });
    this.subscriptions.push(subscription);
  }

  // Utility Methods
  getFullName(owner: VendorOwner): string {
    return `${owner.firstName} ${owner.lastName}`;
  }

  getExperienceBadgeClass(years: number | undefined): string {
    if (!years || years <= 2) return 'beginner';
    if (years <= 5) return 'intermediate';
    if (years <= 10) return 'advanced';
    return 'expert';
  }

  getExperienceLevel(years: number | undefined): string {
    if (!years || years <= 2) return 'Beginner';
    if (years <= 5) return 'Intermediate';
    if (years <= 10) return 'Advanced';
    return 'Expert';
  }

  getVerificationBadge(owner: VendorOwner): string {
    return owner.verified ? 'verified' : 'unverified';
  }

  getStatusBadge(owner: VendorOwner): string {
    return owner.enabled ? 'active' : 'inactive';
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(amount);
  }

  formatDate(date: Date | string | undefined): string {
    if (!date) return 'N/A';
    const dateObj = typeof date === 'string' ? new Date(date) : date;
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    }).format(dateObj);
  }

  exportData(): void {
    // Implementation for exporting data
    console.log('Exporting vendor owners data...');
  }
}