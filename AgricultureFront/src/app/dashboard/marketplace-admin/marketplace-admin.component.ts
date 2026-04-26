import { ReservationVisiteService } from './../../services/reservation/reservation-visite.service';
import { AdminOrderService } from './../services/admin-order.service';
import { Component, OnInit } from '@angular/core';
import { ProductService } from '../../marketplace/services/product.service';
import { LocationService } from '../../services/location/location.service';
import { AdminUserService } from '../services/admin-user.service';
import { firstValueFrom } from 'rxjs';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

type AdminMarketplaceTab =
  | 'products'
  | 'rentals'
  | 'orders'
  | 'reservations'
  | 'paiement'
  | 'contracts';
  type IncomeTab = 'orders' | 'rentals';

@Component({
  selector: 'app-marketplace-admin',
  templateUrl: './marketplace-admin.component.html',
  styleUrls: ['./marketplace-admin.component.css']
})
export class MarketplaceAdminComponent implements OnInit {
  loading = false;

  activeTab: AdminMarketplaceTab = 'products';

  searchTerm = '';

  products: any[] = [];
  filteredProducts: any[] = [];
  paginatedProducts: any[] = [];

  rentals: any[] = [];
  filteredRentals: any[] = [];
  paginatedRentals: any[] = [];

  currentPage = 1;
  pageSize = 6;
  totalPages = 1;


  orders: any[] = [];
  filteredOrders: any[] = [];
  paginatedOrders: any[] = [];

  selectedOrder: any = null;
  showOrderDetails = false;


  reservations: any[] = [];
  filteredReservations: any[] = [];
  paginatedReservations: any[] = [];

  selectedReservationStatus = '';

  selectedOrderStatus = '';

  paidOrders: any[] = [];
  filteredPaidOrders: any[] = [];
  paginatedPaidOrders: any[] = [];

  selectedPaymentStatus = 'VALIDEE';

  incomeTab: IncomeTab = 'orders';

  rentalPayments: any[] = [];
  filteredRentalPayments: any[] = [];
  paginatedRentalPayments: any[] = [];

  rentalIncomeStats = {
    rentalSales: 0,
    rentalRevenue: 0,
    rentalFarmersPayout: 0,
    paidRentalPaymentsCount: 0,
    unpaidRentalPaymentsCount: 0
  };

  showAdminPopup = false;
  adminPopupTitle = '';
  adminPopupMessage = '';
  adminPopupType: 'success' | 'error' = 'success';

  paymentStats = {
    totalSales: 0,
    platformRevenue: 0,
    totalCommission: 0,
    totalTips: 0,
    farmersPayout: 0,
    paidOrdersCount: 0,
    mostBoughtProduct: '—',
    totalProductsSold: 0
  };

  contracts: any[] = [];
  filteredContracts: any[] = [];
  paginatedContracts: any[] = [];

  selectedContractStatus = '';

  constructor(
    private router: Router,
    private productService: ProductService,
    private locationService: LocationService,
    private adminUserService: AdminUserService,
    private adminOrderService: AdminOrderService,
    private reservationVisiteService: ReservationVisiteService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  setTab(tab: AdminMarketplaceTab): void {
    this.activeTab = tab;
    this.searchTerm = '';
    this.currentPage = 1;

    if (tab === 'products' && this.products.length === 0) {
      this.loadProducts();
      return;
    }

    if (tab === 'rentals' && this.rentals.length === 0) {
      this.loadRentals();
      return;
    }

    if (tab === 'orders' && this.orders.length === 0) {
    this.loadOrders();
    return;
    }

    if (tab === 'reservations' && this.reservations.length === 0) {
    this.loadReservations();
    return;
    }

    if (tab === 'paiement') {
      this.loadIncomeData();
      return;
    }


    if (tab === 'contracts' && this.contracts.length === 0) {
    this.loadContracts();
    return;
    }

    this.applyFilter();
  }

  async loadProducts(): Promise<void> {
    this.loading = true;

    this.productService.getAll().subscribe({
      next: async (data: any) => {
        const produits = data?._embedded?.produitAgricoles || data || [];

        this.products = produits.map((p: any) => ({
          id: p.id ?? this.extractId(p._links?.self?.href || ''),
          nom: p.nom,
          description: p.description,
          prix: p.prix,
          quantiteDisponible: p.quantiteDisponible,
          category: p.category,
          photoProduit: p.photoProduit
            ? `http://localhost:8090/uploads/${p.photoProduit}`
            : 'assets/images/product1.jpg',
          idUser: p.idUser,
          userName: 'Loading...'
        }));

        await this.attachUserNames(this.products);
        this.applyFilter();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading products', err);
        this.loading = false;
      }
    });
  }

  async loadRentals(): Promise<void> {
    this.loading = true;

    this.locationService.getAll().subscribe({
      next: async (data: any) => {
        let locations: any[] = [];

        if (data?._embedded?.locations) locations = data._embedded.locations;
        else if (data?.content) locations = data.content;
        else if (Array.isArray(data)) locations = data;

        this.rentals = locations.map((r: any) => ({
          id: r.id ?? this.extractId(r?._links?.self?.href),
          nom: r.nom || (r.type === 'terrain' ? 'Land Rental' : 'Machine Rental'),
          description:
            r.type === 'terrain'
              ? `Land in ${r.localisation || 'Unknown location'}`
              : `${r.marque || 'Machine'} ${r.modele || ''}`.trim(),
          prix: r.prix,
          type: r.type,
          image: r.image && r.image !== 'null'
            ? `http://localhost:8090/uploads/${r.image}`
            : 'assets/images/product1.jpg',
          idUser: r.idUser,
          userName: 'Loading...',
          marque: r.marque,
          modele: r.modele,
          etat: r.etat,
          localisation: r.localisation,
          superficie: r.superficie,
          uniteSuperficie: r.uniteSuperficie,
          typeSol: r.typeSol,
          dateDebutLocation: r.dateDebutLocation,
          dateFinLocation: r.dateFinLocation
        }));

        await this.attachUserNames(this.rentals);
        this.applyFilter();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading rentals', err);
        this.loading = false;
      }
    });
  }

  async attachUserNames(items: any[]): Promise<void> {
    const uniqueUserIds = [...new Set(items.map(item => item.idUser).filter(Boolean))];
    const userMap = new Map<number, string>();

    await Promise.all(
      uniqueUserIds.map(async (userId) => {
        try {
          const user = await firstValueFrom(this.adminUserService.getUserById(userId));
          const fullName =
            `${user?.prenom || ''} ${user?.nom || ''}`.trim() ||
            user?.username ||
            user?.email ||
            `User #${userId}`;

          userMap.set(userId, fullName);
        } catch (e) {
          console.error(`Failed to load user ${userId}`, e);
          userMap.set(userId, `User #${userId}`);
        }
      })
    );

    items.forEach(item => {
      item.userName = userMap.get(item.idUser) || `User #${item.idUser}`;
    });
  }

  applyFilter(): void {
    const term = this.searchTerm.trim().toLowerCase();

    if (this.activeTab === 'products') {
      this.filteredProducts = !term
        ? [...this.products]
        : this.products.filter(p =>
            (p.nom || '').toLowerCase().includes(term) ||
            (p.category || '').toLowerCase().includes(term) ||
            (p.userName || '').toLowerCase().includes(term)
          );
    }

    if (this.activeTab === 'rentals') {
      this.filteredRentals = !term
        ? [...this.rentals]
        : this.rentals.filter(r =>
            (r.nom || '').toLowerCase().includes(term) ||
            (r.type || '').toLowerCase().includes(term) ||
            (r.localisation || '').toLowerCase().includes(term) ||
            (r.userName || '').toLowerCase().includes(term)
          );
    }

    if (this.activeTab === 'orders') {
    this.filteredOrders = this.orders.filter(o => {
        const matchesSearch =
        !term ||
        String(o.id).includes(term) ||
        (o.userName || '').toLowerCase().includes(term) ||
        (o.statut || '').toLowerCase().includes(term);

        const matchesStatus =
        !this.selectedOrderStatus ||
        o.statut === this.selectedOrderStatus;

        return matchesSearch && matchesStatus;
    });
    }


    if (this.activeTab === 'reservations') {
        this.filteredReservations = this.reservations.filter(r => {
            const matchesSearch =
            !term ||
            (r.userName || '').toLowerCase().includes(term) ||
            (r.rentalName || '').toLowerCase().includes(term) ||
            (r.statut || '').toLowerCase().includes(term) ||
            String(r.id).includes(term);

            const matchesStatus =
            !this.selectedReservationStatus ||
            r.statut === this.selectedReservationStatus;

            return matchesSearch && matchesStatus;
        });
    }

    if (this.activeTab === 'paiement') {
  if (this.incomeTab === 'orders') {
    this.filteredPaidOrders = !term
      ? [...this.paidOrders]
      : this.paidOrders.filter(o =>
          String(o.id).includes(term) ||
          (o.userName || '').toLowerCase().includes(term) ||
          (o.statut || '').toLowerCase().includes(term)
      );
  }

  if (this.incomeTab === 'rentals') {
    this.filteredRentalPayments = !term
      ? [...this.rentalPayments]
      : this.rentalPayments.filter(p =>
          String(p.id).includes(term) ||
          String(p.locationId).includes(term) ||
          String(p.propositionId).includes(term) ||
          (p.farmerName || '').toLowerCase().includes(term) ||
          (p.tenantName || '').toLowerCase().includes(term) ||
          (p.statut || '').toLowerCase().includes(term)
      );
  }
}

    if (this.activeTab === 'contracts') {
    this.filteredContracts = !term
        ? [...this.contracts]
        : this.contracts.filter(c =>
            String(c.id).includes(term) ||
            (c.ownerName || '').toLowerCase().includes(term) ||
            (c.tenantName || '').toLowerCase().includes(term) ||
            (c.rentalName || '').toLowerCase().includes(term)
        );
    }

    this.currentPage = 1;
    this.updatePagination();
  }

  updatePagination(): void {
    const list =
        this.activeTab === 'products'
        ? this.filteredProducts
        : this.activeTab === 'rentals'
        ? this.filteredRentals
        : this.activeTab === 'orders'
        ? this.filteredOrders
        : this.activeTab === 'reservations'
        ? this.filteredReservations
        : this.activeTab === 'paiement'
        ? (this.incomeTab === 'orders' ? this.filteredPaidOrders : this.filteredRentalPayments)
        : this.activeTab === 'contracts'
        ? this.filteredContracts
        : [];

    this.totalPages = Math.max(1, Math.ceil(list.length / this.pageSize));

    if (this.currentPage > this.totalPages) {
        this.currentPage = this.totalPages;
    }

    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;

    if (this.activeTab === 'products') {
        this.paginatedProducts = this.filteredProducts.slice(start, end);
    }

    if (this.activeTab === 'rentals') {
        this.paginatedRentals = this.filteredRentals.slice(start, end);
    }

    if (this.activeTab === 'orders') {
        this.paginatedOrders = this.filteredOrders.slice(start, end);
    }

    if (this.activeTab === 'reservations') {
        this.paginatedReservations = this.filteredReservations.slice(start, end);
    }

    if (this.activeTab === 'paiement') {
      if (this.incomeTab === 'orders') {
        this.paginatedPaidOrders = this.filteredPaidOrders.slice(start, end);
      }

      if (this.incomeTab === 'rentals') {
        this.paginatedRentalPayments = this.filteredRentalPayments.slice(start, end);
      }
    }

    if (this.activeTab === 'contracts') {
        this.paginatedContracts = this.filteredContracts.slice(start, end);
    }
    }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.updatePagination();
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  extractId(url: string): number {
    return Number(url.split('/').pop());
  }

  deleteProduct(product: any): void {
    const confirmed = confirm(`Delete product "${product.nom}" ?`);
    if (!confirmed) return;

    this.productService.delete(product.id).subscribe({
      next: () => {
        this.products = this.products.filter(p => p.id !== product.id);
        this.applyFilter();
      },
      error: (err) => {
        console.error('Delete failed', err);
        alert('Failed to delete product.');
      }
    });
  }

  deleteRental(rental: any): void {
  const confirmed = confirm(`Delete rental "${rental.nom}" ?`);
  if (!confirmed) return;

  this.locationService.delete(rental.id).subscribe({
    next: () => {
      this.rentals = this.rentals.filter(r => r.id !== rental.id);
      this.applyFilter();

      this.openAdminPopup(
        'Rental Deleted',
        'The rental was deleted successfully.',
        'success'
      );
    },
    error: (err) => {
      console.error('Delete rental failed', err);

      this.openAdminPopup(
        'Delete Blocked',
        this.getErrorMessage(err),
        'error'
      );
    }
  });
}

  getOrderStatusLabel(status: string): string {
  switch (status) {
    case 'VALIDEE':
      return 'Validated';
    case 'ANNULEE':
      return 'Cancelled';
    case 'EN_COURS':
      return 'In Progress';
    default:
      return status;
  }
}

  async loadOrders(): Promise<void> {
  this.loading = true;

  this.adminOrderService.getAllOrders().subscribe({
    next: async (data: any) => {
      const commandes = Array.isArray(data) ? data : (data?.content || data?._embedded?.commandes || []);

      this.orders = commandes.map((c: any) => ({
        id: c.id,
        dateCommande: c.dateCommande,
        montantTotal: c.montantTotal,
        sousTotal: c.sousTotal,
        commission: c.commission,
        tip: c.tip,
        statut: c.statut,
        idUser: c.userId ?? c.idUser,
        panierId: c.panier_id || c.panierId,
        userName: 'Loading...'
      }));

      await this.attachUserNames(this.orders);
      this.applyFilter();
      this.loading = false;
    },
    error: (err) => {
      console.error('Error loading orders', err);
      this.loading = false;
    }
  });
}


    openOrderDetails(order: any): void {
      this.showOrderDetails = true;

      this.selectedOrder = {
        ...order,
        items: [],
        loadingDetails: true
      };

      this.adminOrderService.getOrderDetails(order.id).subscribe({
        next: (details: any) => {
          this.selectedOrder = {
            ...order,
            items: details?.items || [],
            loadingDetails: false
          };
        },
        error: (err) => {
          console.error('Failed to load order details', err);

          this.selectedOrder = {
            ...order,
            items: [],
            loadingDetails: false
          };
        }
      });
    }

closeOrderDetails(): void {
  this.showOrderDetails = false;
  this.selectedOrder = null;
}

async loadReservations(): Promise<void> {
  this.loading = true;

  this.reservationVisiteService.getAll().subscribe({
    next: async (data: any) => {
      const reservations = Array.isArray(data)
        ? data
        : (data?.content || data?._embedded?.reservationVisites || []);

      this.reservations = reservations.map((r: any) => ({
        id: r.id,
        dateVisite: r.dateVisite,
        heureDebut: r.heureDebut,
        heureFin: r.heureFin,
        statut: r.statut,
        idUser: r.userId ?? r.idUser ?? r.clientId,
        locationId:
          r.location?.id ??
          r.locationId ??
          r.idLocation ??
          this.extractIdFromHref(r._links?.location?.href),
        userName: 'Loading...',
        rentalName: 'Loading...',
        rentalType: '',
        rentalLocation: ''
      }));

      await this.attachReservationExtraData();
      this.applyFilter();
      this.loading = false;
    },
    error: (err) => {
      console.error('Error loading reservations', err);
      this.loading = false;
    }
  });
}

async attachReservationExtraData(): Promise<void> {
  await this.attachUserNames(this.reservations);

  const uniqueLocationIds = [
    ...new Set(
      this.reservations
        .map(r => r.locationId)
        .filter(id => id !== null && id !== undefined)
    )
  ];

  const rentalMap = new Map<number, any>();

  await Promise.all(
    uniqueLocationIds.map(async (locationId) => {
      try {
        const rental = await firstValueFrom(this.locationService.getById(locationId));
        rentalMap.set(locationId, rental);
      } catch (e) {
        console.error('Failed to load rental', locationId, e);
      }
    })
  );

  this.reservations = this.reservations.map(r => {
    const rental = rentalMap.get(r.locationId);

    return {
      ...r,
      rentalName:
        rental?.nom ||
        (rental?.type === 'terrain' ? 'Land Rental' : 'Machine Rental') ||
        `Rental #${r.locationId}`,
      rentalType: rental?.type || '',
      rentalLocation: rental?.localisation || ''
    };
  });
}

extractIdFromHref(href?: string): number | null {
  if (!href) return null;
  const parts = href.split('/');
  const last = parts[parts.length - 1];
  return last ? Number(last) : null;
}

getReservationStatusLabel(status: string): string {
  switch (status) {
    case 'EN_ATTENTE':
      return 'Pending';
    case 'CONFIRMEE':
      return 'Confirmed';
    case 'REFUSEE':
      return 'Refused';
    case 'TERMINEE':
      return 'Completed';
    case 'ANNULEE':
      return 'Cancelled';
    default:
      return status;
  }
}

deleteReservation(reservation: any): void {
  const confirmed = confirm(
    `Delete reservation #${reservation.id} for ${reservation.userName}?`
  );

  if (!confirmed) return;

  this.reservationVisiteService.deleteReservation(reservation.id).subscribe({
    next: () => {
      this.reservations = this.reservations.filter(r => r.id !== reservation.id);
      this.applyFilter();
    },
    error: (err) => {
      console.error('Delete reservation failed', err);
      alert('Failed to delete reservation.');
    }
  });
}

async loadIncomeData(): Promise<void> {
  this.loading = true;

  try {
    await this.loadPaidOrders();
    await this.loadRentalPayments();

    this.applyFilter();
  } catch (err) {
    console.error('Error loading income data', err);
  } finally {
    this.loading = false;
  }
}


async loadPaidOrders(): Promise<void> {

  this.adminOrderService.getAllOrders().subscribe({
    next: async (data: any) => {
      const commandes = Array.isArray(data)
        ? data
        : (data?.content || data?._embedded?.commandes || []);

      const validatedOrders = commandes.filter((c: any) => c.statut === 'VALIDEE');

      const detailedOrders = await Promise.all(
        validatedOrders.map(async (c: any) => {
          let details: any = null;

          try {
            details = await firstValueFrom(this.adminOrderService.getOrderDetails(c.id));
          } catch (e) {
            console.error('Failed to load paid order details', c.id, e);
          }

          return {
            id: c.id,
            dateCommande: c.dateCommande,
            montantTotal: c.montantTotal,
            sousTotal: c.sousTotal,
            commission: c.commission,
            tip: c.tip,
            statut: c.statut,
            idUser: c.userId ?? c.idUser,
            panierId: c.panierId ?? c.panier_id,
            userName: 'Loading...',
            items: details?.items || []
          };
        })
      );

      this.paidOrders = detailedOrders;

      await this.attachUserNames(this.paidOrders);
      this.computePaymentStats();
      this.applyFilter();
    },
    error: (err) => {
      console.error('Error loading paid orders', err);
    }
  });
}

async loadRentalPayments(): Promise<void> {
  try {
    const data: any = await firstValueFrom(
      this.http.get('http://localhost:8089/paiement/api/v1/rental-payments')
    );

    const payments = Array.isArray(data)
      ? data
      : (data?.content || data?._embedded?.rentalPayments || []);

    this.rentalPayments = payments.map((p: any) => ({
      id: p.id,

      agriculteurId: p.agriculteurId ?? p.agriculteur_id,
      locataireId: p.locataireId ?? p.locataire_id,
      locationId: p.locationId ?? p.location_id,
      propositionId: p.propositionId ?? p.proposition_id,

      moisNumero: p.moisNumero ?? p.mois_numero,
      dateEcheance: p.dateEcheance ?? p.date_echeance,
      datePaiement: p.datePaiement ?? p.date_paiement,

      montant: Number(p.montant) || 0,
      statut: p.statut,

      commissionAmount: Number(p.commissionAmount ?? p.commission_amount) || 0,
      commissionRate: Number(p.commissionRate ?? p.commission_rate) || 0,
      farmerAmount: Number(p.farmerAmount ?? p.farmer_amount) || 0,

      farmerName: 'Loading...',
      tenantName: 'Loading...'
    }));

    await this.attachRentalPaymentUserNames();

    this.computeRentalIncomeStats();

    this.filteredRentalPayments = [...this.rentalPayments];
    this.currentPage = 1;
    this.updatePagination();

  } catch (err) {
    console.error('Error loading rental payments', err);

    this.rentalPayments = [];
    this.filteredRentalPayments = [];
    this.paginatedRentalPayments = [];

    this.computeRentalIncomeStats();
    this.updatePagination();
  }
}

async attachRentalPaymentUserNames(): Promise<void> {
  const userIds = [
    ...new Set(
      this.rentalPayments
        .flatMap(p => [p.agriculteurId, p.locataireId])
        .filter(id => id !== null && id !== undefined)
    )
  ];

  const userMap = new Map<number, string>();

  await Promise.all(
    userIds.map(async (userId) => {
      try {
        const user = await firstValueFrom(this.adminUserService.getUserById(userId));
        const fullName =
          `${user?.prenom || ''} ${user?.nom || ''}`.trim() ||
          user?.username ||
          user?.email ||
          `User #${userId}`;

        userMap.set(Number(userId), fullName);
      } catch (e) {
        userMap.set(Number(userId), `User #${userId}`);
      }
    })
  );

  this.rentalPayments = this.rentalPayments.map(p => ({
    ...p,
    farmerName: userMap.get(Number(p.agriculteurId)) || `User #${p.agriculteurId}`,
    tenantName: userMap.get(Number(p.locataireId)) || `User #${p.locataireId}`
  }));
}

computeRentalIncomeStats(): void {
  const paid = this.rentalPayments.filter(p =>
    String(p.statut || '').toUpperCase() === 'PAID'
  );

  const unpaid = this.rentalPayments.filter(p =>
    String(p.statut || '').toUpperCase() === 'UNPAID'
  );

  const rentalSales = paid.reduce(
    (sum, p) => sum + (Number(p.montant) || 0),
    0
  );

  const rentalRevenue = paid.reduce(
    (sum, p) => sum + (Number(p.commissionAmount) || 0),
    0
  );

  const rentalFarmersPayout = paid.reduce(
    (sum, p) => sum + (Number(p.farmerAmount) || 0),
    0
  );

  this.rentalIncomeStats = {
    rentalSales,
    rentalRevenue,
    rentalFarmersPayout,
    paidRentalPaymentsCount: paid.length,
    unpaidRentalPaymentsCount: unpaid.length
  };
}

computePaymentStats(): void {
  const totalSales = this.paidOrders.reduce(
    (sum, order) => sum + (Number(order.montantTotal) || 0),
    0
  );

  const totalCommission = this.paidOrders.reduce(
    (sum, order) => sum + (Number(order.commission) || 0),
    0
  );

  const totalTips = this.paidOrders.reduce(
    (sum, order) => sum + (Number(order.tip) || 0),
    0
  );

  const farmersPayout = this.paidOrders.reduce(
    (sum, order) => sum + (Number(order.sousTotal) || 0),
    0
  );

  const platformRevenue = totalCommission + totalTips;

  const productCounter = new Map<string, number>();
  let totalProductsSold = 0;

  this.paidOrders.forEach(order => {
    (order.items || []).forEach((item: any) => {
      const productName = item.nom || 'Unknown Product';
      const qty = Number(item.quantite) || 0;

      totalProductsSold += qty;
      productCounter.set(productName, (productCounter.get(productName) || 0) + qty);
    });
  });

  let mostBoughtProduct = '—';
  let maxQty = 0;

  productCounter.forEach((qty, name) => {
    if (qty > maxQty) {
      maxQty = qty;
      mostBoughtProduct = name;
    }
  });

  this.paymentStats = {
    totalSales,
    platformRevenue,
    totalCommission,
    totalTips,
    farmersPayout,
    paidOrdersCount: this.paidOrders.length,
    mostBoughtProduct,
    totalProductsSold
  };
}


isFinalizedContract(proposal: any): boolean {
  return !!proposal?.signatureAgriculteur && !!proposal?.signatureClient;
}

async loadContracts(): Promise<void> {
  this.loading = true;

  this.reservationVisiteService.getAllProposals().subscribe({
    next: async (proposals: any[]) => {
      const list = Array.isArray(proposals)
        ? proposals
        : ((proposals as any)?.content || (proposals as any)?._embedded?.propositionLocations || []);

      const finalized = list.filter((p: any) =>
        p.statut === 'FINALISEE' || this.isFinalizedContract(p)
      );

      this.contracts = finalized.map((p: any) => ({
        id: p.id,
        agriculteurId: p.agriculteurId,
        locataireId: p.locataireId,
        locationId: p.locationId,
        dateDebut: p.dateDebut,
        dateFin: p.dateFin,
        nbMois: p.nbMois,
        montantMensuel: p.montantMensuel,
        montantTotal: p.montantTotal,
        statut: p.statut,
        signatureAgriculteur: p.signatureAgriculteur,
        signatureClient: p.signatureClient,
        ownerName: 'Loading...',
        tenantName: 'Loading...',
        rentalName: 'Loading...',
        rentalType: '',
        rentalLocation: ''
      }));

      await this.attachContractExtraData();
      this.applyFilter();
      this.loading = false;
    },
    error: (err) => {
      console.error('Error loading contracts', err);
      this.loading = false;
    }
  });
}

async attachContractExtraData(): Promise<void> {
  const ownerIds = [...new Set(this.contracts.map(c => c.agriculteurId).filter((id: any) => id != null))];
  const tenantIds = [...new Set(this.contracts.map(c => c.locataireId).filter((id: any) => id != null))];
  const locationIds = [...new Set(this.contracts.map(c => c.locationId).filter((id: any) => id != null))];

  const userMap = new Map<number, string>();
  const rentalMap = new Map<number, any>();

  await Promise.all([
    ...ownerIds.map(async (id) => {
      try {
        const user = await firstValueFrom(this.adminUserService.getUserById(id));
        userMap.set(id, `${user?.prenom || ''} ${user?.nom || ''}`.trim() || user?.username || user?.email || `User #${id}`);
      } catch {
        userMap.set(id, `User #${id}`);
      }
    }),
    ...tenantIds.map(async (id) => {
      try {
        const user = await firstValueFrom(this.adminUserService.getUserById(id));
        userMap.set(id, `${user?.prenom || ''} ${user?.nom || ''}`.trim() || user?.username || user?.email || `User #${id}`);
      } catch {
        userMap.set(id, `User #${id}`);
      }
    }),
    ...locationIds.map(async (id) => {
      try {
        const rental = await firstValueFrom(this.locationService.getById(id));
        rentalMap.set(id, rental);
      } catch {
        rentalMap.set(id, null);
      }
    })
  ]);

  this.contracts = this.contracts.map(c => {
    const rental = rentalMap.get(c.locationId);

    return {
      ...c,
      ownerName: userMap.get(c.agriculteurId) || `User #${c.agriculteurId}`,
      tenantName: userMap.get(c.locataireId) || `User #${c.locataireId}`,
      rentalName: rental?.nom || (rental?.type === 'terrain' ? 'Land Rental' : 'Machine Rental') || `Rental #${c.locationId}`,
      rentalType: rental?.type || '',
      rentalLocation: rental?.localisation || ''
    };
  });
}

getContractStatusLabel(status: string): string {
  switch (status) {
    case 'SIGNEE':
      return 'Signed';
    case 'ACCEPTEE':
      return 'Accepted';
    case 'FINALISEE':
      return 'Finalized';
    default:
      return 'Finalized Contract';
  }
}

viewContract(contract: any): void {
  this.router.navigate(
    ['/marketplace/rental-contract', contract.id],
    { queryParams: { from: 'admin' } }
  );
}

openAdminPopup(
  title: string,
  message: string,
  type: 'success' | 'error' = 'success'
): void {
  this.adminPopupTitle = title;
  this.adminPopupMessage = message;
  this.adminPopupType = type;
  this.showAdminPopup = true;
}

closeAdminPopup(): void {
  this.showAdminPopup = false;
}

getErrorMessage(err: any): string {
  if (err?.error?.message) {
    return err.error.message;
  }

  if (typeof err?.error === 'string') {
    return err.error;
  }

  if (err?.message) {
    return err.message;
  }

  return 'Something went wrong.';
}


setIncomeTab(tab: IncomeTab): void {
  this.incomeTab = tab;
  this.searchTerm = '';
  this.currentPage = 1;

  if (tab === 'rentals' && this.rentalPayments.length === 0) {
    this.loadRentalPayments();
    return;
  }

  if (tab === 'orders' && this.paidOrders.length === 0) {
    this.loadPaidOrders();
    return;
  }

  this.applyFilter();
}

}
