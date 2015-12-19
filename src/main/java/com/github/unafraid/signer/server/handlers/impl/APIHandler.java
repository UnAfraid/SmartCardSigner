package com.github.unafraid.signer.server.handlers.impl;

import com.github.unafraid.signer.gui.controllers.SignController;
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
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Created by UnAfraid on 10.7.2015 г..
 */
public class APIHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(APIHandler.class);
    protected static volatile MessageDigest md = null;

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

    @URLPattern("/api/sign/.*")
    public static FullHttpResponse sign(HttpRequest req) {
        String contentToSign;

        try {
            contentToSign = URLDecoder.decode(req.uri().replace("/api/sign/", ""), "UTF-8");
        } catch (Exception e) {
            return null;
        }

        if (contentToSign.isEmpty()) {
            return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, Unpooled.wrappedBuffer("Bad request!".getBytes(StandardCharsets.UTF_8)));
        }

        FXMLLoader fxmlLoader = new FXMLLoader(Object.class.getResource("/views/Sign.fxml"));
        AtomicBoolean isDone = new AtomicBoolean();
        AtomicReference<String> result = new AtomicReference<>("");
        AtomicReference<String> pinCode = new AtomicReference<>("");
        Platform.runLater(() -> {
            try {
                final Stage stage = new Stage();
                final Scene scene = new Scene(fxmlLoader.load());
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Text Signing Request");
                stage.setScene(scene);
                final SignController controller = fxmlLoader.getController();
                controller.setDomainName("google.com");
                controller.setContentToSign(contentToSign);
                pinCode.set(controller.getPinCode());
                stage.showAndWait();
                if (isDone.compareAndSet(false, true)) {
                    result.set((String) scene.getUserData());
                }
            } catch (IOException e) {
                LOGGER.warn("Error: ", e);
            }
        });

        while (!isDone.get()) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
            }
        }

        final Gson gson = new GsonBuilder().create();
        final Map<String, String> data = new LinkedHashMap<>();

        // data.put("text", contentToSign);

        byte[] hash = sha1(result.get().getBytes());
        // data.put("hash", Arrays.toString(hash));

        final SignedDocument resultDocument;
        try {
            resultDocument = DocumentSigner.sign(hash);
        } catch (Exception e) {
            return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(e.getMessage().getBytes(StandardCharsets.UTF_8)));
        }

        data.put("certificationChain", resultDocument.getCertificationChain());
        data.put("signature", resultDocument.getSignature());

        final JsonElement element = gson.toJsonTree(data);
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(element.toString().getBytes(StandardCharsets.UTF_8)));
        response.headers().set(CONTENT_TYPE, "application/json");
        return response;
    }

    protected static byte[] sha1(byte[] input) {
        if (md == null) {
            synchronized (APIHandler.class) {
                if (md == null) {
                    try {
                        md = MessageDigest.getInstance("SHA-1");
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }
        }

        if (md != null) {
            return md.digest(input);
        }

        return null;
    }
}
