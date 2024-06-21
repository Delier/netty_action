package demo.telennt;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TelnetClient {

    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new TelnetClientHandler());
                        }
                    });

            ChannelFuture f = b.connect("localhost", 8080).sync(); // 连接到本地的Telnet服务器端口
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
//                f.channel().writeAndFlush(line + "\r\n");
                ChannelFuture future = f.channel().writeAndFlush(line + "\r\n");
                future.addListener((ChannelFutureListener)futureListener->{
                    if (!futureListener.isSuccess()){
                        Throwable cause = futureListener.cause();
                        System.out.println("发送失败"+cause.toString());
                    }
                });
            }
        } finally {
            group.shutdownGracefully();
        }
    }

    public static class TelnetClientHandler extends SimpleChannelInboundHandler<String> {

        @Override
        public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            System.out.println(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.out.println("exceptionCaught");
            cause.printStackTrace();
            ctx.close();
        }


        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("channelInactive");
            super.channelInactive(ctx);
        }
    }
}
