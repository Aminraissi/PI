import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { DiagnosticAssistantChatRequest } from '../../models/appointments.models';
import { AppointmentsApiService } from '../../services/appointments-api.service';

interface AiChatMessage {
  role: 'farmer' | 'ai';
  content: string;
}

@Component({
  selector: 'app-vet-ai-chatbot',
  templateUrl: './vet-ai-chatbot.component.html',
  styleUrls: ['./vet-ai-chatbot.component.css']
})
export class VetAiChatbotComponent {
  chatting = false;
  chatError = '';
  chatMessages: AiChatMessage[] = [
    {
      role: 'ai',
      content: 'Hello. I am your intelligent AI vet. Feel free to describe your animal\'s case here.'
    }
  ];

  readonly chatForm = this.fb.group({
    question: ['', Validators.required],
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly appointmentsApi: AppointmentsApiService
  ) {}

  sendChat(): void {
    const question = this.chatForm.getRawValue().question?.trim() || '';
    this.chatError = '';

    if (!question || this.chatting) {
      return;
    }

    if (this.chatForm.invalid) {
      this.chatForm.markAllAsTouched();
      this.chatError = 'Saisissez votre question pour discuter avec le chatbot.';
      return;
    }

    this.chatMessages = [...this.chatMessages, { role: 'farmer', content: question }];
    const payload = this.buildChatPayload();
    this.chatForm.patchValue({ question: '' });
    this.chatting = true;

    this.appointmentsApi.chatWithIndependentAssistant(payload)
      .pipe(finalize(() => this.chatting = false))
      .subscribe({
        next: response => {
          this.chatMessages = [
            ...this.chatMessages,
            {
              role: 'ai',
              content: response.answer || 'I could not generate a response at the moment.'
            }
          ];
        },
        error: err => {
          this.chatError = err?.error?.message || 'The AI conversation failed.';
          this.chatMessages = [
            ...this.chatMessages,
            {
              role: 'ai',
              content: 'I could not respond at the moment. Please check the AI service and try again.'
            }
          ];
        }
      });
  }

  hasChatError(controlName: string): boolean {
    const control = this.chatForm.get(controlName);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  private buildChatPayload(): DiagnosticAssistantChatRequest {
    const value = this.chatForm.getRawValue();
    return {
      question: value.question?.trim() || '',
    };
  }
}