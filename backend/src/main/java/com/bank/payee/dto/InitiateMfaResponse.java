package com.bank.payee.dto;

import com.bank.payee.model.MfaMethod;

public class InitiateMfaResponse {

    private String sessionId;
    private String message;
    private MfaMethod mfaMethod;
    /** Included for demo/testing only – would NOT be returned in production. */
    private String demoOtp;

    public InitiateMfaResponse() {}

    public InitiateMfaResponse(String sessionId, String message, MfaMethod mfaMethod, String demoOtp) {
        this.sessionId = sessionId;
        this.message = message;
        this.mfaMethod = mfaMethod;
        this.demoOtp = demoOtp;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public MfaMethod getMfaMethod() { return mfaMethod; }
    public void setMfaMethod(MfaMethod mfaMethod) { this.mfaMethod = mfaMethod; }

    public String getDemoOtp() { return demoOtp; }
    public void setDemoOtp(String demoOtp) { this.demoOtp = demoOtp; }
}
