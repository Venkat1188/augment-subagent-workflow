package com.bank.payee.controller;

import com.bank.payee.dto.AddPayeeRequest;
import com.bank.payee.dto.InitiateMfaResponse;
import com.bank.payee.dto.VerifyOtpRequest;
import com.bank.payee.dto.VerifyOtpResponse;
import com.bank.payee.model.MfaSession;
import com.bank.payee.model.Payee;
import com.bank.payee.service.MfaService;
import com.bank.payee.service.PayeeService;
import com.bank.payee.service.VerifyResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for MFA-protected payee management.
 *
 * <p>Flow:
 * <ol>
 *   <li>POST /api/payees/initiate-mfa  – create pending payee &amp; start an OTP session.</li>
 *   <li>POST /api/payees/verify-otp    – confirm the OTP to finalise adding the payee.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/payees")
public class PayeeController {

    private final MfaService mfaService;
    private final PayeeService payeeService;

    public PayeeController(MfaService mfaService, PayeeService payeeService) {
        this.mfaService = mfaService;
        this.payeeService = payeeService;
    }

    /**
     * Step 1 – Initiate MFA for a new payee.
     *
     * @param request payee details and chosen MFA method
     * @return session ID and (for demo purposes) the generated OTP
     */
    @PostMapping("/initiate-mfa")
    public ResponseEntity<InitiateMfaResponse> initiateMfa(
            @Valid @RequestBody AddPayeeRequest request) {

        Payee pendingPayee = new Payee(
                request.getPayeeName(),
                request.getAccountNumber(),
                request.getBankCode()
        );

        MfaSession session = mfaService.initiateMfa(pendingPayee, request.getMfaMethod());

        InitiateMfaResponse response = new InitiateMfaResponse(
                session.getSessionId(),
                "OTP sent via " + session.getMfaMethod() + ". Please verify to complete adding the payee.",
                session.getMfaMethod(),
                session.getOtpCode()   // demo only – remove in production
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Step 2 – Verify the OTP and finalise adding the payee.
     *
     * @param request session ID and OTP entered by the user
     * @return verification outcome with appropriate HTTP status
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        VerifyResult result = mfaService.verifyOtp(request.getSessionId(), request.getOtpCode());

        return switch (result.getStatus()) {
            case SUCCESS -> {
                Payee savedPayee = payeeService.addPayee(result.getPayee());
                yield ResponseEntity.ok(
                        VerifyOtpResponse.success("Payee added successfully.", savedPayee));
            }
            case WRONG_OTP -> ResponseEntity.badRequest()
                    .body(VerifyOtpResponse.failure(result.getMessage(), result.getRemainingAttempts()));
            case LOCKED -> ResponseEntity.status(HttpStatus.LOCKED)
                    .body(VerifyOtpResponse.locked(result.getMessage(), result.getLockedUntil()));
            case NOT_FOUND, EXPIRED -> ResponseEntity.badRequest()
                    .body(VerifyOtpResponse.error(result.getMessage()));
        };
    }

    /**
     * GET /api/payees – returns all confirmed payees.
     *
     * @return 200 OK with the list of payees
     */
    @GetMapping
    public ResponseEntity<List<Payee>> getPayees() {
        return ResponseEntity.ok(payeeService.getPayees());
    }

    /**
     * DELETE /api/payees/{id} – removes a payee by its UUID.
     *
     * @param id the UUID of the payee to delete
     * @return 204 No Content on success, 404 Not Found if the payee does not exist
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayee(@PathVariable String id) {
        boolean removed = payeeService.deletePayee(id);
        return removed
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
