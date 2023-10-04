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

import org.jboss.netty.buffer.ChannelBuffer;

public class MUSBlowfish {

    MUSBlowfishCypher cipher; // Holds the cipher instance

    // Set up the cipher object to use
    // NOT USED ANYMORE
    public MUSBlowfish(String key) {

        cipher = new MUSBlowfishCypher(key.getBytes());
    }

    public MUSBlowfish(byte[] key) {

        cipher = new MUSBlowfishCypher(key);
    }

    // Create an instance using the global key
    public MUSBlowfish() {

        cipher = new MUSBlowfishCypher();
    }


    // Used to decrypt the login content
    public void decode(byte[] data) {

        try {
            // Align to the next 8 byte border
            int originalLength = data.length;

            if ((originalLength & 7) != 0) {
                byte[] tempBuffer = new byte[(originalLength & (~7)) + 8];
                System.arraycopy(data, 0, tempBuffer, 0, originalLength);

                for (int nI = originalLength; nI < tempBuffer.length; nI++)
                    tempBuffer[nI] = 0x20;

                cipher.decrypt(tempBuffer);
                // Return to original size
                System.arraycopy(tempBuffer, 0, data, 0, originalLength);
            } else {
                cipher.decrypt(data);
            }

            cipher.reset();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Not used
    public void decode(ChannelBuffer data) {

        cipher.decrypt(data, data.readableBytes());
    }


    public void decode(ChannelBuffer data, int length) {

        try {

            cipher.decrypt(data, length);
            // Do not reset() since this method can be used incrementally
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Used to encrypt outgoing messages
    public void encode(ChannelBuffer data) {

        cipher.encrypt(data);
        cipher.reset(); // Always reset since we're only using this for complete outgoing messages
    }


    public void reset() {
        cipher.reset();
    }


    // Used to encrypt outgoing messages
    // NOT USED ANYMORE
    public void encode(byte[] data) {

        try {
            // Align to the next 8 byte border
            int originalLength = data.length;

            if ((originalLength & 7) != 0) {
                byte[] tempBuffer = new byte[(originalLength & (~7)) + 8];
                System.arraycopy(data, 0, tempBuffer, 0, originalLength);

                for (int nI = originalLength; nI < tempBuffer.length; nI++)
                    tempBuffer[nI] = 0x20;

                cipher.encrypt(tempBuffer);
                // Restore original size
                System.arraycopy(tempBuffer, 0, data, 0, originalLength);

            } else {
                cipher.encrypt(data);
            }

            cipher.reset();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
