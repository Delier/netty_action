package heartbeat.retry.failed;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.concurrent.TimeUnit;

public class RetryDemo {
    public static void main(String[] args) throws Exception {
        // 创建 Netty 客户端启动器
        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup group = new NioEventLoopGroup();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO));

        // 主线程等待连接关闭
        try {
            ChannelFuture future = bootstrap.connect("localhost", 8080).sync();
            ChannelFuture closeFuture = future.channel().closeFuture().sync();
            System.out.println("连接关闭1");// 等待连接关闭
            bootstrap.connect("localhost", 8080).sync();
            closeFuture = future.channel().closeFuture().sync();


        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }
}
