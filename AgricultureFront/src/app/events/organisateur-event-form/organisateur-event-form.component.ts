import { Component, OnInit } from '@angular/core';
import {FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EventService } from 'src/app/services/event/event.service';
import { AuthService } from 'src/app/services/auth/auth.service'; 
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const dateRangeValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {

  const start = group.get('dateDebut')?.value;
  const end = group.get('dateFin')?.value;

  if (!start || !end) return null;

  const now = new Date();
  const startDate = new Date(start);
  const endDate = new Date(end);

  const errors: any = {};

  now.setSeconds(0, 0);

  if (startDate <= now) {
    errors.startInvalid = true;
  }

  if (endDate <= now) {
    errors.endInvalid = true;
  }

  if (endDate <= startDate) {
    errors.rangeInvalid = true;
  }

  return Object.keys(errors).length ? errors : null;
};

@Component({
  selector: 'app-organisateur-event-form',
  standalone: false,
  templateUrl: './organisateur-event-form.component.html',
  styleUrl: './organisateur-event-form.component.css'
})
export class OrganisateurEventFormComponent implements OnInit {

  organisateurId!: number;
  isEditMode = false;
  eventId: number | null = null;
  existingEvent: any = null;

  eventForm!: FormGroup;

  imageFile: File | null = null;
  imagePreview: string | null = null;
  authFile: File | null = null;
  authFileName: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private eventService: EventService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const id = this.authService.getCurrentUserId();
    if (!id) {
      this.router.navigate(['/auth']);
      return;
    }
    this.organisateurId = id;


    this.eventForm = new FormGroup({
  titre: new FormControl('', [Validators.required, Validators.minLength(3)]),
  type: new FormControl('', Validators.required),
  description: new FormControl('', [Validators.required, Validators.minLength(10)]),

  dateDebut: new FormControl('', Validators.required),
  dateFin: new FormControl('', Validators.required),

  lieu: new FormControl('', Validators.required),
  region: new FormControl('', Validators.required),
  capaciteMax: new FormControl(null, [Validators.required, Validators.min(1)]),
  montant: new FormControl(0, [Validators.required, Validators.min(0)]),
  statut: new FormControl('PLANNED', Validators.required)

}, { validators: dateRangeValidator });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.eventId = +idParam;
      this.loadEvent(this.eventId);
    }
  }

  get f() { return this.eventForm.controls; }

  loadEvent(id: number): void {
    this.eventService.getEventsByOrganisateur(this.organisateurId).subscribe({
      next: events => {
        const ev = events.find(e => e.id === id);
        if (!ev) { this.router.navigate(['organizer/events']); return; }
        this.existingEvent = ev;
        this.imagePreview = ev.image ? this.eventService.getImageUrl(ev.image) : null;
        this.authFileName  = ev.autorisationmunicipale ?? null;
        this.eventForm.patchValue({
          titre: ev.titre,
          type: ev.type,
          description: ev.description,
          dateDebut: this.toDatetimeLocal(ev.dateDebut),
          dateFin: this.toDatetimeLocal(ev.dateFin),
          lieu: ev.lieu,
          region: ev.region,
          capaciteMax: ev.capaciteMax,
          montant: ev.montant,
          statut: ev.statut
        });
      },
      error: err => console.error('Error loading event', err)
    });
  }

  onSubmit(): void {
    if (this.eventForm.invalid) {
      this.eventForm.markAllAsTouched();
      return;
    }
    this.isEditMode ? this.update() : this.create();
  }

  private create(): void {
  const payload = {
    ...this.eventForm.value,
    statut: 'PLANNED',
    inscrits: 0,
    idOrganisateur: this.organisateurId
  };

  this.eventService.addEvent(payload, this.imageFile!, this.authFile!)
    .subscribe({
      next: () => this.router.navigate(['events/organizer/events']),
      error: err => console.error(err)
    });
}

 private update(): void {

  if (!this.eventId) return;

  const payload = {
    ...this.eventForm.value,
    id: this.eventId,
    idOrganisateur: this.organisateurId
  };

  this.eventService.updateEvent(
    payload,
    this.imageFile || undefined,
    this.authFile || undefined
  ).subscribe({
    next: () => {
      this.router.navigate(['/events/organizer/events'], {
        queryParams: { success: 'updated' }
      });
    },
    error: err => {
      console.error('Update failed', err);
    }
  });
}

  onImageChange(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.imageFile = file;
    const reader = new FileReader();
    reader.onload = e => this.imagePreview = e.target?.result as string;
    reader.readAsDataURL(file);
  }

  onAuthChange(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.authFile = file;
    this.authFileName = file.name;
  }

  goBack(): void {
    this.router.navigate(['events/organizer/events']);
  }

  private toDatetimeLocal(dt: string): string {
    if (!dt) return '';
    return new Date(dt).toISOString().slice(0, 16);
  }
}