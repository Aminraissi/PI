import { Component, Input, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AppointmentsApiService } from '../../services/appointments-api.service';
import { AuthService } from '../../../services/auth/auth.service';
import { BadWordsService } from '../../../services/bad-words/bad-words.service';
import {
  AvisResponse,
  VetRatingSummary,
  CreateAvisRequest,
  CommentaireAvisResponse
} from '../../models/appointments.models';

const GROQ_API_KEY = 'gsk_lqrDYJ6TnFxRBhwlAzp3WGdyb3FYDCaETRWdw5lZHAcjJDm3sWP';
const GROQ_API_URL = 'https://api.groq.com/openai/v1/chat/completions';
const GROQ_MODEL   = 'llama3-8b-8192';

@Component({
  selector: 'app-farmer-avis',
  standalone: false,
  templateUrl: './farmer-avis.component.html',
  styleUrls: ['./farmer-avis.component.css']
})
export class FarmerAvisComponent implements OnInit {
  @Input() vetId!: number;

  // Data
  avis: AvisResponse[] = [];
  summary: VetRatingSummary | null = null;
  loading = true;
  error = '';

  // Current user
  currentUserId!: number;
  currentUserRole = '';
  hasAlreadyReviewed = false;

  // Formulaire nouvel avis
  showAvisForm = false;
  newNote = 0;
  hoverNote = 0;
  newCommentaire = '';
  submittingAvis = false;
  avisError = '';

  // Commentaires (reply input)
  commentInputs:    { [avisId: number]: string }  = {};
  showCommentInput: { [avisId: number]: boolean } = {};
  submittingComment:{ [avisId: number]: boolean } = {};

  // ── Bad-word + Groq sur le champ de reply ───────────────────────────────
  commentBadWord:  { [avisId: number]: boolean } = {};
  checkingGroq:    { [avisId: number]: boolean } = {};
  private groqDebounceTimers: { [id: number]: any } = {};

  // Likes
  likingId: number | null = null;

  // ── Traduction de l'avis principal ───────────────────────────────────────
  translations:      { [avisId: number]: string }        = {};
  translationLang:   { [avisId: number]: 'fr' | 'en' }  = {};
  translatingId:     { [avisId: number]: boolean }       = {};
  translationErrors: { [avisId: number]: string }        = {};

  // ── Traduction des commentaires de reply (par commentaire individuel) ────
  // Clé = commentId (c.id)
  cmtTranslations:      { [cmtId: number]: string }       = {};
  cmtTranslationLang:   { [cmtId: number]: 'fr' | 'en' } = {};
  cmtTranslatingId:     { [cmtId: number]: boolean }      = {};
  cmtTranslationErrors: { [cmtId: number]: string }       = {};

  constructor(
    private api:  AppointmentsApiService,
    private auth: AuthService,
    public  badWords: BadWordsService,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.currentUserId   = this.auth.getCurrentUserId()!;
    this.currentUserRole = this.auth.getCurrentRole() || '';
    this.load();
  }

  load() {
    this.loading = true;
    this.error   = '';

    this.api.getVetRatingSummary(this.vetId).subscribe({
      next: s  => { this.summary = s; },
      error: () => {}
    });

    this.api.getAvisByVet(this.vetId).subscribe({
      next: list => {
        this.avis = list;
        this.hasAlreadyReviewed = list.some(a => a.agriculteurId === this.currentUserId);
        this.loading = false;
      },
      error: e => {
        this.loading = false;
        this.error = e.error?.message || 'Erreur lors du chargement des avis';
      }
    });
  }

  // ── Étoiles ─────────────────────────────────────────────
  setHover(n: number)  { this.hoverNote = n; }
  clearHover()         { this.hoverNote = 0; }
  setNote(n: number)   { this.newNote = n; }

  starsArray(note: number): boolean[] {
    return [1, 2, 3, 4, 5].map(i => i <= Math.round(note));
  }

  noteLabel(n: number): string {
    return ['', 'Poor', 'Fair', 'Good', 'Very Good', 'Excellent'][n] || '';
  }

  distributionWidth(star: number): number {
    if (!this.summary || this.summary.totalAvis === 0) return 0;
    const count = this.summary.distribution?.[star] || 0;
    return Math.round((count / this.summary.totalAvis) * 100);
  }

  distributionCount(star: number): number {
    return this.summary?.distribution?.[star] || 0;
  }

  // ── Créer un avis ────────────────────────────────────────
  openAvisForm()   { this.showAvisForm = true; this.avisError = ''; }
  cancelAvisForm() {
    this.showAvisForm   = false;
    this.newNote        = 0;
    this.newCommentaire = '';
    this.avisError      = '';
  }

  submitAvis() {
    if (this.newNote === 0) {
      this.avisError = 'Please select a rating.'; return;
    }
    if (!this.newCommentaire.trim()) {
      this.avisError = 'Please write a comment.'; return;
    }
    if (this.badWords.containsBadWord(this.newCommentaire)) {
      this.avisError = '⚠️ Your comment contains inappropriate words. Please rephrase it.'; return;
    }

    this.submittingAvis = true;
    this.avisError      = '';

    const req: CreateAvisRequest = {
      note:           this.newNote,
      commentaire:    this.newCommentaire.trim(),
      veterinarianId: this.vetId
    };

    this.api.createAvis(req).subscribe({
      next: newAvis => {
        this.avis.unshift(newAvis);
        this.hasAlreadyReviewed = true;
        this.submittingAvis     = false;
        this.cancelAvisForm();
        this.load();
      },
      error: e => {
        this.submittingAvis = false;
        this.avisError = this.extractErrorMessage(e, 'Error occurred while submitting your review');
      }
    });
  }

  get hasInappropriateContent(): boolean {
    return this.newCommentaire.length > 2 && this.badWords.containsBadWord(this.newCommentaire);
  }

  private extractErrorMessage(error: any, fallback: string): string {
    if (typeof error?.error === 'string' && error.error.trim()) return error.error;
    if (error?.error?.message && String(error.error.message).trim()) return String(error.error.message);
    if (error?.error?.error  && String(error.error.error).trim())   return String(error.error.error);
    if (error?.message       && String(error.message).trim())       return String(error.message);
    if (error?.status === 400) return 'Your review was rejected by moderation. Please rephrase it.';
    return fallback;
  }

  // ── Traduction de l'avis principal (inchangée) ───────────────────────────
  translateAvis(a: AvisResponse): void {
    if (!a.commentaire?.trim()) return;
    this.translatingId[a.id]     = true;
    this.translationErrors[a.id] = '';
    delete this.translations[a.id];
    delete this.translationLang[a.id];
    this.tryTranslate(a, 'en|fr', true);
  }

  private tryTranslate(a: AvisResponse, langPair: string, allowFallback: boolean): void {
    const url = `https://api.mymemory.translated.net/get`
              + `?q=${encodeURIComponent(a.commentaire)}&langpair=${langPair}`;

    this.http.get<any>(url).subscribe({
      next: res => {
        const translated: string = (res?.responseData?.translatedText || '').trim();
        const status: number     =  res?.responseStatus ?? 0;

        if (translated.toUpperCase().startsWith('QUERY LENGTH') ||
            translated.toUpperCase().includes('MYMEMORY WARNING')) {
          this.translatingId[a.id]     = false;
          this.translationErrors[a.id] = 'Free translation limit reached. Please try again later.';
          return;
        }

        if (status === 200 && translated) {
          const sameAsOriginal = translated.toLowerCase() === a.commentaire.toLowerCase().trim();
          if (sameAsOriginal && allowFallback && langPair === 'en|fr') { this.tryTranslate(a, 'ar|fr', false); return; }
          if (sameAsOriginal && langPair === 'ar|fr')                  { this.tryTranslate(a, 'fr|en', false); return; }

          this.translatingId[a.id]   = false;
          this.translations[a.id]    = translated;
          this.translationLang[a.id] = langPair === 'fr|en' ? 'en' : 'fr';
        } else {
          if (allowFallback && langPair === 'en|fr') this.tryTranslate(a, 'ar|fr', false);
          else if (langPair === 'ar|fr')             this.tryTranslate(a, 'fr|en', false);
          else { this.translatingId[a.id] = false; this.translationErrors[a.id] = 'Translation unavailable for this text.'; }
        }
      },
      error: () => {
        this.translatingId[a.id]     = false;
        this.translationErrors[a.id] = 'Connection error. Please check your network.';
      }
    });
  }

  clearTranslation(avisId: number): void {
    delete this.translations[avisId];
    delete this.translationErrors[avisId];
    delete this.translationLang[avisId];
  }

  translationBadgeLabel(avisId: number): string {
    return this.translationLang[avisId] === 'en'
      ? '🇬🇧 Translated to English · MyMemory'
      : '🇫🇷 Translated to French · MyMemory';
  }

  // ── Traduction des commentaires de reply (individuel par cmtId) ──────────
  translateComment(cmtId: number, contenu: string): void {
    if (!contenu?.trim()) return;
    this.cmtTranslatingId[cmtId]     = true;
    this.cmtTranslationErrors[cmtId] = '';
    delete this.cmtTranslations[cmtId];
    delete this.cmtTranslationLang[cmtId];
    this.tryTranslateCmt(cmtId, contenu, 'en|fr', true);
  }

  private tryTranslateCmt(cmtId: number, text: string, langPair: string, allowFallback: boolean): void {
    const url = `https://api.mymemory.translated.net/get`
              + `?q=${encodeURIComponent(text)}&langpair=${langPair}`;

    this.http.get<any>(url).subscribe({
      next: res => {
        const translated: string = (res?.responseData?.translatedText || '').trim();
        const status: number     =  res?.responseStatus ?? 0;

        if (translated.toUpperCase().startsWith('QUERY LENGTH') ||
            translated.toUpperCase().includes('MYMEMORY WARNING')) {
          this.cmtTranslatingId[cmtId]     = false;
          this.cmtTranslationErrors[cmtId] = 'Free translation limit reached. Try again later.';
          return;
        }

        if (status === 200 && translated) {
          const same = translated.toLowerCase() === text.toLowerCase().trim();
          if (same && allowFallback && langPair === 'en|fr') { this.tryTranslateCmt(cmtId, text, 'ar|fr', false); return; }
          if (same && langPair === 'ar|fr')                  { this.tryTranslateCmt(cmtId, text, 'fr|en', false); return; }

          this.cmtTranslatingId[cmtId]   = false;
          this.cmtTranslations[cmtId]    = translated;
          this.cmtTranslationLang[cmtId] = langPair === 'fr|en' ? 'en' : 'fr';
        } else {
          if (allowFallback && langPair === 'en|fr') this.tryTranslateCmt(cmtId, text, 'ar|fr', false);
          else if (langPair === 'ar|fr')             this.tryTranslateCmt(cmtId, text, 'fr|en', false);
          else { this.cmtTranslatingId[cmtId] = false; this.cmtTranslationErrors[cmtId] = 'Translation unavailable.'; }
        }
      },
      error: () => {
        this.cmtTranslatingId[cmtId]     = false;
        this.cmtTranslationErrors[cmtId] = 'Connection error. Check your network.';
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
      ? '🇬🇧 Translated to English · MyMemory'
      : '🇫🇷 Translated to French · MyMemory';
  }

  // ── Bad-word + Groq sur le champ de reply ───────────────────────────────
  onCommentInput(avisId: number): void {
    const text = this.commentInputs[avisId] || '';
    if (this.badWords.containsBadWord(text)) { this.commentBadWord[avisId] = true; return; }
    this.commentBadWord[avisId] = false;
    if (text.trim().length >= 10) this.checkWithGroq(avisId, text);
  }

  private checkWithGroq(avisId: number, text: string): void {
    clearTimeout(this.groqDebounceTimers[avisId]);
    this.groqDebounceTimers[avisId] = setTimeout(() => {
      this.checkingGroq[avisId] = true;
      const body = {
        model: GROQ_MODEL,
        max_tokens: 10,
        messages: [
          {
            role: 'system',
            content: 'You are a strict content moderation assistant. Respond with exactly one word: YES if the text contains offensive, vulgar, abusive, or inappropriate language (insults, hate speech, threats, profanity). Respond NO otherwise. No explanation.'
          },
          { role: 'user', content: text }
        ]
      };
      this.http.post<any>(GROQ_API_URL, body, {
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${GROQ_API_KEY}` }
      }).subscribe({
        next: res => {
          this.checkingGroq[avisId] = false;
          const answer = (res?.choices?.[0]?.message?.content || '').trim().toUpperCase();
          this.commentBadWord[avisId] = answer.startsWith('YES');
        },
        error: () => { this.checkingGroq[avisId] = false; }
      });
    }, 600);
  }

  hasCommentBadWord(avisId: number): boolean {
    const text = this.commentInputs[avisId] || '';
    return text.length > 2 && (this.badWords.containsBadWord(text) || !!this.commentBadWord[avisId]);
  }

  // ── Commentaire d'agriculteur ────────────────────────────
  toggleCommentInput(avisId: number) {
    this.showCommentInput[avisId] = !this.showCommentInput[avisId];
    if (!this.commentInputs[avisId]) this.commentInputs[avisId] = '';
    this.commentBadWord[avisId] = false;
  }

  submitCommentaire(a: AvisResponse) {
    const contenu = (this.commentInputs[a.id] || '').trim();
    if (!contenu) return;
    if (this.badWords.containsBadWord(contenu) || this.commentBadWord[a.id]) {
      return; // Le warning inline bloque déjà — pas d'alert()
    }
    this.submittingComment[a.id] = true;
    this.api.addCommentaire(a.id, contenu).subscribe({
      next: c => {
        a.commentaires.push(c);
        this.commentInputs[a.id]     = '';
        this.showCommentInput[a.id]  = false;
        this.submittingComment[a.id] = false;
        this.commentBadWord[a.id]    = false;
      },
      error: () => { this.submittingComment[a.id] = false; }
    });
  }

  toggleLike(a: AvisResponse) {
    if (a.agriculteurId === this.currentUserId) return;
    this.likingId = a.id;
    this.api.toggleLike(a.id).subscribe({
      next: () => {
        if (a.likedByMe) { a.likedByMe = false; a.nbLikes--; }
        else             { a.likedByMe = true;  a.nbLikes++; }
        this.likingId = null;
      },
      error: () => { this.likingId = null; }
    });
  }

  initials(nom: string, prenom: string): string {
    return ((prenom?.charAt(0) || '') + (nom?.charAt(0) || '')).toUpperCase();
  }

  formatDate(d: string): string {
    if (!d) return '';
    return new Date(d).toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'long', year: 'numeric'
    });
  }

  get isAgriculteur(): boolean {
    return this.currentUserRole === 'AGRICULTEUR';
  }

  get averageDisplay(): string {
    if (!this.summary || this.summary.totalAvis === 0) return '—';
    return this.summary.moyenneNote.toFixed(1);
  }

  stars5 = [1, 2, 3, 4, 5];
}