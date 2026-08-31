import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { ProductService, Product, ProductSearchParams, ProductStats } from '../../services/product.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-products',
  templateUrl: './products.component.html',
  styleUrls: ['./products.component.css'],
  standalone: false
})
export class ProductsComponent implements OnInit, OnDestroy {
  products: Product[] = [];
  filteredProducts: Product[] = [];
  searchTerm: string = '';
  statusFilter: string = 'all';
  categoryFilter: string = 'all';
  sortBy: string = 'name';
  sortDirection: 'asc' | 'desc' = 'asc';
  
  // Pagination
  currentPage: number = 0;
  pageSize: number = 10;
  totalElements: number = 0;
  totalPages: number = 0;
  
  // Loading and error states
  loading: boolean = false;
  error: string | null = null;
  
  // Subscriptions
  private subscriptions: Subscription[] = [];
  
  // Modal states
  showAddModal: boolean = false;
  showEditModal: boolean = false;
  showDeleteModal: boolean = false;
  selectedProduct: Product | null = null;
  
  // Form data
  productForm: Partial<Product> = {
    name: '',
    description: '',
    sku: '',
    category: '',
    price: 0,
    costPrice: 0,
    stockQuantity: 0,
    minStockLevel: 0,
    status: 'ACTIVE',
    brand: '',
    weight: 0,
    dimensions: '',
    tags: []
  };
  
  // Statistics
  stats: ProductStats = {
    total: 0,
    active: 0,
    available: 0,
    featured: 0,
    outOfStock: 0,
    lowStock: 0,
    highRated: 0,
    withDiscount: 0,
    totalValue: 0,
    byCategory: {},
    byStatus: {}
  };
  
  categories = [
    'Electronics',
    'Clothing',
    'Home & Garden',
    'Sports & Outdoors',
    'Books',
    'Health & Beauty',
    'Automotive',
    'Food & Beverages',
    'Toys & Games',
    'Other'
  ];

  constructor(
    private productService: ProductService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadProducts();
    this.calculateStats();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  loadProducts(): void {
    this.loading = true;
    this.error = null;
    
    const searchParams: ProductSearchParams = {
      page: this.currentPage,
      pageSize: this.pageSize,
      sortBy: this.sortBy,
      sortDir: this.sortDirection,
      name: this.searchTerm || undefined,
      category: this.categoryFilter !== 'all' ? this.categoryFilter : undefined,
      status: this.statusFilter !== 'all' ? this.statusFilter : undefined
    };
    
    const subscription = this.productService.searchProducts(searchParams).subscribe({
      next: (response) => {
        this.products = response.content;
        this.filteredProducts = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.currentPage = response.number;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading products:', error);
        this.error = 'Failed to load products. Please try again.';
        this.loading = false;
      }
    });
    
    this.subscriptions.push(subscription);
  }

  // Pagination methods
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadProducts();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadProducts();
  }

  goToFirstPage(): void {
    this.currentPage = 0;
    this.loadProducts();
  }

  goToLastPage(): void {
    this.currentPage = this.totalPages - 1;
    this.loadProducts();
  }

  goToPreviousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadProducts();
    }
  }

  goToNextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadProducts();
    }
  }

  onSearch(term: string): void {
    this.searchTerm = term;
    this.currentPage = 0;
    this.loadProducts();
  }

  onStatusFilter(status: string): void {
    this.statusFilter = status;
    this.currentPage = 0;
    this.loadProducts();
  }

  onCategoryFilter(category: string): void {
    this.categoryFilter = category;
    this.currentPage = 0;
    this.loadProducts();
  }

  onSort(field: string): void {
    if (this.sortBy === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortDirection = 'asc';
    }
    this.currentPage = 0;
    this.loadProducts();
  }

  calculateStats(): void {
    const subscription = this.productService.getProductStats().subscribe({
      next: (stats) => {
        this.stats = stats;
      },
      error: (error: any) => {
        console.error('Error loading product statistics:', error);
        // Fallback to local calculation if API fails
        this.stats.total = this.products.length;
        this.stats.active = this.products.filter(p => p.status === 'ACTIVE').length;
        this.stats.available = this.products.filter(p => p.isAvailable).length;
        this.stats.featured = this.products.filter(p => p.isFeatured).length;
        this.stats.lowStock = this.products.filter(p => p.minStockLevel && p.stockQuantity <= p.minStockLevel && p.stockQuantity > 0).length;
        this.stats.outOfStock = this.products.filter(p => p.stockQuantity === 0).length;
        this.stats.highRated = this.products.filter(p => (p.rating || 0) >= 4.5).length;
        this.stats.withDiscount = this.products.filter(p => (p.discount || 0) > 0).length;
        this.stats.totalValue = this.products.reduce((sum, p) => sum + (p.price * p.stockQuantity), 0);
        this.stats.byCategory = {};
        this.stats.byStatus = {};
      }
    });
    
    this.subscriptions.push(subscription);
  }

  // Modal operations
  openAddModal(): void {
    this.productForm = {
      name: '',
      description: '',
      sku: '',
      category: '',
      price: 0,
      costPrice: 0,
      stockQuantity: 0,
      minStockLevel: 0,
      status: 'ACTIVE',
      brand: '',
      weight: 0,
      dimensions: '',
      isAvailable: true
    };
    this.showAddModal = true;
  }

  openEditModal(product: Product): void {
    this.selectedProduct = product;
    this.productForm = { ...product };
    this.showEditModal = true;
  }

  openDeleteModal(product: Product): void {
    this.selectedProduct = product;
    this.showDeleteModal = true;
  }

  closeModals(): void {
    this.showAddModal = false;
    this.showEditModal = false;
    this.showDeleteModal = false;
    this.selectedProduct = null;
  }

  saveProduct(): void {
    if (this.productForm.name && this.productForm.sku && this.productForm.category) {
      this.loading = true;
      
      if (this.showEditModal && this.selectedProduct) {
        // Update existing product
        const subscription = this.productService.updateProduct(this.selectedProduct.id!, this.productForm as Product).subscribe({
          next: (updatedProduct) => {
            this.loadProducts();
            this.calculateStats();
            this.closeModals();
            this.loading = false;
          },
          error: (error) => {
            console.error('Error updating product:', error);
            this.error = 'Failed to update product. Please try again.';
            this.loading = false;
          }
        });
        this.subscriptions.push(subscription);
      } else if (this.showAddModal) {
        // Add new product
        const subscription = this.productService.createProduct(this.productForm as Product).subscribe({
          next: (newProduct) => {
            this.loadProducts();
            this.calculateStats();
            this.closeModals();
            this.loading = false;
          },
          error: (error) => {
            console.error('Error creating product:', error);
            this.error = 'Failed to create product. Please try again.';
            this.loading = false;
          }
        });
        this.subscriptions.push(subscription);
      }
    }
  }

  deleteProduct(): void {
    if (this.selectedProduct) {
      this.loading = true;
      
      const subscription = this.productService.deleteProduct(this.selectedProduct.id!).subscribe({
        next: () => {
          this.loadProducts();
          this.calculateStats();
          this.closeModals();
          this.loading = false;
        },
        error: (error) => {
          console.error('Error deleting product:', error);
          this.error = 'Failed to delete product. Please try again.';
          this.loading = false;
        }
      });
      this.subscriptions.push(subscription);
    }
  }

  exportData(): void {
    const dataStr = JSON.stringify(this.filteredProducts, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'products-data.json';
    link.click();
    URL.revokeObjectURL(url);
  }

  formatCurrency(amount: number): string {
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
    switch (status) {
      case 'active': return 'status-active';
      case 'inactive': return 'status-inactive';
      case 'discontinued': return 'status-discontinued';
      default: return '';
    }
  }

  getStockStatusClass(product: Product): string {
    if (product.stockQuantity === 0) return 'stock-out';
    if (product.minStockLevel && product.stockQuantity <= product.minStockLevel) return 'stock-low';
    return 'stock-good';
  }

  getStockStatusText(product: Product): string {
    if (product.stockQuantity === 0) return 'Out of Stock';
    if (product.minStockLevel && product.stockQuantity <= product.minStockLevel) return 'Low Stock';
    return 'In Stock';
  }

  getCategoryIcon(category: string): string {
    const icons: { [key: string]: string } = {
      'Electronics': '📱',
      'Clothing': '👕',
      'Home & Garden': '🏠',
      'Sports & Outdoors': '⚽',
      'Books': '📚',
      'Health & Beauty': '💄',
      'Automotive': '🚗',
      'Food & Beverages': '🍕',
      'Toys & Games': '🎮',
      'Other': '📦'
    };
    return icons[category] || '📦';
  }

  generateSKU(): void {
    const prefix = this.productForm.category ? this.productForm.category.substring(0, 3).toUpperCase() : 'PRD';
    const timestamp = Date.now().toString().slice(-6);
    this.productForm.sku = `${prefix}-${timestamp}`;
  }
}