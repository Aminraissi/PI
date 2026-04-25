package org.example.gestionuser.auth;

import lombok.AllArgsConstructor;
import org.example.gestionuser.Services.IUser;
import org.example.gestionuser.dtos.LoginResponse;
import org.example.gestionuser.dtos.SignupResponse;
import org.example.gestionuser.dtos.SignupStep1Request;
import org.example.gestionuser.dtos.SignupStep2Request;
import org.example.gestionuser.dtos.TokenValidationResponse;
import org.example.gestionuser.entities.EmailVerificationStatus;
import org.example.gestionuser.entities.ProfileValidationStatus;
import org.example.gestionuser.entities.Role;
import org.example.gestionuser.entities.StatutCompte;
import org.example.gestionuser.entities.User;
import org.example.gestionuser.util.JwtTokenProvider;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@AllArgsConstructor
public class AuthFacadeImpl implements AuthFacade {

    private final IUser userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse login(String email, String motDePasse) {
        User user = userService.findByEmail(email);

        if (user == null) {
            return new LoginResponse(null, null, null, email, null, null, null, null,
                    "LOGIN", false, "No account was found for this email address");
        }

        if (user.getMotDePasse() == null || !user.getMotDePasse().equals(motDePasse)) {
            return new LoginResponse(null, null, null, email, null, null, null, null,
                    "LOGIN", false, "Incorrect email or password");
        }

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
                "Login successful"
        );
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
        user.setMotDePasse(request.getMotDePasse());
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

        User updated = userService.updateUser(user);
        return new SignupResponse(
                updated.getId(),
                updated.getEmail(),
                updated.getRole() != null ? updated.getRole().name() : null,
                updated.getStatutCompte() != null ? updated.getStatutCompte().name() : null,
                updated.getEmailVerificationStatus() != null ? updated.getEmailVerificationStatus().name() : null,
                updated.getProfileValidationStatus() != null ? updated.getProfileValidationStatus().name() : null,
                "VERIFY_EMAIL",
                "Step 2 completed. Please verify your email."
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
        // Current implementation is a dummy switch to allow integration progress.
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

    private void normalizeStatusesAfterEmailVerification(User user) {
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

    private Role parseRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return null;
        }

        String normalized = rawRole
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");

        return switch (normalized) {
            case "FARMER", "AGRICULTEUR" -> Role.AGRICULTEUR;
            case "AGRICULTURALEXPERT", "EXPERTAGRICOLE" -> Role.EXPERT_AGRICOLE;
            case "EVENTORGANIZER", "ORGANISATEUREVENEMENT" -> Role.ORGANISATEUR_EVENEMENT;
            case "TRANSPORTER", "TRANSPORTEUR" -> Role.TRANSPORTEUR;
            case "VETERINARIAN", "VETERINAIRE" -> Role.VETERINAIRE;
            case "ADMIN" -> Role.ADMIN;
            case "BUYER", "ACHETEUR" -> Role.ACHETEUR;
            case "AGENT" -> Role.AGENT;
            default -> null;
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
        String nom = user.getNom() == null ? "" : user.getNom().trim();
        String fullName = (prenom + " " + nom).trim();

        if (!fullName.isEmpty()) {
            return fullName;
        }

        return user.getEmail();
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

        return new TokenValidationResponse(
                true,
                user.getId(),
                user.getEmail(),
                "Token is valid"
        );
    }
}

