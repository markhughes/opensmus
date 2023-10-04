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
 * Interface used by OpenSMUS for user authentication, using the internal database system or an SQL source.
 * <BR> The database backend used for user authentication is configured in the OpenSMUS.cfg file.
 * Use the getServerUserDatabase() method of the ServerObject interface to acquire this object.
 */
public interface ServerUserDatabase {

    int AUTHENTICATION_NONE = 0;
    int AUTHENTICATION_OPTIONAL = 1;
    int AUTHENTICATION_REQUIRED = 2;

    /**
     * Creates a user record in the user database
     *
     * @param usernamein Username string, will be converted to uppercase for storage
     * @param password   Password string
     * @param userlevel  User access level as a string. Usual values are between 20 and 100.
     *                   <BR> If the string specified can not be converted the default user level will be set.
     * @return true if the user account is created successfully, false if an error occurs
     */
    public boolean createUser(String usernamein, String password, String userlevel);

    /**
     * Updates the last login time for this user in the database to the current time.
     *
     * @param userid User id integer retrieved by the getDBUser method.
     */
    public void updateUserLastLoginTime(int userid);

    /**
     * Returns the integer id for the user in the database.
     * <BR>Other methods use the user id for speed.
     *
     * @param usernamein Username string
     */
    public int getDBUser(String usernamein) throws DBException, UserNotFoundException;

    /**
     * Deletes the user with the specified user id from the database.
     *
     * @param userid User id integer retrieved by the getDBUser method.
     * @return true if the user account is deleted successfully, false if an error occurs
     */
    public boolean deleteDBUser(int userid);

    /**
     * Retrieves the user access level
     *
     * @param userid User id integer retrieved by the getDBUser method.
     */
    public int getDBUserLevel(int userid) throws DBException;

    /**
     * Retrieves the user password
     *
     * @param userid User id integer retrieved by the getDBUser method.
     */
    public String getDBUserPassword(int userid) throws DBException;

    /**
     * Checks if a user/pass combination is valid and sets the userlevel
     *
     * @param oneUser The user object that will have the userlevel set
     * @param username
     * @param password
     * @return 0 if allowed to login, MUS errorcode if not.
     */
    public int checkLogin(MUSUser oneUser, String username, String password);

    /**
     * Checks if the user database is enabled.
     * <BR>Databases are enabled by default, but can be disabled by using the OpenSMUS.cfg file.
     */
    public boolean isEnabled();
}
 