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

import java.util.*;

/**
 * Base class representing a ServerSideScript object. All server side scripts must extend this class.
 * <BR> The ServerSideScript class implements the ServerUser interface, so scripts can join movies and
 * send/receive messages as if they were a connected user.
 * <BR> Server side scripts are instantiated when a movie is created on the server. Classes are mapped to movies
 * according to the Scriptmap.cfg file.
 * <BR> Messages that are addressed to a server side script should be sent with the system.script.* prefix.
 */
public class ServerSideScript implements ServerUser {
    private ServerObject m_server;
    private ServerMovie m_movie;
    //public ServerGroup m_group;


    /**
     *The name of the script object.
     */
    // public String m_name = "";

    /**
     * The user access level for this script. Default is 100.
     */
    public int m_userlevel = 100;
    private Vector<ServerGroup> m_grouplist = new Vector<ServerGroup>();
    private int m_creationtime = 0;

    /**
     * Constructor. Reserved for internal use of OpenSMUS.
     */
    public ServerSideScript() {

    }

    /**
     * Retrieves a pointer to a ServerObject instance representing the current server.
     */
    public ServerObject serverObject() {
        return m_server;
    }

    /**
     * Reserved for internal use of OpenSMUS.
     * <BR> Scripts should implement the scriptCreate() method to perform initialization tasks.
     */
    public void initScript(ServerObject srv, ServerMovie mov) {
        //do script stuff here
        m_server = srv;
        m_movie = mov;
        //m_group = grp;
        m_creationtime = m_server.timeStamp();
        scriptCreate();
    }

    /**
     * Called by the OpenSMUS server when a message addressed to this script object arrives.
     * <BR>Scripts should implement this method to receive message from other users.
     * Messages should be addressed to system.script.*, and are passed intact for processing.
     *
     * @param user ServerUser reference, representing the user that has sent the message.
     * @param msg  Message to be processed.
     */
    public void incomingMessage(ServerUser user, MUSMessage msg) {

    }

    /**
     * Called by the OpenSMUS server when the script object is created on the server.
     * Scripts should implement this method to perform initialization tasks.
     */
    public void scriptCreate() {

    }

    /**
     * Called by the OpenSMUS server when the script object is destroyed on the server.
     * Scripts should implement this method to perform cleanup tasks.
     */
    public void scriptDelete() {

    }

    /**
     * Called by the OpenSMUS server when a user connects to the movie associated with this server side script.
     *
     * @param usr ServerUser reference
     */
    public void userLogOn(ServerUser usr) {

    }

    /**
     * Called by the OpenSMUS server when a user is disconnected from the movie associated with this server side script.
     *
     * @param usr ServerUser reference
     */
    public void userLogOff(ServerUser usr) {

    }

    /**
     * Called by the OpenSMUS server when a group is created in the movie associated with this server side script.
     *
     * @param grp ServerGroup reference
     */
    public void groupCreate(ServerGroup grp) {

    }

    /**
     * Called by the OpenSMUS server when a group is deleted from the movie associated with this server side script.
     *
     * @param grp ServerGroup reference
     */
    public void groupDelete(ServerGroup grp) {

    }

    /**
     * Called by the OpenSMUS server when a user joins a group in the movie associated with this server side script.
     *
     * @param usr ServerUser reference
     * @param grp ServerGroup reference
     */
    public void groupJoin(ServerUser usr, ServerGroup grp) {

    }

    /**
     * Called by the OpenSMUS server when a user leaves a group in the movie associated with this server side script.
     *
     * @param usr ServerUser reference
     * @param grp ServerGroup reference
     */
    public void groupLeave(ServerUser usr, ServerGroup grp) {

    }


    //ServerUser interface methods
    /**
     * Sends a message to this script directly. Message will be received by the incomingMessage() method.
     * <BR>This method is implemented for compatibility with the ServerUser interface.
     *
     * @param msg Message to send
     */
    public void sendMessage(MUSMessage msg) {
        ServerUser sender;
        try {
            sender = ((MUSMovie)m_movie).getUser(msg.m_senderID.toString());
        } catch (UserNotFoundException e) {
            // If the user can not be found, assume a default one for safety (our script)
            // @TODO: Shouldn't this throw an exception or just ignore the message?
            sender = this;
        }
        incomingMessage(sender, msg);
    }

    /**
     * Posts a message to the OpenSMUS server dispatcher.
     * <BR> This method is part of the ServerUser interface implemented by ServerSideScript objects.
     *
     * @param msg Message to post
     */
    public void postMessage(MUSMessage msg) {
        MUSMovie mov = (MUSMovie) m_movie;
        mov.handleMsg(this, msg);
    }

    /**
     * Gets the script name.
     * <BR> This method is part of the ServerUser interface implemented by ServerSideScript objects.
     */
    public String name() {
        return this.toString();
    }

    /**
     * Gets the user access level for this script.
     * <BR> By default all scripts are instantiated with full privileges (userlevel 100).
     * <BR> This method is part of the ServerUser interface implemented by ServerSideScript objects.
     */
    public int userLevel() {
        return m_userlevel;
    }

    /**
     * Sets the user access level for this script.
     * <BR> This method is part of the ServerUser interface implemented by ServerSideScript objects.
     *
     * @param level new user access level
     */
    public void setuserLevel(int level) {
        m_userlevel = level;
    }

    /**
     * Retrieves a pointer to the server movie object that created this script.
     * <BR> This method is part of the ServerUser interface implemented by ServerSideScript objects.
     *
     * @return ServerMovie pointer
     */
    public ServerMovie serverMovie() {
        return m_movie;
    }

    /**
     * Gets this scripts's creationTime on the server.
     * <BR> This method is part of the ServerUser interface implemented by ServerSideScript objects.
     */
    public long creationTime() {
        return m_creationtime;
    }

    /**
     * Returns "localhost" for server side scripts.
     * <BR>This method is implemented for compatibility with the ServerUser interface.
     */
    public String ipAddress() {
        return "localhost";
    }

    /**
     * Gets a list of the groups this script is a member of.
     * <BR> This method is part of the ServerUser interface implemented by ServerSideScript objects.
     *
     * @return Java Vector with the group names as Strings.
     */
    public Vector<String> getGroupNames() {

        Vector<String> groups = new Vector<String>();
        for (ServerGroup group : m_grouplist) {

            groups.addElement(((MUSGroup) group).m_name);
        }

        return groups;
    }

     /**
     * Gets a list of the groups this script is a member of.
     * <BR> This method is part of the ServerUser interface implemented by ServerSideScript objects.
     *
     * @return Java Vector with the groups.
     */
    public Vector<ServerGroup> getGroups() {

        return new Vector<ServerGroup>(m_grouplist);
    }

    /**
     * Gets the number of groups this script is a member of.
     * <BR> This method is part of the ServerUser interface implemented by ServerSideScript objects.
     *
     * @return the number of groups
     */
    public int getGroupsCount() {
        return m_grouplist.size();
    }

    /**
     * Scripts should not call this method, it is reserved for internal use of OpenSMUS.
     * <BR> This method is part of the ServerUser interface implemented by ServerSideScript objects.
     */
    public void deleteUser() {
    }

    /**
     * Called by the OpenSMUS server when the script joins a group.
     * <BR> This method is part of the ServerUser interface implemented by ServerSideScript objects.
     *
     * @param grp ServerGroup reference
     */
    public void groupJoined(ServerGroup grp) {
        m_grouplist.addElement(grp);
    }

    /**
     * Called by the OpenSMUS server when the script leaves a group.
     * <BR> This method is part of the ServerUser interface implemented by ServerSideScript objects.
     *
     * @param grp ServerGroup reference
     */
    public void groupLeft(ServerGroup grp) {
        m_grouplist.removeElement(grp);
    }

}