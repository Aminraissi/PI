import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AppointmentsApiService } from '../../services/appointments-api.service';
import { AuthService } from '../../../services/auth/auth.service';
import { BadWordsService } from '../../../services/bad-words/bad-words.service';
import { AvisResponse, VetRatingSummary } from '../../models/appointments.models';

const GROQ_API_KEY = 'gsk_lqrDYJ6TnFxRBhwlAzp3WGdyb3FYDCaETRWdw5lZHAcjJDm3sWP';
const GROQ_API_URL = 'https://api.groq.com/openai/v1/chat/completions';
const GROQ_MODEL   = 'llama3-8b-8192';

@Component({
  selector: 'app-vet-avis',
  standalone: false,
  templateUrl: './vet-avis.component.html',
  styleUrls: ['./vet-avis.component.css']
})
export class VetAvisComponent implements OnInit {

  avis: AvisResponse[] = [];
  summary: VetRatingSummary | null = null;
  loading = true;
  error = '';
  currentVetId!: number;

  // Réponses vétérinaire
  replyInputs: { [avisId: number]: string } = {};
  showReplyInput: { [avisId: number]: boolean } = {};
  submittingReply: { [avisId: number]: boolean } = {};
  replyError: { [avisId: number]: string } = {};

  // Bad-word check en temps réel pour les réponses
  replyBadWord: { [avisId: number]: boolean } = {};
  checkingGroq: { [avisId: number]: boolean } = {};
  private groqDebounceTimers: { [id: number]: any } = {};

  // Traduction des commentaires (commentaire principal + sous-commentaires)
  // Clé = commentId (a.id pour le commentaire principal, c.id pour les sous-commentaires)
  cmtTranslations:      { [cmtId: number]: string }       = {};
  cmtTranslationLang:   { [cmtId: number]: 'fr' | 'en' } = {};
  cmtTranslatingId:     { [cmtId: number]: boolean }      = {};
  cmtTranslationErrors: { [cmtId: number]: string }       = {};

  // Traduction de la réponse du vétérinaire
  vetReplyTranslations: { [avisId: number]: string } = {};
  vetReplyTranslationLang: { [avisId: number]: 'fr' | 'en' } = {};
  vetReplyTranslatingId: { [avisId: number]: boolean } = {};
  vetReplyTranslationErrors: { [avisId: number]: string } = {};

  // Filtre
  filterNote: number | null = null;
  stars5 = [1, 2, 3, 4, 5];

  constructor(
      private api:      AppointmentsApiService,
      private auth:     AuthService,
      public  badWords: BadWordsService,
      private http:     HttpClient
  ) {}

  ngOnInit() {
    this.currentVetId = this.auth.getCurrentUserId()!;
    this.load();
  }

  load() {
    this.loading = true;
    this.error = '';
    this.api.getVetRatingSummary(this.currentVetId).subscribe({ next: s => { this.summary = s; }, error: () => {} });
    this.api.getAvisByVet(this.currentVetId).subscribe({
      next: list => { this.avis = list; this.loading = false; },
      error: e => { this.loading = false; this.error = e.error?.message || 'Erreur lors du chargement des avis'; }
    });
  }

  // Filtrage
  setFilter(n: number | null) { this.filterNote = n; }
  get filteredAvis(): AvisResponse[] {
    if (this.filterNote === null) return this.avis;
    return this.avis.filter(a => a.note === this.filterNote);
  }
  countByNote(n: number): number { return this.avis.filter(a => a.note === n).length; }

  // Répondre à un avis
  toggleReplyInput(avisId: number) {
    this.showReplyInput[avisId] = !this.showReplyInput[avisId];
    if (!this.replyInputs[avisId]) this.replyInputs[avisId] = '';
    this.replyError[avisId]   = '';
    this.replyBadWord[avisId] = false;
  }

  onReplyInput(avisId: number) {
    const text = this.replyInputs[avisId] || '';
    if (this.badWords.containsBadWord(text)) { this.replyBadWord[avisId] = true; return; }
    this.replyBadWord[avisId] = false;
    if (text.trim().length >= 10) this.checkWithGroq(avisId, text);
  }

  private checkWithGroq(avisId: number, text: string): void {
    clearTimeout(this.groqDebounceTimers[avisId]);
    this.groqDebounceTimers[avisId] = setTimeout(() => {
      this.checkingGroq[avisId] = true;
      const body = {
        model: GROQ_MODEL, max_tokens: 10,
        messages: [
          { role: 'system', content: 'You are a strict content moderation assistant. Respond with exactly one word: YES if the text contains offensive, vulgar, abusive, or inappropriate language. Respond NO otherwise. No explanation.' },
          { role: 'user', content: text }
        ]
      };
      this.http.post<any>(GROQ_API_URL, body, {
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${GROQ_API_KEY}` }
      }).subscribe({
        next: res => {
          this.checkingGroq[avisId] = false;
          const answer = (res?.choices?.[0]?.message?.content || '').trim().toUpperCase();
          this.replyBadWord[avisId] = answer.startsWith('YES');
        },
        error: () => { this.checkingGroq[avisId] = false; }
      });
    }, 600);
  }

  submitReply(a: AvisResponse) {
    const contenu = (this.replyInputs[a.id] || '').trim();
    if (!contenu) { this.replyError[a.id] = 'La réponse ne peut pas être vide.'; return; }
    if (this.badWords.containsBadWord(contenu) || this.replyBadWord[a.id]) {
      this.replyError[a.id] = '⚠️ Votre réponse contient des mots inappropriés. Veuillez la reformuler.';
      return;
    }
    this.submittingReply[a.id] = true;
    this.replyError[a.id] = '';
    this.api.addReponseVet(a.id, contenu).subscribe({
      next: rep => {
        a.reponseVet = rep;
        this.replyInputs[a.id] = '';
        this.showReplyInput[a.id] = false;
        this.submittingReply[a.id] = false;
        this.replyBadWord[a.id] = false;
      },
      error: e => {
        this.submittingReply[a.id] = false;
        this.replyError[a.id] = e.error?.message || "Erreur lors de l'envoi de la réponse";
      }
    });
  }

  hasReplyBadWord(avisId: number): boolean {
    const text = this.replyInputs[avisId] || '';
    return text.length > 2 && (this.badWords.containsBadWord(text) || !!this.replyBadWord[avisId]);
  }

  // ── Traduction des commentaires (commentaire principal ET sous-commentaires) ────
  translateComment(cmtId: number, contenu: string): void {
    if (!contenu?.trim()) return;
    this.cmtTranslatingId[cmtId]     = true;
    this.cmtTranslationErrors[cmtId] = '';
    delete this.cmtTranslations[cmtId];
    delete this.cmtTranslationLang[cmtId];
    this.tryTranslateCmt(cmtId, contenu, 'fr|en', true);
  }

  private tryTranslateCmt(cmtId: number, text: string, langPair: string, allowFallback: boolean): void {
    const url = `https://api.mymemory.translated.net/get?q=${encodeURIComponent(text)}&langpair=${langPair}`;
    this.http.get<any>(url).subscribe({
      next: res => {
        const translated: string = (res?.responseData?.translatedText || '').trim();
        const status: number     =  res?.responseStatus ?? 0;

        if (translated.toUpperCase().startsWith('QUERY LENGTH') || translated.toUpperCase().includes('MYMEMORY WARNING')) {
          this.cmtTranslatingId[cmtId]     = false;
          this.cmtTranslationErrors[cmtId] = 'Limite de traduction atteinte. Réessayez plus tard.';
          return;
        }

        if (status === 200 && translated) {
          const same = translated.toLowerCase() === text.toLowerCase().trim();

          if (same && allowFallback) {
            // Fallback: si fr→en ne change rien, essayer fr→ar
            if (langPair === 'fr|en') {
              this.tryTranslateCmt(cmtId, text, 'fr|ar', false);
            } else if (langPair === 'fr|ar') {
              this.tryTranslateCmt(cmtId, text, 'fr|en', false);
            }
            return;
          }

          this.cmtTranslatingId[cmtId]   = false;
          this.cmtTranslations[cmtId]    = translated;
          this.cmtTranslationLang[cmtId] = langPair === 'fr|en' ? 'en' : 'fr';
        } else {
          if (allowFallback && langPair === 'fr|en') {
            this.tryTranslateCmt(cmtId, text, 'fr|ar', false);
          } else if (allowFallback && langPair === 'fr|ar') {
            this.tryTranslateCmt(cmtId, text, 'fr|en', false);
          } else {
            this.cmtTranslatingId[cmtId] = false;
            this.cmtTranslationErrors[cmtId] = 'Traduction non disponible.';
          }
        }
      },
      error: () => {
        this.cmtTranslatingId[cmtId]     = false;
        this.cmtTranslationErrors[cmtId] = 'Erreur réseau.';
      }
    });
  }

  clearCommentTranslation(cmtId: number): void {
    delete this.cmtTranslations[cmtId];
    delete this.cmtTranslationErrors[cmtId];
    delete this.cmtTranslationLang[cmtId];
  }

  cmtTranslationBadgeLabel(cmtId: number): string {
    return this.cmtTranslationLang[cmtId] === 'en'
        ? '🇬🇧 Traduit en anglais · MyMemory'
        : '🇫🇷 Traduit en français · MyMemory';
  }

  // Traduction réponse vétérinaire
  translateVetReply(a: AvisResponse): void {
    if (!a.reponseVet?.contenu?.trim()) return;
    this.vetReplyTranslatingId[a.id]     = true;
    this.vetReplyTranslationErrors[a.id] = '';
    delete this.vetReplyTranslations[a.id];
    delete this.vetReplyTranslationLang[a.id];
    this.tryTranslateVetReply(a, a.reponseVet.contenu, 'fr|en', true);
  }

  private tryTranslateVetReply(a: AvisResponse, text: string, langPair: string, allowFallback: boolean): void {
    const url = `https://api.mymemory.translated.net/get?q=${encodeURIComponent(text)}&langpair=${langPair}`;
    this.http.get<any>(url).subscribe({
      next: res => {
        const translated: string = (res?.responseData?.translatedText || '').trim();
        const status: number     =  res?.responseStatus ?? 0;

        if (translated.toUpperCase().startsWith('QUERY LENGTH') || translated.toUpperCase().includes('MYMEMORY WARNING')) {
          this.vetReplyTranslatingId[a.id] = false;
          this.vetReplyTranslationErrors[a.id] = 'Limite de traduction atteinte.';
          return;
        }

        if (status === 200 && translated) {
          const same = translated.toLowerCase() === text.toLowerCase().trim();

          if (same && allowFallback) {
            if (langPair === 'fr|en') {
              this.tryTranslateVetReply(a, text, 'fr|ar', false);
            } else if (langPair === 'fr|ar') {
              this.tryTranslateVetReply(a, text, 'fr|en', false);
            }
            return;
          }

          this.vetReplyTranslatingId[a.id]   = false;
          this.vetReplyTranslations[a.id]    = translated;
          this.vetReplyTranslationLang[a.id] = langPair === 'fr|en' ? 'en' : 'fr';
        } else {
          if (allowFallback && langPair === 'fr|en') {
            this.tryTranslateVetReply(a, text, 'fr|ar', false);
          } else if (allowFallback && langPair === 'fr|ar') {
            this.tryTranslateVetReply(a, text, 'fr|en', false);
          } else {
            this.vetReplyTranslatingId[a.id] = false;
            this.vetReplyTranslationErrors[a.id] = 'Traduction non disponible.';
          }
        }
      },
      error: () => { this.vetReplyTranslatingId[a.id] = false; this.vetReplyTranslationErrors[a.id] = 'Erreur réseau.'; }
    });
  }

  clearVetReplyTranslation(avisId: number): void {
    delete this.vetReplyTranslations[avisId];
    delete this.vetReplyTranslationErrors[avisId];
    delete this.vetReplyTranslationLang[avisId];
  }

  vetReplyTranslationBadgeLabel(avisId: number): string {
    return this.vetReplyTranslationLang[avisId] === 'en'
        ? '🇬🇧 Traduit en anglais · MyMemory'
        : '🇫🇷 Traduit en français · MyMemory';
  }

  // Helpers
  initials(nom: string, prenom: string): string {
    return ((prenom?.charAt(0) || '') + (nom?.charAt(0) || '')).toUpperCase();
  }
  formatDate(d: string): string {
    if (!d) return '';
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  }
  starsArray(note: number): boolean[] { return [1,2,3,4,5].map(i => i <= Math.round(note)); }
  distributionWidth(star: number): number {
    if (!this.summary || this.summary.totalAvis === 0) return 0;
    return Math.round(((this.summary.distribution?.[star] || 0) / this.summary.totalAvis) * 100);
  }
  distributionCount(star: number): number { return this.summary?.distribution?.[star] || 0; }
  get averageDisplay(): string {
    if (!this.summary || this.summary.totalAvis === 0) return '—';
    return this.summary.moyenneNote.toFixed(1);
  }
  noteLabel(n: number): string { return ['','Mauvais','Insuffisant','Bien','Très bien','Excellent'][n] || ''; }
  trackById(_: number, a: AvisResponse) { return a.id; }
}