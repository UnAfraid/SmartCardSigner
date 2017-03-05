package com.github.unafraid.signer.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

/**
 * Created by UnAfraid on 10.7.2015 г..
 */
public class IOUtils
{
	public static String streamToByteArray(InputStream stream) throws IOException
	{
		final char[] buffer = new char[8192];
		final StringBuilder sb = new StringBuilder();
		try (InputStreamReader in = new InputStreamReader(stream))
		{
			while (in.read(buffer, 0, buffer.length) > 0)
			{
				sb.append(buffer);
			}
		}
		return sb.toString();
	}
	
	public static Map<String, List<String>> parseHttpRequest(HttpRequest req) throws IOException
	{
		final Map<String, List<String>> result = new HashMap<>();
		
		if (req.method() == HttpMethod.POST)
		{
			final HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(req);
			for (InterfaceHttpData data : decoder.getBodyHttpDatas())
			{
				try
				{
					if (InterfaceHttpData.HttpDataType.Attribute == data.getHttpDataType())
					{
						final Attribute attribute = (Attribute) data;
						result.computeIfAbsent(attribute.getName(), key -> new ArrayList<>()).add(attribute.getValue());
					}
				}
				finally
				{
					data.release();
				}
			}
		}
		else if (req.method() == HttpMethod.GET)
		{
			final QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
			decoder.parameters().entrySet().forEach(entry -> entry.getValue().forEach(value -> result.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).add(value)));
		}
		return result;
	}
}
