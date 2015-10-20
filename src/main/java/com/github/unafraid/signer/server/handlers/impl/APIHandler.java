package com.github.unafraid.signer.server.handlers.impl;

import com.github.unafraid.signer.gui.controllers.SignController;
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
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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

        // show form
        System.out.println("show form start");
        FXMLLoader fxmlLoader = new FXMLLoader(Object.class.getResource("/views/Sign.fxml"));

        Platform.runLater(() -> {
            try {
                final Stage stage = new Stage();
                final Scene scene = new Scene(fxmlLoader.load());
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("ABC");
                stage.setScene(scene);
                final SignController controller = fxmlLoader.getController();
                controller.setDomainName("google.com");
                controller.setContentToSign(contentToSign);
                stage.showAndWait();
                System.out.println("closed dialog");
                System.out.println(scene.getUserData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        System.out.println("show form end");

        if (System.getenv("x") == null) {
            return null;
        }

        final Gson gson = new GsonBuilder().create();
        final Map<String, String> data = new LinkedHashMap<>();

        data.put("text", contentToSign);

        byte[] hash = sha1(contentToSign.getBytes());
        data.put("hash", Arrays.toString(hash));

        final DocumentSigner signer = new DocumentSigner();
        final SignedDocument result;

        try {
            result = signer.sign(hash, Preferences.userRoot().get(ServerManager.MIDLWARE_PATH, null), System.getProperty(ServerManager.CARD_PIN));
        } catch (Exception e) {
            return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(e.getMessage().getBytes(StandardCharsets.UTF_8)));
        }

        data.put("certificationChain", result.getCertificationChain());
        data.put("signature", result.getSignature());

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
