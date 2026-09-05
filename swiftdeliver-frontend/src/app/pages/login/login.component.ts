import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { AuthService, LoginRequest } from '../../services/auth.service';
import { filter, take } from 'rxjs/operators';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule]
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;

  loading: boolean = false;
  error: string = '';
  showPassword: boolean = false;

  private returnUrl: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      usernameOrEmail: ['', [Validators.required]],
      password: ['', [Validators.required]],
      rememberMe: [false]
    });
  }

  ngOnInit(): void {
    // Get return url from route parameters, otherwise fall back to role default
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || null;

    // If user is already authenticated, redirect to the return URL
    if (this.authService.isAuthenticated()) {
      this.router.navigate([this.returnUrl || this.defaultRouteFor(this.authService.getCurrentUser()?.role)]);
    }
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.error = 'Please enter both username/email and password';
      return;
    }

    this.loading = true;
    this.error = '';
    this.loginForm.disable();

    const credentials: LoginRequest = {
      usernameOrEmail: this.loginForm.get('usernameOrEmail')!.value,
      password: this.loginForm.get('password')!.value
    };
    const rememberMe = this.loginForm.get('rememberMe')!.value;

    this.authService.login(credentials).subscribe({
      next: (response) => {
        this.loading = false;
        this.loginForm.enable();
        // Store remember me preference
        if (rememberMe) {
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
        this.loginForm.enable();
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

  onDriverRegister(): void {
    this.router.navigate(['/driver-register']);
  }

  clearError(): void {
    this.error = '';
  }
}