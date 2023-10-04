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

import org.jboss.netty.channel.*;

import java.io.*;
import java.util.*;
import java.net.*;

import net.sf.opensmus.io.SMUSPipeline;

/////////////////////////////////////////////////////////////
public class MUSUser implements ServerUser {

    private MUSServer m_server;

    // Netty
    Channel channel;
    Channel udpchannel;
    public static ChannelFutureListener REPORT_CLOSE = new ChannelFutureListener() {  // Used to track channel close during dev
        public void operationComplete(ChannelFuture future) {
            MUSUser whatUser = ((SMUSPipeline) future.getChannel().getPipeline()).user;
            if (future.isSuccess()) {
               // MUSLog.Log("Close success for " + whatUser, MUSLog.kDeb);
            } else {
                MUSLog.Log("Close failure for " + whatUser + ": " + future.getCause(), MUSLog.kDeb);
                whatUser.m_scheduledToDie = false;
                future.getChannel().close(); // Try again...
            }
        }
    };

    private DatagramSocket m_udpsocket = null;
    MUSUDPListener m_udplistener;
    InetSocketAddress m_UDPSocketAddress; // Used by Netty to write outgoing messages
    private int m_udpportnumber = 0;
    public int m_udpcookie = 0;
    public boolean m_udpenabled = false;

    private Thread m_timer;
    boolean m_scheduledToDie = false;

    public boolean logged = false;

    public String m_pathname; // Added so we can check this value after logging on
    public int ip;

    // These are ServerUser properties
    public String m_name = "";
    public MUSMovie m_movie;
    public int m_userlevel = 0;
    private Vector<ServerGroup> m_grouplist = new Vector<ServerGroup>();
    private int m_creationtime = 0;


    /////////////////////////////////////////////////////////////

    public MUSUser(MUSServer svr, Channel s) throws IOException {

        m_server = svr;
        channel = s;
        m_creationtime = m_server.timeStamp();

        ip = this.ipAsInteger();

        m_name = "tmp_login_" + channel.getId();
    }


    public void setUDPEnabled(MUSLogonMessage logmsg) {

        if (m_movie.m_props.getIntProperty("EnableUDP") != 1)
            return;

        if (logmsg.m_localUDPPortInfo.getType() == LValue.vt_Void) // Check if the client logged on with UDP enabled. (Used #localUDPPort in the connectToNetServer call)
            return;

        InetAddress m_userUDPAddress;
        if (logmsg.m_localAddressInfo.getType() == LValue.vt_Void) {
            // Get from tcp socket
            m_userUDPAddress = ((InetSocketAddress) channel.getRemoteAddress()).getAddress();
        } else {
            // User specified ipaddress
            try {
                m_userUDPAddress = InetAddress.getByName(logmsg.m_localUDPAddress);
            } catch (UnknownHostException uh) {
                return;
            }
        }

        // Store the SocketAddress object used when Netty writes outgoing messages
        m_UDPSocketAddress = new InetSocketAddress(m_userUDPAddress, logmsg.m_localUDPPort);
        MUSLog.Log("Remote UDP socket : " + m_UDPSocketAddress, MUSLog.kDeb);

        if (!createUDPSocket())
            return;

        // Everything ok, enable UDP
        m_udpenabled = true;
    }


    /////////////////////////////////////////////////////////////

    public boolean createUDPSocket() {

        try {
            InetAddress iad;
            String ipaddress = m_server.m_udpAddress;

            if (ipaddress.equalsIgnoreCase("default"))
                iad = InetAddress.getLocalHost();
            else
                iad = InetAddress.getByName(ipaddress);

            m_udpportnumber = m_server.getUDPPortNumber();

            // Netty
            udpchannel = m_server.UDPBootstrap.bind(new InetSocketAddress(iad, m_udpportnumber));
            // Set the UDP channel's user to the "parent" tcp channel's user
            ((SMUSPipeline) udpchannel.getPipeline()).user = this;

            // If you are using NIO and DatagramChannel.write() you have to connect the channel to the target, 
            // so that NIO knows where to send the packets, because the write() API doesn't have the target address
            udpchannel.connect(m_UDPSocketAddress);

            /*
            m_udpsocket = new DatagramSocket(m_udpportnumber, iad);

            m_udplistener = new MUSUDPListener(this, m_udpsocket, m_server.m_props.getIntProperty("MaxUDPPacket"));
            */

            m_server.addUDPPort(m_udpportnumber);

            MUSLog.Log("UDP socket created > " + m_udpportnumber, MUSLog.kDeb);
            return true;


        } catch (IOException e) {
            MUSLog.Log("UDP socket not created > " + m_udpportnumber, MUSLog.kDeb);
            m_udpsocket = null;
            return false;
        }

    }

    public void replyUDPInformation() {

        Random rd = new Random();
        m_udpcookie = rd.nextInt();

        String udpAdd = m_server.m_udpAddress;

        if (m_server.m_udpAddress.equalsIgnoreCase("default")) {
            try {
                InetAddress iad = InetAddress.getLocalHost();
                udpAdd = iad.getHostAddress();
            } catch (UnknownHostException uhe) {
                MUSLog.Log("Unknown host exception while getting localhost address, udp reply aborted", MUSLog.kDeb);
                return;
            }
        }

        String udpadd = udpAdd + ":" + m_udpportnumber;
        String udpcook = Integer.toString(m_udpcookie);

        MUSMessage reply = new MUSMessage();
        reply.m_errCode = 0;
        reply.m_timeStamp = 0;
        reply.m_subject = new MUSMsgHeaderString("udp");
        reply.m_senderID = new MUSMsgHeaderString("System");
        reply.m_recptID = new MUSMsgHeaderStringList();
        reply.m_recptID.addElement(new MUSMsgHeaderString(m_name));
        reply.m_msgContent = new LValue();
        LList ls = new LList();
        ls.addElement(new LString(udpadd));
        ls.addElement(new LString(udpcook));
        reply.m_msgContent = ls;

        sendMessage(reply);
    }

    public void replyLogon(MUSLogonMessage msg) {

        if (m_udpenabled)
            replyUDPInformation();

        MUSMessage reply = new MUSMessage();
        reply.m_errCode = 0;
        reply.m_timeStamp = 0;
        reply.m_subject = new MUSMsgHeaderString("Logon");
        reply.m_senderID = new MUSMsgHeaderString("System");
        reply.m_recptID = new MUSMsgHeaderStringList();

        if (msg.m_userID != null)
            reply.m_recptID.addElement(new MUSMsgHeaderString(msg.m_userID));
        else
            reply.m_recptID.addElement(new MUSMsgHeaderString());

        if (msg.m_moviename != null)
            reply.m_msgContent = new LString(msg.m_moviename);
        else
            reply.m_msgContent = new LString();

        sendMessage(reply);
    }

    public void replyLogonError(MUSLogonMessage msg, int error) {

        MUSMessage reply = new MUSMessage();
        reply.m_errCode = error;
        reply.m_timeStamp = 0;
        reply.m_subject = new MUSMsgHeaderString("Logon");
        reply.m_senderID = new MUSMsgHeaderString("System");
        reply.m_recptID = new MUSMsgHeaderStringList();

        if (msg.m_userID != null) // Only available in logon messages
            reply.m_recptID.addElement(new MUSMsgHeaderString(msg.m_userID));
        else
            reply.m_recptID.addElement(new MUSMsgHeaderString());

        if (msg.m_moviename != null)
            reply.m_msgContent = new LString(msg.m_moviename);
        else
            reply.m_msgContent = new LString();

        sendMessage(reply);
    }

    // NOT USED ANYMORE
    public void processUDPPacket(byte[] content) {
        try {

            ByteArrayInputStream bs = new ByteArrayInputStream(content);
            DataInputStream ds = new DataInputStream(bs);

            // Grab the first 8 bytes of the incoming message so we can see how long this message is
            // (We need to read 8 bytes in order to decrypt it in case we are using #All encryption)
            byte[] headers = new byte[8];
            int bytesRead = ds.read(headers, 0, 8);
            if (bytesRead != 8) return;

            byte[] decodedheaders = new byte[8];
            System.arraycopy(headers, 0, decodedheaders, 0, 8);

            // TEMP COMMENTED
            //      if (m_server.m_allencrypted) {
            //          cipher.decode(decodedheaders);
            //      }

            // Check that the packet has the SMUS signature bytes
            if (decodedheaders[0] != 114) return;
            if (decodedheaders[1] != 0) return;

            int msgsize = ConversionUtils.byteArrayToInt(decodedheaders, 2); // Next 4 bytes is full message byte size (rest of the data)

            // Allocate room for the entire message (including the 2 header bytes and the 4 length info bytes)
            byte[] finalmsg = new byte[msgsize + 6];

            // Read the rest of the data
            // The -2 below is because we have read 2 bytes of the rest of the data already
            // (The initial 8 bytes we read consists of 2 bytes ID, 4 bytes length and 2 bytes from the rest of the data)
            ds.readFully(finalmsg, 8, (msgsize - 2));

            // Add back the first 8 bytes we read in during initial block decode
            System.arraycopy(headers, 0, finalmsg, 0, 8);

            // TEMP COMMENTED
            //      if (m_server.m_allencrypted) {
            //           cipher.decode(finalmsg);
            //       }

            MUSMessage msg;

            if (logged)
                msg = new MUSMessage();
            else
                return;

            // TEMP COMMENTED     msg.extractMUSMessage(finalmsg, 6);
            msg.m_udp = true;

            // Check if udpcookie is correct in the timestamp slot
            if (m_udpcookie == msg.m_timeStamp) {
                postMessage(msg);
                // threadDate = new java.util.Date();
            }
        } catch (Exception e) {
            MUSLog.Log("Error reading UDP packet data from user " + name(), MUSLog.kUsr);

        }
    }

    /////////////////////////////////////////////////////////////


    // Waits 600 ms and then calls killMUSUser()
    public void deleteUser() {

        if (!m_scheduledToDie) {
            m_timer = new MUSKillUserTimer(this, 600);
            m_scheduledToDie = true;
        }
    }


    public void killMUSUser() {

        disconnectFromMovie();

        if (m_udpenabled)
            m_server.releaseUDPPort(m_udpportnumber);

        if (m_udplistener != null)
            m_udplistener.kill();

        //  if (m_sendqueue != null)
        //     m_sendqueue.kill();

        // Netty
        if (channel.isOpen()) {
            MUSLog.Log("Channel open in killMUSUser(), closing...: " + name(), MUSLog.kUsr);
            channel.close().addListener(REPORT_CLOSE);
        }


        /*

        try {
            // Java 2 and up only
            socket.shutdownInput();
            socket.shutdownOutput();

            in.close();
            out.close();

            if (m_udpsocket != null)
                m_udpsocket.close();

            MUSLog.Log("User socket for user " + name() + " closed", MUSLog.kDebWarn);

            socket.close();
            if (m_isConnected) m_isConnected = false;

        } catch (IOException ioe) {
            MUSLog.Log("IOException received while closing socket", MUSLog.kDebWarn);
            MUSLog.Log(ioe, MUSLog.kDebWarn);
            MUSLog.Log(name() + "socket is still open", MUSLog.kDeb);
            if (m_isConnected) m_isConnected = false;
        }
        */
    }

    public void disconnectFromMovie() {
        if (m_movie != null) {
            m_movie.removeUser(this);
            m_movie = null;
        }

        m_grouplist.clear();

        m_server.removeMUSUser(this);
        // MUSLog.Log("Removed " + name() + " from m_clientlist", MUSLog.kUsr);
    }

    public void addToMovie(MUSMovie newmov) {

        m_movie = newmov;
        m_movie.addUser(this);
        MUSLog.Log("User " + name() + " logged to movie " + m_movie.name(), MUSLog.kUsr);

        // Join allusers group directly
        MUSGroup mg;
        try {
            mg = m_movie.getGroup("@AllUsers");
        } catch (GroupNotFoundException gnf) {
            mg = new MUSGroup(m_movie, "@AllUsers");
        } catch (MUSErrorCode err) {
            // Should not happen, @Allusers group is safe
            // Got an error code here
            // err.printStackTrace();
            mg = new MUSGroup(m_movie, "@AllUsers");
        }

        try {
            mg.addUser(this);
        } catch (MUSErrorCode err) {
            // Someone disabled AllUsers group... should be OK
        }
    }


    public void logDroppedMsg() {
        m_server.logDroppedMsg();
    }


    // ServerUser interface methods

    public void sendMessage(MUSMessage msg) {
        if (msg.m_udp && m_udpenabled) {
            MUSLog.Log("Writing outgoing UDP message : " + msg, MUSLog.kDeb);
            udpchannel.write(msg, m_UDPSocketAddress); // m_udplistener.send(msg, m_userUDPAddress, m_userUDPPort);
        } else {
            // Netty
            // MUSLog.Log("Writing outgoing message to " + m_name + ": " + msg, MUSLog.kDeb);
            channel.write(msg);
        }
    }

    public void postMessage(MUSMessage msg) {

        // Simple prevention of flooding with same packet, by timestamp
        /* if (m_enablefloodprevention) {
            if (msg.m_timeStamp > 0) {
                if (msg.m_timeStamp <= m_lastTimestamp) {
                    if (++m_repeatTimestamps >= m_floodthreshold) {
                        MUSLog.Log("User " + name() + " disconnected by flood prevention filter", MUSLog.kSrv);
                        deleteUser();
                    }
                } else {
                    m_lastTimestamp = msg.m_timeStamp;
                    m_repeatTimestamps = 0;
                }
            }
        } */

        // m_movie.m_dispatcher.queue(new MUSQueuedMessage(this, msg)); // Can cause nullpointer exception (Called from messageReceived(IOHandler.java:59)
        // Call handleMsg() directly instead since there's no queue anymore
        // m_movie can be null!
        if (m_movie != null) {
            m_movie.handleMsg(this, msg);
        }
    }

    public String name() {
        return m_name;
    }

    public String pathname() {
        return m_pathname;
    }

    public int userLevel() {
        return m_userlevel;
    }

    public void setuserLevel(int level) {

        m_userlevel = level;

        // Netty
        if (level >= m_server.m_props.getIntProperty("AntiFloodUserLevelIgnore")) {
            try {
                channel.getPipeline().remove("floodfilter");
            }
            catch (NoSuchElementException e) {
                // MUSLog.Log("Couldn't remove the antiflood filter.", MUSLog.kDeb);
            }
        }
    }

    public ServerMovie serverMovie() {
        return m_movie;
    }

    public long creationTime() { // Why return a long? Data is int.
        return m_creationtime;
    }

    public String ipAddress() {
        return ((InetSocketAddress) channel.getRemoteAddress()).getAddress().getHostAddress();
    }

    public int ipAsInteger() {
        byte[] adr = ((InetSocketAddress) channel.getRemoteAddress()).getAddress().getAddress();
        return ConversionUtils.byteArrayToInt(adr, 0);
    }

    public Vector<String> getGroupNames() {

        Vector<String> groups = new Vector<String>();
        for (ServerGroup group : m_grouplist) {
            groups.addElement(((MUSGroup) group).m_name);
        }

        return groups;
    }

    public Vector<ServerGroup> getGroups() {
        return new Vector<ServerGroup>(m_grouplist);
    }

    public int getGroupsCount() {
        return m_grouplist.size();
    }

    public void groupJoined(ServerGroup grp) {
        m_grouplist.addElement(grp);
    }

    public void groupLeft(ServerGroup grp) {
        m_grouplist.removeElement(grp);
    }

    @Override
    public String toString() {
        return m_name + " (" + ipAddress() + ")";
    }
}