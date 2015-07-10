package com.github.unafraid.signer.server.handlers.impl;

import com.github.unafraid.signer.server.ServerManager;
import com.github.unafraid.signer.server.handlers.model.URLPattern;
import com.github.unafraid.signer.signer.DocumentSigner;
import com.github.unafraid.signer.signer.model.SignedDocument;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.prefs.Preferences;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Created by UnAfraid on 10.7.2015 year..
 */
public class SignHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SignHandler.class);

    @URLPattern("/sign/.*")
    public static FullHttpResponse sign(HttpRequest req) {
        final URLDecoder decoder = new URLDecoder();
        String contentToSign;
        try {
            contentToSign = decoder.decode(req.uri().replace("/sign/", ""), "UTF-8");
        } catch (Exception e) {
            return null;
        }
        if (contentToSign.isEmpty()) {
            return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, Unpooled.wrappedBuffer("Bad request!".getBytes(StandardCharsets.UTF_8)));
        }

        final DocumentSigner signer = new DocumentSigner();
        final SignedDocument result;
        try {
            result = signer.sign(contentToSign.getBytes(), Preferences.userRoot().get(ServerManager.MIDLWARE_PATH, null), System.getProperty(ServerManager.CARD_PIN));
        } catch (Exception e) {
            LOGGER.info("Failed to sign: ", e.getMessage());
            return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(e.getMessage().getBytes(StandardCharsets.UTF_8)));
        }

        return new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer((result.getCertificationChain() + System.lineSeparator() + "|" + System.lineSeparator() + result.getSignature()).getBytes(StandardCharsets.UTF_8)));
    }
}
