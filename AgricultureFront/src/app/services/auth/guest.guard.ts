import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class GuestGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(): Observable<boolean> | boolean {
    if (this.authService.hasActiveSession()) {
      return this.authService.validateCurrentSession().pipe(
        map((response) => {
          if (!response.valid) {
            return true;
          }

          const redirectRoute = this.authService.getDefaultRouteForRole(this.authService.getCurrentRole());
          this.router.navigate([redirectRoute]);
          return false;
        }),
        catchError(() => {
          this.authService.setAuthNotice('Your session is no longer valid.');
          this.authService.logout();
          return of(true);
        })
      );
    }

    return true;
  }
}
