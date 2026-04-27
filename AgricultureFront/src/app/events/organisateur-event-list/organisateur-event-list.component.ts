import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { Router } from '@angular/router';
import { EventService } from '../../services/event/event.service';
import { AuthService } from 'src/app/services/auth/auth.service';
import { AiEventService } from 'src/app/services/aiEvent/aiEvent.service';
import {
  FormGroup,
  FormControl,
  Validators,
  AbstractControl,
  ValidationErrors
} from '@angular/forms';

interface ChatMessage {
  role: 'user' | 'ai';
  text: string;
}

@Component({
  selector: 'app-organisateur-event-list',
  templateUrl: './organisateur-event-list.component.html',
  styleUrls: ['./organisateur-event-list.component.css']
})
export class OrganisateurEventListComponent implements OnInit, AfterViewChecked {

  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  organisateurId!: number;
  events: any[] = [];

  selectedFile: File | null = null;

  showSuccess = false;
  message = '';

  showDelayModal = false;
  delayEventId: number | null = null;

  delayForm!: FormGroup;

  showCancelConfirm = false;
  cancelEventId: number | null = null;

  selectedFileName = '';

  aiOpen = false;
  userInput = '';
  isLoading = false;

  messages: ChatMessage[] = [
    {
      role: 'ai',
      text: `Hello! I am your AI agricultural assistant 🌿<br><br>
      I can help you choose the <strong>location</strong>, the <strong>date</strong>,
      estimate <strong>attendance</strong>, and analyze <strong>weather</strong> conditions.`
    }
  ];

  constructor(
    private AiEventService: AiEventService,
    public eventService: EventService,
    private router: Router,
    private authService: AuthService
  ) {}

  dateDelayValidator = (group: AbstractControl): ValidationErrors | null => {
    const startValue = group.get('newDateDebut')?.value;
    const endValue = group.get('newDateFin')?.value;

    if (!startValue || !endValue) return null;

    const start = new Date(startValue);
    const end = new Date(endValue);
    const now = new Date();

    const errors: any = {};

    if (start.getTime() <= now.getTime()) {
      errors.startPast = true;
    }

    if (end.getTime() <= now.getTime()) {
      errors.endPast = true;
    }

    if (end.getTime() <= start.getTime()) {
      errors.rangeInvalid = true;
    }

    return Object.keys(errors).length ? errors : null;
  };

  ngOnInit(): void {
    const id = this.authService.getCurrentUserId();
    if (!id) {
      this.router.navigate(['/auth']);
      return;
    }

    this.organisateurId = id;
    this.loadEvents();
  }

  loadEvents(): void {
    this.eventService.getEventsByOrganisateur(this.organisateurId).subscribe({
      next: data => (this.events = data),
      error: (err: any) => console.error(err)
    });
  }

  openDelayModal(id: number): void {
    this.delayEventId = id;

    this.delayForm = new FormGroup({
      reason: new FormControl('', [Validators.required, Validators.minLength(5)]),
      newDateDebut: new FormControl('', Validators.required),
      newDateFin: new FormControl('', Validators.required),
      autorisationMunicipale: new FormControl(null, Validators.required)
    }, {
      validators: this.dateDelayValidator
    });

    this.showDelayModal = true;
  }

  openCancelConfirm(id: number): void {
    this.cancelEventId = id;
    this.showCancelConfirm = true;
  }

  closeDelayModal(): void {
    this.showDelayModal = false;
    this.delayEventId = null;
    this.selectedFile = null;
    this.selectedFileName = '';
    this.delayForm = null as any;
  }

  onFileSelected(event: any) {
    const file: File = event.target.files?.[0];

    if (file && this.delayForm) {
      this.selectedFile = file;
      this.selectedFileName = file.name;
      this.delayForm.get('autorisationMunicipale')?.setValue(file);
      this.delayForm.get('autorisationMunicipale')?.markAsTouched();
    }
  }

  submitDelay(): void {
    if (!this.delayForm || this.delayForm.invalid || !this.delayEventId) {
        this.delayForm?.markAllAsTouched();
        return;
    }

    const formData = new FormData();
    
    const startDate = new Date(this.delayForm.value.newDateDebut);
    const endDate = new Date(this.delayForm.value.newDateFin);
    
    const formatDate = (date: Date): string => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
    };
    
    const delayData = {
        reason: this.delayForm.value.reason,
        newDateDebut: formatDate(startDate),
        newDateFin: formatDate(endDate)
    };
    
  
    formData.append('data', JSON.stringify(delayData));
    
    if (this.selectedFile) {
        formData.append('file', this.selectedFile);
    }

    this.eventService.delayEvent(this.delayEventId, formData).subscribe({
        next: (response: any) => {
            console.log("Response from server:", response);
            const ev = this.events.find(e => e.id === this.delayEventId);
            if (ev) {
                ev.dateDebut = this.delayForm.value.newDateDebut;
                ev.dateFin = this.delayForm.value.newDateFin;
                ev.statut = 'POSTPONED';
            }
            this.closeDelayModal();
            this.triggerSuccess('updated');
            this.loadEvents();
        },
        error: (err: any) => {
            console.error('Erreur lors du report:', err);
            alert('Erreur lors du report de l\'événement: ' + (err.error?.message || err.message));
        }
    });
}

  goToAdd(): void {
    this.router.navigate(['events/organizer/events/add']);
  }

  goToEdit(ev: any): void {
    this.router.navigate(['events/organizer/events/edit', ev.id]);
  }

  onDelete(id: number): void {
    if (!confirm('Delete this event?')) return;

    this.eventService.deleteEvent(id).subscribe({
      next: () => {
        this.events = this.events.filter(e => e.id !== id);
        this.triggerSuccess('deleted');
      },
      error: (err: any) => console.error(err)
    });
  }

  onCancelEvent(): void {
    if (!this.cancelEventId) return;

    this.eventService.cancelEvent(this.cancelEventId).subscribe({
      next: () => {
        const ev = this.events.find(e => e.id === this.cancelEventId);
        if (ev) {
          ev.statut = 'CANCELLED';
        }
        this.showCancelConfirm = false;
        this.cancelEventId = null;
        this.triggerSuccess('updated');
        this.loadEvents();
      },
      error: (err: any) => console.error(err)
    });
  }

  closeCancelConfirm(): void {
    this.showCancelConfirm = false;
    this.cancelEventId = null;
  }

  private triggerSuccess(type: 'created' | 'updated' | 'deleted'): void {
    this.message =
      type === 'created'
        ? 'Event created successfully!'
        : type === 'updated'
        ? 'Event updated successfully!'
        : 'Event deleted successfully!';

    this.showSuccess = true;
    setTimeout(() => (this.showSuccess = false), 3000);
  }

  toggleAI(): void {
    this.aiOpen = !this.aiOpen;
  }

  quickAsk(question: string): void {
    this.userInput = question;
    this.sendMessage();
  }

  sendMessage(): void {
    const q = this.userInput.trim();
    if (!q || this.isLoading) return;

    this.messages.push({ role: 'user', text: q });
    this.userInput = '';
    this.isLoading = true;

    const history = this.messages
      .filter(m => m.role === 'user')
      .map(m => ({
        role: 'user',
        content: m.text.replace(/<[^>]*>/g, '')
      }));

    this.AiEventService.chat(history).subscribe({
      next: (res: any) => {
        const formatted = res.reply
          .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
          .replace(/\n/g, '<br>');

        this.messages.push({ role: 'ai', text: formatted });
        this.isLoading = false;
      },
      error: () => {
        this.messages.push({ role: 'ai', text: '⚠️ AI service error. Please try again.' });
        this.isLoading = false;
      }
    });
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    try {
      if (this.messagesContainer?.nativeElement) {
        this.messagesContainer.nativeElement.scrollTop =
          this.messagesContainer.nativeElement.scrollHeight;
      }
    } catch {}
  }
}