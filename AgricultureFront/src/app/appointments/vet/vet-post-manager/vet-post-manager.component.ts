import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { AppointmentsApiService } from '../../services/appointments-api.service';
import { AuthService } from '../../../services/auth/auth.service';
import { VetPost, PostType } from '../../models/appointments.models';
import { ToastService } from 'src/app/core/services/toast.service';

@Component({
  selector: 'app-vet-post-manager',
  standalone: false,
  templateUrl: './vet-post-manager.component.html',
  styleUrls: ['./vet-post-manager.component.css']
})
export class VetPostManagerComponent implements OnInit {

  posts: VetPost[] = [];
  loading = true;
  submitting = false;
  error = '';

  // Formulaire
  showForm = false;
  editingPost: VetPost | null = null;
  selectedFile: File | null = null;
  filePreviewUrl: string | null = null;
  fileError = '';

  form = new FormGroup({
    titre:       new FormControl('', [Validators.required, Validators.minLength(3)]),
    description: new FormControl(''),
    type:        new FormControl<PostType>('ARTICLE', Validators.required),
  });

  // Confirmation suppression
  deleteTargetId: number | null = null;

  readonly BASE = 'http://localhost:8088';

  constructor(
    private api: AppointmentsApiService,
    private auth: AuthService,
    private toast: ToastService
  ) {}

  ngOnInit() { this.load(); }

  load() {
    const id = this.auth.getCurrentUserId()!;
    this.loading = true;
    this.api.getVetPosts(id).subscribe({
      next: p => { this.posts = p; this.loading = false; },
      error: () => { this.error = 'Impossible de charger les publications.'; this.loading = false; }
    });
  }

  openCreate() {
    this.editingPost = null;
    this.form.reset({ titre: '', description: '', type: 'ARTICLE' });
    this.selectedFile = null;
    this.filePreviewUrl = null;
    this.fileError = '';
    this.showForm = true;
  }

  openEdit(post: VetPost) {
    this.editingPost = post;
    this.form.setValue({ titre: post.titre, description: post.description || '', type: post.type });
    this.selectedFile = null;
    this.filePreviewUrl = post.mediaUrl ? this.mediaUrl(post) : null;
    this.fileError = '';
    this.showForm = true;
  }

  closeForm() { this.showForm = false; this.editingPost = null; }

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    this.fileError = '';
    if (!input.files?.length) return;
    const file = input.files[0];
    const type = this.form.value.type;
    const maxSize = type === 'VIDEO' ? 100 * 1024 * 1024 : 5 * 1024 * 1024;

    if (type === 'VIDEO' && !file.type.startsWith('video/')) {
      this.fileError = 'Veuillez sélectionner un fichier vidéo (MP4, WebM…)'; return;
    }
    if (type === 'ARTICLE' && !file.type.startsWith('image/')) {
      this.fileError = 'Veuillez sélectionner une image (JPG, PNG, WebP…)'; return;
    }
    if (file.size > maxSize) {
      this.fileError = `Fichier trop volumineux (max ${type === 'VIDEO' ? '100 Mo' : '5 Mo'})`; return;
    }

    this.selectedFile = file;
    const reader = new FileReader();
    reader.onload = e => this.filePreviewUrl = e.target?.result as string;
    reader.readAsDataURL(file);
  }

  submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting = true;

    const formData = new FormData();
    const blob = new Blob([JSON.stringify({
      titre:       this.form.value.titre,
      description: this.form.value.description,
      type:        this.form.value.type,
    })], { type: 'application/json' });
    formData.append('data', blob);
    if (this.selectedFile) formData.append('media', this.selectedFile);

    const obs = this.editingPost
      ? this.api.updateVetPost(this.editingPost.id, formData)
      : this.api.createVetPost(formData);

    obs.subscribe({
      next: saved => {
        if (this.editingPost) {
          this.posts = this.posts.map(p => p.id === saved.id ? saved : p);
          this.toast.success('Publication mise à jour !');
        } else {
          this.posts.unshift(saved);
          this.toast.success('Publication créée avec succès !');
        }
        this.submitting = false;
        this.closeForm();
      },
      error: e => {
        this.submitting = false;
        this.toast.error(e.error?.message || 'Erreur lors de l\'enregistrement');
      }
    });
  }

  confirmDelete(id: number) { this.deleteTargetId = id; }
  cancelDelete()            { this.deleteTargetId = null; }

  doDelete() {
    if (!this.deleteTargetId) return;
    const id = this.deleteTargetId;
    this.deleteTargetId = null;
    this.api.deleteVetPost(id).subscribe({
      next: () => {
        this.posts = this.posts.filter(p => p.id !== id);
        this.toast.success('Publication supprimée.');
      },
      error: () => this.toast.error('Erreur lors de la suppression.')
    });
  }

  mediaUrl(post: VetPost): string {
    if (!post.mediaUrl) return '';
    if (post.mediaUrl.startsWith('http')) return post.mediaUrl;
    if (post.mediaUrl.startsWith('/inventaires/')) return this.BASE + post.mediaUrl;
    return `${this.BASE}/inventaires${post.mediaUrl}`;
  }

  formatDate(d: string): string {
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  }

  get f() { return this.form.controls; }
  get isVideo() { return this.form.value.type === 'VIDEO'; }

  get articleCount() { return this.posts.filter(p => p.type === 'ARTICLE').length; }
  get videoCount()   { return this.posts.filter(p => p.type === 'VIDEO').length;   }
}
