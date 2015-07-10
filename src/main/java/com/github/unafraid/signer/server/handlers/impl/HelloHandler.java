package com.github.unafraid.signer.server.handlers.impl;

import com.github.unafraid.signer.server.handlers.model.URLPattern;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Created by UnAfraid on 10.7.2015 г..
 */
public class HelloHandler {
    public HelloHandler() {
    }

    @URLPattern("/helloworld/?")
    public FullHttpResponse index() {
        return new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer("Hello World!".getBytes()));
    }


    @URLPattern("/hello/[a-zA-Z]{3,3}[A-Za-z0-9]*")
    public FullHttpResponse parameterized(HttpRequest req) {
        return new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(("Hello " + req.uri().replaceAll("/hello/", "")).getBytes()));
    }
}
