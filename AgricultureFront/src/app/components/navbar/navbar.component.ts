import { Component, EventEmitter, HostListener, OnDestroy, OnInit, Output } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { AuthService, BackendRole } from 'src/app/services/auth/auth.service';

interface NavLink {
    label: string;
    route: string;
    queryParams?: Record<string, string>;
    roles?: BackendRole[];
}

interface NavDropdownLink extends NavLink {
    icon: string;
    hasSubmenu?: boolean;
    submenu?: Array<{ label: string; icon: string; route: string; queryParams?: Record<string, string> }>;
}

@Component({
    selector: 'app-navbar',
    standalone: false,
    templateUrl: './navbar.component.html',
    styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit, OnDestroy {
    @Output() onAuthOpen = new EventEmitter<'signin' | 'signup'>();

    private destroy$ = new Subject<void>();
    private hideSubmenuTimer: any;

    isScrolled = false;
    isMobileMenuOpen = false;
    isLoggedIn = false;
    isHomePage = true;
    activeLink = '/';
    moreDropdownOpen = false;
    activeSubmenu: NavDropdownLink['submenu'] | null = null;

    navLinks: NavLink[] = [
        { label: 'Home', route: '/' },
        { label: 'Marketplace', route: '/marketplace' },
        { label: 'Forum', route: '/forums' },
        {
            label: 'Events',
            route: '/events/listEvents',
            roles: ['AGRICULTEUR', 'EXPERT_AGRICOLE', 'ORGANISATEUR_EVENEMENT']
        }
    ];

    dropdownLinks: NavDropdownLink[] = [
        {
            label: 'Inventory',
            icon: 'fas fa-boxes',
            route: '/inventory',
            roles: ['AGRICULTEUR']
        },
        {
            label: 'List Animal',
            icon: 'fas fa-paw',
            route: '/inventory',
            queryParams: { tab: 'animals' },
            roles: ['AGRICULTEUR']
        },
        {
            label: 'Terrain',
            icon: 'fas fa-leaf',
            route: '/farm',
            roles: ['AGRICULTEUR']
        },
        {
            label: 'Help Request',
            icon: 'fas fa-hands-helping',
            route: '/help-request',
            roles: ['AGRICULTEUR']
        },
        {
            label: 'Trainings',
            icon: 'fas fa-graduation-cap',
            route: '/formations',
            roles: ['AGRICULTEUR']
        },
        {
            label: 'Loans',
            icon: 'fas fa-hand-holding-usd',
            route: '/loans',
            roles: ['AGRICULTEUR']
        },
        {
            label: 'Agent Loans',
            icon: 'fas fa-briefcase',
            route: '/loans/agent/services',
            roles: ['AGENT']
        },
        {
            label: 'Expert Request',
            icon: 'fas fa-clipboard-list',
            route: '/expert/assistance-requests',
            roles: ['EXPERT_AGRICOLE']
        }
    ];

    private readonly protectedRoutes = [
        '/inventory',
        '/claims',
        '/help-request',
        '/farm',
        '/loans',
        '/expert/assistance-requests'
    ];

    constructor(
        private router: Router,
        private authService: AuthService
    ) {}

    ngOnInit(): void {
        this.isLoggedIn = this.authService.hasActiveSession();
        this.isHomePage = this.router.url === '/';
        this.activeLink = this.router.url.split('?')[0];

        this.authService.currentUser$
            .pipe(takeUntil(this.destroy$))
            .subscribe(user => {
                this.isLoggedIn = !!user && this.authService.hasActiveSession();
            });

        this.router.events
            .pipe(
                filter((e): e is NavigationEnd => e instanceof NavigationEnd),
                takeUntil(this.destroy$)
            )
            .subscribe((e: NavigationEnd) => {
                this.isLoggedIn = this.authService.hasActiveSession();
                this.isHomePage = e.urlAfterRedirects === '/';
                this.activeLink = e.urlAfterRedirects.split('?')[0];
                this.moreDropdownOpen = false;
                this.isMobileMenuOpen = false;
                this.activeSubmenu = null;
            });
    }

    get visibleNavLinks(): NavLink[] {
        return this.navLinks.filter(link => this.canAccess(link.roles));
    }

    get visibleDropdownLinks(): NavDropdownLink[] {
        return this.dropdownLinks.filter(link => this.canAccess(link.roles));
    }

    @HostListener('window:scroll', [])
    onScroll(): void {
        this.isScrolled = window.scrollY > 80;
    }

    @HostListener('document:click', ['$event'])
    onDocumentClick(event: MouseEvent): void {
        const target = event.target as HTMLElement;
        if (!target.closest('.dropdown-wrap')) {
            this.moreDropdownOpen = false;
        }
    }

    navigate(route: string): void {
        this.navigateTo({ route });
    }

    navigateTo(link: Pick<NavLink, 'route' | 'queryParams'>): void {
        const needsAuth = this.protectedRoutes.some(protectedRoute => link.route.startsWith(protectedRoute));

        if (needsAuth && !this.authService.hasActiveSession()) {
            localStorage.setItem('authMode', 'signin');
            localStorage.setItem('postLoginRoute', link.route);
            this.router.navigate(['/auth']);
        } else {
            this.router.navigate(
                [link.route],
                link.queryParams ? { queryParams: link.queryParams } : undefined
            );
        }

        this.isMobileMenuOpen = false;
        this.moreDropdownOpen = false;
        this.activeSubmenu = null;
    }

    toggleDropdown(event: MouseEvent): void {
        event.stopPropagation();
        this.moreDropdownOpen = !this.moreDropdownOpen;
        if (!this.moreDropdownOpen) {
            this.activeSubmenu = null;
        }
    }

    showSubmenu(dropdownItem: NavDropdownLink): void {
        clearTimeout(this.hideSubmenuTimer);
        if (dropdownItem.hasSubmenu) {
            this.activeSubmenu = dropdownItem.submenu ?? null;
        }
    }

    keepSubmenu(): void {
        clearTimeout(this.hideSubmenuTimer);
    }

    hideSubmenu(): void {
        this.hideSubmenuTimer = setTimeout(() => {
            this.activeSubmenu = null;
        }, 150);
    }

    toggleOrNavigate(dropdownItem: NavDropdownLink): void {
        if (dropdownItem.hasSubmenu) {
            clearTimeout(this.hideSubmenuTimer);
            this.activeSubmenu = this.activeSubmenu === dropdownItem.submenu
                ? null
                : (dropdownItem.submenu ?? null);
            return;
        }

        this.navigateTo(dropdownItem);
    }

    isDropdownActive(): boolean {
        return this.visibleDropdownLinks.some(link => this.isDropdownLinkActive(link));
    }

    isDropdownLinkActive(link: NavDropdownLink): boolean {
        if (this.activeLink === link.route) return true;
        if (link.hasSubmenu) return this.activeLink.startsWith(`${link.route}/`);
        return false;
    }

    toggleMobile(): void {
        this.isMobileMenuOpen = !this.isMobileMenuOpen;
    }

    signIn(): void {
        localStorage.setItem('authMode', 'signin');
        if (this.onAuthOpen.observers.length > 0) {
            this.onAuthOpen.emit('signin');
            return;
        }
        this.router.navigate(['/auth'], { queryParams: { returnUrl: this.router.url } });
    }

    signUp(): void {
        localStorage.setItem('authMode', 'signup');
        if (this.onAuthOpen.observers.length > 0) {
            this.onAuthOpen.emit('signup');
            return;
        }
        this.router.navigate(['/auth'], { queryParams: { returnUrl: this.router.url } });
    }

    logout(): void {
        this.authService.logout();
        this.isLoggedIn = false;
        this.isMobileMenuOpen = false;
        this.moreDropdownOpen = false;
        this.activeSubmenu = null;
        this.router.navigate(['/']);
    }

    ngOnDestroy(): void {
        clearTimeout(this.hideSubmenuTimer);
        this.destroy$.next();
        this.destroy$.complete();
    }

    private canAccess(roles?: BackendRole[]): boolean {
        if (!roles || roles.length === 0) return true;
        return this.authService.hasAnyRole(...roles);
    }
}
