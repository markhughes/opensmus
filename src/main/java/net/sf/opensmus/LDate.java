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

import java.util.Date;

/** 
 *Class representing a Lingo compatible date value (LDate for short)
 *Date values are stored and retrieved as an opaque array of bytes
 *Lingo is a trademark of Adobe, Inc. All rights reserved.
 */
public class LDate extends LValue {

    private byte[] m_bytes;

    /**
     * Constructor
     */
    public LDate(byte[] initbytes) {
        m_bytes = initbytes;
        setType(LValue.vt_Date);
    }

    /**
     * Constructor
     */
    public LDate() {
        m_bytes = new byte[8];
        byte[] tempBytes = new byte[8];
        // Default to the current date
        long time = System.currentTimeMillis()/1000; // the number of seconds that have elapsed since January 1, 1970 (midnight UTC/GMT)
        ConversionUtils.longToByteArray(time, tempBytes, 0);
        // The lower 32 bits are bytes 5-8, so they need to be swapped.
        m_bytes[0] = tempBytes[4];
        m_bytes[1] = tempBytes[5];
        m_bytes[2] = tempBytes[6];
        m_bytes[3] = tempBytes[7];
        m_bytes[4] = tempBytes[0];
        m_bytes[5] = tempBytes[1];
        m_bytes[6] = tempBytes[2];
        m_bytes[7] = tempBytes[3];

        setType(LValue.vt_Date);
    }

    /**
     * Returns the byte array storing the date value in binary format
     */
    @Override
    public byte[] toBytes() {
        return m_bytes;
    }

    /**
     * Returns a Java Date object
     */
    public Date toDate() {
        
        byte[] tempBytes = new byte[8];
        tempBytes[0] = m_bytes[4];
        tempBytes[1] = m_bytes[5];
        tempBytes[2] = m_bytes[6];
        tempBytes[3] = m_bytes[7];
        tempBytes[4] = m_bytes[0];
        tempBytes[5] = m_bytes[1];
        tempBytes[6] = m_bytes[2];
        tempBytes[7] = m_bytes[3];
        long epoch = ConversionUtils.byteArrayToLong(tempBytes, 0) * 1000; // Measured in milliseconds
        return (new java.util.Date (epoch));
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public int extractFromBytes(byte[] rawBytes, int offset) {

        System.arraycopy(rawBytes, offset, m_bytes, 0, 8);
        return 8;
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public byte[] getBytes() {

        byte[] finalbytes = new byte[10];
        ConversionUtils.shortToByteArray((int) vt_Date, finalbytes, 0);
        // ConversionUtils.intToByteArray(m_bytes.length, finalbytes, 2);
        System.arraycopy(m_bytes, 0, finalbytes, 2, m_bytes.length);

        return finalbytes;
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public void dump() {
        MUSLog.Log("Date> " + ConversionUtils.bytesToBinHex(m_bytes), MUSLog.kDeb);
    }

    
    @Override
    public String toString() {

        return toDate().toString();
    }

}
