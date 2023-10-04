/* 
	CHATBOT
	Server side script demo for OpenSMUS

	You may utilize this source file to create and compile object code for 
	use within products you may create.  THIS CODE IS PROVIDED "AS IS", 
	WITHOUT WARRANTY OF ANY KIND */

import net.sf.opensmus.*;

import java.util.*;
import java.text.*;

/*This is a simple script that acquires the ServerUserDatabase interface and
  can be used to create and delete users on the server. The sample is constructed
  to illustrate how to parse and construct property lists and interface with standard
  OpenSMUS objects (ServerUserDatabase in this example.)
  
  To test this script your movie can send the following messages:
  
  Recipient: system.script.createUser
  Content: [#userid:"username", #password:"password", #userlevel: 20]
  
  Recipient: system.script.deleteUser
  Content: [#userid:"username"]
  
  The advantage of using the server side approach is to be able to create users when
  using the SQL database for user authentication, transparently. When used with the 
  standard OpenSMUS MUSDB these commands will produce results similar to 
  
  system.DBAdmin.createUser
  system.DBAdmin.deleteUser

  */


public class CreateUserSample extends ServerSideScript {

    // Member instance to store a pointer to the ServerUserDatabase object
    ServerUserDatabase m_userdb;

    public void scriptCreate() {
        // Script is being created, log a message to the server output
        serverObject().put("CreateUserSample script created");

        // Store a pointer to the ServerUserDatabase interface
        m_userdb = serverObject().getServerUserDatabase();
    }

    public void incomingMessage(ServerUser user, MUSMessage msg) {
        // We received a message (sent via system.script.createUser, for example)

        // Extracts the intended recipient from this message as a Java String
        Enumeration e = msg.m_recptID.m_stringlist.elements();
        String recpt = "";
        while (e.hasMoreElements()) {
            MUSMsgHeaderString MUSrecpt = (MUSMsgHeaderString) e.nextElement();
            recpt = MUSrecpt.toString();
        }

        // Constructs a reply message object
        MUSMessage reply = new MUSMessage();
        reply.m_errCode = 0;
        reply.m_timeStamp = serverObject().timeStamp();

        // Use the same subject as the original message
        reply.m_subject = new MUSMsgHeaderString(msg.m_subject.toString());

        // Set ourselves as the sender
        reply.m_senderID = new MUSMsgHeaderString(this.name());

        // Now constructs the recipient list, message will be sent back to the user
        reply.m_recptID = new MUSMsgHeaderStringList();
        reply.m_recptID.addElement(new MUSMsgHeaderString(user.name()));

        // Stores the message content value
        LValue msgcont = msg.m_msgContent;


        // Now executes the command contained in the recipient line
        try {
            // First check if we have a property list in the contents portion
            // of the message, otherwise throws an error
            if (msgcont.getType() != LValue.vt_PropList)
                throw new MUSErrorCode(MUSErrorCode.BadParameter);

            // We passed the test, the content is really a property list
            LPropList plist = (LPropList) msgcont;

            // First test command: we were told to create an user
            if (recpt.equalsIgnoreCase("system.script.createUser")) {

                LValue arguserid = new LValue();
                LValue argpasswd = new LValue();
                LValue arguserlevel = new LValue();

                // Try to acquire the #userid and #password values
                // These are required, and if they can not be found an error is thrown
                try {
                    arguserid = plist.getElement(new LSymbol("userID"));
                    argpasswd = plist.getElement(new LSymbol("password"));
                } catch (PropertyNotFoundException pnf) {
                    // userid and password are needed
                    throw new MUSErrorCode(MUSErrorCode.BadParameter);
                }

                // Try to acquire the #userlevel value, if present
                try {
                    arguserlevel = plist.getElement(new LSymbol("userlevel"));
                } catch (PropertyNotFoundException pnf) {
                    // userlevel is optional
                    arguserlevel = new LInteger(20);
                }

                // Check types for arguments, to make sure we can proceed
                if (arguserid.getType() != LValue.vt_String |
                        argpasswd.getType() != LValue.vt_String |
                        arguserlevel.getType() != LValue.vt_Integer) {
                    throw new MUSErrorCode(MUSErrorCode.BadParameter);
                }

                LString arguseridstr = (LString) arguserid;
                LString argpasswdstr = (LString) argpasswd;

                // Check for illegal characters in the userid argument,
                // since the username can not contain @ or #
                StringCharacterIterator sci = new StringCharacterIterator(arguseridstr.toString());
                for (char c = sci.first(); c != CharacterIterator.DONE; c = sci.next()) {
                    if ((c == '@') || (c == '#'))
                        throw new MUSErrorCode(MUSErrorCode.InvalidUserID);
                }

                // The ServerUserDatabase method requires the userlevel to be
                // passed as a String, not an integer
                LInteger userlevelint = (LInteger) arguserlevel;
                LString arguserlevelstr = new LString(Integer.toString(userlevelint.toInteger()));

                // Attempts to create the user in the ServerUserDatabase
                boolean usercreated = m_userdb.createUser(arguseridstr.toString(), argpasswdstr.toString(), arguserlevelstr.toString());

                if (usercreated) {
                    // Success! Construct a property list to reply
                    LPropList pl = new LPropList();
                    pl.addElement(new LSymbol("userID"), arguserid);
                    reply.m_msgContent = pl;
                } else {
                    // Error creating user. Add the appropriate error code to the reply
                    reply.m_errCode = MUSErrorCode.DatabaseDataRecordNotUnique;
                    LPropList pl = new LPropList();
                    pl.addElement(new LSymbol("userID"), arguserid);
                    reply.m_msgContent = pl;
                }

                // Don't forget to actually send the reply to the user!
                user.sendMessage(reply);
                return;

            }


            // First test command: we were told to delete an user
            else if (recpt.equalsIgnoreCase("system.script.deleteUser")) {
                LValue arguserid = new LValue();

                // Try to acquire the #userid value
                // This is required, and if it can not be found an error is thrown
                try {
                    arguserid = plist.getElement(new LSymbol("userID"));
                } catch (PropertyNotFoundException pnf) {
                    // userid is needed
                    throw new MUSErrorCode(MUSErrorCode.BadParameter);
                }

                // Check types for userid value
                if (arguserid.getType() != LValue.vt_String)
                    throw new MUSErrorCode(MUSErrorCode.BadParameter);

                LString arguseridstr = (LString) arguserid;

                int userid = 0;

                // First check if the user exists in the database
                try {
                    userid = m_userdb.getDBUser(arguseridstr.toString());
                } catch (UserNotFoundException dbe) {
                    // OOps, user does not exist. Reply with an error message.
                    MUSLog.Log("Invalid Command: could not get user " + arguseridstr.toString(), MUSLog.kMsgErr);
                    throw new MUSErrorCode(MUSErrorCode.InvalidUserID);
                } catch (DBException dbe) {
                    // Database error while getting the user. Reply with an error message.
                    MUSLog.Log("Invalid Command: could not get user " + arguseridstr.toString(), MUSLog.kMsgErr);
                    throw new MUSErrorCode(MUSErrorCode.DatabaseError);
                }

                // If we reached this far then the user exists, and we have its
                // user id. Let's delete it.
                boolean userdeleted = m_userdb.deleteDBUser(userid);

                if (userdeleted) {
                    // Success! Construct a property list to reply
                    LPropList pl = new LPropList();
                    pl.addElement(new LSymbol("userID"), arguseridstr);
                    reply.m_msgContent = pl;
                } else {
                    // Error creating user. Add the appropriate error code to the reply
                    reply.m_errCode = MUSErrorCode.DatabaseError;
                    LPropList pl = new LPropList();
                    pl.addElement(new LSymbol("userID"), arguseridstr);
                    reply.m_msgContent = pl;
                }

                // Don't forget to actually send the reply to the user!
                user.sendMessage(reply);
                return;

            }
        } catch (MUSErrorCode err) {
            // If an error has been thrown we need to update the reply and send it to the user
            reply.m_errCode = err.m_errCode;
            user.sendMessage(reply);
        }
    }
}