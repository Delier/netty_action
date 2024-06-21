package heartbeat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ChatMessageHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("received message: " +  msg);
        for (Channel channel: ChatServerChannels.getChannels() ) {
            if (channel != ctx.channel()){
                channel.writeAndFlush(msg + "\n");
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception{
        ChatServerChannels.addChannel(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
        ChatServerChannels.removeChannel(ctx.channel());
        super.channelInactive(ctx);
    }
}
