import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';
import { UserRole } from './services/auth.service';

const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'driver-register',
    loadComponent: () => import('./pages/driver-register/driver-register.component').then(m => m.DriverRegisterComponent)
  },
  {
    path: 'access-denied',
    loadComponent: () => import('./pages/access-denied/access-denied.component').then(m => m.AccessDeniedComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'tracking',
    loadComponent: () => import('./pages/tracking/tracking.component').then(m => m.TrackingComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'tracking/:trackingNumber',
    loadComponent: () => import('./pages/tracking/tracking.component').then(m => m.TrackingComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'partnerships',
    loadComponent: () => import('./pages/partnerships/partnerships.component').then(m => m.PartnershipsComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: [UserRole.SUPER_ADMIN, UserRole.VENDOR_OWNER, UserRole.DELIVERY_OWNER] }
  },
  {
    path: 'pool',
    loadComponent: () => import('./pages/pool/pool.component').then(m => m.PoolComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: [UserRole.SUPER_ADMIN, UserRole.DELIVERY_OWNER, UserRole.DRIVER] }
  },
  {
    path: 'bids',
    loadComponent: () => import('./pages/bids/bids.component').then(m => m.BidsComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: [UserRole.SUPER_ADMIN, UserRole.VENDOR_OWNER, UserRole.CLIENT] }
  },
  {
    path: 'my-jobs',
    loadComponent: () => import('./pages/driver-portal/driver-portal.component').then(m => m.DriverPortalComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: [UserRole.SUPER_ADMIN, UserRole.DRIVER] }
  },
  {
    path: 'notifications',
    loadComponent: () => import('./pages/notifications/notifications.component').then(m => m.NotificationsComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'profile',
    loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'vendorcompanies',
    loadComponent: () => import('./pages/vendor-companies/vendor-companies.component').then(m => m.VendorCompaniesComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: [UserRole.SUPER_ADMIN, UserRole.VENDOR_OWNER] }
  },
  {
    path: 'deliverycompanies',
    loadComponent: () => import('./pages/delivery-companies/delivery-companies.component').then(m => m.DeliveryCompaniesComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: [UserRole.SUPER_ADMIN, UserRole.DELIVERY_OWNER] }
  },
  {
    path: 'deliveryowners',
    loadComponent: () => import('./pages/delivery-owners/delivery-owners.component').then(m => m.DeliveryOwnersComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: [UserRole.SUPER_ADMIN] }
  },
  {
    path: 'vendorowners',
    loadComponent: () => import('./pages/vendor-owners/vendor-owners.component').then(m => m.VendorOwnersComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: [UserRole.SUPER_ADMIN] }
  },
  {
    path: 'customers',
    loadComponent: () => import('./pages/customers/customers.component').then(m => m.CustomersComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: [UserRole.SUPER_ADMIN] }
  },
  {
    path: 'products',
    loadComponent: () => import('./pages/products/products.component').then(m => m.ProductsComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: [UserRole.SUPER_ADMIN, UserRole.VENDOR_OWNER] }
  },
  {
    path: 'drivers',
    loadComponent: () => import('./pages/drivers/drivers.component').then(m => m.DriversComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: [UserRole.SUPER_ADMIN, UserRole.DELIVERY_OWNER] }
  },
  {
    path: 'orders',
    loadComponent: () => import('./pages/orders/orders.component').then(m => m.OrdersComponent),
    canActivate: [AuthGuard]
  },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }