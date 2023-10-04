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
 * Interface representing a movie connected to the OpenSMUS server.
 * <BR> ServerSideScript classes can use the serverMovie() method to retrieve a pointer to the ServerMovie object that created them.
 * <BR> Scripts can also use methods of the ServerObject interface to retrieve pointers to other movies connected to the server.
 */
public interface ServerMovie {

    /**
     * Retrieves a pointer to a ServerGroup object representing a group that exists on this ServerMovie.
     *
     * @param groupname String with the name of the group to be returned
     * @return ServerGroup pointer, or throws a GroupNotFoundException
     */
    public ServerGroup getServerGroup(String groupname) throws GroupNotFoundException;

    /**
     * Retrieves a pointer to a ServerGroup object representing a group that exists on this ServerMovie.
     *
     * @param groupidx index of the movie to be returned
     * @return ServerGroup pointer, or throws a GroupNotFoundException
     */
    public ServerGroup getServerGroup(int groupidx) throws GroupNotFoundException;

    /**
     * Creates a new group on the movie and returns a pointer to it.
     *
     * @param groupname String with the name of the group to be created
     * @return ServerGroup pointer, or throws a MUSErrorCode if the creation fails
     */
    public ServerGroup createServerGroup(String groupname) throws MUSErrorCode;

    /**
     * Deletes a group from the movie.
     *
     * @param groupname String with the name of the group to be deleted
     */
    public void deleteServerGroup(String groupname);

    /**
     * Gets the number of groups in this movie.
     *
     * @return the number of groups
     */
    public int serverGroupCount();

    /**
     * Gets the number of users connected to this movie.
     *
     * @return the number of users
     */
    public int serverUserCount();

    /**
     * Returns the name of the movie as a String
     */
    public String name();

    /**
     * Gets the default user levels for users that connect to this movie and do not have a user account in the DB.
     * <BR> This value can be configured by the DefaultUserLevel directive in a movie configuration file.
     */
    public int userLevel();

    /**
     * Sets the default user levels for users that connect to this movie and do not have a user account in the DB.
     * <BR> When this method is used it overrides the value configured by the DefaultUserLevel directive in a movie configuration file.
     */
    public void setuserLevel(int level);

    /**
     * Enables this group to receive new users. Groups are enabled by default.
     */
    public void enableGroup(String gname);

    /**
     * Disables this group so new users can not join it.
     */
    public void disableGroup(String gname);

    /**
     * Checks if the movie is set to persist on the server even when no users are connected to it.
     * Movies are not persistent by default.
     *
     * @return TRUE if the movie is set to persist, FALSE otherwise
     */
    public boolean persists();

    /**
     * Toggles the movie persistent flag for this movie.
     * Persistent movies are not destroyed even when no users are connected to it.
     *
     * @param persistflag TRUE if the movie needs to persist on the server, FALSE otherwise
     */
    public void setpersists(boolean persistflag);
  
} 