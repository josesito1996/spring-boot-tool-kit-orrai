package com.library.support.orrai.handler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.library.support.orrai.exception.ForbiddenException;
import com.library.support.orrai.exception.InternalErrorException;
import com.library.support.orrai.exception.ResourceNotFoundException;
import com.library.support.orrai.exception.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("ResourceNotFoundException -> 404 problem+json")
    void resourceNotFound_returns404ProblemDetail() throws Exception {
        mockMvc.perform(get("/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Resource not Found"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/not-found"));
    }

    @Test
    @DisplayName("UnauthorizedException -> 401 problem+json")
    void unauthorized_returns401ProblemDetail() throws Exception {
        mockMvc.perform(get("/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("ForbiddenException -> 403 problem+json")
    void forbidden_returns403ProblemDetail() throws Exception {
        mockMvc.perform(get("/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("InternalErrorException -> 500 problem+json with its message")
    void internalError_returns500ProblemDetail() throws Exception {
        mockMvc.perform(get("/internal"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Internal error occurred"))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }

    @Test
    @DisplayName("Unanticipated exception -> 500 generic message, no leak")
    void unexpected_returns500GenericProblemDetail() throws Exception {
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.errorCode").value("UNEXPECTED_ERROR"));
    }

    @Test
    @DisplayName("ConstraintViolationException -> ValidationExceptionHandler wins over the catch-all (400)")
    void constraintViolation_handledByValidationAdviceDespiteRegistrationOrder() throws Exception {
        // GlobalExceptionHandler registered FIRST on purpose: only its lower @Order keeps its
        // catch-all from shadowing ValidationExceptionHandler.
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler(), new ValidationExceptionHandler())
                .build();
        mvc.perform(get("/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("@Valid body failure -> 400 with field errors")
    void invalidBody_returns400WithErrors() throws Exception {
        mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @RestController
    static class TestController {

        @GetMapping("/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Resource not Found");
        }

        @GetMapping("/unauthorized")
        void unauthorized() {
            throw new UnauthorizedException("without authorization");
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new ForbiddenException("insufficient permissions");
        }

        @GetMapping("/internal")
        void internal() {
            throw new InternalErrorException("Internal error occurred");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("low-level detail that must not leak");
        }

        @GetMapping("/constraint")
        void constraint() {
            throw new ConstraintViolationException("constraint failed", Collections.emptySet());
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody SampleRequest request) {
            // no-op; validation runs before the method body
        }
    }

    record SampleRequest(@NotBlank String name) {
    }
}
