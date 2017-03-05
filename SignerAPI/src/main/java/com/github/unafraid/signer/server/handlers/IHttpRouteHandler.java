package com.github.unafraid.signer.server.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.router.RouteResult;

public interface IHttpRouteHandler
{
	public FullHttpResponse onRequest(ChannelHandlerContext ctx, HttpRequest req, RouteResult<IHttpRouteHandler> routeResult);
}
