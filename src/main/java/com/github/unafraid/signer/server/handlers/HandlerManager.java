package com.github.unafraid.signer.server.handlers;

import com.github.unafraid.signer.server.handlers.impl.*;
import com.github.unafraid.signer.server.handlers.model.URLPattern;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by UnAfraid on 10.7.2015 г..
 */
public class HandlerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerManager.class);

    private final Map<String, BiFunction<ChannelHandlerContext, HttpRequest, FullHttpResponse>> _handlers = new LinkedHashMap<>();

    protected HandlerManager() {
        registerHandler(new HelloHandler());
        registerHandler(new NotFoundHandler());
        registerHandler(new APIHandler());
        registerHandler(new SignHandler());

        // Registering default handler last!!
        registerHandler(new DefaultHandler());
    }

    public void registerHandler(Object handler) {
        findAnnotation(handler, handler.getClass());
    }

    private void findAnnotation(Object handler, Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(URLPattern.class)) {
                final URLPattern pattern = method.getAnnotation(URLPattern.class);
                _handlers.put(pattern.value(), (ctx, req) -> doCallAndGetReturn(method, handler, ctx, req));
            }
        }
    }

    private FullHttpResponse doCallAndGetReturn(Method method, Object handler, ChannelHandlerContext ctx, HttpRequest request) {
        final int modifiers = method.getModifiers();
        final boolean isStatic = Modifier.isStatic(modifiers);
        try {
            final boolean isAccessible = method.isAccessible();
            if (!isAccessible) {
                method.setAccessible(true);
            }
            final Object[] params = new Object[method.getParameterCount()];
            final Object[] paramTypes = method.getParameterTypes();
            for (int i = 0; i < method.getParameterCount(); i++) {
                if (paramTypes[i] == ChannelHandlerContext.class) {
                    params[i] = ctx;
                } else if (paramTypes[i] == HttpRequest.class) {
                    params[i] = request;
                }
            }
            final FullHttpResponse response = (FullHttpResponse) method.invoke(!isStatic && handler != null ? handler : null, params);
            method.setAccessible(isAccessible);
            return response;
        } catch (Exception e) {
            LOGGER.warn("Failed to execute {}", method.getName());
        }
        return null;
    }

    public List<BiFunction<ChannelHandlerContext, HttpRequest, FullHttpResponse>> getHandler(String uri) {
        return _handlers.entrySet().stream().filter(handler -> uri.matches(handler.getKey())).map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public void forEachHandler(String uri, Consumer<BiFunction<ChannelHandlerContext, HttpRequest, FullHttpResponse>> action) {
        _handlers.entrySet().stream().filter(handler -> uri.matches(handler.getKey())).map(Map.Entry::getValue).forEach(action);
    }

    public static HandlerManager getInstance() {
        return SingletonHolder._instance;
    }

    private static class SingletonHolder {
        protected static final HandlerManager _instance = new HandlerManager();
    }
}
