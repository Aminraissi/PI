import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AdminUser {
  id?: number;
  nom?: string;
  prenom?: string;
  username?: string;
  photo?: string;
  email?: string;
  motDePasse?: string;
  telephone?: string;
  dateCreation?: string;
  statutCompte?: 'EN_ATTENTE' | 'APPROUVE' | 'REFUSE' | 'SUSPENDU';
  emailVerificationStatus?: 'PENDING' | 'VERIFIED';
  profileValidationStatus?: 'NOT_REQUIRED' | 'INCOMPLETE' | 'PENDING_VALIDATION' | 'VALIDATED' | 'REJECTED';
  motifRefus?: string;
  role?: string;
  isOnline?: boolean;
  lastSeen?: string;
  region?: string;
  diplomeExpert?: string;
  typeVehicule?: string;
  capaciteKg?: number;
  numeroPlaque?: string;
  agence?: string;
  certificatTravail?: string;
  adresseCabinet?: string;
  presentationCarriere?: string;
  telephoneCabinet?: string;
  nom_organisation?: string;
  logo_organisation?: string;
  cin?: string;
  description?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminUserService {
  private readonly baseUrl = 'http://localhost:8089/user/api/user';

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.baseUrl}/getAll`);
  }

  getPendingProfileReviews(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.baseUrl}/profile-review/pending`);
  }

  getUserById(userId: number): Observable<AdminUser> {
    return this.http.get<AdminUser>(`${this.baseUrl}/getUser/${userId}`);
  }

  updateUser(user: AdminUser): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.baseUrl}/updateaUser`, user);
  }

  updateAccountStatus(userId: number, statut: AdminUser['statutCompte']): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.baseUrl}/updateStatut/${userId}?statut=${statut}`, {});
  }

  reviewProfile(userId: number, approved: boolean, motifRefus?: string): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.baseUrl}/reviewProfile/${userId}`, {
      approved,
      motifRefus: motifRefus ?? null
    });
  }

  deleteUser(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/del/${userId}`);
  }
}
