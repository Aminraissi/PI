import { Component, OnInit, OnDestroy, AfterViewInit } from '@angular/core';
import { FormGroup, FormControl, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { AuthService, BackendRole, SignupStep1Request, SignupResponse } from '../../services/auth/auth.service';
import { Router, ActivatedRoute } from '@angular/router';

declare var grecaptcha: any;
declare const google: any;
declare const FB: any;

@Component({
  selector: 'app-auth',
  standalone: false,
  templateUrl: './auth.component.html',
  styleUrls: ['./auth.component.css']
})
export class AuthComponent implements OnInit, OnDestroy, AfterViewInit {

  mode: 'signin' | 'signup' | 'verify' | 'forgot' | 'forgotPhone' | 'reset' = 'signin';
  showSignInPass = false;
  showSignUpPass = false;
  previewUrl: string | null = null;
  photoUploadUrl: string | null = null;
  loginError: string | null = null;
  isLoading = false;
  verificationEmail: string | null = null;
  verificationMessage: string | null = null;
  verificationLoading = false;
  verificationError: string | null = null;

  signInForm!: FormGroup;
  signUpForm!: FormGroup;
  forgotEmailForm!: FormGroup;
  forgotPhoneForm!: FormGroup;
  resetPasswordForm!: FormGroup;

  resetEmail: string | null = null;
  resetPhone: string | null = null;
  resetMessage: string | null = null;
  resetError: string | null = null;
  resetLoading = false;

  siteKey = '6LdVg8ssAAAAAGykd7vnzD3RveWYa55rWK7b2oy2';
  private captchaWidgetId: number | null = null;

  private readonly googleClientId = '270359567342-1evqop862k1bolr40f5qsn47b4d9q4rl.apps.googleusercontent.com';
  googleSignupMode = false;
  googleCredential: string | null = null;
  googleProfile: any = null;
  private googleInitialized = false;

  private readonly facebookAppId = '1855481011803635';
  private facebookInitialized = false;
  facebookSignupMode = false;
  facebookAccessToken: string | null = null;
  facebookProfile: any = null;

  get socialSignupMode(): boolean {
    return this.googleSignupMode || this.facebookSignupMode;
  }

  rolesWithExtra = [
    'Farmer', 'Transporter', 'AgriculturalExpert',
    'Agent', 'Veterinarian', 'EventOrganizer'
  ];

  get selectedRole(): string {
    return this.signUpForm?.get('role')?.value || '';
  }

  get needsExtraStep(): boolean {
    return this.rolesWithExtra.includes(this.selectedRole);
  }

  get submitLabel(): string {
    return this.needsExtraStep ? 'Next' : 'Create Account';
  }

  get isVerificationMode(): boolean {
    return this.mode === 'verify';
  }

  constructor(
      private authService: AuthService,
      private router: Router,
      private route: ActivatedRoute
  ) { }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      if (token) {
        this.verifyEmailToken(token);
      }
    });

    const savedMode = localStorage.getItem('authMode');
    if (
        savedMode === 'signin' ||
        savedMode === 'signup' ||
        savedMode === 'verify' ||
        savedMode === 'forgot' ||
        savedMode === 'forgotPhone' ||
        savedMode === 'reset'
    ) {
      this.mode = savedMode as typeof this.mode;
    }

    const pendingVerificationEmail = localStorage.getItem('pendingVerificationEmail');
    const pendingVerificationMessage = localStorage.getItem('pendingVerificationMessage');
    if (this.mode === 'verify') {
      this.verificationEmail = pendingVerificationEmail;
      this.verificationMessage = pendingVerificationMessage || 'Please verify your email to continue.';
    }

    const rememberedEmail = localStorage.getItem('rememberedEmail');
    const authNotice = this.authService.consumeAuthNotice();
    if (authNotice) {
      this.loginError = authNotice;
    }

    this.signInForm = new FormGroup({
      email: new FormControl(rememberedEmail || '', [Validators.required, Validators.email]),
      password: new FormControl('', [Validators.required, Validators.minLength(8)]),
      remember: new FormControl(rememberedEmail !== null)
    });

    this.signUpForm = new FormGroup({
      firstName: new FormControl('', [Validators.required, Validators.pattern(/^[a-zA-Z]+$/)]),
      lastName: new FormControl('', [Validators.required, Validators.pattern(/^[a-zA-Z]+$/)]),
      email: new FormControl('', [Validators.required, Validators.email]),
      phone: new FormControl('', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]),
      password: new FormControl('', [Validators.required, Validators.minLength(8)]),
      photo: new FormControl(null, [Validators.required, this.imageValidator]),
      role: new FormControl('', Validators.required)
    });

    this.forgotEmailForm = new FormGroup({
      email: new FormControl('', [Validators.required, Validators.email])
    });

    this.forgotPhoneForm = new FormGroup({
      phone: new FormControl('', [Validators.required, Validators.pattern(/^[0-9]{8}$/)])
    });

    this.resetPasswordForm = new FormGroup({
      code: new FormControl('', [Validators.required, Validators.pattern(/^[0-9]{6}$/)]),
      newPassword: new FormControl('', [Validators.required, Validators.minLength(8)]),
      confirmPassword: new FormControl('', [Validators.required])
    }, { validators: this.passwordsMatchValidator });

    this.forgotEmailForm.get('email')?.valueChanges.subscribe(() => { this.resetError = null; });
    this.forgotPhoneForm.get('phone')?.valueChanges.subscribe(() => { this.resetError = null; });
    this.resetPasswordForm.valueChanges.subscribe(() => { this.resetError = null; });

    if (this.authService.hasActiveSession()) {
      const redirectRoute = this.authService.getDefaultRouteForRole(this.authService.getCurrentRole());
      this.router.navigate([redirectRoute]);
    }

    this.waitForRecaptcha();
  }

  ngAfterViewInit(): void {
    this.initGoogleButton();
    this.initFacebookSdk();
  }

  ngOnDestroy(): void {
    this.destroyCaptcha();
  }

  private waitForRecaptcha(): void {
    const checkInterval = setInterval(() => {
      if (typeof grecaptcha !== 'undefined' && this.mode === 'signin') {
        clearInterval(checkInterval);
        this.renderCaptcha();
      }
    }, 500);
    setTimeout(() => clearInterval(checkInterval), 10000);
  }

  private renderCaptcha(): void {
    const container = document.querySelector('.g-recaptcha');
    if (container && !this.captchaWidgetId && typeof grecaptcha !== 'undefined') {
      this.captchaWidgetId = grecaptcha.render(container, {
        sitekey: this.siteKey,
        callback: () => {
          console.log('✅ CAPTCHA résolu');
          this.loginError = null;
        },
        'expired-callback': () => {
          console.log('⚠️ CAPTCHA expiré');
          this.loginError = 'La vérification a expiré, veuillez recommencer';
        }
      });
    }
  }

  private destroyCaptcha(): void {
    if (this.captchaWidgetId !== null && typeof grecaptcha !== 'undefined') {
      try {
        grecaptcha.reset(this.captchaWidgetId);
        this.captchaWidgetId = null;
      } catch (e) {
        console.warn('Erreur destruction CAPTCHA:', e);
      }
    }
  }

  resetCaptcha(): void {
    if (this.captchaWidgetId !== null && typeof grecaptcha !== 'undefined') {
      grecaptcha.reset(this.captchaWidgetId);
    }
  }

  switchTo(m: 'signin' | 'signup'): void {
    this.mode = m;
    this.loginError = null;
    this.resetError = null;
    localStorage.setItem('authMode', m);

    if (m === 'signin') {
      this.clearSocialSignupState();
      this.restoreNormalSignupValidators();

      setTimeout(() => {
        if (typeof grecaptcha !== 'undefined' && !this.captchaWidgetId) {
          this.renderCaptcha();
        } else if (this.captchaWidgetId) {
          this.resetCaptcha();
        }
      }, 100);
    } else {
      this.destroyCaptcha();
    }

    setTimeout(() => { this.initGoogleButton(); }, 0);
  }

  togglePass(field: 'signin' | 'signup'): void {
    if (field === 'signin') this.showSignInPass = !this.showSignInPass;
    else this.showSignUpPass = !this.showSignUpPass;
  }

  siInvalid(field: string): boolean {
    if (!this.signInForm) return false;
    const c = this.signInForm.get(field);
    return !!(c && c.invalid && c.touched);
  }

  suInvalid(field: string): boolean {
    if (!this.signUpForm) return false;
    const c = this.signUpForm.get(field);
    return !!(c && c.invalid && c.touched);
  }

  feInvalid(field: string): boolean {
    const c = this.forgotEmailForm?.get(field);
    return !!(c && c.invalid && c.touched);
  }

  fpInvalid(field: string): boolean {
    const c = this.forgotPhoneForm?.get(field);
    return !!(c && c.invalid && c.touched);
  }

  rpInvalid(field: string): boolean {
    const c = this.resetPasswordForm?.get(field);
    return !!(c && c.invalid && c.touched);
  }

  submitSignIn(): void {
    if (this.signInForm.invalid) {
      this.signInForm.markAllAsTouched();
      return;
    }

    let captchaToken = '';
    if (typeof grecaptcha !== 'undefined') {
      if (this.captchaWidgetId !== null) {
        captchaToken = grecaptcha.getResponse(this.captchaWidgetId);
      } else {
        captchaToken = grecaptcha.getResponse();
      }
      if (!captchaToken || captchaToken.length === 0) {
        this.loginError = 'Veuillez cocher "Je ne suis pas un robot"';
        return;
      }
    } else {
      captchaToken = 'dev-simulated-token';
    }

    this.isLoading = true;
    this.loginError = null;

    const { email, remember, password } = this.signInForm.value;

    this.authService.login(email, password, remember).subscribe({
      next: (response) => {
        if (response.token) {
          if (remember) {
            localStorage.setItem('rememberedEmail', email);
          } else {
            localStorage.removeItem('rememberedEmail');
          }
          const redirectRoute = this.authService.getDefaultRouteForRole(
              response.role ? response.role as BackendRole : null
          );
          this.router.navigate([redirectRoute]);
        } else if (response.verificationRequired || response.nextStep === 'VERIFY_EMAIL') {
          this.enterVerificationMode(response.email, response.message || 'Check your email to verify your account.');
        } else {
          this.loginError = response.message || 'Login failed';
          this.resetCaptcha();
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.loginError = err.error?.message || 'An error occurred during login';
        this.isLoading = false;
        this.resetCaptcha();
      }
    });
  }

  submitSignUp(): void {
    if (this.signUpForm.invalid) {
      this.signUpForm.markAllAsTouched();
      return;
    }

    if (this.googleSignupMode) {
      this.submitGoogleSignupCompletion();
      return;
    }

    if (this.facebookSignupMode) {
      this.submitFacebookSignupCompletion();
      return;
    }

    this.isLoading = true;
    this.loginError = null;

    const payload: SignupStep1Request = {
      nom: this.signUpForm.value.firstName ?? '',
      prenom: this.signUpForm.value.lastName ?? '',
      email: this.signUpForm.value.email ?? '',
      motDePasse: this.signUpForm.value.password ?? '',
      role: this.selectedRole,
      photo: this.photoUploadUrl,
      telephone: this.signUpForm.value.phone ?? ''
    };

    this.authService.signupStep1(payload).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (response.nextStep === 'SIGNUP_STEP2' && response.userId != null) {
          localStorage.setItem('signupBase', JSON.stringify({
            ...this.signUpForm.value,
            photo: this.photoUploadUrl
          }));
          localStorage.setItem('signupRole', this.selectedRole);
          localStorage.setItem('signupUserId', String(response.userId));
          localStorage.setItem('signupEmail', response.email || payload.email);
          localStorage.setItem('signupMessage', response.message || 'Complete your profile.');
          this.router.navigate(['/register-extra']);
          return;
        }

        if (response.nextStep === 'VERIFY_EMAIL') {
          this.enterVerificationMode(response.email, response.message || 'Please verify your email to continue.');
          return;
        }

        this.enterVerificationMode(response.email, response.message || 'Please verify your email to continue.');
      },
      error: (err) => {
        this.isLoading = false;
        this.loginError = err.error?.message || 'Could not create your account';
      }
    });
  }

  resendVerification(): void {
    this.verificationMessage = 'Please check your email and click the verification link.';
  }

  verifyEmailToken(token: string): void {
    this.verificationLoading = true;
    this.verificationError = null;

    this.authService.verifyEmail(token).subscribe({
      next: (res) => {
        this.verificationLoading = false;

        if (!res.userId) {
          this.verificationError = res.message;
          this.mode = 'verify';
          return;
        }

        this.verificationMessage = res.message;

        if (res.nextStep === 'SIGNUP_STEP2') {
          this.router.navigate(['/register-extra']);
          return;
        }

        this.mode = 'signin';
        this.router.navigate(['/auth']);
      },
      error: () => {
        this.verificationLoading = false;
        this.verificationError = 'Invalid verification link';
      }
    });
  }

  switchToForgot(): void {
    this.mode = 'forgot';
    this.resetEmail = null;
    this.resetPhone = null;
    this.resetError = null;
    this.resetMessage = null;
  }

  backToSignIn(): void {
    this.mode = 'signin';
    this.resetError = null;
    this.resetMessage = null;
  }

  submitForgotEmail(): void {
    if (this.forgotEmailForm.invalid) {
      this.forgotEmailForm.markAllAsTouched();
      return;
    }

    this.resetLoading = true;
    this.resetError = null;
    this.resetMessage = null;

    const email = this.forgotEmailForm.value.email;

    this.authService.forgotPasswordCheckEmail(email).subscribe({
      next: (response) => {
        this.resetLoading = false;
        this.resetEmail = email;
        this.resetMessage = response.message || 'Email found. Please confirm your phone number.';
        this.mode = 'forgotPhone';
      },
      error: (err) => {
        this.resetLoading = false;
        this.resetError = err.error?.message || 'We couldn\'t find an account with this email.';
      }
    });
  }

  submitForgotPhone(): void {
    if (this.forgotPhoneForm.invalid) {
      this.forgotPhoneForm.markAllAsTouched();
      return;
    }

    if (!this.resetEmail) {
      this.resetError = 'Missing email. Please start again.';
      this.mode = 'forgot';
      return;
    }

    this.resetLoading = true;
    this.resetError = null;
    this.resetMessage = null;

    const phone = this.forgotPhoneForm.value.phone;

    this.authService.forgotPasswordByPhone(this.resetEmail, phone).subscribe({
      next: (response) => {
        this.resetLoading = false;
        this.resetPhone = phone;
        this.resetMessage = response.message || 'Reset code sent to your phone.';
        this.mode = 'reset';
      },
      error: (err) => {
        this.resetLoading = false;
        this.resetError = err.error?.message || 'This phone number is not linked to the email you entered.';
      }
    });
  }

  submitResetPassword(): void {
    if (this.resetPasswordForm.invalid) {
      this.resetPasswordForm.markAllAsTouched();
      return;
    }

    if (!this.resetEmail || !this.resetPhone) {
      this.resetError = 'Missing email or phone. Please start again.';
      this.mode = 'forgot';
      return;
    }

    this.resetLoading = true;
    this.resetError = null;
    this.resetMessage = null;

    this.authService.resetPasswordByPhone(
        this.resetEmail,
        this.resetPhone,
        this.resetPasswordForm.value.code,
        this.resetPasswordForm.value.newPassword
    ).subscribe({
      next: (response) => {
        this.resetLoading = false;
        this.resetMessage = response.message || 'Password reset successfully.';
        this.resetPasswordForm.reset();
        this.forgotEmailForm.reset();
        this.forgotPhoneForm.reset();
        this.mode = 'signin';
        this.loginError = 'Password reset successfully. You can now sign in.';
      },
      error: (err) => {
        this.resetLoading = false;
        this.resetError = err.error?.message || 'Password reset failed.';
      }
    });
  }

  passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
    const newPassword = control.get('newPassword')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;
    if (!newPassword || !confirmPassword) return null;
    return newPassword === confirmPassword ? null : { passwordsMismatch: true };
  }

  imageValidator = (control: AbstractControl): ValidationErrors | null => {
    const file = control.value;
    if (!file) return null;
    const allowed = ['image/jpeg', 'image/png', 'image/jpg'];
    if (!allowed.includes(file.type)) return { invalidType: true };
    if (file.size > 2 * 1024 * 1024) return { maxSize: true };
    return null;
  };

  onFileChange(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.signUpForm.get('photo')?.setValue(file);
    this.signUpForm.get('photo')?.markAsTouched();
    this.photoUploadUrl = this.buildMockFileUrl(file);
    const reader = new FileReader();
    reader.onload = () => this.previewUrl = reader.result as string;
    reader.readAsDataURL(file);
  }

  private buildMockFileUrl(file: File): string {
    const safeName = encodeURIComponent(file.name.replace(/\s+/g, '-'));
    return `https://files.greenroots.local/uploads/${Date.now()}-${safeName}`;
  }

  private enterVerificationMode(email: string | null, message: string): void {
    this.verificationEmail = email;
    this.verificationMessage = message;
    this.mode = 'verify';
    localStorage.setItem('authMode', 'verify');
    if (email) localStorage.setItem('pendingVerificationEmail', email);
    localStorage.setItem('pendingVerificationMessage', message);
  }

  initGoogleButton(): void {
    setTimeout(() => {
      if (typeof google === 'undefined') return;

      const googleBtn = document.getElementById('googleSignInButton');
      if (!googleBtn) return;

      googleBtn.innerHTML = '';

      if (!this.googleInitialized) {
        google.accounts.id.initialize({
          client_id: this.googleClientId,
          callback: (response: any) => this.handleGoogleCredential(response)
        });
        this.googleInitialized = true;
      }

      google.accounts.id.renderButton(googleBtn, {
        theme: 'outline',
        size: 'large',
        shape: 'rectangular',
        width: 190,
        text: this.mode === 'signup' ? 'signup_with' : 'signin_with',
        logo_alignment: 'left'
      });
    }, 200);
  }

  handleGoogleCredential(response: any): void {
    if (!response?.credential) {
      this.loginError = 'Google authentication failed. Please try again.';
      return;
    }

    if (this.mode === 'signup') {
      this.startGoogleSignup(response.credential);
      return;
    }

    this.loginWithGoogle(response.credential);
  }

  loginWithGoogle(credential: string): void {
    this.isLoading = true;
    this.loginError = null;

    this.authService.loginWithGoogle(credential).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res?.nextStep === 'SIGNUP') {
          this.startGoogleSignup(credential);
          return;
        }
        if (res?.verificationRequired || res?.nextStep === 'VERIFY_EMAIL') {
          this.enterVerificationMode(res.email, res.message || 'Please verify your email.');
          return;
        }
        if (!res?.token) {
          this.loginError = res?.message || 'Google login failed.';
          return;
        }
        const redirectRoute = this.authService.getDefaultRouteForRole(
            res.role ? res.role as BackendRole : null
        );
        this.router.navigate([redirectRoute]);
      },
      error: (err) => {
        this.isLoading = false;
        this.loginError = err.error?.message || 'Google login failed.';
      }
    });
  }

  startGoogleSignup(credential: string): void {
    this.isLoading = false;
    this.loginError = null;

    const profile = this.decodeGoogleCredential(credential);

    if (!profile?.email) {
      this.loginError = 'Could not read your Google account information. Please try again.';
      return;
    }

    this.googleSignupMode = true;
    this.googleCredential = credential;
    this.googleProfile = profile;
    this.facebookSignupMode = false;
    this.facebookAccessToken = null;
    this.facebookProfile = null;
    this.mode = 'signup';
    localStorage.setItem('authMode', 'signup');

    this.signUpForm.patchValue({
      firstName: profile.firstName || '',
      lastName: profile.lastName || '',
      email: profile.email || '',
      password: 'GOOGLE_AUTH',
      photo: null,
      phone: '',
      role: ''
    });

    this.previewUrl = profile.picture || null;
    this.photoUploadUrl = profile.picture || null;
    this.applyGoogleSignupValidators();
  }

  applyGoogleSignupValidators(): void {
    this.signUpForm.get('firstName')?.clearValidators();
    this.signUpForm.get('lastName')?.clearValidators();
    this.signUpForm.get('email')?.clearValidators();
    this.signUpForm.get('password')?.clearValidators();
    this.signUpForm.get('photo')?.clearValidators();

    this.signUpForm.get('phone')?.setValidators([Validators.required, Validators.pattern(/^[0-9]{8}$/)]);
    this.signUpForm.get('role')?.setValidators([Validators.required]);

    Object.keys(this.signUpForm.controls).forEach(key => {
      this.signUpForm.get(key)?.updateValueAndValidity();
    });
  }

  restoreNormalSignupValidators(): void {
    if (!this.signUpForm) return;
    this.clearSocialSignupState();

    this.signUpForm.get('firstName')?.setValidators([Validators.required, Validators.pattern(/^[a-zA-Z]+$/)]);
    this.signUpForm.get('lastName')?.setValidators([Validators.required, Validators.pattern(/^[a-zA-Z]+$/)]);
    this.signUpForm.get('email')?.setValidators([Validators.required, Validators.email]);
    this.signUpForm.get('password')?.setValidators([Validators.required, Validators.minLength(8)]);
    this.signUpForm.get('photo')?.setValidators([Validators.required, this.imageValidator]);
    this.signUpForm.get('phone')?.setValidators([Validators.required, Validators.pattern(/^[0-9]{8}$/)]);
    this.signUpForm.get('role')?.setValidators([Validators.required]);

    Object.keys(this.signUpForm.controls).forEach(key => {
      this.signUpForm.get(key)?.updateValueAndValidity();
    });
  }

  private clearSocialSignupState(): void {
    this.googleSignupMode = false;
    this.googleCredential = null;
    this.googleProfile = null;
    this.facebookSignupMode = false;
    this.facebookAccessToken = null;
    this.facebookProfile = null;
  }

  submitGoogleSignupCompletion(): void {
    if (!this.googleCredential) {
      this.loginError = 'Missing Google signup session. Please try again.';
      return;
    }

    if (this.signUpForm.invalid) {
      this.signUpForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.loginError = null;

    const payload = {
      credential: this.googleCredential,
      telephone: this.signUpForm.value.phone,
      role: this.selectedRole
    };

    this.authService.completeGoogleSignup(payload).subscribe({
      next: (response) => {
        this.isLoading = false;

        if (response.nextStep === 'SIGNUP_STEP2' && response.userId != null) {
          localStorage.setItem('signupBase', JSON.stringify({
            firstName: this.signUpForm.value.firstName,
            lastName: this.signUpForm.value.lastName,
            email: this.signUpForm.value.email,
            phone: this.signUpForm.value.phone,
            role: this.selectedRole,
            photo: this.photoUploadUrl
          }));
          localStorage.setItem('signupRole', this.selectedRole);
          localStorage.setItem('signupUserId', String(response.userId));
          localStorage.setItem('signupEmail', response.email || this.signUpForm.value.email);
          localStorage.setItem('signupMessage', response.message || 'Google signup completed. Please complete your profile.');
          this.router.navigate(['/register-extra']);
          return;
        }

        if ('verificationRequired' in response && (response.verificationRequired || response.nextStep === 'VERIFY_EMAIL')) {
          this.enterVerificationMode(response.email, response.message || 'Please verify your email.');
          return;
        }

        if ('token' in response && response.token) {
          const redirectRoute = this.authService.getDefaultRouteForRole(
              response.role ? response.role as BackendRole : null
          );
          this.router.navigate([redirectRoute]);
          return;
        }

        this.mode = 'signin';
        this.clearSocialSignupState();
        this.restoreNormalSignupValidators();
        this.loginError = response.message || 'Google signup completed. You can now sign in.';
      },
      error: (err) => {
        this.isLoading = false;
        this.loginError = err.error?.message || 'Could not complete Google signup.';
      }
    });
  }

  decodeGoogleCredential(credential: string): any {
    try {
      const payload = credential.split('.')[1];
      const decodedPayload = JSON.parse(
          decodeURIComponent(
              atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
                  .split('')
                  .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                  .join('')
          )
      );
      return {
        email: decodedPayload.email,
        firstName: decodedPayload.given_name,
        lastName: decodedPayload.family_name,
        picture: decodedPayload.picture
      };
    } catch (e) {
      console.error('Could not decode Google credential', e);
      return null;
    }
  }

  initFacebookSdk(): void {
    setTimeout(() => {
      if (typeof FB === 'undefined') return;
      if (this.facebookInitialized) return;

      FB.init({
        appId: this.facebookAppId,
        cookie: true,
        xfbml: false,
        version: 'v20.0'
      });

      this.facebookInitialized = true;
    }, 500);
  }

  loginWithFacebook(): void {
    this.initFacebookSdk();

    if (typeof FB === 'undefined') {
      this.loginError = 'Facebook authentication is not available. Please refresh the page.';
      return;
    }

    this.loginError = null;

    FB.login((response: any) => {
      if (!response?.authResponse?.accessToken) {
        this.loginError = 'Facebook login was cancelled or failed.';
        return;
      }

      const accessToken = response.authResponse.accessToken;

      FB.api('/me', { fields: 'id,first_name,last_name,name,email,picture' }, (profile: any) => {
        if (!profile || profile.error) {
          this.loginError = 'Could not read your Facebook profile.';
          return;
        }

        if (this.mode === 'signup') {
          this.startFacebookSignup(accessToken, profile);
          return;
        }

        this.signInWithFacebook(accessToken, profile);
      });
    }, { scope: 'email,public_profile', return_scopes: true });
  }

  startFacebookSignup(accessToken: string, profile: any): void {
    this.loginError = null;

    if (!profile?.email) {
      this.loginError = 'Your Facebook account did not provide an email. Please use Google or email signup.';
      return;
    }

    this.facebookSignupMode = true;
    this.facebookAccessToken = accessToken;
    this.facebookProfile = profile;
    this.googleSignupMode = false;
    this.googleCredential = null;
    this.googleProfile = null;
    this.mode = 'signup';
    localStorage.setItem('authMode', 'signup');

    this.signUpForm.patchValue({
      firstName: profile.first_name || '',
      lastName: profile.last_name || '',
      email: profile.email || '',
      password: 'FACEBOOK_AUTH',
      photo: null,
      phone: '',
      role: ''
    });

    this.previewUrl = profile.picture?.data?.url || null;
    this.photoUploadUrl = profile.picture?.data?.url || null;
    this.applyGoogleSignupValidators();
  }

  signInWithFacebook(accessToken: string, profile?: any): void {
    this.isLoading = true;
    this.loginError = null;

    this.authService.loginWithFacebook(accessToken).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res?.nextStep === 'SIGNUP') {
          if (profile) {
            this.startFacebookSignup(accessToken, profile);
          } else {
            this.loginError = res?.message || 'No account found. Please sign up first.';
          }
          return;
        }
        if (res?.verificationRequired || res?.nextStep === 'VERIFY_EMAIL') {
          this.enterVerificationMode(res.email, res.message || 'Please verify your email.');
          return;
        }
        if (!res?.token) {
          this.loginError = res?.message || 'Facebook login failed.';
          return;
        }
        const redirectRoute = this.authService.getDefaultRouteForRole(
            res.role ? res.role as BackendRole : null
        );
        this.router.navigate([redirectRoute]);
      },
      error: (err) => {
        this.isLoading = false;
        this.loginError = err.error?.message || 'Facebook login failed.';
      }
    });
  }

  submitFacebookSignupCompletion(): void {
    if (!this.facebookAccessToken) {
      this.loginError = 'Missing Facebook signup session. Please try again.';
      return;
    }

    if (this.signUpForm.invalid) {
      this.signUpForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.loginError = null;

    const payload = {
      accessToken: this.facebookAccessToken,
      telephone: this.signUpForm.value.phone,
      role: this.selectedRole
    };

    this.authService.completeFacebookSignup(payload).subscribe({
      next: (response) => {
        this.isLoading = false;

        if (response.nextStep === 'SIGNUP_STEP2' && response.userId != null) {
          localStorage.setItem('signupBase', JSON.stringify({
            firstName: this.signUpForm.value.firstName,
            lastName: this.signUpForm.value.lastName,
            email: this.signUpForm.value.email,
            phone: this.signUpForm.value.phone,
            role: this.selectedRole,
            photo: this.photoUploadUrl
          }));
          localStorage.setItem('signupRole', this.selectedRole);
          localStorage.setItem('signupUserId', String(response.userId));
          localStorage.setItem('signupEmail', response.email || this.signUpForm.value.email);
          localStorage.setItem('signupMessage', response.message || 'Facebook signup completed. Please complete your profile.');
          this.router.navigate(['/register-extra']);
          return;
        }

        if ('verificationRequired' in response && (response.verificationRequired || response.nextStep === 'VERIFY_EMAIL')) {
          this.enterVerificationMode(response.email, response.message || 'Please verify your email.');
          return;
        }

        if ('token' in response && response.token) {
          const redirectRoute = this.authService.getDefaultRouteForRole(
              response.role ? response.role as BackendRole : null
          );
          this.router.navigate([redirectRoute]);
          return;
        }

        this.mode = 'signin';
        this.clearSocialSignupState();
        this.restoreNormalSignupValidators();
        this.loginError = response.message || 'Facebook signup completed. You can now sign in.';
      },
      error: (err) => {
        this.isLoading = false;
        this.loginError = err.error?.message || 'Could not complete Facebook signup.';
      }
    });
  }
}