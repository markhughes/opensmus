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
import java.text.*;


public class MUSDBConnection implements ServerUserDatabase {
    private Connection m_conn;
    private MUSServer m_server;
    public boolean m_enabled;

    public MUSDBConnection(MUSServer srv, boolean enableFlag) {
        m_server = srv;

        // Exceptions may occur
        try {

            if (!enableFlag) {
                throw new DBException("Database not enabled");
            }
            // Load the JDBC driver for the internal database (usually hsqldb)
            Class.forName(m_server.m_props.getProperty("MUDatabaseDriver")).newInstance();

            // Connect to the database
            // It will be created automatically by hsqldb if it does not yet exist
            m_conn = DriverManager.getConnection(m_server.m_props.getProperty("MUDatabaseURL"), m_server.m_props.getProperty("MUDatabaseUsername"), m_server.m_props.getProperty("MUDatabasePassword"));

            ensureDBPresence();

            MUSLog.Log("MUS database functions enabled", MUSLog.kSys);
            m_enabled = true;

            processDBConfigCommands();
            purgeBannedTable();
        } catch (DBException e) {
            // Database not enabled exception thrown, config option disabled
            MUSLog.Log("MUS database functions not enabled", MUSLog.kDB);
            m_enabled = false;
        } catch (Exception e) {
            // Print out the error message
            MUSLog.Log("DB initialization error> ", MUSLog.kDB);
            MUSLog.Log(e, MUSLog.kDB);
            m_enabled = false;
        }
    }

    public boolean isEnabled() {
        return m_enabled;
    }

    public void killDBConnection() {
        try {

            if (m_enabled) {
                try {
                    String dbbackend = m_server.m_props.getProperty("MUDatabaseSQLBackend");
                    if (dbbackend.equalsIgnoreCase("hsqldb")) {
                        Statement stat = m_conn.createStatement();
                        stat.executeQuery("SHUTDOWN COMPACT");
                    }

                } catch (SQLException sqle) {
                    MUSLog.Log(sqle, MUSLog.kDB);
                }
                m_conn.close();
            }

            m_enabled = false;
        } catch (SQLException sqle) {
            MUSLog.Log(sqle, MUSLog.kDB);
        }
    }


    public void processDBConfigCommands() {
        String[] nusers = m_server.m_props.getStringListProperty("CreateUser");
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

        String[] nattributes = m_server.m_props.getStringListProperty("DeclareAttribute");
        for (String nattribute : nattributes) {
            if (!nattribute.equalsIgnoreCase("default")) {
                declareAttribute(nattribute);
            }
        }
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
            result.close();

            // Go on, get next global id for this user
            ResultSet idresult = stat.executeQuery("SELECT MAXID FROM IDTABLE WHERE TABLENAME='GLOBALID'");
            int userid = 0;

            if (idresult.next()) {
                userid = idresult.getInt(1);
            }
            idresult.close();

            // Increment globalid
            userid++;

            PreparedStatement prep =
                    m_conn.prepareStatement("INSERT INTO USERS (ID, NAME) VALUES (?,?)");

            prep.setInt(1, userid);
            prep.setString(2, username);

            prep.executeUpdate();
            prep.close();

            // Now insert the related attributes
            PreparedStatement atprep =
                    m_conn.prepareStatement("INSERT INTO ATTRIBUTES (OWNERID, ATTRID ,DATAVALUE) VALUES (?,?,?)");

            atprep.setInt(1, userid);
            atprep.setInt(2, 5); // password
            LString pass = new LString(password);
            atprep.setBytes(3, pass.getBytes());
            atprep.executeUpdate();
            atprep.clearParameters();

            atprep.setInt(1, userid);
            atprep.setInt(2, 6); // lastupdate time
            LString currenttime = (LString) MUSAttribute.getTime();
            atprep.setBytes(3, currenttime.getBytes());
            atprep.executeUpdate();
            atprep.clearParameters();

            atprep.setInt(1, userid);
            atprep.setInt(2, 7); // lastlogin time
            atprep.setBytes(3, currenttime.getBytes());
            atprep.executeUpdate();
            atprep.clearParameters();

            atprep.setInt(1, userid);
            atprep.setInt(2, 3); // userlevel
            LInteger currentlevel;
            try {
                currentlevel = new LInteger(Integer.parseInt(userlevel));
            } catch (NumberFormatException e) {
                currentlevel = new LInteger(m_server.m_props.getIntProperty("DefaultUserLevel"));
            }
            atprep.setBytes(3, currentlevel.getBytes());
            atprep.executeUpdate();
            atprep.clearParameters();

            atprep.setInt(1, userid);
            atprep.setInt(2, 2); // status
            LInteger currentstatus = new LInteger(m_server.m_props.getIntProperty("DefaultUserStatus"));
            atprep.setBytes(3, currentstatus.getBytes());
            atprep.executeUpdate();
            atprep.clearParameters();

            atprep.close();

            // Stuff global id back...
            stat.executeUpdate("UPDATE IDTABLE SET MAXID=" + userid + " WHERE TABLENAME='GLOBALID'");
            stat.close();

            MUSLog.Log("User " + username + " added to database", MUSLog.kDB);
            return true;
        } catch (SQLException sqle) {
            MUSLog.Log(sqle, MUSLog.kDB);
            return false;
        }
    }

    public synchronized boolean createApplication(String applicationnamein, String description) {
        try {

            String applicationname = applicationnamein.toUpperCase();
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT * FROM APPLICATIONS WHERE NAME='" + applicationname + "'");

            if (result.next()) {
                MUSLog.Log("Application " + applicationname + " already in database", MUSLog.kDB);
                return false;
            }
            result.close();


            //go on, get next global id for this user
            ResultSet idresult = stat.executeQuery("SELECT MAXID FROM IDTABLE WHERE TABLENAME='GLOBALID'");
            int appid = 0;

            if (idresult.next()) {
                appid = idresult.getInt(1);
            }
            idresult.close();

            //increment globalid
            appid++;

            PreparedStatement prep =
                    m_conn.prepareStatement("INSERT INTO APPLICATIONS (ID, NAME) VALUES (?,?)");

            prep.setInt(1, appid);
            prep.setString(2, applicationname);

            prep.executeUpdate();
            prep.close();

            //now insert the related attributes
            PreparedStatement atprep =
                    m_conn.prepareStatement("INSERT INTO ATTRIBUTES (OWNERID, ATTRID ,DATAVALUE) VALUES (?,?,?)");


            atprep.setInt(1, appid);
            atprep.setInt(2, 6);//lastupdate time
            LString currenttime = (LString) MUSAttribute.getTime();
            atprep.setBytes(3, currenttime.getBytes());
            atprep.executeUpdate();
            atprep.clearParameters();

            atprep.setInt(1, appid);
            atprep.setInt(2, 4);//description
            LString ldescription = new LString(description);
            atprep.setBytes(3, ldescription.getBytes());
            atprep.executeUpdate();
            atprep.clearParameters();


            atprep.close();

            // stuff global id back...
            stat.executeUpdate("UPDATE IDTABLE SET MAXID=" + appid + " WHERE TABLENAME='GLOBALID'");

            stat.close();
            MUSLog.Log("Application " + applicationname + " added to database", MUSLog.kDB);
            return true;
        } catch (SQLException sqle) {
            MUSLog.Log(sqle, MUSLog.kDB);
            return false;
        }
    }

    public synchronized boolean createDBPlayer(int userid, int appid) {
        try {

            Statement stat = m_conn.createStatement();
            //go on, get next global id for this user
            ResultSet idresult = stat.executeQuery("SELECT MAXID FROM IDTABLE WHERE TABLENAME='GLOBALID'");
            int playerid = 0;

            if (idresult.next()) {
                playerid = idresult.getInt(1);
            }
            idresult.close();

            //increment globalid
            playerid++;

            PreparedStatement prep =
                    m_conn.prepareStatement("INSERT INTO PLAYERS (ID, USERID, APPID) VALUES (?,?,?)");

            prep.setInt(1, playerid);
            prep.setInt(2, userid);
            prep.setInt(3, appid);

            prep.executeUpdate();
            prep.close();

            //now insert the related attributes
            PreparedStatement atprep =
                    m_conn.prepareStatement("INSERT INTO ATTRIBUTES (OWNERID, ATTRID ,DATAVALUE) VALUES (?,?,?)");

            atprep.setInt(1, playerid);
            atprep.setInt(2, 1);//creation time
            LString currenttime = (LString) MUSAttribute.getTime();
            atprep.setBytes(3, currenttime.getBytes());
            atprep.executeUpdate();
            atprep.clearParameters();

            atprep.setInt(1, playerid);
            atprep.setInt(2, 6);//lastupdate time
            atprep.setBytes(3, currenttime.getBytes());
            atprep.executeUpdate();
            atprep.clearParameters();


            atprep.close();

            // stuff global id back...
            stat.executeUpdate("UPDATE IDTABLE SET MAXID=" + playerid + " WHERE TABLENAME='GLOBALID'");
            stat.close();
            MUSLog.Log("DBPlayer added to database", MUSLog.kDB);
            return true;
        } catch (SQLException sqle) {
            MUSLog.Log(sqle, MUSLog.kDB);
            return false;
        }
    }

    public void checkPoint() {
        try {
            String dbbackend = m_server.m_props.getProperty("MUDatabaseSQLBackend");
            if (dbbackend.equalsIgnoreCase("hsqldb")) {
                Statement stat = m_conn.createStatement();
                stat.executeQuery("CHECKPOINT");
                stat.close();
            }

        } catch (SQLException sqle) {
            MUSLog.Log(sqle, MUSLog.kDB);
        }
    }

    public boolean isBanned(String inentry) {
        try {
            String entry = inentry.toUpperCase();

            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT EXPDATE FROM BANLIST WHERE ENTRY='" + entry + "'");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

            if (result.next()) {
                //compare date
                String expdate = result.getString(1);
                try {
                    java.util.Date d = sdf.parse(expdate);
                    if (d.getTime() > (new java.util.Date()).getTime()) {
                        MUSLog.Log("User ban in effect for entry " + entry + " until " + expdate, MUSLog.kDB);
                        return true;
                    } else {
                        removeBannedEntry(entry);
                        return false;
                    }
                } catch (ParseException pe) {
                    MUSLog.Log("Error parsing banned entry date", MUSLog.kDB);
                    return false;
                }
            }
            result.close();
            stat.close();

            return false;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in isBanned", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return false;
        }
    }

    public void purgeBannedTable() {
        try {
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT EXPDATE, ENTRY FROM BANLIST");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

            while (result.next()) {
                //compare date
                String expdate = result.getString(1);
                String entry = result.getString(2);
                try {
                    java.util.Date d = sdf.parse(expdate);
                    if (d.getTime() < (new java.util.Date()).getTime()) {
                        MUSLog.Log("Purging ban entry " + entry, MUSLog.kDB);
                        removeBannedEntry(entry);
                    }
                } catch (ParseException pe) {

                }
            }
            result.close();
            stat.close();

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in purgeBannedTable", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
        }
    }

    public void addBannedEntry(String inentry, int secsToBan) {
        try {
            //entry can be an ipaddress or username
            String entry = inentry.toUpperCase();

            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ENTRY FROM BANLIST WHERE ENTRY='" + entry + "'");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            java.util.Date expires = new java.util.Date(System.currentTimeMillis() + (secsToBan * 1000));
            String expdate = sdf.format(expires);

            if (result.next()) {
                //whe have an entry, update it
                PreparedStatement prep =
                        m_conn.prepareStatement("UPDATE BANLIST SET EXPDATE=? WHERE ENTRY=?");
                prep.setString(1, expdate);
                prep.setString(2, entry);
                prep.executeUpdate();
                prep.close();

            } else {
                //new entry
                PreparedStatement prep =
                        m_conn.prepareStatement("INSERT INTO BANLIST (ENTRY, EXPDATE) VALUES (?,?)");
                prep.setString(1, entry);
                prep.setString(2, expdate);
                prep.executeUpdate();
                prep.close();
            }
            result.close();
            stat.close();

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in addBanEntry", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
        }
    }

    public void removeBannedEntry(String inentry) {
        try {
            String entry = inentry.toUpperCase();

            PreparedStatement prep =
                    m_conn.prepareStatement("DELETE FROM BANLIST WHERE ENTRY=?");
            prep.setString(1, entry);
            prep.executeUpdate();
            prep.close();

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in addBanEntry", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
        }
    }

    public synchronized int createApplicationData(int applicationid) {
        try {

            Statement stat = m_conn.createStatement();

            //get next global id for this user
            ResultSet idresult = stat.executeQuery("SELECT MAXID FROM IDTABLE WHERE TABLENAME='GLOBALID'");
            int appdataid = 0;

            if (idresult.next()) {
                appdataid = idresult.getInt(1);
            }

            idresult.close();

            //increment globalid
            appdataid++;


            Statement stat2 = m_conn.createStatement();
            stat2.executeUpdate("INSERT INTO APPDATA (ID, APPID) VALUES (" + appdataid + "," + applicationid + ")");
            stat2.close();

            // stuff global id back...
            stat.executeUpdate("UPDATE IDTABLE SET MAXID=" + appdataid + " WHERE TABLENAME='GLOBALID'");
            stat.close();


            MUSLog.Log("Application data " + appdataid + " added to database", MUSLog.kDB);
            return appdataid;

        } catch (SQLException sqle) {
            MUSLog.Log(sqle, MUSLog.kDB);
            return -1;
        }
    }

    public synchronized boolean declareAttribute(String attributenamein) {
        try {

            String attributename = attributenamein.toUpperCase();
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT * FROM ATTRLIST WHERE NAME='" + attributename + "'");

            if (result.next()) {
                MUSLog.Log("Attribute " + attributename + " already in database", MUSLog.kDB);
                return false;
            }
            result.close();

            //go on, get next global id for this user
            ResultSet idresult = stat.executeQuery("SELECT MAXID FROM IDTABLE WHERE TABLENAME='ATTRID'");
            int attrid = 0;

            if (idresult.next()) {
                attrid = idresult.getInt(1);
            }
            idresult.close();

            //increment attributeid
            attrid++;

            PreparedStatement prep =
                    m_conn.prepareStatement("INSERT INTO ATTRLIST (ID, NAME) VALUES (?,?)");

            prep.setInt(1, attrid);
            prep.setString(2, attributename);

            prep.executeUpdate();
            prep.close();

            // stuff attribute id back...
            stat.executeUpdate("UPDATE IDTABLE SET MAXID=" + attrid + " WHERE TABLENAME='ATTRID'");
            stat.close();

            MUSLog.Log("Attribute " + attributename + " added to database", MUSLog.kDB);
            return true;
        } catch (SQLException sqle) {
            MUSLog.Log(sqle, MUSLog.kDB);
            return false;
        }
    }

    public void ensureDBPresence() throws SQLException {

        try {
            // Create a statement object
            Statement stat = m_conn.createStatement();
            //try a query to verify if the default tables are there
            ResultSet result = stat.executeQuery("SELECT * FROM ATTRLIST");
            result.close();
            stat.close();

        } catch (SQLException sqle) {
            //db not initialized, init it
            createDefaultDB();
        }
    }

    public void createDefaultDB() throws SQLException {

        String dbbackend = m_server.m_props.getProperty("MUDatabaseSQLBackend");
        String longchartype, binarytype, tabletype;
        if (dbbackend.equalsIgnoreCase("mysql")) {
            tabletype = "CREATE TABLE ";
            longchartype = "text";
            binarytype = "blob";
        } else if (dbbackend.equalsIgnoreCase("postgresql")) {
            tabletype = "CREATE TABLE ";
            longchartype = "text";
            //binarytype = new String("oid");
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
        //default
        {
            tabletype = "CREATE TABLE ";
            longchartype = "VARCHAR";
            binarytype = "LONGVARBINARY";
        }


        Statement stat = m_conn.createStatement();

        stat.executeUpdate(tabletype + "APPDATA(ID INT,APPID INT)");

        stat.executeUpdate("CREATE INDEX IDXAPPDATA ON APPDATA(APPID)");

        stat.executeUpdate(tabletype + "APPLICATIONS(ID INT,NAME " + longchartype + ")");

        stat.executeUpdate("CREATE UNIQUE INDEX IDXAPPLICATIONS ON APPLICATIONS(ID)");

        stat.executeUpdate(tabletype + "ATTRIBUTES(OWNERID INT,ATTRID INT,DATAVALUE " + binarytype + ")");

        stat.executeUpdate("CREATE INDEX IDXATTRIBUTES ON ATTRIBUTES(OWNERID,ATTRID)");

        stat.executeUpdate(tabletype + "IDTABLE(TABLENAME " + longchartype + ",MAXID INT)");

        stat.executeUpdate(tabletype + "PLAYERS(ID INT,USERID INT,APPID INT)");

        stat.executeUpdate("CREATE INDEX IDXPLAYERS ON PLAYERS(USERID)");

        stat.executeUpdate(tabletype + "USERS(ID INT,NAME " + longchartype + ")");

        stat.executeUpdate("CREATE UNIQUE INDEX IDXUSERS ON USERS(ID)");

        stat.executeUpdate(tabletype + "ATTRLIST(ID INT,NAME " + longchartype + ")");

        stat.executeUpdate("CREATE UNIQUE INDEX IDXATTRLIST ON ATTRLIST(ID)");

        stat.executeUpdate(tabletype + "BANLIST(ENTRY " + longchartype + ",EXPDATE " + longchartype + ")");

        stat.executeUpdate("INSERT INTO IDTABLE VALUES('ATTRID',7)");
        stat.executeUpdate("INSERT INTO IDTABLE VALUES('GLOBALID',0)");
        stat.executeUpdate("INSERT INTO ATTRLIST VALUES(1,'CREATIONTIME')");
        stat.executeUpdate("INSERT INTO ATTRLIST VALUES(2,'STATUS')");
        stat.executeUpdate("INSERT INTO ATTRLIST VALUES(3,'USERLEVEL')");
        stat.executeUpdate("INSERT INTO ATTRLIST VALUES(4,'DESCRIPTION')");
        stat.executeUpdate("INSERT INTO ATTRLIST VALUES(5,'PASSWORD')");
        stat.executeUpdate("INSERT INTO ATTRLIST VALUES(6,'LASTUPDATETIME')");
        stat.executeUpdate("INSERT INTO ATTRLIST VALUES(7,'LASTLOGINTIME')");

        stat.close();
/*	stat.executeQuery("CREATE ALIAS DAYNAME FOR 'org.hsqldb.Library.dayname'");
	stat.executeQuery("CREATE ALIAS SPACE FOR 'org.hsqldb.Library.space'");
	stat.executeQuery("CREATE ALIAS SUBSTRING FOR 'org.hsqldb.Library.substring'");
	stat.executeQuery("CREATE ALIAS SQRT FOR 'java.lang.Math.sqrt'");
	stat.executeQuery("CREATE ALIAS ABS FOR 'java.lang.Math.abs'");
	stat.executeQuery("CREATE ALIAS POWER FOR 'java.lang.Math.pow'");
	stat.executeQuery("CREATE ALIAS CHAR FOR 'org.hsqldb.Library.character'");
	stat.executeQuery("CREATE ALIAS CONCAT FOR 'org.hsqldb.Library.concat'");
	stat.executeQuery("CREATE ALIAS PI FOR 'org.hsqldb.Library.pi'");
	stat.executeQuery("CREATE ALIAS SECOND FOR 'org.hsqldb.Library.second'");
	stat.executeQuery("CREATE ALIAS TRUNCATE FOR 'org.hsqldb.Library.truncate'");
	stat.executeQuery("CREATE ALIAS MONTH FOR 'org.hsqldb.Library.month'");
	stat.executeQuery("CREATE ALIAS LOWER FOR 'org.hsqldb.Library.lcase'");
	stat.executeQuery("CREATE ALIAS ATAN2 FOR 'java.lang.Math.atan2'");
	stat.executeQuery("CREATE ALIAS REPEAT FOR 'org.hsqldb.Library.repeat'");
	stat.executeQuery("CREATE ALIAS DAYOFMONTH FOR 'org.hsqldb.Library.dayofmonth'");
	stat.executeQuery("CREATE ALIAS TAN FOR 'java.lang.Math.tan'");
	stat.executeQuery("CREATE ALIAS RADIANS FOR 'java.lang.Math.toRadians'");
	stat.executeQuery("CREATE ALIAS FLOOR FOR 'java.lang.Math.floor'");
	stat.executeQuery("CREATE ALIAS NOW FOR 'org.hsqldb.Library.now'");
	stat.executeQuery("CREATE ALIAS ACOS FOR 'java.lang.Math.acos'");
	stat.executeQuery("CREATE ALIAS DAYOFWEEK FOR 'org.hsqldb.Library.dayofweek'");
	stat.executeQuery("CREATE ALIAS CEILING FOR 'java.lang.Math.ceil'");
	stat.executeQuery("CREATE ALIAS DAYOFYEAR FOR 'org.hsqldb.Library.dayofyear'");
	stat.executeQuery("CREATE ALIAS LCASE FOR 'org.hsqldb.Library.lcase'");
	stat.executeQuery("CREATE ALIAS WEEK FOR 'org.hsqldb.Library.week'");
	stat.executeQuery("CREATE ALIAS SOUNDEX FOR 'org.hsqldb.Library.soundex'");
	stat.executeQuery("CREATE ALIAS ASIN FOR 'java.lang.Math.asin'");
	stat.executeQuery("CREATE ALIAS LOCATE FOR 'org.hsqldb.Library.locate'");
	stat.executeQuery("CREATE ALIAS EXP FOR 'java.lang.Math.exp'");
	stat.executeQuery("CREATE ALIAS MONTHNAME FOR 'org.hsqldb.Library.monthname'");
	stat.executeQuery("CREATE ALIAS YEAR FOR 'org.hsqldb.Library.year'");
	stat.executeQuery("CREATE ALIAS LEFT FOR 'org.hsqldb.Library.left'");
	stat.executeQuery("CREATE ALIAS ROUNDMAGIC FOR 'org.hsqldb.Library.roundMagic'");
	stat.executeQuery("CREATE ALIAS BITOR FOR 'org.hsqldb.Library.bitor'");
	stat.executeQuery("CREATE ALIAS LTRIM FOR 'org.hsqldb.Library.ltrim'");
	stat.executeQuery("CREATE ALIAS COT FOR 'org.hsqldb.Library.cot'");
	stat.executeQuery("CREATE ALIAS COS FOR 'java.lang.Math.cos'");
	stat.executeQuery("CREATE ALIAS MOD FOR 'org.hsqldb.Library.mod'");
	stat.executeQuery("CREATE ALIAS SIGN FOR 'org.hsqldb.Library.sign'");
	stat.executeQuery("CREATE ALIAS DEGREES FOR 'java.lang.Math.toDegrees'");
	stat.executeQuery("CREATE ALIAS LOG FOR 'java.lang.Math.log'");
	stat.executeQuery("CREATE ALIAS SIN FOR 'java.lang.Math.sin'");
	stat.executeQuery("CREATE ALIAS CURTIME FOR 'org.hsqldb.Library.curtime'");
	stat.executeQuery("CREATE ALIAS DIFFERENCE FOR 'org.hsqldb.Library.difference'");
	stat.executeQuery("CREATE ALIAS INSERT FOR 'org.hsqldb.Library.insert'");
	stat.executeQuery("CREATE ALIAS SUBSTR FOR 'org.hsqldb.Library.substring'");
	stat.executeQuery("CREATE ALIAS DATABASE FOR 'org.hsqldb.Library.database'");
	stat.executeQuery("CREATE ALIAS MINUTE FOR 'org.hsqldb.Library.minute'");
	stat.executeQuery("CREATE ALIAS HOUR FOR 'org.hsqldb.Library.hour'");
	stat.executeQuery("CREATE ALIAS IDENTITY FOR 'org.hsqldb.Library.identity'");
	stat.executeQuery("CREATE ALIAS QUARTER FOR 'org.hsqldb.Library.quarter'");
	stat.executeQuery("CREATE ALIAS CURDATE FOR 'org.hsqldb.Library.curdate'");
	stat.executeQuery("CREATE ALIAS BITAND FOR 'org.hsqldb.Library.bitand'");
	stat.executeQuery("CREATE ALIAS USER FOR 'org.hsqldb.Library.user'");
	stat.executeQuery("CREATE ALIAS UCASE FOR 'org.hsqldb.Library.ucase'");
	stat.executeQuery("CREATE ALIAS RTRIM FOR 'org.hsqldb.Library.rtrim'");
	stat.executeQuery("CREATE ALIAS LOG10 FOR 'org.hsqldb.Library.log10'");
	stat.executeQuery("CREATE ALIAS RIGHT FOR 'org.hsqldb.Library.right'");
	stat.executeQuery("CREATE ALIAS ATAN FOR 'java.lang.Math.atan'");
	stat.executeQuery("CREATE ALIAS UPPER FOR 'org.hsqldb.Library.ucase'");
	stat.executeQuery("CREATE ALIAS ASCII FOR 'org.hsqldb.Library.ascii'");
	stat.executeQuery("CREATE ALIAS RAND FOR 'java.lang.Math.random'");
	stat.executeQuery("CREATE ALIAS LENGTH FOR 'org.hsqldb.Library.length'");
	stat.executeQuery("CREATE ALIAS ROUND FOR 'org.hsqldb.Library.round'");
	stat.executeQuery("CREATE ALIAS REPLACE FOR 'org.hsqldb.Library.replace'");*/
    }

    public LValue srvcmd_getUserCount() {
        try {

            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ID FROM USERS");
            int numofusers = 0;

            while (result.next()) {
                numofusers++;
            }
            result.close();
            stat.close();

            return new LInteger(numofusers);

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in srvcmd_getUserCount", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return new LInteger(0);
        }
    }

    public LValue srvcmd_getUserNames() {
        try {

            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT NAME FROM USERS");
            LList ulist = new LList();

            while (result.next()) {
                ulist.addElement(new LString(result.getString(1)));
            }
            result.close();
            stat.close();

            return ulist;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in srvcmd_getUserNames", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return new LList();
        }
    }

    public LValue srvcmd_getApplicationCount() {
        try {

            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ID FROM APPLICATIONS");
            int numofapps = 0;

            while (result.next()) {
                numofapps++;
            }
            result.close();
            stat.close();

            return new LInteger(numofapps);

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in srvcmd_getApplicationCount", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return new LInteger(0);
        }
    }

    public LValue srvcmd_getApplicationNames() {
        try {

            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT NAME FROM APPLICATIONS");
            LList alist = new LList();

            while (result.next()) {
                alist.addElement(new LString(result.getString(1)));
            }
            result.close();
            stat.close();

            return alist;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in srvcmd_getApplicationNames", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return new LList();
        }
    }

    public LValue srvcmd_getAttributeCount() {
        try {

            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ID FROM ATTRLIST");
            int numofatts = 0;

            while (result.next()) {
                numofatts++;
            }
            result.close();
            stat.close();

            return new LInteger(numofatts);

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in srvcmd_getAttributeCount", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return new LInteger(0);
        }
    }

    public LValue srvcmd_getAttributeNames() {
        try {

            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT NAME FROM ATTRLIST");
            LList alist = new LList();

            while (result.next()) {
                alist.addElement(new LString(result.getString(1)));
            }
            result.close();
            stat.close();

            return alist;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in srvcmd_getAttributeNames", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return new LList();
        }
    }

    public LValue srvcmd_getBanned() {
        try {

            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ENTRY, EXPDATE FROM BANLIST");
            LList blist = new LList();

            while (result.next()) {
                LPropList entry = new LPropList();
                entry.addElement(new LSymbol("user"), new LString(result.getString(1)));
                entry.addElement(new LSymbol("expires"), new LString(result.getString(2)));
                blist.addElement(entry);
            }
            result.close();
            stat.close();

            return blist;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in srvcmd_getApplicationNames", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return new LList();
        }
    }

    public void updateUserLastLoginTime(int userid) {
        try {

            PreparedStatement upprep;
            upprep = m_conn.prepareStatement("UPDATE ATTRIBUTES SET DATAVALUE=? WHERE OWNERID=? AND ATTRID=?");

            //update lastlogintime
            LString currenttime = (LString) MUSAttribute.getTime();
            upprep.setBytes(1, currenttime.getBytes());
            upprep.setInt(2, userid);
            upprep.setInt(3, 7); //lastlogin time
            upprep.executeUpdate();
            upprep.clearParameters();
            upprep.close();

        } catch (SQLException sqle) {
            MUSLog.Log("Failed to update lastlogintime attr for user " + userid, MUSLog.kDB);
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
            stat.executeUpdate("DELETE FROM ATTRIBUTES WHERE OWNERID=" + userid);

            Statement stat2 = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ID FROM PLAYERS WHERE USERID=" + userid);

            while (result.next()) {
                int playerid = result.getInt(1);
                deleteDBPlayer(playerid);
            }
            result.close();
            stat.close();
            stat2.close();

            MUSLog.Log("User " + userid + "removed from database", MUSLog.kDB);
            return true;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in deleteDBUser", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return false;
        }
    }

    public int getAttribute(String attributenamein) throws DBException {
        try {

            String attributename = attributenamein.toUpperCase();
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ID FROM ATTRLIST WHERE NAME='" + attributename + "'");

            if (result.next()) {
                int id = result.getInt(1);
                result.close();
                stat.close();
                return id;
            }

            result.close();
            stat.close();
            throw new DBException("Attribute not found");

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in getAttribute", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            throw new DBException("Attribute not found");
        }
    }

    public boolean deleteAttribute(int attid) {
        try {

            //do not delete default attributes
            if (attid < 8)
                return false;

            Statement stat = m_conn.createStatement();
            stat.executeUpdate("DELETE FROM ATTRIBUTES WHERE ATTRID=" + attid);
            stat.executeUpdate("DELETE FROM ATTRLIST WHERE ID=" + attid);
            stat.close();

            MUSLog.Log("Attribute removed from database", MUSLog.kDB);
            return true;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in deleteAttribute", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return false;
        }
    }

    public boolean removeAttribute(int ownerid, MUSAttribute attin) throws AttributeNotFoundException {
        try {

            int attid = -1;
            String attributename = attin.getName().toString().toUpperCase();
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ID FROM ATTRLIST WHERE NAME='" + attributename + "'");

            if (result.next()) {
                attid = result.getInt(1);
            }
            result.close();

            if (attid == -1) {
                stat.close();
                throw new AttributeNotFoundException("Attribute not found");
            }

            //do not delete default attributes
            if (attid < 8) {
                stat.close();
                return false;
            }

            stat.executeUpdate("DELETE FROM ATTRIBUTES WHERE ATTRID=" + attid + " AND OWNERID=" + ownerid);
            stat.close();

            MUSLog.Log("Attribute removed from database", MUSLog.kDB);
            return true;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in removeAttribute", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return false;
        }
    }

    public LValue getAttributeNames(int ownerid) {
        LList cl = new LList();
        try {
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ATTRLIST.NAME FROM ATTRLIST, ATTRIBUTES WHERE ATTRIBUTES.OWNERID=" + ownerid + " AND ATTRLIST.ID = ATTRIBUTES.ATTRID");

            while (result.next()) {
                String attrname = result.getString(1);

                cl.addElement(new LSymbol(attrname));
            }
            result.close();
            stat.close();

            return cl;
        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in getAttributeNames", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return cl;
        }

    }

    public MUSAttribute getAttribute(int ownerid, String attributenamein) throws DBException, AttributeNotFoundException {
        try {

            int id = -1;
            String attributename = attributenamein.toUpperCase();
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ID FROM ATTRLIST WHERE NAME='" + attributename + "'");

            if (result.next()) {
                id = result.getInt(1);
            }
            result.close();

            if (id == -1) {
                stat.close();
                throw new AttributeNotFoundException("Attribute not found");
            }

            result = stat.executeQuery("SELECT DATAVALUE FROM ATTRIBUTES WHERE ATTRID=" + id + " AND OWNERID=" + ownerid);

            if (result.next()) {
                MUSAttribute att = new MUSAttribute(new LSymbol(attributename), LValue.fromRawBytes(result.getBytes(1), 0));
                result.close();
                stat.close();
                return att;
            }

            stat.close();
            //no results, throw exception
            throw new AttributeNotFoundException("Attribute not found");

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in getAttribute", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            throw new DBException("Attribute not found");
        }
    }

    public boolean getAttributeExists(int ownerid, int attrid) throws DBException {
        try {

            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT * FROM ATTRIBUTES WHERE OWNERID=" + ownerid + " AND ATTRID=" + attrid);

            if (result.next()) {
                result.close();
                stat.close();
                return true;
            }

            //no results
            result.close();
            stat.close();
            return false;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in getAttributeExists", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            throw new DBException("Error while checking if Attribute exists in DB");
        }
    }

    public void setAttribute(int ownerid, MUSAttribute attin) throws DBException {
        try {

            LValue attvalue = attin.get();
            int attrid = -1;
            String attributename = attin.getName().toString().toUpperCase();
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ID FROM ATTRLIST WHERE NAME='" + attributename + "'");

            if (result.next()) {
                attrid = result.getInt(1);
            }
            result.close();
            stat.close();

            if (attrid == -1) {
                throw new DBException("Attribute not found");
            }

            //check if attribute exists

            boolean recordexists = getAttributeExists(ownerid, attrid);

            PreparedStatement upprep, inprep;
            upprep = m_conn.prepareStatement("UPDATE ATTRIBUTES SET DATAVALUE=? WHERE OWNERID=? AND ATTRID=?");

            if (!recordexists) {
                inprep = m_conn.prepareStatement("INSERT INTO ATTRIBUTES (OWNERID, ATTRID ,DATAVALUE) VALUES (?,?,?)");
                inprep.setInt(1, ownerid);
                inprep.setInt(2, attrid);
                inprep.setBytes(3, attvalue.getBytes());
                inprep.executeUpdate();
                inprep.clearParameters();
                inprep.close();
            } else {
                upprep.setBytes(1, attvalue.getBytes());
                upprep.setInt(2, ownerid);
                upprep.setInt(3, attrid);
                upprep.executeUpdate();
                upprep.clearParameters();
                //upprep.close();
            }

            //update lastupdatetime
            LString currenttime = (LString) MUSAttribute.getTime();
            upprep.setBytes(1, currenttime.getBytes());
            upprep.setInt(2, ownerid);
            upprep.setInt(3, 6); //lastupdate time
            upprep.executeUpdate();
            upprep.clearParameters();
            upprep.close();


            //throw new DBException("Failed to set attribute");

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in setAttribute", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            throw new DBException("Failed to set attribute");
        }
    }

    public void setDBObjectAttributes(int objectid, LList attrlist, LList vallist) {
        // atrrlist is a LSymbol list, objectid is known to exist
        // Now insert the related attributes
        try {
            PreparedStatement atprep =
                    m_conn.prepareStatement("INSERT INTO ATTRIBUTES (OWNERID, ATTRID ,DATAVALUE) VALUES (?,?,?)");

            for (int a = 0; a < attrlist.count(); a++) {
                LInteger attributeid = (LInteger) attrlist.getElementAt(a);
                LValue attrvalue = vallist.getElementAt(a);

                atprep.setInt(1, objectid);
                atprep.setInt(2, attributeid.toInteger()); // lastupdate time
                // LString currenttime = (LString) MUSAttribute.getTime();
                atprep.setBytes(3, attrvalue.getBytes());
                atprep.executeUpdate();
                atprep.clearParameters();
            }
            atprep.close();
        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in setDBObjectAttribute", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
        }

    }

    public int getDBUserLevel(int userid) throws DBException {
        try {
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT DATAVALUE FROM ATTRIBUTES WHERE OWNERID=" + userid + " AND ATTRID=3");

            if (result.next()) {
                byte[] vl = result.getBytes(1);
                LInteger lvl = new LInteger(0);
                lvl.extractFromBytes(vl, 2); // Extract the value without first two bytes, type info
                result.close();
                stat.close();
                return lvl.toInteger();
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
            /*ResultSet result = stat.executeQuery("SELECT PASSWORD FROM USERS WHERE ID="+userid);

           if (result.next()) {
               String pass = result.getString(1);

               return pass;
           }*/
            ResultSet result = stat.executeQuery("SELECT DATAVALUE FROM ATTRIBUTES WHERE OWNERID=" + userid + " AND ATTRID=5");

            if (result.next()) {
                LValue res = LValue.fromRawBytes(result.getBytes(1), 0);
                result.close();
                stat.close();
                if (res.getType() == LValue.vt_String)
                    return res.toString();
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

        try {
            int userid = this.getDBUser(username.toUpperCase());
            String passwd = this.getDBUserPassword(userid);
            if (passwd.equals(password)) {
                try {
                    oneUser.setuserLevel(this.getDBUserLevel(userid));
                } catch (DBException dbex) {
                    oneUser.setuserLevel(oneUser.m_movie.m_props.getIntProperty("DefaultUserLevel"));
                }

                return 0; // OK to log in
            } else {
                return MUSErrorCode.InvalidPassword;
            }
        } catch (UserNotFoundException dbe) {
            if (oneUser.m_movie.getServer().authentication == ServerUserDatabase.AUTHENTICATION_REQUIRED) {
                return MUSErrorCode.InvalidUserID;
            } else {
                // No user record exists, but it is ok to login
                oneUser.setuserLevel(oneUser.m_movie.m_props.getIntProperty("DefaultUserLevel")); // Gets NullPointerException here sometimes !?
                return 0;
            }
        } catch (DBException dbe) {
            if (oneUser.m_movie.getServer().authentication == ServerUserDatabase.AUTHENTICATION_REQUIRED) {
                return MUSErrorCode.InvalidUserID;
            } else {
                // No user record exists, but it is ok to login
                oneUser.setuserLevel(oneUser.m_movie.m_props.getIntProperty("DefaultUserLevel"));
            }
        }

        return 0; // Everything OK. User is cleared to logon.
    }

    public int getDBApplication(String appnamein) throws DBException {
        try {
            String appname = appnamein.toUpperCase();

            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ID FROM APPLICATIONS WHERE NAME='" + appname + "'");

            if (result.next()) {
                int id = result.getInt(1);
                result.close();
                stat.close();
                return id;
            }

            result.close();
            stat.close();
            throw new DBException("Application not found");

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in getDBApplication", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            throw new DBException("Application not found");
        }
    }

    public boolean deleteDBApplication(int appid) {
        try {

            Statement stat = m_conn.createStatement();
            stat.executeQuery("DELETE FROM APPLICATIONS WHERE ID=" + appid);

            Statement stat2 = m_conn.createStatement();
            ResultSet result = stat2.executeQuery("SELECT ID FROM APPDATA WHERE APPID=" + appid);

            while (result.next()) {
                int appdataid = result.getInt(1);
                deleteDBApplicationData(appdataid);
            }
            result.close();
            stat.close();

            Statement stat3 = m_conn.createStatement();
            ResultSet result2 = stat2.executeQuery("SELECT ID FROM PLAYERS WHERE APPID=" + appid);

            while (result2.next()) {
                int playerid = result2.getInt(1);
                deleteDBPlayer(playerid);
            }
            result2.close();
            stat2.close();
            stat3.close();

            MUSLog.Log("Application " + appid + " removed from database", MUSLog.kDB);

            return true;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in deleteDBApplication", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return false;
        }
    }

    public boolean deleteDBApplicationData(int appdataid) {
        try {

            Statement stat = m_conn.createStatement();
            stat.executeUpdate("DELETE FROM APPDATA WHERE ID=" + appdataid);
            stat.executeUpdate("DELETE FROM ATTRIBUTES WHERE OWNERID=" + appdataid);
            stat.close();
            MUSLog.Log("Application data " + appdataid + " removed from database", MUSLog.kDB);
            return true;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in deleteDBApplicationData", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return false;
        }
    }

    public boolean deleteDBPlayer(int playerid) {
        try {

            Statement stat = m_conn.createStatement();
            stat.executeUpdate("DELETE FROM PLAYERS WHERE ID=" + playerid);
            stat.executeUpdate("DELETE FROM ATTRIBUTES WHERE OWNERID=" + playerid);
            stat.close();
            MUSLog.Log("Player " + playerid + " removed from database", MUSLog.kDB);

            return true;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in deleteDBPlayer", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return false;
        }
    }

    public int getDBPlayer(int userid, int appid) throws DBException, PlayerNotFoundException {
        try {
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ID FROM PLAYERS WHERE USERID=" + userid + " AND APPID=" + appid);

            if (result.next()) {
                int id = result.getInt(1);
                result.close();
                stat.close();
                return id;
            }
            result.close();
            stat.close();
            throw new PlayerNotFoundException("Player not found");

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in getDBPlayer", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            throw new DBException("Player not found");
        }
    }

    public LList getApplicationDataListFromApplicationID(int appid) {
        LList appdataidlist = new LList();
        try {
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ID FROM APPDATA WHERE APPID=" + appid);

            while (result.next()) {
                int appdataid = result.getInt(1);
                appdataidlist.addElement(new LInteger(appdataid));
            }

            result.close();
            stat.close();
            return appdataidlist;
        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in getApplicationDataListFromApplicationID", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return appdataidlist;
        }

    }

    public LPropList getApplicationDataAsL(int appid) {
        LPropList res = new LPropList();
        try {
            Statement stat = m_conn.createStatement();
            ResultSet result = stat.executeQuery("SELECT ATTRLIST.NAME, ATTRIBUTES.DATAVALUE FROM ATTRLIST, ATTRIBUTES WHERE ATTRIBUTES.OWNERID=" + appid + " AND ATTRLIST.ID = ATTRIBUTES.ATTRID");

            while (result.next()) {
                String attrname = result.getString(1);
                LValue attrval = LValue.fromRawBytes(result.getBytes(2), 0);

                res.addElement(new LSymbol(attrname), attrval);
            }
            result.close();
            stat.close();
            return res;
        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in getApplicationDataAsL", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return res;
        }

    }

    public LList searchApplicationDataForText(int appid, int attrid, LList appdataidlist, LValue argsearch1) {
        LList resultlist = new LList();
        try {
            Statement stat = m_conn.createStatement();
            LString lstring = (LString) argsearch1;
            String stringtosearch = lstring.toString();


            for (int a = 0; a < appdataidlist.count(); a++) {
                LInteger thisappLint = (LInteger) appdataidlist.getElementAt(a);
                int thisappid = thisappLint.toInteger();
                ResultSet result = stat.executeQuery("SELECT DATAVALUE FROM ATTRIBUTES WHERE ATTRID=" + attrid + " AND OWNERID=" + thisappid);

                while (result.next()) {
                    byte[] bytevalue = result.getBytes(1);
                    int elemType = ConversionUtils.byteArrayToShort(bytevalue, 0);
                    if (elemType == LValue.vt_String) {
                        LString thisrestr = new LString();
                        thisrestr.extractFromBytes(bytevalue, 2);
                        if (thisrestr.toString().equalsIgnoreCase(stringtosearch))
                            resultlist.addElement(new LInteger(thisappid));
                    }
                }
                result.close();
            }
            stat.close();
            return resultlist;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in searchApplicationDataForText", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return resultlist;
        }
    }

    public LList searchApplicationDataForNumber(int appid, int attrid, LList appdataidlist, LValue argsearch1) {
        LList resultlist = new LList();
        LInteger lint;
        try {

            Statement stat = m_conn.createStatement();
            lint = (LInteger) argsearch1;

            for (int a = 0; a < appdataidlist.count(); a++) {
                LInteger thisappLint = (LInteger) appdataidlist.getElementAt(a);
                int thisappid = thisappLint.toInteger();
                ResultSet result = stat.executeQuery("SELECT DATAVALUE FROM ATTRIBUTES WHERE ATTRID=" + attrid + " AND OWNERID=" + thisappid);

                while (result.next()) {
                    byte[] bytevalue = result.getBytes(1);
                    int elemType = ConversionUtils.byteArrayToShort(bytevalue, 0);
                    if (elemType == LValue.vt_Integer) {
                        LInteger thisresint = new LInteger();
                        thisresint.extractFromBytes(bytevalue, 2);
                        if (thisresint.toInteger() == lint.toInteger())
                            resultlist.addElement(new LInteger(thisappid));
                    }
                }
                result.close();
            }
            stat.close();
            return resultlist;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in searchApplicationDataForNumber", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return resultlist;
        }

    }

    public LList searchApplicationDataForRange(int appid, int attrid, LList appdataidlist, LValue argsearch1, LValue argsearch2) {
        LList resultlist = new LList();
        LInteger lint;
        LInteger lint2;
        try {

            Statement stat = m_conn.createStatement();
            lint = (LInteger) argsearch1;
            lint2 = (LInteger) argsearch2;

            for (int a = 0; a < appdataidlist.count(); a++) {
                LInteger thisappLint = (LInteger) appdataidlist.getElementAt(a);
                int thisappid = thisappLint.toInteger();
                ResultSet result = stat.executeQuery("SELECT DATAVALUE FROM ATTRIBUTES WHERE ATTRID=" + attrid + " AND OWNERID=" + thisappid);

                while (result.next()) {
                    byte[] bytevalue = result.getBytes(1);
                    int elemType = ConversionUtils.byteArrayToShort(bytevalue, 0);
                    if (elemType == LValue.vt_Integer) {
                        LInteger thisresint = new LInteger();
                        thisresint.extractFromBytes(bytevalue, 2);
                        if ((thisresint.toInteger() <= lint2.toInteger()) & (thisresint.toInteger() >= lint.toInteger()))
                            resultlist.addElement(new LInteger(thisappid));
                    }
                }
                result.close();
            }
            stat.close();
            return resultlist;

        } catch (SQLException sqle) {
            MUSLog.Log("SQL exception in searchApplicationDataForRange", MUSLog.kDB);
            MUSLog.Log(sqle, MUSLog.kDB);
            return resultlist;
        }
    }


    ////////////// Stuff from MUSDBDispatcher below //////////////

    public void deliver(ServerUser user, MUSMovie mov, String[] args, MUSMessage msg, MUSMessage reply) {
        if (!m_enabled) {
            reply.m_msgContent = new LString("Database disabled");
            user.sendMessage(reply);
            return;
        }

        int msguserlevel = user.userLevel();

        // First batch of DBAdmin commands take no parameters
        if (args[1].equalsIgnoreCase("DBAdmin")) {
            if (args[2].equalsIgnoreCase("getUserCount")) {
                reply.m_msgContent = srvcmd_getUserCount();
                user.sendMessage(reply);
                return;
            } else if (args[2].equalsIgnoreCase("getUserNames")) {
                reply.m_msgContent = srvcmd_getUserNames();
                user.sendMessage(reply);
                return;
            } else if (args[2].equalsIgnoreCase("getApplicationCount")) {
                reply.m_msgContent = srvcmd_getApplicationCount();
                user.sendMessage(reply);
                return;
            } else if (args[2].equalsIgnoreCase("getApplicationNames")) {
                reply.m_msgContent = srvcmd_getApplicationNames();
                user.sendMessage(reply);
                return;
            } else if (args[2].equalsIgnoreCase("getAttributeCount")) {
                reply.m_msgContent = srvcmd_getAttributeCount();
                user.sendMessage(reply);
                return;
            } else if (args[2].equalsIgnoreCase("getAttributeNames")) {
                reply.m_msgContent = srvcmd_getAttributeNames();
                user.sendMessage(reply);
                return;
            } else if (args[2].equalsIgnoreCase("getBanned")) {
                reply.m_msgContent = srvcmd_getBanned();
                user.sendMessage(reply);
                return;
            }

        } // End first batch of DBAdmin commands

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

        if (args[1].equalsIgnoreCase("DBAdmin")) {
            try {
                if (args[2].equalsIgnoreCase("createUser")) {
                    LValue arguserid;
                    LValue argpasswd;
                    LValue arguserlevel;


                    try {
                        arguserid = plist.getElement(new LSymbol("userID"));
                        argpasswd = plist.getElement(new LSymbol("password"));
                    } catch (PropertyNotFoundException pnf) {
                        // Userid and password are needed
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    try {
                        arguserlevel = plist.getElement(new LSymbol("userlevel"));
                    } catch (PropertyNotFoundException pnf) {
                        // Userlevel is optional
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

                    // Can not create user with higher user level
                    if (msguserlevel < userlevelint.toInteger())
                        throw new MUSErrorCode(MUSErrorCode.NotPermittedWithUserLevel);

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
                        // Userid is needed
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    LList contlist = new LList();
                    MUSMovie.GetStringListFromContents(contlist, arguserid);

                    if (contlist.count() == 0)
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);

                    for (int e = 0; e < contlist.count(); e++) {
                        LString arguseridstr = (LString) contlist.getElementAt(e);

                        int userid;
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
                } else if (args[2].equalsIgnoreCase("createApplication")) {
                    LValue argappid;
                    LValue argdescription;

                    try {
                        argappid = plist.getElement(new LSymbol("application"));
                        argdescription = plist.getElement(new LSymbol("description"));
                    } catch (PropertyNotFoundException pnf) {
                        // Application and description are needed
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }


                    // Check types for arguments
                    if (argappid.getType() != LValue.vt_String ||
                            argdescription.getType() != LValue.vt_String) {
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    LString argappidstr = (LString) argappid;
                    LString argdescriptionstr = (LString) argdescription;


                    // Check for illegal characters in arguments
                    StringCharacterIterator sci = new StringCharacterIterator(argappidstr.toString());
                    for (char c = sci.first(); c != CharacterIterator.DONE; c = sci.next()) {
                        if ((c == '@') || (c == '#'))
                            throw new MUSErrorCode(MUSErrorCode.InvalidUserID);
                    }

                    boolean appcreated = createApplication(argappidstr.toString(), argdescriptionstr.toString());

                    if (appcreated) {
                        LPropList pl = new LPropList();
                        pl.addElement(new LSymbol("application"), argappid);
                        reply.m_msgContent = pl;
                    } else {
                        reply.m_errCode = MUSErrorCode.DatabaseDataRecordNotUnique;
                        LPropList pl = new LPropList();
                        pl.addElement(new LSymbol("application"), argappid);
                        reply.m_msgContent = pl;
                    }

                    user.sendMessage(reply);
                    return;

                } else if (args[2].equalsIgnoreCase("deleteApplication")) {
                    LValue argappid;
                    try {
                        argappid = plist.getElement(new LSymbol("application"));
                    } catch (PropertyNotFoundException pnf) {
                        // Application is needed
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    LList contlist = new LList();
                    MUSMovie.GetStringListFromContents(contlist, argappid);

                    if (contlist.count() == 0)
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);

                    for (int e = 0; e < contlist.count(); e++) {
                        LString argappidstr = (LString) contlist.getElementAt(e);

                        int appid;
                        try {
                            appid = getDBApplication(argappidstr.toString());
                        } catch (DBException dbe) {
                            MUSLog.Log("Invalid DB Command: could not get application " + argappidstr.toString(), MUSLog.kMsgErr);
                            throw new MUSErrorCode(MUSErrorCode.DatabaseRecordNotExists);
                        }

                        boolean appdeleted = deleteDBApplication(appid);

                        if (appdeleted) {
                            LPropList pl = new LPropList();
                            pl.addElement(new LSymbol("application"), argappidstr);
                            reply.m_msgContent = pl;
                            user.sendMessage(reply);
                        } else {
                            reply.m_errCode = MUSErrorCode.DatabaseError;
                            LPropList pl = new LPropList();
                            pl.addElement(new LSymbol("application"), argappidstr);
                            reply.m_msgContent = pl;
                            user.sendMessage(reply);
                        }
                    }
                    return;
                } else if (args[2].equalsIgnoreCase("declareAttribute")) {
                    LValue attributes;

                    try {
                        attributes = plist.getElement(new LSymbol("attribute"));
                    } catch (PropertyNotFoundException pnf) {
                        // Attribute is needed
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    LList contlist = new LList();
                    MUSAttribute.getSymbolListFromContents(contlist, attributes);

                    if (contlist.count() != 1) {
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    for (int e = 0; e < contlist.count(); e++) {
                        LString attidstr = (LString) contlist.getElementAt(e);
                        LSymbol attidsym = new LSymbol(attidstr.toString());


                        // Check for illegal characters in arguments
                        StringCharacterIterator sci = new StringCharacterIterator(attidsym.toString());
                        for (char c = sci.first(); c != CharacterIterator.DONE; c = sci.next()) {
                            if ((c == '@') || (c == '#'))
                                throw new MUSErrorCode(MUSErrorCode.InvalidMessageFormat);
                        }

                        boolean attcreated = declareAttribute(attidsym.toString());

                        if (attcreated) {
                            LPropList pl = new LPropList();
                            pl.addElement(new LSymbol("attribute"), attidsym);
                            reply.m_msgContent = pl;
                        } else {
                            reply.m_errCode = MUSErrorCode.DatabaseDataRecordNotUnique;
                            LPropList pl = new LPropList();
                            pl.addElement(new LSymbol("attribute"), attidsym);
                            reply.m_msgContent = pl;
                        }

                        user.sendMessage(reply);
                    }
                    return;

                } else if (args[2].equalsIgnoreCase("deleteAttribute")) {
                    LValue attributes;
                    try {
                        attributes = plist.getElement(new LSymbol("attribute"));
                    } catch (PropertyNotFoundException pnf) {
                        // Application is needed
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    LList contlist = new LList();
                    MUSAttribute.getSymbolListFromContents(contlist, attributes);

                    if (contlist.count() == 0)
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);

                    for (int e = 0; e < contlist.count(); e++) {
                        LSymbol attidsym = (LSymbol) contlist.getElementAt(e);

                        int attid;
                        try {
                            attid = getAttribute(attidsym.toString());
                        } catch (DBException dbe) {
                            MUSLog.Log("Invalid DB Command: could not get attribute " + attidsym.toString(), MUSLog.kMsgErr);
                            throw new MUSErrorCode(MUSErrorCode.DatabaseRecordNotExists);
                        }

                        boolean attdeleted = deleteAttribute(attid);

                        if (attdeleted) {
                            LPropList pl = new LPropList();
                            pl.addElement(new LSymbol("attribute"), attidsym);
                            reply.m_msgContent = pl;
                            user.sendMessage(reply);
                        } else {
                            reply.m_errCode = MUSErrorCode.DatabaseError;
                            LPropList pl = new LPropList();
                            pl.addElement(new LSymbol("attribute"), attidsym);
                            reply.m_msgContent = pl;
                            user.sendMessage(reply);
                        }
                    }
                    return;
                } else if (args[2].equalsIgnoreCase("createApplicationData")) {
                    LValue argappid;
                    LValue argattributes;

                    try {
                        argappid = plist.getElement(new LSymbol("application"));
                        argattributes = plist.getElement(new LSymbol("attribute"));
                    } catch (PropertyNotFoundException pnf) {
                        // Application and description are needed
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    // Check types for arguments
                    if (argappid.getType() != LValue.vt_String ||
                            argattributes.getType() != LValue.vt_PropList) {
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    LString argappidstr = (LString) argappid;
                    LList attrlist = new LList();
                    LList vallist = new LList();
                    MUSAttribute.getSetAttributeListsFromContents(attrlist, vallist, argattributes);

                    LList intattrlist = new LList();

                    // This will thrown a MUSErrorCode
                    validateAttributesList(attrlist, intattrlist, user, 0);

                    if (attrlist.count() == 0)
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);

                    // @TODO: maybe check if at least one attribute is a string/number here...

                    int appid;
                    try {
                        appid = getDBApplication(argappidstr.toString());
                    } catch (DBException dbe) {
                        MUSLog.Log("Invalid DB Command: could not get application " + argappidstr.toString(), MUSLog.kMsgErr);
                        throw new MUSErrorCode(MUSErrorCode.DatabaseRecordNotExists);
                    }

                    int appdatacreated = createApplicationData(appid);

                    if (appdatacreated != -1) {
                        setDBObjectAttributes(appdatacreated, intattrlist, vallist);
                        LPropList pl = new LPropList();
                        pl.addElement(new LSymbol("application"), argappid);
                        reply.m_msgContent = pl;
                    } else {
                        reply.m_errCode = MUSErrorCode.DatabaseError;
                        LPropList pl = new LPropList();
                        pl.addElement(new LSymbol("application"), argappid);
                        reply.m_msgContent = pl;
                    }

                    user.sendMessage(reply);
                    return;

                } else if (args[2].equalsIgnoreCase("deleteApplicationData")) {
                    LList matchedappdataidlist = getMatchedApplicationDataList(plist);

                    for (int a = 0; a < matchedappdataidlist.count(); a++) {
                        LInteger appint = (LInteger) matchedappdataidlist.getElementAt(a);
                        deleteDBApplicationData(appint.toInteger());
                    }
                    reply.m_msgContent = msgcont;
                    user.sendMessage(reply);
                    return;

                } else if (args[2].equalsIgnoreCase("ban")) {
                    LValue argentry;
                    LValue argseconds;
                    try {
                        argentry = plist.getElement(new LSymbol("user"));
                        argseconds = plist.getElement(new LSymbol("timeToBan"));
                    } catch (PropertyNotFoundException pnf) {
                        // Entry and time are needed
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    // Check types for arguments
                    if (argentry.getType() != LValue.vt_String ||
                            argseconds.getType() != LValue.vt_Integer) {
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    LString argentrystr = (LString) argentry;
                    LInteger argsecondsint = (LInteger) argseconds;

                    addBannedEntry(argentrystr.toString(), argsecondsint.toInteger());

                    LPropList pl = new LPropList();
                    pl.addElement(new LSymbol("user"), argentry);
                    reply.m_msgContent = pl;

                    user.sendMessage(reply);
                    return;
                } else if (args[2].equalsIgnoreCase("revokeBan")) {
                    LValue argentry;
                    try {
                        argentry = plist.getElement(new LSymbol("user"));
                    } catch (PropertyNotFoundException pnf) {
                        // Entry is needed
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    // Check types for arguments
                    if (argentry.getType() != LValue.vt_String) {
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    LString argentrystr = (LString) argentry;

                    removeBannedEntry(argentrystr.toString());

                    LPropList pl = new LPropList();
                    pl.addElement(new LSymbol("user"), argentry);
                    reply.m_msgContent = pl;

                    user.sendMessage(reply);
                    return;
                }


            } catch (MUSErrorCode err) {
                reply.m_errCode = err.m_errCode;
                reply.m_msgContent = new LInteger(0);
                user.sendMessage(reply);
                return;
            }
        } // End DBAdmin commands

        else if (args[1].equalsIgnoreCase("DBApplication")) {

            // LValue attributes = new LValue();

            try {
                if (args[2].equalsIgnoreCase("getApplicationData")) {
                    LList matchedappdataidlist = getMatchedApplicationDataList(plist);

                    LList replylist = new LList();
                    for (int a = 0; a < matchedappdataidlist.count(); a++) {
                        LInteger appint = (LInteger) matchedappdataidlist.getElementAt(a);
                        replylist.addElement(getApplicationDataAsL(appint.toInteger()));
                    }
                    reply.m_msgContent = replylist;
                    user.sendMessage(reply);
                    return;
                } else if (args[2].equalsIgnoreCase("getAttribute") ||
                        args[2].equalsIgnoreCase("setAttribute") ||
                        args[2].equalsIgnoreCase("deleteAttribute") ||
                        args[2].equalsIgnoreCase("getAttributeNames")) {

                    LPropList cl = new LPropList();
                    LValue argappid;
                    // LValue argattributes = new LValue();
                    LString argappidstr = new LString();
                    LList appidlist = new LList();

                    try {
                        try {

                            argappid = plist.getElement(new LSymbol("application"));
                        } catch (PropertyNotFoundException pnf) {
                            throw new MUSErrorCode(MUSErrorCode.BadParameter);
                        }

                        MUSMovie.GetStringListFromContents(appidlist, argappid);

                        if (appidlist.count() == 0)
                            throw new MUSErrorCode(MUSErrorCode.BadParameter);

                        for (int a = 0; a < appidlist.count(); a++) {
                            try {
                                argappidstr = (LString) appidlist.getElementAt(a);

                                int appid;
                                try {
                                    appid = getDBApplication(argappidstr.toString());
                                } catch (DBException dbe) {
                                    MUSLog.Log("Invalid DB Command: could not get application " + argappidstr.toString(), MUSLog.kMsgErr);
                                    throw new MUSErrorCode(MUSErrorCode.InvalidMovieID);
                                }

                                LValue ret = handleAttributeMessage(appid, reply, args[2], plist, user);

                                cl.addElement(new LString(argappidstr.toString()), ret);
                            } catch (MUSErrorCode err) {
                                LPropList tl = new LPropList();
                                tl.addElement(new LSymbol("errorCode"), new LInteger(err.m_errCode));
                                reply.m_errCode = MUSErrorCode.MessageContainsErrorInfo;
                                cl.addElement(new LString(argappidstr.toString()), tl);
                            } catch (DBException err) {
                                LPropList tl = new LPropList();
                                tl.addElement(new LSymbol("errorCode"), new LInteger(MUSErrorCode.DatabaseError));
                                reply.m_errCode = MUSErrorCode.MessageContainsErrorInfo;
                                cl.addElement(new LString(argappidstr.toString()), tl);
                            }
                        }

                    } catch (MUSErrorCode err) {
                        LPropList tl = new LPropList();
                        tl.addElement(new LSymbol("errorCode"), new LInteger(err.m_errCode));
                        reply.m_errCode = MUSErrorCode.MessageContainsErrorInfo;
                        cl.addElement(new LString(argappidstr.toString()), tl);
                    }

                    reply.m_msgContent = cl;

                    user.sendMessage(reply);
                    return;

                }

            } catch (MUSErrorCode err) {
                reply.m_errCode = err.m_errCode;
                reply.m_msgContent = msgcont;
                user.sendMessage(reply);
                return;
            }
        }// End DBApplicationCommands

        else if (args[1].equalsIgnoreCase("DBUser")) {
            // LValue attributes = new LValue();

            if (args[2].equalsIgnoreCase("getAttribute") ||
                    args[2].equalsIgnoreCase("setAttribute") ||
                    args[2].equalsIgnoreCase("deleteAttribute") ||
                    args[2].equalsIgnoreCase("getAttributeNames")) {

                LPropList cl = new LPropList();
                LValue arguserid;
                // LValue argattributes = new LValue();
                LString arguseridstr = new LString();
                LList useridlist = new LList();

                try {
                    try {

                        arguserid = plist.getElement(new LSymbol("userID"));
                    } catch (PropertyNotFoundException pnf) {
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    MUSMovie.GetStringListFromContents(useridlist, arguserid);

                    if (useridlist.count() == 0)
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);

                    for (int a = 0; a < useridlist.count(); a++) {
                        try {
                            arguseridstr = (LString) useridlist.getElementAt(a);

                            int userid;
                            try {
                                userid = getDBUser(arguseridstr.toString());
                            } catch (UserNotFoundException dbe) {
                                MUSLog.Log("Invalid DB Command: could not get user " + arguseridstr.toString(), MUSLog.kMsgErr);
                                throw new MUSErrorCode(MUSErrorCode.InvalidUserID);
                            }

                            LValue ret = handleAttributeMessage(userid, reply, args[2], plist, user);

                            cl.addElement(new LString(arguseridstr.toString()), ret);
                        } catch (MUSErrorCode err) {
                            LPropList tl = new LPropList();
                            tl.addElement(new LSymbol("errorCode"), new LInteger(err.m_errCode));
                            reply.m_errCode = MUSErrorCode.MessageContainsErrorInfo;
                            cl.addElement(new LString(arguseridstr.toString()), tl);
                        } catch (DBException err) {
                            LPropList tl = new LPropList();
                            tl.addElement(new LSymbol("errorCode"), new LInteger(MUSErrorCode.DatabaseError));
                            reply.m_errCode = MUSErrorCode.MessageContainsErrorInfo;
                            cl.addElement(new LString(arguseridstr.toString()), tl);
                        }
                    }


                } catch (MUSErrorCode err) {
                    LPropList tl = new LPropList();
                    tl.addElement(new LSymbol("errorCode"), new LInteger(err.m_errCode));
                    reply.m_errCode = MUSErrorCode.MessageContainsErrorInfo;
                    cl.addElement(new LString(arguseridstr.toString()), tl);
                }

                reply.m_msgContent = cl;

                user.sendMessage(reply);
                return;

            }

        } // End DBUser

        else if (args[1].equalsIgnoreCase("DBPlayer")) {

            // LValue attributes = new LValue();

            try {

                if (args[2].equalsIgnoreCase("getAttribute") ||
                        args[2].equalsIgnoreCase("setAttribute") ||
                        args[2].equalsIgnoreCase("deleteAttribute") ||
                        args[2].equalsIgnoreCase("getAttributeNames")) {

                    LPropList cl = new LPropList();
                    LValue arguserid;
                    LValue argappid;
                    // LValue argattributes = new LValue();
                    LString arguseridstr = new LString();
                    LString argappidstr;


                    try {
                        arguserid = plist.getElement(new LSymbol("userID"));
                        argappid = plist.getElement(new LSymbol("application"));
                    } catch (PropertyNotFoundException pnf) {
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);
                    }

                    // Verify parameters
                    if (arguserid.getType() != LValue.vt_String ||
                            argappid.getType() != LValue.vt_String)
                        throw new MUSErrorCode(MUSErrorCode.BadParameter);


                    try {
                        arguseridstr = (LString) arguserid;
                        argappidstr = (LString) argappid;

                        int userid;
                        try {
                            userid = getDBUser(arguseridstr.toString());
                        } catch (UserNotFoundException dbe) {
                            MUSLog.Log("Invalid DB Command: could not get user " + arguseridstr.toString(), MUSLog.kMsgErr);
                            throw new MUSErrorCode(MUSErrorCode.InvalidUserID);
                        }

                        int appid;
                        try {
                            appid = getDBApplication(argappidstr.toString());
                        } catch (DBException dbe) {
                            MUSLog.Log("Invalid DB Command: could not get application " + argappidstr.toString(), MUSLog.kMsgErr);
                            throw new MUSErrorCode(MUSErrorCode.InvalidMovieID);
                        }

                        int playerid;
                        try {
                            playerid = getDBPlayer(userid, appid);
                        } catch (DBException dbe) {
                            MUSLog.Log("Invalid DB Command: could not get player", MUSLog.kMsgErr);
                            throw new MUSErrorCode(MUSErrorCode.DatabaseError);
                        } catch (PlayerNotFoundException pnf) {
                            // If settingattribute then create player
                            if (args[2].equalsIgnoreCase("setAttribute"))
                                if (!createDBPlayer(userid, appid))
                                    throw new MUSErrorCode(MUSErrorCode.DatabaseError);

                            try {
                                playerid = getDBPlayer(userid, appid);
                            } catch (PlayerNotFoundException pnf2) {
                                // Player still not created, problem with Database
                                throw new MUSErrorCode(MUSErrorCode.DatabaseError);
                            }
                        }

                        LValue ret = handleAttributeMessage(playerid, reply, args[2], plist, user);

                        cl.addElement(new LString(arguseridstr.toString()), ret);
                    } catch (MUSErrorCode err) {
                        LPropList tl = new LPropList();
                        tl.addElement(new LSymbol("errorCode"), new LInteger(err.m_errCode));
                        reply.m_errCode = MUSErrorCode.MessageContainsErrorInfo;
                        cl.addElement(new LString(arguseridstr.toString()), tl);
                    } catch (DBException err) {
                        LPropList tl = new LPropList();
                        tl.addElement(new LSymbol("errorCode"), new LInteger(MUSErrorCode.DatabaseError));
                        reply.m_errCode = MUSErrorCode.MessageContainsErrorInfo;
                        cl.addElement(new LString(arguseridstr.toString()), tl);
                    }
                    reply.m_msgContent = cl;
                    user.sendMessage(reply);
                    return;
                }


            } catch (MUSErrorCode err) {
                reply.m_errCode = err.m_errCode;
                reply.m_msgContent = msgcont;
                user.sendMessage(reply);
                return;
            }


        } // End DBPlayer
    }

    public LValue handleAttributeMessage(int ownerid, MUSMessage reply, String attrcommand, LPropList msgattributes, ServerUser user) throws DBException, MUSErrorCode {
        if (attrcommand.equalsIgnoreCase("getAttributeNames"))
            return getAttributeNames(ownerid);

        LValue attributes;
        // Other commands require a valid #attribute field, SMUS reacts creating one
        try {
            attributes = msgattributes.getElement(new LSymbol("attribute"));
        } catch (PropertyNotFoundException pnf) {
            attributes = new LList();
        }

        LPropList al = new LPropList();

        // Handle setAtribute call
        if (attrcommand.equalsIgnoreCase("setAttribute")) {
            LList attrlist = new LList();
            LList vallist = new LList();
            MUSAttribute.getSetAttributeListsFromContents(attrlist, vallist, attributes);

            LList checkattrlist = new LList();

            validateAttributesList(attrlist, checkattrlist, user, 0);

            if (attrlist.count() == 0)
                throw new MUSErrorCode(MUSErrorCode.BadParameter);

            // Validate optional lastUpdateTime property
            LValue msgupdatetime;
            LSymbol lutsym = new LSymbol("lastUpdateTime");
            LValue tval = new LValue();
            try {
                msgupdatetime = msgattributes.getElement(lutsym);
                try {
                    MUSAttribute att = getAttribute(ownerid, lutsym.toString());
                    tval = att.get();
                } catch (AttributeNotFoundException anf) {
                    // Should not happen, lastUpdateTime is always present
                    MUSLog.Log("DBError: could not locate lastUpdateTime attribute", MUSLog.kDB);
                }
                if (msgupdatetime.getType() == LValue.vt_String) {
                    LString msgtime = (LString) msgupdatetime;
                    LString curtime = (LString) tval;
                    if (!msgtime.toString().equals(curtime.toString())) {
                        // Not OK, return error and indicate this in the reply
                        reply.m_errCode = MUSErrorCode.MessageContainsErrorInfo;
                        al.addElement(new LSymbol("errorCode"), new LInteger(MUSErrorCode.DataConcurrencyError));
                        al.addElement(lutsym, tval);
                        return al;
                    }
                } else {
                    // Not a valid time string specified, return error and indicate this in the reply
                    reply.m_errCode = MUSErrorCode.MessageContainsErrorInfo;
                    al.addElement(new LSymbol("errorCode"), new LInteger(MUSErrorCode.DataConcurrencyError));
                    al.addElement(lutsym, tval);
                    return al;
                }

            } catch (PropertyNotFoundException pnf) {
                // OK, go on
                // Property not found while checking for lastupdatetime
            }

            for (int e = 0; e < attrlist.count(); e++) {
                LSymbol attname = (LSymbol) attrlist.getElementAt(e);
                LValue attvalue = vallist.getElementAt(e);
                MUSAttribute att;
                try {
                    att = getAttribute(ownerid, attname.toString());
                    att.set(attvalue);
                    setAttribute(ownerid, att);
                } catch (AttributeNotFoundException anf) {
                    att = new MUSAttribute(attname, attvalue);
                    setAttribute(ownerid, att);
                }
            }
            // Update lastUpdateTime and report it
            tval = (LString) MUSAttribute.getTime();
            try {
                MUSAttribute att = getAttribute(ownerid, lutsym.toString());
                att.set(tval);
                setAttribute(ownerid, att);
            } catch (AttributeNotFoundException anf) {
                // Should not happen, lastUpdateTime is always present
                MUSLog.Log("DB Error: could not locate lastUpdateTime attribute", MUSLog.kDB);
            }
            al.addElement(lutsym, tval);
            return al;
        }

        // Process get/delete here

        LList attrlist = new LList();
        MUSAttribute.getSymbolListFromContents(attrlist, attributes);

        LList checkattrlist = new LList();

        validateAttributesList(attrlist, checkattrlist, user, 1);

        if (attrlist.count() == 0)
            throw new MUSErrorCode(MUSErrorCode.BadParameter);


        if (attrcommand.equalsIgnoreCase("getAttribute")) {
            // Add lastUpdateTime anyway
            attrlist.addElement(new LSymbol("lastUpdateTime"));

            for (int e = 0; e < attrlist.count(); e++) {
                try {
                    LSymbol attname = (LSymbol) attrlist.getElementAt(e);
                    MUSAttribute att = getAttribute(ownerid, attname.toString());
                    LValue attvalue = att.get();
                    al.addElement(attname, attvalue);

                } catch (AttributeNotFoundException anf) {
                }
            }
            return al;

        } else if (attrcommand.equalsIgnoreCase("deleteAttribute")) {
            for (int e = 0; e < attrlist.count(); e++) {
                try {
                    LSymbol attname = (LSymbol) attrlist.getElementAt(e);
                    // lastupdatetime is protected
                    if (!attname.toString().equalsIgnoreCase("lastUpdateTime")) {
                        MUSAttribute att = getAttribute(ownerid, attname.toString());
                        // LValue attvalue = att.get();
                        removeAttribute(ownerid, att);
                    }
                } catch (AttributeNotFoundException anf) {
                }
            }
            return al;

        }

        return new LValue();

    }

    public LList getMatchedApplicationDataList(LPropList plist) throws MUSErrorCode {
        LValue argapplication;
        LValue argattribute;
        String searchtype = "None";
        boolean hasValidSearchArg = false;
        LValue argsearch1 = new LValue();
        LValue argsearch2 = new LValue();

        try {
            argapplication = plist.getElement(new LSymbol("application"));
            argattribute = plist.getElement(new LSymbol("attribute"));
        } catch (PropertyNotFoundException pnf) {
            // Application and attribute are needed
            throw new MUSErrorCode(MUSErrorCode.BadParameter);
        }

        // Check types for compulsory arguments
        if (argapplication.getType() != LValue.vt_String ||
                argattribute.getType() != LValue.vt_Symbol) {
            throw new MUSErrorCode(MUSErrorCode.BadParameter);
        }

        LString argapplicationstr = (LString) argapplication;
        LSymbol argattributesym = (LSymbol) argattribute;
        // Now try to get #text or #number

        try {
            argsearch1 = plist.getElement(new LSymbol("text"));
            if (argsearch1.getType() != LValue.vt_String) {
                throw new MUSErrorCode(MUSErrorCode.BadParameter);
            }
            hasValidSearchArg = true;
            searchtype = "Text";
        } catch (PropertyNotFoundException pnf) {
            // OK, next check
        }

        if (!hasValidSearchArg) {
            try {
                argsearch1 = plist.getElement(new LSymbol("number"));
                if (argsearch1.getType() != LValue.vt_Integer) {
                    throw new MUSErrorCode(MUSErrorCode.BadParameter);
                }
                hasValidSearchArg = true;
                searchtype = "Number";
            } catch (PropertyNotFoundException pnf) {
                // OK, next check
            }
        }

        if (!hasValidSearchArg) {
            try {
                argsearch1 = plist.getElement(new LSymbol("lowNum"));
                if (argsearch1.getType() != LValue.vt_Integer) {
                    throw new MUSErrorCode(MUSErrorCode.BadParameter);
                }
                argsearch2 = plist.getElement(new LSymbol("highNum"));
                if (argsearch2.getType() != LValue.vt_Integer) {
                    throw new MUSErrorCode(MUSErrorCode.BadParameter);
                }
                hasValidSearchArg = true;
                searchtype = "Range";
            } catch (PropertyNotFoundException pnf) {
                // All checks failed
            }
        }

        if (!hasValidSearchArg) {
            try {
                argsearch1 = plist.getElement(new LSymbol("all"));
                hasValidSearchArg = true;
                searchtype = "All";
            } catch (PropertyNotFoundException pnf) {
                // All checks failed
            }
        }

        if (!hasValidSearchArg) {
            MUSLog.Log("Invalid DB Command: Bad search attribute in getApplicationData", MUSLog.kMsgErr);
            throw new MUSErrorCode(MUSErrorCode.BadParameter);
        }

        int appid;
        try {
            appid = getDBApplication(argapplicationstr.toString());
        } catch (DBException dbe) {
            MUSLog.Log("Invalid DB Command: could not get application " + argapplicationstr.toString(), MUSLog.kDB);
            throw new MUSErrorCode(MUSErrorCode.DatabaseRecordNotExists);
        }

        int attrid;
        try {
            attrid = getAttribute(argattributesym.toString());
        } catch (DBException dbe) {
            MUSLog.Log("Invalid DB Command: could not get attribute " + argattributesym.toString(), MUSLog.kDB);
            throw new MUSErrorCode(MUSErrorCode.DatabaseRecordNotExists);
        }

        LList appdataidlist = getApplicationDataListFromApplicationID(appid);
        LList matchedappdataidlist = new LList();

        if (appdataidlist.count() == 0) {
            throw new MUSErrorCode(MUSErrorCode.DatabaseRecordNotExists);
        }

        if (searchtype.equalsIgnoreCase("text")) {
            matchedappdataidlist = searchApplicationDataForText(appid, attrid, appdataidlist, argsearch1);
        } else if (searchtype.equalsIgnoreCase("number")) {
            matchedappdataidlist = searchApplicationDataForNumber(appid, attrid, appdataidlist, argsearch1);
        } else if (searchtype.equalsIgnoreCase("range")) {
            matchedappdataidlist = searchApplicationDataForRange(appid, attrid, appdataidlist, argsearch1, argsearch2);
        } else if (searchtype.equalsIgnoreCase("all")) {
            matchedappdataidlist = appdataidlist;
        }

        return matchedappdataidlist;

    }

    public void validateAttributesList(LValue attributes, LList intattrlist, ServerUser user, int accessmode) throws MUSErrorCode {
        // Accessmode mode mask
        // 0 - set
        // 1 - get

        LList attrlist = new LList();
        MUSAttribute.getSymbolListFromContents(attrlist, attributes);
        for (int e = 0; e < attrlist.count(); e++) {

            LSymbol attname = (LSymbol) attrlist.getElementAt(e);
            int attid;
            try {
                attid = getAttribute(attname.toString());
                if (attid < 8) {
                    // System property, validate userlevel
                    if (accessmode == 1) { // Get is less restrictive
                        if (!canGetAttribute(attname.toString(), user))
                            throw new MUSErrorCode(MUSErrorCode.NotPermittedWithUserLevel);
                    } else {
                        if (!canSetAttribute(attname.toString(), user))
                            throw new MUSErrorCode(MUSErrorCode.NotPermittedWithUserLevel);
                    }
                }
                intattrlist.addElement(new LInteger(attid));
            } catch (DBException dbe) {
                MUSLog.Log("Invalid DB Command: could not get attribute " + attname.toString(), MUSLog.kDB);
                throw new MUSErrorCode(MUSErrorCode.DatabaseRecordNotExists);
            }

        }
    }

    public boolean canSetAttribute(String attname, ServerUser user) {
        // All system attributes are protected

        // Some can not be set via scripting, no matter the user level
        if (attname.equalsIgnoreCase("creationtime"))
            return false;

        if (attname.equalsIgnoreCase("lastupdatetime"))
            return false;

        if (attname.equalsIgnoreCase("lastlogintime"))
            return false;


        MUSMovie mov = (MUSMovie) user.serverMovie();
        int reqlevel = mov.getRequiredUserLevel("System.DBUser.SetSystemAttribute");

        if (user.userLevel() >= reqlevel)
            return true;
        else
            return false;
    }

    public boolean canGetAttribute(String attname, ServerUser user) {
        boolean isProtectedAttribute = false;

        if (attname.equalsIgnoreCase("password") || attname.equalsIgnoreCase("userlevel"))
            isProtectedAttribute = true;

        if (!isProtectedAttribute)
            return true;

        MUSMovie mov = (MUSMovie) user.serverMovie();
        int reqlevel = mov.getRequiredUserLevel("System.DBUser.GetSystemAttribute");

        if (user.userLevel() >= reqlevel)
            return true;
        else
            return false;

    }

}
