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
 * Class representing a Lingo compatible Float value (LFloat for short).
 * Lingo is a trademark of Adobe, Inc. All rights reserved.
 */
public class LFloat extends LValue {

    // private double m_float;
    private byte[] m_bytes;

    /**
     * Constructor
     */
    public LFloat(double initInt) {
        // m_float = initInt;
        m_bytes = new byte[8];
        long tlong = Double.doubleToLongBits(initInt);
        ConversionUtils.longToByteArray(tlong, m_bytes, 0);
        setType(LValue.vt_Float);
    }

    /**
     * Constructor
     */
    public LFloat() {
        // m_float = 0;
        m_bytes = new byte[8];
        setType(LValue.vt_Float);
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public int extractFromBytes(byte[] rawBytes, int offset) {

        System.arraycopy(rawBytes, offset, m_bytes, 0, 8);
        return 8;

        // long tlong = ConversionUtils.byteArrayToLong(rawBytes, offset);
        //m_float = Double.longBitsToDouble(tlong);
    }

    /**
     * Returns this LFloat as a Java double.
     */
    public double toDouble() {
        long tlong = ConversionUtils.byteArrayToLong(m_bytes, 0);
        return ( Double.longBitsToDouble(tlong) );
        // return m_float;
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public byte[] getBytes() {
        byte[] finalbytes = new byte[10];
        ConversionUtils.shortToByteArray((int) vt_Float, finalbytes, 0);
        // long tlong = Double.doubleToLongBits(m_float);
        // ConversionUtils.longToByteArray(tlong, finalbytes, 2);
        System.arraycopy(m_bytes, 0, finalbytes, 2, m_bytes.length);

        return finalbytes;
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public void dump() {

        MUSLog.Log("Float> " + this.toDouble(), MUSLog.kDeb);
    }


    @Override
    public String toString() {

        return Double.toString(toDouble());
    }

}
