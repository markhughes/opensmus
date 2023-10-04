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

import java.sql.*;
import java.util.*;
import java.text.StringCharacterIterator;
import java.text.CharacterIterator;


public class MUSSQLConnection implements ServerUserDatabase, SQLGateway {
    private Connection m_conn;
    private MUSServer m_server;
    public boolean m_enabled;

    String m_driver;
    String m_url;
    String m_userid;
    String m_password;

    public MUSSQLConnection(MUSServer srv, boolean enableFlag) {
        m_server = srv;

        // Exceptions may occur
        try {

            if (!enableFlag) {
                throw new DBException("Database not enabled");
            }
            // Load the SQL database JDBC driver
            m_driver = m_server.m_props.getProperty("SQLDatabaseDriver");
            m_url = m_server.m_props.getProperty("SQLDatabaseURL");
            m_userid = m_server.m_props.getProperty("SQLDatabaseUsername");
            m_password = m_server.m_props.getProperty("SQLDatabasePassword");

            Class.forName(m_driver).newInstance();

            // Connect to the database
            m_conn = DriverManager.getConnection(m_url, m_userid, m_password);

            ensureDBPresence();
            MUSLog.Log("SQL database connection established", MUSLog.kSys);
            m_enabled = true;

            processDBConfigCommands();

        } catch (DBException e) {
            // Database not enabled exception thrown, config option disabled
            MUSLog.Log("SQL database functions disabled", MUSLog.kDB);
            m_enabled = false;
        } catch (Exception e) {
            // Print out the error message
            MUSLog.Log("SQL DB initialization error> ", MUSLog.kDB);
            MUSLog.Log(e, MUSLog.kDB);
            m_enabled = false;
        }
    }

    public boolean isEnabled() {
        return m_enabled;
    }

    public boolean connect(String sqldriver, String sqlurl, String sqluser, String sqlpass) {
        // Exceptions may occur
        try {

            if (!m_conn.isClosed())
                throw new DBException("Already connected to a SQL database");

            m_driver = sqldriver;
            m_url = sqlurl;
            m_userid = sqluser;
            m_password = sqlpass;

            // Load JDBC driver
            Class.forName(m_driver).newInstance();

            // Connect to the database
            m_conn = DriverManager.getConnection(m_url, m_userid, m_password);

            MUSLog.Log("SQL connection established", MUSLog.kSys);
            m_enabled = true;
            return m_enabled;

        } catch (DBException e) {
            // Database not enabled exception thrown, config option disabled
            MUSLog.Log("SQL Database reconnection failed", MUSLog.kDB);
            MUSLog.Log(e, MUSLog.kDB);
            return false;
        } catch (Exception e) {
            // Print out the error message
            MUSLog.Log("SQL DB initialization error> ", MUSLog.kDB);
            MUSLog.Log(e, MUSLog.kDB);
            m_enabled = false;
            return false;
        }
    }

    public boolean isConnected() {
        // Exceptions may occur
        try {
            return m_conn.isClosed();
        } catch (Exception e) {
            // Print out the error message
            MUSLog.Log(e, MUSLog.kDB);
            return false;
        }
    }

    public void checkPoint() {
        try {
            // checkPoint is only called when the db is enabled
            // make sure connection has not gone bad
            if (m_conn.isClosed())
                connect(m_driver, m_url, m_userid, m_password);

            String dbbackend = m_server.m_props.getProperty("SQLBackend");
            if (dbbackend.equalsIgnoreCase("hsqldb")) {
                Statement stat = m_conn.createStatement();
                stat.executeQuery("CHECKPOINT");
                stat.close();
            }

        } catch (SQLException sqle) {
            MUSLog.Log(sqle, MUSLog.kDB);
        }
    }

    public void disconnect() {
        killDBConnection();
    }

    public java.sql.Connection getConnection() {
        return m_conn;
    }

    public void killDBConnection() {
        try {
            if (m_enabled) {
                try {
                    String dbbackend = m_server.m_props.getProperty("SQLBackend");
                    if (dbbackend.equalsIgnoreCase("hsqldb")) {
                        Statement stat = m_conn.createStatement();
                        stat.executeQuery("SHUTDOWN COMPACT");
                        stat.close();
                    }

                } catch (SQLException sqle) {
                    MUSLog.Log(sqle, MUSLog.kDB);
                }
                m_conn.close();
            }

            m_enabled = false;
        } catch (SQLException sqle) {
            MUSLog.Log(sqle, MUSLog.kDB);
            m_enabled = false;
        }
    }

    public void processDBConfigCommands() {
        String[] nusers = m_server.m_props.getStringListProperty("CreateSQLUser");
        for (String nuser : nusers) {
            if (!nuser.equalsIgnoreCase("default")) {
                StringTokenizer st = new StringTokenizer(nuser, ",");
                if (st.countTokens() == 3) {
                    String username = st.nextToken();
                    String password = st.nextToken();
                    String userlevel = st.nextToken();
                    createUser(username, password, userlevel);
                }
            }
        }

    }

    public void ensureDBPresence() throws SQLException {

        try {
            // Create a statement object
            Statement stat = m_conn.createStatement();
            // Try a query to verify if the default tables are there
            ResultSet result = stat.executeQuery("SELECT * FROM USERS WHERE ID=1");
            result.close();
            stat.close();
        } catch (SQLException sqle) {
            // db not initialized, init it
            if (m_server.m_props.getIntProperty("CreateSQLUserTable") == 1)
                createDefaultDB();
        }
    }

    public void createDefaultDB() throws SQLException {

        String dbbackend = m_server.m_props.getProperty("SQLBackend");
        String longchartype, binarytype, tabletype;
        if (dbbackend.equalsIgnoreCase("mysql")) {
            tabletype = "CREATE TABLE ";
            longchartype = "text";
            binarytype = "blob";
        } else if (dbbackend.equalsIgnoreCase("postgresql")) {
            tabletype = "CREATE TABLE ";
            longchartype = "text";
            binarytype = "text";
        } else if (dbbackend.equalsIgnoreCase("ODBCaccess")) {
            tabletype = "CREATE TABLE ";
            longchartype = "TEXT";
            binarytype = "BINARY";
        } else if (dbbackend.equalsIgnoreCase("hsqldb")) {
            tabletype = "CREATE CACHED TABLE ";
            longchartype = "VARCHAR";
            binarytype = "LONGVARBINARY";
        } else
        // Default
        {
            tabletype = "CREATE TABLE ";
            longchartype = "VARCHAR";
            binarytype = "LONGVARBINARY";
        }

        Statement stat = m_conn.createStatement();

        stat.executeUpdate(tabletype + "USERS(ID INTEGER,NAME " + longchartype + ",PASSWORD " + longchartype + ",USERLEVEL INTEGER, LASTLOGIN " + longchartype + ")");
        stat.executeUpdate("CREATE UNIQUE INDEX IDXUSERS ON USERS(ID)");
        stat.close();
    }

    public synchronized boolean createUser(String usernamein, String password, String userlevel) {
        try {

            String username = usernamein.toUpperCase();
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT * FROM USERS WHERE NAME='" + username + "'");

            if (result.next()) {
                MUSLog.Log("User " + username + " already in database", MUSLog.kDB);
                return false;
            }

            // Go on, get next global id for this user
            ResultSet idresult = stat.executeQuery("SELECT MAX(ID) FROM USERS");
            int userid = 0;

            if (idresult.next()) {
                userid = idresult.getInt(1);
            }
            result.close();
            stat.close();
            // Increment globalid
            userid++;

            PreparedStatement prep =
                    m_conn.prepareStatement("INSERT INTO USERS (ID, NAME, PASSWORD, USERLEVEL, LASTLOGIN) VALUES (?,?,?,?,?)");

            prep.setInt(1, userid);
            prep.setString(2, username);
            prep.setString(3, password);

            int currentlevel;
            try {
                currentlevel = Integer.parseInt(userlevel);
            } catch (NumberFormatException e) {
                currentlevel = m_server.m_props.getIntProperty("DefaultUserLevel");
            }

            prep.setInt(4, currentlevel);
            prep.setString(5, "NULL");

            prep.executeUpdate();
            prep.close();


            MUSLog.Log("User " + username + " added to database", MUSLog.kDB);
            return true;
        } catch (SQLException sqle) {
            MUSLog.Log(sqle, MUSLog.kDB);
            return false;
        }
    }


    public void updateUserLastLoginTime(int userid) {
        try {
            PreparedStatement upprep;
            upprep = m_conn.prepareStatement("UPDATE USERS SET LASTLOGIN=? WHERE ID=?");

            // Update lastlogintime
            LString currenttime = (LString) MUSAttribute.getTime();
            upprep.setString(1, currenttime.toString());
            upprep.setInt(2, userid);
            upprep.executeUpdate();
            upprep.clearParameters();
            upprep.close();

        } catch (SQLException sqle) {
            MUSLog.Log("Failed to update lastlogintime sql value for user " + userid, MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
        }
    }

    public int getDBUser(String usernamein) throws DBException, UserNotFoundException {
        try {

            String username = usernamein.toUpperCase();
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ID FROM USERS WHERE NAME='" + username + "'");

            if (result.next()) {
                int id = result.getInt(1);
                result.close();
                stat.close();
                return id;
            }
            result.close();
            stat.close();
            throw new UserNotFoundException("User not found");

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in getDBUser", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            throw new DBException("User not found");
        }
    }

    public boolean deleteDBUser(int userid) {
        try {
            Statement stat = m_conn.createStatement();
            stat.executeUpdate("DELETE FROM USERS WHERE ID=" + userid);
            stat.close();
            MUSLog.Log("User " + userid + "removed from database", MUSLog.kDB);
            return true;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in deleteDBUser", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return false;
        }
    }


    public int getDBUserLevel(int userid) throws DBException {
        try {
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT USERLEVEL FROM USERS WHERE ID=" + userid);

            if (result.next()) {
                int userlevel;
                userlevel = result.getInt(1);
                result.close();
                stat.close();
                return userlevel;
            }

            result.close();
            stat.close();
            throw new DBException("Userlevel not found");

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in getDBUserLevel", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            throw new DBException("Userlevel not found");
        }
    }

    public String getDBUserPassword(int userid) throws DBException {
        try {
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT PASSWORD FROM USERS WHERE ID=" + userid);

            if (result.next()) {
                String pass = result.getString(1);
                result.close();
                stat.close();
                return pass;
            }
            result.close();
            stat.close();
            throw new DBException("Password not found");

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in getDBUserPassword", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            throw new DBException("Password not found");
        }
    }

    public int checkLogin(MUSUser oneUser, String username, String password) {

        ResultSet rs = null;
        PreparedStatement loginStatement = null;
        try {
            loginStatement = m_conn.prepareStatement("SELECT PASSWORD, USERLEVEL FROM USERS WHERE NAME = ?");
            loginStatement.setString(1, username.toUpperCase());
            rs = loginStatement.executeQuery();

            if (rs.next()) {
                // Account with that name exists in the database
                String storedPW = rs.getString(1);
                if (password.equals(storedPW)) {
                    int storedLevel = rs.getInt(2);
                    oneUser.setuserLevel(storedLevel);
                } else {
                    // Password mismatch
                    return MUSErrorCode.InvalidPassword;
                }
            } else {
                // No account exists
                if (oneUser.m_movie.getServer().authentication == ServerUserDatabase.AUTHENTICATION_REQUIRED) {  // NullPointerException has happened here...
                    // UserRecordRequired
                    return MUSErrorCode.InvalidUserID;
                } else {
                    // No user record exists, but it is ok to login
                    oneUser.setuserLevel(oneUser.m_movie.m_props.getIntProperty("DefaultUserLevel")); // Gets NullPointerException here sometimes !?
                }
            }

        }
        catch (SQLException e) {
            // Allow users to log in if the database breaks
            return 0;
        }
        finally {
            try {
                if (rs != null) rs.close();
            }
            catch (Exception e) {
                //
            }
            try {
                if (loginStatement != null) loginStatement.close();
            }
            catch (Exception e) {
                //
            }
        }

        return 0; // Everything OK. User is cleared to logon.
    }


    public boolean executeUpdate(String sqlquery, LList params) {
        try {
            // Statement stat = m_conn.createStatement();
            PreparedStatement prep;
            prep = m_conn.prepareStatement(sqlquery);

            for (int e = 0; e < params.count(); e++) {
                LValue thisparam = params.getElementAt(e);
                switch (thisparam.getType()) {
                    case LValue.vt_Void:
                        prep.setNull(e + 1, Types.NULL);
                        break;

                    case LValue.vt_Integer:
                        prep.setInt(e + 1, thisparam.toInteger());
                        break;

                    case LValue.vt_Symbol:
                        prep.setString(e + 1, thisparam.toString());
                        break;

                    case LValue.vt_String:
                        prep.setString(e + 1, thisparam.toString());
                        break;

                    case LValue.vt_Picture:
                        prep.setBytes(e + 1, thisparam.toBytes());
                        break;

                    case LValue.vt_Float:
                        prep.setDouble(e + 1, thisparam.toDouble());
                        break;

                    case LValue.vt_Media:
                        prep.setBytes(e + 1, thisparam.toBytes());
                        break;


                    default:
                        prep.setInt(e + 1, 0);
                        break;

                }

            }

            prep.executeUpdate();
            prep.close();

            return true;
        } catch (SQLException sqle) {
            MUSLog.Log("SQL Exception in executeUpdate", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return false;
        } catch (NullPointerException npe) {
            MUSLog.Log("Null result in executeUpdate", MUSLog.kDB);
            MUSLog.Log(npe, MUSLog.kDB);
            return false;
        }
    }

    public LValue executeQuery(String sqlquery, LList params) {
        try {
            PreparedStatement prep;
            prep = m_conn.prepareStatement(sqlquery);

            for (int e = 0; e < params.count(); e++) {
                LValue thisparam = params.getElementAt(e);
                switch (thisparam.getType()) {
                    case LValue.vt_Void:
                        prep.setNull(e + 1, Types.NULL);
                        break;

                    case LValue.vt_Integer:
                        prep.setInt(e + 1, thisparam.toInteger());
                        break;

                    case LValue.vt_Symbol:
                        prep.setString(e + 1, thisparam.toString());
                        break;

                    case LValue.vt_String:
                        prep.setString(e + 1, thisparam.toString());
                        break;

                    case LValue.vt_Picture:
                        prep.setBytes(e + 1, thisparam.toBytes());
                        break;

                    case LValue.vt_Float:
                        prep.setDouble(e + 1, thisparam.toDouble());
                        break;

                    case LValue.vt_Media:
                        prep.setBytes(e + 1, thisparam.toBytes());
                        break;

                    default:
                        prep.setInt(e + 1, 0);
                        break;

                }

            }

            ResultSet result = prep.executeQuery();
            ResultSetMetaData meta = result.getMetaData();
            int numcols = meta.getColumnCount();
            int[] coltypes = new int[numcols];
            for (int i = 0; i < numcols; i++) {
                coltypes[i] = meta.getColumnType(i + 1);
            }

            LList ml = new LList();

            while (result.next()) {
                LList cl = new LList();
                for (int i = 0; i < numcols; i++) {
                    switch (coltypes[i]) {
                        case Types.INTEGER:
                            cl.addElement(LValue.getLValue(result.getInt(i + 1)));
                            break;

                        case Types.SMALLINT:
                            cl.addElement(LValue.getLValue(result.getInt(i + 1)));
                            break;

                        case Types.BIGINT:
                            cl.addElement(LValue.getLValue(result.getInt(i + 1)));
                            break;

                        case Types.TINYINT:
                            cl.addElement(LValue.getLValue(result.getInt(i + 1)));
                            break;

                        case Types.REAL:
                            cl.addElement(LValue.getLValue(result.getFloat(i + 1)));
                            break;

                        case Types.FLOAT:
                            cl.addElement(LValue.getLValue(result.getDouble(i + 1)));
                            break;

                        case Types.NUMERIC:
                            cl.addElement(LValue.getLValue(result.getDouble(i + 1)));
                            break;

                        case Types.DOUBLE:
                            cl.addElement(LValue.getLValue(result.getDouble(i + 1)));
                            break;

                        case Types.DECIMAL:
                            // cl.addElement(LValue.getLValue(result.getDouble(i+1)));
                            cl.addElement(LValue.getLValue(result.getBigDecimal(i + 1).setScale(2).doubleValue()));
                            break;

                        case Types.BINARY:
                            cl.addElement(LValue.getLValue(result.getBytes(i + 1)));
                            break;

                        case Types.VARBINARY:
                            cl.addElement(LValue.getLValue(result.getBytes(i + 1)));
                            break;

                        case Types.LONGVARBINARY:
                            cl.addElement(LValue.getLValue(result.getBytes(i + 1)));
                            break;

                        case Types.NULL:
                            cl.addElement(new LVoid());
                            break;

                        case Types.BIT:
                        case Types.CHAR:
                        case Types.DATE:
                        case Types.LONGVARCHAR:
                        case Types.OTHER:
                        case Types.TIME:
                        case Types.VARCHAR:

                            cl.addElement(LValue.getLValue(result.getString(i + 1)));
                            break;

                        default:
                            cl.addElement(new LVoid());
                            break;
                    }
                }
                ml.addElement(cl);
            }

            result.close();
            prep.close();
            return ml;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL Exception in executeQuery", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return new LList();
        } catch (NullPointerException npe) {
            MUSLog.Log("Null result in executeQuery", MUSLog.kDB);
            MUSLog.Log(npe, MUSLog.kDB);
            return new LList();
        }
    }



    ////////////// Stuff from MUSSQLDispatcher below //////////////

    public void deliver(ServerUser user, MUSMovie mov, String[] args, MUSMessage msg, MUSMessage reply) {
        if (!(m_enabled) & !(args[2].equalsIgnoreCase("connect"))) {
            reply.m_msgContent = new LString("Database disabled");
            user.sendMessage(reply);
            return;
        }

        // int msguserlevel = user.userLevel();

        // First batch of SQL commands take no parameters
        if (args[1].equalsIgnoreCase("SQL")) {
            if (args[2].equalsIgnoreCase("disconnect")) {
                killDBConnection();
                reply.m_msgContent = new LValue();
                //reply.m_msgContent.addElement();
                user.sendMessage(reply);
                return;
            }
        }


        // Other database commands require a property list
        LValue msgcont = msg.m_msgContent;
        if (msgcont.getType() != LValue.vt_PropList) {
            // Error, we need a proplist
            reply.m_errCode = MUSErrorCode.BadParameter;
            reply.m_msgContent = new LInteger(0);
            user.sendMessage(reply);
            return;
        }
        LPropList plist = (LPropList) msgcont;

        if (args[1].equalsIgnoreCase("SQL")) {
            try {
                if (args[2].equalsIgnoreCase("createUser")) {
                    LValue arguserid = new LValue();
                    LValue argpasswd = new LValue();
                    LValue arguserlevel = new LValue();
                    try {
                        arguserid = plist.getElement(new LSymbol("userID"));
                        argpasswd = plist.getElement(new LSymbol("password"));
                    } catch (PropertyNotFoundException pnf) {
                        // userid and password are needed
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    try {
                        arguserlevel = plist.getElement(new LSymbol("userlevel"));
                    } catch (PropertyNotFoundException pnf) {
                        // userlevel is optional
                        arguserlevel = new LInteger(mov.m_props.getIntProperty("DefaultUserLevel"));
                    }

                    // Check types for arguments
                    if (arguserid.getType() != LValue.vt_String ||
                            argpasswd.getType() != LValue.vt_String ||
                            arguserlevel.getType() != LValue.vt_Integer) {
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    LString arguseridstr = (LString) arguserid;
                    LString argpasswdstr = (LString) argpasswd;

                    // Check for illegal characters in arguments
                    StringCharacterIterator sci = new StringCharacterIterator(arguseridstr.toString());
                    for (char c = sci.first(); c != CharacterIterator.DONE; c = sci.next()) {
                        if ((c == '@') || (c == '#'))
                            throw new MUSErrorCode(MUSErrorCode.InvalidUserID);
                    }


                    LInteger userlevelint = (LInteger) arguserlevel;
                    LString arguserlevelstr = new LString(Integer.toString(userlevelint.toInteger()));
                    boolean usercreated = createUser(arguseridstr.toString(), argpasswdstr.toString(), arguserlevelstr.toString());

                    if (usercreated) {
                        LPropList pl = new LPropList();
                        pl.addElement(new LSymbol("userID"), arguserid);
                        reply.m_msgContent = pl;
                    } else {
                        reply.m_errCode = MUSErrorCode.DatabaseDataRecordNotUnique;
                        LPropList pl = new LPropList();
                        pl.addElement(new LSymbol("userID"), arguserid);
                        reply.m_msgContent = pl;
                    }

                    user.sendMessage(reply);
                    return;

                } else if (args[2].equalsIgnoreCase("deleteUser")) {
                    LValue arguserid;
                    try {
                        arguserid = plist.getElement(new LSymbol("userID"));
                    } catch (PropertyNotFoundException pnf) {
                        // userid is needed
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    LList contlist = new LList();
                    MUSMovie.GetStringListFromContents(contlist, arguserid);

                    if (contlist.count() == 0)
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);

                    for (int e = 0; e < contlist.count(); e++) {
                        LString arguseridstr = (LString) contlist.getElementAt(e);

                        int userid = 0;
                        try {
                            userid = getDBUser(arguseridstr.toString());
                        } catch (UserNotFoundException dbe) {
                            MUSLog.Log("Invalid DB Command: could not get user " + arguseridstr.toString(), MUSLog.kMsgErr);
                            throw new MUSErrorCode(MUSErrorCode.InvalidUserID);
                        } catch (DBException dbe) {
                            MUSLog.Log("Invalid DB Command: could not get user " + arguseridstr.toString(), MUSLog.kMsgErr);
                            throw new MUSErrorCode(MUSErrorCode.DatabaseError);
                        }

                        boolean userdeleted = deleteDBUser(userid);

                        if (userdeleted) {
                            LPropList pl = new LPropList();
                            pl.addElement(new LSymbol("userID"), arguseridstr);
                            reply.m_msgContent = pl;
                            user.sendMessage(reply);
                        } else {
                            reply.m_errCode = MUSErrorCode.DatabaseError;
                            LPropList pl = new LPropList();
                            pl.addElement(new LSymbol("userID"), arguseridstr);
                            reply.m_msgContent = pl;
                            user.sendMessage(reply);
                        }
                    }
                    return;
                } else if (args[2].equalsIgnoreCase("connect")) {
                    LValue argsqldriver;
                    LValue argsqlurl;
                    LValue arguserid;
                    LValue argpasswd;

                    try {
                        arguserid = plist.getElement(new LSymbol("userID"));
                        argpasswd = plist.getElement(new LSymbol("password"));
                        argsqldriver = plist.getElement(new LSymbol("driver"));
                        argsqlurl = plist.getElement(new LSymbol("url"));
                    } catch (PropertyNotFoundException pnf) {
                        //userid and password are needed
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }


                    // Check types for arguments
                    if (arguserid.getType() != LValue.vt_String ||
                            argpasswd.getType() != LValue.vt_String ||
                            argsqldriver.getType() != LValue.vt_String ||
                            argsqlurl.getType() != LValue.vt_String) {
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }


                    boolean connOK = connect(argsqldriver.toString(), argsqlurl.toString(), arguserid.toString(), argpasswd.toString());

                    if (connOK)
                        reply.m_msgContent = new LString("Connected");
                    else
                        throw new MUSErrorCode(MUSErrorCode.DatabaseError);

                    user.sendMessage(reply);
                    return;

                } else if (args[2].equalsIgnoreCase("executeUpdate")) {
                    LValue argentry;
                    LValue argparams;
                    try {
                        argentry = plist.getElement(new LSymbol("sql"));
                    } catch (PropertyNotFoundException pnf) {
                        // Entry ais needed
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    try {
                        argparams = plist.getElement(new LSymbol("values"));
                    } catch (PropertyNotFoundException pnf) {
                        // userlevel is optional
                        argparams = new LList();
                    }

                    // Check types for arguments
                    if (argentry.getType() != LValue.vt_String ||
                            argparams.getType() != LValue.vt_List) {
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    LString argentrystr = (LString) argentry;
                    LList argparamslist = (LList) argparams;

                    // Check for illegal number of parameters
                    int argslots = 0;

                    StringCharacterIterator sci = new StringCharacterIterator(argentrystr.toString());
                    for (char c = sci.first(); c != CharacterIterator.DONE; c = sci.next()) {
                        if (c == '?')
                            argslots++;
                    }

                    if (argslots != argparamslist.count())
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);

                    boolean qOK = executeUpdate(argentrystr.toString(), argparamslist);

                    if (qOK)
                        reply.m_msgContent = new LString("UpdateOK");
                    else
                        throw new MUSErrorCode(MUSErrorCode.DatabaseError);


                    user.sendMessage(reply);
                    return;
                } else if (args[2].equalsIgnoreCase("executeQuery")) {
                    LValue argentry = new LValue();
                    LValue argparams = new LValue();
                    try {
                        argentry = plist.getElement(new LSymbol("sql"));
                    } catch (PropertyNotFoundException pnf) {
                        // Entry ais needed
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    try {
                        argparams = plist.getElement(new LSymbol("values"));
                    } catch (PropertyNotFoundException pnf) {
                        // userlevel is optional
                        argparams = new LList();
                    }

                    // Check types for arguments
                    if (argentry.getType() != LValue.vt_String ||
                            argparams.getType() != LValue.vt_List) {
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    LString argentrystr = (LString) argentry;
                    LList argparamslist = (LList) argparams;

                    // Check for illegal number of parameters
                    int argslots = 0;

                    StringCharacterIterator sci = new StringCharacterIterator(argentrystr.toString());
                    for (char c = sci.first(); c != CharacterIterator.DONE; c = sci.next()) {
                        if (c == '?')
                            argslots++;
                    }

                    if (argslots != argparamslist.count())
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);

                    LValue ret = executeQuery(argentrystr.toString(), argparamslist);

                    // reply.m_msgContent.addElement(ret);
                    reply.m_msgContent = ret;

                    user.sendMessage(reply);
                    return;
                }


            } catch (MUSErrorCode err) {
                reply.m_errCode = err.m_errCode;

                reply.m_msgContent = new LInteger(0);
                user.sendMessage(reply);
                return;
            }
        } // End SQL commands
    }

}
