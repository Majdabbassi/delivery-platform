import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { NotificationsComponent } from './pages/notifications/notifications.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { VendorCompaniesComponent } from './pages/vendor-companies/vendor-companies.component';
import { DeliveryCompaniesComponent } from './pages/delivery-companies/delivery-companies.component';
import { CustomersComponent } from './pages/customers/customers.component';
import { ProductsComponent } from './pages/products/products.component';
import { DeliveryOwnersComponent } from './pages/delivery-owners/delivery-owners.component';
import { VendorOwnersComponent } from './pages/vendor-owners/vendor-owners.component';
import { DriversComponent } from './pages/drivers/drivers.component';
import { OrdersComponent } from './pages/orders/orders.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { DriverRegisterComponent } from './pages/driver-register/driver-register.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { AccessDeniedComponent } from './pages/access-denied/access-denied.component';
import { PartnershipsComponent } from './pages/partnerships/partnerships.component';
import { TrackingComponent } from './pages/tracking/tracking.component';
import { PoolComponent } from './pages/pool/pool.component';
import { BidsComponent } from './pages/bids/bids.component';
import { DriverPortalComponent } from './pages/driver-portal/driver-portal.component';
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';
import { UserRole } from './services/auth.service';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'driver-register', component: DriverRegisterComponent },
  { path: 'access-denied', component: AccessDeniedComponent, canActivate: [AuthGuard] },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
  { path: 'tracking', component: TrackingComponent, canActivate: [AuthGuard] },
  { path: 'tracking/:trackingNumber', component: TrackingComponent, canActivate: [AuthGuard] },
  { path: 'partnerships', component: PartnershipsComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: [UserRole.SUPER_ADMIN, UserRole.VENDOR_OWNER, UserRole.DELIVERY_OWNER] } },
  { path: 'pool', component: PoolComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: [UserRole.SUPER_ADMIN, UserRole.DELIVERY_OWNER, UserRole.DRIVER] } },
  { path: 'bids', component: BidsComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: [UserRole.SUPER_ADMIN, UserRole.VENDOR_OWNER, UserRole.CLIENT] } },
  { path: 'my-jobs', component: DriverPortalComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: [UserRole.SUPER_ADMIN, UserRole.DRIVER] } },
  { path: 'notifications', component: NotificationsComponent, canActivate: [AuthGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [AuthGuard] },
  { path: 'vendorcompanies', component: VendorCompaniesComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: [UserRole.SUPER_ADMIN, UserRole.VENDOR_OWNER] } },
  { path: 'deliverycompanies', component: DeliveryCompaniesComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: [UserRole.SUPER_ADMIN, UserRole.DELIVERY_OWNER] } },
  { path: 'deliveryowners', component: DeliveryOwnersComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: [UserRole.SUPER_ADMIN] } },
  { path: 'vendorowners', component: VendorOwnersComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: [UserRole.SUPER_ADMIN] } },
  { path: 'customers', component: CustomersComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: [UserRole.SUPER_ADMIN] } },
  { path: 'products', component: ProductsComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: [UserRole.SUPER_ADMIN, UserRole.VENDOR_OWNER] } },
  { path: 'drivers', component: DriversComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: [UserRole.SUPER_ADMIN, UserRole.DELIVERY_OWNER] } },
  { path: 'orders', component: OrdersComponent, canActivate: [AuthGuard] },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
