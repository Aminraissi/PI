import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ContratService } from '../../../services/loans/contrat.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-contrat-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './contrat-admin.component.html',
  styleUrls: ['./contrat-admin.component.css']
})
export class ContratAdminComponent implements OnInit {

  pendingContracts: any[] = [];
  validatedContracts: any[] = [];
  rejectedContracts: any[] = [];
  selectedTab: string = 'pending';
  isLoading = false;
  selectedContract: any = null;
  showDetailsModal = false;
  rejectionReason: string = '';
  pdfUrl: SafeResourceUrl | null = null;

  constructor(
    private contratService: ContratService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.loadPendingContracts();
  }

  loadPendingContracts() {
    this.isLoading = true;
    this.contratService.getPendingContracts().subscribe({
      next: (data: any[]) => {
        this.pendingContracts = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading pending contracts:', err);
        this.isLoading = false;
        alert('Error loading contracts');
      }
    });
  }

  loadValidatedContracts() {
    this.isLoading = true;
    this.contratService.getValidatedContracts().subscribe({
      next: (data: any[]) => {
        this.validatedContracts = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading validated contracts:', err);
        this.isLoading = false;
      }
    });
  }

  loadRejectedContracts() {
    this.isLoading = true;
    this.contratService.getRejectedContracts().subscribe({
      next: (data: any[]) => {
        this.rejectedContracts = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading rejected contracts:', err);
        this.isLoading = false;
      }
    });
  }

  onTabChange(tab: string) {
    this.selectedTab = tab;
    if (tab === 'pending') {
      this.loadPendingContracts();
    } else if (tab === 'validated') {
      this.loadValidatedContracts();
    } else if (tab === 'rejected') {
      this.loadRejectedContracts();
    }
  }

  viewContractDetails(contract: any) {
    this.selectedContract = contract;
    this.showDetailsModal = true;
    this.rejectionReason = '';
    this.pdfUrl = null;
    
   
    if (contract.contenuContrat && (contract.contenuContrat.endsWith('.pdf') || contract.contenuContrat.includes('.pdf'))) {
      this.loadPDF(contract.id);
    }
  }

  loadPDF(contratId: number) {
    this.contratService.downloadPDF(contratId).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
      },
      error: (err) => {
        console.error('Error loading PDF:', err);
      }
    });
  }

  closeModal() {
    this.showDetailsModal = false;
    this.selectedContract = null;
    if (this.pdfUrl) {
      URL.revokeObjectURL(this.pdfUrl.toString());
    }
  }

  validateContract(contractId: number) {
    if (confirm('Are you sure you want to VALIDATE this contract?')) {
      this.isLoading = true;
      this.contratService.validateContract(contractId, true, '').subscribe({
        next: () => {
          alert(' Contract validated successfully!');
          this.loadPendingContracts();
          this.closeModal();
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Error validating contract:', err);
          alert(' Error validating contract');
          this.isLoading = false;
        }
      });
    }
  }

  rejectContract(contractId: number) {
    const reason = prompt('Please enter the rejection reason:');
    if (reason && reason.trim()) {
      this.isLoading = true;
      this.contratService.validateContract(contractId, false, reason).subscribe({
        next: () => {
          alert(' Contract rejected!');
          this.loadPendingContracts();
          this.closeModal();
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Error rejecting contract:', err);
          alert('❌ Error rejecting contract');
          this.isLoading = false;
        }
      });
    } else if (reason === '') {
      alert('Please enter a reason for rejection');
    }
  }

  getStatusBadgeClass(status: string): string {
    switch(status) {
      case 'SIGNE': return 'badge-signed';
      case 'SIGNE': return 'badge-signed';
      default: return 'badge-pending';
    }
  }

  getStatusLabel(status: string): string {
    switch(status) {
      case 'SIGNE': return 'Signed';
      default: return 'Pending';
    }
  }

  
  getValidationStatus(validationAdmin: string): string {
    if (validationAdmin === 'APPROVED') return 'Approved';
    if (validationAdmin === 'REJECTED') return 'Rejected';
    if (validationAdmin === 'PENDING') return 'Pending';
    if (validationAdmin === 'oui') return 'Approved';
    if (validationAdmin === 'non') return 'Pending';
    return validationAdmin || 'Pending';
  }

  getValidationClass(validationAdmin: string): string {
    if (validationAdmin === 'APPROVED' || validationAdmin === 'oui') return 'approved';
    if (validationAdmin === 'REJECTED') return 'rejected';
    return 'pending';
  }

 
  isPendingValidation(contract: any): boolean {
    return contract.validationAdmin === 'PENDING' || contract.validationAdmin === 'non';
  }

 
  isApproved(contract: any): boolean {
    return contract.validationAdmin === 'APPROVED' || contract.validationAdmin === 'oui';
  }

  
  isRejected(contract: any): boolean {
    return contract.validationAdmin === 'REJECTED';
  }

  calculateMensualite(montant: number, taux: number, duree: number): number {
    if (!montant || !taux || !duree) return 0;
    const interest = (montant * taux / 100);
    const total = montant + interest;
    return total / duree;
  }
}