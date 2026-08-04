package br.com.fiap.authlambda.support.observability;

import com.amazonaws.services.lambda.runtime.Context;

public interface AuthTelemetry {

    void recordLoginSuccess(Context context, String cpf, String correlationId, long latencyMs);

    void recordLoginFailure(Context context, String cpf, String correlationId, String reason, long latencyMs);

    void recordTokenIssued(Context context, String cpf, String correlationId);
}

