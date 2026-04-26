// contrat.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ContratService {

  private apiUrl = 'http://localhost:8089/pret/api/contrat';

  constructor(private http: HttpClient) {}

  generateContrat(demandeId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/generate/${demandeId}`, {});
  }

  getContratByDemande(demandeId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/by-demande/${demandeId}`);
  }

 signContrat(contratId: number, signatureBase64: string): Observable<any> {
  return this.http.post(`${this.apiUrl}/sign`, {
    contratId: Number(contratId),
    signatureBase64: signatureBase64
  });
}
  getContrat(id: number): Observable<any> {
  return this.http.get(`${this.apiUrl}/getContrat/${id}`);
}
 signContratWithPDF(contratId: number, signatureBase64: string, pdfFile: Blob): Observable<any> {
    const formData = new FormData();
    formData.append('contratId', contratId.toString());
    formData.append('signatureBase64', signatureBase64);
    formData.append('pdfFile', pdfFile, `contrat_${contratId}.pdf`);
    
    return this.http.post(`${this.apiUrl}/sign-with-pdf`, formData);
  }

  getPendingContracts(): Observable<any[]> {
  return this.http.get<any[]>(`${this.apiUrl}/pending-validation`);
}

getValidatedContracts(): Observable<any[]> {
  return this.http.get<any[]>(`${this.apiUrl}/validated-contracts`);
}

getRejectedContracts(): Observable<any[]> {
  return this.http.get<any[]>(`${this.apiUrl}/rejected-contracts`);
}

validateContract(contratId: number, valide: boolean, raison: string): Observable<any> {
  return this.http.post(`${this.apiUrl}/validate/${contratId}`, { valide, raison });
}

downloadPDF(contratId: number): Observable<Blob> {
  return this.http.get(`${this.apiUrl}/download-pdf/${contratId}`, {
    responseType: 'blob'
  });
}
}