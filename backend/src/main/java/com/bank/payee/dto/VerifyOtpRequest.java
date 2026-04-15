package com.bank.payee.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyOtpRequest {

    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotBlank(message = "OTP code is required")
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
