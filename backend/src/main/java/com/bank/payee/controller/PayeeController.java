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
import reactor.core.publisher.Mono;

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
                null   // OTP never returned in API response — delivered out-of-band
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Step 2 – Verify the OTP and finalise adding the payee.
     * Returns {@code Mono<ResponseEntity<>>} because {@link PayeeService#addPayee} is reactive.
     *
     * @param request session ID and OTP entered by the user
     * @return verification outcome with appropriate HTTP status
     */
    @PostMapping("/verify-otp")
    public Mono<ResponseEntity<VerifyOtpResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        VerifyResult result = mfaService.verifyOtp(request.getSessionId(), request.getOtpCode());

        return switch (result.getStatus()) {
            case SUCCESS -> payeeService.addPayee(result.getPayee())
                    .map(saved -> ResponseEntity.ok(
                            VerifyOtpResponse.success("Payee added successfully.", saved)));
            case WRONG_OTP -> Mono.just(ResponseEntity.badRequest()
                    .body(VerifyOtpResponse.failure(result.getMessage(), result.getRemainingAttempts())));
            case LOCKED -> Mono.just(ResponseEntity.status(HttpStatus.LOCKED)
                    .body(VerifyOtpResponse.locked(result.getMessage(), result.getLockedUntil())));
            case NOT_FOUND, EXPIRED -> Mono.just(ResponseEntity.badRequest()
                    .body(VerifyOtpResponse.error(result.getMessage())));
        };
    }

    /**
     * GET /api/payees – returns all confirmed payees from the Dapr state store.
     *
     * @return {@code Mono<ResponseEntity<List<Payee>>>} — 200 OK
     */
    @GetMapping
    public Mono<ResponseEntity<List<Payee>>> getPayees() {
        return payeeService.getPayees().map(ResponseEntity::ok);
    }

    /**
     * DELETE /api/payees/{id} – removes a payee from the Dapr state store.
     *
     * @param id the UUID of the payee to delete
     * @return 204 No Content on success, 404 Not Found if the payee does not exist
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deletePayee(@PathVariable String id) {
        return payeeService.deletePayee(id)
                .map(removed -> removed
                        ? ResponseEntity.<Void>noContent().build()
                        : ResponseEntity.<Void>notFound().build());
    }
}
