package com.github.unafraid.signer.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author UnAfraid
 */
public class NetworkManager {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private static final String HOSTNAME = System.getenv().getOrDefault("network.hostname", "127.0.0.1");
    private static final boolean SSL = System.getenv().getOrDefault("network.protocol", "").equalsIgnoreCase("ssl");
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("network.port", SSL ? "8443" : "8080"));


    private final ServerBootstrap _serverBootstrap;
    private final String _host;
    private final int _port;

    private ChannelFuture _channelFuture;

    public NetworkManager(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ChannelInitializer<SocketChannel> clientInitializer) {

        SslContext sslCtx = null;
        if (SSL) {
            try {
                final SelfSignedCertificate ssc = new SelfSignedCertificate("localhost");
                sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
            } catch (Exception e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
        // @formatter:off
        _serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(clientInitializer)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ServerInitializer(sslCtx));
        // @formatter:on
        _host = HOSTNAME;
        _port = PORT;
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