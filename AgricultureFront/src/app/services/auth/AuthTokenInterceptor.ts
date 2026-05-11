import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable()
export class AuthTokenInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService) {}

  private isApiRequest(url: string): boolean {
    return (
      url.startsWith('/forums/') ||
      url.startsWith('/user/') ||
      url.startsWith('/livraison/') ||
      url.startsWith('/osrm/') ||
      url.startsWith('/Vente/') ||
      url.startsWith('/formation/') ||
      url.startsWith('/explorer/') ||
      url.startsWith('/evenement/') ||
      url.startsWith('/support/') ||
      url.startsWith('/reclamations/') ||
      url.startsWith('/pret/') ||
      url.startsWith('/inventaires/') ||
      url.startsWith('/assistance/') ||
      url.startsWith('/paiement/')
    );
  }

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {

    const token = this.authService.getToken();
    let request = req;

    // 1️⃣ Ajouter token seulement si API
    if (this.isApiRequest(req.url) && token) {
      request = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    // 2️⃣ Normaliser URL Gateway (optionnel mais propre)
    const cleanedUrl = request.url
      .replace('http://localhost:8089', '')
      .replace('http://localhost:8095', '');

    if (cleanedUrl !== request.url) {
      request = request.clone({ url: cleanedUrl });
    }

    return next.handle(request);
  }
}
