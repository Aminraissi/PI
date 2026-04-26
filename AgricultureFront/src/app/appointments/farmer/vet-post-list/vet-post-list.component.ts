import { Component, Input, OnInit } from '@angular/core';
import { AppointmentsApiService } from '../../services/appointments-api.service';
import { VetPost } from '../../models/appointments.models';

@Component({
  selector: 'app-vet-post-list',
  standalone: false,
  templateUrl: './vet-post-list.component.html',
  styleUrls: ['./vet-post-list.component.css']
})
export class VetPostListComponent implements OnInit {
  @Input() vetId!: number;

  posts: VetPost[] = [];
  loading = true;
  error = '';

  activeFilter: 'ALL' | 'ARTICLE' | 'VIDEO' = 'ALL';
  selectedPost: VetPost | null = null;

  readonly BASE = 'http://localhost:8088';

  constructor(private api: AppointmentsApiService) {}

  ngOnInit() {
    this.api.getVetPosts(this.vetId).subscribe({
      next: p => { this.posts = p; this.loading = false; },
      error: () => { this.error = 'Unable to load publications.'; this.loading = false; }
    });
  }

  get filtered(): VetPost[] {
    if (this.activeFilter === 'ALL') return this.posts;
    return this.posts.filter(p => p.type === this.activeFilter);
  }

  get articleCount() { return this.posts.filter(p => p.type === 'ARTICLE').length; }
  get videoCount()   { return this.posts.filter(p => p.type === 'VIDEO').length;   }

  mediaUrl(post: VetPost): string {
    if (!post.mediaUrl) return '';
    if (post.mediaUrl.startsWith('http')) return post.mediaUrl;
    if (post.mediaUrl.startsWith('/inventaires/')) return this.BASE + post.mediaUrl;
    return `${this.BASE}/inventaires${post.mediaUrl}`;
  }

  openPost(post: VetPost) { this.selectedPost = post; }
  closePost() { this.selectedPost = null; }

  descriptionText(post: VetPost): string {
    const div = document.createElement('div');
    div.innerHTML = post.description || '';
    return div.textContent || div.innerText || '';
  }

  formatDate(d: string): string {
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  }
}
