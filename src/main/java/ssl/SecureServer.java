package ssl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.io.File;


public class SecureServer {
    private final int port;
    public SecureServer(int port){
        this.port = port;
    }

    public void start() throws Exception {
//        SelfSignedCertificate ssc = new SelfSignedCertificate();
//        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

//        生成私钥
//        openssl genpkey -algorithm RSA -out server_private.key
//        生成自签名证书
//        openssl req -new -x509 -key server_private.key -out server_cert.crt -days 365
        File certChainFile = new File("server_cert.crt");
        File keyFile = new File("server_private.key");

        SslContext sslCtx = SslContextBuilder.forServer(certChainFile, keyFile).build();

        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workGroup = new NioEventLoopGroup();

        try{
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup,workGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addFirst(sslCtx.newHandler(socketChannel.alloc()));
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new SecureServerHandler());
                        }
                    });
            ChannelFuture f = b.bind(port).sync();
            System.out.println("Server started and listening on " + f.channel().localAddress());
            f.channel().closeFuture().sync();
        }finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception{
        new SecureServer(8080).start();
    }

}

class SecureServerHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("Received message: " + msg);
        ctx.writeAndFlush("Hello from server!\n");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
