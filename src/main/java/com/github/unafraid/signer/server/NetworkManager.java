package com.github.unafraid.signer.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by UnAfraid on 11.7.2015 ã..
 */
public class NetworkManager {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final ServerBootstrap _serverBootstrap;
    private final String _host;
    private final int _port;

    private ChannelFuture _channelFuture;

    public NetworkManager(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ChannelInitializer<SocketChannel> clientInitializer, String host, int port) {
        // @formatter:off
        _serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(clientInitializer)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        // @formatter:on
        _host = host;
        _port = port;
    }

    public ChannelFuture getChannelFuture() {
        return _channelFuture;
    }

    public void start() throws InterruptedException {
        if ((_channelFuture != null) && !_channelFuture.isDone()) {
            return;
        }

        _channelFuture = _serverBootstrap.bind(_host, _port).sync();
        LOGGER.info("Listening on {}:{}", _host, _port);
    }

    public void stop() throws InterruptedException {
        _channelFuture.channel().close().sync();
    }
}