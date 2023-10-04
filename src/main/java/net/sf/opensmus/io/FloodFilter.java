package net.sf.opensmus.io;

import net.sf.opensmus.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import java.util.ArrayList;

public class FloodFilter implements ChannelUpstreamHandler {

    final ArrayList<ArrayList> settings;

    private int[] floodCount;
    private int[] repeatCount;
    private long[] lastMessageTime;

    private long lastMessageData = 0;
    private int lastMessageSize = 0;

    public FloodFilter(ArrayList<ArrayList> globalSettings) {
        settings = globalSettings;
        floodCount = new int[globalSettings.size()];
        repeatCount = new int[globalSettings.size()];
        lastMessageTime = new long[globalSettings.size()];
    }


    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) {

        if (e instanceof MessageEvent) {

            MessageEvent msg = (MessageEvent) e;
            ChannelBuffer inBuffer = (ChannelBuffer) msg.getMessage();

            // Grab the message subject
            inBuffer.markReaderIndex(); // First mark the position
            inBuffer.skipBytes(8); // Jump past the errorcode & timestamp.
            // The next int is the string length
            // The following from extractMUSMsgHeaderString()
            int strLen = inBuffer.readInt();

            // Sanity check
            if (strLen < 0 || strLen > inBuffer.readableBytes()) {
                MUSLog.Log("Floodfilter MUSMsgHeaderString size error : " + strLen + " " + inBuffer.readableBytes() + " " + ChannelBuffers.hexDump(inBuffer), MUSLog.kDeb);
                throw new NullPointerException("Floodfilter MUSMsgHeaderString size error " + strLen + " " + inBuffer.readableBytes());
            }

            byte[] stringBytes = new byte[strLen];
            inBuffer.readBytes(stringBytes, 0, strLen);
            // Use the platform's default charset, to allow for older non UTF movies.
            String subject = new String(stringBytes); // To force utf-8, we would use String(stringBytes, "UTF-8")

            // Move back the reader index after we have got the subject
            inBuffer.resetReaderIndex();

            // MUSLog.Log("ANTIFLOOD SUBJECT: " + subject, MUSLog.kDeb);

            // Check if this message should be controlled
            for (int n = 0; n < settings.size(); n++) {
                ArrayList s = settings.get(n);
                String sub = (String) s.get(0);
                if (sub.equals("*") || subject.equals(sub)) {
                    long currTime = System.currentTimeMillis();
                    if (currTime < lastMessageTime[n] + (Integer) s.get(1)) {  // Minimum time allowed between two messages.
                        floodCount[n]++;
                        // MUSLog.Log("Flood filter message count: " + floodCount, MUSLog.kDeb);
                        if (floodCount[n] >= (Integer) s.get(2)) {  // Number of messages under the allowed time in a row the server will accept before it's considered flooding.
                            // Figure out what user this is
                            MUSUser whatUser = ((SMUSPipeline) ctx.getPipeline()).user;
                            // Decode the entire message so we can log it
                            MUSMessage decodedMsg = new MUSMessage(inBuffer);
                            MUSLog.Log("User " + whatUser + " disconnected by flood prevention filter: " + decodedMsg, MUSLog.kSys);
                            notifyFloodDisconnect(whatUser);
                            whatUser.killMUSUser();
                            floodCount[n] = -9999; // To prevent further triggering while the disconnect is in process
                            return;
                        }
                    } else {
                        floodCount[n] = 0;
                    }

                    lastMessageTime[n] = currTime;

                    int repeatThreshold = (Integer) s.get(3) -2; // The number of identical messages allowed before being disconnected

                    if (repeatThreshold > -2) { // No need to calculate if not enabled
                        // Check if the message is the same as the last one sent

                        // Only messages of the same size can be equal, so we skip checking for equality if the sizes doesn't match
                        // This implementation means the first repeatCount will be counted on the THIRD message
                        // 1st message = new size
                        // 2nd message = same size but new hash
                        // 3rd message = both size and hash will match
                        // So the lowest value allowed for the repeat setting is 3.
                        int msgSize = inBuffer.readableBytes();
                        if (msgSize == lastMessageSize) {

                            // @TODO: Research working max size to process for using the hashcode.
                            // Perhaps use the hashcode for message sizes less than x bytes, and the current method for larger ones?
                            // int hash = inBuffer.hashCode();
                            long data = inBuffer.getLong(inBuffer.writerIndex() - 8); // Read the last 8 bytes of the message

                            if (data == lastMessageData) {
                                repeatCount[n]++;
                                // MUSLog.Log("Flood filter repeat count: " + repeatCount, MUSLog.kDeb);
                                if (repeatCount[n] > repeatThreshold) {
                                    // Figure out what user this is
                                    MUSUser whatUser = ((SMUSPipeline) ctx.getPipeline()).user;
                                    // Decode the entire message so we can log it
                                    MUSMessage decodedMsg = new MUSMessage(inBuffer);
                                    MUSLog.Log("User " + whatUser + " disconnected by flood prevention filter (repeats): " + decodedMsg, MUSLog.kSys);
                                    notifyFloodDisconnect(whatUser);
                                    whatUser.killMUSUser();
                                    repeatCount[n] = -9999;
                                    return;
                                }
                            } else {
                                repeatCount[n] = 0;
                                lastMessageData = data;
                            }
                        } else {
                            repeatCount[n] = 0;
                            lastMessageSize = msgSize;
                        }
                    }
                    // We have done an antiflood check for this message, don't do any more.
                    break;
                } else {
                    // This message has a different subject. Clear any accumulated repeat data.
                    repeatCount[n] = 0;
                }
            }

            // sendUpstream() -> "It is recommended to use the shortcut methods in Channels rather than calling this method directly"
            Channels.fireMessageReceived(ctx, inBuffer);
        } else {
            // Not a message event, so we just pass it along
            ctx.sendUpstream(e);
        }
    }


    private void notifyFloodDisconnect(MUSUser usr) {

        if (usr.m_movie == null) {
            // TEMP
            MUSLog.Log("User movie is null in notifyFloodDisconnect!", MUSLog.kSys);
            return;
        }

        MUSMessage reply = new MUSMessage();
        reply.m_errCode = MUSErrorCode.MessageContainsErrorInfo; // Temp
        reply.m_timeStamp = usr.m_movie.getServer().timeStamp();  // Has thrown NullPointerException
        reply.m_subject = new MUSMsgHeaderString("FloodDetected");
        reply.m_senderID = new MUSMsgHeaderString("System");
        reply.m_recptID = new MUSMsgHeaderStringList();

        reply.m_recptID.addElement(new MUSMsgHeaderString(usr.name()));

        reply.m_msgContent = new LVoid();

        usr.sendMessage(reply);
    }
}