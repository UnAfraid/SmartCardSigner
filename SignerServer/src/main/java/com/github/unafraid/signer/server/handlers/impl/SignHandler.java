package com.github.unafraid.signer.server.handlers.impl;

import static io.netty.handler.codec.http.HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.unafraid.signer.DocumentSigner;
import com.github.unafraid.signer.model.SignedDocument;
import com.github.unafraid.signer.server.NetworkManager;
import com.github.unafraid.signer.server.handlers.IHttpRouteHandler;
import com.github.unafraid.signer.server.handlers.model.HttpMethodType;
import com.github.unafraid.signer.server.handlers.model.Route;
import com.github.unafraid.signer.server.listeners.ISignRequestListener;
import com.github.unafraid.signer.utils.IOUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.router.RouteResult;

/**
 * @author UnAfraid
 */
@Route(types = HttpMethodType.POST, paths = "/api/sign")
public class SignHandler implements IHttpRouteHandler
{
	@Override
	public FullHttpResponse onRequest(ChannelHandlerContext ctx, HttpRequest req, RouteResult<IHttpRouteHandler> routeResult)
	{
		String contentToSign;
		String domain = "google.com";
		try
		{
			final Map<String, List<String>> requestParams = IOUtils.parseHttpRequest(req);
			if (requestParams.isEmpty() || requestParams.values().isEmpty())
			{
				return new DefaultFullHttpResponse(HTTP_1_1, NO_CONTENT, Unpooled.wrappedBuffer("No Content!".getBytes(StandardCharsets.UTF_8)));
			}
			
			final List<String> dataToSign = requestParams.get("data");
			if ((dataToSign == null) || dataToSign.isEmpty())
			{
				return new DefaultFullHttpResponse(HTTP_1_1, NO_CONTENT, Unpooled.wrappedBuffer("No 'data' Content!".getBytes(StandardCharsets.UTF_8)));
			}
			contentToSign = dataToSign.get(0);
			
			final List<String> domains = requestParams.get("domain");
			if ((domains != null) && !domains.isEmpty())
			{
				domain = domains.get(0);
			}
		}
		catch (Exception e)
		{
			return null;
		}
		
		if (contentToSign.isEmpty())
		{
			return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, Unpooled.wrappedBuffer("Bad request!".getBytes(StandardCharsets.UTF_8)));
		}
		
		for (ISignRequestListener request : NetworkManager.getInstance().getListeners())
		{
			if (request.processRequest(domain, contentToSign))
			{
				final SignedDocument resultDocument;
				try
				{
					resultDocument = DocumentSigner.sign(contentToSign.getBytes());
				}
				catch (Exception e)
				{
					return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(e.getMessage().getBytes(StandardCharsets.UTF_8)));
				}
				
				final ByteBuf buffer = Unpooled.wrappedBuffer(Stream.of(resultDocument.getSignedData().split("(?<=\\G.{64})")).collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8));
				final DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, buffer);
				//@formatter:off
				response.headers()
					.set(CONTENT_TYPE, "text/plain; charset=us-ascii")
					.set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
				//@formatter:on
				return response;
			}
		}
		
		return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer("No match".getBytes(StandardCharsets.UTF_8)));
	}
}
