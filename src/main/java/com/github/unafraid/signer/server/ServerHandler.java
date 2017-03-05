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

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.unafraid.signer.server.handlers.IHttpRouteHandler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.router.RouteResult;
import io.netty.handler.codec.http.router.Router;

/**
 * @author UnAfraid
 */
public class ServerHandler extends ChannelInboundHandlerAdapter
{
	protected static final Logger LOGGER = LoggerFactory.getLogger(ServerHandler.class);
	private final Router<IHttpRouteHandler> _router;
	
	public ServerHandler(Router<IHttpRouteHandler> router)
	{
		_router = router;
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx)
	{
		ctx.flush();
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		if (msg instanceof HttpRequest)
		{
			final HttpRequest req = (HttpRequest) msg;
			
			if (HttpUtil.is100ContinueExpected(req))
			{
				ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
			}
			
			final RouteResult<IHttpRouteHandler> routeResult = _router.route(req.method(), req.uri());
			final FullHttpResponse response = routeResult.target().onRequest(ctx, req, routeResult);
			if (response.headers().get(CONTENT_TYPE, null) == null)
			{
				response.headers().set(CONTENT_TYPE, "text/html");
			}
			response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
			
			if (!HttpUtil.isKeepAlive(req))
			{
				ctx.write(response).addListener(ChannelFutureListener.CLOSE);
			}
			else
			{
				response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
				ctx.write(response);
			}
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		LOGGER.warn("exceptionCaught", cause);
		ctx.close();
	}
}