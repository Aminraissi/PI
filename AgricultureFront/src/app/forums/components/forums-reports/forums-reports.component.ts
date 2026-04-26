import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subject, forkJoin } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from 'src/app/services/auth/auth.service';
import {
  ForumPost,
  ForumReportCaseActionTarget,
  ForumReportDetail,
  ForumsMockService
} from '../../forums.mock.service';

type ReportStatusFilter = 'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED';

interface ReportCaseSummary {
  key: string;
  targetType: 'POST' | 'REPLY' | 'COMMENT';
  targetId: number;
  postId: number;
  replyId?: number | null;
  commentId?: number | null;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  reports: ForumReportDetail[];
  reportCount: number;
  uniqueReporters: number;
  latestAt: string;
  latestReason: string;
  reporterIds: number[];
  postTitle: string;
  targetPreview: string;
}

@Component({
  selector: 'app-forums-reports',
  templateUrl: './forums-reports.component.html',
  styleUrls: ['./forums-reports.component.css']
})
export class ForumsReportsComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  reports: ForumReportDetail[] = [];
  cases: ReportCaseSummary[] = [];
  filteredCases: ReportCaseSummary[] = [];
  posts: ForumPost[] = [];
  statusFilter: ReportStatusFilter = 'ALL';
  searchTerm = '';
  loading = false;
  loadError = '';
  actionFeedback = '';
  selectedCase: ReportCaseSummary | null = null;
  moderatingCaseKey: string | null = null;

  constructor(
    private forumsService: ForumsMockService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (!this.authService.hasRole('ADMIN')) {
      this.router.navigate(['/forums']);
      return;
    }

    this.reload();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  reload(): void {
    this.loading = true;
    this.loadError = '';

    forkJoin({
      reports: this.forumsService.getAllReports(),
      posts: this.forumsService.getPosts(this.authService.getCurrentUserId() ?? undefined, 'newest', 'desc')
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ reports, posts }) => {
          this.loading = false;
          this.reports = reports;
          this.posts = posts;
          this.cases = this.buildCases(reports, posts);
          this.applyFilters();

          if (this.selectedCase) {
            this.selectedCase = this.cases.find((item) => item.key === this.selectedCase?.key) ?? null;
          }
        },
        error: (err) => {
          this.loading = false;
          this.loadError = err?.error?.error || 'Could not load moderation reports right now.';
        }
      });
  }

  applyFilters(): void {
    const query = this.searchTerm.trim().toLowerCase();
    this.filteredCases = this.cases.filter((item) => {
      const matchesStatus = this.statusFilter === 'ALL' || item.status === this.statusFilter;
      if (!matchesStatus) {
        return false;
      }

      if (!query) {
        return true;
      }

      const haystack = [
        item.targetType,
        item.postTitle,
        item.targetPreview,
        item.latestReason,
        ...item.reports.map((report) => report.reason),
        ...item.reporterIds.map((id) => `reporter ${id}`)
      ]
        .join(' ')
        .toLowerCase();

      return haystack.includes(query);
    });

    if (this.selectedCase && !this.filteredCases.some((item) => item.key === this.selectedCase?.key)) {
      this.selectedCase = null;
    }
  }

  selectCase(item: ReportCaseSummary): void {
    this.selectedCase = item;
    this.actionFeedback = '';
  }

  openCaseThread(item: ReportCaseSummary): void {
    const post = this.posts.find((entry) => entry.id === item.postId);
    if (post?.groupId != null) {
      this.router.navigate(['/forums/group', post.groupId, 'post', item.postId]);
      return;
    }

    this.router.navigate(['/forums/post', item.postId]);
  }

  approveSelected(): void {
    if (!this.selectedCase) {
      return;
    }

    this.runModerationAction(this.selectedCase, 'approve');
  }

  rejectSelected(): void {
    if (!this.selectedCase) {
      return;
    }

    this.runModerationAction(this.selectedCase, 'reject');
  }

  getStatusLabel(status: ReportCaseSummary['status']): string {
    switch (status) {
      case 'PENDING':
        return 'Pending review';
      case 'APPROVED':
        return 'Resolved: approved';
      default:
        return 'Resolved: rejected';
    }
  }

  isPending(item: ReportCaseSummary | null): boolean {
    return !!item && item.status === 'PENDING';
  }

  private runModerationAction(item: ReportCaseSummary, action: 'approve' | 'reject'): void {
    const target: ForumReportCaseActionTarget = {
      targetType: item.targetType,
      targetId: item.targetId,
      postId: item.postId,
      replyId: item.replyId,
      commentId: item.commentId
    };

    this.moderatingCaseKey = item.key;
    this.actionFeedback = '';

    const request$ = action === 'approve'
      ? this.forumsService.approveReportTarget(target)
      : this.forumsService.rejectReportTarget(target);

    request$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.moderatingCaseKey = null;
          this.actionFeedback = action === 'approve'
            ? 'Reported content was approved and restored.'
            : 'Reported content was rejected and removed.';
          this.reload();
        },
        error: (err) => {
          this.moderatingCaseKey = null;
          this.actionFeedback = err?.error?.error || 'Could not complete moderation action.';
        }
      });
  }

  private buildCases(reports: ForumReportDetail[], posts: ForumPost[]): ReportCaseSummary[] {
    const grouped = new Map<string, ForumReportDetail[]>();
    reports.forEach((report) => {
      const key = `${report.targetType}-${report.targetId}`;
      const bucket = grouped.get(key) ?? [];
      bucket.push(report);
      grouped.set(key, bucket);
    });

    return Array.from(grouped.entries())
      .map(([key, bucket]) => {
        const sorted = [...bucket].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        const latest = sorted[0];
        const resolvedStatus = sorted.find((report) => report.status === 'PENDING')?.status ?? latest.status;
        const reporterIds = Array.from(new Set(sorted.map((report) => report.reporterId)));
        const content = this.resolveTargetContext(latest, posts);

        return {
          key,
          targetType: latest.targetType,
          targetId: latest.targetId,
          postId: latest.postId ?? latest.targetId,
          replyId: latest.replyId,
          commentId: latest.commentId,
          status: resolvedStatus,
          reports: sorted,
          reportCount: sorted.length,
          uniqueReporters: reporterIds.length,
          latestAt: latest.createdAt,
          latestReason: latest.reason,
          reporterIds,
          postTitle: content.postTitle,
          targetPreview: content.targetPreview
        };
      })
      .sort((a, b) => new Date(b.latestAt).getTime() - new Date(a.latestAt).getTime());
  }

  private resolveTargetContext(report: ForumReportDetail, posts: ForumPost[]): { postTitle: string; targetPreview: string } {
    const post = posts.find((item) => item.id === report.postId);
    if (!post) {
      return {
        postTitle: `Post #${report.postId ?? report.targetId}`,
        targetPreview: 'Content preview unavailable.'
      };
    }

    if (report.targetType === 'POST') {
      return {
        postTitle: post.title,
        targetPreview: post.content
      };
    }

    const reply = post.replies.find((item) => item.id === report.replyId);
    if (!reply) {
      return {
        postTitle: post.title,
        targetPreview: 'Reply preview unavailable.'
      };
    }

    if (report.targetType === 'REPLY') {
      return {
        postTitle: post.title,
        targetPreview: reply.content
      };
    }

    const comment = reply.comments.find((item) => item.id === report.commentId);
    return {
      postTitle: post.title,
      targetPreview: comment?.content ?? 'Comment preview unavailable.'
    };
  }
}
