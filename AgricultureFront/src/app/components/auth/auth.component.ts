import { Component, OnInit } from '@angular/core';
import { FormGroup, FormControl, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { AuthService, BackendRole, SignupStep1Request, SignupResponse } from '../../services/auth/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-auth',
  standalone: false,
  templateUrl: './auth.component.html',
  styleUrls: ['./auth.component.css']
})
export class AuthComponent implements OnInit {

  mode: 'signin' | 'signup' | 'verify' = 'signin';
  showSignInPass    = false;
  showSignUpPass    = false;
  previewUrl:           string | null = null;
  photoUploadUrl:       string | null = null;
  loginError:           string | null = null;
  isLoading                           = false;
  photoUploading                     = false;
  photoUploadError:    string | null = null;
  verificationUserId:   number | null = null;
  verificationEmail:    string | null = null;
  verificationMessage:  string | null = null;
  verificationLoading                 = false;
  verificationError:    string | null = null;

  signInForm!: FormGroup;
  signUpForm!: FormGroup;

  // Roles that require extra info
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
      private router:      Router
  ) {}

  ngOnInit(): void {
    const authNotice = this.authService.consumeAuthNotice();
    if (authNotice) {
      this.loginError = authNotice;
      this.mode = 'signin';
      localStorage.setItem('authMode', 'signin');
    }

    const savedMode = localStorage.getItem('authMode');
    if (savedMode === 'signin' || savedMode === 'signup' || savedMode === 'verify') {
      this.mode = savedMode;
    }

    const pendingVerificationId      = localStorage.getItem('pendingVerificationUserId');
    const pendingVerificationEmail   = localStorage.getItem('pendingVerificationEmail');
    const pendingVerificationMessage = localStorage.getItem('pendingVerificationMessage');
    if (this.mode === 'verify') {
      this.verificationUserId  = pendingVerificationId ? Number(pendingVerificationId) : null;
      this.verificationEmail   = pendingVerificationEmail;
      this.verificationMessage = pendingVerificationMessage || 'Please verify your email to continue.';
    }

    const rememberedEmail = localStorage.getItem('rememberedEmail');

    this.signInForm = new FormGroup({
      email:    new FormControl(rememberedEmail || '', [Validators.required, Validators.email]),
      password: new FormControl('', [Validators.required, Validators.minLength(8)]),
      remember: new FormControl(true)
    });

    this.signUpForm = new FormGroup({
      firstName: new FormControl('', [Validators.required, Validators.pattern(/^[a-zA-Z]+$/)]),
      lastName:  new FormControl('', [Validators.required, Validators.pattern(/^[a-zA-Z]+$/)]),
      email:     new FormControl('', [Validators.required, Validators.email]),
      phone:     new FormControl('', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]),
      password:  new FormControl('', [Validators.required, Validators.minLength(8)]),
      photo:     new FormControl(null, [Validators.required, this.imageValidator]),
      role:      new FormControl('', Validators.required)
    });

    if (!authNotice && this.authService.hasActiveSession()) {
      const redirectRoute = this.authService.getDefaultRouteForRole(this.authService.getCurrentRole());
      this.router.navigate([redirectRoute]);
    }
  }

  switchTo(m: 'signin' | 'signup'): void {
    this.mode = m;
    localStorage.setItem('authMode', m);
  }

  togglePass(field: 'signin' | 'signup'): void {
    if (field === 'signin') this.showSignInPass = !this.showSignInPass;
    else                    this.showSignUpPass = !this.showSignUpPass;
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

  submitSignIn(): void {
    if (this.signInForm.invalid) { this.signInForm.markAllAsTouched(); return; }

    this.isLoading  = true;
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
          this.enterVerificationMode(response.userId, response.email, response.message || 'Please verify your email.');
        } else {
          this.loginError = response.nextStep === 'PENDING_ADMIN_REVIEW'
            ? (response.message || 'Your account is awaiting document review by an administrator.')
            : (response.message || 'Login failed');
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.loginError = err.error?.message || 'An error occurred during login';
        this.isLoading  = false;
      }
    });
  }

  submitSignUp(): void {
    if (this.photoUploading) {
      this.loginError = 'Please wait until the profile photo finishes uploading.';
      return;
    }

    if (!this.photoUploadUrl) {
      this.loginError = 'Please upload a valid profile photo before continuing.';
      return;
    }

    if (this.signUpForm.invalid) { this.signUpForm.markAllAsTouched(); return; }

    this.isLoading  = true;
    this.loginError = null;

    const payload: SignupStep1Request = {
      nom:        this.signUpForm.value.lastName  ?? '',
      prenom:     this.signUpForm.value.firstName ?? '',
      email:      this.signUpForm.value.email     ?? '',
      motDePasse: this.signUpForm.value.password  ?? '',
      role:       this.selectedRole,
      photo:      this.photoUploadUrl,
      telephone:  this.signUpForm.value.phone     ?? ''
    };

    this.authService.signupStep1(payload).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (response.nextStep === 'SIGNUP_STEP2' && response.userId != null) {
          localStorage.setItem('signupBase', JSON.stringify({
            ...this.signUpForm.value,
            photo: this.photoUploadUrl
          }));
          localStorage.setItem('signupRole',    this.selectedRole);
          localStorage.setItem('signupUserId',  String(response.userId));
          localStorage.setItem('signupEmail',   response.email || payload.email);
          localStorage.setItem('signupMessage', response.message || 'Complete your profile.');
          this.router.navigate(['/register-extra']);
          return;
        }

        if (response.nextStep === 'VERIFY_EMAIL') {
          this.enterVerificationMode(response.userId, response.email, response.message || 'Please verify your email to continue.');
          return;
        }

        this.enterVerificationMode(response.userId, response.email, response.message || 'Please verify your email to continue.');
      },
      error: (err) => {
        this.isLoading  = false;
        this.loginError = err.error?.message || 'Could not create your account';
      }
    });
  }

  resendVerification(): void {
    if (this.verificationUserId == null) {
      this.verificationError = 'Missing verification context.';
      return;
    }

    this.verificationLoading = true;
    this.verificationError   = null;

    this.authService.verifyEmail(this.verificationUserId).subscribe({
      next: (response) => {
        this.verificationLoading = false;
        this.verificationMessage = response.message || 'Email verified successfully.';
        localStorage.removeItem('pendingVerificationUserId');
        localStorage.removeItem('pendingVerificationEmail');
        localStorage.removeItem('pendingVerificationMessage');
        this.mode = 'signin';
        if (this.verificationEmail) {
          this.signInForm.get('email')?.setValue(this.verificationEmail);
          this.signInForm.get('remember')?.setValue(true);
        }
        this.loginError = null;
      },
      error: (err) => {
        this.verificationLoading = false;
        this.verificationError   = err.error?.message || 'Verification failed';
      }
    });
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
    this.photoUploadError = null;
    this.photoUploading = true;
    const reader    = new FileReader();
    reader.onload   = () => this.previewUrl = reader.result as string;
    reader.readAsDataURL(file);

    this.authService.uploadUserFile(file).subscribe({
      next: ({ url }) => {
        this.photoUploadUrl = url;
        this.photoUploading = false;
      },
      error: (err) => {
        this.photoUploadUrl = null;
        this.photoUploading = false;
        this.photoUploadError = err.error?.message || err.error || 'Could not upload the profile photo.';
        this.signUpForm.get('photo')?.setErrors({ uploadFailed: true });
      }
    });
  }

  private enterVerificationMode(userId: number | null, email: string | null, message: string): void {
    this.verificationUserId  = userId;
    this.verificationEmail   = email;
    this.verificationMessage = message;
    this.mode = 'verify';
    localStorage.setItem('authMode', 'verify');
    if (userId != null) localStorage.setItem('pendingVerificationUserId', String(userId));
    if (email)          localStorage.setItem('pendingVerificationEmail', email);
    localStorage.setItem('pendingVerificationMessage', message);
  }

  canSubmitSignUp(): boolean {
    return !this.isLoading && !this.photoUploading;
  }
}
