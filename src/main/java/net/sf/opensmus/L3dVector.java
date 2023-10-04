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
 * Class representing a Lingo compatible Vector value (L3dVector for short).
 * Lingo is a trademark of Adobe, Inc. All rights reserved.
 */
public class L3dVector extends LValue {

    /**
     * Array of 3 floats representing the Lingo Vector
     */
    // public float[] m_floats;
    private byte[] m_bytes;

    /**
     * Constructor
     */
    public L3dVector() {
        // m_floats = new float[3];
        m_bytes = new byte[12];
        setType(LValue.vt_3dVector);
    }

    /**
     * Constructor. 3 float values represent the Lingo Vector (x,y,z)
     */
    public L3dVector(float x, float y, float z) {
//        m_floats = new float[3];
//        m_floats[0] = x;
//        m_floats[1] = y;
//        m_floats[2] = z;
        m_bytes = new byte[12];
        int tint = Float.floatToIntBits(x);
        ConversionUtils.intToByteArray(tint, m_bytes, 0);
        tint = Float.floatToIntBits(y);
        ConversionUtils.intToByteArray(tint, m_bytes, 4);
        tint = Float.floatToIntBits(z);
        ConversionUtils.intToByteArray(tint, m_bytes, 8);

        setType(LValue.vt_3dVector);
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public int extractFromBytes(byte[] rawBytes, int offset) {

//        int tint = 0;
//
//        for (int i = 0; i < 3; i++) {
//            tint = ConversionUtils.byteArrayToInt(rawBytes, offset);
//            m_floats[i] = Float.intBitsToFloat(tint);
//            offset = offset + 4;
//        }

        System.arraycopy(rawBytes, offset, m_bytes, 0, 12);
        return 12;

    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public void dump() {
       // MUSLog.Log("3dVector element: " + m_floats[0] + " - " + m_floats[1] + " - " + m_floats[2], MUSLog.kDeb);
        float[] f = new float[3];
        int tint;
        int offset = 0;

        for (int i = 0; i < 3; i++) {
            tint = ConversionUtils.byteArrayToInt(m_bytes, offset);
            f[i] = Float.intBitsToFloat(tint);
            offset = offset + 4;
        }

         MUSLog.Log("3dVector element: " + f[0] + " - " + f[1] + " - " + f[2], MUSLog.kDeb);
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public byte[] getBytes() {
        byte[] finalbytes = new byte[14];
        ConversionUtils.shortToByteArray((int) getType(), finalbytes, 0);
//        int tint;
//        for (int i = 0; i < 3; i++) {
//            tint = Float.floatToIntBits(m_floats[i]);
//            ConversionUtils.intToByteArray(tint, finalbytes, 2 + (i * 4));
//        }
        System.arraycopy(m_bytes, 0, finalbytes, 2, m_bytes.length);

        return finalbytes;
    }

}
