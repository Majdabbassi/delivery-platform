import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MessagerieComponent } from './pages/messagerie/messagerie.component';
import { NotificationsComponent } from './pages/notifications/notifications.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { SettingsComponent } from './pages/settings/settings.component';
import { VendorCompaniesComponent } from './pages/vendor-companies/vendor-companies.component';
import { DeliveryCompaniesComponent } from './pages/delivery-companies/delivery-companies.component';
import { CustomersComponent } from './pages/customers/customers.component';
import { ProductsComponent } from './pages/products/products.component';
import { DeliveryOwnersComponent } from './pages/delivery-owners/delivery-owners.component';
import { VendorOwnersComponent } from './pages/vendor-owners/vendor-owners.component';
import { AdminsComponent } from './pages/admins/admins.component';
import { DriversComponent } from './pages/drivers/drivers.component';
import { OrdersComponent } from './pages/orders/orders.component';
import { LoginComponent } from './pages/login/login.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';
import { UserRole } from './services/auth.service';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: [UserRole.SUPER_ADMIN] } },
  { path: 'messagerie', component: MessagerieComponent, canActivate: [AuthGuard] },
  { path: 'notifications', component: NotificationsComponent, canActivate: [AuthGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [AuthGuard] },
  { path: 'settings', component: SettingsComponent, canActivate: [AuthGuard] },
  { path: 'vendorcompanies', component: VendorCompaniesComponent, canActivate: [AuthGuard] },
  { path: 'deliverycompanies', component: DeliveryCompaniesComponent, canActivate: [AuthGuard] },
  { path: 'deliveryowners', component: DeliveryOwnersComponent, canActivate: [AuthGuard] },
  { path: 'vendorowners', component: VendorOwnersComponent, canActivate: [AuthGuard] },
  { path: 'customers', component: CustomersComponent, canActivate: [AuthGuard] },
  { path: 'products', component: ProductsComponent, canActivate: [AuthGuard] },
  { path: 'admins', component: AdminsComponent, canActivate: [AuthGuard] },
  { path: 'drivers', component: DriversComponent, canActivate: [AuthGuard] },
  { path: 'orders', component: OrdersComponent, canActivate: [AuthGuard] },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
