package com.github.unafraid.signer.server.handlers.impl;

import com.github.unafraid.signer.server.handlers.model.URLPattern;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Created by UnAfraid on 10.7.2015 г..
 */
public class NotFoundHandler {
    public NotFoundHandler() {
    }

    @URLPattern("/404/?.*")
    public FullHttpResponse index() {
        return new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND, Unpooled.wrappedBuffer("Page not found!".getBytes()));
    }
}
