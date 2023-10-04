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

public class MUSUDPListener extends Thread {
    MUSUser m_user;
    DatagramSocket m_udpsocket;
    int m_maxpacketsize;
    boolean m_alive = true;


    public MUSUDPListener(MUSUser user, DatagramSocket udpsocket, int maxpacketsize) {
        m_user = user;
        m_udpsocket = udpsocket;
        m_maxpacketsize = maxpacketsize;
        if (maxpacketsize > 16384)
            m_maxpacketsize = 16384;

        start();
    }

    @Override
    public void run() {
        try {
            while (m_alive) {
                byte[] receivebuffer = new byte[m_maxpacketsize];
                DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                m_udpsocket.receive(receivePacket);
                // m_user.logInBytes(receivePacket.getLength());
                m_user.processUDPPacket(receivePacket.getData());
            }
        } catch (IOException e) {
            if (m_alive)
                MUSLog.Log("UDP Socket Read IO exception", MUSLog.kDeb);
        }
    }

    public void send(MUSMessage msg, InetAddress addr, int port) {

        /*
        try {
           DatagramPacket toSend = msg.toDatagramPacket(addr, port, m_user.m_allencrypted);
            m_user.logOutBytes(toSend.getLength());
            m_udpsocket.send(toSend);

        } catch (IOException e) {
            if (m_alive)
                MUSLog.Log("UDP Socket Send IO exception", MUSLog.kDeb);
        }
        */
    }

    public void kill() {
        m_alive = false;
    }
}
