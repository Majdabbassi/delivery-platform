import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-access-denied',
  templateUrl: './access-denied.component.html',
  styleUrl: './access-denied.component.css',
  standalone: false
})
export class AccessDeniedComponent {
  constructor(private router: Router, private authService: AuthService) {}

  goBack(): void {
    if (window.history.length > 1) {
      window.history.back();
    } else {
      this.router.navigate(['/dashboard']);
    }
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}