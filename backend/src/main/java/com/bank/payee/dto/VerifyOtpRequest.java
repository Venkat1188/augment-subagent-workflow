package com.bank.payee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class VerifyOtpRequest {

    @NotBlank(message = "Session ID is required")
    private String sessionId;

    // CWE-20: 6-digit numeric OTP only — prevents oversized payloads and non-numeric guessing
    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP code must be exactly 6 digits")
    @Pattern(regexp = "[0-9]{6}", message = "OTP code must contain exactly 6 digits")
    private String otpCode;

    public VerifyOtpRequest() {}

    public VerifyOtpRequest(String sessionId, String otpCode) {
        this.sessionId = sessionId;
        this.otpCode = otpCode;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}
