import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { AdminService, Admin, AdminSearchParams, AdminStats } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admins',
  templateUrl: './admins.component.html',
  styleUrls: ['./admins.component.css'],
  standalone: false
})
export class AdminsComponent implements OnInit, OnDestroy {
  admins: Admin[] = [];
  filteredAdmins: Admin[] = [];
  searchTerm: string = '';
  statusFilter: string = 'all';
  roleFilter: string = 'all';
  accessLevelFilter: string = 'all';
  departmentFilter: string = 'all';
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
  
  // Modal states
  showAddModal: boolean = false;
  showEditModal: boolean = false;
  showDeleteModal: boolean = false;
  showDetailsModal: boolean = false;
  showPermissionsModal: boolean = false;
  
  // Form data
  adminForm: Partial<Admin> = {};
  selectedAdmin: Admin | null = null;
  
  // Statistics
  stats: AdminStats = {
    total: 0,
    active: 0,
    verified: 0,
    activeAndVerified: 0,
    superAdmins: 0,
    admins: 0,
    moderators: 0,
    recentlyHired: 0,
    totalAdmins: 0,
    activeAdmins: 0,
    lockedAdmins: 0
  };
  
  private subscriptions: Subscription[] = [];
  
  // Available permissions
  availablePermissions: string[] = [
    'USER_MANAGEMENT',
    'SYSTEM_CONFIGURATION',
    'REPORTS_ACCESS',
    'CONTENT_MANAGEMENT',
    'FINANCIAL_ACCESS',
    'AUDIT_LOGS',
    'BACKUP_RESTORE',
    'SECURITY_SETTINGS',
    'API_ACCESS',
    'NOTIFICATION_MANAGEMENT'
  ];
  
  // Available modules
  availableModules: string[] = [
    'Dashboard',
    'Users',
    'Products',
    'Orders',
    'Reports',
    'Settings',
    'Analytics',
    'Notifications',
    'Audit',
    'System'
  ];
  
  // Available departments
  availableDepartments: string[] = [
    'IT',
    'Operations',
    'Customer Service',
    'Finance',
    'Marketing',
    'Security',
    'Quality Assurance',
    'Business Development'
  ];

  constructor(
    private adminService: AdminService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadAdmins();
    this.calculateStatistics();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  loadAdmins(): void {
    this.loading = true;
    this.error = null;
    
    const searchParams: AdminSearchParams = {
      page: this.currentPage,
      size: this.pageSize,
      sortBy: this.sortBy,
      sortDir: this.sortDirection
    };
    
    // Add filters to search params
    if (this.searchTerm) {
      searchParams.username = this.searchTerm;
      searchParams.email = this.searchTerm;
      searchParams.firstName = this.searchTerm;
      searchParams.lastName = this.searchTerm;
    }
    
    if (this.statusFilter !== 'all') {
      if (this.statusFilter === 'enabled') {
        searchParams.enabled = true;
      } else if (this.statusFilter === 'disabled') {
        searchParams.enabled = false;
      }
    }
    
    if (this.roleFilter !== 'all') {
      searchParams.role = this.roleFilter;
    }
    
    if (this.departmentFilter !== 'all') {
      searchParams.department = this.departmentFilter;
    }
    
    const subscription = this.adminService.searchAdmins(searchParams).subscribe({
      next: (response) => {
        this.admins = response.content;
        this.filteredAdmins = [...this.admins];
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading admins:', error);
        this.error = 'Failed to load admins. Please try again.';
        this.loading = false;
      }
    });
    
    this.subscriptions.push(subscription);
  }



  onSearch(searchTerm: string): void {
    this.searchTerm = searchTerm;
    this.currentPage = 0;
    this.loadAdmins();
  }

  onStatusFilter(status: string): void {
    this.statusFilter = status;
    this.currentPage = 0;
    this.loadAdmins();
  }

  onRoleFilter(role: string): void {
    this.roleFilter = role;
    this.currentPage = 0;
    this.loadAdmins();
  }

  onAccessLevelFilter(accessLevel: string): void {
    this.accessLevelFilter = accessLevel;
    this.currentPage = 0;
    this.loadAdmins();
  }

  onDepartmentFilter(department: string): void {
    this.departmentFilter = department;
    this.currentPage = 0;
    this.loadAdmins();
  }



  onSort(field: string): void {
    if (this.sortBy === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortDirection = 'asc';
    }
    this.currentPage = 0;
    this.loadAdmins();
  }

  calculateStatistics(): void {
    const subscription = this.adminService.getAdminCounts().subscribe({
      next: (stats) => {
        this.stats = stats;
      },
      error: (error) => {
        console.error('Error loading admin statistics:', error);
        // Fallback to local calculation
        this.stats = {
          total: this.admins.length,
          active: this.admins.filter(admin => admin.enabled).length,
          verified: this.admins.filter(admin => admin.verified).length,
          activeAndVerified: this.admins.filter(admin => admin.enabled && admin.verified).length,
          superAdmins: this.admins.filter(admin => admin.role === 'SUPER_ADMIN').length,
          admins: this.admins.filter(admin => admin.role === 'ADMIN').length,
          moderators: this.admins.filter(admin => admin.role === 'MODERATOR').length,
          recentlyHired: 0,
          totalAdmins: this.admins.length,
          activeAdmins: this.admins.filter(admin => admin.enabled).length,
          lockedAdmins: this.admins.filter(admin => admin.isLocked).length
        };
      }
    });
    
    this.subscriptions.push(subscription);
  }

  // Pagination methods
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadAdmins();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadAdmins();
  }

  goToFirstPage(): void {
    this.currentPage = 0;
    this.loadAdmins();
  }

  goToLastPage(): void {
    this.currentPage = this.totalPages - 1;
    this.loadAdmins();
  }

  goToPreviousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadAdmins();
    }
  }

  goToNextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadAdmins();
    }
  }

  // Modal operations
  openAddModal(): void {
    this.adminForm = {
      role: 'ADMIN',
      enabled: true,
      isEnabled: true,
      verified: false,
      permissions: [],
      assignedModules: [],
      accessLevel: 'LIMITED',
      isLocked: false,
      lockReason: '',
      loginAttempts: 0,
      canManageUsers: false,
      canManageSystem: false,
      canViewReports: false,
      canManageContent: false,
      sessionTimeout: 30,
      description: '',
      username: '',
      email: '',
      firstName: '',
      lastName: ''
    };
    this.showAddModal = true;
  }

  openEditModal(admin: Admin | null): void {
    if (admin) {
      this.selectedAdmin = admin;
      this.adminForm = { ...admin };
      this.showEditModal = true;
    }
  }

  openDeleteModal(admin: Admin): void {
    this.selectedAdmin = admin;
    this.showDeleteModal = true;
  }

  openDetailsModal(admin: Admin): void {
    this.selectedAdmin = admin;
    this.showDetailsModal = true;
  }

  openPermissionsModal(admin: Admin): void {
    this.selectedAdmin = admin;
    this.adminForm = { ...admin };
    this.showPermissionsModal = true;
  }

  closeModals(): void {
    this.showAddModal = false;
    this.showEditModal = false;
    this.showDeleteModal = false;
    this.showDetailsModal = false;
    this.showPermissionsModal = false;
    this.selectedAdmin = null;
    this.adminForm = {};
  }

  saveAdmin(): void {
    if (this.showAddModal) {
      // Add new admin
      const subscription = this.adminService.createAdmin(this.adminForm as Admin).subscribe({
        next: (admin) => {
          this.loadAdmins();
          this.calculateStatistics();
          this.closeModals();
        },
        error: (error) => {
          console.error('Error creating admin:', error);
          this.error = 'Failed to create admin. Please try again.';
        }
      });
      this.subscriptions.push(subscription);
    } else if (this.showEditModal && this.selectedAdmin) {
      // Update existing admin
      const subscription = this.adminService.updateAdmin(this.selectedAdmin.id!, this.adminForm as Admin).subscribe({
        next: (admin) => {
          this.loadAdmins();
          this.calculateStatistics();
          this.closeModals();
        },
        error: (error) => {
          console.error('Error updating admin:', error);
          this.error = 'Failed to update admin. Please try again.';
        }
      });
      this.subscriptions.push(subscription);
    }
  }

  deleteAdmin(): void {
    if (this.selectedAdmin) {
      const subscription = this.adminService.deleteAdmin(this.selectedAdmin.id!).subscribe({
        next: () => {
          this.loadAdmins();
          this.calculateStatistics();
          this.closeModals();
        },
        error: (error) => {
          console.error('Error deleting admin:', error);
          this.error = 'Failed to delete admin. Please try again.';
        }
      });
      this.subscriptions.push(subscription);
    }
  }

  toggleAdminStatus(admin: Admin): void {
    const subscription = this.adminService.updateEnabledStatus(admin.id!, !admin.enabled).subscribe({
      next: (updatedAdmin: Admin) => {
        const index = this.admins.findIndex(a => a.id === admin.id);
        if (index !== -1) {
          this.admins[index] = updatedAdmin;
        }
        this.calculateStatistics();
      },
      error: (error: any) => {
        console.error('Error updating admin status:', error);
        this.error = 'Failed to update admin status. Please try again.';
      }
    });
    this.subscriptions.push(subscription);
  }

  unlockAdmin(admin: Admin): void {
    const subscription = this.adminService.unlockAdmin(admin.id!).subscribe({
      next: (updatedAdmin: Admin) => {
        const index = this.admins.findIndex(a => a.id === admin.id);
        if (index !== -1) {
          this.admins[index] = updatedAdmin;
        }
        this.calculateStatistics();
      },
      error: (error: any) => {
        console.error('Error unlocking admin:', error);
        this.error = 'Failed to unlock admin. Please try again.';
      }
    });
    this.subscriptions.push(subscription);
  }

  lockAdmin(admin: Admin, reason: string): void {
    const subscription = this.adminService.lockAdmin(admin.id!, reason).subscribe({
      next: (updatedAdmin: Admin) => {
        const index = this.admins.findIndex(a => a.id === admin.id);
        if (index !== -1) {
          this.admins[index] = updatedAdmin;
        }
        this.calculateStatistics();
      },
      error: (error: any) => {
        console.error('Error locking admin:', error);
        this.error = 'Failed to lock admin. Please try again.';
      }
    });
    this.subscriptions.push(subscription);
  }

  exportData(): void {
    const dataStr = JSON.stringify(this.admins, null, 2);
    const dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(dataStr);
    
    const exportFileDefaultName = `admins_export_${new Date().toISOString().split('T')[0]}.json`;
    
    const linkElement = document.createElement('a');
    linkElement.setAttribute('href', dataUri);
    linkElement.setAttribute('download', exportFileDefaultName);
    linkElement.click();
  }

  // Getter methods for template
  get totalAdmins(): number {
    return this.stats.totalAdmins || this.stats.total;
  }

  get activeAdmins(): number {
    return this.stats.activeAdmins || this.stats.active;
  }

  get lockedAdmins(): number {
    return this.stats.lockedAdmins;
  }

  get superAdmins(): number {
    return this.stats.superAdmins;
  }

  // Utility methods
  formatDate(date: Date | string | undefined): string {
    if (!date) return 'Never';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getStatusClass(admin: Admin): string {
    return admin.enabled ? 'status-enabled' : 'status-disabled';
  }

  getRoleIcon(role: string): string {
    switch (role) {
      case 'SUPER_ADMIN': return 'fas fa-crown';
      case 'ADMIN': return 'fas fa-user-shield';
      case 'MANAGER': return 'fas fa-user-tie';
      case 'MODERATOR': return 'fas fa-user-check';
      default: return 'fas fa-user';
    }
  }

  getAccessLevelBadge(accessLevel: string | undefined): string {
    switch (accessLevel) {
      case 'FULL': return 'access-full';
      case 'LIMITED': return 'access-limited';
      case 'READ_ONLY': return 'access-readonly';
      default: return 'access-none';
    }
  }

  getAccessLevelText(accessLevel: string | undefined): string {
    if (!accessLevel) return 'Not Set';
    return accessLevel.replace('_', ' ');
  }

  getPermissionCount(admin: Admin): number {
    return admin.permissions ? admin.permissions.length : 0;
  }

  getModuleCount(admin: Admin): number {
    return admin.assignedModules ? admin.assignedModules.length : 0;
  }

  onPermissionChange(permission: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (!this.adminForm.permissions) {
      this.adminForm.permissions = [];
    }
    
    if (checked) {
      if (!this.adminForm.permissions.includes(permission)) {
        this.adminForm.permissions.push(permission);
      }
    } else {
      this.adminForm.permissions = this.adminForm.permissions.filter(p => p !== permission);
    }
  }

  onModuleChange(module: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (!this.adminForm.assignedModules) {
      this.adminForm.assignedModules = [];
    }
    
    if (checked) {
      if (!this.adminForm.assignedModules.includes(module)) {
        this.adminForm.assignedModules.push(module);
      }
    } else {
      this.adminForm.assignedModules = this.adminForm.assignedModules.filter(m => m !== module);
    }
  }

  isPermissionSelected(permission: string): boolean {
    return this.adminForm.permissions ? this.adminForm.permissions.includes(permission) : false;
  }

  isModuleSelected(module: string): boolean {
    return this.adminForm.assignedModules ? this.adminForm.assignedModules.includes(module) : false;
  }

  getPermissionDescription(permission: string): string {
    const descriptions: { [key: string]: string } = {
      'USER_MANAGEMENT': 'Create, edit, and manage user accounts',
      'SYSTEM_CONFIGURATION': 'Configure system settings and parameters',
      'REPORTS_ACCESS': 'View and generate system reports',
      'CONTENT_MANAGEMENT': 'Manage platform content and media',
      'FINANCIAL_ACCESS': 'Access financial data and transactions',
      'AUDIT_LOGS': 'View system audit logs and activity',
      'BACKUP_RESTORE': 'Perform system backup and restore operations',
      'SECURITY_SETTINGS': 'Configure security policies and settings',
      'API_ACCESS': 'Access and manage API endpoints',
      'NOTIFICATION_MANAGEMENT': 'Manage system notifications and alerts'
    };
    return descriptions[permission] || 'No description available';
  }
}