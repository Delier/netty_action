package heartbeat.retry.failed;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;


// ctx.channel().pipeline().channel().parent()会为null
//错误 连接后马上就端开，并且重试失败

public class RetryClient {

    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(HOST, PORT))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) {
                            ch.pipeline().addLast(new ClientHandler(b));
                        }
                    });

            connect(b);
        } finally {
//            group.shutdownGracefully().sync();
        }
    }

    private static void connect(Bootstrap b) {
        ChannelFuture channelFuture = b.connect();
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                System.out.println("Connected to server!");
            } else {
                System.err.println("Failed to connect to server, retrying in 5 seconds...");
                future.channel().eventLoop().schedule(() -> connect(b), 5, TimeUnit.SECONDS);
            }
        });
//        try {
//            channelFuture.channel().closeFuture().sync();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    static class ClientHandler extends ChannelInboundHandlerAdapter {
        private static Bootstrap b;
        public ClientHandler(Bootstrap b){
            this.b = b;
        }
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.err.println("Disconnected from server!");
            // 当连接断开时触发重连
//            connect((Bootstrap) ctx.channel().pipeline().channel().parent());
            connect(b);
            super.channelInactive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // 处理收到的消息
            System.out.println("Received message: " + msg);
            super.channelRead(ctx, msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
