package org.sysethereum.agents.service.rest;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsExchange;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.sysethereum.agents.core.syscoin.SyscoinRPCClient;
import org.sysethereum.agents.util.RestError;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

@Service
@Slf4j(topic = "GetSyscoinRPCHandler")
public class GetSyscoinRPCHandler extends CommonHttpHandler {

    private final Gson gson;
    private final SyscoinRPCClient syscoinRPCClient;

    public GetSyscoinRPCHandler(Gson gson, SyscoinRPCClient syscoinRPCClient) {
        this.gson = gson;
        this.syscoinRPCClient = syscoinRPCClient;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        HttpsExchange httpsExchange = (HttpsExchange) httpExchange;
        if (setOriginAndHandleOptionsMethod(httpsExchange)) return;
        String response;

        if ("post".equalsIgnoreCase(httpsExchange.getRequestMethod())) {
            String postString = IOUtils.toString(httpsExchange.getRequestBody());
            Map<String,String> postInfo = formData2Dic(postString);
            try {
                String method = postInfo.get("method");
                postInfo.remove("method");
                ArrayList<Object> paramList = new ArrayList<>(postInfo.values());
                response = syscoinRPCClient.makeCoreCall(method, paramList);
            } catch (Exception e) {
                response = gson.toJson(new RestError(e.toString()));
            }
        } else {
            LinkedHashMap<String, String> params = queryToMap(httpsExchange.getRequestURI().getQuery());
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
        }
        writeResponse(httpsExchange, response);
    }

    public static Map<String,String> formData2Dic(String formData ) {
        Map<String,String> result = new HashMap<>();
        if(formData== null || formData.trim().length() == 0) {
            return result;
        }
        final String[] items = formData.split("&");
        Arrays.stream(items).forEach(item ->{
            final String[] keyAndVal = item.split("=");
            if( keyAndVal.length == 2) {
                try{
                    final String key = URLDecoder.decode( keyAndVal[0],"utf8");
                    final String val = URLDecoder.decode( keyAndVal[1],"utf8");
                    result.put(key,val);
                }catch (UnsupportedEncodingException e) {}
            }
        });
        return result;
    }
}
