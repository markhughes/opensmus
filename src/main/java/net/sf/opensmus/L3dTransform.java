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
 * Class representing a Lingo compatible 3dTransform value (L3dTransform for short).
 * Lingo is a trademark of Adobe, Inc. All rights reserved.
 */
public class L3dTransform extends LValue {

    /**
     * Array of 16 floats representing the Lingo Transform
     */
    // public float[] m_floats;
    private byte[] m_bytes;

    /**
     * Constructor
     */
    public L3dTransform() {
        // m_floats = new float[16];
        m_bytes = new byte[64]; // Specs say 8*16, but it is 4*16 :-/
        setType(LValue.vt_3dTransform);
    }

    /**
     * Constructor. 16 float values represent the Lingo Transform
     */
    public L3dTransform(float a, float b, float c, float d, float e, float f,
                        float g, float h, float i, float j, float k, float l, float m, float n, float o,
                        float p) {

        int tint;

        tint = Float.floatToIntBits(a);
        ConversionUtils.intToByteArray(tint, m_bytes, 0);
        tint = Float.floatToIntBits(b);
        ConversionUtils.intToByteArray(tint, m_bytes, 4);
        tint = Float.floatToIntBits(c);
        ConversionUtils.intToByteArray(tint, m_bytes, 8);
        tint = Float.floatToIntBits(d);
        ConversionUtils.intToByteArray(tint, m_bytes, 12);
        tint = Float.floatToIntBits(e);
        ConversionUtils.intToByteArray(tint, m_bytes, 16);
        tint = Float.floatToIntBits(f);
        ConversionUtils.intToByteArray(tint, m_bytes, 20);
        tint = Float.floatToIntBits(g);
        ConversionUtils.intToByteArray(tint, m_bytes, 24);
        tint = Float.floatToIntBits(h);
        ConversionUtils.intToByteArray(tint, m_bytes, 28);
        tint = Float.floatToIntBits(i);
        ConversionUtils.intToByteArray(tint, m_bytes, 32);
        tint = Float.floatToIntBits(j);
        ConversionUtils.intToByteArray(tint, m_bytes, 36);
        tint = Float.floatToIntBits(k);
        ConversionUtils.intToByteArray(tint, m_bytes, 40);
        tint = Float.floatToIntBits(l);
        ConversionUtils.intToByteArray(tint, m_bytes, 44);
        tint = Float.floatToIntBits(m);
        ConversionUtils.intToByteArray(tint, m_bytes, 48);
        tint = Float.floatToIntBits(n);
        ConversionUtils.intToByteArray(tint, m_bytes, 52);
        tint = Float.floatToIntBits(o);
        ConversionUtils.intToByteArray(tint, m_bytes, 56);
        tint = Float.floatToIntBits(p);
        ConversionUtils.intToByteArray(tint, m_bytes, 60);

//        m_floats = new float[16];
//        m_floats[0] = a;
//        m_floats[1] = b;
//        m_floats[2] = c;
//        m_floats[3] = d;
//        m_floats[4] = e;
//        m_floats[5] = f;
//        m_floats[6] = g;
//        m_floats[7] = h;
//        m_floats[8] = i;
//        m_floats[9] = j;
//        m_floats[10] = k;
//        m_floats[11] = l;
//        m_floats[12] = m;
//        m_floats[13] = n;
//        m_floats[14] = o;
//        m_floats[15] = p;

        setType(LValue.vt_3dTransform);
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public int extractFromBytes(byte[] rawBytes, int offset) {

//        int tint = 0;
//
//        for (int i = 0; i < 16; i++) {
//            tint = ConversionUtils.byteArrayToInt(rawBytes, offset);
//            m_floats[i] = Float.intBitsToFloat(tint);
//            offset = offset + 4;
//        }

        System.arraycopy(rawBytes, offset, m_bytes, 0, 64);

        return 64;
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public void dump() {
        MUSLog.Log("3dVector element: ", MUSLog.kDeb);
        int offset = 0;
        float f;
        int tint;
        for (int a = 0; a < 16; a++) {
            tint = ConversionUtils.byteArrayToInt(m_bytes, offset);
            f = Float.intBitsToFloat(tint);
            offset += 4;
            MUSLog.Log(Float.toString(f), MUSLog.kDeb);
        }
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public byte[] getBytes() {
        byte[] finalbytes = new byte[66];
        ConversionUtils.shortToByteArray((int) getType(), finalbytes, 0);
//        int tint;
//        for (int i = 0; i < 16; i++) {
//            tint = Float.floatToIntBits(m_floats[i]);
//            ConversionUtils.intToByteArray(tint, finalbytes, 2 + (i * 4));
//        }

        System.arraycopy(m_bytes, 0, finalbytes, 2, m_bytes.length);

        return finalbytes;
    }
}
