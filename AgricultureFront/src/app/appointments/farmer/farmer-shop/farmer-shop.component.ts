import { Component, Input, OnInit, Output, EventEmitter } from '@angular/core';
import { InventoryApiService } from 'src/app/inventory/services/inventory-api.service';
import { InventoryProduct } from 'src/app/inventory/models/inventory.models';
import { CartService } from 'src/app/shop/services/cart.service';

@Component({
  selector: 'app-farmer-shop',
  standalone: false,
  templateUrl: './farmer-shop.component.html',
  styleUrls: ['./farmer-shop.component.css']
})
export class FarmerShopComponent implements OnInit {
  @Input() vetId!: number;
  @Input() vetName = '';
  @Input() vetRegion = '';
  @Output() back = new EventEmitter<void>();

  products: InventoryProduct[] = [];
  loading = true;
  error = '';
  searchTerm = '';
  selectedCategory = '';
  selectedProduct: InventoryProduct | null = null;
  selectedQty = 1;

  showCart = false;
  showCheckout = false;

  addedProductId: number | null = null;

  categories = [
    { value: '', label: 'All Categories' },
    { value: 'VACCIN',     label: '💉 Vaccines' },
    { value: 'MEDICAMENT', label: '💊 Medications' },
    { value: 'ALIMENT',    label: '🌾 Feeds' },
    { value: 'RECOLTE',    label: '🌿 Crops' },
    { value: 'AUTRE',      label: '📦 Other' },
  ];

  constructor(private api: InventoryApiService, public cartService: CartService) {}

  ngOnInit() {
    this.api.getPublicShop(this.vetId).subscribe({
      next: p => { this.products = p; this.loading = false; },
      error: () => { this.loading = false; this.error = 'Unable to load the store.'; }
    });
  }

  get filtered(): InventoryProduct[] {
    return this.products.filter(p => {
      const matchSearch = !this.searchTerm ||
        p.nom.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        (p.description || '').toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchCat = !this.selectedCategory || p.categorie === this.selectedCategory;
      return matchSearch && matchCat;
    });
  }

  openDetail(p: InventoryProduct) {
    this.selectedProduct = p;
    this.selectedQty = 1;
  }
  closeDetail() { this.selectedProduct = null; }

  addToCart(p: InventoryProduct, qty: number = 1) {
    if ((p.currentQuantity ?? 0) <= 0) return;
    const added = this.cartService.addItem(p, qty, this.vetId, this.vetName, this.vetRegion);
    if (added) {
      this.addedProductId = p.id;
      setTimeout(() => { this.addedProductId = null; }, 1500);
    }
  }

  addDetailToCart() {
    if (this.selectedProduct) {
      this.addToCart(this.selectedProduct, this.selectedQty);
      this.closeDetail();
    }
  }

  imageUrl(p: InventoryProduct): string {
    return this.api.resolveMediaUrl(p.imageUrl);
  }

  categoryLabel(c: string): string {
    return { VACCIN:'Vaccines', MEDICAMENT:'Medications', ALIMENT:'Feeds', RECOLTE:'Crops', AUTRE:'Other' }[c] || c;
  }

  categoryEmoji(c: string): string {
    return { VACCIN:'💉', MEDICAMENT:'💊', ALIMENT:'🌾', RECOLTE:'🌿', AUTRE:'📦' }[c] || '📦';
  }

  get inStockCount()    { return this.products.filter(p => (p.currentQuantity ?? 0) > 0).length; }
  get outOfStockCount() { return this.products.filter(p => !((p.currentQuantity ?? 0) > 0)).length; }

  openCart()  { this.showCart = true; }
  closeCart() { this.showCart = false; }

  goCheckout() { this.showCart = false; this.showCheckout = true; }
  closeCheckout() { this.showCheckout = false; }
}
