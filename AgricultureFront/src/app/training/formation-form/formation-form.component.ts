import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormationService, Formation } from '../../services/formation/formation.service';
import { AuthService } from '../../services/auth/auth.service';
import { SharedModule } from '../../shared/shared.module';

@Component({
  selector: 'app-formation-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, SharedModule],
  templateUrl: './formation-form.component.html',
  styleUrl: './formation-form.component.css'
})
export class FormationFormComponent implements OnInit {
  private readonly maxOriginalImageBytes = 8 * 1024 * 1024;
  private readonly maxUploadImageBytes = 1200 * 1024;
  private readonly maxImageDimension = 1400;

  formation: Formation = {
    titre: '',
    description: '',
    thematique: '',
    niveau: 'DEBUTANT',
    type: 'PRESENTIEL',
    prix: 0,
    estPayante: false,
    langue: 'FR',
    imageUrl: '',
    statut: 'BROUILLON'
  };

  isLoading = false;
  isSubmitting = false;
  error: string | null = null;
  success: string | null = null;
  isEditMode = false;
  currentUserId: number | null = null;
  isExpertAgricole = false;
  isAccountApproved = false;

  selectedImageFile: File | null = null;
  imagePreview: string | null = null;

  niveaux = ['DEBUTANT', 'INTERMEDIAIRE', 'AVANCE'];
  types = ['PRESENTIEL', 'EN_LIGNE', 'HYBRIDE'];
  langues = ['FR', 'EN', 'ES', 'DE', 'IT'];
  statuts = ['BROUILLON', 'PUBLIEE', 'ARCHIVEE'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private formationService: FormationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.checkUserPermissions();
    this.loadFormation();
  }

  checkUserPermissions(): void {
    const user = this.authService.getCurrentUser();
    this.currentUserId = user?.userId ?? null;
    this.isExpertAgricole = this.authService.hasRole('EXPERT_AGRICOLE');
    this.isAccountApproved = this.authService.isAccountApproved();

    if (!this.currentUserId) {
      this.router.navigate(['/auth']);
      return;
    }

    if (!this.isExpertAgricole || !this.isAccountApproved) {
      this.router.navigate(['/training']);
    }
  }

  loadFormation(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isLoading = true;
      this.isEditMode = true;
      this.formationService.getFormationById(Number(id)).subscribe({
        next: (data) => {
          this.formation = data;

          if (this.formation.imageUrl) {
            this.imagePreview = this.formation.imageUrl;
          }

          this.isLoading = false;
        },
        error: (err) => {
          console.error('Error loading formation:', err);
          this.error = 'Unable to load the training.';
          this.isLoading = false;
        }
      });
    }
  }

  onSubmit(): void {
    if (!this.validateForm()) {
      return;
    }

    this.isSubmitting = true;
    this.error = null;
    this.success = null;

    if (this.selectedImageFile) {
      this.uploadImageAndSubmit();
    } else {
      this.submitFormation();
    }
  }

  private uploadImageAndSubmit(): void {
    if (!this.selectedImageFile) {
      this.submitFormation();
      return;
    }

    this.formationService.uploadImage(this.selectedImageFile).subscribe({
      next: (response) => {
        this.formation.imageUrl = response.imageUrl;
        this.imagePreview = response.imageUrl;
        this.selectedImageFile = null;
        this.submitFormation();
      },
      error: (err) => {
        console.error('Error uploading image:', err);
        this.error = 'An error occurred while uploading the image.';
        this.isSubmitting = false;
      }
    });
  }

  private submitFormation(): void {
    if (this.isEditMode && this.formation.idFormation) {
      this.formationService.updateFormation(this.formation.idFormation, this.formation).subscribe({
        next: () => {
          this.success = 'Training updated successfully.';
          setTimeout(() => this.router.navigate(['/training', this.formation.idFormation]), 1500);
        },
        error: (err) => {
          console.error('Error updating formation:', err);
          this.error = 'An error occurred while updating the training.';
          this.isSubmitting = false;
        }
      });
    } else {
      const formationData = { ...this.formation };
      formationData.userId = this.currentUserId ?? undefined;
      delete formationData.idFormation;

      this.formationService.createFormation(formationData).subscribe({
        next: (data) => {
          this.success = 'Training created successfully.';
          setTimeout(() => this.router.navigate(['/training', data.idFormation]), 1500);
        },
        error: (err) => {
          console.error('Error creating formation:', err);
          this.error = 'An error occurred while creating the training.';
          this.isSubmitting = false;
        }
      });
    }
  }

  validateForm(): boolean {
    if (!this.formation.titre?.trim()) {
      this.error = 'Title is required.';
      return false;
    }
    if (!this.formation.description?.trim()) {
      this.error = 'Description is required.';
      return false;
    }
    if (!this.formation.thematique?.trim()) {
      this.error = 'Topic is required.';
      return false;
    }
    if (this.formation.estPayante && !this.formation.prix) {
      this.error = 'Price is required for a paid training.';
      return false;
    }
    return true;
  }

  async onFileSelected(event: any): Promise<void> {
    const file = event.target.files[0];
    if (file) {
      if (!file.type.startsWith('image/')) {
        this.error = 'Please select a valid image file.';
        event.target.value = '';
        return;
      }

      if (file.size > this.maxOriginalImageBytes) {
        this.error = 'The image size must not exceed 5MB.';
        return;
      }

      const optimizedFile = await this.optimizeImageForUpload(file).catch((err) => {
        console.error('Error optimizing image:', err);
        this.error = 'Unable to prepare this image.';
        event.target.value = '';
        return null;
      });
      if (!optimizedFile) {
        return;
      }
      if (optimizedFile.size > this.maxUploadImageBytes) {
        this.error = 'The image is still too large after optimization. Choose a smaller image.';
        event.target.value = '';
        return;
      }

      this.selectedImageFile = optimizedFile;
      this.error = null;

      const reader = new FileReader();
      reader.onload = (e) => {
        this.imagePreview = e.target?.result as string;
        this.formation.imageUrl = this.imagePreview;
      };
      reader.readAsDataURL(optimizedFile);
    }
  }

  private optimizeImageForUpload(file: File): Promise<File> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      const objectUrl = URL.createObjectURL(file);

      img.onload = () => {
        URL.revokeObjectURL(objectUrl);

        const scale = Math.min(1, this.maxImageDimension / Math.max(img.width, img.height));
        const width = Math.max(1, Math.round(img.width * scale));
        const height = Math.max(1, Math.round(img.height * scale));
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        if (!ctx) {
          reject(new Error('Canvas not available'));
          return;
        }

        ctx.drawImage(img, 0, 0, width, height);

        const createBlob = (quality: number) => {
          canvas.toBlob(
            (blob) => {
              if (!blob) {
                reject(new Error('Image compression failed'));
                return;
              }

              if (blob.size > this.maxUploadImageBytes && quality > 0.52) {
                createBlob(quality - 0.12);
                return;
              }

              const optimizedName = file.name.replace(/\.[^.]+$/, '') + '.jpg';
              resolve(new File([blob], optimizedName, { type: 'image/jpeg' }));
            },
            'image/jpeg',
            quality
          );
        };

        createBlob(0.82);
      };

      img.onerror = () => {
        URL.revokeObjectURL(objectUrl);
        reject(new Error('Invalid image'));
      };

      img.src = objectUrl;
    });
  }

  removeImage(): void {
    this.selectedImageFile = null;
    this.imagePreview = null;
    this.formation.imageUrl = '';
  }

  goBack(): void {
    if (this.isEditMode && this.formation.idFormation) {
      this.router.navigate(['/training', this.formation.idFormation]);
    } else {
      this.router.navigate(['/training']);
    }
  }
}
