import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PaymentApiService } from 'src/app/shop/services/payment-api.service';
import { CartService } from 'src/app/shop/services/cart.service';
import { ToastService } from 'src/app/core/services/toast.service';

@Component({
  selector: 'app-payment-return',
  standalone: false,
  template: `
    <div class="payment-return-wrapper">

      <div *ngIf="status === 'processing'" class="pr-card">
        <div class="pr-spinner"></div>
        <h3>Confirmation du paiement...</h3>
        <p>Veuillez patienter</p>
      </div>

      <div *ngIf="status === 'success'" class="pr-card pr-success">
        <div class="pr-icon">✓</div>
        <h3>Paiement réussi !</h3>
        <p>Votre commande <strong>CMD-{{ orderId }}</strong> a été validée.</p>
        <button class="pr-btn" (click)="goToShop()">Retour au shop</button>
      </div>

      <div *ngIf="status === 'cancel'" class="pr-card pr-cancel">
        <div class="pr-icon">✕</div>
        <h3>Paiement annulé</h3>
        <p>Votre paiement a été annulé. Votre panier est conservé.</p>
        <button class="pr-btn" (click)="goToShop()">Retour au shop</button>
      </div>

      <div *ngIf="status === 'error'" class="pr-card pr-error">
        <div class="pr-icon">!</div>
        <h3>Erreur</h3>
        <p>{{ errorMsg }}</p>
        <button class="pr-btn" (click)="goToShop()">Retour au shop</button>
      </div>

    </div>
  `,
  styles: [`
    .payment-return-wrapper {
      display: flex; align-items: center; justify-content: center;
      min-height: 60vh; padding: 2rem;
    }
    .pr-card {
      background: white; border-radius: 16px; padding: 3rem 2.5rem;
      text-align: center; box-shadow: 0 8px 32px rgba(0,0,0,.12);
      max-width: 420px; width: 100%;
    }
    .pr-icon { font-size: 3rem; margin-bottom: 1rem; }
    .pr-success .pr-icon { color: #22c55e; }
    .pr-cancel  .pr-icon { color: #f59e0b; }
    .pr-error   .pr-icon { color: #ef4444; }
    .pr-spinner {
      width: 48px; height: 48px; border: 4px solid #e5e7eb;
      border-top-color: #22c55e; border-radius: 50%;
      animation: spin 0.8s linear infinite; margin: 0 auto 1.5rem;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    h3 { font-size: 1.4rem; margin-bottom: .5rem; }
    p  { color: #6b7280; margin-bottom: 1.5rem; }
    .pr-btn {
      background: #22c55e; color: white; border: none;
      padding: .75rem 2rem; border-radius: 8px; cursor: pointer;
      font-size: 1rem; font-weight: 600;
    }
    .pr-btn:hover { background: #16a34a; }
  `]
})
export class PaymentReturnComponent implements OnInit {

  status: 'processing' | 'success' | 'cancel' | 'error' = 'processing';
  orderId: number | null = null;
  errorMsg = '';

  private readonly STORAGE_KEY = 'pendingCheckoutOrderId';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentApi: PaymentApiService,
    private cartService: CartService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    const payment = this.route.snapshot.queryParamMap.get('payment');

    // Nettoyer les query params de l'URL
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {},
      replaceUrl: true
    });

    if (payment === 'cancel') {
      localStorage.removeItem(this.STORAGE_KEY);
      this.status = 'cancel';
      this.toast.error('Paiement annulé.');
      return;
    }

    if (payment === 'success') {
      const raw = localStorage.getItem(this.STORAGE_KEY);
      const pendingOrderId = raw ? Number(raw) : NaN;

      if (!Number.isFinite(pendingOrderId)) {
        this.status = 'error';
        this.errorMsg = 'Paiement réussi, mais commande introuvable en local.';
        this.toast.error(this.errorMsg);
        return;
      }

      this.orderId = pendingOrderId;
      this.status = 'processing';

      this.paymentApi.confirmerPaiementCommande(pendingOrderId).subscribe({
        next: () => {
          localStorage.removeItem(this.STORAGE_KEY);
          this.cartService.clear();
          this.status = 'success';
          this.toast.success('Paiement confirmé !');
        },
        error: (e) => {
          this.status = 'error';
          this.errorMsg = e.error?.error || e.error?.message || 'Erreur lors de la confirmation.';
          this.toast.error(this.errorMsg);
        }
      });
      return;
    }

    // Pas de query param ?payment= → rediriger
    this.router.navigate(['/appointments']);
  }

  goToShop() {
    this.router.navigate(['/appointments']);
  }
}