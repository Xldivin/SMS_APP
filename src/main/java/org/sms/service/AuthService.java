package org.sms.service;

import org.sms.dto.AuthResponse;
import org.sms.model.ResetToken;
import org.sms.model.User;
import org.sms.repository.UserRepository;
import org.sms.repository.ResetTokenRepository;
import org.sms.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResetTokenRepository resetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    public User registerUser(String username, String password, String email) {
        // Check if user already exists
        userRepository.findByUsername(username).ifPresent(user -> {
            throw new IllegalStateException("Username is already taken");
        });


        // Save new user
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);

        return userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Return a Spring Security User object
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), new ArrayList<>()); // Add authorities if applicable
    }

    public AuthResponse authenticateUser(String username, String rawPassword) {
        UserDetails userDetails = loadUserByUsername(username);

        // Validate password
        if (rawPassword.equals(userDetails.getPassword())) {
            String token = JwtUtil.generateToken(new org.springframework.security.core.userdetails.User(
                    userDetails.getUsername(),
                    userDetails.getPassword(),
                    new ArrayList<>()
            ));
            return new AuthResponse("Login successful", token);
        } else {
            throw new IllegalArgumentException("Invalid username or password");
        }
    }

    public boolean doesEmailExist(String email) {
        return userRepository.findByEmail(email) != null;
    }

    @Transactional // Ensure transactional context
    public void deleteExistingResetTokenByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            resetTokenRepository.findByUser(user).ifPresent(resetToken -> {
                resetTokenRepository.delete(resetToken);
                System.out.println("Deleted existing token: " + resetToken.getToken());
            });
        }
    }

    @Transactional // Ensure transactional context
    public boolean sendPasswordResetEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return false; // User not found
        }


        // Generate a new token and save it
        String token = UUID.randomUUID().toString();
        saveResetTokenForUser(user, token);

        // Prepare and send email
        String resetUrl = "http://localhost:8081/reset-password?token=" + token;
        String message = "To reset your password, click the link below:\n" + resetUrl;
        sendEmail(email, "Password Reset", message);

        return true;
    }

    @Transactional // Ensure transactional context
    private void saveResetTokenForUser(User user, String token) {
        ResetToken resetToken = new ResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15)); // Set token expiry to 15 minutes

        resetTokenRepository.save(resetToken);
    }

    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public boolean validatePasswordResetToken(String token) {
        Optional<ResetToken> resetTokenOptional = resetTokenRepository.findByToken(token);
        if (resetTokenOptional.isPresent()) {
            ResetToken resetToken = resetTokenOptional.get();
            boolean isValid = resetToken.getExpiryDate().isAfter(LocalDateTime.now());
            System.out.println("Token: " + token + ", Valid: " + isValid); // Log the token status
            return isValid;
        }
        System.out.println("Token: " + token + " not found."); // Log if token is not found
        return false;
    }

    @Transactional // Ensure transactional context
    public boolean resetUserPassword(String token, String newPassword) {
        // Validate the token
        if (!validatePasswordResetToken(token)) {
            System.out.println("Invalid or expired token: " + token);
            return false; // Token is invalid or expired
        }

        // Find the user associated with the token
        Optional<User> userOptional = findUserByResetToken(token);
        if (!userOptional.isPresent()) {
            System.out.println("No user found for the token: " + token);
            return false;
        }

        User user = userOptional.get();
        user.setPassword(newPassword); // Update the user's password
        userRepository.save(user);

        // Invalidate the token after successful password reset
        resetTokenRepository.deleteByToken(token);

        return true;
    }

    public Optional<User> findUserByResetToken(String token) {
        return resetTokenRepository.findByToken(token)
                .map(ResetToken::getUser); // Directly map the ResetToken to User if present
    }
}

