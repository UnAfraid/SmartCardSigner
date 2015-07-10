package com.github.unafraid.signer.server.handlers.impl;

import com.github.unafraid.signer.server.handlers.model.URLPattern;
import com.github.unafraid.signer.utils.IOUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Created by UnAfraid on 10.7.2015 г..
 */
public class DefaultHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHandler.class);

    public DefaultHandler() {
    }

    @URLPattern(".*")
    public static FullHttpResponse handleRequest(ChannelHandlerContext ctx, HttpRequest req) {
        final ByteBuf outputBuf = Unpooled.buffer();
        try {
            String template = IOUtils.streamToByteArray(DefaultHandler.class.getResourceAsStream("/index.html"));
            final StringBuilder tableBuilder = new StringBuilder();
            final QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
            final StringBuilder paramsBuilder = new StringBuilder();
            decoder.parameters().entrySet().forEach(entry -> entry.getValue().forEach(value -> paramsBuilder.append("<tr><td>").append(entry.getKey()).append("</td><td>").append(value).append("</td></tr>")));
            req.headers().forEach(httpRequest -> tableBuilder.append("<tr><td>").append(httpRequest.getKey()).append("</td><td>").append(httpRequest.getValue()).append("</td></tr>"));
            template = template.replace("%client%", ctx.channel().toString());
            template = template.replace("%protocolVersion%", req.protocolVersion().protocolName().toString());
            template = template.replace("%uri%", req.uri());
            template = template.replace("%method%", req.method().name().toString());
            template = template.replace("%headers%", tableBuilder.toString());
            template = template.replace("%params%", paramsBuilder.toString());
            outputBuf.writeBytes(template.getBytes());

        } catch (Exception e) {
            LOGGER.warn("Error while reading index.html!", e);
        }

        return new DefaultFullHttpResponse(HTTP_1_1, OK, outputBuf);
    }
}
