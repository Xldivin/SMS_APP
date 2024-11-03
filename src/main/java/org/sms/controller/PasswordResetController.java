package org.sms.controller;


import org.sms.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/reset/auth")
public class PasswordResetController {

    @Autowired
    private AuthService authService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> handleForgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (!authService.doesEmailExist(email)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email address not found.");
        }

        authService.deleteExistingResetTokenByEmail(email);

        boolean emailSent = authService.sendPasswordResetEmail(email);
        return emailSent ?
                ResponseEntity.ok("A reset link has been sent to your email.") :
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send email.");
    }

    @GetMapping("/reset-password")
    public ResponseEntity<?> validateResetToken(@RequestParam("token") String token) {
        boolean isValidToken = authService.validatePasswordResetToken(token);
        return isValidToken ? ResponseEntity.ok("Valid token") :
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired token.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        String confirmNewPassword = request.get("confirmNewPassword");

        if (!newPassword.equals(confirmNewPassword)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Passwords do not match.");
        }

        boolean isResetSuccessful = authService.resetUserPassword(token, newPassword);
        return isResetSuccessful ?
                ResponseEntity.ok("Your password has been successfully reset.") :
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to reset password.");
    }
}
