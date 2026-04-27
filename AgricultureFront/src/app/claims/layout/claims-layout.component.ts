import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';

@Component({
  selector: 'app-claims-layout',
  standalone: false,
  templateUrl: './claims-layout.component.html',
  styleUrls: ['./claims-layout.component.css']
})
export class ClaimsLayoutComponent implements OnInit {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (!this.authService.getCurrentUser()) {
      this.router.navigate(['/claims/auth']);
    }
  }
}
