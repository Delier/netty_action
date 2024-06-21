package heartbeat.one;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class HeartbeatChatServer {

    private static final int PORT = 8080;
    private static final int READ_IDLE_TIME = 60; // 读超时时间，单位：秒

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // 添加心跳检测处理器
                            pipeline.addLast(new IdleStateHandler(READ_IDLE_TIME, 0, 0, TimeUnit.SECONDS) {
                                @Override
                                protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
                                    if (evt.state() == IdleState.READER_IDLE) {
                                        System.out.println("Client idle timeout, closing connection.");
                                        ctx.close();
                                    }
                                }
                            });

                            // 添加自定义的聊天消息处理器
                            pipeline.addLast(new SimpleChatServerHandler());
                        }
                    });

            ChannelFuture f = b.bind(PORT).sync();
            System.out.println("Server started on port " + PORT);
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

class SimpleChatServerHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        // 处理接收到的消息
        System.out.println("Received message from client: " + msg);

        // 在这里可以实现聊天服务器的逻辑，比如广播消息给所有连接的客户端等
        // 例如，向客户端发送消息
        ctx.writeAndFlush("Server received: " + msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 异常处理，一般在这里处理客户端连接异常或关闭连接等
        System.out.println("heart beat server exceptionCaught");
        cause.printStackTrace();
        ctx.close();
    }
}