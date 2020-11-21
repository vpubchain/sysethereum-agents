package org.sysethereum.agents.service.rest;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsExchange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sysethereum.agents.core.syscoin.SyscoinRPCClient;
import org.sysethereum.agents.util.RestError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j(topic = "GetSyscoinRPCHandler")
public class GetSyscoinRPCHandler extends JsonHttpHandler {

    private final Gson gson;
    private final SyscoinRPCClient syscoinRPCClient;

    public GetSyscoinRPCHandler(Gson gson, SyscoinRPCClient syscoinRPCClient) {
        this.gson = gson;
        this.syscoinRPCClient = syscoinRPCClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(HttpExchange httpExchange) throws IOException {
        HttpsExchange httpsExchange = (HttpsExchange) httpExchange;
        if (setOriginAndHandleOptionsMethod(httpsExchange)) return;
        String response;
        Map<String, String> params;

        if (isMatchedApplicationJson(httpsExchange.getRequestHeaders())) {
            params = (Map<String, String>) (Object) getParams(httpsExchange);
        } else {
            params = queryToMap(httpsExchange.getRequestURI().getQuery());
        }
        try {
            String method = params.get("method");
            params.remove("method");
            if (method.equals("signrawtransactionwithkey")) {
                String hexstring = params.get("hexstring");
                String privkey = params.get("privkeys");
                String[] split = privkey.split(",");
                List<String> privkeList = new ArrayList<>();
                for (String s : split) {
                    privkeList.add(s);
                }
                ArrayList<Object> paramList = new ArrayList<>();
                paramList.add(hexstring);
                paramList.add(privkeList);
                response = syscoinRPCClient.makeCoreCall(method, paramList);
            } else {
                ArrayList<Object> paramList = new ArrayList<>(params.values());
                response = syscoinRPCClient.makeCoreCall(method, paramList);
            }
        } catch (Exception e) {
            response = gson.toJson(new RestError(e.toString()));
        }
        writeResponse(httpsExchange, response);
    }
}
