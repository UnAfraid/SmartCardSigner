package com.github.unafraid.signer.server.handlers.impl;

import static io.netty.handler.codec.http.HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.unafraid.signer.DocumentSigner;
import com.github.unafraid.signer.gui.controllers.SignController;
import com.github.unafraid.signer.model.SignedDocument;
import com.github.unafraid.signer.server.handlers.IHttpRouteHandler;
import com.github.unafraid.signer.server.handlers.model.HttpMethodType;
import com.github.unafraid.signer.server.handlers.model.Route;
import com.github.unafraid.signer.utils.IOUtils;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.router.RouteResult;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * @author UnAfraid
 */
@Route(types = HttpMethodType.POST, paths = "/api/sign")
public class APIHandler implements IHttpRouteHandler
{
	private static final Logger LOGGER = LoggerFactory.getLogger(APIHandler.class);
	
	@Override
	public FullHttpResponse onRequest(ChannelHandlerContext ctx, HttpRequest req, RouteResult<IHttpRouteHandler> routeResult)
	{
		String contentToSign;
		
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
		}
		catch (Exception e)
		{
			return null;
		}
		
		if (contentToSign.isEmpty())
		{
			return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, Unpooled.wrappedBuffer("Bad request!".getBytes(StandardCharsets.UTF_8)));
		}
		
		FXMLLoader fxmlLoader = new FXMLLoader(Object.class.getResource("/views/Sign.fxml"));
		AtomicBoolean isDone = new AtomicBoolean();
		AtomicReference<String> result = new AtomicReference<>("");
		AtomicReference<String> pinCode = new AtomicReference<>("");
		Platform.runLater(() ->
		{
			try
			{
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
				if (isDone.compareAndSet(false, true))
				{
					result.set((String) scene.getUserData());
				}
			}
			catch (IOException e)
			{
				LOGGER.warn("Error: ", e);
			}
		});
		
		while (!isDone.get())
		{
			try
			{
				Thread.sleep(100L);
			}
			catch (InterruptedException e)
			{
			}
		}
		
		final SignedDocument resultDocument;
		try
		{
			resultDocument = DocumentSigner.sign(contentToSign.getBytes());
		}
		catch (Exception e)
		{
			return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(e.getMessage().getBytes(StandardCharsets.UTF_8)));
		}
		
		final DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(Stream.of(resultDocument.getSignedData().split("(?<=\\G.{64})")).collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8)));
		response.headers().set(CONTENT_TYPE, "text/plain; charset=us-ascii").set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		return response;
	}
}
