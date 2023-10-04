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
 *Interface representing a user connected to the server.
 *<BR> Scripts can use methods of the ServerGroup interface to retrieve pointers to existing users.
 */

import java.util.*;

public interface ServerUser {

    /**
     * Sends a message to this server user directly.
     *
     * @param msg Message to send
     */
    public void sendMessage(MUSMessage msg);

    /**
     * Posts a message to the OpenSMUS server dispatcher.
     * <BR> Scripts can use this method to post messages to the server as if they were a connected user, since the ServerSideScript class implements the ServerUser interface.
     *
     * @param msg Message to post
     */
    public void postMessage(MUSMessage msg);

    /**
     * Returns the name of the user as a String
     */
    public String name();

    /**
     * Gets the user access level for this user
     */
    public int userLevel();

    /**
     * Sets the user access level for this user.
     *
     * @param level new user access level
     */
    public void setuserLevel(int level);

    /**
     * Retrieves the ServerMovie object representing the movie this user is connected to.
     * <BR> Server side scripts can use this method to retrieve a pointer to the movie that created them.
     */
    public ServerMovie serverMovie();

    /**
     * Gets this user's creationTime on the server, equivalent to the user login
     */
    public long creationTime();

    /**
     * Gets this user's IP address as a String
     */
    public String ipAddress();

    /**
     * Gets a list of the groups this user is a member of.
     * By default all users are at least members of one group, @AllUsers
     *
     * @return Java Vector with the group names as Strings.
     */
    public Vector<String> getGroupNames();

     /**
     * Gets a list of the groups this user is a member of.
     * By default all users are at least members of one group, @AllUsers
     *
     * @return Java Vector with the group objects.
     */
    public Vector<ServerGroup> getGroups();

    /**
     * Gets the number of groups this user is a member of.
     * By default all users are at least members of one group, @AllUsers
     *
     * @return the number of groups
     */
    public int getGroupsCount();

    /**
     * Deletes this user, disconnecting him from the server.
     */
    public void deleteUser();

    /**
     * Called by the OpenSMUS server when the user joins a group.
     *
     * @param grp ServerGroup reference
     */
    public void groupJoined(ServerGroup grp);

    /**
     * Called by the OpenSMUS server when the user leaves a group.
     *
     * @param grp ServerGroup reference
     */
    public void groupLeft(ServerGroup grp);

} 