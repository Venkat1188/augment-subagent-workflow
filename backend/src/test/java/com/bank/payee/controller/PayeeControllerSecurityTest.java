package com.bank.payee.controller;

import com.bank.payee.config.SecurityConfig;
import com.bank.payee.service.MfaService;
import com.bank.payee.service.PayeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import reactor.core.publisher.Mono;

import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security-focused tests for PayeeController.
 * Verifies that the GET and DELETE endpoints reject unauthenticated requests
 * and permit authenticated ones.
 */
@WebMvcTest(PayeeController.class)
@Import(SecurityConfig.class)
class PayeeControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MfaService mfaService;

    @MockBean
    private PayeeService payeeService;

    // -------------------------------------------------------------------------
    // GET /api/payees — unauthenticated
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/payees should return 401 when request is unauthenticated")
    void test_getPayees_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/payees"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /api/payees — authenticated
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser
    @DisplayName("GET /api/payees should return 200 when user is authenticated")
    void test_getPayees_authenticated_returns200() throws Exception {
        when(payeeService.getPayees()).thenReturn(Mono.just(Collections.emptyList()));

        mockMvc.perform(get("/api/payees"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/payees/{id} — unauthenticated
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/payees/{id} should return 401 when request is unauthenticated")
    void test_deletePayee_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/payees/some-uuid"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/payees/{id} — authenticated
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/payees/{id} should return 204 when authenticated and payee exists")
    void test_deletePayee_authenticated_existingId_returns204() throws Exception {
        when(payeeService.deletePayee("some-uuid")).thenReturn(Mono.just(true));

        // Mono<ResponseEntity<Void>> is async — must use asyncDispatch to get the real status code
        MvcResult mvcResult = mockMvc.perform(delete("/api/payees/some-uuid"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNoContent());
    }
}
