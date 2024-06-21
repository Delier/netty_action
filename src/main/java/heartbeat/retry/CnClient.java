package heartbeat.retry;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class CnClient {

    public static void connect(String address, int port) throws Exception{
        NioEventLoopGroup group = new NioEventLoopGroup();
        try{
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO));
            ChannelFuture future = bootstrap.connect("localhost", 8080).sync();
            ChannelFuture closeFuture = future.channel().closeFuture();
            closeFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()){
                        System.out.println("重新连接...");
                        connect(address, port);
                    }
                }
            });
        }finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        try {
            connect("localhost",8080);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
