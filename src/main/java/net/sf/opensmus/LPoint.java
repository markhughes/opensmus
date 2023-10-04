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
 * Class representing a Lingo compatible Point value (LPoint for short).
 * Lingo is a trademark of Adobe, Inc. All rights reserved.
 */
public class LPoint extends LValue {

    // private byte[] m_bytes;

    /**
     * X Coordinate of the Point stored as an LValue. It can be an LInteger or an LFloat
     */
    public LValue m_X;
    /**
     * Y Coordinate of the Point stored as an LValue. It can be an LInteger or an LFloat
     */
    public LValue m_Y;

    /**
     * Constructor
     */
    public LPoint() {
        setType(LValue.vt_Point);
    }

    /**
     * Constructor. 2 LValues representing the Lingo Point coordinates
     */
    public LPoint(LValue x, LValue y) {
        m_X = x;
        m_Y = y;
        setType(LValue.vt_Point);
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public int extractFromBytes(byte[] rawBytes, int offset) {

        short elemType = 0;
        LValue newVal;
        int chunkSize = 0;
        // First element X
        {
            elemType = ConversionUtils.byteArrayToShort(rawBytes, offset + chunkSize);
            chunkSize = chunkSize + 2;

            switch (elemType) {
                case LValue.vt_Integer:
                    newVal = new LInteger();
                    break;

                case LValue.vt_Float:
                    newVal = new LFloat();
                    break;

                default:
                    newVal = new LVoid();
                    break;
            }

            chunkSize = chunkSize + newVal.extractFromBytes(rawBytes, offset + chunkSize);
            m_X = newVal;
        }

        // Second element Y
        {
            elemType = ConversionUtils.byteArrayToShort(rawBytes, offset + chunkSize);
            chunkSize = chunkSize + 2;

            switch (elemType) {
                case LValue.vt_Integer:
                    newVal = new LInteger();
                    break;

                case LValue.vt_Float:
                    newVal = new LFloat();
                    break;

                default:
                    newVal = new LVoid();
                    break;
            }

            chunkSize = chunkSize + newVal.extractFromBytes(rawBytes, offset + chunkSize);
            m_Y = newVal;
        }
        return chunkSize;

    }

    @Override
    public String toString() {

        return ("point(" + m_X + ", " + m_Y + ")");
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public void dump() {
        MUSLog.Log("Point element: ", MUSLog.kDeb);
        m_X.dump();
        m_Y.dump();

    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public byte[] getBytes() {
        byte[] pX = m_X.getBytes();
        byte[] pY = m_Y.getBytes();

        byte[] finalbytes = new byte[2 + pX.length + pY.length];
        ConversionUtils.shortToByteArray((int) vt_Point, finalbytes, 0);
        System.arraycopy(pX, 0, finalbytes, 2, pX.length);
        System.arraycopy(pY, 0, finalbytes, 2 + pX.length, pY.length);

        return finalbytes;
    }

}
