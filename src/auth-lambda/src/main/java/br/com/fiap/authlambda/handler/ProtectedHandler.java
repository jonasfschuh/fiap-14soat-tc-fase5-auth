package br.com.fiap.authlambda.handler;

import br.com.fiap.authlambda.support.ApiResponseFactory;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.Map;

public class ProtectedHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        return ApiResponseFactory.json(200, Map.of(
                "message", "request authorized",
                "status", "ok"
        ));
    }
}

