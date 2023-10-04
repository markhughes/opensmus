package net.sf.opensmus.io;

import org.jboss.netty.channel.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelUpstreamHandler;
import net.sf.opensmus.*;

import java.nio.channels.ClosedChannelException;
import java.io.IOException;

@ChannelHandler.Sharable
public class IOHandler extends IdleStateAwareChannelUpstreamHandler {

    public IOHandler() {

    }


    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        // MUSUser whatUser = ((SMUSPipeline) ctx.getPipeline()).user;
        // MUSLog.Log("Channel disconnect for " + whatUser.name(), MUSLog.kDeb);
        ctx.getChannel().close().addListener(MUSUser.REPORT_CLOSE);
    }


    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        MUSUser whatUser = ((SMUSPipeline) ctx.getPipeline()).user; // Figure out what user this is
        // MUSLog.Log("Channel closed for: " + whatUser.name(), MUSLog.kDeb);
        whatUser.killMUSUser();
    }


    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

        // Sometimes the MessageEvent is just bool "false"
        // This can happen when a client disconnects
        // Not sure if this still happens in Netty 3.2.1...
        Object incoming = e.getMessage();
        if (!(incoming instanceof ChannelBuffer)) {
            System.out.println("Illegal messageReceived: " + e + e.getMessage());
            return;
        }
        // Always assume the (complete) message is a ChannelBuffer, created by the framer
        ChannelBuffer buffer = (ChannelBuffer) incoming;

        // Decode the message
        MUSMessage msg = new MUSMessage(buffer); // The incoming buffer does NOT have the 6 headerbytes (ID & length info)

         // Figure out what user this is
        MUSUser whatUser = ((SMUSPipeline) ctx.getPipeline()).user;

        // Prevent spoofing of userid by ignoring the parsed senderID !
        // ALWAYS do this on incoming messages.
        // Only serverside scripts might want to send under a different name and that's only for outgoing messages
        // Remotely connected users are not supposed to be able to change their senderID!
        msg.m_senderID = new MUSMsgHeaderString(whatUser.name());

        // ^--- All above from SMUSDecoder

        whatUser.postMessage(msg);
    }


    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) throws Exception {
        // Disconnect an idle client
        MUSUser whatUser = ((SMUSPipeline) ctx.getPipeline()).user; // Figure out what user this is
        // MUSLog.Log("Idle event for " + whatUser.name(), MUSLog.kDeb);
        notifyIdleDisconnect(whatUser);
        whatUser.deleteUser();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        // Normal to get these exceptions:
        // java.io.IOException: Connection reset by peer
        // java.nio.channels.ClosedChannelException

        Channel ch = e.getChannel();
        MUSUser whatUser = ((SMUSPipeline) ch.getPipeline()).user;
        Throwable cause = e.getCause();
        if (!(cause instanceof ClosedChannelException || cause instanceof IOException)) {
            MUSLog.Log("Netty Exception " + cause + " for " + whatUser, MUSLog.kDeb);
            cause.printStackTrace();
        }
        // Close the connection on exceptional situations
        ch.close().addListener(MUSUser.REPORT_CLOSE);
    }

    private void notifyIdleDisconnect(MUSUser usr) {

        if (usr.m_movie == null) {
            // TEMP
            MUSLog.Log("User movie is null in notifyIdleDisconnect!", MUSLog.kSys);
            return;
        }

        MUSMessage msg = new MUSMessage();
        msg.m_errCode = MUSErrorCode.MessageContainsErrorInfo; // Temp
        msg.m_timeStamp = usr.m_movie.getServer().timeStamp();
        msg.m_subject = new MUSMsgHeaderString("IdleDisconnect");
        msg.m_senderID = new MUSMsgHeaderString("System");
        msg.m_recptID = new MUSMsgHeaderStringList();

        msg.m_recptID.addElement(new MUSMsgHeaderString(usr.name()));

        msg.m_msgContent = new LVoid();

        usr.sendMessage(msg);
    }

}