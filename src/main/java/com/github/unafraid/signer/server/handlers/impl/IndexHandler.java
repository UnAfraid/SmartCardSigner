package com.github.unafraid.signer.server.handlers.impl;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.unafraid.signer.server.handlers.IHttpRouteHandler;
import com.github.unafraid.signer.server.handlers.model.HttpMethodType;
import com.github.unafraid.signer.server.handlers.model.Route;
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
@Route(types =
{
	HttpMethodType.GET,
	HttpMethodType.POST
}, paths = "/")
public class IndexHandler implements IHttpRouteHandler
{
	private static final Logger LOGGER = LoggerFactory.getLogger(IndexHandler.class);
	
	public IndexHandler()
	{
	}
	
	@Override
	public FullHttpResponse onRequest(ChannelHandlerContext ctx, HttpRequest req, RouteResult<IHttpRouteHandler> routeResult)
	{
		final ByteBuf outputBuf = Unpooled.buffer();
		try
		{
			String template = IOUtils.streamToByteArray(IndexHandler.class.getResourceAsStream("/index.html"));
			final StringBuilder tableBuilder = new StringBuilder();
			final StringBuilder paramsBuilder = new StringBuilder();
			final Map<String, List<String>> params = IOUtils.parseHttpRequest(req);
			params.entrySet().forEach(entry -> entry.getValue().forEach(value -> paramsBuilder.append("<tr><td>").append(entry.getKey()).append("</td><td>").append(value).append("</td></tr>")));
			
			req.headers().forEach(httpRequest -> tableBuilder.append("<tr><td>").append(httpRequest.getKey()).append("</td><td>").append(httpRequest.getValue()).append("</td></tr>"));
			template = template.replace("%client%", ctx.channel().toString());
			template = template.replace("%protocolVersion%", req.protocolVersion().protocolName().toString());
			template = template.replace("%uri%", req.uri());
			template = template.replace("%method%", req.method().name().toString());
			template = template.replace("%headers%", tableBuilder.toString());
			template = template.replace("%params%", paramsBuilder.toString());
			outputBuf.writeBytes(template.getBytes());
		}
		catch (Exception e)
		{
			LOGGER.warn("Error while reading index.html!", e);
		}
		
		return new DefaultFullHttpResponse(HTTP_1_1, OK, outputBuf);
	}
}
