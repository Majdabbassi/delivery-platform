import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { VendorCompanyService, VendorCompany, VendorCompanySearchParams, VendorCompanyStats } from '../../services/vendor-company.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-vendor-companies',
  templateUrl: './vendor-companies.component.html',
  styleUrls: ['./vendor-companies.component.css'],
  standalone: false
})
export class VendorCompaniesComponent implements OnInit, OnDestroy {
  companies: VendorCompany[] = [];
  filteredCompanies: VendorCompany[] = [];
  searchTerm: string = '';
  statusFilter: string = 'all';
  industryFilter: string = 'all';
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
  showProductsModal: boolean = false;
  selectedCompany: VendorCompany | null = null;
  
  // Form data
  companyForm: Partial<VendorCompany> = {
    name: '',
    email: '',
    phone: '',
    address: '',
    website: '',
    businessCategory: '',
    industry: '',
    status: 'ACTIVE' as 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'SUSPENDED',
    contactPerson: '',
    businessDescription: '',
    notes: '',
    contractStartDate: ''
  };
  
  // Statistics
  stats: VendorCompanyStats = {
    total: 0,
    active: 0,
    pending: 0,
    verified: 0,
    activeAndVerified: 0,
    highRated: 0,
    withMultipleProducts: 0,
    recentlyEstablished: 0,
    totalRevenue: 0
  };
  
  private subscriptions: Subscription[] = [];
  
  industries = [
    'Technology',
    'Healthcare',
    'Finance',
    'Manufacturing',
    'Retail',
    'Education',
    'Real Estate',
    'Food & Beverage',
    'Transportation',
    'Other'
  ];

  private currentUser: any;

  constructor(
    private vendorCompanyService: VendorCompanyService,
    private authService: AuthService
  ) {
    this.currentUser = this.authService.getCurrentUser();
  }

  get isSuperAdmin(): boolean {
    return this.currentUser?.role === 'SUPER_ADMIN';
  }

  get isVendorOwner(): boolean {
    return this.currentUser?.role === 'VENDOR_OWNER';
  }

  ngOnInit(): void {
    this.loadCompanies();
    this.calculateStats();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  loadCompanies(): void {
    this.loading = true;
    this.error = null;

    // VENDOR_OWNER is only allowed to see their own companies (owner-scoped endpoint).
    // The general /search and listing endpoints are SUPER_ADMIN only, so use the
    // owner-scoped endpoint and filter/paginate locally for this role.
    if (this.isVendorOwner && this.currentUser) {
      const subscription = this.vendorCompanyService.getVendorCompaniesByOwner(this.currentUser.id).subscribe({
        next: (companies) => {
          this.companies = companies || [];
          this.totalElements = this.companies.length;
          this.totalPages = 1;
          this.applyLocalFilter();
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading vendor companies:', error);
          this.error = 'Failed to load your vendor companies. Please try again.';
          this.loading = false;
        }
      });
      this.subscriptions.push(subscription);
      return;
    }

    const searchParams: VendorCompanySearchParams = {
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
      searchParams.status = this.statusFilter.toUpperCase();
    }
    
    if (this.industryFilter !== 'all') {
      searchParams.businessCategory = this.industryFilter;
    }
    
    const subscription = this.vendorCompanyService.searchVendorCompanies(searchParams).subscribe({
      next: (response) => {
        this.companies = response.content;
        this.filteredCompanies = [...this.companies];
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading vendor companies:', error);
        this.error = 'Failed to load vendor companies. Please try again.';
        this.loading = false;
      }
    });
    
    this.subscriptions.push(subscription);
  }

  private applyLocalFilter(): void {
    const term = this.searchTerm?.toLowerCase() || '';
    let filtered = this.companies.filter(c =>
      !term ||
      c.name?.toLowerCase().includes(term) ||
      c.email?.toLowerCase().includes(term) ||
      c.contactPerson?.toLowerCase().includes(term)
    );

    if (this.statusFilter !== 'all') {
      filtered = filtered.filter(c => c.status === this.statusFilter.toUpperCase());
    }

    if (this.industryFilter !== 'all') {
      filtered = filtered.filter(c => c.businessCategory === this.industryFilter);
    }

    if (this.sortBy && this.sortDirection) {
      filtered.sort((a, b) => {
        let aVal: any = a[this.sortBy as keyof VendorCompany];
        let bVal: any = b[this.sortBy as keyof VendorCompany];
        if (typeof aVal === 'string') aVal = aVal.toLowerCase();
        if (typeof bVal === 'string') bVal = bVal.toLowerCase();
        return this.sortDirection === 'asc' ? (aVal > bVal ? 1 : aVal < bVal ? -1 : 0) : (aVal < bVal ? 1 : aVal > bVal ? -1 : 0);
      });
    }

    this.filteredCompanies = filtered;
    this.totalElements = this.companies.length;
    this.totalPages = 1;
  }

  // Pagination methods
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadCompanies();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadCompanies();
  }

  goToFirstPage(): void {
    this.currentPage = 0;
    this.loadCompanies();
  }

  goToLastPage(): void {
    this.currentPage = this.totalPages - 1;
    this.loadCompanies();
  }

  goToPreviousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadCompanies();
    }
  }

  goToNextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadCompanies();
    }
  }

  onSearch(term: string): void {
    this.searchTerm = term;
    this.currentPage = 0;
    this.loadCompanies();
  }

  onStatusFilter(status: string): void {
    this.statusFilter = status;
    this.currentPage = 0;
    this.loadCompanies();
  }

  onIndustryFilter(industry: string): void {
    this.industryFilter = industry;
    this.currentPage = 0;
    this.loadCompanies();
  }

  onSort(field: string): void {
    if (this.sortBy === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortDirection = 'asc';
    }
    this.currentPage = 0;
    this.loadCompanies();
  }

  calculateStats(): void {
    const subscription = this.vendorCompanyService.getVendorCompanyCounts().subscribe({
      next: (stats) => {
        this.stats = stats;
      },
      error: (error) => {
        console.error('Error loading vendor company statistics:', error);
        // Fallback to local calculation if API fails
        this.stats.total = this.companies.length;
        this.stats.active = this.companies.filter(c => c.status === 'ACTIVE').length;
        this.stats.pending = this.companies.filter(c => c.status === 'PENDING').length;
        this.stats.verified = this.companies.filter(c => c.isVerified).length;
        this.stats.activeAndVerified = this.companies.filter(c => c.status === 'ACTIVE' && c.isVerified).length;
        this.stats.highRated = this.companies.filter(c => (c.rating || 0) >= 4.5).length;
        this.stats.withMultipleProducts = this.companies.filter(c => (c.totalProducts || 0) > 1).length;
        this.stats.recentlyEstablished = this.companies.filter(c => {
          const established = new Date(c.establishedDate || '');
          const oneYearAgo = new Date();
          oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
          return established > oneYearAgo;
        }).length;
        this.stats.totalRevenue = this.companies.reduce((sum, c) => sum + (c.totalRevenue || 0), 0);
      }
    });
    
    this.subscriptions.push(subscription);
  }

  // Modal operations
  openAddModal(): void {
    this.companyForm = {
      name: '',
      email: '',
      phone: '',
      address: '',
      website: '',
      businessCategory: '',
      industry: '',
      status: 'ACTIVE' as 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'SUSPENDED',
      contactPerson: '',
      businessDescription: '',
      notes: '',
      contractStartDate: '',
      isVerified: false,
      rating: 0,
      totalProducts: 0,
    };
    this.showAddModal = true;
  }

  openEditModal(company: VendorCompany): void {
    this.selectedCompany = company;
    this.companyForm = { ...company };
    this.showEditModal = true;
  }

  openDeleteModal(company: VendorCompany): void {
    this.selectedCompany = company;
    this.showDeleteModal = true;
  }

  openDetailsModal(company: VendorCompany): void {
    this.selectedCompany = company;
    this.showDetailsModal = true;
  }

  openProductsModal(company: VendorCompany): void {
    this.selectedCompany = company;
    this.showProductsModal = true;
  }

  closeModals(): void {
    this.showAddModal = false;
    this.showEditModal = false;
    this.showDeleteModal = false;
    this.showDetailsModal = false;
    this.showProductsModal = false;
    this.selectedCompany = null;
  }

  toggleCompanyStatus(company: VendorCompany): void {
    this.loading = true;
    
    const updatedCompany = {
      ...company,
      status: (company.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE') as 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'SUSPENDED'
    };
    
    const subscription = this.vendorCompanyService.updateVendorCompany(company.id!, updatedCompany).subscribe({
      next: (updated) => {
        this.loadCompanies();
        this.calculateStats();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error updating company status:', error);
        this.error = 'Failed to update company status. Please try again.';
        this.loading = false;
      }
    });
    this.subscriptions.push(subscription);
  }

  saveCompany(): void {
    if (this.companyForm.name && this.companyForm.email && this.companyForm.phone && this.companyForm.address && this.companyForm.businessCategory && this.companyForm.contactPerson) {
      this.loading = true;
      
      if (this.showEditModal && this.selectedCompany) {
        // Update existing company
        const subscription = this.vendorCompanyService.updateVendorCompany(this.selectedCompany.id!, this.companyForm as VendorCompany).subscribe({
          next: (updatedCompany) => {
            this.loadCompanies();
            this.calculateStats();
            this.closeModals();
            this.loading = false;
          },
          error: (error) => {
            console.error('Error updating vendor company:', error);
            this.error = 'Failed to update company. Please try again.';
            this.loading = false;
          }
        });
        this.subscriptions.push(subscription);
      } else if (this.showAddModal) {
        // Add new company
        const subscription = this.vendorCompanyService.createVendorCompany(this.companyForm as VendorCompany).subscribe({
          next: (newCompany) => {
            this.loadCompanies();
            this.calculateStats();
            this.closeModals();
            this.loading = false;
          },
          error: (error) => {
            console.error('Error creating vendor company:', error);
            this.error = 'Failed to create company. Please try again.';
            this.loading = false;
          }
        });
        this.subscriptions.push(subscription);
      }
    }
  }

  deleteCompany(): void {
    if (this.selectedCompany) {
      this.loading = true;
      
      const subscription = this.vendorCompanyService.hardDeleteVendorCompany(this.selectedCompany.id!).subscribe({
        next: () => {
          this.loadCompanies();
          this.calculateStats();
          this.closeModals();
          this.loading = false;
        },
        error: (error) => {
          console.error('Error deleting vendor company:', error);
          this.error = 'Failed to delete company. Please try again.';
          this.loading = false;
        }
      });
      this.subscriptions.push(subscription);
    }
  }

  exportData(): void {
    const dataStr = JSON.stringify(this.filteredCompanies, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'vendor-companies-data.json';
    link.click();
    URL.revokeObjectURL(url);
  }

  formatCurrency(amount: number | undefined): string {
    if (!amount && amount !== 0) return '$0.00';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  }

  formatDate(date: Date | string | undefined): string {
    if (!date) return 'N/A';
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    }).format(new Date(date));
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'status-active';
      case 'INACTIVE': return 'status-inactive';
      case 'PENDING': return 'status-pending';
      case 'SUSPENDED': return 'status-suspended';
      default: return '';
    }
  }

  getIndustryIcon(industry: string | undefined): string {
    if (!industry) return '🏢';
    const icons: { [key: string]: string } = {
      'Technology': '💻',
      'Healthcare': '🏥',
      'Finance': '💰',
      'Manufacturing': '🏭',
      'Retail': '🛍️',
      'Education': '🎓',
      'Real Estate': '🏢',
      'Food & Beverage': '🍽️',
      'Transportation': '🚛',
      'Other': '🏢'
    };
    return icons[industry] || '🏢';
  }
}