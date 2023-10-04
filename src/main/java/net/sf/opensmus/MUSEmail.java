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

import java.net.*;
import java.io.*;

public class MUSEmail extends Thread {

    Socket socket;
    PrintStream ps;
    BufferedReader dis;
    InetAddress rina;
    InetAddress lina;
    String m_sender;
    String m_recpt;
    String m_subject;
    String m_smtphost;
    String[] m_data;

    public MUSEmail(String sender, String recpt, String subject, String smtphost, String[] data) {
        // Open connection to SMTP server
        // smtp port is 25

        m_sender = sender;
        m_recpt = recpt;
        m_subject = subject;
        m_smtphost = smtphost;
        m_data = data;

        start();

    }

    public void run() {

        try {
            try {
                socket = new Socket(m_smtphost, 25);

                rina = socket.getInetAddress();
                lina = rina.getLocalHost();
                ps = new PrintStream(socket.getOutputStream());
                // dis = new DataInputStream(socket.getInputStream());
                dis = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Send message
                sendline("HELO " + lina.toString());
                sendline("MAIL FROM:" + m_sender);
                sendline("RCPT TO:" + m_recpt);
                sendline("DATA");
                sendnowait("To:      " + m_recpt);
                sendnowait("From:    " + m_sender);
                sendnowait("Subject: " + m_subject);
                sendnowait("");
                for (String aM_data : m_data) {
                    sendnowait(aM_data);
                }
                // Send a line with a . to close the connection
                sendline(".");
            }

            catch (Exception ex) {
                socket.close();
            }

            // Log action
            MUSLog.Log("Email message sent to " + m_recpt, MUSLog.kDeb);
            socket.close();

        } catch (Exception ex) {
            // Fatal exception
        }

    }

    void sendline(String data) throws IOException {
        ps.println(data);
        ps.flush();
        String s = dis.readLine();
    }

    void sendnowait(String data) throws IOException {
        ps.println(data);
        ps.flush();
    }

}