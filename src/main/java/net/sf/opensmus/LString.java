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

import java.io.UnsupportedEncodingException;

/**
 * Class representing a Lingo compatible String value (LString for short).
 * Lingo is a trademark of Adobe, Inc. All rights reserved.
 */
public class LString extends LValue {

    private byte[] m_string;

    /**
     * Constructor
     */
    public LString(String initString) {

        //L does not accept 0 length strings
        //        if (initString.length() == 0)
        //            m_string = "".getBytes();
        //        else

        m_string = initString.getBytes();

        setType(LValue.vt_String);
    }

    /**
     * Constructor
     */
    public LString() {
        m_string = "".getBytes();
        setType(LValue.vt_String);
    }

    /**
     * Constructs a LString from a java String using the named charset.
     */
    public LString(String initString, String charsetName) throws UnsupportedEncodingException {

        m_string = initString.getBytes(charsetName);

        setType(LValue.vt_String);
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public int extractFromBytes(byte[] rawBytes, int offset) {

        int strSize = ConversionUtils.byteArrayToInt(rawBytes, offset); // First 4 bytes = length of string
        // m_string = new String(rawBytes, 4 + offset, strSize);

        // Sanity check
        if (strSize < 0 || strSize > (rawBytes.length - offset - 4)) {
            MUSLog.Log("String size error : " + strSize + " " + rawBytes.length + " " + ConversionUtils.bytesToBinHex(rawBytes), MUSLog.kDeb);
            throw new NullPointerException("String size error " + strSize + " " + rawBytes.length);
        }

        m_string = new byte[strSize];
        System.arraycopy(rawBytes, 4 + offset, m_string, 0, strSize);

        int chunkSize = 4 + strSize; // m_string.length
        if ((strSize % 2) != 0) chunkSize++; // Variable byte length sections are padded to even byte boundaries.

        return chunkSize;
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public byte[] getBytes() {

        int finalSize = 6 + m_string.length; // Type identifier & length info takes up 6 bytes.

        // Variable byte length sections are padded to even byte boundaries.
        if ((finalSize % 2) != 0) finalSize++;

        byte[] finalbytes = new byte[finalSize];
        // byte[] finalbytes = new byte[(m_string.length % 2 == 0) ? m_string.length + 6 : m_string.length +7];

        ConversionUtils.shortToByteArray((int) getType(), finalbytes, 0); // 2 bytes = Type Identifier
        ConversionUtils.intToByteArray(m_string.length, finalbytes, 2); // 4 bytes = string length
        System.arraycopy(m_string, 0, finalbytes, 6, m_string.length); // string data

        // Not necessary, bytes default value is 0
        // if (addPadding) finalbytes[finalSize - 1] = 0x00; // Variable byte length sections are padded to even byte boundaries.

        return finalbytes;
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public void dump() {

        MUSLog.Log("String> " + new String(m_string), MUSLog.kDeb);
    }

    /**
     * Returns this LString as a Java String.
     */
    @Override
    public String toString() {

        return new String(m_string);
    }

    /**
     * Returns this LString as a Java String using a specified charset.
     */
    public String toString(String charsetName) throws UnsupportedEncodingException {

        return new String(m_string, charsetName);
    }

}
