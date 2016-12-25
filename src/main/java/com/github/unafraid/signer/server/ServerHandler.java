/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.unafraid.signer.server;

import com.github.unafraid.signer.server.handlers.HandlerManager;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BiFunction;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;


/**
 * @author UnAfraid
 */
public class ServerHandler extends ChannelHandlerAdapter {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ServerHandler.class);

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            final HttpRequest req = (HttpRequest) msg;

            if (HttpHeaderUtil.is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
            }

            final List<BiFunction<ChannelHandlerContext, HttpRequest, FullHttpResponse>> handlers = HandlerManager.getInstance().getHandler(req.uri());
            for (BiFunction<ChannelHandlerContext, HttpRequest, FullHttpResponse> handler : handlers) {
                final FullHttpResponse response = handler.apply(ctx, req);
                if (response != null) {
                    processResponse(ctx, req, response);
                    return;
                }
            }

            // This shouldn't happen ever!
            processResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND, Unpooled.wrappedBuffer("Default handler was not found".getBytes())));
        }
    }

    private void processResponse(ChannelHandlerContext ctx, HttpRequest req, FullHttpResponse response) {
        if (response.headers().get(CONTENT_TYPE, null) == null) {
            response.headers().set(CONTENT_TYPE, "text/html");
        }
        response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());

        if (!HttpHeaderUtil.isKeepAlive(req)) {
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.write(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}