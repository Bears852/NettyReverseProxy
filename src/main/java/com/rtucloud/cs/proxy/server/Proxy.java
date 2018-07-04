package com.rtucloud.cs.proxy.server;

import com.rtucloud.cs.proxy.config.AppConfig;
import com.rtucloud.cs.proxy.utils.ExecutorsUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;

@Component
public class Proxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(Proxy.class);
    private static final String FRONTEND_PROXY_THREAD_NAME = "Frontend-Proxy-Server-Thread";

    @Autowired
    public AppConfig appConfig;
    @Autowired
    FrontendPipeline frontendPipeline;

    @PostConstruct
    public void initFrontend() {
        ExecutorService executorService = ExecutorsUtils.newSingleThreadExecutor(FRONTEND_PROXY_THREAD_NAME);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                LOGGER.debug("启动服务，端口:" + appConfig.getLocalPort());

                EventLoopGroup bossGroup = new NioEventLoopGroup();
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    ServerBootstrap b = new ServerBootstrap();
                    b.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(frontendPipeline)
                            .childOption(ChannelOption.AUTO_READ, false);

                    ChannelFuture f = b.bind(Integer.valueOf(appConfig.getLocalPort())).sync();

                    f.channel().closeFuture().sync();

                } catch (Exception e) {
                    LOGGER.error("代理服务器启动失败", e);
                } finally {
                    workerGroup.shutdownGracefully();
                    bossGroup.shutdownGracefully();
                }
            }
        });
    }

    public void setFrontendPipeline(FrontendPipeline frontendPipeline) {
        this.frontendPipeline = frontendPipeline;
    }
}