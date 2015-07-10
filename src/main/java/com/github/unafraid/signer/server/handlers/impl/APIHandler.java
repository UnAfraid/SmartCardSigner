package com.github.unafraid.signer.server.handlers.impl;

import com.github.unafraid.signer.server.ServerManager;
import com.github.unafraid.signer.server.handlers.model.URLPattern;
import com.github.unafraid.signer.signer.DocumentSigner;
import com.github.unafraid.signer.signer.model.SignedDocument;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Created by UnAfraid on 10.7.2015 г..
 */
public class APIHandler {
    @URLPattern("/api/?")
    public static FullHttpResponse index() {
        return new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED, Unpooled.wrappedBuffer("Not Authorized!!!".getBytes()));
    }

    @URLPattern("/api/headers")
    public static FullHttpResponse headers(HttpRequest req) {
        final Map<String, String> headers = new LinkedHashMap<>();
        req.headers().forEach(entry -> headers.put(entry.getKey().toString(), entry.getValue().toString()));
        final Gson gson = new GsonBuilder().create();
        final JsonElement element = gson.toJsonTree(headers);
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(element.toString().getBytes(StandardCharsets.UTF_8)));
        response.headers().set(CONTENT_TYPE, "application/json");
        return response;
    }

    @URLPattern("/api/sing/.*")
    public static FullHttpResponse sign(HttpRequest req) {
        final URLDecoder decoder = new URLDecoder();
        String contentToSign;
        try {
            contentToSign = decoder.decode(req.uri().replace("/api/sing/", ""), "UTF-8");
        } catch (Exception e) {
            return null;
        }

        if (contentToSign.isEmpty()) {
            return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, Unpooled.wrappedBuffer("Bad request!".getBytes(StandardCharsets.UTF_8)));
        }

        final DocumentSigner signer = new DocumentSigner();
        final Gson gson = new GsonBuilder().create();
        final SignedDocument result;
        try {
            result = signer.sign(contentToSign.getBytes(), Preferences.userRoot().get(ServerManager.MIDLWARE_PATH, null), System.getProperty(ServerManager.CARD_PIN));
        } catch (Exception e) {
            return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(e.getMessage().getBytes(StandardCharsets.UTF_8)));
        }

        final Map<String, String> data = new LinkedHashMap<>();
        data.put("certificationChain", result.getCertificationChain());
        data.put("signature", result.getSignature());
        final JsonElement element = gson.toJsonTree(data);
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(element.toString().getBytes(StandardCharsets.UTF_8)));
        response.headers().set(CONTENT_TYPE, "application/json");
        return response;
    }
}
