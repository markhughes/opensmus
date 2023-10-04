package net.sf.opensmus.io;

import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.buffer.ChannelBuffer;
import net.sf.opensmus.*;

import java.net.InetSocketAddress;

@ChannelHandler.Sharable
public class LogonHandler extends IOHandler {

    MUSServer m_server;
    ChannelGroup channels;

    public LogonHandler(MUSServer srv, ChannelGroup cg) {
        m_server = srv;
        channels = cg;
    }


    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
        // Add all open channels to the global group so that they are closed on shutdown.
        // If the added channel is closed before shutdown, it will be removed from the group automatically.
        channels.add(e.getChannel());
    }


    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

        String ip = ((InetSocketAddress) ctx.getChannel().getRemoteAddress()).getAddress().getHostAddress();
        
        MUSLog.Log("Client connection initialized : " + (m_server.m_clientlist.size() + 1) + " (" + ip + ")", MUSLog.kSrv);
        // Create a user object for this new connection
        MUSUser newUser = new MUSUser(m_server, ctx.getChannel());

        // Store the user object in the session so we will know who it is when data arrive
        ((SMUSPipeline) ctx.getPipeline()).user = newUser;
    }


    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

        // Figure out what user this is
        MUSUser whatUser = ((SMUSPipeline) ctx.getPipeline()).user;

        MUSLogonMessage msg = new MUSLogonMessage();

        // Always assume the (complete) message is a ChannelBuffer, created by the framer
        ChannelBuffer buffer = (ChannelBuffer) e.getMessage();

        // Decode the message
        msg.extractMUSMessage(buffer);  // The incoming buffer does NOT have the 6 headerbytes (ID & length info)

        m_server.queueLogonMessage(msg, whatUser);
    }
}