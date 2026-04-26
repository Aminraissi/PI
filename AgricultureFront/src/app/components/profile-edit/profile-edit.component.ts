import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import {
  AuthService,
  BackendRole,
  UserProfile,
  UserProfileUpdateRequest
} from '../../services/auth/auth.service';

@Component({
  selector: 'app-profile-edit',
  standalone: false,
  templateUrl: './profile-edit.component.html',
  styleUrls: ['./profile-edit.component.css']
})
export class ProfileEditComponent implements OnInit {
  profileForm!: FormGroup;
  role: BackendRole | null = null;
  userId: number | null = null;
  photoPreview: string | null = null;
  logoPreview: string | null = null;
  selectedPhotoFile: File | null = null;
  isLoading = true;
  isSaving = false;
  submitError = '';
  submitSuccess = '';

  readonly roleLabels: Record<string, string> = {
    AGRICULTEUR: 'Agriculteur',
    VETERINAIRE: 'Veterinaire',
    EXPERT_AGRICOLE: 'Expert agricole',
    AGENT: 'Agent',
    ORGANISATEUR_EVENEMENT: 'Organisateur',
    TRANSPORTEUR: 'Transporteur',
    ACHETEUR: 'Acheteur'
  };

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser || currentUser.role === 'ADMIN') {
      this.router.navigate(['/']);
      return;
    }

    this.role = currentUser.role;
    this.userId = currentUser.userId;
    this.createForm();
    this.loadProfile();
  }

  hasRole(...roles: BackendRole[]): boolean {
    return !!this.role && roles.includes(this.role);
  }

  invalid(field: string): boolean {
    const control = this.profileForm.get(field);
    return !!(control && control.invalid && control.touched);
  }

  onImageChange(event: Event, field: 'photo' | 'logoOrganisation'): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      this.submitError = 'Veuillez choisir une image valide.';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const value = reader.result as string;
      this.profileForm.get(field)?.markAsDirty();
      if (field === 'photo') {
        this.selectedPhotoFile = file;
        this.photoPreview = value;
      } else {
        this.profileForm.get(field)?.setValue(value);
        this.logoPreview = value;
      }
    };
    reader.readAsDataURL(file);
  }

  submit(): void {
    if (this.profileForm.invalid || this.userId == null) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    this.submitError = '';
    this.submitSuccess = '';

    const saveProfile = () => {
      const payload = this.buildPayload();
      this.authService.updateProfile(this.userId as number, payload).subscribe({
      next: (profile) => {
        this.isSaving = false;
        this.submitSuccess = 'Profil mis a jour avec succes.';
        this.selectedPhotoFile = null;
        this.patchPreviews(profile);
      },
      error: (err) => {
        this.isSaving = false;
        this.submitError = this.extractError(err, 'Impossible de mettre a jour le profil.');
      }
      });
    };

    if (this.selectedPhotoFile) {
      this.authService.uploadProfilePhoto(this.userId, this.selectedPhotoFile).subscribe({
        next: () => saveProfile(),
        error: (err) => {
          this.isSaving = false;
          this.submitError = this.extractError(err, 'Impossible d uploader la photo.');
        }
      });
      return;
    }

    saveProfile();
  }

  private createForm(): void {
    this.profileForm = this.fb.group({
      nom: ['', Validators.required],
      prenom: ['', Validators.required],
      photo: [null],
      email: ['', [Validators.required, Validators.email]],
      motDePasse: [''],
      telephone: ['', Validators.required],
      region: ['', Validators.required],
      cin: ['', Validators.required],
      adresseCabinet: [''],
      presentationCarriere: [''],
      telephoneCabinet: [''],
      agence: [''],
      certificatTravail: [''],
      nomOrganisation: [''],
      logoOrganisation: [null],
      description: ['']
    });

    if (this.hasRole('VETERINAIRE')) {
      this.profileForm.get('adresseCabinet')?.addValidators(Validators.required);
      this.profileForm.get('presentationCarriere')?.addValidators(Validators.required);
      this.profileForm.get('telephoneCabinet')?.addValidators(Validators.required);
    }

    if (this.hasRole('AGENT')) {
      this.profileForm.get('agence')?.addValidators(Validators.required);
      this.profileForm.get('certificatTravail')?.addValidators(Validators.required);
    }

    if (this.hasRole('ORGANISATEUR_EVENEMENT')) {
      this.profileForm.get('nomOrganisation')?.addValidators(Validators.required);
      this.profileForm.get('description')?.addValidators(Validators.required);
    }

    Object.values(this.profileForm.controls).forEach(control => control.updateValueAndValidity());
  }

  private loadProfile(): void {
    if (this.userId == null) return;

    this.authService.getProfile(this.userId).subscribe({
      next: (profile) => {
        this.isLoading = false;
        this.profileForm.patchValue({
          nom: profile.nom || '',
          prenom: profile.prenom || '',
          photo: profile.photo || null,
          email: profile.email || '',
          motDePasse: '',
          telephone: profile.telephone || '',
          region: profile.region || '',
          cin: profile.cin || '',
          adresseCabinet: profile.adresseCabinet || '',
          presentationCarriere: profile.presentationCarriere || '',
          telephoneCabinet: profile.telephoneCabinet || '',
          agence: profile.agence || '',
          certificatTravail: profile.certificatTravail || '',
          nomOrganisation: profile.nom_organisation || '',
          logoOrganisation: profile.logo_organisation || null,
          description: profile.description || ''
        });
        this.patchPreviews(profile);
      },
      error: () => {
        this.isLoading = false;
        this.submitError = 'Impossible de charger votre profil.';
      }
    });
  }

  private patchPreviews(profile: UserProfile): void {
    this.photoPreview = this.buildImageUrl(profile.photo || this.profileForm.get('photo')?.value || null);
    this.logoPreview = profile.logo_organisation || this.profileForm.get('logoOrganisation')?.value || null;
  }

  private buildImageUrl(value: string | null): string | null {
    if (!value) {
      return null;
    }
    if (value.startsWith('data:') || value.startsWith('http://') || value.startsWith('https://')) {
      return value;
    }
    if (value.startsWith('/')) {
      return value;
    }
    return `/${value}`;
  }

  private buildPayload(): UserProfileUpdateRequest {
    const value = this.profileForm.value;
    const payload: UserProfileUpdateRequest = {
      nom: value.nom,
      prenom: value.prenom,
      email: value.email,
      telephone: value.telephone,
      region: value.region,
      cin: String(value.cin)
    };

    if (value.motDePasse?.trim()) {
      payload.motDePasse = value.motDePasse.trim();
    }

    if (this.hasRole('VETERINAIRE')) {
      payload.adresseCabinet = value.adresseCabinet;
      payload.presentationCarriere = value.presentationCarriere;
      payload.telephoneCabinet = value.telephoneCabinet;
    }

    if (this.hasRole('AGENT')) {
      payload.agence = value.agence;
      payload.certificatTravail = value.certificatTravail;
    }

    if (this.hasRole('ORGANISATEUR_EVENEMENT')) {
      payload.nomOrganisation = value.nomOrganisation;
      payload.logoOrganisation = value.logoOrganisation;
      payload.description = value.description;
    }

    return payload;
  }

  private extractError(err: any, fallback: string): string {
    if (typeof err?.error === 'string') {
      return err.error;
    }
    if (typeof err?.error?.message === 'string') {
      return err.error.message;
    }
    if (typeof err?.message === 'string') {
      return err.message;
    }
    return fallback;
  }
}
