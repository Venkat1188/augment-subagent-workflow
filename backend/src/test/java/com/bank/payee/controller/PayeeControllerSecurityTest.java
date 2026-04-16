package com.bank.payee.controller;

import com.bank.payee.config.SecurityConfig;
import com.bank.payee.service.MfaService;
import com.bank.payee.service.PayeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security-focused tests for PayeeController (Spring Boot 4 compatible).
 *
 * <p>Uses {@code @ContextConfiguration} + {@code webAppContextSetup} instead of
 * {@code @WebMvcTest} because Spring Boot 4 removed the test-autoconfigure web slice.
 * {@code @MockitoBean} (Spring Framework 6.2+) replaces the removed {@code @MockBean}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PayeeControllerSecurityTest.WebMvcTestConfig.class,
        PayeeController.class,
        SecurityConfig.class
})
@WebAppConfiguration
class PayeeControllerSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private MfaService mfaService;

    @MockitoBean
    private PayeeService payeeService;

    private MockMvc mockMvc;

    /** Minimal Spring MVC config that registers Jackson message converters. */
    @Configuration
    @EnableWebMvc
    static class WebMvcTestConfig {}

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

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

        // Mono<ResponseEntity<>> is async — use asyncDispatch to get real status code
        // accept(APPLICATION_JSON) required so the message converter can be selected
        MvcResult mvcResult = mockMvc.perform(get("/api/payees")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
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
