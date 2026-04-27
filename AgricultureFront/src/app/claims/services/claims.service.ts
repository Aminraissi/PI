import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ReclamationResponse,
  CreateReclamationRequest,
  AddMessageRequest,
  UpdateStatusRequest,
  ReclamationStatus
} from '../models/claims.models';

export interface CorrectDescriptionRequest {
  subject: string;
   category: string | null;
  description: string;
}

export interface CorrectDescriptionResponse {
  description: string;
}

@Injectable({ providedIn: 'root' })
export class ClaimsService {

  private readonly BASE = 'http://localhost:8095/reclamations/api/reclamations';

  constructor(private http: HttpClient) {}

  getAll(status?: ReclamationStatus): Observable<ReclamationResponse[]> {
    const options = status
      ? { params: new HttpParams().set('status', status) }
      : {};

    return this.http.get<ReclamationResponse[]>(this.BASE, options);
  }

  getByUser(userId: number): Observable<ReclamationResponse[]> {
    return this.http.get<ReclamationResponse[]>(`${this.BASE}/user/${userId}`);
  }

  getById(id: number): Observable<ReclamationResponse> {
    return this.http.get<ReclamationResponse>(`${this.BASE}/${id}`);
  }

  create(request: CreateReclamationRequest): Observable<ReclamationResponse> {
    return this.http.post<ReclamationResponse>(this.BASE, request);
  }

  createWithAttachment(request: CreateReclamationRequest, attachment: File): Observable<ReclamationResponse> {
    const formData = new FormData();
    const data = new Blob([JSON.stringify(request)], { type: 'application/json' });
    formData.append('data', data);
    formData.append('attachment', attachment);

    return this.http.post<ReclamationResponse>(this.BASE, formData);
  }

  addMessage(id: number, request: AddMessageRequest): Observable<ReclamationResponse> {
    return this.http.post<ReclamationResponse>(`${this.BASE}/${id}/messages`, request);
  }

  updateStatus(id: number, request: UpdateStatusRequest): Observable<ReclamationResponse> {
    return this.http.patch<ReclamationResponse>(`${this.BASE}/${id}/status`, request);
  }

  correctDescription(request: CorrectDescriptionRequest): Observable<CorrectDescriptionResponse> {
    return this.http.post<CorrectDescriptionResponse>(`${this.BASE}/ai/correct-description`, request);
  }
}
