package com.github.unafraid.signer.server.handlers.impl;

import com.github.unafraid.signer.server.ServerManager;
import com.github.unafraid.signer.server.handlers.model.URLPattern;
import com.github.unafraid.signer.signer.DocumentSigner;
import com.github.unafraid.signer.signer.model.SignedDocument;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Created by UnAfraid on 10.7.2015 year..
 */
public class SignHandler {
    @URLPattern("/sing/.*")
    public static FullHttpResponse sign(HttpRequest req) {
        final String contentToSign = req.uri().replace("/api/sing/", "");
        if (contentToSign.isEmpty()) {
            return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, Unpooled.wrappedBuffer("Bad request!".getBytes(StandardCharsets.UTF_8)));
        }

        final DocumentSigner signer = new DocumentSigner();
        final SignedDocument result;
        try {
            result = signer.sign(contentToSign.getBytes(), System.getProperty(ServerManager.MIDLWARE_PATH), System.getProperty(ServerManager.CARD_PIN));
        } catch (Exception e) {
            return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(e.getMessage().getBytes(StandardCharsets.UTF_8)));
        }

        return new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer((result.getCertificationChain() + System.lineSeparator() + "|" + System.lineSeparator() + result.getSignature()).getBytes(StandardCharsets.UTF_8)));
    }
}
