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

import com.github.unafraid.signer.server.handlers.IHttpRouteHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.BadClientSilencer;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.router.Router;
import io.netty.handler.ssl.SslContext;

public class ServerInitializer extends ChannelInitializer<SocketChannel>
{
	private final SslContext _sslCtx;
	private final Router<IHttpRouteHandler> _router;
	private final BadClientSilencer badClientSilencer = new BadClientSilencer();
	
	public ServerInitializer(SslContext sslCtx, RouterInitializer initializer)
	{
		_sslCtx = sslCtx;
		_router = initializer.getRouter();
	}
	
	@Override
	public void initChannel(SocketChannel ch)
	{
		final ChannelPipeline p = ch.pipeline();
		if (_sslCtx != null)
		{
			p.addLast(_sslCtx.newHandler(ch.alloc()));
		}
		p.addLast(new HttpServerCodec());
		p.addLast(new HttpObjectAggregator(1048576));
		p.addLast(new ServerHandler(_router));
		p.addLast(badClientSilencer);
	}
}