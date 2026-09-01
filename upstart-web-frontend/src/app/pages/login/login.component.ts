import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService, LoginRequest } from '../../services/auth.service';
import { filter, take } from 'rxjs/operators';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: false
})
export class LoginComponent implements OnInit {
  loginForm: LoginRequest = {
    usernameOrEmail: '',
    password: ''
  };
  
  loading: boolean = false;
  error: string = '';
  showPassword: boolean = false;
  rememberMe: boolean = false;
  
  private returnUrl: string | null = null;
  
  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}
  
  ngOnInit(): void {
    // Get return url from route parameters, otherwise fall back to role default
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || null;
    
    // If user is already authenticated, redirect to the return URL
    if (this.authService.isAuthenticated()) {
      this.router.navigate([this.returnUrl || this.defaultRouteFor(this.authService.getCurrentUser()?.role)]);
    }
  }
  
  onSubmit(): void {
    if (!this.loginForm.usernameOrEmail || !this.loginForm.password) {
      this.error = 'Please enter both username/email and password';
      return;
    }
    
    this.loading = true;
    this.error = '';
    
    this.authService.login(this.loginForm).subscribe({
      next: (response) => {
        this.loading = false;
        // Store remember me preference
        if (this.rememberMe) {
          localStorage.setItem('rememberMe', 'true');
        }
        // Wait for authentication state to be updated before navigating
        this.authService.isAuthenticated$
          .pipe(
            filter(isAuth => isAuth === true),
            take(1)
          )
          .subscribe(() => {
            const target = this.returnUrl || this.defaultRouteFor(response.user.role);
            this.router.navigate([target]);
          });
      },
      error: (error) => {
        this.loading = false;
        console.error('Login error:', error);
        
        // Handle different error scenarios
        if (error.status === 401) {
          this.error = 'Invalid username or password';
        } else if (error.status === 403) {
          this.error = 'Account is locked or disabled';
        } else if (error.status === 0) {
          this.error = 'Unable to connect to server. Please try again later.';
        } else {
          this.error = 'Login failed. Please try again.';
        }
      }
    });
  }
  
  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  private defaultRouteFor(role?: string): string {
    switch (role) {
      case 'SUPER_ADMIN': return '/dashboard';
      case 'VENDOR_OWNER': return '/vendorcompanies';
      case 'DELIVERY_OWNER': return '/deliverycompanies';
      case 'DRIVER': return '/my-jobs';
      case 'CLIENT':
      default: return '/orders';
    }
  }
  
  onForgotPassword(): void {
    this.error = '';
    window.alert('Password reset is managed by your account administrator. Please contact support for assistance.');
  }
  
  onRegister(): void {
    this.router.navigate(['/register']);
  }
  
  clearError(): void {
    this.error = '';
  }
}