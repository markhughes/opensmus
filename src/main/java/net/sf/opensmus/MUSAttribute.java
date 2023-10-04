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
 * This class represents a multiuser server Attribute, consisting of a name and a value.
 */
public class MUSAttribute {

    private LSymbol m_name;
    private LValue m_value;

    /**
     * Constructor
     */
    public MUSAttribute(LSymbol name, LValue value) {
        m_name = name;
        m_value = value;
    }

    /**
     * Update the LValue associated with the attribute
     */
    public void set(LValue value) {
        m_value = value;
    }

    /**
     * Retrieve the LValue associated with the attribute
     */
    public LValue get() {
        return m_value;
    }

    /**
     * Get the attribute name as an LSymbol
     */
    public LSymbol getName() {
        return m_name;
    }

    /**
     * Static utility function.
     * Reserved for internal use of OpenSMUS.
     */
    public static LValue getTime() {

        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSSSS");
        java.util.Date currentTime = new java.util.Date();
        String dateString = formatter.format(currentTime);
        return new LString(dateString);
    }

    /**
     * Static utility function.
     * Reserved for internal use of OpenSMUS.
     */
    public static void getSymbolListFromContents(LList attrlist, LValue attributes) {

        if (attributes.getType() == LValue.vt_List) {
            LList atlist = (LList) attributes;
            for (int e = 0; e < atlist.count(); e++) {
                getSymbolListFromContents(attrlist, atlist.getElementAt(e));
            }
        } else if (attributes.getType() == LValue.vt_Symbol) {
            attrlist.addElement(attributes);
        } else if (attributes.getType() == LValue.vt_String) {
            LString elstring = (LString) attributes;
            attrlist.addElement(new LSymbol(elstring.toString()));
        } else {
            // Ignore this attribute silently

        }
    }

    /**
     * Static utility function.
     * Reserved for internal use of OpenSMUS.
     */
    public static void getSetAttributeListsFromContents(LList attrlist, LList vallist, LValue attributes) {

        if (attributes.getType() == LValue.vt_PropList) {
            LPropList atlist = (LPropList) attributes;
            for (int e = 0; e < atlist.count(); e++) {
                LValue thisprop = atlist.getPropAt(e);
                if (thisprop.getType() == LValue.vt_Symbol) {
                    LSymbol tp = (LSymbol) thisprop;
                    attrlist.addElement(tp);
                    vallist.addElement(atlist.getElementAt(e));
                } else if (thisprop.getType() == LValue.vt_String) {
                    LString ts = (LString) thisprop;
                    attrlist.addElement(new LSymbol(ts.toString()));
                    vallist.addElement(atlist.getElementAt(e));
                }
            }
        } else {
            // Ignore this attribute silently

        }

    }

}
