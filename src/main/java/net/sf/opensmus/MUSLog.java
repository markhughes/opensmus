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
 *Class to log messages to the server output (console or text file) 
 */
public class MUSLog {

  /** Bitmaks value set automatically by OpenSMUS from the config file log directives */
  public static int m_LogLevel;
  
  /** Type of log message : system */
  public static final int kSys = 1;
  /** Type of log message : server */
  public static final int kSrv = 2;
  /** Type of log message : movie */
  public static final int kMov = 4;
  /** Type of log message : group */
  public static final int kGrp = 8;
  /** Type of log message : user */
  public static final int kUsr = 16;
  /** Type of log message : database */
  public static final int kDB = 32;
  /** Type of log message : error in message handling */
  public static final int kMsgErr = 64;
  /** Type of log message : scripting */
  public static final int kScr = 128;
  /** Type of log message : debug */
  public static final int kDeb = 256;
  /** Type of log message : debug warning */
  public static final int kDebWarn = 512;
  
  public MUSLog() 
  {
  }
  
  /** Integer value represent the log level bitmask */
  public static void setLogLevel (int level){
  	m_LogLevel = level;
  }

  /** Log as message to output. 
  * Integer parameter is the type of message, for example MUSLog.kSys. 
  */
  public static void Log(String str, int level){
	if ((m_LogLevel & level) > 0)
	 System.out.println(str);
  } 
  
  /** Logs a java exception to output. 
  * Integer parameter is the type of message, for example MUSLog.kDB.
  */
  public static void Log(Exception e, int level){
	if ((m_LogLevel & level) > 0)
	{
	 //System.out.println(e);
	 e.printStackTrace();
	}
  }
    
}