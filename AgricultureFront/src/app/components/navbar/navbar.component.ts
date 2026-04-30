import { Component, EventEmitter, HostListener, OnDestroy, OnInit, Output } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { AuthService, BackendRole } from 'src/app/services/auth/auth.service';

declare global {
    interface Window {
        google?: {
            translate?: {
                TranslateElement: {
                    new (options: Record<string, unknown>, elementId: string): unknown;
                    InlineLayout: {
                        SIMPLE: unknown;
                    };
                };
            };
        };
        googleTranslateElementInit?: () => void;
    }
}

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
    private googleBannerObserver: MutationObserver | null = null;

    isScrolled = false;
    isMobileMenuOpen = false;
    isLoggedIn = false;
    isHomePage = true;
    activeLink = '/';
    moreDropdownOpen = false;
    activeSubmenu: NavDropdownLink['submenu'] | null = null;
    isLanguageMenuOpen = false;
    selectedLanguage: 'en' | 'fr' | 'ar' = 'en';

    readonly languageOptions: Array<{ code: 'en' | 'fr' | 'ar'; label: string }> = [
        { code: 'en', label: 'English' },
        { code: 'fr', label: 'French' },
        { code: 'ar', label: 'Arabic' }
    ];

    private isTranslateReady = false;
    private readonly languageStorageKey = 'preferredLanguage';
    private readonly languageReloadGuardKey = 'translationReloadLang';

    navLinks: NavLink[] = [
        { label: 'Home', route: '/' },

        { label: 'Marketplace', route: '/marketplace' },
        { label: 'Forum', route: '/forums' },
         {
        label: 'Events',
        route: '/events/listEvents',
        roles: ['AGRICULTEUR', 'EXPERT_AGRICOLE']
    },
    {
        label: 'Events',
        route: 'events/organizer/events',
        roles: ['ORGANISATEUR_EVENEMENT']
    },
        {
            label: 'Deliveries',
            route: '/delivery',
            roles: ['ADMIN','AGRICULTEUR','TRANSPORTEUR']
        },
     {label : 'profile', route: '/profile/edit', roles: ['AGRICULTEUR', 'EXPERT_AGRICOLE', 'ORGANISATEUR_EVENEMENT', 'AGENT', 'VETERINAIRE']},
         { label: 'Reclamations', route: '/claims', roles: ['AGRICULTEUR', 'EXPERT_AGRICOLE', 'ORGANISATEUR_EVENEMENT', 'AGENT', 'VETERINAIRE'] },
    ];

    dropdownLinks: NavDropdownLink[] = [
      
        {
            label: 'Terrain',
            icon: 'fas fa-leaf',
            route: '/farm/list',
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
            route: '/training',
            roles: ['AGRICULTEUR','EXPERT_AGRICOLE']
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
        },
       {
            label: 'Calendar',
            icon: 'fas fa-calendar',
            route: '/farm/calendar',
            roles: ['AGRICULTEUR']
        },
         {
            label: 'Veterinaire & IA',
            icon: 'fas fa-calendar-check',
            route: '/appointments',
            roles: ['AGRICULTEUR']
        },
        {
            label: 'Inventory',
            icon: 'fas fa-calendar-check',
            route: '/inventory',
            roles: ['AGRICULTEUR']
        },
         {
            label: 'Appoinyments & Shop',
            icon: 'fas fa-calendar-check',
            route: '/appointments',
            roles: [ 'VETERINAIRE']
        },{
        label: 'Disease predictor ',
    icon: 'fas fa-microscope',
    route: '/disease-predictor',
    roles: [ 'AGRICULTEUR','EXPERT_AGRICOLE']
},
           

    ];

    private readonly protectedRoutes = [
        '/inventory',
        '/claims',
        '/help-request',
        '/farm',
        '/loans',
        '/expert/assistance-requests',
        '/profile'
    ];

    constructor(
        private router: Router,
        private authService: AuthService
    ) {}

    ngOnInit(): void {
        this.isLoggedIn = this.authService.hasActiveSession();
        this.isHomePage = this.router.url === '/';
        this.activeLink = this.router.url.split('?')[0];
        this.syncProfileAccess();
        this.initializeLanguage();
        this.initGoogleTranslate();
        this.startGoogleBannerObserver();

        this.authService.currentUser$
            .pipe(takeUntil(this.destroy$))
            .subscribe(user => {
                this.isLoggedIn = !!user && this.authService.hasActiveSession();
                 this.syncProfileAccess();
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
                this.syncLanguageFromStorageAndCookie();
                this.applyGoogleTranslation(this.selectedLanguage);
                this.moreDropdownOpen = false;
                this.isMobileMenuOpen = false;
                this.activeSubmenu = null;
                 this.syncProfileAccess();
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
        if (!target.closest('.language-switcher')) {
            this.isLanguageMenuOpen = false;
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

    toggleLanguageMenu(event: MouseEvent): void {
        event.stopPropagation();
        this.isLanguageMenuOpen = !this.isLanguageMenuOpen;
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
        this.googleBannerObserver?.disconnect();
        this.destroy$.next();
        this.destroy$.complete();
    }

    onLanguageChange(language: string): void {
        if (!this.isValidLanguage(language)) {
            return;
        }

        const previousLanguage = this.selectedLanguage;
        this.isLanguageMenuOpen = false;

        this.selectedLanguage = language;
        localStorage.setItem(this.languageStorageKey, language);
        this.setTranslationCookie(language);
        this.updateDocumentDirection(language);
        this.applyGoogleTranslation(language);

        // Force one refresh only when language actually changes.
        if (previousLanguage !== language) {
            const lastReloadLanguage = sessionStorage.getItem(this.languageReloadGuardKey);
            if (lastReloadLanguage !== language) {
                sessionStorage.setItem(this.languageReloadGuardKey, language);
                window.location.reload();
                return;
            }
        }

        sessionStorage.removeItem(this.languageReloadGuardKey);
    }

    private canAccess(roles?: BackendRole[]): boolean {
        if (!roles || roles.length === 0) return true;
        return this.authService.hasAnyRole(...roles);
    }

    private initializeLanguage(): void {
        this.syncLanguageFromStorageAndCookie();
    }

    private initGoogleTranslate(): void {
        window.googleTranslateElementInit = () => {
            const translate = window.google?.translate?.TranslateElement;
            if (!translate) {
                return;
            }

            new translate(
                {
                    pageLanguage: 'en',
                    includedLanguages: 'en,fr,ar',
                    autoDisplay: false,
                    layout: translate.InlineLayout.SIMPLE
                },
                'google_translate_element'
            );

            this.isTranslateReady = true;
            this.forceHideGoogleBanner();
            this.applyGoogleTranslation(this.selectedLanguage);
        };

        const existingScript = document.getElementById('google-translate-script');
        if (existingScript) {
            if (window.google?.translate?.TranslateElement) {
                window.googleTranslateElementInit?.();
            }
            return;
        }

        const script = document.createElement('script');
        script.id = 'google-translate-script';
        script.src = 'https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';
        script.async = true;
        document.body.appendChild(script);

        this.forceHideGoogleBanner();
    }

    private applyGoogleTranslation(language: string, attempt = 0): void {
        if (!this.isTranslateReady) {
            if (attempt < 10) {
                window.setTimeout(() => this.applyGoogleTranslation(language, attempt + 1), 250);
            }
            return;
        }

        const combo = document.querySelector('.goog-te-combo') as HTMLSelectElement | null;
        if (!combo) {
            if (attempt < 10) {
                window.setTimeout(() => this.applyGoogleTranslation(language, attempt + 1), 250);
            }
            return;
        }

        const targetValue = language;
        if (combo.value === targetValue) {
            return;
        }

        combo.value = targetValue;

        const event = document.createEvent('HTMLEvents');
        event.initEvent('change', true, true);
        combo.dispatchEvent(event);

        combo.dispatchEvent(new Event('change', { bubbles: true }));
    }

    private setTranslationCookie(language: 'en' | 'fr' | 'ar'): void {
        const cookieValue = `/en/${language}`;

        // Clear potential duplicates first, then set one canonical cookie.
        document.cookie = 'googtrans=;path=/;expires=Thu, 01 Jan 1970 00:00:00 GMT';
        document.cookie = `googtrans=;path=/;domain=${window.location.hostname};expires=Thu, 01 Jan 1970 00:00:00 GMT`;
        document.cookie = `googtrans=${cookieValue};path=/`;
    }

    private updateDocumentDirection(language: 'en' | 'fr' | 'ar'): void {
        document.documentElement.dir = language === 'ar' ? 'rtl' : 'ltr';
    }

    private syncLanguageFromStorageAndCookie(): void {
        const widgetLanguage = this.getLanguageFromTranslateWidget();
        const cookieLanguage = this.getLanguageFromTranslateCookie();
        const storedLanguage = localStorage.getItem(this.languageStorageKey);

        let nextLanguage: 'en' | 'fr' | 'ar' = 'en';

        if (this.isValidLanguage(widgetLanguage)) {
            nextLanguage = widgetLanguage;
        } else if (this.isValidLanguage(cookieLanguage)) {
            nextLanguage = cookieLanguage;
        } else if (this.isValidLanguage(storedLanguage)) {
            nextLanguage = storedLanguage;
        }

        this.selectedLanguage = nextLanguage;
        localStorage.setItem(this.languageStorageKey, nextLanguage);
        this.setTranslationCookie(nextLanguage);
        this.updateDocumentDirection(nextLanguage);
    }

    private getLanguageFromTranslateCookie(): 'en' | 'fr' | 'ar' | null {
        const rawCookies = document.cookie
            .split(';')
            .map(chunk => chunk.trim())
            .filter(chunk => chunk.startsWith('googtrans='));

        if (rawCookies.length === 0) {
            return null;
        }

        let parsedLanguage: 'en' | 'fr' | 'ar' | null = null;

        rawCookies.forEach(cookie => {
            const rawValue = decodeURIComponent(cookie.split('=')[1] ?? '');
            const parts = rawValue.split('/').filter(Boolean);
            const language = parts[parts.length - 1] ?? null;

            if (this.isValidLanguage(language)) {
                parsedLanguage = language;
            }
        });

        return parsedLanguage;
    }

    private getLanguageFromTranslateWidget(): 'en' | 'fr' | 'ar' | null {
        const combo = document.querySelector('.goog-te-combo') as HTMLSelectElement | null;
        if (!combo) {
            return null;
        }

        const value = (combo.value || '').trim().toLowerCase();
        return this.isValidLanguage(value) ? value : null;
    }

    private isValidLanguage(language: string | null): language is 'en' | 'fr' | 'ar' {
        return language === 'en' || language === 'fr' || language === 'ar';
    }

    private startGoogleBannerObserver(): void {
        this.forceHideGoogleBanner();

        this.googleBannerObserver = new MutationObserver(() => {
            this.forceHideGoogleBanner();
        });

        this.googleBannerObserver.observe(document.documentElement, {
            childList: true,
            subtree: true
        });
    }

    private forceHideGoogleBanner(): void {
        const selectors = [
            'iframe.goog-te-banner-frame',
            '.goog-te-banner-frame.skiptranslate',
            '.goog-te-banner-frame',
            'iframe.VIpgJd-ZVi9od-ORHb-OEVmcd',
            '.VIpgJd-ZVi9od-ORHb-OEVmcd',
            '.VIpgJd-ZVi9od-aZ2wEe-wOHMyf',
            '#goog-gt-tt'
        ];

        selectors.forEach(selector => {
            document.querySelectorAll(selector).forEach(node => {
                const element = node as HTMLElement;
                element.style.setProperty('display', 'none', 'important');
                element.style.setProperty('visibility', 'hidden', 'important');
                element.style.setProperty('height', '0', 'important');
                element.style.setProperty('min-height', '0', 'important');
                element.style.setProperty('max-height', '0', 'important');
                element.style.setProperty('opacity', '0', 'important');
                element.style.setProperty('pointer-events', 'none', 'important');
            });
        });

        document.body.style.setProperty('top', '0px', 'important');
        document.documentElement.style.setProperty('top', '0px', 'important');
        document.body.style.setProperty('position', 'static', 'important');
    }

     private syncProfileAccess(): void {
        const role = this.authService.getCurrentRole();
       
    }

}
