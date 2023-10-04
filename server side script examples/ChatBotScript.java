/* 
	CHATBOT
	Server side script demo for OpenSMUS
	You may utilize this source file to create and compile object code for 
	use within products you may create.  THIS CODE IS PROVIDED "AS IS", 
	WITHOUT WARRANTY OF ANY KIND */

import net.sf.opensmus.*;

import java.io.*;
import java.util.*;

/*This is a simple script that collects posts and sends a reply to @AllUsers
  The sample shows how a server side script object can join a movie and a group,
  showing as a connected user to other users of the server.

  */

public class ChatBotScript extends ServerSideScript {

    public void scriptCreate() {
        try {
            // Script is being initialized, log a message to the server output
            serverObject().put("ChatBot script created");

            // Get a reference to the @AllUsers group in the current movie
            ServerGroup all = serverMovie().getServerGroup("@AllUsers");

            // Joins the @AllUsers group as an user
            all.addUser(this);

        } catch (Exception e) {
            // We will log any exceptions to the server output as script events
            MUSLog.Log(e, MUSLog.kScr);
        }
    }

    public void scriptDelete() {
        // Script is being deleted, log a message to the server output
        serverObject().put("ChatBot script deleted");
    }

    public void userLogOn(ServerUser usr) {
        // A user has connected to our movie, log a message to the server output
        serverObject().put("scriptcmd: userLogon " + usr.name());
    }

    public void userLogOff(ServerUser usr) {
        // A user has left to our movie, log a message to the server output
        serverObject().put("scriptcmd: userLogOff " + usr.name());
    }

    public void groupCreate(ServerGroup grp) {
        /*A group is being created on the server.
 We are interested in joining all groups, so if it is a new group (created by
 another user) then we want to join it*/
        try {
            if (!grp.name().equalsIgnoreCase("@AllUsers"))
                grp.addUser(this);
        } catch (Exception e) {
            //We will log any exceptions to the server output as script events
            MUSLog.Log(e, MUSLog.kScr);
        }
    }

    public void groupDelete(ServerGroup grp) {
        // A group is being deleted on the server, log a message to the server output
        serverObject().put("scriptcmd: group delete " + grp.name());
    }

    public void groupJoin(ServerUser usr, ServerGroup grp) {
        // A user has joined a group in our movie, log a message to the server output
        serverObject().put("scriptcmd: group join " + usr.name() + " in " + grp.name());
    }

    public void groupLeave(ServerUser usr, ServerGroup grp) {
        // A user has left a group in our movie, log a message to the server output
        serverObject().put("scriptcmd: group leave " + usr.name() + " in " + grp.name());
    }

    public void incomingMessage(ServerUser user, MUSMessage msg) {
        // We received a message, let's check the sender id
        String sender = msg.m_senderID.toString();
        // Bail out if it is a message that we have sent (to avoid being stuck in a loop)
        if (sender.equalsIgnoreCase(this.name()))
            return;

        // Gets the content portion of the message
        LValue cont = msg.m_msgContent;
        // We will only operate messages that are a single string, so check the type
        if (cont.getType() == LValue.vt_String) {
            // Casts the LValue to a LString
            LString str = (LString) cont;

            // Constructs a reply
            MUSMessage reply = new MUSMessage();
            reply.m_errCode = 0;
            reply.m_timeStamp = serverObject().timeStamp();

            // Use the same subject
            reply.m_subject = new MUSMsgHeaderString(msg.m_subject.toString());

            // Set ourselves as the sender
            reply.m_senderID = new MUSMsgHeaderString(this.name());

            // Now constructs the recipient list, message will be sent to the @AllUsers group
            reply.m_recptID = new MUSMsgHeaderStringList();
            reply.m_recptID.addElement(new MUSMsgHeaderString("@AllUsers"));

            // Finally constructs a LString with the name of the original poster
            // and the original message
            reply.m_msgContent = new LString(msg.m_senderID.toString() + " said: " + str.toString());

            // Creates a PostDelay class to post this message 2 seconds in the future
            PostDelay delay = new PostDelay(this, reply, 2000);
        }
    }


    public void postDelayedMessage(MUSMessage msg) {
        // Simply post the Message, using a method from the ServerUser interface
        // implemented by all ServerSideScript objects
        postMessage(msg);
    }

    // Overrides ServerUser interface method
    public String name() {
        // Returns a meaningful name for our ChatBot user
        return "ChatBot";
    }

}


// The PostDelay class is used to schedule posting of a message sometime in the future.
class PostDelay extends Thread {
    //Reference to script object
    ChatBotScript m_user;
    //Message to be posted
    MUSMessage m_msg;
    //Time to sleep before the message is sent
    int m_delay;

    // Constructor
    public PostDelay(ChatBotScript oneuser, MUSMessage msg, int delay) {
        // Stores initialization variables and starts the thread
        m_user = oneuser;
        m_delay = delay;
        m_msg = msg;
        start();
    }

    public void run() {
        try {
            // Waits for the specified delay
            sleep(m_delay);
            // Calls the postDelayedMessage method of our ChatBotScript class,
            // passing the message to be posted
            m_user.postDelayedMessage(m_msg);
        } catch (InterruptedException e) {
            System.out.println("Post Error!");
        }
    }
}