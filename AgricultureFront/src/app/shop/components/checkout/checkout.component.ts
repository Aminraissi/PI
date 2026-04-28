import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CartService } from '../../services/cart.service';
import { PaymentApiService, CommandeRequest } from '../../services/payment-api.service';
import { ToastService } from 'src/app/core/services/toast.service';
import { AuthService } from 'src/app/services/auth/auth.service';

@Component({
  selector: 'app-checkout',
  standalone: false,
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.css']
})
export class CheckoutComponent implements OnInit {
  @Output() close   = new EventEmitter<void>();
  @Output() success = new EventEmitter<void>();

  step: 'summary' | 'processing' | 'redirecting' | 'error' = 'summary';
  error = '';

  private readonly PENDING_KEY = 'pendingCheckoutOrderId';

  constructor(
    public cartService: CartService,
    private paymentApi: PaymentApiService,
    private toast: ToastService,
    private auth: AuthService
  ) {}

  ngOnInit() {}

  proceedToPayment() {
    const userId = this.auth.getCurrentUserId();
    if (!userId) {
      this.toast.error('Vous devez être connecté.');
      return;
    }

    const request: CommandeRequest = {
      agriculteurId: userId,
      items: this.cartService.items.map(i => ({
        productId: i.product.id,
        vetId: i.product.owner?.id ?? 0,
        nomProduit: i.product.nom,
        vetNom: i.vetNom,
        vetRegion: i.vetRegion,
        prixUnitaire: i.product.prixVente ?? 0,
        quantite: i.quantity
      }))
    };

    this.step = 'processing';
    this.paymentApi.creerCommande(request).subscribe({
      next: (commande) => {
        if (!commande.stripeClientSecret || !/^https?:\/\//i.test(commande.stripeClientSecret)) {
          this.error = 'URL de paiement Stripe invalide.';
          this.step = 'error';
          this.toast.error(this.error);
          return;
        }

        // Stocker l'ID de commande pour PaymentReturnComponent
        localStorage.setItem(this.PENDING_KEY, String(commande.id));
        this.step = 'redirecting';
        window.location.href = commande.stripeClientSecret;
      },
      error: (e) => {
        this.error = e.error?.error || e.error?.message || 'Erreur lors de la création de la commande.';
        this.step = 'error';
        this.toast.error(this.error);
      }
    });
  }

  retryPayment() {
    this.step = 'summary';
    this.error = '';
  }
}