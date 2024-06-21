package demo.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

public class HttpHelloWorldServer {
    private final int port;

    public HttpHelloWorldServer(int port){
        this.port = port;
    }

    public void start() throws InterruptedException {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline p = socketChannel.pipeline();
                            p.addLast(new HttpServerCodec());
//                            p.addLast(new HttpObjectAggregator(65536));
                            p.addLast(new HttpHelloWorldServerHandler());
                        }
                    });
            ChannelFuture future = b.bind(port).sync();
            future.channel().closeFuture().sync();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException{
        new HttpHelloWorldServer(8080).start();
    }
}

class HttpHelloWorldServerHandler extends SimpleChannelInboundHandler<HttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
//        System.out.println("decode result: " + req.decoderResult().isFailure());
//        System.out.println("Received request: " + req.uri());
//        System.out.println("Headers: " + req.headers());
//        System.out.println("Method: " + req.method());
//        System.out.println("Content: " + req.content().toString(CharsetUtil.UTF_8));
        if (HttpUtil.is100ContinueExpected(req)){
            ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
        }
        boolean keepAlive = HttpUtil.isKeepAlive(req);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,HttpResponseStatus.OK,
                Unpooled.copiedBuffer("Hello World", CharsetUtil.UTF_8)
        );

        response.headers().set(HttpHeaderNames.CONTENT_TYPE,"text/plain;charset=UTF-8");

        if (keepAlive){
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH,response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderValues.KEEP_ALIVE);
        }

//        ChannelFuture f = ctx.write(response);
        ChannelFuture f = ctx.writeAndFlush(response);
        if (!keepAlive){
            f.addListener(ChannelFutureListener.CLOSE);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        cause.printStackTrace();
        ctx.close();
    }
}
