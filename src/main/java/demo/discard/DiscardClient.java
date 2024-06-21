package demo.discard;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import util.ServerUtil;

public final class DiscardClient {
    static final String HOST = System.getProperty("host","127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port","8009"));
    static final int SIZE = Integer.parseInt(System.getProperty("size","256"));

    public static void main(String[] args) throws Exception {

        final SslContext sslCtx = ServerUtil.buildSslContext();
        NioEventLoopGroup group = new NioEventLoopGroup();
        try{
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            if (sslCtx != null){
                                pipeline.addLast(sslCtx.newHandler(socketChannel.alloc(),HOST,PORT));
                                pipeline.addLast(new DiscardClientHandler());
                            }
                        }
                    });
            ChannelFuture sync = b.connect(HOST, PORT).sync();
            sync.channel().closeFuture().sync();
        }finally {
            group.shutdownGracefully();
        }
    }
}
