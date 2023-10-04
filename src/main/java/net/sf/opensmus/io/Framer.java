package net.sf.opensmus.io;

import net.sf.opensmus.*;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

public class Framer extends FrameDecoder {

    int messageSize = 0;

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {

        if (messageSize == 0) {
            // Start of new message
            // Grab the first 6 bytes of the incoming message so we can see how long this message is
            if (buffer.readableBytes() < 6) return false; // Wait until the length prefix is available.

            // Check that the packet has the SMUS signature header
            if (buffer.readShort() != 0x7200) {
                MUSUser whatUser = ((SMUSPipeline) ctx.getPipeline()).user; // Figure out what user this is
                MUSLog.Log("Invalid message format from " + whatUser + ": " + buffer + " " + ChannelBuffers.hexDump(buffer, buffer.readerIndex() - 2, 8), MUSLog.kMsgErr);
                whatUser.killMUSUser();
                buffer.clear(); // Consume everything in the buffer so that the FrameDecoder doesn't keep calling decode() while the user is being disconnected
                return null;
            }

            messageSize = buffer.readInt(); // Next 4 bytes is full message size in bytes (rest of the data)
        }

        if (buffer.readableBytes() < messageSize) {
            // Not enough bytes available for the rest of the message
            return null;
        }

        // We could have a max size check here...
        // if (messageSize > m_movie.getServer().m_props.getIntProperty("MaxMessageSize") )
        // MUSErrorCode.MessageTooLarge

        // Store the message bytes (stripped of the 2 header bytes and 4 length info bytes) in a new buffer
        // This also updates the readerIndex of the original buffer, so FrameDecoder will know we consumed the bytes in it.
        ChannelBuffer msgData = buffer.readBytes(messageSize);

        messageSize = 0; // Get ready for the next message
        return (msgData);
    }
}



