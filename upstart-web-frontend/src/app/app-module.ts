import { NgModule, provideBrowserGlobalErrorListeners } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppRoutingModule } from './app-routing-module';
import { App } from './app';
import { Navbar } from './components/navbar/navbar';
import { Sidebar } from './components/sidebar/sidebar';
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

import { ThemeService } from './services/theme.service';
import { AuthInterceptor } from './interceptors/auth.interceptor';


@NgModule({
  declarations: [
    App,
    Navbar,
    Sidebar,
    MessagerieComponent,
    NotificationsComponent,
    ProfileComponent,
    SettingsComponent,
    VendorCompaniesComponent,
    DeliveryCompaniesComponent,
    CustomersComponent,
    ProductsComponent,
    DeliveryOwnersComponent,
    VendorOwnersComponent,
    AdminsComponent,
    DriversComponent,
    OrdersComponent,
    LoginComponent,
    DashboardComponent,

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
