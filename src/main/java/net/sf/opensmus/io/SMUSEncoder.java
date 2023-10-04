package net.sf.opensmus.io;

import net.sf.opensmus.*;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channel;


@ChannelHandler.Sharable
public class SMUSEncoder extends OneToOneEncoder {

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object message) throws Exception {

        MUSMessage msg = (MUSMessage) message;
        // Return the constructed raw message bytes.
        return (msg.getBytes());
    }
}
