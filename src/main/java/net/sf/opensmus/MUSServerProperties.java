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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

public class MUSServerProperties {

    public static final String OPENSMUS_CONFIGFILELOCATION = "OpenSMUS.cfg";
    
    public static final String DEFAULT_LOGFILENAME = "serverlog.txt";
    
	public Properties m_props = new Properties();

    public MUSServerProperties() {

    	this.fillInDefaults();
    	this.loadPropertiesFromConfigurationFile();

        // Adjust the encryption key if it is less than 20 bytes
        // according to the SMUS Protocol docs
        String enckey = m_props.getProperty("EncryptionKey");
        if (enckey.length() < 20) {
            m_props.put("EncryptionKey", enckey + "IPAddress resolution");
            // MUSLog.Log("Encryption key too short, adjusted", MUSLog.kSys);
        }
    }    
    
    public String getProperty(String prop) {
        return m_props.getProperty(prop);
    }

    public String[] getStringListProperty(String prop) {

        String data = m_props.getProperty(prop);
        if (data == null) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(data, ";");
        String[] result = new String[st.countTokens()];

        int i = 0;
        while (st.hasMoreTokens()) {
            result[i++] = st.nextToken();
        }
        return result;
    }

    public int[] getIntListProperty(String prop) {
        try {
            StringTokenizer st = new StringTokenizer(m_props.getProperty(prop), ";");
            int[] result = new int[st.countTokens()];
            int i = 0;
            while (st.hasMoreTokens()) {
                result[i++] = Integer.parseInt(st.nextToken());
            }
            return result;
        } catch (NumberFormatException e) {
            MUSLog.Log("Bad or missing property value in configuration file: " + prop, MUSLog.kSys);
            return new int[0];
        }
    }

    public int getIntProperty(String prop) {
        try {
            return Integer.parseInt(m_props.getProperty(prop));
        } catch (NumberFormatException e) {
            MUSLog.Log("Bad or missing property value in configuration file: " + prop, MUSLog.kSys);
            return 0;
        }
    }

    public static String parseIPAddress(String fullip) {
        return fullip.substring(0, fullip.indexOf(":"));
    }

    public static int parseIPPort(String fullip) {
        try {
            String ipportstr = fullip.substring(fullip.indexOf(":") + 1, fullip.length());
            return Integer.parseInt(ipportstr);
        } catch (NumberFormatException e) {
            MUSLog.Log("Bad ipaddress specified: " + fullip, MUSLog.kSys);
            return 0;
        }
    }

   private void fillInDefaults() {
    	
        m_props.put("ServerOwnerName", "default");
        m_props.put("ServerPort", "1626");
        m_props.put("ServerIPAddress", "default");
        m_props.put("UDPServerAddress", "default");
        m_props.put("EnableUDP", "0");
        m_props.put("EnableServerSideScripting", "0");
        m_props.put("MaxMessageSize", "16384");
        m_props.put("MaxUDPPacket", "1024");
        m_props.put("tcpNoDelay", "1");
        m_props.put("soLingerTime", "-1");
        m_props.put("ConnectionLimit", "1000");
        m_props.put("EncryptionKey", "IPAddress resolution");
        m_props.put("LogFileName", DEFAULT_LOGFILENAME);
        m_props.put("ClearLogAtStartup", "1");
        m_props.put("ServerOutputToLogFile", "1");
        m_props.put("AllowMovies", "default");
        m_props.put("MoviePathName", "default");
        m_props.put("IdleTimeOut", "600");
        m_props.put("ServerStatusReportInterval", "300");

        m_props.put("StartupMovies", "none");

        m_props.put("LogServerEvents", "1");
        m_props.put("LogMovieEvents", "1");
        m_props.put("LogGroupEvents", "1");
        m_props.put("LogUserEvents", "1");
        m_props.put("LogDBEvents", "1");
        m_props.put("LogInvalidMsgEvents", "0");
        m_props.put("LogScriptEvents", "0");
        m_props.put("LogDebugInformation", "0");
        m_props.put("LogDebugExtInformation", "0");

        m_props.put("dumpLoginMessage", "0");

        m_props.put("ThreadPoolSize", "16");
        m_props.put("MaxUserMemorySize", "0");
        m_props.put("MaxTotalMemorySize", "0");

        m_props.put("MaxLoginMsgQueue", "5");
        m_props.put("MaxMsgQueueWait", "5");

        m_props.put("MaxLoginWait", "15");
        m_props.put("MinLoginPeriod", "0");

        m_props.put("DropUserWhenReconnectingFromSameIP", "1");

        m_props.put("Authentication", "UserRecordOptional");
        m_props.put("DefaultUserLevel", "20");
        m_props.put("MessagingUserLevel", "0");
        m_props.put("MessagingAllUserLevel", "80");
        m_props.put("DefaultUserStatus", "20");
        m_props.put("UserLevel.System.Server.GetMovies", "20");
        m_props.put("UserLevel.System.Server.GetMovieCount", "20");
        m_props.put("UserLevel.System.Server.GetTime", "20");
        m_props.put("UserLevel.System.Server.GetVersion", "20");
        m_props.put("UserLevel.System.Server.Restart", "100");
        m_props.put("UserLevel.System.Server.Shutdown", "100");
        m_props.put("UserLevel.System.Server.Enable", "80");
        m_props.put("UserLevel.System.Server.Disable", "80");
        m_props.put("UserLevel.System.Server.DisconnectAll", "80");
        m_props.put("UserLevel.System.Server.SendEmail", "80");
        m_props.put("UserLevel.System.Movie.Enable", "80");
        m_props.put("UserLevel.System.Movie.Disable", "80");
        m_props.put("UserLevel.System.Movie.Delete", "80");
        m_props.put("UserLevel.System.Movie.GetGroups", "20");
        m_props.put("UserLevel.System.Movie.GetUserCount", "20");
        m_props.put("UserLevel.System.Movie.GetGroupCount", "20");

        m_props.put("UserLevel.System.Movie.GetScriptCount", "20");
        m_props.put("UserLevel.System.Movie.ReloadAllScripts", "80");
        m_props.put("UserLevel.System.Movie.ReloadScript", "80");
        m_props.put("UserLevel.System.Movie.DeleteScript", "80");

        m_props.put("UserLevel.System.Group.GetUsers", "20");
        m_props.put("UserLevel.System.Group.GetUserCount", "20");
        m_props.put("UserLevel.System.Group.CreateUniqueName", "20");
        m_props.put("UserLevel.System.Group.Join", "20");
        m_props.put("UserLevel.System.Group.Leave", "20");
        m_props.put("UserLevel.System.Group.Enable", "80");
        m_props.put("UserLevel.System.Group.Disable", "80");
        m_props.put("UserLevel.System.Group.Delete", "80");
        m_props.put("UserLevel.System.User.Delete", "80");
        m_props.put("UserLevel.System.User.GetGroupCount", "20");
        m_props.put("UserLevel.System.User.GetGroups", "20");
        m_props.put("UserLevel.System.User.GetAddress", "80");
        m_props.put("UserLevel.System.User.ChangeMovie", "20");
        m_props.put("UserLevel.System.Group.SetAttribute", "20");
        m_props.put("UserLevel.System.Group.GetAttribute", "20");
        m_props.put("UserLevel.System.Group.DeleteAttribute", "20");
        m_props.put("UserLevel.System.Group.GetAttributeNames", "20");
        m_props.put("UserLevel.System.DBAdmin.CreateUser", "80");
        m_props.put("UserLevel.System.DBAdmin.DeleteUser", "80");
        m_props.put("UserLevel.System.DBAdmin.CreateApplication", "80");
        m_props.put("UserLevel.System.DBAdmin.DeleteApplication", "80");
        m_props.put("UserLevel.System.DBAdmin.CreateApplicationData", "20");
        m_props.put("UserLevel.System.DBAdmin.DeleteApplicationData", "20");
        m_props.put("UserLevel.System.DBAdmin.GetUserCount", "80");
        m_props.put("UserLevel.System.DBAdmin.GetUserNames", "80");
        m_props.put("UserLevel.System.DBAdmin.DeclareAttribute", "80");
        m_props.put("UserLevel.System.DBAdmin.DeleteAttribute", "80");
        m_props.put("UserLevel.System.DBAdmin.Ban", "80");
        m_props.put("UserLevel.System.DBAdmin.RevokeBan", "80");
        m_props.put("UserLevel.System.DBAdmin.GetApplicationCount", "80");
        m_props.put("UserLevel.System.DBAdmin.GetApplicationNames", "80");
        m_props.put("UserLevel.System.DBAdmin.GetAttributeCount", "80");
        m_props.put("UserLevel.System.DBAdmin.GetAttributeNames", "80");
        m_props.put("UserLevel.System.DBAdmin.GetBanned", "80");

        m_props.put("UserLevel.System.DBUser.SetAttribute", "20");
        m_props.put("UserLevel.System.DBUser.GetAttribute", "20");
        m_props.put("UserLevel.System.DBUser.SetSystemAttribute", "100");
        m_props.put("UserLevel.System.DBUser.GetSystemAttribute", "100");
        m_props.put("UserLevel.System.DBUser.GetAttributeNames", "20");
        m_props.put("UserLevel.System.DBUser.DeleteAttribute", "20");
        m_props.put("UserLevel.System.DBPlayer.SetAttribute", "20");
        m_props.put("UserLevel.System.DBPlayer.GetAttribute", "20");
        m_props.put("UserLevel.System.DBPlayer.GetAttributeNames", "20");
        m_props.put("UserLevel.System.DBPlayer.DeleteAttribute", "20");
        m_props.put("UserLevel.System.DBApplication.SetAttribute", "20");
        m_props.put("UserLevel.System.DBApplication.GetAttribute", "20");
        m_props.put("UserLevel.System.DBApplication.GetAttributeNames", "20");
        m_props.put("UserLevel.System.DBApplication.DeleteAttribute", "20");
        m_props.put("UserLevel.System.DBApplication.GetApplicationData", "20");

        m_props.put("UserLevel.System.SQL.executeUpdate", "20");
        m_props.put("UserLevel.System.SQL.executeQuery", "20");
        m_props.put("UserLevel.System.SQL.connect", "80");
        m_props.put("UserLevel.System.SQL.disconnect", "80");

        m_props.put("EnableDatabaseCommands", "1");
        m_props.put("CreateUser", "default");
        m_props.put("DeclareAttribute", "default");

        m_props.put("MUDatabaseSQLBackend", "hsqldb");
        m_props.put("MUDatabaseDriver", "org.hsqldb.jdbcDriver");
        m_props.put("MUDatabaseURL", "jdbc:hsqldb:OpenSMUSDB");
        m_props.put("MUDatabaseUsername", "sa");
        m_props.put("MUDatabasePassword", "");

        m_props.put("SQLDatabaseDriver", "org.hsqldb.jdbcDriver");
        m_props.put("SQLBackend", "hsqldb");
        m_props.put("SQLDatabaseURL", "jdbc:hsqldb:OpenSMUSSQL");
        m_props.put("SQLDatabaseUsername", "sa");
        m_props.put("SQLDatabasePassword", "");
        m_props.put("EnableSQLDatabase", "0");
        m_props.put("CreateSQLUserTable", "0");
        m_props.put("CreateSQLUser", "default");
        m_props.put("UseSQLDatabaseForAuthentication", "0");

        m_props.put("AntiFloodUserLevelIgnore", "100");

        m_props.put("EnableServerStructureChecks", "0");
    }
    
    private void loadPropertiesFromConfigurationFile() {
    	
    	this.loadPropertiesFromConfigurationFile(System.getProperty("OpenSMUSConfigFile", OPENSMUS_CONFIGFILELOCATION));
    }
    
    private void loadPropertiesFromConfigurationFile(final String cfgFilename) {
    	
    	FileInputStream in = null;
    	
        try {
            in = new FileInputStream(this.createConfigFileDescriptor(cfgFilename));
            m_props.load(in);
        } catch (FileNotFoundException e) {
            MUSLog.Log("Using default configuration data", MUSLog.kSys);
        } catch (IOException e) {
            MUSLog.Log("Error reading configuration file!", MUSLog.kSys);
        } finally {
        	if (in != null) {
        		try {
					in.close();
				}
				catch (IOException ignoreEx) {
				}
        	}
        }
    }
    
    private File createConfigFileDescriptor(final String cfgFilename) {
    	
    	return new File(cfgFilename != null ? cfgFilename : OPENSMUS_CONFIGFILELOCATION);
    }

    public static ArrayList parseAntiFloodSettings(String str) {
        try {
            StringTokenizer st = new StringTokenizer(str, ",");
            if (st.countTokens() == 4) {
                String subject = st.nextToken();
                int time = Integer.parseInt(st.nextToken());
                int tolerance = Integer.parseInt(st.nextToken());
                int repeats = Integer.parseInt(st.nextToken());
                ArrayList ret = new ArrayList(4);
                ret.add(subject);
                ret.add(time);
                ret.add(tolerance);
                ret.add(repeats);
                return ret;
            } else {
                MUSLog.Log("Invalid antiflood setting parameter count.", MUSLog.kSys);
                return null;
            }

        } catch (NumberFormatException e) {
            MUSLog.Log("Bad AntiFlood settings specified", MUSLog.kSys);
            return null;
        }
    }

}





