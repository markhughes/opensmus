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
 * Base class representing a Lingo compatible value (LValue for short).
 * Lingo is a trademark of Adobe, Inc. All rights reserved.
 */
public class LValue {

    // http://go.adobe.com/kb/ts_tn_15465_en-us
    public final static short vt_Void = 0;
    public final static short vt_Integer = 1;
    public final static short vt_Symbol = 2;
    public final static short vt_String = 3;
    public final static short vt_Picture = 5;
    public final static short vt_Float = 6;
    public final static short vt_List = 7;
    public final static short vt_Point = 8;
    public final static short vt_Rect = 9;
    public final static short vt_PropList = 10;
    public final static short vt_Color = 18;
    public final static short vt_Date = 19;
    public final static short vt_Media = 20;
    public final static short vt_3dVector = 22;
    public final static short vt_3dTransform = 23;


    protected short valueType;

    /**
     * Constructor
     */
    public LValue() {
    }

    /**
     * Static function to construct an LValue from a Java int.
     * Returns an LInteger value
     */
    public static LValue getLValue(int initval) {
        try {
            return new LInteger(initval);
        } catch (NullPointerException np) {
            MUSLog.Log("Null exception in Lingo type conversion, returning LVoid.", MUSLog.kDeb);
            return new LVoid();
        }
    }

    /**
     * Static function to construct an LValue from a Java String.
     * Returns an LString value
     */
    public static LValue getLValue(String initval) {
        try {
            return new LString(initval);
        } catch (NullPointerException np) {
            MUSLog.Log("Null exception in Lingo type conversion, returning LVoid.", MUSLog.kDeb);
            return new LVoid();
        }
    }

    /**
     * Static function to construct an LValue from a Java double.
     * Returns an LFloat value
     */
    public static LValue getLValue(double initval) {
        try {
            return new LFloat(initval);
        } catch (NullPointerException np) {
            MUSLog.Log("Null exception in Lingo type conversion, returning LVoid.", MUSLog.kDeb);
            return new LVoid();
        }
    }

    /**
     * Static function to construct an LValue from a Java float.
     * Returns an LFloat value
     */
    public static LValue getLValue(float initval) {
        try {
            return new LFloat((double) initval);
        } catch (NullPointerException np) {
            MUSLog.Log("Null exception in Lingo type conversion, returning LVoid.", MUSLog.kDeb);
            return new LVoid();
        }
    }

    /**
     * Static function to construct an LValue from an array of bytes.
     * Returns an LMedia value
     */
    public static LValue getLValue(byte[] initval) {
        try {
            return new LMedia(initval);
        } catch (NullPointerException np) {
            MUSLog.Log("Null exception in Lingo type conversion, returning LVoid.", MUSLog.kDeb);
            return new LVoid();
        }
    }

    /**
     * Sets the type of a newly created LValue.
     * Type is a short LValue type (LValue.vtVoid, LValue.vtString, etc.)
     */
    public void setType(short type) {
        valueType = type;
    }

    /**
     * Returns the type of an LValue (LValue.vtVoid, LValue.vtString, etc.)
     */
    public short getType() {
        return valueType;
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    public int extractFromBytes(byte[] rawBytes, int offset) {
        return 0;
    }


    /**
     * Reserved for internal use of OpenSMUS.
     */
    public byte[] getBytes() {
        return new byte[0];
    }

    /**
     * Reserved for internal use of OpenSMUS.
     */
    public void dump() {
    }

    /**
     * Base method, returns this LValue as a String. Not guaranteed to be implemented for all value types.
     */
    public String toString() {
        return "{LValue type " + Short.toString(valueType) + "}";
    }

    /**
     * Base method, returns this LValue as an integer. Not guaranteed to be implemented for all value types.
     */
    public int toInteger() {
        return 0;
    }

    /**
     * Base method, returns this LValue as a double. Not guaranteed to be implemented for all value types.
     */
    public double toDouble() {
        return 0;
    }

    /**
     * Base method, returns this LValue as bytes. Not guaranteed to be implemented for all value types.
     */
    public byte[] toBytes() {
        return new byte[0];
    }

    /**
     * Static function to construct an LValue from a raw byte array containg a Lingo formatted value and associated type information.
     * Reserved for internal use of OpenSMUS.
     */
    public static LValue fromRawBytes(byte[] rawBytes, int offset) {

        short elemType;
        LValue newVal;

        // First 2 bytes = type identifier
        elemType = ConversionUtils.byteArrayToShort(rawBytes, offset);

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

        // Here we could simply store the incoming raw bytes instead of decoding it further...
        newVal.extractFromBytes(rawBytes, offset + 2);

        return newVal;

    }

}
