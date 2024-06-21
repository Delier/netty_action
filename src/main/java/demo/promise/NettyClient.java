package demo.promise;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyClient {
    public static void main(String[] args) throws Exception{
        NioEventLoopGroup group = new NioEventLoopGroup();
        try{
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new ClientHandler());
                        }
                    });
            ChannelFuture future = bootstrap.connect("localhost", 8080).sync();
            Channel channel = future.channel();
            ByteBuf data = Unpooled.copiedBuffer("hello,server!".getBytes());

            ChannelPromise promise = channel.newPromise();
            channel.writeAndFlush(data,promise);
            promise.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()){
                        System.out.println("data was successfully written to the channel");
                    }else{
                        System.out.println("Failed to write to the channel");
                    }
                }
            });
            channel.close().sync();
        }finally {
            group.shutdownGracefully();
        }
    }
}
