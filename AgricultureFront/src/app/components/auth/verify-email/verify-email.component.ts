import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../services/auth/auth.service';

@Component({
  selector: 'app-verify-email',
  templateUrl: './verify-email.component.html',
  styleUrls: ['./verify-email.component.css']
})
export class VerifyEmailComponent implements OnInit, OnDestroy {
  status: 'loading' | 'success' | 'error' = 'loading';
  message = '';
  detail = '';
  redirectCountdown = 5;
  private timer: any;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token');
    
    if (!token) {
      this.status = 'error';
      this.message = 'Lien de vérification invalide';
      this.detail = 'Aucun token trouvé dans l\'URL';
      return;
    }

    this.authService.verifyEmail(token).subscribe({
   next: (response) => {
  if (response.userId) {

    this.status = 'success';
    this.message = 'Email vérifié avec succès !';
    this.detail = `Bienvenue ${response.email || 'utilisateur'}`;

    if (response.nextStep === 'SIGNUP_STEP2') {
      this.router.navigate(['/register-extra']);
      return;
    }

    if (response.nextStep === 'LOGIN') {
      this.startRedirectTimer();
      return;
    }

  } else {
    this.status = 'error';
    this.message = 'Échec de la vérification';
    this.detail = response.message || 'Une erreur est survenue';
  }
}
    });
  }

  startRedirectTimer() {
    this.timer = setInterval(() => {
      this.redirectCountdown--;
      if (this.redirectCountdown === 0) {
        clearInterval(this.timer);
        this.router.navigate(['/auth']);
      }
    }, 1000);
  }

  goToLogin() {
    if (this.timer) clearInterval(this.timer);
    this.router.navigate(['/auth']);
  }

  resendVerification() {
    this.status = 'loading';
  }

  ngOnDestroy() {
    if (this.timer) clearInterval(this.timer);
  }
}