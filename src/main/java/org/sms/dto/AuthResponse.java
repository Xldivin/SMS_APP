package org.sms.dto;

public class AuthResponse {
    private String message;
    private String token;

    public AuthResponse(String message, String token){
    this.message = message;
    this.token = token;
    }

    // Getter
    public String getMessage() {
        return message;
    }
    public String getToken() {
        return token;
    }
}

