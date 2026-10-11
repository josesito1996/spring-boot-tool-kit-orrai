package com.library.support.orrai.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.library.support.orrai.exception.BusinessException;
import com.library.support.orrai.exception.ForbiddenException;
import com.library.support.orrai.exception.InternalErrorException;
import com.library.support.orrai.exception.ResourceNotFoundException;
import com.library.support.orrai.exception.UnauthorizedException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
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
    @DisplayName("ResourceNotFoundException -> 404 ApiError")
    void resourceNotFound_returns404() throws Exception {
        mockMvc.perform(get("/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Resource not Found"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/not-found"));
    }

    @Test
    @DisplayName("UnauthorizedException -> 401 ApiError")
    void unauthorized_returns401() throws Exception {
        mockMvc.perform(get("/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("ForbiddenException -> 403 ApiError")
    void forbidden_returns403() throws Exception {
        mockMvc.perform(get("/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("InternalErrorException -> 500 ApiError with its message")
    void internalError_returns500() throws Exception {
        mockMvc.perform(get("/internal"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal error occurred"))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }

    @Test
    @DisplayName("Unanticipated exception -> 500 generic message, no leak")
    void unexpected_returns500Generic() throws Exception {
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.errorCode").value("UNEXPECTED_ERROR"));
    }

    @Test
    @DisplayName("ConstraintViolationException -> ValidationExceptionHandler wins over the catch-all (400)")
    void constraintViolation_handledByValidationAdviceDespiteRegistrationOrder() throws Exception {
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

    @Test
    @DisplayName("BusinessException without properties -> body identical to 0.2.4, field by field")
    void noProperties_bodyUnchangedFrom024() throws Exception {
        String body = mockMvc.perform(get("/not-found"))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> json = JsonPath.read(body, "$");
        assertEquals(new HashSet<>(Arrays.asList("timestamp", "status", "errorCode", "message", "path")),
                json.keySet());
        assertEquals(404, json.get("status"));
        assertEquals("RESOURCE_NOT_FOUND", json.get("errorCode"));
        assertEquals("Resource not Found", json.get("message"));
        assertEquals("/not-found", json.get("path"));
        Instant.parse((String) json.get("timestamp"));
    }

    @Test
    @DisplayName("BusinessException with a property -> key appears verbatim, Long rendered as a number")
    void withProperty_addedToBodyWithExactKeyAndType() throws Exception {
        mockMvc.perform(get("/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DOCUMENTO_DUPLICADO"))
                .andExpect(jsonPath("$.message").value("documento ya registrado"))
                .andExpect(jsonPath("$.paciente_id").isNumber())
                .andExpect(jsonPath("$.paciente_id").value(42))
                .andExpect(jsonPath("$.pacienteId").doesNotExist())
                .andExpect(jsonPath("$.properties").doesNotExist());
    }

    @Test
    @DisplayName("BusinessException with a null-valued property -> key absent from body")
    void withNullProperty_keyAbsent() throws Exception {
        mockMvc.perform(get("/null-property"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.paciente_id").doesNotExist());
    }

    @RestController
    static class TestController {

        @GetMapping("/duplicate")
        void duplicate() {
            throw new BusinessException(409, "DOCUMENTO_DUPLICADO", "documento ya registrado")
                    .withProperty("paciente_id", 42L);
        }

        @GetMapping("/null-property")
        void nullProperty() {
            throw new BusinessException(409, "DOCUMENTO_DUPLICADO", "documento ya registrado")
                    .withProperty("paciente_id", null);
        }

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

    static class SampleRequest {
        @NotBlank
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
