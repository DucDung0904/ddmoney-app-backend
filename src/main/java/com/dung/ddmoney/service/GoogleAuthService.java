package com.dung.ddmoney.service;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dung.ddmoney.entity.AuthProvider;
import com.dung.ddmoney.entity.User;
import com.dung.ddmoney.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Service
public class GoogleAuthService {

    @Value("${google.client-id}")
    private String clientId;

    @Autowired
    private UserRepository userRepository;

    public User verifyGoogleTokenAndGetUser(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                // Get profile information from payload
                String email = payload.getEmail();
                boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");
                String googleId = payload.getSubject();

                if (!emailVerified) {
                    throw new RuntimeException("Google email is not verified.");
                }

                return processUser(googleId, email, name, pictureUrl);
            } else {
                throw new RuntimeException("Invalid Google ID token.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Google token: " + e.getMessage(), e);
        }
    }

    private User processUser(String googleId, String email, String name, String pictureUrl) {
        // CASE A: User exists by googleId
        Optional<User> userByGoogleId = userRepository.findByGoogleId(googleId);
        if (userByGoogleId.isPresent()) {
            User user = userByGoogleId.get();
            if (!user.isEnabled()) {
                throw new RuntimeException("Account is disabled.");
            }
            return user;
        }

        // CASE B: User exists by email
        Optional<User> userByEmail = userRepository.findByEmail(email);
        if (userByEmail.isPresent()) {
            User user = userByEmail.get();
            if (!user.isEnabled()) {
                throw new RuntimeException("Account is disabled.");
            }
            // Link account
            user.setGoogleId(googleId);
            user.setProvider(AuthProvider.GOOGLE);
            if (user.getAvatarUrl() == null && pictureUrl != null) {
                user.setAvatarUrl(pictureUrl);
            }
            return userRepository.save(user);
        }

        // CASE C: User does not exist, create new
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(email); // Use email as username for simplicity or generate random
        newUser.setFullName(name);
        newUser.setGoogleId(googleId);
        newUser.setProvider(AuthProvider.GOOGLE);
        newUser.setAvatarUrl(pictureUrl);
        newUser.setEnabled(true);
        // Generate a random password since it's a Google account
        newUser.setPassword(UUID.randomUUID().toString());

        return userRepository.save(newUser);
    }
}
