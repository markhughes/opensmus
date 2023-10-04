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

/** 
 *Class representing a Lingo compatible color value (LColor for short)
 *Color values are stored and retrieved as an opaque array of bytes
 *Lingo is a trademark of Adobe, Inc. All rights reserved.
 */
public class LColor extends LValue {

    private byte[] m_bytes;

    /**
     * Constructor
     */
    public LColor(byte[] initbytes) {
        m_bytes = initbytes;
        setType(LValue.vt_Color);
    }

    /**
     * Constructor
     */
    public LColor() {
        m_bytes = new byte[4];
        setType(LValue.vt_Color);
    }

    /**
     * Returns the byte array storing the color data in binary format
     */
    @Override
    public byte[] toBytes() {
        return m_bytes;
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public int extractFromBytes(byte[] rawBytes, int offset) {

        System.arraycopy(rawBytes, offset, m_bytes, 0, 4);
        return 4;
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public byte[] getBytes() {

        byte[] finalbytes = new byte[6];
        ConversionUtils.shortToByteArray((int) vt_Color, finalbytes, 0);
        // ConversionUtils.intToByteArray(m_bytes.length, finalbytes, 2);   // Removed by Rob, must be a bug.
        System.arraycopy(m_bytes, 0, finalbytes, 2, m_bytes.length);

        return finalbytes;
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public void dump() {
        MUSLog.Log("Color> " + ConversionUtils.bytesToBinHex(m_bytes), MUSLog.kDeb);
    }

    @Override
    public String toString() {
        // Not entirely sure about the byte meanings here... rgb+a?
        return ("color(" + m_bytes[0] + ", " + m_bytes[1] + ", " + m_bytes[2] +")");
    }
}
