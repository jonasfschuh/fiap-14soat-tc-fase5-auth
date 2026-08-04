package br.com.fiap.authlambda.support.observability;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudWatchAuthTelemetryTest {

    @Test
    void shouldLogStructuredSuccessAndMetrics() {
        PrintStream original = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            CloudWatchAuthTelemetry telemetry = new CloudWatchAuthTelemetry();
            telemetry.recordLoginSuccess(null, "02772020940", "corr-001", 21);
            telemetry.recordTokenIssued(null, "02772020940", "corr-001");
        } finally {
            System.setOut(original);
        }

        String combined = out.toString();
        assertTrue(combined.contains("\"event\":\"auth_login\""));
        assertTrue(combined.contains("\"authResult\":\"SUCCESS\""));
        assertTrue(combined.contains("\"correlationId\":\"corr-001\""));
        assertTrue(combined.contains("\"auth_login_success_total\":1"));
        assertTrue(combined.contains("\"auth_token_issued_total\":1"));
        assertTrue(combined.contains("\"entity.name\""));
        assertTrue(combined.contains("\"entity.type\":\"AWSLAMBDAFUNCTION\""));
    }

    @Test
    void shouldLogFailureReasonAndMetricDimension() {
        PrintStream original = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            CloudWatchAuthTelemetry telemetry = new CloudWatchAuthTelemetry();
            telemetry.recordLoginFailure(null, "02772020940", "corr-002", "invalid_credentials", 44);
        } finally {
            System.setOut(original);
        }

        String combined = out.toString();
        assertTrue(combined.contains("\"authResult\":\"FAILURE\""));
        assertTrue(combined.contains("\"reason\":\"invalid_credentials\""));
        assertTrue(combined.contains("\"auth_login_failure_total\":1"));
        assertTrue(combined.contains("\"entity.name\""));
    }
}

