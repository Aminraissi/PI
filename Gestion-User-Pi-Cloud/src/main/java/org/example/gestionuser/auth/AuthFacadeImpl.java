package org.example.gestionuser.auth;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.example.gestionuser.Services.IHachageService;
import org.example.gestionuser.Services.IRecaptchaService;
import org.example.gestionuser.Services.IUser;
import org.example.gestionuser.Services.SmsService;
import org.example.gestionuser.dtos.*;
import org.example.gestionuser.entities.EmailVerificationStatus;
import org.example.gestionuser.entities.ProfileValidationStatus;
import org.example.gestionuser.entities.Role;
import org.example.gestionuser.entities.StatutCompte;
import org.example.gestionuser.entities.User;
import org.example.gestionuser.util.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthFacadeImpl implements AuthFacade {

    private final IUser userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final IHachageService hachageService;
    private final IRecaptchaService recaptchaService;
    private final SmsService smsService;


    @Value("${google.client-id}")
    private String googleClientId;

    @Override
    public LoginResponse login(String email, String motDePasse, String captchaToken) {

        boolean isCaptchaValid = recaptchaService.verifyCaptcha(captchaToken);
        if (!isCaptchaValid) {
            System.err.println("Échec validation CAPTCHA");
            return new LoginResponse(null, null, null, email, null, null, null, null,
                    "LOGIN", false, "Échec de vérification CAPTCHA. Veuillez réessayer.");
        }

        User user = userService.findByEmail(email);

        if (user == null) {
            return new LoginResponse(null, null, null, email, null, null, null, null,
                    "LOGIN", false, "No account was found for this email address");
        }

        if (!hachageService.verifyPassword(motDePasse, user.getMotDePasse())) {
            return new LoginResponse(null, null, null, email, null, null, null, null,
                    "LOGIN", false, "Incorrect email or password");
        }

        return buildAuthorizedLoginResponse(user, "Login successful");
    }

    @Override
    public SignupResponse signupStep1(SignupStep1Request request) {
        Role normalizedRole = parseRole(request.getRole());

        if (request.getEmail() == null || request.getEmail().isBlank() ||
                request.getMotDePasse() == null || request.getMotDePasse().isBlank() ||
                normalizedRole == null) {
            return new SignupResponse(null, request.getEmail(), null, null,
                    null, null, "SIGNUP_STEP1", "Missing required fields or invalid role");
        }

        User existing = userService.findByEmail(request.getEmail());
        if (existing != null) {
            return new SignupResponse(existing.getId(), existing.getEmail(),
                    existing.getRole() != null ? existing.getRole().name() : null,
                    existing.getStatutCompte() != null ? existing.getStatutCompte().name() : null,
                    existing.getEmailVerificationStatus() != null ? existing.getEmailVerificationStatus().name() : null,
                    existing.getProfileValidationStatus() != null ? existing.getProfileValidationStatus().name() : null,
                    "LOGIN",
                    "Email already exists");
        }

        User user = new User();
        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setEmail(request.getEmail());
        user.setMotDePasse(hachageService.hashPassword(request.getMotDePasse()));
        if (request.getPhoto() != null) {
            user.setPhoto(request.getPhoto());
        }
        if (request.getTelephone() != null) {
            user.setTelephone(request.getTelephone());
        }
        user.setRole(normalizedRole);
        user.setStatutCompte(StatutCompte.EN_ATTENTE);
        user.setEmailVerificationStatus(EmailVerificationStatus.PENDING);
        user.setProfileValidationStatus(roleNeedsExtendedProfile(normalizedRole)
                ? ProfileValidationStatus.INCOMPLETE
                : ProfileValidationStatus.NOT_REQUIRED);

        User saved = userService.adduser(user);

        // TODO(email-team): Generate verification token and send confirmation email to saved.getEmail().
        triggerEmailVerificationEmail(saved);

        String nextStep = roleNeedsExtendedProfile(saved.getRole()) ? "SIGNUP_STEP2" : "VERIFY_EMAIL";
        return new SignupResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getRole().name(),
                saved.getStatutCompte().name(),
                saved.getEmailVerificationStatus().name(),
                saved.getProfileValidationStatus().name(),
                nextStep,
                roleNeedsExtendedProfile(saved.getRole())
                        ? "Step 1 completed. Continue with profile details."
                        : "Account created. Please verify your email before login."
        );
    }

    @Override
    public SignupResponse signupStep2(Long userId, SignupStep2Request request) {
        User user = userService.getUser(userId);
        if (user == null) {
            return new SignupResponse(null, null, null, null,
                    null, null, "SIGNUP_STEP2", "User not found");
        }

        if (request.getPhoto() != null) user.setPhoto(request.getPhoto());
        if (request.getTelephone() != null) user.setTelephone(request.getTelephone());
        if (request.getRegion() != null) user.setRegion(request.getRegion());

        if (request.getDiplomeExpert() != null) user.setDiplomeExpert(request.getDiplomeExpert());
        if (request.getDocumentUrl() != null) user.setDiplomeExpert(request.getDocumentUrl());

        if (request.getVehicule() != null) user.setTypeVehicule(request.getVehicule());
        if (request.getCapacite() != null) user.setCapaciteKg(request.getCapacite());

        if (request.getAgence() != null) user.setAgence(request.getAgence());
        if (request.getCertificatTravail() != null) user.setCertificatTravail(request.getCertificatTravail());

        if (request.getAdresseCabinet() != null) user.setAdresseCabinet(request.getAdresseCabinet());
        if (request.getPresentationCarriere() != null) user.setPresentationCarriere(request.getPresentationCarriere());
        if (request.getTelephoneCabinet() != null) user.setTelephoneCabinet(request.getTelephoneCabinet());

        if (request.getNomOrganisation() != null) user.setNom_organisation(request.getNomOrganisation());
        if (request.getOrganizationLogo() != null) user.setLogo_organisation(request.getOrganizationLogo());
        if (request.getCin() != null) user.setCin(request.getCin());
        if (request.getDescription() != null) user.setDescription(request.getDescription());

        if (roleNeedsDocumentValidation(user.getRole())) {
            user.setProfileValidationStatus(ProfileValidationStatus.PENDING_VALIDATION);
        } else if (roleNeedsExtendedProfile(user.getRole())) {
            user.setProfileValidationStatus(ProfileValidationStatus.VALIDATED);
        } else {
            user.setProfileValidationStatus(ProfileValidationStatus.NOT_REQUIRED);
        }

        if (user.getEmailVerificationStatus() == EmailVerificationStatus.VERIFIED) {
            normalizeStatusesAfterEmailVerification(user);
        }

        User updated = userService.updateUser(user);
        String nextStep = updated.getEmailVerificationStatus() == EmailVerificationStatus.VERIFIED ? "LOGIN" : "VERIFY_EMAIL";
        String message = updated.getEmailVerificationStatus() == EmailVerificationStatus.VERIFIED
                ? postSignupMessage(updated)
                : "Step 2 completed. Please verify your email.";

        return new SignupResponse(
                updated.getId(),
                updated.getEmail(),
                updated.getRole() != null ? updated.getRole().name() : null,
                updated.getStatutCompte() != null ? updated.getStatutCompte().name() : null,
                updated.getEmailVerificationStatus() != null ? updated.getEmailVerificationStatus().name() : null,
                updated.getProfileValidationStatus() != null ? updated.getProfileValidationStatus().name() : null,
                nextStep,
                message
        );
    }

    @Override
    public SignupResponse verifyEmail(Long userId) {
        User user = userService.getUser(userId);
        if (user == null) {
            return new SignupResponse(null, null, null, null,
                    null, null, "VERIFY_EMAIL", "User not found");
        }

        // TODO(email-team): Validate verification token and expiration before marking email as verified.
        user.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        normalizeStatusesAfterEmailVerification(user);

        User updated = userService.updateUser(user);
        return new SignupResponse(
                updated.getId(),
                updated.getEmail(),
                updated.getRole() != null ? updated.getRole().name() : null,
                updated.getStatutCompte() != null ? updated.getStatutCompte().name() : null,
                updated.getEmailVerificationStatus() != null ? updated.getEmailVerificationStatus().name() : null,
                updated.getProfileValidationStatus() != null ? updated.getProfileValidationStatus().name() : null,
                "LOGIN",
                "Email verified successfully. You can now log in."
        );
    }

    @Override
    public SignupResponse forgotPasswordCheckEmail(String email) {
        if (email == null || email.isBlank()) {
            return new SignupResponse(null, null, null, null, null, null,
                    "FORGOT_PASSWORD_EMAIL", "Email is required");
        }

        User user = userService.findByEmail(email);

        if (user == null) {
            return new SignupResponse(null, email, null, null, null, null,
                    "SIGNUP", "No account with this email. Please create one.");
        }

        return new SignupResponse(
                user.getId(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatutCompte() != null ? user.getStatutCompte().name() : null,
                user.getEmailVerificationStatus() != null ? user.getEmailVerificationStatus().name() : null,
                user.getProfileValidationStatus() != null ? user.getProfileValidationStatus().name() : null,
                "FORGOT_PASSWORD_PHONE",
                "Email found. Please confirm your phone number."
        );
    }

    @Override
    public SignupResponse forgotPasswordByPhone(String email, String telephone) {
        if (email == null || email.isBlank() || telephone == null || telephone.isBlank()) {
            return new SignupResponse(null, email, null, null, null, null,
                    "FORGOT_PASSWORD_PHONE", "Email and phone number are required");
        }

        User user = userService.findByEmail(email);

        if (user == null) {
            return new SignupResponse(null, email, null, null, null, null,
                    "SIGNUP", "No account with this email. Please create one.");
        }

        String savedPhone = user.getTelephone() == null ? "" : user.getTelephone().trim().replace(" ", "");
        String providedPhone = telephone.trim().replace(" ", "");

        if (!savedPhone.equals(providedPhone)) {
            return new SignupResponse(null, user.getEmail(), null, null, null, null,
                    "FORGOT_PASSWORD_PHONE", "Wrong phone number for this email account.");
        }

        String smsPhone = normalizePhoneForSms(telephone);
        smsService.sendSms(smsPhone, "Your verification code for GreenRoots website.");

        return new SignupResponse(
                user.getId(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatutCompte() != null ? user.getStatutCompte().name() : null,
                user.getEmailVerificationStatus() != null ? user.getEmailVerificationStatus().name() : null,
                user.getProfileValidationStatus() != null ? user.getProfileValidationStatus().name() : null,
                "RESET_PASSWORD",
                "Reset code sent to your phone."
        );
    }

    @Override
    public SignupResponse resetPasswordByPhone(String email, String telephone, String code, String newPassword) {
        if (email == null || email.isBlank()
                || telephone == null || telephone.isBlank()
                || code == null || code.isBlank()
                || newPassword == null || newPassword.isBlank()) {
            return new SignupResponse(null, email, null, null, null, null,
                    "RESET_PASSWORD", "Email, phone, code and new password are required");
        }

        User user = userService.findByEmail(email);

        if (user == null) {
            return new SignupResponse(null, email, null, null, null, null,
                    "SIGNUP", "No account with this email. Please create one.");
        }

        String savedPhone = user.getTelephone() == null ? "" : user.getTelephone().trim().replace(" ", "");
        String providedPhone = telephone.trim().replace(" ", "");

        if (!savedPhone.equals(providedPhone)) {
            return new SignupResponse(null, user.getEmail(), null, null, null, null,
                    "RESET_PASSWORD", "Wrong phone number for this email account.");
        }

        String smsPhone = normalizePhoneForSms(telephone);
        boolean validCode = smsService.checkCode(smsPhone, code);

        if (!validCode) {
            return new SignupResponse(null, user.getEmail(), null, null, null, null,
                    "RESET_PASSWORD", "Invalid or expired reset code");
        }

        if (newPassword.length() < 8) {
            return new SignupResponse(null, user.getEmail(), null, null, null, null,
                    "RESET_PASSWORD", "Password must be at least 8 characters");
        }

        user.setMotDePasse(hachageService.hashPassword(newPassword)); // ← bug fix: hash before saving
        User updated = userService.updateUser(user);

        return new SignupResponse(
                updated.getId(),
                updated.getEmail(),
                updated.getRole() != null ? updated.getRole().name() : null,
                updated.getStatutCompte() != null ? updated.getStatutCompte().name() : null,
                updated.getEmailVerificationStatus() != null ? updated.getEmailVerificationStatus().name() : null,
                updated.getProfileValidationStatus() != null ? updated.getProfileValidationStatus().name() : null,
                "LOGIN",
                "Password reset successfully. You can now sign in."
        );
    }

    @Override
    public LoginResponse loginWithGoogle(String credential) {
        try {
            if (credential == null || credential.isBlank()) {
                return new LoginResponse(null, null, null, null, null, null, null, null,
                        "GOOGLE_LOGIN", false, "Missing Google credential");
            }

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);

            if (idToken == null) {
                return new LoginResponse(null, null, null, null, null, null, null, null,
                        "GOOGLE_LOGIN", false, "Invalid Google token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email    = payload.getEmail();
            String firstName = (String) payload.get("given_name");
            String lastName  = (String) payload.get("family_name");
            String picture   = (String) payload.get("picture");

            User user = userService.findByEmail(email);

            if (user == null) {
                return new LoginResponse(null, null, null, email, null, null, null, null,
                        "SIGNUP", false, "No account was found for this Google email. Please sign up first.");
            }

            if (user.getPrenom() == null || user.getPrenom().isBlank()) {
                user.setPrenom(firstName);
            }
            if (user.getNom() == null || user.getNom().isBlank()) {
                user.setNom(lastName);
            }
            if ((user.getPhoto() == null || user.getPhoto().isBlank()) && picture != null && !picture.isBlank()) {
                user.setPhoto(picture);
            }
            user = userService.updateUser(user);

            return buildAuthorizedLoginResponse(user, "Google login successful");

        } catch (Exception e) {
            return new LoginResponse(null, null, null, null, null, null, null, null,
                    "GOOGLE_LOGIN", false, "Google login failed: " + e.getMessage());
        }
    }

    @Override
    public LoginResponse completeGoogleSignup(GoogleCompleteSignupRequest request) {
        try {
            if (request == null
                    || request.getCredential() == null || request.getCredential().isBlank()
                    || request.getTelephone() == null || request.getTelephone().isBlank()
                    || request.getRole() == null || request.getRole().isBlank()) {
                return new LoginResponse(null, null, null, null, null, null, null, null,
                        "GOOGLE_COMPLETE_SIGNUP", false, "Missing required Google signup fields");
            }

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getCredential());

            if (idToken == null) {
                return new LoginResponse(null, null, null, null, null, null, null, null,
                        "GOOGLE_COMPLETE_SIGNUP", false, "Missing, invalid, or expired token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email     = payload.getEmail();
            String firstName = (String) payload.get("given_name");
            String lastName  = (String) payload.get("family_name");
            String picture   = (String) payload.get("picture");

            Role normalizedRole = parseRole(request.getRole());
            if (normalizedRole == null) {
                return new LoginResponse(null, null, null, email, null, null, null, null,
                        "GOOGLE_COMPLETE_SIGNUP", false, "Invalid role");
            }

            User user = userService.findByEmail(email);
            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setPrenom(firstName);
                user.setNom(lastName);
                user.setPhoto(picture);
                user.setMotDePasse(null);
            }

            user.setTelephone(request.getTelephone());
            user.setRole(normalizedRole);
            user.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
            user.setProfileValidationStatus(
                    roleNeedsExtendedProfile(normalizedRole)
                            ? ProfileValidationStatus.INCOMPLETE
                            : ProfileValidationStatus.NOT_REQUIRED
            );
            normalizeStatusesAfterEmailVerification(user);

            User saved = user.getId() == null
                    ? userService.adduser(user)
                    : userService.updateUser(user);
            boolean needsExtraProfile = roleNeedsExtendedProfile(saved.getRole());

            if (!needsExtraProfile) {
                return buildAuthorizedLoginResponse(saved, "Google signup completed successfully.");
            }

            return new LoginResponse(
                    null,
                    saved.getId(),
                    buildUsername(saved),
                    saved.getEmail(),
                    saved.getRole() != null ? saved.getRole().name() : null,
                    saved.getStatutCompte() != null ? saved.getStatutCompte().name() : null,
                    saved.getEmailVerificationStatus() != null ? saved.getEmailVerificationStatus().name() : null,
                    saved.getProfileValidationStatus() != null ? saved.getProfileValidationStatus().name() : null,
                    "SIGNUP_STEP2",
                    false,
                    "Google signup completed. Please complete your profile."
            );

        } catch (Exception e) {
            return new LoginResponse(null, null, null, null, null, null, null, null,
                    "GOOGLE_COMPLETE_SIGNUP", false, "Google signup failed: " + e.getMessage());
        }
    }

    @Override
    public LoginResponse completeFacebookSignup(FacebookCompleteSignupRequest request) {
        try {
            if (request == null
                    || request.getAccessToken() == null || request.getAccessToken().isBlank()
                    || request.getTelephone() == null || request.getTelephone().isBlank()
                    || request.getRole() == null || request.getRole().isBlank()) {
                return new LoginResponse(null, null, null, null, null, null, null, null,
                        "FACEBOOK_COMPLETE_SIGNUP", false, "Missing required Facebook signup fields");
            }

            String graphUrl = "https://graph.facebook.com/me"
                    + "?fields=id,first_name,last_name,name,email,picture"
                    + "&access_token=" + request.getAccessToken();

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> facebookProfile = restTemplate.getForObject(graphUrl, Map.class);

            if (facebookProfile == null || facebookProfile.get("id") == null) {
                return new LoginResponse(null, null, null, null, null, null, null, null,
                        "FACEBOOK_COMPLETE_SIGNUP", false, "Invalid Facebook token");
            }

            String email     = (String) facebookProfile.get("email");
            String firstName = (String) facebookProfile.get("first_name");
            String lastName  = (String) facebookProfile.get("last_name");

            if (email == null || email.isBlank()) {
                return new LoginResponse(null, null, null, null, null, null, null, null,
                        "FACEBOOK_COMPLETE_SIGNUP", false, "Facebook account did not provide an email");
            }

            String pictureUrl = null;
            Object pictureObj = facebookProfile.get("picture");
            if (pictureObj instanceof Map<?, ?> pictureMap) {
                Object dataObj = pictureMap.get("data");
                if (dataObj instanceof Map<?, ?> dataMap) {
                    Object urlObj = dataMap.get("url");
                    if (urlObj != null) pictureUrl = urlObj.toString();
                }
            }

            Role normalizedRole = parseRole(request.getRole());
            if (normalizedRole == null) {
                return new LoginResponse(null, null, null, email, null, null, null, null,
                        "FACEBOOK_COMPLETE_SIGNUP", false, "Invalid role");
            }

            User user = userService.findByEmail(email);
            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setPrenom(firstName);
                user.setNom(lastName);
                user.setPhoto(pictureUrl);
                user.setMotDePasse(null);
            }

            user.setTelephone(request.getTelephone());
            user.setRole(normalizedRole);
            user.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
            user.setProfileValidationStatus(
                    roleNeedsExtendedProfile(normalizedRole)
                            ? ProfileValidationStatus.INCOMPLETE
                            : ProfileValidationStatus.NOT_REQUIRED
            );
            normalizeStatusesAfterEmailVerification(user);

            User saved = user.getId() == null
                    ? userService.adduser(user)
                    : userService.updateUser(user);
            boolean needsExtraProfile = roleNeedsExtendedProfile(saved.getRole());

            if (!needsExtraProfile) {
                return buildAuthorizedLoginResponse(saved, "Facebook signup completed successfully.");
            }

            return new LoginResponse(
                    null,
                    saved.getId(),
                    buildUsername(saved),
                    saved.getEmail(),
                    saved.getRole() != null ? saved.getRole().name() : null,
                    saved.getStatutCompte() != null ? saved.getStatutCompte().name() : null,
                    saved.getEmailVerificationStatus() != null ? saved.getEmailVerificationStatus().name() : null,
                    saved.getProfileValidationStatus() != null ? saved.getProfileValidationStatus().name() : null,
                    "SIGNUP_STEP2",
                    false,
                    "Facebook signup completed. Please complete your profile."
            );

        } catch (Exception e) {
            return new LoginResponse(null, null, null, null, null, null, null, null,
                    "FACEBOOK_COMPLETE_SIGNUP", false, "Facebook signup failed: " + e.getMessage());
        }
    }

    @Override
    public LoginResponse loginWithFacebook(String accessToken) {
        try {
            if (accessToken == null || accessToken.isBlank()) {
                return new LoginResponse(null, null, null, null, null, null, null, null,
                        "FACEBOOK_LOGIN", false, "Missing Facebook access token");
            }

            String graphUrl = "https://graph.facebook.com/me"
                    + "?fields=id,first_name,last_name,name,email,picture"
                    + "&access_token=" + accessToken;

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> facebookProfile = restTemplate.getForObject(graphUrl, Map.class);

            if (facebookProfile == null || facebookProfile.get("id") == null) {
                return new LoginResponse(null, null, null, null, null, null, null, null,
                        "FACEBOOK_LOGIN", false, "Invalid Facebook token");
            }

            String email     = (String) facebookProfile.get("email");
            String firstName = (String) facebookProfile.get("first_name");
            String lastName  = (String) facebookProfile.get("last_name");

            if (email == null || email.isBlank()) {
                return new LoginResponse(null, null, null, null, null, null, null, null,
                        "FACEBOOK_LOGIN", false, "Facebook account did not provide an email");
            }

            User user = userService.findByEmail(email);

            if (user == null) {
                return new LoginResponse(null, null, null, email, null, null, null, null,
                        "SIGNUP", false, "No account was found for this Facebook email. Please sign up first.");
            }

            return buildAuthorizedLoginResponse(user, "Facebook login successful");

        } catch (Exception e) {
            return new LoginResponse(null, null, null, null, null, null, null, null,
                    "FACEBOOK_LOGIN", false, "Facebook login failed: " + e.getMessage());
        }
    }

    private void normalizeStatusesAfterEmailVerification(User user) {
        if (user.getStatutCompte() == StatutCompte.SUSPENDU || user.getStatutCompte() == StatutCompte.REFUSE) {
            return;
        }

        if (roleNeedsDocumentValidation(user.getRole())) {
            if (user.getProfileValidationStatus() == null || user.getProfileValidationStatus() == ProfileValidationStatus.INCOMPLETE) {
                user.setProfileValidationStatus(ProfileValidationStatus.PENDING_VALIDATION);
            }
            if (user.getProfileValidationStatus() == ProfileValidationStatus.VALIDATED) {
                user.setStatutCompte(StatutCompte.APPROUVE);
            } else if (user.getProfileValidationStatus() == ProfileValidationStatus.REJECTED) {
                user.setStatutCompte(StatutCompte.REFUSE);
            } else {
                user.setStatutCompte(StatutCompte.EN_ATTENTE);
            }
            return;
        }

        if (roleNeedsExtendedProfile(user.getRole())
                && (user.getProfileValidationStatus() == null || user.getProfileValidationStatus() == ProfileValidationStatus.INCOMPLETE)) {
            user.setProfileValidationStatus(ProfileValidationStatus.INCOMPLETE);
            user.setStatutCompte(StatutCompte.EN_ATTENTE);
            return;
        }

        if (user.getProfileValidationStatus() == null) {
            user.setProfileValidationStatus(roleNeedsExtendedProfile(user.getRole())
                    ? ProfileValidationStatus.VALIDATED
                    : ProfileValidationStatus.NOT_REQUIRED);
        }
        user.setStatutCompte(StatutCompte.APPROUVE);
    }

    private LoginResponse buildAuthorizedLoginResponse(User user, String successMessage) {
        if (user.getEmailVerificationStatus() != EmailVerificationStatus.VERIFIED) {
            return new LoginResponse(
                    null,
                    user.getId(),
                    buildUsername(user),
                    user.getEmail(),
                    user.getRole() != null ? user.getRole().name() : null,
                    user.getStatutCompte() != null ? user.getStatutCompte().name() : null,
                    user.getEmailVerificationStatus() != null ? user.getEmailVerificationStatus().name() : EmailVerificationStatus.PENDING.name(),
                    user.getProfileValidationStatus() != null ? user.getProfileValidationStatus().name() : ProfileValidationStatus.INCOMPLETE.name(),
                    "VERIFY_EMAIL",
                    true,
                    "Email verification required before access"
            );
        }

        normalizeStatusesAfterEmailVerification(user);
        user = userService.updateUser(user);

        if (user.getStatutCompte() == StatutCompte.SUSPENDU) {
            return blockedLoginResponse(user, "ACCOUNT_SUSPENDED", "Account suspended by administrator");
        }

        if (user.getStatutCompte() == StatutCompte.REFUSE) {
            String message = user.getMotifRefus() != null && !user.getMotifRefus().isBlank()
                    ? "Account request rejected: " + user.getMotifRefus()
                    : "Account request rejected by administrator";
            return blockedLoginResponse(user, "ACCOUNT_REFUSED", message);
        }

        if (user.getStatutCompte() == StatutCompte.EN_ATTENTE) {
            String message = user.getProfileValidationStatus() == ProfileValidationStatus.PENDING_VALIDATION
                    ? "Your account is awaiting document review by an administrator"
                    : "Account pending administrator review";
            return blockedLoginResponse(user, "PENDING_ADMIN_REVIEW", message);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        String username = buildUsername(user);
        String role = user.getRole() != null ? user.getRole().name() : null;
        String statutCompte = user.getStatutCompte() != null ? user.getStatutCompte().name() : null;
        String emailStatus = user.getEmailVerificationStatus() != null ? user.getEmailVerificationStatus().name() : EmailVerificationStatus.PENDING.name();
        String profileStatus = user.getProfileValidationStatus() != null ? user.getProfileValidationStatus().name() : ProfileValidationStatus.NOT_REQUIRED.name();

        return new LoginResponse(
                token,
                user.getId(),
                username,
                user.getEmail(),
                role,
                statutCompte,
                emailStatus,
                profileStatus,
                "ACCESS_GRANTED",
                false,
                successMessage
        );
    }

    private Role parseRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) return null;

        String normalized = rawRole
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");

        return switch (normalized) {
            case "FARMER", "AGRICULTEUR"                         -> Role.AGRICULTEUR;
            case "AGRICULTURALEXPERT", "EXPERTAGRICOLE"          -> Role.EXPERT_AGRICOLE;
            case "EVENTORGANIZER", "ORGANISATEUREVENEMENT"       -> Role.ORGANISATEUR_EVENEMENT;
            case "TRANSPORTER", "TRANSPORTEUR"                   -> Role.TRANSPORTEUR;
            case "VETERINARIAN", "VETERINAIRE"                   -> Role.VETERINAIRE;
            case "ADMIN"                                         -> Role.ADMIN;
            case "BUYER", "ACHETEUR"                             -> Role.ACHETEUR;
            case "AGENT"                                         -> Role.AGENT;
            default                                              -> null;
        };
    }

    private boolean roleNeedsExtendedProfile(Role role) {
        return role != null && role != Role.ADMIN && role != Role.ACHETEUR;
    }

    private boolean roleNeedsDocumentValidation(Role role) {
        return role == Role.EXPERT_AGRICOLE
                || role == Role.AGENT
                || role == Role.ORGANISATEUR_EVENEMENT;
    }

    private void triggerEmailVerificationEmail(User user) {
        // TODO(email-team): integrate mail provider here, generate signed token and send verification link.
    }

    private String buildUsername(User user) {
        String prenom = user.getPrenom() == null ? "" : user.getPrenom().trim();
        String nom    = user.getNom()    == null ? "" : user.getNom().trim();
        String fullName = (prenom + " " + nom).trim();
        return !fullName.isEmpty() ? fullName : user.getEmail();
    }

    private LoginResponse blockedLoginResponse(User user, String nextStep, String message) {
        return new LoginResponse(
                null,
                user.getId(),
                buildUsername(user),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatutCompte() != null ? user.getStatutCompte().name() : null,
                user.getEmailVerificationStatus() != null ? user.getEmailVerificationStatus().name() : null,
                user.getProfileValidationStatus() != null ? user.getProfileValidationStatus().name() : null,
                nextStep,
                false,
                message
        );
    }

    private String postSignupMessage(User user) {
        if (user.getStatutCompte() == StatutCompte.SUSPENDU) {
            return "Account suspended by administrator";
        }
        if (user.getStatutCompte() == StatutCompte.REFUSE) {
            return user.getMotifRefus() != null && !user.getMotifRefus().isBlank()
                    ? "Account request rejected: " + user.getMotifRefus()
                    : "Account request rejected by administrator";
        }
        if (user.getStatutCompte() == StatutCompte.EN_ATTENTE) {
            return user.getProfileValidationStatus() == ProfileValidationStatus.PENDING_VALIDATION
                    ? "Profile submitted. Your account is awaiting document review by an administrator."
                    : "Profile submitted. Your account is pending administrator review.";
        }
        return "Profile completed successfully. You can now log in.";
    }

    private String normalizePhoneForSms(String telephone) {
        String cleaned = telephone.trim().replace(" ", "");
        if (cleaned.startsWith("+")) return cleaned;
        if (cleaned.length() == 8) return "+216" + cleaned;
        return cleaned;
    }

    @Override
    public TokenValidationResponse validateAuthorizationHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new TokenValidationResponse(false, null, null, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return new TokenValidationResponse(false, null, null, "Token is invalid or expired");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        User user = userId == null ? null : userService.getUser(userId);
        if (user == null) {
            return new TokenValidationResponse(false, null, null, "User not found");
        }

        if (user.getStatutCompte() == StatutCompte.SUSPENDU) {
            return new TokenValidationResponse(false, user.getId(), user.getEmail(), "Account suspended by administrator");
        }

        return new TokenValidationResponse(true, user.getId(), user.getEmail(), "Token is valid");
    }
}
