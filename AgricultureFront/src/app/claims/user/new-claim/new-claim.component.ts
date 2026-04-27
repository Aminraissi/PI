import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ClaimsService } from '../../services/claims.service';
import { AuthService } from '../../../services/auth/auth.service';
import { ReclamationCategory, ReclamationPriority, CATEGORY_LABELS, PRIORITY_LABELS } from '../../models/claims.models';

@Component({
  selector: 'app-new-claim',
  standalone: false,
  templateUrl: './new-claim.component.html',
  styleUrls: ['./new-claim.component.css']
})
export class NewClaimComponent implements OnInit {

  form!: FormGroup;
  submitting = false;
  submitError: string | null = null;
  submitSuccess = false;
  selectedAttachment: File | null = null;
  attachmentError: string | null = null;
  correctingDescription = false;
  correctionError: string | null = null;

  categories: { value: ReclamationCategory; label: string }[] = [
    { value: 'COMMANDE',     label: CATEGORY_LABELS.COMMANDE },
    { value: 'LIVRAISON',    label: CATEGORY_LABELS.LIVRAISON },
    { value: 'PAIEMENT',     label: CATEGORY_LABELS.PAIEMENT },
    { value: 'COMPTE',       label: CATEGORY_LABELS.COMPTE },
    { value: 'RENDEZ_VOUS',  label: CATEGORY_LABELS.RENDEZ_VOUS },
    { value: 'INVENTAIRE',   label: CATEGORY_LABELS.INVENTAIRE },
    { value: 'AUTRE',        label: CATEGORY_LABELS.AUTRE }
  ];

  priorities: { value: ReclamationPriority; label: string; icon: string }[] = [
    { value: 'BASSE',   label: PRIORITY_LABELS.BASSE,   icon: 'fas fa-arrow-down' },
    { value: 'MOYENNE', label: PRIORITY_LABELS.MOYENNE, icon: 'fas fa-minus' },
    { value: 'HAUTE',   label: PRIORITY_LABELS.HAUTE,   icon: 'fas fa-arrow-up' }
  ];

  constructor(
    private fb: FormBuilder,
    private claimsService: ClaimsService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      subject:     ['', [Validators.required, Validators.minLength(5), Validators.maxLength(100)]],
      category:    ['', Validators.required],
      description: ['', [Validators.required, Validators.minLength(20)]],
      priority:    ['MOYENNE', Validators.required]
    });
  }

  setPriority(p: ReclamationPriority): void {
    this.form.patchValue({ priority: p });
  }

  onAttachmentChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.attachmentError = null;
    this.selectedAttachment = null;

    if (!input.files?.length) return;

    const file = input.files[0];
    const maxSize = 10 * 1024 * 1024;
    if (file.size > maxSize) {
      this.attachmentError = 'File is too large. Maximum size is 10 MB.';
      input.value = '';
      return;
    }

    this.selectedAttachment = file;
  }

  removeAttachment(): void {
    this.selectedAttachment = null;
    this.attachmentError = null;
  }

  improveDescription(): void {
    const description = (this.form.get('description')?.value || '').trim();
    if (description.length < 20) {
      this.correctionError = 'Write at least 20 characters before using AI correction.';
      this.form.get('description')?.markAsTouched();
      return;
    }

    this.correctingDescription = true;
    this.correctionError = null;

    this.claimsService.correctDescription({
      subject: this.form.get('subject')?.value || '',
  category: this.form.get('category')?.value || '',
      description
    }).subscribe({
      next: (response) => {
        this.correctingDescription = false;
        this.form.patchValue({ description: response.description });
      },
      error: (error) => {
        this.correctingDescription = false;
        this.correctionError = this.extractErrorMessage(error, 'AI correction is unavailable for the moment.');
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const userId = this.authService.getCurrentUserId();
    if (!userId) { this.router.navigate(['/claims/auth']); return; }

    this.submitting = true;
    this.submitError = null;

    const request = { userId, ...this.form.value };
    const create$ = this.selectedAttachment
      ? this.claimsService.createWithAttachment(request, this.selectedAttachment)
      : this.claimsService.create(request);

    create$.subscribe({
      next: (rec) => {
        this.submitting = false;
        this.submitSuccess = true;
        setTimeout(() => this.router.navigate(['/claims/detail', rec.id]), 1500);
      },
      error: (error) => {
        this.submitting = false;
        this.submitError = this.extractErrorMessage(error, 'An error occurred. Please try again.');
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/claims/my-claims']);
  }

  fieldInvalid(name: string): boolean {
    const c = this.form.get(name);
    return !!(c && c.invalid && c.touched);
  }

  private extractErrorMessage(error: any, fallback: string): string {
    if (typeof error?.error === 'string' && error.error.trim()) {
      return error.error;
    }

    if (typeof error?.error?.message === 'string' && error.error.message.trim()) {
      return error.error.message;
    }

    if (typeof error?.message === 'string' && error.message.trim()) {
      return error.message;
    }

    return fallback;
  }
}
