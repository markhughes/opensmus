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

import java.io.UnsupportedEncodingException;

/**
 * Class representing a message header String.
 * It's the same as a LString except it doesn't have the 2-byte type identifier prefix
 * Direct methods for conversion to/from a Java string are provided.
 */
public class MUSMsgHeaderString {

    private byte[] m_string;

    /**
     * Constructs a MUSMsgHeaderString from a Java String.
     */
    public MUSMsgHeaderString(String initString) {

        m_string = initString.getBytes();
    }


    /**
     * Default Constructor
     */
    public MUSMsgHeaderString() {

        m_string = "".getBytes();
    }


    /**
     * Constructs a MUSMsgHeaderString from a Java String using the named charset.
     */
    public MUSMsgHeaderString(String initString, String charsetName) throws UnsupportedEncodingException {

        m_string = initString.getBytes(charsetName);
    }


    /**
     * Reserved for internal use of OpenSMUS.
     */
    public void extractMUSMsgHeaderString(ChannelBuffer buffer) {

        int strSize = buffer.readInt();

        // Sanity check
        if (strSize < 0 || strSize > buffer.readableBytes()) {
            MUSLog.Log("MUSMsgHeaderString size error : " + strSize + " " + buffer.readableBytes() + " " + ChannelBuffers.hexDump(buffer), MUSLog.kDeb);
            throw new NullPointerException("MUSMsgHeaderString size error " + strSize + " " + buffer.readableBytes());
        }

        m_string = new byte[strSize];

        buffer.readBytes(m_string, 0, strSize);

        if ((strSize % 2) != 0) buffer.skipBytes(1); // Consume one more byte
    }


    /**
     * Returns this MUSMsgHeaderString as a Java String.
     */
    @Override
    public String toString() {

        return new String(m_string);
    }


    /**
     * Reserved for internal use of OpenSMUS.
     */
    public void dump() {

        MUSLog.Log("String> " + new String(m_string), MUSLog.kDeb);
    }


    /**
     * Reserved for internal use of OpenSMUS.
     */
    public byte[] getBytes() {

        int finalSize = 4 + m_string.length;
        boolean addPadding = false;
        if ((finalSize % 2) != 0) {
            finalSize++;
            addPadding = true;
        }

        byte[] finalbytes = new byte[finalSize];
        ConversionUtils.intToByteArray(m_string.length, finalbytes, 0);
        System.arraycopy(m_string, 0, finalbytes, 4, m_string.length);

        if (addPadding) finalbytes[finalSize - 1] = 0x00;

        return finalbytes;
    }

}
