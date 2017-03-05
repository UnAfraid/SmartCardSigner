package com.github.unafraid.signer.server;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.unafraid.signer.server.listeners.ISignRequestListener;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * @author UnAfraid
 */
public class NetworkManager
{
	private final Logger LOGGER = LoggerFactory.getLogger(NetworkManager.class);
	private static final String HOSTNAME = System.getenv().getOrDefault("network.hostname", "127.0.0.1");
	private static final boolean SSL = System.getenv().getOrDefault("network.protocol", "").equalsIgnoreCase("ssl");
	private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("network.port", SSL ? "8443" : "8080"));
	
	private final ServerBootstrap _serverBootstrap;
	private final String _host;
	private final int _port;
	
	private ChannelFuture _channelFuture;
	private final List<ISignRequestListener> _listeners = new ArrayList<>();
	
	public NetworkManager()
	{
		SslContext sslCtx = null;
		if (SSL)
		{
			try
			{
				SelfSignedCertificate ssc = new SelfSignedCertificate();
				sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
			}
			catch (Exception e)
			{
				LOGGER.warn(e.getMessage(), e);
			}
		}
		
		// @formatter:off
        _serverBootstrap = new ServerBootstrap()
                .group(new NioEventLoopGroup(1), new NioEventLoopGroup(1))
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ServerInitializer(sslCtx, new RouterInitializer()));
        // @formatter:on
		_host = HOSTNAME;
		_port = PORT;
	}
	
	public ChannelFuture getChannelFuture()
	{
		return _channelFuture;
	}
	
	public void start() throws InterruptedException
	{
		if ((_channelFuture != null) && !_channelFuture.isDone())
		{
			return;
		}
		
		_channelFuture = _serverBootstrap.bind(_host, _port).sync();
		LOGGER.info("Listening on {}:{}", _host, _port);
	}
	
	public void stop() throws InterruptedException
	{
		_channelFuture.channel().close().sync();
	}
	
	public void addListener(ISignRequestListener listener)
	{
		_listeners.add(listener);
	}
	
	public List<ISignRequestListener> getListeners()
	{
		return _listeners;
	}
	
	public static NetworkManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final NetworkManager INSTANCE = new NetworkManager();
	}
}