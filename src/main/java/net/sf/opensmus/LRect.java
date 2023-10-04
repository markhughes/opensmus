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
 * Class representing a Lingo compatible Rect value (LRect for short).
 * Lingo is a trademark of Adobe, Inc. All rights reserved.
 */
public class LRect extends LValue {
    /**
     * X Coordinate of the Rect stored as an LValue. It can be an LInteger or an LFloat
     */
    public LValue m_X;
    /**
     * Y Coordinate of the Rect stored as an LValue. It can be an LInteger or an LFloat
     */
    public LValue m_Y;
    /**
     * W Coordinate of the Rect stored as an LValue. It can be an LInteger or an LFloat
     */
    public LValue m_W;
    /**
     * H Coordinate of the Rect stored as an LValue. It can be an LInteger or an LFloat
     */
    public LValue m_H;

    /**
     * Constructor
     */
    public LRect() {
        setType(LRect.vt_Rect);
    }

    /**
     * Constructor. 4 LValues representing the Lingo Rect coordinates
     */
    public LRect(LValue x, LValue y, LValue w, LValue h) {
        m_X = x;
        m_Y = y;
        m_W = w;
        m_H = h;
        setType(LRect.vt_Rect);
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

        // Third element W
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
            m_W = newVal;
        }

        // Fourth element H
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
            m_H = newVal;
        }
        return chunkSize;

    }

     @Override
    public String toString() {

        return ("rect(" + m_X + ", " + m_Y + ", " + m_W + ", " + m_H +")");
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public void dump() {
        MUSLog.Log("Rect element: ", MUSLog.kDeb);
        m_X.dump();
        m_Y.dump();
        m_W.dump();
        m_H.dump();

    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public byte[] getBytes() {
        byte[] pX = m_X.getBytes();
        byte[] pY = m_Y.getBytes();
        byte[] pW = m_W.getBytes();
        byte[] pH = m_H.getBytes();

        int a = pX.length;
        int b = pY.length;
        int c = pW.length;
        int d = pH.length;


        byte[] finalbytes = new byte[2 + a + b + c + d];
        ConversionUtils.shortToByteArray((int) vt_Rect, finalbytes, 0);
        System.arraycopy(pX, 0, finalbytes, 2, a);
        System.arraycopy(pY, 0, finalbytes, 2 + a, b);
        System.arraycopy(pW, 0, finalbytes, 2 + a + b, c);
        System.arraycopy(pH, 0, finalbytes, 2 + a + b + c, d);

        return finalbytes;
    }

}
