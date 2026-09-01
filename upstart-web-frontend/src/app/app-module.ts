import { NgModule, provideBrowserGlobalErrorListeners } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppRoutingModule } from './app-routing-module';
import { App } from './app';
import { Navbar } from './components/navbar/navbar';
import { Sidebar } from './components/sidebar/sidebar';
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
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { AccessDeniedComponent } from './pages/access-denied/access-denied.component';
import { PartnershipsComponent } from './pages/partnerships/partnerships.component';
import { TrackingComponent } from './pages/tracking/tracking.component';
import { PoolComponent } from './pages/pool/pool.component';
import { BidsComponent } from './pages/bids/bids.component';
import { DriverPortalComponent } from './pages/driver-portal/driver-portal.component';

import { ThemeService } from './services/theme.service';
import { AuthInterceptor } from './interceptors/auth.interceptor';


@NgModule({
  declarations: [
    App,
    Navbar,
    Sidebar,
    NotificationsComponent,
    ProfileComponent,
    VendorCompaniesComponent,
    DeliveryCompaniesComponent,
    CustomersComponent,
    ProductsComponent,
    DeliveryOwnersComponent,
    VendorOwnersComponent,
    DriversComponent,
    OrdersComponent,
    LoginComponent,
    RegisterComponent,
    DashboardComponent,
    AccessDeniedComponent,
    PartnershipsComponent,
    TrackingComponent,
    PoolComponent,
    BidsComponent,
    DriverPortalComponent,
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    FormsModule,
    HttpClientModule
  ],
  providers: [
    provideBrowserGlobalErrorListeners(),
    ThemeService,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    }
  ],
  bootstrap: [App]
})
export class AppModule { }
