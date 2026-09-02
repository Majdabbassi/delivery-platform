import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService, RegisterRequest } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrl: './register.component.css',
  standalone: false
})
export class RegisterComponent {
  registerForm: RegisterRequest = {
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phoneNumber: ''
  };

  confirmPassword = '';
  loading = false;
  success = false;
  error = '';
  showPassword = false;

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit(): void {
    this.error = '';
    this.success = false;

    if (!this.registerForm.firstName || !this.registerForm.lastName ||
        !this.registerForm.email || !this.registerForm.username || !this.registerForm.password) {
      this.error = 'Please fill in all required fields';
      return;
    }

    if (this.registerForm.password.length < 8) {
      this.error = 'Password must be at least 8 characters long';
      return;
    }

    if (this.registerForm.password !== this.confirmPassword) {
      this.error = 'Passwords do not match';
      return;
    }

    this.loading = true;
    this.authService.register(this.registerForm).subscribe({
      next: () => {
        this.loading = false;
        this.success = true;
        this.error = '';
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 400) {
          const message = typeof err.error === 'string' ? err.error : (err.error?.message || 'Invalid registration data.');
          this.error = message || 'Invalid registration data. Please check your details.';
        } else if (err.status === 409) {
          this.error = 'An account with this username or email already exists.';
        } else if (err.status === 0) {
          this.error = 'Unable to connect to server. Please try again later.';
        } else {
          this.error = 'Registration failed. Please try again.';
        }
      }
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}