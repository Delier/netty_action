package heartbeat;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    private static final int READ_IDLE_TIME = 60;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx,Object evt) throws Exception{
        if (evt instanceof IdleStateEvent){
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE){
                ctx.close();
            }else if (event.state() == IdleState.WRITER_IDLE){
                ctx.writeAndFlush("heartbeat").addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } else {
            super.userEventTriggered(ctx,evt);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception{
        ctx.pipeline().addFirst(new IdleStateHandler(READ_IDLE_TIME,0,0, TimeUnit.SECONDS));
        super.channelActive(ctx);
    }
}
