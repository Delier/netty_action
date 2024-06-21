package demo.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

public class SimpleHttpServer {

    public static void main(String[] args) throws Exception {
        // 创建两个EventLoopGroup，一个用于处理连接的接收，一个用于处理连接的数据读写
        EventLoopGroup bossGroup = new NioEventLoopGroup(1); // 用于接收连接的线程池
        EventLoopGroup workerGroup = new NioEventLoopGroup(); // 用于处理连接的数据读写的线程池

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // 使用NIO进行网络通信
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            // 添加HTTP请求处理器
                            p.addLast(new HttpServerCodec()); // HTTP编解码器
                            p.addLast(new HttpObjectAggregator(65536)); // 将HTTP消息的多个部分合成一条完整的HTTP消息
                            p.addLast(new SimpleHttpServerHandler()); // 自定义HTTP请求处理器
                        }
                    });

            // 绑定端口，开始接收进来的连接
            ChannelFuture f = b.bind(8080).sync();

            // 等待服务器socket关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅地关闭线程池
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    // 自定义的HTTP请求处理器
    static class SimpleHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
            System.out.println("decode result: " + msg.decoderResult().isFailure());
            if (msg.decoderResult().isFailure()) {
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                return;
            }

            // 构造HTTP响应
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    ctx.alloc().buffer().writeBytes("Hello, Netty!".getBytes())
            );

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

            // 发送HTTP响应
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }

        private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    status,
                    ctx.alloc().buffer().writeBytes(("Failure: " + status.toString() + "\r\n").getBytes())
            );
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

            // 发送HTTP响应
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
