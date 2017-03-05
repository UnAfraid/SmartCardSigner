package com.github.unafraid.signer.server;

import java.util.Set;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.unafraid.signer.server.handlers.IHttpRouteHandler;
import com.github.unafraid.signer.server.handlers.model.HttpMethodType;
import com.github.unafraid.signer.server.handlers.model.Route;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.router.Router;

/**
 * @author UnAfraid
 */
public class RouterInitializer
{
	private static final Logger LOGGER = LoggerFactory.getLogger(RouterInitializer.class);
	private final Router<IHttpRouteHandler> _router = new Router<>();
	
	public RouterInitializer()
	{
		init();
	}
	
	private void init()
	{
		final Reflections reflections = new Reflections("com.github.unafraid.signer");
		final Set<Class<? extends IHttpRouteHandler>> handlers = reflections.getSubTypesOf(IHttpRouteHandler.class);
		for (Class<? extends IHttpRouteHandler> clazzHandler : handlers)
		{
			if (clazzHandler.isAnnotationPresent(Route.class))
			{
				try
				{
					final Route routeAnnotation = clazzHandler.getAnnotation(Route.class);
					final IHttpRouteHandler handler = clazzHandler.newInstance();
					for (HttpMethodType type : routeAnnotation.types())
					{
						for (String path : routeAnnotation.paths())
						{
							_router.addRoute(type.getMethod(), path, handler);
						}
					}
					LOGGER.info("Initialized path: {} methods: {} handler: {}", routeAnnotation.paths(), routeAnnotation.types(), handler.getClass().getSimpleName());
					
				}
				catch (Exception e)
				{
					LOGGER.warn("Failed to initialize handler: {}", clazzHandler.getSimpleName(), e);
				}
			}
		}
		_router.notFound((ctx, req, routeResult) -> new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, Unpooled.wrappedBuffer("Page not found!".getBytes())));
	}
	
	public Router<IHttpRouteHandler> getRouter()
	{
		return _router;
	}
}
