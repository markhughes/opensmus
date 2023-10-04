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

import org.jboss.netty.buffer.ChannelBuffer;

import java.util.*;
import java.io.*;

/**
 * Class representing a list of MUSMsgHeaderString objects, stored as a Java Vector.
 */
public class MUSMsgHeaderStringList {

    /**
     * Public vector element storing the MUSMsgHeaderString members.
     * It is safe to access the elements directly.
     */
    public Vector<MUSMsgHeaderString> m_stringlist;

    /**
     * Default Constructor
     */
    public MUSMsgHeaderStringList() {
        m_stringlist = new Vector<MUSMsgHeaderString>();
    }

    /**
     * Adds a MUSMsgHeaderString to the list
     *
     * @param elem MUSMsgHeaderString to add
     * @return boolean
     */
    public boolean addElement(MUSMsgHeaderString elem) {
        m_stringlist.addElement(elem);

        return true;
    }

    /**
     * Retrieves an Enumeration object containing the MUSMsgHeaderString elements.
     */
    public Enumeration<MUSMsgHeaderString> elements() {
        return m_stringlist.elements();
    }

    /**
     * Retrieves an Enumeration object containing the MUSMsgHeaderString elements.
     */
    public ArrayList<String> getAllRecipients() {

        ArrayList<String> recep = new ArrayList<String>(m_stringlist.size());
        Enumeration r = m_stringlist.elements();
        while (r.hasMoreElements()) {
            recep.add(r.nextElement().toString());
        }
        return recep; // m_stringlist.toArray(new MUSMsgHeaderString[0]);
    }

    /**
     * Removes all MUSMsgHeaderString elements.
     */
    public void clear() {
         m_stringlist.clear();
    }

    /**
     * Returns the number of MUSMsgHeaderString elements.
     */
    public int count() {
         return m_stringlist.size();
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    public void extractMUSMsgHeaderStringList(ChannelBuffer buffer) {

        int numStrings = buffer.readInt();
        MUSMsgHeaderString tempStr;
        for (int a = 0; a < numStrings; a++) {
            tempStr = new MUSMsgHeaderString();
            tempStr.extractMUSMsgHeaderString(buffer);
            m_stringlist.addElement(tempStr);
        }
    }


    /**
     * Reserved for internal use of OpenSMUS.
     */
    public void dump() {

        for (MUSMsgHeaderString ms : m_stringlist) {
            MUSLog.Log("String list: ", MUSLog.kDeb);
            ms.dump();
        }
    }

    @Override
    public String toString() {

        StringBuffer temp = new StringBuffer();
        temp.append("MUSMsgHeaderStringList{");

        for (MUSMsgHeaderString ms : m_stringlist) {
            temp.append(ms.toString()).append(", ");
        }
        temp.setLength(temp.length() - 2);
        temp.append("}");

        return temp.toString();
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    public byte[] getBytes() {

        try { // @TODO: Convert to ByteBuffer
            ByteArrayOutputStream bstream = new ByteArrayOutputStream(2);
            DataOutputStream datastream = new DataOutputStream(bstream);

            datastream.writeInt(m_stringlist.size());

            for (MUSMsgHeaderString elem : m_stringlist) {
                byte[] elemBytes = elem.getBytes();
                datastream.write(elemBytes, 0, elemBytes.length);
            }

            return bstream.toByteArray();
        } catch (IOException e) {
            MUSLog.Log("Error in stringlist stream", MUSLog.kDeb);
            return "0".getBytes();
        }
    }

}
