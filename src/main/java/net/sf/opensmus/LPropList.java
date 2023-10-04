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

import java.util.*;
import java.io.*;

/**
 * Class representing a Lingo compatible List value (LList for short).
 * Lingo is a trademark of Adobe, Inc. All rights reserved.
 */
public class LPropList extends LValue {

    /**
     * Public vector element storing the property names as LSymbols
     */
    public Vector<LValue> m_proplist;
    /**
     * Public vector element storing the list members as LValues
     */
    public Vector<LValue> m_list;

    /**
     * Constructor
     */
    public LPropList() {
        m_proplist = new Vector<LValue>();
        m_list = new Vector<LValue>();
        setType(LValue.vt_PropList);
    }

    /**
     * Adds an LValue element to the list
     *
     * @param property LSymbol with property name
     * @param elem     LValue to add
     * @return boolean
     */
    public boolean addElement(LValue property, LValue elem) {
        m_proplist.addElement(property);
        m_list.addElement(elem);

        return true;
    }


    /**
     * Fetches an LValue element from the list
     *
     * @param pos index of the element to be retrieved
     * @return LValue
     */
    public LValue getElementAt(int pos) {
        return m_list.elementAt(pos);
    }

    /**
     * Fetches an LValue property name from the list
     *
     * @param pos index of the property to be retrieved
     * @return LValue
     */
    public LValue getPropAt(int pos) {
        return m_proplist.elementAt(pos);
    }

    /**
     * Fetches an LValue element from the list
     *
     * @param prop LSymbol representing the property name
     * @return LValue
     */
    public LValue getElement(LSymbol prop) throws PropertyNotFoundException {

        // Enumeration enume = m_proplist.elements();
        int idx = 0;
        // LValue elem;
        // while (enume.hasMoreElements()) {
        for (LValue elem : m_proplist) {
            // elem = (LValue) enume.nextElement();
            if (elem.getType() != LValue.vt_Symbol) continue;
            LSymbol sym = (LSymbol) elem;
            if (prop.toString().equalsIgnoreCase(sym.toString())) {
                return m_list.elementAt(idx);
            }
            idx++;
        }

        // return new LVoid();
        throw new PropertyNotFoundException(prop.toString());
    }

    /**
     * Returns the number of elements in the list
     */
    public int count() {
        return m_proplist.size();
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public int extractFromBytes(byte[] rawBytes, int offset) {
        int numOfElems = ConversionUtils.byteArrayToInt(rawBytes, offset);
        int chunkSize = 4;
        int elemSize = 0;
        short elemType = 0;
        LValue newProp;
        LValue newVal;
        for (int i = 0; i < numOfElems; i++) {
            // Extract prop
            // Extract element type (should be symbol anyway) //corrected to accept any type
            elemType = ConversionUtils.byteArrayToShort(rawBytes, offset + chunkSize);
            chunkSize += 2;

            switch (elemType) {
                case LValue.vt_Void:
                    newProp = new LVoid();
                    break;

                case LValue.vt_Integer:
                    newProp = new LInteger();
                    break;

                case LValue.vt_Symbol:
                    newProp = new LSymbol();
                    break;

                case LValue.vt_String:
                    newProp = new LString();
                    break;

                case LValue.vt_Picture:
                    newProp = new LPicture();
                    break;

                case LValue.vt_Float:
                    newProp = new LFloat();
                    break;

                case LValue.vt_List:
                    newProp = new LList();
                    break;

                case LValue.vt_Point:
                    newProp = new LPoint();
                    break;

                case LValue.vt_Rect:
                    newProp = new LRect();
                    break;

                case LValue.vt_PropList:
                    newProp = new LPropList();
                    break;

                case LValue.vt_Color:
                    newProp = new LColor();
                    break;

                case LValue.vt_Date:
                    newProp = new LDate();
                    break;

                case LValue.vt_Media:
                    newProp = new LMedia();
                    break;

                case LValue.vt_3dVector:
                    newProp = new L3dVector();
                    break;

                case LValue.vt_3dTransform:
                    newProp = new L3dTransform();
                    break;

                default:
                    newProp = new LVoid();
                    break;

            }

            chunkSize = chunkSize + newProp.extractFromBytes(rawBytes, offset + chunkSize);
            m_proplist.addElement(newProp);

            // Extract element
            elemType = ConversionUtils.byteArrayToShort(rawBytes, offset + chunkSize);
            chunkSize += 2;

            switch (elemType) {
                case LValue.vt_Void:
                    newVal = new LVoid();
                    break;

                case LValue.vt_Integer:
                    newVal = new LInteger();
                    break;

                case LValue.vt_Symbol:
                    newVal = new LSymbol();
                    break;

                case LValue.vt_String:
                    newVal = new LString();
                    break;

                case LValue.vt_Picture:
                    newVal = new LPicture();
                    break;

                case LValue.vt_Float:
                    newVal = new LFloat();
                    break;

                case LValue.vt_List:
                    newVal = new LList();
                    break;

                case LValue.vt_Point:
                    newVal = new LPoint();
                    break;

                case LValue.vt_Rect:
                    newVal = new LRect();
                    break;

                case LValue.vt_PropList:
                    newVal = new LPropList();
                    break;

                case LValue.vt_Color:
                    newVal = new LColor();
                    break;

                case LValue.vt_Date:
                    newVal = new LDate();
                    break;

                case LValue.vt_Media:
                    newVal = new LMedia();
                    break;

                case LValue.vt_3dVector:
                    newVal = new L3dVector();
                    break;

                case LValue.vt_3dTransform:
                    newVal = new L3dTransform();
                    break;

                default:
                    newVal = new LVoid();
                    break;

            }
            chunkSize = chunkSize + newVal.extractFromBytes(rawBytes, offset + chunkSize);
            m_list.addElement(newVal);

        }
        return chunkSize;
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public void dump() {

        for (LValue temp : m_proplist) {
            MUSLog.Log("proplist property: ", MUSLog.kDeb);
            temp.dump();
        }

        for (LValue temp : m_list) {
            MUSLog.Log("proplist element: ", MUSLog.kDeb);
            temp.dump();
        }
    }


    @Override
    public String toString() {

       StringBuffer s = new StringBuffer("[");
       for (int n=0; n< m_proplist.size(); n++) {
           s.append(m_proplist.get(n).toString());
           s.append(": ");
           s.append(m_list.get(n).toString());
           s.append(", ");
       }
        if (s.length() > 2) s.setLength(s.length() -2);
        s.append("]");
       return s.toString();
    }
    

    /**
     * Reserved for internal use of OpenSMUS.
     */
    @Override
    public byte[] getBytes() {

        try {
            ByteArrayOutputStream bstream = new ByteArrayOutputStream(2);
            DataOutputStream datastream = new DataOutputStream(bstream);

            datastream.writeShort(vt_PropList);
            datastream.writeInt(m_proplist.size());

            byte[] elemBytes;
            LValue elem;
            Enumeration enume = m_proplist.elements();
            Enumeration enum2 = m_list.elements();
            while (enume.hasMoreElements()) {
                elem = (LValue) enume.nextElement();
                elemBytes = elem.getBytes();
                datastream.write(elemBytes, 0, elemBytes.length);
                elem = (LValue) enum2.nextElement();
                elemBytes = elem.getBytes();
                datastream.write(elemBytes, 0, elemBytes.length);
            }

            return bstream.toByteArray();
        } catch (IOException e) {
            MUSLog.Log("Error in LPropList stream", MUSLog.kSys);
            return "0".getBytes();
        }
    }

}
