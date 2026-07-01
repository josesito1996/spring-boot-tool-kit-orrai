package com.library.support.orrai.http;

import com.library.support.orrai.exception.OrraiHttpClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Spring Boot 3 {@link OrraiHttpClient} adapter backed by {@link RestClient}.
 *
 * <p>Receives a pre-configured {@code RestClient} (base URL, timeouts and message converters are set
 * by {@code OrraiHttpClientFactory}) and, on each call, applies the supplied headers and query
 * parameters, serializes the body, and deserializes the response. All {@code 4xx}/{@code 5xx}
 * responses and transport failures are mapped centrally to a single {@link OrraiHttpClientException},
 * so the upstream transport never leaks to the consumer.
 */
@Slf4j
public class RestClientOrraiHttpClient implements OrraiHttpClient {

    private final RestClient restClient;

    /**
     * @param restClient a fully configured {@link RestClient} (base URL and timeouts already applied)
     */
    public RestClientOrraiHttpClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public <T, R> R execute(
            String path,
            String method,
            Map<String, String> headers,
            Map<String, String> queryParams,
            T body,
            Class<R> responseType) {

        HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase(Locale.ROOT));
        try {
            return retrieve(httpMethod, path, headers, queryParams, body).body(responseType);
        } catch (OrraiHttpClientException ex) {
            throw ex; // already mapped inside the status handler — propagate as-is
        } catch (ResourceAccessException ex) {
            throw transportError(httpMethod, path, ex);
        } catch (RestClientException ex) {
            throw deserializationError(httpMethod, path, ex);
        }
    }

    @Override
    public <T, R> R execute(
            String path,
            String method,
            Map<String, String> headers,
            Map<String, String> queryParams,
            T body,
            OrraiTypeRef<R> responseType) {

        HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase(Locale.ROOT));
        ParameterizedTypeReference<R> typeRef = ParameterizedTypeReference.forType(responseType.getType());
        try {
            return retrieve(httpMethod, path, headers, queryParams, body).body(typeRef);
        } catch (OrraiHttpClientException ex) {
            throw ex; // already mapped inside the status handler — propagate as-is
        } catch (ResourceAccessException ex) {
            throw transportError(httpMethod, path, ex);
        } catch (RestClientException ex) {
            throw deserializationError(httpMethod, path, ex);
        }
    }

    /** Builds the request, applies headers/body, and attaches the central error mapper. */
    private RestClient.ResponseSpec retrieve(
            HttpMethod httpMethod, String path, Map<String, String> headers,
            Map<String, String> queryParams, Object body) {

        RestClient.RequestBodySpec spec = restClient.method(httpMethod)
                .uri(uriBuilder -> {
                    uriBuilder.path(path);
                    if (queryParams != null) {
                        queryParams.forEach(uriBuilder::queryParam);
                    }
                    return uriBuilder.build();
                });

        if (headers != null) {
            headers.forEach(spec::header);
        }
        if (body != null) {
            spec.body(body);
        }

        return spec.retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> mapError(httpMethod, path, response));
    }

    /** Wraps a transport-level failure (no response received) into an {@link OrraiHttpClientException}. */
    private OrraiHttpClientException transportError(HttpMethod method, String path, ResourceAccessException ex) {
        log.error("Transport error calling {} {}", method, path, ex);
        return new OrraiHttpClientException(path, "Unable to reach upstream service at '" + path + "'", ex);
    }

    /**
     * Wraps a response-body extraction failure (the call succeeded but the payload could not be
     * deserialized into the requested type — e.g. a Jackson {@code MismatchedInputException} because
     * the JSON shape does not match) into an {@link OrraiHttpClientException} with an accurate
     * message, so it is not mistaken for a transport error.
     */
    private OrraiHttpClientException deserializationError(HttpMethod method, String path, RestClientException ex) {
        log.error("Failed to deserialize the response body of {} {} into the requested type", method, path, ex);
        return new OrraiHttpClientException(
                path, "Response from '" + path + "' could not be deserialized into the requested type", ex);
    }

    /**
     * Maps an upstream error response into an {@link OrraiHttpClientException}, snapshotting the
     * status, headers and body here (while the transport stream is still open) so the consumer can
     * process the error response after the exchange has closed.
     */
    private static void mapError(HttpMethod method, String path, ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String responseBody = readBody(response);
        Map<String, List<String>> responseHeaders = copyHeaders(response.getHeaders());
        log.warn("Upstream HTTP call {} {} failed with status {}", method, path, status);
        throw new OrraiHttpClientException(status, path, responseBody, responseHeaders,
                "Upstream call to '" + path + "' failed with status " + status);
    }

    /** Snapshots the response headers into a plain map (the live {@code HttpHeaders} is not retained). */
    private static Map<String, List<String>> copyHeaders(HttpHeaders headers) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        headers.forEach((name, values) -> copy.put(name, List.copyOf(values)));
        return copy;
    }

    /** Best-effort read of the response body for diagnostics; never throws. */
    private static String readBody(ClientHttpResponse response) {
        try {
            return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.debug("Could not read upstream error response body", ex);
            return null;
        }
    }
}