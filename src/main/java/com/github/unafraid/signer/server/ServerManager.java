/*
 * Copyright 2012 The Netty Project
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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * @author UnAfraid
 */
public final class ServerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerManager.class);
    private static final String HOSTNAME = System.getProperty("network.hostname", "127.0.0.1");
    private static final boolean SSL = System.getProperty("network.protocol", "").equalsIgnoreCase("ssl");
    private static final int PORT = Integer.parseInt(System.getProperty("network.port", SSL ? "8443" : "8080"));

    private ServerManager() {
        init();
    }

    private void init() {
        SslContext sslCtx = null;
        if (SSL) {
            try {
                final SelfSignedCertificate ssc = new SelfSignedCertificate("localhost");
                sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
            } catch (Exception e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }

        InetAddress listenAddress;
        try {
            listenAddress = Inet4Address.getByName(HOSTNAME);
        } catch (Exception e) {
            LOGGER.warn("Incorrect listen ip specified: {} using localhost instead!", HOSTNAME);
            listenAddress = Inet4Address.getLoopbackAddress();
        }
        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            final ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ServerInitializer(sslCtx));

            // Start listening
            final Channel ch = b.bind(listenAddress, PORT).sync().channel();

            LOGGER.info("Open your web browser and navigate to {}://{}{}/", (SSL ? "https" : "http"), listenAddress.getHostAddress(), (PORT != 443 && PORT != 80 ? ":" + PORT : ""));

            // Block til closed
            ch.closeFuture().sync();

        } catch (Exception e) {
            LOGGER.warn("Failed to initialize server: ", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


    public static void main(String[] args) throws Exception {
        new ServerManager();
    }
}