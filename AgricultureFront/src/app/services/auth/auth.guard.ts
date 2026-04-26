import { Injectable } from '@angular/core';
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService, BackendRole } from './auth.service';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | boolean {
    if (!this.authService.hasActiveSession()) {
      this.router.navigate(['/auth'], { queryParams: { returnUrl: state.url } });
      return false;
    }

    const user = this.authService.getCurrentUser();
    if (!user) {
      this.router.navigate(['/auth'], { queryParams: { returnUrl: state.url } });
      return false;
    }

    return this.authService.validateCurrentSession().pipe(
      map((response) => {
        if (!response.valid) {
          this.router.navigate(['/auth'], { queryParams: { returnUrl: state.url } });
          return false;
        }

        const requiredRoles = route.data['roles'] as BackendRole[] | undefined;
        if (requiredRoles && !this.authService.hasAnyRole(...requiredRoles)) {
          const fallbackRoute = this.authService.getDefaultRouteForRole(user.role);
          this.router.navigate([fallbackRoute]);
          return false;
        }

        return true;
      }),
      catchError(() => {
        this.authService.setAuthNotice('Your session is no longer valid.');
        this.authService.logout();
        this.router.navigate(['/auth'], { queryParams: { returnUrl: state.url } });
        return of(false);
      })
    );
  }
}
