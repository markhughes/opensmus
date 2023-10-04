/*
  Part of OpenSMUS Source Code.
  
  IMPORTANT: Notice that the SMUS protocol (and OpenSMUS) uses a 
  variation of Blowfish, not compatible with the official Blowfish 
  specification or existing implementations. The implementation was
  reverse-engineered by trial and error. The key tables are the same,
  but the encryption/decryption feedback routines are non-standard. 
  
  Note: This class was implemented used a combination of two 
  reference Blowfish implementations (C++ and Java) in 2001, with the
  feedback algorithms adjusted to match the non-standard SMUS behavior. 
  Some of the methods in this file are based on classes 
  published as part of the initial BlowfishForJava v1.7d (08/10/01) 
  open source release.
  Copyright (c) 1997-2001 Markus Hahn <markus_hahn at gmx dot net>
  At that time, no standard open source license was specified in
  the Blowfish4Java distribution. Later versions used LGPL or Apache.
  
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
 * Collection of static methods for data conversion.
 * Data is treated in network byte order.
 * This class is reserved for internal use of OpenSMUS
 */

public class ConversionUtils {


    public static long byteArrayToLong(byte[] buffer,
                                       int nStartIndex) {
        return (((long) buffer[nStartIndex]) << 56) |
                (((long) buffer[nStartIndex + 1] & 0x0ffL) << 48) |
                (((long) buffer[nStartIndex + 2] & 0x0ffL) << 40) |
                (((long) buffer[nStartIndex + 3] & 0x0ffL) << 32) |
                (((long) buffer[nStartIndex + 4] & 0x0ffL) << 24) |
                (((long) buffer[nStartIndex + 5] & 0x0ffL) << 16) |
                (((long) buffer[nStartIndex + 6] & 0x0ffL) << 8) |
                ((long) buffer[nStartIndex + 7] & 0x0ff);
    }

    public static int byteArrayToInt(byte[] buffer,
                                     int nStartIndex) {
        return (((int) buffer[nStartIndex]) << 24) |
                (((int) buffer[nStartIndex + 1] & 0x0ff) << 16) |
                (((int) buffer[nStartIndex + 2] & 0x0ff) << 8) |
                ((int) buffer[nStartIndex + 3] & 0x0ff);
    }

    public static short byteArrayToShort(byte[] buffer,
                                         int nStartIndex) {
        return (short) ((buffer[nStartIndex] << 8) |
                (buffer[nStartIndex + 1] & 0xff));
    }


    public static void longToByteArray(long lValue,
                                       byte[] buffer,
                                       int nStartIndex) {
        buffer[nStartIndex] = (byte) (lValue >>> 56);
        buffer[nStartIndex + 1] = (byte) ((lValue >>> 48) & 0x0ff);
        buffer[nStartIndex + 2] = (byte) ((lValue >>> 40) & 0x0ff);
        buffer[nStartIndex + 3] = (byte) ((lValue >>> 32) & 0x0ff);
        buffer[nStartIndex + 4] = (byte) ((lValue >>> 24) & 0x0ff);
        buffer[nStartIndex + 5] = (byte) ((lValue >>> 16) & 0x0ff);
        buffer[nStartIndex + 6] = (byte) ((lValue >>> 8) & 0x0ff);
        buffer[nStartIndex + 7] = (byte) lValue;
    }


    public static int swapIntBytes(int lValue) {
        byte[] bytes = new byte[4];
        byte[] adjbytes = new byte[4];
        intToByteArray(lValue, bytes, 0);
        adjbytes[0] = bytes[3];
        adjbytes[1] = bytes[2];
        adjbytes[2] = bytes[1];
        adjbytes[3] = bytes[0];
        return byteArrayToInt(adjbytes, 0);
    }


    public static void intToByteArray(int lValue,
                                      byte[] buffer,
                                      int nStartIndex) {
        buffer[nStartIndex] = (byte) (lValue >>> 24);
        buffer[nStartIndex + 1] = (byte) ((lValue >>> 16) & 0x0ff);
        buffer[nStartIndex + 2] = (byte) ((lValue >>> 8) & 0x0ff);
        buffer[nStartIndex + 3] = (byte) lValue;
    }

    public static void shortToByteArray(int lValue,
                                        byte[] buffer,
                                        int nStartIndex) {
        buffer[nStartIndex] = (byte) (0xff & (lValue >> 8));
        buffer[nStartIndex + 1] = (byte) (0xff & lValue);


    }


    public static long intArrayToLong(int[] buffer,
                                      int nStartIndex) {
        return (((long) buffer[nStartIndex]) << 32) |
                (((long) buffer[nStartIndex + 1]) & 0x0ffffffffL);
    }


    public static void longToIntArray(long lValue,
                                      int[] buffer,
                                      int nStartIndex) {
        buffer[nStartIndex] = (int) (lValue >>> 32);
        buffer[nStartIndex + 1] = (int) lValue;
    }


    public static long makeLong(int nLo,
                                int nHi) {
        return (((long) nHi << 32) |
                ((long) nLo & 0x00000000ffffffffL));
    }


    public static int longLo32(long lVal) {
        return (int) lVal;
    }

    public static int longHi32(long lVal) {
        return (int) ((long) (lVal >>> 32));
    }

    final static char[] HEXTAB = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};


    public static String bytesToBinHex(byte[] data) {
        return bytesToBinHex(data, 0, data.length);
    }

    public static String longToBinHex(long lValue) {
        byte[] data = new byte[8];
        longToByteArray(lValue, data, 0);
        return bytesToBinHex(data, 0, data.length);
    }

    public static String intToBinHex(int lValue) {
        byte[] data = new byte[4];
        intToByteArray(lValue, data, 0);
        return bytesToBinHex(data, 0, data.length);
    }


    public static String bytesToBinHex(byte[] data,
                                       int nStartPos,
                                       int nNumOfBytes) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.setLength(nNumOfBytes << 1);

        int nPos = 0;
        for (int nI = 0; nI < nNumOfBytes; nI++) {
            sbuf.setCharAt(nPos++, HEXTAB[(data[nI + nStartPos] >> 4) & 0x0f]);
            sbuf.setCharAt(nPos++, HEXTAB[data[nI + nStartPos] & 0x0f]);
        }
        return sbuf.toString();
    }


    public static int binHexToBytes(String sBinHex,
                                    byte[] data,
                                    int nSrcPos,
                                    int nDstPos,
                                    int nNumOfBytes) {
        int nStrLen = sBinHex.length();

        int nAvailBytes = (nStrLen - nSrcPos) >> 1;
        if (nAvailBytes < nNumOfBytes) {
            nNumOfBytes = nAvailBytes;
        }

        int nOutputCapacity = data.length - nDstPos;
        if (nNumOfBytes > nOutputCapacity) {
            nNumOfBytes = nOutputCapacity;
        }

        int nResult = 0;
        for (int nI = 0; nI < nNumOfBytes; nI++) {
            byte bActByte = 0;
            boolean blConvertOK = true;
            for (int nJ = 0; nJ < 2; nJ++) {
                bActByte <<= 4;
                char cActChar = sBinHex.charAt(nSrcPos++);

                if ((cActChar >= 'a') && (cActChar <= 'f')) {
                    bActByte |= (byte) (cActChar - 'a') + 10;
                } else {
                    if ((cActChar >= '0') && (cActChar <= '9')) {
                        bActByte |= (byte) (cActChar - '0');
                    } else {
                        blConvertOK = false;
                    }
                }
            }
            if (blConvertOK) {
                data[nDstPos++] = bActByte;
                nResult++;
            }
        }

        return nResult;
    }

    public static String byteArrayToUNCString(byte[] data,
                                              int nStartPos,
                                              int nNumOfBytes) {
        // We need two bytes for every character
        nNumOfBytes &= ~1;

        // Enough bytes in the buffer?
        int nAvailCapacity = data.length - nStartPos;

        if (nAvailCapacity < nNumOfBytes) {
            nNumOfBytes = nAvailCapacity;
        }

        StringBuffer sbuf = new StringBuffer();
        sbuf.setLength(nNumOfBytes >> 1);

        int nSBufPos = 0;

        while (nNumOfBytes > 0) {
            sbuf.setCharAt(nSBufPos++,
                    (char) (((int) data[nStartPos] << 8) | ((int) data[nStartPos + 1] & 0x0ff)));
            nStartPos += 2;
            nNumOfBytes -= 2;
        }

        return sbuf.toString();
    }
}
