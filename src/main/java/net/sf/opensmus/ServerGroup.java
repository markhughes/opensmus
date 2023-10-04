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

import java.util.Vector;

/**
 * Interface representing a group on a movie.
 * <BR> Scripts can use methods of the ServerMovie interface to retrieve pointers to existing groups.
 */
public interface ServerGroup {

    /**
     * Retrieves a pointer to a ServerUser object representing a user connected to the movie.
     *
     * @param username String with the name of the user to be returned
     * @return ServerUser pointer, or throws a UserNotFoundException
     */
    public ServerUser getServerUser(String username) throws UserNotFoundException;

    /**
     * Retrieves a pointer to a ServerUser object representing a user connected to the movie.
     *
     * @param useridx index of the user to be returned
     * @return ServerUser pointer, or throws a UserNotFoundException
     */
    public ServerUser getServerUser(int useridx) throws UserNotFoundException;

    /**
     * Adds a user to this server group
     *
     * @param oneuser ServerUser object to be added
     */
    public void addUser(ServerUser oneuser) throws MUSErrorCode;

    /**
     * Removes a user from this server group.
     *
     * @param oneuser ServerUser object to be removed
     */
    public void removeUser(ServerUser oneuser);

    /**
     * Gets the number of users in this group.
     *
     * @return the number of users
     */
    public int serverUserCount();

    /**
     * Sends a message to all users that are members of this group
     *
     * @param msg Message to send
     */
    public void sendMessage(MUSMessage msg);

    /**
     * Returns the name of the group as a String
     */
    public String name();

    /**
     * Returns the maximum number of users allowed to join this group.
     * <BR> By default there is no limit on the number of users, unless the
     * GroupSizeLimits directive in specified in a movie configuration file.
     *
     * @return the number of users allowed to join the group, or -1 for unlimited users
     */
    public int userLimit();

    /**
     * Sets the maximum number of users allowed to join this group.
     * <BR> By default there is no limit on the number of users, unless the
     * GroupSizeLimits directive in specified in a movie configuration file.
     *
     * @param level the number of users allowed to join the group, or -1 for unlimited users
     */
    public void setuserLimit(int level);

    /**
     * Checks if the group is set to persist on the movie even when no users members of it.
     * Groups are not persistent by default.
     *
     * @return TRUE if the group is set to persist, FALSE otherwise
     */
    public boolean persists();

    /**
     * Toggles the group persistent flag for this group.
     * Persistent groups are not destroyed even when no users are connected to it.
     *
     * @param persistflag TRUE if the group needs to persist on the movie, FALSE otherwise
     */
    public void setpersists(boolean persistflag);

    /**
     * Returns a list of the names of the users in the group.
     */
    public Vector<String> getUserNames();

    /**
     * Returns a list of all the users in the group.
     */
    public Vector<ServerUser> getServerUsers();
} 