import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from 'src/environments/environment';
import { BehaviorSubject, Observable, from } from 'rxjs';
import { map, tap, switchMap } from 'rxjs/operators';

export type BackendRole =
    | 'AGRICULTEUR'
    | 'EXPERT_AGRICOLE'
    | 'ORGANISATEUR_EVENEMENT'
    | 'TRANSPORTEUR'
    | 'VETERINAIRE'
    | 'ADMIN'
    | 'ACHETEUR'
    | 'AGENT';

export interface LoginResponse {
    token:                    string | null;
    userId:                   number | null;
    username:                 string | null;
    email:                    string;
    role:                     string | null;
    statutCompte?:            string | null;
    emailVerificationStatus?: string | null;
    profileValidationStatus?: string | null;
    nextStep?:                'LOGIN' | 'VERIFY_EMAIL' | 'SIGNUP' | 'SIGNUP_STEP2' | 'ACCESS_GRANTED' | string | null;
    verificationRequired?:    boolean;
    message:                  string;
}

export interface SignupResponse {
    userId:                   number | null;
    email:                    string | null;
    role:                     string | null;
    statutCompte:             string | null;
    emailVerificationStatus?: string | null;
    profileValidationStatus?: string | null;
    nextStep?:                'VERIFY_EMAIL' | 'SIGNUP_STEP2' | 'LOGIN' | string | null;
    message:                  string;
}

export interface FileUploadResponse {
    url:      string;
    fileName: string;
}

export interface SignupStep1Request {
    nom:        string;
    prenom:     string;
    email:      string;
    motDePasse: string;
    role:       string;
    photo?:     string | null;
    telephone?: string | null;
}

export interface SignupStep2Request {
    photo?:                string | null;
    telephone?:            string | null;
    region?:               string | null;
    diplomeExpert?:        string | null;
    documentUrl?:          string | null;
    vehicule?:             string | null;
    capacite?:             number | null;
    agence?:               string | null;
    certificatTravail?:    string | null;
    organizationLogo?:     string | null;
    cin?:                  string | null;
    adresseCabinet?:       string | null;
    presentationCarriere?: string | null;
    telephoneCabinet?:     string | null;
    nomOrganisation?:      string | null;
    description?:          string | null;
}

export interface AuthUser {
    userId:        number;
    username:      string;
    email:         string;
    role:          BackendRole;
    statutCompte?: string;
}

export interface TokenValidationResponse {
    valid:   boolean;
    userId:  number | null;
    email:   string | null;
    message: string;
}

declare var grecaptcha: any;

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private apiUrl = 'http://localhost:8089/user/api/auth';
    private readonly tokenKey = 'authToken';
    private readonly userKey = 'authUser';
    private currentUserSubject = new BehaviorSubject<AuthUser | null>(this.getUserFromStorage());
    public currentUser$ = this.currentUserSubject.asObservable();

    siteKey = environment.siteKey;

    constructor(private http: HttpClient) {}

    private getCaptchaToken(): Promise<string> {
        return new Promise((resolve, reject) => {
            if (typeof grecaptcha === 'undefined') {
                if (!environment.production) {
                    console.warn('Mode dev: simulation CAPTCHA');
                    resolve('dev-simulated-token');
                } else {
                    reject('reCAPTCHA not loaded');
                }
                return;
            }

            const token = grecaptcha.getResponse();
            if (token) {
                resolve(token);
            } else {
                reject('CAPTCHA not completed by user');
            }
        });
    }

    login(email: string, password: string, rememberSession = true): Observable<LoginResponse> {
        return from(this.getCaptchaToken()).pipe(
            switchMap(captchaToken => this.http.post<LoginResponse>(`${this.apiUrl}/login`, {
                email,
                motDePasse: password,
                captchaToken
            })),
            map(response => {
                this.persistSessionIfAllowed(response, rememberSession);
                return response;
            })
        );
    }

    signupStep1(payload: SignupStep1Request): Observable<SignupResponse> {
        return this.http.post<SignupResponse>(`${this.apiUrl}/signup/step1`, payload);
    }

    signupStep2(userId: number, payload: SignupStep2Request): Observable<SignupResponse> {
        return this.http.put<SignupResponse>(`${this.apiUrl}/signup/step2/${userId}`, payload);
    }

    verifyEmail(token: string): Observable<SignupResponse> {
        return this.http.get<SignupResponse>(`${this.apiUrl}/verify-email?token=${token}`);
    }

    uploadUserFile(file: File): Observable<FileUploadResponse> {
        const formData = new FormData();
        formData.append('file', file);
        return this.http.post<FileUploadResponse>('http://localhost:8089/user/api/user/upload', formData);
    }

    validateCurrentSession(): Observable<TokenValidationResponse> {
        return this.http.get<TokenValidationResponse>(`${this.apiUrl}/validate`).pipe(
            tap(response => {
                if (!response.valid) {
                    this.setAuthNotice(response.message || 'Your session is no longer valid.');
                    this.logout();
                }
            })
        );
    }

    logout(): void {
        this.clearSession();
        this.currentUserSubject.next(null);
    }

    setAuthNotice(message: string): void {
        sessionStorage.setItem('authNotice', message);
    }

    consumeAuthNotice(): string | null {
        const message = sessionStorage.getItem('authNotice');
        if (message) {
            sessionStorage.removeItem('authNotice');
        }
        return message;
    }

    hasActiveSession(): boolean {
        return !!this.getToken() && !!this.getCurrentUser();
    }

    isLoggedIn(): boolean {
        return !!this.getToken();
    }

    getToken(): string | null {
        const token = this.readStoredValue(this.tokenKey);
        if (token && token !== 'null' && token !== 'undefined') {
            return token;
        }
        return null;
    }

    getCurrentUser(): AuthUser | null {
        return this.currentUserSubject.value;
    }

    getCurrentRole(): BackendRole | null {
        return this.getCurrentUser()?.role ?? null;
    }

    getCurrentUserId(): number | null {
        return this.getCurrentUser()?.userId ?? null;
    }

    getAccountStatus(): string | undefined {
        return this.getCurrentUser()?.statutCompte;
    }

    isAccountApproved(): boolean {
        return this.getCurrentUser()?.statutCompte === 'APPROUVE';
    }

    hasRole(role: BackendRole): boolean {
        const user = this.getCurrentUser();
        return user ? user.role.toUpperCase() === role.toUpperCase() : false;
    }

    hasAnyRole(...roles: BackendRole[]): boolean {
        const user = this.getCurrentUser();
        return user ? roles.map(r => r.toUpperCase()).includes(user.role.toUpperCase()) : false;
    }

    getDefaultRouteForRole(role: BackendRole | null): string {
        if (!role) return '/';
        switch (role) {
            case 'ADMIN': return '/dashboard';
            case 'ACHETEUR': return '/marketplace';
            case 'AGRICULTEUR': return '/delivery';
            case 'EXPERT_AGRICOLE': return '/expert/home';
            case 'TRANSPORTEUR': return '/transporter/home';
            case 'VETERINAIRE': return '/appointments';
            case 'AGENT': return '/agent/home';
            case 'ORGANISATEUR_EVENEMENT': return '/organizer/home';
            default: return '/';
        }
    }

    forgotPasswordCheckEmail(email: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/forgot-password/email`, { email });
    }

    forgotPasswordByPhone(email: string, telephone: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/forgot-password/phone`, { email, telephone });
    }

    resetPasswordByPhone(email: string, telephone: string, code: string, newPassword: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/reset-password/phone`, { email, telephone, code, newPassword });
    }

    loginWithGoogle(credential: string): Observable<LoginResponse> {
        return this.http.post<LoginResponse>(`${this.apiUrl}/google`, { credential }).pipe(
            map(response => {
                this.persistSessionIfAllowed(response, true);
                return response;
            })
        );
    }

    completeGoogleSignup(payload: {
        credential: string;
        telephone: string;
        role: string;
    }): Observable<LoginResponse | SignupResponse> {
        return this.http.post<LoginResponse | SignupResponse>(`${this.apiUrl}/google/complete-signup`, payload).pipe(
            map((response: any) => {
                this.persistSessionIfAllowed(response, true);
                return response;
            })
        );
    }

    completeFacebookSignup(payload: {
        accessToken: string;
        telephone: string;
        role: string;
    }): Observable<LoginResponse | SignupResponse> {
        return this.http.post<LoginResponse | SignupResponse>(`${this.apiUrl}/facebook/complete-signup`, payload).pipe(
            map((response: any) => {
                this.persistSessionIfAllowed(response, true);
                return response;
            })
        );
    }

    loginWithFacebook(accessToken: string): Observable<LoginResponse> {
        return this.http.post<LoginResponse>(`${this.apiUrl}/facebook`, { accessToken }).pipe(
            map(response => {
                this.persistSessionIfAllowed(response, true);
                return response;
            })
        );
    }

    private persistSessionIfAllowed(response: LoginResponse | SignupResponse | any, rememberSession: boolean): void {
        if (!this.shouldStoreSession(response)) {
            return;
        }

        const user: AuthUser = {
            userId: response.userId as number,
            username: response.username || response.email,
            email: response.email,
            role: response.role as BackendRole,
            statutCompte: response.statutCompte || undefined
        };

        this.storeSession(response.token as string, user, rememberSession);
        this.currentUserSubject.next(user);
    }

    private shouldStoreSession(response: LoginResponse | any): response is LoginResponse {
        return !!response
            && !!response.token
            && response.userId !== null
            && !!response.role
            && response.nextStep === 'ACCESS_GRANTED';
    }

    private storeSession(token: string, user: AuthUser, rememberSession: boolean): void {
        this.clearSession();
        const storage = rememberSession ? localStorage : sessionStorage;
        storage.setItem(this.tokenKey, token);
        storage.setItem(this.userKey, JSON.stringify(user));
    }

    private readStoredValue(key: string): string | null {
        return localStorage.getItem(key) ?? sessionStorage.getItem(key);
    }

    private getUserFromStorage(): AuthUser | null {
        const user = this.readStoredValue(this.userKey);
        return user ? JSON.parse(user) as AuthUser : null;
    }

    private clearSession(): void {
        localStorage.removeItem(this.tokenKey);
        localStorage.removeItem(this.userKey);
        sessionStorage.removeItem(this.tokenKey);
        sessionStorage.removeItem(this.userKey);
    }
}