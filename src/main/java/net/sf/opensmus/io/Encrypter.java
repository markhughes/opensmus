package net.sf.opensmus.io;

import org.jboss.netty.channel.*;
import org.jboss.netty.buffer.ChannelBuffer;
import net.sf.opensmus.MUSBlowfish;

public class Encrypter extends SimpleChannelHandler {

    MUSBlowfish cipher = new MUSBlowfish();

    // Server sending a message to a user (raw bytes)
    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

        // Always assume only one complete message at a time will be in the buffer.
        ChannelBuffer inBuffer = (ChannelBuffer) e.getMessage();

        inBuffer.markReaderIndex(); // @TODO: Maybe we can assume that the reader index will alwyas be 0?
        cipher.encode(inBuffer);

        inBuffer.resetReaderIndex(); // Rewind the reader index since it was changed by the encode()
        // Forward the now encrypted message along the pipeline
        Channels.write(ctx, e.getFuture(), inBuffer);
    }
}
