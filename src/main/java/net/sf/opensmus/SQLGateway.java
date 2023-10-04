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
   *Interface used by OpenSMUS for communication with the default SQL database. 
   *This object is optimized to work with lists of LValues as parameters and return values
   *to SQL queries. Scripting users will probably prefer to work with JDBC objects directly
   *if data manipulation is required.
   *<BR> SQLGateway is only present if the EnableSQLDatabase is used in the OpenSMUS.cfg file.
   *Use the getSQLGateway() method of the ServerObject interface to acquire this object. 
   */ 
public interface SQLGateway {  
	
   /** 
   *Checks if the SQL database connection is alive
   */
   public boolean isConnected();
   
   /** 
   *Returns the default SQL database connection as a java.sql.Connection object.
   *<BR> This is useful if the script author wants to call JDBC methods directly.
   */
   public java.sql.Connection getConnection();
   
   /** 
   *Connects to the default SQL database.
   *<BR>This method is usually not necessary since OpenSMUS establishes the connection
   *automatically using the following directives from OpenSMUS.cfg:
   *<BR>SQLDatabaseDriver
   *<BR>SQLDatabaseURL
   *<BR>SQLDatabaseUsername
   *<BR>SQLDatabasePassword
   * @param sqldriver JDBC database driver
   * @param sqlurl JDBC database URL
   * @param sqluser Username for connection
   * @param sqlpassword Password for connection
   */
   public boolean connect(String sqldriver, String sqlurl, String sqluser, String sqlpassword);
    
   /** 
   *Disconnects from the SQL database
   *Usually the connection is kept open for the entire duration of the OpenSMUS session
   */
   public void disconnect();
    
   /** 
   *Executes an SQL update call
   * <BR>Use question marks in the query as placeholders for values contained in the params list.
   * <BR>OpenSMUS automatically maps the LList values to the appropriate SQL type.
   * <BR>Example:
   * <BR>LList params= new LList();
   * <BR>params.addElement(new LString("John"));
   * <BR>params.addElement(new LInteger(25));
   * <BR>sqlgateway.executeUpdate("UPDATE USERS SET NAME=? WHERE USERID=?",params);
   * @param sqlquery Prepared SQL query string. 
   * @param params LList of parameters to the prepared statement.
   * @return true if no SQL error occurs
   */ 
   public boolean executeUpdate(String sqlquery, LList params);
   
   /** 
   *Executes an SQL query call
   * <BR>Use question marks in the query as placeholders for values contained in the params list.
   * <BR>OpenSMUS automatically maps the LList values to the appropriate SQL type.
   * <BR>
    * <BR>Example:
   * <BR>LList params= new LList();
   * <BR>params.addElement(new LString("John"));
   * <BR>params.addElement(new LInteger(25));
   * <BR>LValue result = sqlgateway.executeQuery("SELECT LASTNAME, AGE FROM USERS WHERE FIRSTNAME=? AND AGE>?",params);
   * @param sqlquery Prepared SQL query string. 
   * @param params LList of parameters to the prepared statement.
   * @return LList containing the query results as LValues.
   *<BR> Each row in the result is returned as an LList inside the main return list.
   *<BR> Sample output (Lingo formatted): [["Perkins",10],["Garcia",23]]
   */ 
   public LValue executeQuery(String sqlquery, LList params);

}
 