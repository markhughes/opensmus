/*
  Part of OpenSMUS Source Code.
  OpenSMUS is licensed under a MIT License, compatible with both
  open source (GPL or not) and commercial development.

  Copyright (c) 2001-2008 Mauricio Piacentini <mauricio@tabuleiro.com>

  Permission is hereby granted, free of charge, to any person
  obtaining a copy of this software and associated documentation
  files (the "Software"), to deal in the Software without
  restriction, including without limitation the rights to use,
  copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the
  Software is furnished to do so, subject to the following
  conditions:

  The above copyright notice and this permission notice shall be
  included in all copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
  OTHER DEALINGS IN THE SOFTWARE.
*/

package net.sf.opensmus;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import java.io.*;
import java.net.*;

/**
 * Class representing a message formatted according to the Shockwave MultiUserServer specs.
 * <BR>See technote 15465 "Shockwave Multiuser protocol description" at www.macromedia.com/support
 * for more information about the internal structure of a Shockwave binary message.
 * Shockwave is a trademark of Macromedia, Inc. All rights reserved.
 */
public class MUSMessage {

    /**
     * Default MUS message header, included automatically with each message.
     */
    public final static byte[] m_header = {0x72, 0x00};

    /**
     * Message error code, represented as a MUSErrorCode type (for example MUSErrorCode.NoError)
     */
    public int m_errCode = 0;

    /**
     * Message timestamp. Usually set automatically by OpenSMUS's message dispatcher.
     */
    public int m_timeStamp = 0;

    /**
     * A single MUSMsgHeaderString object corresponding to the message's subject.
     */
    public MUSMsgHeaderString m_subject;

    /**
     * A single MUSMsgHeaderString object corresponding to the name of the message's sender.
     */
    public MUSMsgHeaderString m_senderID;

    /**
     * A MUSMsgHeaderStringList object containing one or more MUSMsgHeaderStrings, each corresponding to
     * one intended recipient for this message.
     */
    public MUSMsgHeaderStringList m_recptID;

    /**
     * The content part of this message. Content is always one single LValue, but it may be a linear or property list including other LValues.
     */
    public LValue m_msgContent;

    /**
     * UDP flag for this message. When set to TRUE the dispatcher will attempt to deliver the message
     * using the UDP connection channel.
     */
    public boolean m_udp = false;


    /**
     * Default Constructor
     */
    public MUSMessage() {
    }

     /**
     * Constructor. Created a message from raw bytes.
     */
     public MUSMessage(ChannelBuffer buf) {
         this.extractMUSMessage(buf);
     }

    /**
     * Constructor. Clones another message.
     */
    public MUSMessage(MUSMessage msg) {
        // byte[] raw = msg.getBytes().toByteBuffer().array();
        ChannelBuffer raw = msg.getBytes();
        raw.readerIndex(6); // Forward past the header bytes
        extractMUSMessage(raw);
        // TODO: Replace the m_senderID here if we want to skip reading the bytes
    }


    /**
     * Reserved for internal use of OpenSMUS.
     */
    // (Only called directly for new message objects from the logoon handlers)
    // Other usages use the constructor instead.
    public void extractMUSMessage(ChannelBuffer buf) {

        byte[] m_rawContents = readRawBytes(buf);

        // Debug helpers
        // System.out.println("Encrypted: " + m_encrypted);
        // System.out.println("Length: " + m_rawContents.length);
        // System.out.println("String format: " + new String(m_rawContents));
        // System.out.println("Decoded >" + ConversionUtils.bytesToBinHex(m_rawContents));

        //m_msgContent = new MUSMsgContent();
        //m_msgContent = new LValue();
        //m_msgContent.extractFromBytes(m_rawContents);
        m_msgContent = LValue.fromRawBytes(m_rawContents, 0);
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    byte[] readRawBytes(ChannelBuffer msg) {

        m_errCode = msg.readInt();
        m_timeStamp = msg.readInt();

        m_subject = new MUSMsgHeaderString();
        m_subject.extractMUSMsgHeaderString(msg);

        // Sender ID always gets overwritten with the correct username later
        // in order to prevent spoofed messages
        m_senderID = new MUSMsgHeaderString();
        m_senderID.extractMUSMsgHeaderString(msg);

        // TODO: Optimize by just skipping these bytes since they will be replaced anyway
        // Can't do this because the senderID is not replaced when cloning a message using the MUSMessage(MUSMessage msg) constructor
        // Need to take care of that first.
//        int strSize = msg.readInt();
//        if ((strSize % 2) != 0) strSize++;
//        msg.skipBytes(strSize);

        m_recptID = new MUSMsgHeaderStringList();
        m_recptID.extractMUSMsgHeaderStringList(msg);

        int contentSize = msg.readableBytes(); // The rest of the data is the contents

        byte[] rawContents = new byte[contentSize];
        msg.readBytes(rawContents, 0, contentSize);
        return rawContents;
    }


    protected int extractInt(byte[] rawmsg, int offset) {
        return ConversionUtils.byteArrayToInt(rawmsg, offset);
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    public void dump() {
        MUSLog.Log("MUSMessage begin <<<<<<<<<<<<<<<<<<", MUSLog.kDeb);
        MUSLog.Log("m_errCode: " + m_errCode, MUSLog.kDeb);
        MUSLog.Log("m_timeStamp: " + m_timeStamp, MUSLog.kDeb);
        MUSLog.Log("m_subject: " + m_subject.toString(), MUSLog.kDeb);
        MUSLog.Log("m_senderID: " + m_senderID.toString(), MUSLog.kDeb);
        MUSLog.Log("m_receiptID: ", MUSLog.kDeb);
        m_recptID.dump();
        MUSLog.Log("m_content: ", MUSLog.kDeb);
        m_msgContent.dump();
        MUSLog.Log("MUSMessage end >>>>>>>>>>>>>>>>>>>>", MUSLog.kDeb);
    }

    @Override
    public String toString() {
        return "MUSMessage{" +
                "m_errCode=" + m_errCode +
                ", m_timeStamp=" + m_timeStamp +
                ", m_subject=" + m_subject.toString() +
                ", m_senderID=" + m_senderID.toString() +
                ", m_recptID=" + m_recptID +
                ", m_msgContent=" + m_msgContent +
                ", m_udp=" + m_udp +
                '}';
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    public ChannelBuffer getBytes() {

        byte[] subjectBytes = m_subject.getBytes();
        byte[] senderBytes = m_senderID.getBytes();
        byte[] recptBytes = m_recptID.getBytes();
        byte[] contentBytes = m_msgContent.getBytes();

        int contentSize = 8 + subjectBytes.length + senderBytes.length + recptBytes.length + contentBytes.length; // +8 = errorCode & timeStamp
        ChannelBuffer buffer = ChannelBuffers.buffer(6 + contentSize);

        buffer.writeBytes(MUSMessage.m_header);
        buffer.writeInt(contentSize);
        buffer.writeInt(m_errCode);
        buffer.writeInt(m_timeStamp);

        buffer.writeBytes(subjectBytes);
        buffer.writeBytes(senderBytes);
        buffer.writeBytes(recptBytes);
        buffer.writeBytes(contentBytes);

        return buffer;
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    public DatagramPacket toDatagramPacket(InetAddress addr, int port, boolean encrypted) {

        // @TODO: FIX THIS      byte[] msgbody = getBytes();
        byte[] msgbody = "TEMP".getBytes();


        byte[] pbuffer = new byte[6 + msgbody.length];

        byte[] len = new byte[4];
        ConversionUtils.intToByteArray(msgbody.length, len, 0);

        System.arraycopy(MUSMessage.m_header, 0, pbuffer, 0, 2); // Add the SMUS header signature bytes
        System.arraycopy(len, 0, pbuffer, 2, 4); // Add the length
        System.arraycopy(msgbody, 0, pbuffer, 6, msgbody.length); // Add the message data

        // TODO: FIX THIS      if (encrypted)  m_cipher.encode(pbuffer);


        return new DatagramPacket(pbuffer, pbuffer.length, addr, port);
    }

}
