package com.opsdesk.opsdeskbackend.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.opsdesk.opsdeskbackend.common.ConflictException;
import com.opsdesk.opsdeskbackend.common.UnauthenticatedException;
import com.opsdesk.opsdeskbackend.security.AuthUser;
import com.opsdesk.opsdeskbackend.security.JwtService;
import com.opsdesk.opsdeskbackend.user.AppUser;
import com.opsdesk.opsdeskbackend.user.AppUserRepository;
import com.opsdesk.opsdeskbackend.user.UserRole;

@Service
public class AuthService {

    // Logs ids only: never a password, a token, or an email address.
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    static final String EMAIL_TAKEN = "An account with this email address already exists.";
    static final String WRONG_LOGIN = "The email address or password is wrong.";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Checked when the email is not registered, so that answer takes as long
    // as a wrong password and the timing does not give away who has an account.
    private final String noUserHash;

    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.noUserHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    // Always makes a USER. Only an admin can give someone another role.
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        if (appUserRepository.existsByEmail(email)) {
            throw new ConflictException(EMAIL_TAKEN);
        }
        AppUser user = new AppUser(request.name(), email, "", UserRole.USER, now());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        try {
            user = appUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Someone registered the same email in the moment since the check.
            throw new ConflictException(EMAIL_TAKEN);
        }
        log.info("User {} registered", user.getId());
        return AuthResponse.from(jwtService.issue(user), user);
    }

    // An unknown email, a user with no password and a wrong password all get
    // the same answer, so it does not reveal which emails have accounts.
    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmail(request.email().toLowerCase(Locale.ROOT)).orElse(null);
        String hash = user == null || user.getPasswordHash() == null ? noUserHash : user.getPasswordHash();
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (user == null || user.getPasswordHash() == null || !passwordMatches) {
            // WARN: many of these in a row can mean someone is guessing
            // passwords. The email is not logged; it may be a typed password.
            if (user == null) {
                log.warn("Login failed: no account with that email");
            } else {
                log.warn("Login failed for user {}", user.getId());
            }
            throw new UnauthenticatedException(WRONG_LOGIN);
        }
        return AuthResponse.from(jwtService.issue(user), user);
    }

    // The token is still valid but the account may have been deleted since.
    public AppUser findCurrentUser(AuthUser authUser) {
        return appUserRepository.findById(authUser.id())
                .orElseThrow(() -> new UnauthenticatedException("Log in to continue."));
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
