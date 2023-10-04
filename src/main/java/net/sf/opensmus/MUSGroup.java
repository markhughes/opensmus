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
import java.util.concurrent.*;

/////////////////////////////////////////////////////////////
public class MUSGroup implements ServerGroup {

    private MUSMovie m_movie;
    public String m_name;
    private ConcurrentHashMap<String, ServerUser> m_userlist = new ConcurrentHashMap<String, ServerUser>();
    public Vector<MUSAttribute> m_attributelist = new Vector<MUSAttribute>();
    public boolean m_enabled = true;
    public boolean m_persists = false;

    public int m_userlimit = -1;

    /////////////////////////////////////////////////////////////
    public MUSGroup(MUSMovie initmovie, String initname) {

        m_movie = initmovie;
        m_name = initname;

        // Add the lastupdateTime attribute
        LSymbol attname = new LSymbol("lastUpdateTime");
        LValue attvalue = (LString) MUSAttribute.getTime();
        addAttribute(new MUSAttribute(attname, attvalue));

        // When a group is created add it to the movie list of groups
        MUSLog.Log("Group created:" + initname, MUSLog.kGrp);
        m_movie.addGroup(this);

    }
    /////////////////////////////////////////////////////////////

    public void addUser(ServerUser oneuser) throws MUSErrorCode {

        if (!m_enabled)
            throw new MUSErrorCode(MUSErrorCode.ErrorJoiningGroup);

        if ((m_userlist.size() >= m_userlimit) && (m_userlimit != -1))
            throw new MUSErrorCode(MUSErrorCode.ErrorJoiningGroup);

        String ukey = oneuser.name().toUpperCase();
        if (m_userlist.putIfAbsent(ukey, oneuser) == null) {

            // Add the user to the group before we notify the scripts
            MUSLog.Log(oneuser.name() + " joined group " + name(), MUSLog.kUsr);
            oneuser.groupJoined(this);

            for (ServerSideScript script : m_movie.m_scriptList) {
                script.groupJoin(oneuser, this);
            }
        } else {
            MUSLog.Log("Attempt to join same group twice: " + oneuser.name(), MUSLog.kGrp);
        }
    }
    /////////////////////////////////////////////////////////////

    public void removeUser(ServerUser oneuser) {

        String ukey = oneuser.name().toUpperCase();
        if (m_userlist.containsKey(ukey)) {

             // Remove the user from the group before we notify the scripts
            m_userlist.remove(ukey);
            MUSLog.Log(oneuser.name() + " left group " + name(), MUSLog.kUsr);
            oneuser.groupLeft(this);

            for (ServerSideScript script : m_movie.m_scriptList) {
                script.groupLeave(oneuser, this);
            }

            if (m_userlist.isEmpty() && !m_persists) {
                m_movie.deleteServerGroup(m_name); // Delete the group if empty and not set to persist
            }
        }
    }

    public void checkStructure() {

        for (ServerUser user : m_userlist.values()) {
            String ukey = user.name().toUpperCase();
            if (!m_movie.userThreadAlive(ukey)) {
                MUSLog.Log("Found dead user at group:" + m_name + ", user:" + user.name(), MUSLog.kDeb);
                removeUser(user);
            }
        }
    }

    public void addAttribute(MUSAttribute oneatt) {
        m_attributelist.addElement(oneatt);
    }

    public void removeAttribute(MUSAttribute oneatt) {
        m_attributelist.removeElement(oneatt);
    }

    public MUSAttribute getAttribute(String attname) throws AttributeNotFoundException {

        for (MUSAttribute at : m_attributelist) {
            if (attname.equalsIgnoreCase(at.getName().toString())) {
                return at;
            }
        }

        throw new AttributeNotFoundException("Attribute not found");
    }

    public void logDroppedMsg() {
        m_movie.logDroppedMsg();
    }

    public LValue srvcmd_handleAttributeMessage(MUSMessage reply, String attrcommand, LPropList msgattributes) {
        if (attrcommand.equalsIgnoreCase("getAttributeNames"))
            return srvcmd_getAttributeNames();

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

            // Validate optional lastUpdateTime property
            LValue msgupdatetime;
            LSymbol lutsym = new LSymbol("lastUpdateTime");
            LValue tval = new LValue();
            try {
                msgupdatetime = msgattributes.getElement(lutsym);
                try {
                    MUSAttribute att = getAttribute(lutsym.toString());
                    tval = att.get();
                } catch (AttributeNotFoundException anf) {
                    // Should not happen, lastUpdateTime is always present
                    MUSLog.Log("Could not locate group lastUpdateTime attribute", MUSLog.kDeb);
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
                // property not found while checking time
            }

            for (int e = 0; e < attrlist.count(); e++) {
                LSymbol attname = (LSymbol) attrlist.getElementAt(e);
                LValue attvalue = vallist.getElementAt(e);
                MUSAttribute att;
                try {
                    att = getAttribute(attname.toString());
                    att.set(attvalue);
                } catch (AttributeNotFoundException anf) {
                    att = new MUSAttribute(attname, attvalue);
                    addAttribute(att);
                }
            }
            // Update lastUpdateTime and report it
            tval = (LString) MUSAttribute.getTime();
            try {
                MUSAttribute att = getAttribute(lutsym.toString());
                att.set(tval);
            } catch (AttributeNotFoundException anf) {
                // Should not happen, lastUpdateTime is always present
                MUSLog.Log("Could not locate group lastUpdateTime attribute", MUSLog.kDeb);
            }
            al.addElement(lutsym, tval);
            return al;
        }

        // Process get/delete here
        LList attrlist = new LList();
        MUSAttribute.getSymbolListFromContents(attrlist, attributes);


        if (attrcommand.equalsIgnoreCase("getAttribute")) {
            // Add lastUpdateTime anyway
            attrlist.addElement(new LSymbol("lastUpdateTime"));

            for (int e = 0; e < attrlist.count(); e++) {
                try {
                    LSymbol attname = (LSymbol) attrlist.getElementAt(e);
                    MUSAttribute att = getAttribute(attname.toString());
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
                        MUSAttribute att = getAttribute(attname.toString());
                        LValue attvalue = att.get();
                        removeAttribute(att);
                    }
                } catch (AttributeNotFoundException anf) {
                }
            }
            return al;

        }

        return new LValue();

    }

    public LValue srvcmd_getAllUsersCount() {
        return new LInteger(m_userlist.size());
    }

    public LValue srvcmd_getUsers() {
        LPropList pl = new LPropList();
        pl.addElement(new LSymbol("groupName"), new LString(m_name));
        LList ml = new LList();

        for (ServerUser mv : m_userlist.values()) {
            ml.addElement(new LString(mv.name()));
        }

        pl.addElement(new LSymbol("groupMembers"), ml);
        return pl;
    }

    public LValue srvcmd_getAttributeNames() {

        LList cl = new LList();
        for (MUSAttribute mv : m_attributelist) {
            cl.addElement(mv.getName());
        }

        return cl;
    }

    public LValue srvcmd_getUserCount() {

        LPropList pl = new LPropList();
        pl.addElement(new LSymbol("groupName"), new LString(m_name));
        pl.addElement(new LSymbol("numberMembers"), new LInteger(m_userlist.size()));
        return pl;
    }

    public ServerUser getUser(String uname) throws UserNotFoundException {

        String ukey = uname.toUpperCase();
        ServerUser user = m_userlist.get(ukey);
        if (user == null) {
            throw new UserNotFoundException("User not found");
        } else {
            return user;
        }
    }

    public void removeAllUsers() {

        for (ServerUser mu : m_userlist.values()) {
            mu.groupLeft(this);
        }

        m_userlist.clear();

        // This will kill the group
        if (m_userlist.isEmpty()) {
            MUSLog.Log("killing group " + m_name, MUSLog.kGrp);
            m_movie.deleteServerGroup(m_name);
        }
    }

    // ServerGroup interface methods
    public ServerUser getServerUser(String username) throws UserNotFoundException {
        return getUser(username);
    }

    public ServerUser getServerUser(int useridx) throws UserNotFoundException {

        try {
            Enumeration enume = m_userlist.elements();
            int enumidx = 1;
            while (enume.hasMoreElements()) {
                ServerUser gn = (ServerUser) enume.nextElement();
                if (useridx == enumidx)
                    return gn;

                enumidx++;
            }
            // Throw user not found otherwise
            throw new UserNotFoundException("User not found");
        } catch (Exception e) {
            throw new UserNotFoundException("User not found");
        }
    }

    public Vector<String> getUserNames() {
        return new Vector<String>(m_userlist.keySet());
    }

    public Vector<ServerUser> getServerUsers() {
        return new Vector<ServerUser>(m_userlist.values());
    }

    public int serverUserCount() {
        return m_userlist.size();
    }

    public void sendMessage(MUSMessage msg) {
        for (ServerUser oneClient : m_userlist.values()) {
            oneClient.sendMessage(msg);
        }
    }

    public String name() {
        return m_name;
    }

    public int userLimit() {
        return m_userlimit;
    }

    public void setuserLimit(int level) {
        m_userlimit = level;
    }

    public boolean persists() {
        return m_persists;
    }

    public void setpersists(boolean persistflag) {
        m_persists = persistflag;
    }
} 