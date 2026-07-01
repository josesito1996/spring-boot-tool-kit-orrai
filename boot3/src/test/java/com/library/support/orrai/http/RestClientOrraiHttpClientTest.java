package com.library.support.orrai.http;

import com.library.support.orrai.exception.OrraiHttpClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestClientOrraiHttpClientTest {

    private static final String BASE_URL = "https://api.example.com";

    private MockRestServiceServer server;
    private OrraiHttpClient client;

    record Greeting(String message) {}

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        // bindTo configures the builder, so the built RestClient picks up the mock transport.
        client = new RestClientOrraiHttpClient(builder.baseUrl(BASE_URL).build());
    }

    @Test
    @DisplayName("execute applies headers and query params and maps the response body")
    void execute_successfulGet_returnsMappedBody() {
        server.expect(requestTo(BASE_URL + "/greetings?lang=es"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("X-Tenant", "orrai"))
                .andExpect(queryParam("lang", "es"))
                .andRespond(withSuccess("{\"message\":\"hola\"}", MediaType.APPLICATION_JSON));

        Greeting result = client.execute(
                "/greetings", "GET", Map.of("X-Tenant", "orrai"), Map.of("lang", "es"), null, Greeting.class);

        assertThat(result.message()).isEqualTo("hola");
        server.verify();
    }

    @Test
    @DisplayName("execute with OrraiTypeRef deserializes a JSON array into a List<T>")
    void execute_listResponse_returnsTypedList() {
        server.expect(requestTo(BASE_URL + "/greetings"))
                .andRespond(withSuccess(
                        "[{\"message\":\"hola\"},{\"message\":\"chau\"}]", MediaType.APPLICATION_JSON));

        List<Greeting> result = client.execute(
                "/greetings", "GET", null, null, null, new OrraiTypeRef<List<Greeting>>() {});

        assertThat(result).extracting(Greeting::message).containsExactly("hola", "chau");
        server.verify();
    }

    @Test
    @DisplayName("execute maps a 4xx response to OrraiHttpClientException preserving the upstream status")
    void execute_clientError_throwsOrraiHttpClientException() {
        server.expect(requestTo(BASE_URL + "/users/99"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"missing\"}"));

        assertThatThrownBy(() ->
                client.execute("/users/99", "GET", null, null, null, Greeting.class))
                .isInstanceOf(OrraiHttpClientException.class)
                .satisfies(ex -> {
                    OrraiHttpClientException httpEx = (OrraiHttpClientException) ex;
                    assertThat(httpEx.getUpstreamStatusCode()).isEqualTo(404);
                    assertThat(httpEx.getStatusCode()).isEqualTo(502); // surfaced as BAD_GATEWAY
                    assertThat(httpEx.getErrorCode()).isEqualTo("UPSTREAM_HTTP_ERROR");
                    assertThat(httpEx.getResponseBody()).contains("missing");
                    assertThat(httpEx.getRequestPath()).isEqualTo("/users/99");
                    // response snapshot: headers captured while the transport stream was still open
                    assertThat(httpEx.getResponseHeaders().get("Content-Type"))
                            .containsExactly("application/json");
                });
        server.verify();
    }

    @Test
    @DisplayName("execute maps a body deserialization mismatch to a deserialization error, not a transport error")
    void execute_deserializationMismatch_throwsWithAccurateMessage() {
        // 200 OK, but a JSON array where a Greeting object is expected -> Jackson MismatchedInputException
        server.expect(requestTo(BASE_URL + "/greetings"))
                .andRespond(withSuccess("[{\"message\":\"hola\"}]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() ->
                client.execute("/greetings", "GET", null, null, null, Greeting.class))
                .isInstanceOf(OrraiHttpClientException.class)
                .hasMessageContaining("could not be deserialized")
                .satisfies(ex -> assertThat(((OrraiHttpClientException) ex).getUpstreamStatusCode()).isZero());
        server.verify();
    }

    @Test
    @DisplayName("execute maps a 5xx response to OrraiHttpClientException")
    void execute_serverError_throwsOrraiHttpClientException() {
        server.expect(requestTo(BASE_URL + "/orders"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() ->
                client.execute("/orders", "POST", null, null, new Greeting("hi"), Greeting.class))
                .isInstanceOf(OrraiHttpClientException.class)
                .satisfies(ex -> assertThat(((OrraiHttpClientException) ex).getUpstreamStatusCode()).isEqualTo(503));
        server.verify();
    }
}
