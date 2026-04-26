import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AdminUser, AdminUserService } from '../services/admin-user.service';

type AdminTab = 'all' | 'review';
type AdminViewMode = 'cards' | 'table';
type AccountStatusFilter = 'ALL' | 'EN_ATTENTE' | 'APPROUVE' | 'REFUSE' | 'SUSPENDU';
type ProfileStatusFilter = 'ALL' | 'NOT_REQUIRED' | 'INCOMPLETE' | 'PENDING_VALIDATION' | 'VALIDATED' | 'REJECTED';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css']
})
export class UsersComponent implements OnInit {
  users: AdminUser[] = [];
  pendingReviews: AdminUser[] = [];
  loading = false;
  error = '';
  success = '';
  activeTab: AdminTab = 'all';
  viewMode: AdminViewMode = 'cards';
  searchTerm = '';
  roleFilter = 'ALL';
  accountStatusFilter: AccountStatusFilter = 'ALL';
  profileStatusFilter: ProfileStatusFilter = 'ALL';
  editingUserId: number | null = null;
  editDraft: AdminUser | null = null;

  constructor(private adminUserService: AdminUserService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.error = '';

    forkJoin({
      users: this.adminUserService.getAllUsers(),
      pendingReviews: this.adminUserService.getPendingProfileReviews()
    }).subscribe({
      next: ({ users, pendingReviews }) => {
        this.users = users ?? [];
        this.pendingReviews = pendingReviews ?? [];
        this.loading = false;
      },
      error: (err) => {
        console.error('loadUsers error', err);
        this.error = 'Failed to load user management data.';
        this.loading = false;
      }
    });
  }

  get filteredUsers(): AdminUser[] {
    const term = this.searchTerm.trim().toLowerCase();

    return this.users.filter((user) => {
      const fullName = `${user.nom ?? ''} ${user.prenom ?? ''}`.trim().toLowerCase();
      const reverseFullName = `${user.prenom ?? ''} ${user.nom ?? ''}`.trim().toLowerCase();
      const normalizedSearch = term.replace(/\s+/g, ' ').trim();

      const matchesTerm = !term || [
        user.nom,
        user.prenom,
        user.email,
        user.role,
        user.region,
        fullName,
        reverseFullName
      ].some((value) => (value ?? '').toLowerCase().includes(normalizedSearch));

      const matchesRole = this.roleFilter === 'ALL' || user.role === this.roleFilter;
      const matchesAccount = this.accountStatusFilter === 'ALL' || user.statutCompte === this.accountStatusFilter;
      const matchesProfile = this.profileStatusFilter === 'ALL' || user.profileValidationStatus === this.profileStatusFilter;

      return matchesTerm && matchesRole && matchesAccount && matchesProfile;
    });
  }

  get availableRoles(): string[] {
    return Array.from(new Set(this.users.map((user) => user.role).filter((role): role is string => !!role))).sort();
  }

  get reviewQueue(): AdminUser[] {
    return this.pendingReviews.filter((user) => user.profileValidationStatus === 'PENDING_VALIDATION');
  }

  setTab(tab: AdminTab): void {
    this.activeTab = tab;
  }

  setViewMode(viewMode: AdminViewMode): void {
    this.viewMode = viewMode;
  }

  startEdit(user: AdminUser): void {
    this.editingUserId = user.id ?? null;
    this.editDraft = { ...user };
    this.clearMessages();
  }

  cancelEdit(): void {
    this.editingUserId = null;
    this.editDraft = null;
  }

  saveEdit(): void {
    if (!this.editDraft?.id) {
      this.error = 'Missing user identifier.';
      return;
    }

    this.loading = true;
    this.clearMessages();

    this.adminUserService.updateUser(this.editDraft).subscribe({
      next: () => {
        this.success = `User ${this.getFullName(this.editDraft)} updated successfully.`;
        this.cancelEdit();
        this.loadUsers();
      },
      error: (err) => {
        console.error('saveEdit error', err);
        this.loading = false;
        this.error = 'Could not save user changes.';
      }
    });
  }

  updateAccountStatus(user: AdminUser, statut: NonNullable<AdminUser['statutCompte']>): void {
    if (!user.id) {
      this.error = 'Missing user identifier.';
      return;
    }

    this.loading = true;
    this.clearMessages();

    this.adminUserService.updateAccountStatus(user.id, statut).subscribe({
      next: () => {
        this.success = `${this.getFullName(user)} is now ${this.prettyLabel(statut)}.`;
        this.loadUsers();
      },
      error: (err) => {
        console.error('updateAccountStatus error', err);
        this.loading = false;
        this.error = 'Could not update account status.';
      }
    });
  }

  approveReview(user: AdminUser): void {
    if (!user.id) {
      this.error = 'Missing user identifier.';
      return;
    }

    this.loading = true;
    this.clearMessages();

    this.adminUserService.reviewProfile(user.id, true).subscribe({
      next: () => {
        this.success = `${this.getFullName(user)} has been approved.`;
        this.loadUsers();
      },
      error: (err) => {
        console.error('approveReview error', err);
        this.loading = false;
        this.error = 'Could not approve this profile review.';
      }
    });
  }

  rejectReview(user: AdminUser): void {
    if (!user.id) {
      this.error = 'Missing user identifier.';
      return;
    }

    const motifRefus = window.prompt('Reason for rejection (optional):', user.motifRefus ?? '');
    if (motifRefus === null) {
      return;
    }

    this.loading = true;
    this.clearMessages();

    this.adminUserService.reviewProfile(user.id, false, motifRefus.trim() || undefined).subscribe({
      next: () => {
        this.success = `${this.getFullName(user)} has been rejected.`;
        this.loadUsers();
      },
      error: (err) => {
        console.error('rejectReview error', err);
        this.loading = false;
        this.error = 'Could not reject this profile review.';
      }
    });
  }

  deleteUser(user: AdminUser): void {
    if (!user.id) {
      this.error = 'Missing user identifier.';
      return;
    }

    const confirmed = window.confirm(`Delete ${this.getFullName(user)} permanently?`);
    if (!confirmed) {
      return;
    }

    this.loading = true;
    this.clearMessages();

    this.adminUserService.deleteUser(user.id).subscribe({
      next: () => {
        this.success = `${this.getFullName(user)} deleted successfully.`;
        this.loadUsers();
      },
      error: (err) => {
        console.error('deleteUser error', err);
        this.loading = false;
        this.error = 'Could not delete this user.';
      }
    });
  }

  getFullName(user: AdminUser | null): string {
    if (!user) {
      return 'Unknown user';
    }

    const fullName = `${user.prenom ?? ''} ${user.nom ?? ''}`.trim();
    return fullName || user.email || `User #${user.id ?? ''}`;
  }

  prettyLabel(value: string | null | undefined): string {
    return (value ?? 'UNKNOWN').replace(/_/g, ' ');
  }

  getUserDocuments(user: AdminUser): string[] {
    return [
      user.diplomeExpert,
      user.certificatTravail,
      user.cin,
      user.logo_organisation
    ]
      .filter((value): value is string => !!value)
      .map((value) => this.normalizeDocumentUrl(value));
  }

  getDocumentLabel(documentUrl: string): string {
    const fileName = documentUrl.split('/').pop() ?? 'Document';
    return decodeURIComponent(fileName);
  }

  getPhotoUrl(photoUrl: string | null | undefined): string | null {
    if (!photoUrl) {
      return null;
    }

    return this.normalizeDocumentUrl(photoUrl);
  }

  getDocumentCount(user: AdminUser): number {
    return this.getUserDocuments(user).length;
  }

  trackByUserId(_: number, user: AdminUser): number | undefined {
    return user.id;
  }

  canApprove(user: AdminUser): boolean {
    return user.statutCompte !== 'APPROUVE';
  }

  canSuspend(user: AdminUser): boolean {
    return user.statutCompte === 'APPROUVE';
  }

  canRefuse(user: AdminUser): boolean {
    return user.statutCompte === 'EN_ATTENTE';
  }

  private normalizeDocumentUrl(documentUrl: string): string {
    if (documentUrl.startsWith('/user/uploads/')) {
      return documentUrl;
    }

    if (documentUrl.startsWith('/uploads/')) {
      return `/user${documentUrl}`;
    }

    const uploadsIndex = documentUrl.indexOf('/uploads/');
    if (uploadsIndex >= 0) {
      return `/user/uploads/${documentUrl.substring(uploadsIndex + '/uploads/'.length)}`;
    }

    if (/^https?:\/\//i.test(documentUrl)) {
      return documentUrl;
    }

    return `/user/uploads/${documentUrl}`;
  }

  private clearMessages(): void {
    this.error = '';
    this.success = '';
  }
}
