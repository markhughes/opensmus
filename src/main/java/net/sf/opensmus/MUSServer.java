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

import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.text.*;

import net.sf.opensmus.io.SMUSPipelineFactory;

import static net.sf.opensmus.ServerUserDatabase.*;

/////////////////////////////////////////////////////////////
public class MUSServer implements ServerObject {

    public final ConcurrentHashMap<String, MUSUser> m_clientlist = new ConcurrentHashMap<String, MUSUser>();
    public final ConcurrentHashMap<String, MUSMovie> m_movielist = new ConcurrentHashMap<String, MUSMovie>();

    protected MUSServerLoginQueue m_loginqueue;

    // @TODO: These can be sets
    protected final Vector<String> m_allowedmovieslist = new Vector<String>();
    protected final Vector<String> m_disabledmovieslist = new Vector<String>();
    protected final Vector<String> m_allowedmoviepathnames = new Vector<String>();
    public final Vector<MUSConnectionPort> m_ports = new Vector<MUSConnectionPort>();

    public final LinkedHashMap<Integer, Long> recentIPs = new LinkedHashMap<Integer, Long>();

    public MUSDBConnection m_dbConn;
    public MUSSQLConnection m_sqlConn;
    public MUSServerProperties m_props;
    public MUSScriptMap m_scriptmap;
    public long m_starttime;
    public static String m_vendorname = "OpenSMUS.sf.net";
    public static String m_version = "2.0";
    
    public volatile int idle = 600;
    
    public int m_maxconnections = 0;
    public int m_loginlimit = 0;
    protected boolean m_enabled = true;
    
    /**
     * Alive flag of server. Set to true on startup and false on shutdown.
     * Must be volatile, since other threads will read its state
     */
    public volatile boolean m_alive = true;
    
    public String encryptionKey;
    public int authentication; // Valid states defined in ServerUserDatabase

    private MUSIdleCheck m_bgtask;
    private MUSServerStatusLogger m_slogger;

    public int m_udpStartingPort = 1627;
    public String m_udpAddress = "default";
    DatagramChannelFactory UDPFactory;
    ConnectionlessBootstrap UDPBootstrap;
    protected Vector<Integer> m_udpPortsInUse = new Vector<Integer>();
    public ChannelGroup UDP_channels;

    public long in_bytes = 0;
    public long out_bytes = 0;
    public long in_msg = 0;
    public long out_msg = 0;
    public long drop_msg = 0;

    /////////////////////////////////////////////////////////////
    public MUSServer() {
        
    	this(null);
    }

    public MUSServer(final MUSServerProperties properties) {
    	
    	this.initServer(properties);
    }
    
    /////////////////////////////////////////////////////////////
    private void initServer(final MUSServerProperties properties) {

        m_props = properties != null ? properties : new MUSServerProperties();

        this.installServerLogging();

        int loglevel = MUSLog.kSys;
        if (m_props.getIntProperty("LogServerEvents") == 1)
            loglevel = loglevel | MUSLog.kSrv;
        if (m_props.getIntProperty("LogMovieEvents") == 1)
            loglevel = loglevel | MUSLog.kMov;
        if (m_props.getIntProperty("LogGroupEvents") == 1)
            loglevel = loglevel | MUSLog.kGrp;
        if (m_props.getIntProperty("LogUserEvents") == 1)
            loglevel = loglevel | MUSLog.kUsr;
        if (m_props.getIntProperty("LogDBEvents") == 1)
            loglevel = loglevel | MUSLog.kDB;
        if (m_props.getIntProperty("LogInvalidMsgEvents") == 1)
            loglevel = loglevel | MUSLog.kMsgErr;
        if (m_props.getIntProperty("LogScriptEvents") == 1)
            loglevel = loglevel | MUSLog.kScr;
        if (m_props.getIntProperty("LogDebugInformation") == 1)
            loglevel = loglevel | MUSLog.kDeb;
        if (m_props.getIntProperty("LogDebugExtInformation") == 1)
            loglevel = loglevel | MUSLog.kDebWarn;

        MUSLog.setLogLevel(loglevel);

        MUSLog.Log("OpenSMUS Started", MUSLog.kSys);
        MUSLog.Log("Version " + m_version, MUSLog.kSys);
        MUSLog.Log(timeString(), MUSLog.kSys);

        // Print number of connections allowed
        m_maxconnections = m_props.getIntProperty("ConnectionLimit");
        MUSLog.Log(m_maxconnections + " connections allowed", MUSLog.kSys);

        if (m_props.getIntProperty("EnableServerSideScripting") == 1)
            m_scriptmap = new MUSScriptMap(true);
        else
            m_scriptmap = new MUSScriptMap(false);

        idle = m_props.getIntProperty("IdleTimeOut");

        String auth = m_props.getProperty("Authentication");
        if (auth.equalsIgnoreCase("UserRecordRequired")) {
            authentication = AUTHENTICATION_REQUIRED;
        } else if (auth.equalsIgnoreCase("UserRecordOptional")) {
            authentication = AUTHENTICATION_OPTIONAL;
        } else {
            authentication = AUTHENTICATION_NONE;
        }

        encryptionKey = m_props.getProperty("EncryptionKey");
        MUSBlowfishCypher.initGlobalBoxes(encryptionKey);

        boolean dbenabled = false;
        if (m_props.getIntProperty("EnableDatabaseCommands") == 1)
            dbenabled = true;

        m_dbConn = new MUSDBConnection(this, dbenabled);

        boolean sqlenabled = false;
        if (m_props.getIntProperty("EnableSQLDatabase") == 1)
            sqlenabled = true;

        m_sqlConn = new MUSSQLConnection(this, sqlenabled);

        this.installLoginQueueing();
        this.installIdleChecker();

        if (this.m_props.getIntProperty("ServerStatusReportInterval") != 0) {
        	this.installStatusLogger();
        }

        m_loginlimit = m_props.getIntProperty("MinLoginPeriod") * 1000;

        m_starttime = System.currentTimeMillis();

        initConnectionPorts();

        if (m_props.getIntProperty("EnableUDP") == 1) {
            // Netty Init UDP
            // http://www.adobe.com/support/director/multiuser/using_udp/using_udp02.html

            UDPFactory = new NioDatagramChannelFactory(Executors.newCachedThreadPool());
            UDPBootstrap = new ConnectionlessBootstrap(UDPFactory);
            UDP_channels = new DefaultChannelGroup("UDP");
            UDPBootstrap.setPipelineFactory(new SMUSPipelineFactory(this, UDP_channels, true));
            UDPBootstrap.setOption("broadcast", "false");
            UDPBootstrap.setOption("receiveBufferSize", m_props.getIntProperty("MaxUDPPacket"));
            UDPBootstrap.setOption("sendBufferSize", m_props.getIntProperty("MaxUDPPacket"));
            UDPBootstrap.setOption("reuseAddress", true);

            // Get UDP address and starting port number
            String udpaddress = m_props.getProperty("UDPServerAddress");
            if (!udpaddress.equalsIgnoreCase("default")) {
                m_udpAddress = MUSServerProperties.parseIPAddress(udpaddress);
                m_udpStartingPort = MUSServerProperties.parseIPPort(udpaddress);
            }
        }

        // Get list of allowed movies
        String[] allowedmovies = m_props.getStringListProperty("AllowMovies");
        for (String am : allowedmovies) {
            if (!am.equalsIgnoreCase("default")) {
                m_allowedmovieslist.addElement(am);
            }
        }

        // Get list of optional movie pathnames
        String[] allowedmovienames = m_props.getStringListProperty("MoviePathName");
        for (String movname : allowedmovienames) {
            if (!movname.equalsIgnoreCase("default")) {
                m_allowedmoviepathnames.addElement(movname);
            }
        }

        // Finally, load a movie if specified in the config file (new in 1.3)
        String[] defaultmovies = m_props.getStringListProperty("StartupMovies");
        for (String movname : defaultmovies) {
            if (!movname.equalsIgnoreCase("none")) {
                MUSMovie mov = new MUSMovie(this, movname); // Should check if the moviename exists first for full safety. Would only happen if the same name is configured twice in settings...
                mov.setpersists(true);
            }
        }
    }


    /////////////////////////////////////////////////////////////
    public void killServer() {

        m_sqlConn.killDBConnection();
        m_dbConn.killDBConnection();

        m_loginqueue.kill();

        freeConnectionPorts();

        disconnectAllUsers();

        m_alive = false;

        MUSLog.Log("Server Stopped", MUSLog.kSys);
        
        this.deinstallServerLogging();
    }

    public void disconnectAllUsers() {

        for (MUSUser mu : m_clientlist.values()) {
            mu.deleteUser();
        }
    }

    public void freeConnectionPorts() {

        if (m_props.getIntProperty("EnableUDP") == 1) {
            UDP_channels.close().awaitUninterruptibly();
            UDPFactory.releaseExternalResources();
        }

        // Make a copy of the ports list to avoid ConcurrentModificationException
        List<MUSConnectionPort> connectionPorts = new ArrayList<MUSConnectionPort>(this.m_ports);
        for (MUSConnectionPort mcp : connectionPorts) {
            mcp.killConnectionPort();
        }
    }

    public void initConnectionPorts() {

        String[] ipaddresses = m_props.getStringListProperty("ServerIPAddress");
        for (String ipaddress : ipaddresses) {
            if (!ipaddress.equalsIgnoreCase("default")) {
                String ipnumber = MUSServerProperties.parseIPAddress(ipaddress);
                int ipport = MUSServerProperties.parseIPPort(ipaddress);
                this.addConnectionPort(ipnumber, ipport);
            }
        }

        // If no serveripaddresses have been initialized try the ServerPort
        if (m_ports.isEmpty()) {
            int[] ports = m_props.getIntListProperty("ServerPort");
            for (int port : ports) {
            	this.addConnectionPort("default", port);
            }
        }
    }
    
    private void addConnectionPort(final String connectionAddress, final int portNumber) {
    	@SuppressWarnings("unused")
		MUSConnectionPort thisport = new MUSConnectionPort(this, connectionAddress, portNumber);
    }

    public void disable() {
        m_enabled = false;
    }

    public void enable() {
        m_enabled = true;
    }

    public void checkDatabaseConnections() {
    	
        if (this.m_dbConn != null && this.m_dbConn.m_enabled) {
            this.m_dbConn.checkPoint();
        }

        if (this.m_sqlConn != null && this.m_sqlConn.m_enabled) {
            this.m_sqlConn.checkPoint();
        }
    }

    public void ensureLoggerThreadIsAlive() {
        // MUSLog.Log("Checking health of status logger thread", MUSLog.kDeb);
        // Status logger is initialized after the bgtask idle check, avoid null
        if (m_slogger != null) {
            if (m_slogger.isInterrupted() || !m_slogger.isAlive()) {
                MUSLog.Log("Server status logger thread restarted", MUSLog.kDeb);
                
                this.installStatusLogger();
            }
        }
    }

    public void ensureThreadsAreAlive() {
        try {
            MUSLog.Log("Checking health of server threads", MUSLog.kDeb);

            if (m_bgtask.isInterrupted() || !m_bgtask.isAlive()) {
                MUSLog.Log("Idle check thread restarted", MUSLog.kDeb);
                this.installIdleChecker();
            }

            // Status logger is initialized after the bgtask idle check, avoid null
            if (m_slogger != null) {
                if (m_slogger.isInterrupted() || !m_slogger.isAlive()) {
                    MUSLog.Log("Server status logger thread restarted", MUSLog.kDeb);
                    this.installStatusLogger();
                }
            }

            if (m_loginqueue.isInterrupted() || !m_loginqueue.isAlive()) {
                MUSLog.Log("Login message queue thread restarted", MUSLog.kDeb);
                this.installLoginQueueing();
            }

        } catch (NullPointerException e) {
            MUSLog.Log("Null pointer when checking threads status", MUSLog.kDeb);
            MUSLog.Log(e, MUSLog.kDeb);
        }
    }

    /////////////////////////////////////////////////////////////
    // public synchronized void removeMUSUser(MUSUser oneClient) {
    public void removeMUSUser(MUSUser oneClient) {
        m_clientlist.remove(oneClient.m_name.toUpperCase());
    }

    public void addMUSUser(MUSUser oneClient) {
        m_clientlist.putIfAbsent(oneClient.m_name.toUpperCase(), oneClient);
    }

    public boolean userThreadAlive(String uname) {
        return m_clientlist.get(uname.toUpperCase()) != null;
    }

    public void checkStructure() {

        MUSLog.Log("Checking server structure...", MUSLog.kDeb);

        for (MUSMovie mv : m_movielist.values()) {
            mv.checkStructure();
        }
    }

    public void logInBytes(int bytes) {
        in_bytes += bytes;
        in_msg++;
    }

    public void logOutBytes(int bytes) {
        out_bytes += bytes;
        out_msg++;
    }

    public void logDroppedMsg() {
        drop_msg++;
    }

    public void addConnectionPort(MUSConnectionPort onePort) {
        m_ports.addElement(onePort);
    }

    public void removeConnectionPort(MUSConnectionPort onePort) {
        m_ports.removeElement(onePort);
    }

    public int getUDPPortNumber() {
        boolean portFound = false;
        int freePort = m_udpStartingPort - 1;

        while (!portFound) {
            freePort++;
            Integer tryPort = freePort;
            if (!m_udpPortsInUse.contains(tryPort)) {
                freePort = tryPort;
                portFound = true;
            }
        }

        return freePort;
    }

    public void addUDPPort(int portNum) {
        m_udpPortsInUse.addElement(portNum);
    }

    public void releaseUDPPort(int portNum) {

        m_udpPortsInUse.removeElement(portNum);
    }

    protected boolean isMovieAllowed(String moviename) {
        // First check the movies that have been disabled using Lingo

        for (String mn : m_disabledmovieslist) {
            if (moviename.equalsIgnoreCase(mn)) {
                return false;
            }
        }

        // Now check the allowMovies list
        // If there are no movies in list all are allowed
        if (m_allowedmovieslist.isEmpty())
            return true;

        for (String mn : m_allowedmovieslist) {
            if (moviename.equalsIgnoreCase(mn)) {
                return true;
            }
        }

        return false;
    }

    protected boolean isMoviePathnameAllowed(String pathname, int logonmsgformat) {
        // If there are no movies in list all are allowed
        if (m_allowedmoviepathnames.isEmpty())
            return true;

        // If there is a list of allowed moviepathnames we NEED to have the path in the logon message
        // The oldest format does not include this information
        if (logonmsgformat < 1)
            return false;

        for (String mn : m_allowedmoviepathnames) {
            if (pathname.equalsIgnoreCase(mn)) {   // @TODO: Allow wildcards for partial paths
                return true;
            }
        }

        return false;
    }

    public MUSMovie getMovie(String mname) throws MovieNotFoundException {

        String gkey = mname.toUpperCase();
        MUSMovie mv = m_movielist.get(gkey);
        if (mv == null) {
            throw new MovieNotFoundException("Movie not found");
        } else {
            return mv;
        }
    }

    public void disableMovie(String mname) {

        boolean inList = false;
        for (String mn : m_disabledmovieslist) {
            if (mname.equalsIgnoreCase(mn)) {
                inList = true;
                break;
            }
        }

        if (!inList)
            m_disabledmovieslist.addElement(mname);

        // Mark the movie instance, if it exists
        try {
            MUSMovie mv = getMovie(mname);
            mv.m_enabled = false;
        } catch (MovieNotFoundException mnf) {
            // That's OK
        }
    }

    public void enableMovie(String mname) {

        for (String mn : m_disabledmovieslist) {
            if (mname.equalsIgnoreCase(mn)) {
                m_disabledmovieslist.removeElement(mn);
            }
        }

        // This is delicate: if the allowmovies list is NOT empty
        // we need to add the movie to the list, since it is used as
        // the control method for movie authorization
        if (m_allowedmovieslist.size() > 0)
            m_allowedmovieslist.addElement(mname);

        // Mark the movie instance, if it exists
        try {
            MUSMovie mv = getMovie(mname);
            mv.m_enabled = true;
        } catch (MovieNotFoundException mnf) {
            // That's OK
        }
    }

    public synchronized void changeUserMovie(ServerUser wuser, String wnewmovie) throws MUSErrorCode {

        if (!isMovieAllowed(wnewmovie)) {
            throw new MUSErrorCode(MUSErrorCode.InvalidMovieID);
        }

        // Cast to MUSUser
        MUSUser oneUser;
        try {
            oneUser = (MUSUser) wuser;
        } catch (ClassCastException e) {
            throw new MUSErrorCode(MUSErrorCode.ConnectionRefused);
        }
        MUSMovie newmov;

        try {
            newmov = getMovie(wnewmovie);
        } catch (MovieNotFoundException mnf) {
            newmov = new MUSMovie(this, wnewmovie);
        }

        try {
            @SuppressWarnings("unused")
			ServerUser test = newmov.getUser(oneUser.name());
            // If we exist then we can not login, schedule death
            throw new MUSErrorCode(MUSErrorCode.InvalidUserID);

        } catch (UserNotFoundException unf) {
            // This is expected, we don't exist, all ok so far
        }

        // Check if new connections to the movie are allowed
        if (!newmov.IsConnectionAllowed(oneUser)) {
            throw new MUSErrorCode(MUSErrorCode.ConnectionRefused);
        }

        // Passed all requirements, disconnect from old movie
        oneUser.disconnectFromMovie();

        // Now add the user to the movie
        oneUser.addToMovie(newmov);
    }

    public void queueLogonMessage(MUSMessage msg, MUSUser oneUser) {
    	
        if (m_loginqueue.isInterrupted() || !m_loginqueue.isAlive()) {
            MUSLog.Log("Login message queue thread restarted", MUSLog.kDeb);
            this.installLoginQueueing();
        }

        if (!m_loginqueue.queue(new MUSQueuedMessage(oneUser, msg))) {
            MUSLog.Log("Login refused: login message queue is full", MUSLog.kDeb);
            oneUser.replyLogonError((MUSLogonMessage) msg, MUSErrorCode.ConnectionRefused);
            oneUser.deleteUser();

            /*lets flush it*/
            //m_loginqueue.kill();
            m_loginqueue.interrupt();
        }
    }

    protected void processLogonMessage(MUSMessage msg, MUSUser oneUser) {
        // We need to handle this in a centralized place, to avoid synchronization issues
        // This is called from each MUSUser thread when logging in

        // MUSLog.Log("Warning: Entering logon procedure", MUSLog.kDebWarn);
        // if (m_props.getIntProperty("dumpLoginMessage") == 1) msg.dump();

        if (msg.m_subject.toString().equalsIgnoreCase("Logon")) {  // @TODO: Do we need IgnoreCase here?
            MUSLogonMessage logmsg = (MUSLogonMessage) msg;
            // MUSLog.Log("Warning: Valid logon subject", MUSLog.kDebWarn);

            if (!logmsg.extractLoginInfo()) {
                // Error in the login package, schedule death
                MUSLog.Log("Warning: could not extract login info", MUSLog.kDebWarn);

                oneUser.replyLogonError((MUSLogonMessage) msg, MUSErrorCode.InvalidMessageFormat);
                oneUser.deleteUser();
                return;
            }

            // Prevent login floods
            if (m_loginlimit > 0) {
                // Remove expired entries
                long oldTime = System.currentTimeMillis() - m_loginlimit;
                Iterator<Long> it = recentIPs.values().iterator();
                // TODO: Optimize this. (would be nice if we could iterate backwards)
                while (it.hasNext())
                    if (it.next() < oldTime) it.remove();

                if (recentIPs.put(oneUser.ip, System.currentTimeMillis()) != null) {
                    MUSLog.Log("Login error: flooding - " + oneUser, MUSLog.kDeb);
                    oneUser.replyLogonError((MUSLogonMessage) msg, MUSErrorCode.ConnectionRefused);
                    oneUser.deleteUser();
                    return;
                }
            }
            oneUser.m_pathname = logmsg.m_pathname; // Store in the user object

            // Move to end, only if authorized
            // oneUser.m_name =  logmsg.m_userID;

            if (m_clientlist.size() >= m_maxconnections) {
                // We can not login to the server, schedule death
                MUSLog.Log("Login error: maximum number of connections reached", MUSLog.kDeb);

                oneUser.replyLogonError((MUSLogonMessage) msg, MUSErrorCode.NoConnectionsAvailable);
                oneUser.deleteUser();
                return;
            }

            // Check for empty or invalid userid
            if (logmsg.m_userID.equals("") || logmsg.m_userID.equals("System")) {
                MUSLog.Log("Login error: invalid user id", MUSLog.kDeb);
                oneUser.replyLogonError((MUSLogonMessage) msg, MUSErrorCode.InvalidUserID);
                oneUser.deleteUser();
                return;
            }

            /*// Simulating failure
               if (logmsg.m_userID.equals("Bomb")) throw new NullPointerException();*/

            // Check for illegal characters in arguments
            StringCharacterIterator sci = new StringCharacterIterator(logmsg.m_userID);
            for (char c = sci.first(); c != CharacterIterator.DONE; c = sci.next()) {
                if ((c == '@') || (c == '#')) {
                    MUSLog.Log("Login error: invalid user id", MUSLog.kDeb);
                    oneUser.replyLogonError((MUSLogonMessage) msg, MUSErrorCode.InvalidUserID);
                    oneUser.deleteUser();
                    return;
                }
            }

            if (!isMoviePathnameAllowed(logmsg.m_pathname, logmsg.m_logonPacketFormat)) {
                // We can not login to this movie, schedule death
                MUSLog.Log("Login error: movie pathname not allowed: " + logmsg.m_pathname, MUSLog.kDeb);

                oneUser.replyLogonError((MUSLogonMessage) msg, MUSErrorCode.InvalidMovieID);
                oneUser.deleteUser();
                return;
            }

            if (!isMovieAllowed(logmsg.m_moviename)) {
                // We can not login to this movie, schedule death
                MUSLog.Log("Login error: movie not allowed", MUSLog.kDeb);

                oneUser.replyLogonError((MUSLogonMessage) msg, MUSErrorCode.InvalidMovieID);
                oneUser.deleteUser();
                return;
            }

            // Assign the user to a movie. If it doesn't exist, create it.
            try {
                oneUser.m_movie = getMovie(logmsg.m_moviename);
            } catch (MovieNotFoundException mnf) {
                oneUser.m_movie = new MUSMovie(this, logmsg.m_moviename);
            }

            /*moved to after authentication
                        try{
                        ServerUser test = oneUser.m_movie.getUser(logmsg.m_userID);
                        //if we exist then we can not login, schedule death
                        MUSLog.Log("Login error: username already in the movie", MUSLog.kDeb);

                        //new login for existing ipAddress with same username, kill both for safety
                        if (oneUser.ipAddress().equals(test.ipAddress()))
                            {
                            MUSLog.Log("User connection from same IP, connections will be closed", MUSLog.kDeb);
                            test.deleteUser();
                            }

                        oneUser.replyLogonError((MUSLogonMessage) msg, MUSErrorCode.ConnectionRefused);
                        oneUser.deleteUser();

                        return;
                        } catch (UserNotFoundException unf){
                        //this is expected, we don't exist, all ok so far
                        }
            */

            // Check if new connections to the movie are allowed
            if (!oneUser.m_movie.IsConnectionAllowed(oneUser)) {
                MUSLog.Log("Login error: new connections to the movie not allowed", MUSLog.kDeb);

                oneUser.replyLogonError((MUSLogonMessage) msg, MUSErrorCode.ConnectionRefused);
                oneUser.deleteUser();
                return;
            }

            // Check db conn to be used for authentication
            ServerUserDatabase authdb;

            if (m_props.getIntProperty("UseSQLDatabaseForAuthentication") == 1)
                authdb = m_sqlConn;
            else
                authdb = m_dbConn;

            // Check username and password in the db
            if (authdb.isEnabled()) {
                int errorCode = 0;
                if (authentication == AUTHENTICATION_NONE) { // m_props.getProperty("Authentication").equalsIgnoreCase("None")
                    // Anyone can login
                    oneUser.setuserLevel(oneUser.m_movie.m_props.getIntProperty("DefaultUserLevel"));
                } else {
                    // Authorization checks in place
                    errorCode = authdb.checkLogin(oneUser, logmsg.m_userID, logmsg.m_password);
                }

                if (errorCode != 0) {
                    MUSLog.Log("Login error: user authentication process failed for " + logmsg.m_userID + " (" + oneUser.ipAddress() + ")", MUSLog.kDeb);
                    oneUser.replyLogonError((MUSLogonMessage) msg, errorCode);
                    oneUser.deleteUser();
                    return;
                }
            } else // Authentication db not enabled, set userlevel
            {
                oneUser.setuserLevel(oneUser.m_movie.m_props.getIntProperty("DefaultUserLevel"));
            }

            if (m_dbConn.m_enabled) {
                // Check if there is a banning for the user name and ipaddress
                if (m_dbConn.isBanned(logmsg.m_userID) || m_dbConn.isBanned(oneUser.ipAddress())) {
                    MUSLog.Log("Login error: username or ip is banned", MUSLog.kDeb);
                    oneUser.replyLogonError((MUSLogonMessage) msg, MUSErrorCode.ConnectionRefused);
                    oneUser.deleteUser();
                    return;
                }
            }

            // Check if the server is disabled for common users
            if (!m_enabled) {
                if (oneUser.m_userlevel < 100) {
                    MUSLog.Log("Login error: server restricted to admin users", MUSLog.kDeb);

                    oneUser.replyLogonError((MUSLogonMessage) msg, MUSErrorCode.NoConnectionsAvailable);
                    oneUser.deleteUser();
                }
                return;
            }

            // Check for duplicate userids
            try {
                ServerUser test = oneUser.m_movie.getUser(logmsg.m_userID);
                // If we exist then we can not login, schedule death
                MUSLog.Log("Login error: username already in the movie: " + logmsg.m_userID, MUSLog.kDeb);

                if (oneUser.ipAddress().equals(test.ipAddress())) {
                    // New login for existing ipAddress with same username
                    if (m_props.getIntProperty("DropUserWhenReconnectingFromSameIP") == 1) {
                        MUSLog.Log("User reconnecting from same IP, old connection closed.", MUSLog.kDeb);
                        ((MUSUser) test).killMUSUser(); // Remove previous user immediately
                    } else {
                        MUSLog.Log("User reconnecting from same IP, existing connection kept.", MUSLog.kDeb);
                        oneUser.replyLogonError((MUSLogonMessage) msg, MUSErrorCode.ConnectionRefused);
                        oneUser.deleteUser();
                        return;
                    }
                } else { // Not same ip
                    MUSLog.Log("User connecting from different ip. Old: " + test.ipAddress() + " New: " + oneUser.ipAddress(), MUSLog.kDeb);
                    oneUser.replyLogonError((MUSLogonMessage) msg, MUSErrorCode.ConnectionRefused);
                    oneUser.deleteUser();
                    return;
                }
            } catch (UserNotFoundException unf) {
                // This is expected, we don't exist, all OK so far
            }

            // UFFF... passed all requirements, add the user to the movie
            oneUser.m_name = logmsg.m_userID;
            oneUser.addToMovie(oneUser.m_movie);

            // At this point add it to our client list
            addMUSUser(oneUser);

            // Check if we have UDP information ready
            oneUser.setUDPEnabled(logmsg);

            oneUser.logged = true;

            // Netty
            // Remove the handler used for processing the logon and replace it with a handler for normal messages.
            ChannelPipeline pl = oneUser.channel.getPipeline();
            pl.remove("logonhandler");
            pl.addLast("handler", SMUSPipelineFactory.HANDLER);

            oneUser.replyLogon(logmsg);

            // Moved from authentication phase above
            // Password already checked in the db
            if (authdb.isEnabled()) {
                String authmode = m_props.getProperty("Authentication");
                if (!authmode.equalsIgnoreCase("None")) {
                    try {
                        int userid = authdb.getDBUser(logmsg.m_userID.toUpperCase());
                        authdb.updateUserLastLoginTime(userid);
                    } catch (UserNotFoundException dbe) {
                    } catch (DBException dbe) {
                    }
                }
            }

        } else // Wrong subject in message
        {
            // Not logged and wrong Logon Message
            MUSLog.Log("Login error: wrong logon message from " + oneUser + ": " + msg.m_subject, MUSLog.kDeb);
            // throw new NullPointerException();
            oneUser.deleteUser();
        }
    }

    public void addMovie(MUSMovie onemovie) {

        String gkey = onemovie.m_name.toUpperCase();
        m_movielist.putIfAbsent(gkey, onemovie);
    }

    public void removeMovie(MUSMovie onemovie) {

        // Inform server side scripts attached to this movie that everything is going away
        for (ServerSideScript script : onemovie.m_scriptList) {
            script.scriptDelete();
        }

        String gkey = onemovie.m_name.toUpperCase();
        m_movielist.remove(gkey);

        MUSLog.Log("Movie removed:" + onemovie.name(), MUSLog.kMov);
    }

    public LValue srvcmd_getVersion() {

        LPropList pl = new LPropList();
        pl.addElement(new LSymbol("vendor"), new LString(m_vendorname));
        pl.addElement(new LSymbol("version"), new LString(m_version));
        String sysname = System.getProperty("os.name");
        pl.addElement(new LSymbol("platform"), new LString(sysname));
        return pl;
    }

    public LValue srvcmd_getUserCount() {
        return new LInteger(m_clientlist.size());
    }

    public LValue srvcmd_getMovies() {

        LList ml = new LList();

        for (MUSMovie mv : m_movielist.values()) {
            ml.addElement(new LString(mv.m_name));
        }
        return ml;
    }


    //ServerObject interface

    public SQLGateway getSQLGateway() {
        return m_sqlConn;
    }

    public ServerUserDatabase getServerUserDatabase() {

        // Check db conn to be used for authentication
        ServerUserDatabase authdb;

        if (m_props.getIntProperty("UseSQLDatabaseForAuthentication") == 1)
            authdb = m_sqlConn;
        else
            authdb = m_dbConn;

        return authdb;
    }

    // Used only by scripts, to store info in the logfile
    public void put(String msg) {
        MUSLog.Log(msg, MUSLog.kScr);
    }

    public ServerMovie getServerMovie(String moviename) throws MovieNotFoundException {
        return getMovie(moviename);
    }

    /**
     * TODO Movies are stored in a map without guaranteed sequence order
     * 
     * @see net.sf.opensmus.ServerObject#getServerMovie(int)
     * @deprecated since it's not working correctly
     */
    public ServerMovie getServerMovie(int movieidx) throws MovieNotFoundException {

        try {
            Enumeration<MUSMovie> enume = m_movielist.elements();
            int enumidx = 1;
            while (enume.hasMoreElements()) {
                MUSMovie mu = enume.nextElement();
                if (movieidx == enumidx)
                    return mu;

                enumidx++;
            }
            // Throw group not found otherwise
            throw new MovieNotFoundException("Movie not found");
        } catch (Exception e) {
            throw new MovieNotFoundException("Movie not found");
        }
    }

    public ServerMovie createServerMovie(String moviename) throws MUSErrorCode {

        if (!isMovieAllowed(moviename)) {
            throw new MUSErrorCode(MUSErrorCode.InvalidMovieID);
        }

        try {
            return getMovie(moviename);
        } catch (MovieNotFoundException mnf) {
            MUSMovie mov = new MUSMovie(this, moviename);
            mov.setpersists(true);
            return mov;
        }
    }

    public void deleteServerMovie(String moviename) {
        try {
            MUSMovie mov = getMovie(moviename);
            removeMovie(mov);
        } catch (MovieNotFoundException mnf) {
        }
    }

    public int serverMovieCount() {
        return m_movielist.size();
    }

    public String path() {
        return System.getProperty("user.dir");
    }

    public String timeString() {
        java.text.SimpleDateFormat formatter
                = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        java.util.Date ct = new java.util.Date();
        return formatter.format(ct);
    }

    public int timeStamp() {
        return (int) (System.currentTimeMillis() - m_starttime) % Integer.MAX_VALUE;
    }

    public int language() {
        // 0 is English, the only one supported so far
        return 0;
    }

    public int userLevel() {
        return m_props.getIntProperty("DefaultUserLevel");
    }

    public void setuserLevel(int level) {
        m_props.m_props.put("DefaultUserLevel", Integer.toString(level));
    }

    /**
     * Installs the logger that reports the server status with a predefined interval.
     * Interval is configured in property 'ServerStatusReportInterval'
     */
    private void installStatusLogger() {
    	
    	MUSServerStatusLogger logger = new MUSServerStatusLogger(
    			this, this.m_props.getIntProperty("ServerStatusReportInterval"));
    	logger.start();
    	
    	this.m_slogger = logger;
    }
    
    private void installIdleChecker() {
    	
    	MUSIdleCheck checkThread = new MUSIdleCheck(this);
    	checkThread.start();
    	
    	this.m_bgtask = checkThread;
    }
    
    private void installLoginQueueing() {
    	
         MUSServerLoginQueue loginQueueProcessor = new MUSServerLoginQueue(
        		 this, m_props.getIntProperty("MaxLoginMsgQueue"), m_props.getIntProperty("MaxMsgQueueWait"));
         loginQueueProcessor.start();
         
         this.m_loginqueue = loginQueueProcessor;
    }
    
    /**
     * Installs the server logging by redirecting the standard system out to the 
     * server log file
     */
    private void installServerLogging() {
    	
        if (this.doesServerLogging()) {

        	try {
                boolean appendToLog = true;
                if (m_props.getIntProperty("ClearLogAtStartup") == 1)
                    appendToLog = false;

                PrintStream stdout = new PrintStream(
                		new BufferedOutputStream(new FileOutputStream(m_props.getProperty("LogFileName"), appendToLog), 128), true);
                System.setOut(stdout);
            } 
        	catch (IOException e) {
                MUSLog.Log("Error creating log file", MUSLog.kSys);
            }
        }
    }
    
    /**
     * Deinstalls the server logging by setting standard output back to console 
     * and closing the server log file
     */
    private void deinstallServerLogging() {
    	
    	if (this.doesServerLogging()) {
    		
    		PrintStream serverLoggingStream = System.out;
    		if (serverLoggingStream != null)
    			serverLoggingStream.close();
    		
            PrintStream stdout = new PrintStream(
            		new BufferedOutputStream(new FileOutputStream(FileDescriptor.out), 128), true);
            System.setOut(stdout);
    	}
    }
    
    /**
     * @return
     */
    public final boolean doesServerLogging() {
    	
    	return this.m_props.getIntProperty("ServerOutputToLogFile") == 1;
    }
} 