package net.sf.opensmus.io;

import org.jboss.netty.channel.*;
import org.jboss.netty.buffer.ChannelBuffer;
import net.sf.opensmus.MUSServer;

@ChannelHandler.Sharable
public class LogFilter implements ChannelUpstreamHandler, ChannelDownstreamHandler {

    MUSServer m_server;

    public LogFilter(MUSServer srv) {
        m_server = srv;
    }


    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {

        if (e instanceof MessageEvent) {
            MessageEvent me = (MessageEvent) e;
            if (me.getMessage() instanceof ChannelBuffer) { // @TODO: This check might be redundant...
                m_server.logInBytes(((ChannelBuffer) me.getMessage()).readableBytes());
            }
            Channels.fireMessageReceived(ctx, me.getMessage());
        } else {
            ctx.sendUpstream(e);  // ^--- "It is recommended to use the shortcut methods in Channels rather than calling this method directly."
        }
    }


    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {

        if (e instanceof MessageEvent) {
            MessageEvent me = (MessageEvent) e;
            if (me.getMessage() instanceof ChannelBuffer) {
                m_server.logOutBytes(((ChannelBuffer) me.getMessage()).readableBytes());
            }
            Channels.write(ctx, e.getFuture(), me.getMessage());
        } else {
            ctx.sendDownstream(e); // ^--- "It is recommended to use the shortcut methods in Channels rather than calling this method directly."
        }
    }

}
