import { Component } from '@angular/core';
import { CartService, CartConflict } from '../../services/cart.service';

@Component({
  selector: 'app-cart-conflict',
  standalone: false,
  template: `
<ng-container *ngIf="cartService.conflict$ | async as conflict">
  <div class="conflict-backdrop" (click)="keep()"></div>
  <div class="conflict-modal">
    <div class="conflict-icon">🛒⚠️</div>
    <h3 class="conflict-title">Different veterinary products</h3>
    <p class="conflict-body">
      Your cart already contains products from
      <strong>Dr. {{ conflict.currentVetNom }}</strong>.<br>
      You cannot mix products from multiple veterinarians in a single order.
    </p>
    <p class="conflict-question">
      Do you want to empty the current cart and start a new order with
      <strong>Dr. {{ conflict.newVetNom }}</strong> ?
    </p>
    <div class="conflict-actions">
      <button class="btn-keep" (click)="keep()">
        <i class="fas fa-arrow-left"></i> Keep the current cart
      </button>
      <button class="btn-replace" (click)="replace()">
        <i class="fas fa-trash-alt"></i> Empty and restart
      </button>
    </div>
  </div>
</ng-container>
  `,
  styleUrls: ['./cart-conflict.component.css']
})
export class CartConflictComponent {
  constructor(public cartService: CartService) {}
  keep()    { this.cartService.resolveConflictKeep(); }
  replace() { this.cartService.resolveConflictReplace(); }
}
