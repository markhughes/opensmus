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

public class MUSLogonMessage extends MUSMessage {

    private static final LSymbol MOVIEID = new LSymbol("!movieID");
    private static final LSymbol USERID = new LSymbol("!userID");
    private static final LSymbol PASSWORD = new LSymbol("!password");
    private static final LSymbol PATHNAME = new LSymbol("!pathname");
    private static final LSymbol LOGON = new LSymbol("!Logon");
    private static final LSymbol UDPPORT = new LSymbol("!udpport");
    private static final LSymbol LOCALADDRESS = new LSymbol("!localaddress");

    public String m_moviename;
    public String m_userID;
    public String m_password;
    public String m_pathname = "default";
    public LValue m_logon = new LVoid();
    public LValue m_localUDPPortInfo = new LVoid();
    public LValue m_localAddressInfo = new LVoid();
    String m_localUDPAddress;
    int m_localUDPPort;

    private MUSBlowfish m_cipher; // Only used to decrypt the logon contents

    int m_logonPacketFormat = 0; // 0 = old style (list with 3 items), 1 = SMUS 3 style (property list)

    public MUSLogonMessage() {
        super();
        m_cipher = new MUSBlowfish();
    }

    @Override
    public void extractMUSMessage(ChannelBuffer msg) {

        byte[] rawContents = readRawBytes(msg);

        // Only for Login messages: decrypt the content
        m_cipher.decode(rawContents);

        m_msgContent = LValue.fromRawBytes(rawContents, 0);
    }


    public boolean extractLoginInfo() {

        LValue info = m_msgContent;

        if (info.getType() == LValue.vt_List) {
            // Original login package format
            LList loginfo = (LList) info;
            LString moviename = (LString) loginfo.getElementAt(0);
            m_moviename = moviename.toString();

            LString userID = (LString) loginfo.getElementAt(1);
            m_userID = userID.toString();

            LString pass = (LString) loginfo.getElementAt(2);
            m_password = pass.toString();

            return true;
        } else if (info.getType() == LValue.vt_PropList) {
            // New format
            m_logonPacketFormat = 1;
            LPropList ploginfo = (LPropList) info;

            try {
                LString moviename = (LString) ploginfo.getElement(MOVIEID);
                m_moviename = moviename.toString();

                LString userID = (LString) ploginfo.getElement(USERID);
                m_userID = userID.toString();

                LString pass = (LString) ploginfo.getElement(PASSWORD);
                m_password = pass.toString();
            } catch (PropertyNotFoundException pnf) {
                // FatalError
                return false;
            }

            try {
                LString pathname = (LString) ploginfo.getElement(PATHNAME);
                MUSLog.Log("Logon req from " + pathname.toString(), MUSLog.kDebWarn);
                m_pathname = pathname.toString();
            } catch (PropertyNotFoundException pnf) {
                // Optional property
            }

            try {
                m_logon = ploginfo.getElement(LOGON);  // What is this used for?
            } catch (PropertyNotFoundException pnf) {
                // Optional property
            }

            try {
                m_localUDPPortInfo = ploginfo.getElement(UDPPORT); // The client wants to use UDP
                if (m_localUDPPortInfo.getType() == LValue.vt_Integer) {
                    LInteger m_localUDPPortInt = (LInteger) m_localUDPPortInfo;
                    m_localUDPPort = m_localUDPPortInt.toInteger();
                }
            } catch (PropertyNotFoundException pnf) {
                // Optional property
            }

            try {
                m_localAddressInfo = ploginfo.getElement(LOCALADDRESS);
                if (m_localAddressInfo.getType() == LValue.vt_String) {
                    m_localUDPAddress = m_localAddressInfo.toString();
                }
            } catch (PropertyNotFoundException pnf) {
                // Optional property
            }

            return true;
        } // End proplist package

        // Unknown content for list
        return false;

    }
}
