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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * TODO Why are the class variables like 'm_server' package protected?
 *
 */
public class MUSServerLoginQueue extends Thread {
	
    final MUSServer m_server;
    
    final BlockingQueue<MUSQueuedMessage> m_queue;
    
    final int m_queuewait;
    
    volatile boolean m_alive = true;
    

    // Handles log in messages
    public MUSServerLoginQueue(final MUSServer server, int maxmessages, int queuewait) {
    	
    	super("MUSServerLoginQueueThread");
    	
        this.m_server = server;
        this.m_queue = new LinkedBlockingQueue<MUSQueuedMessage>(maxmessages);
        this.m_queuewait = queuewait;
        
        // TODO No good idea to play around with the thread priority
        // On heavy load no more log ins are possible
        this.setPriority(MAX_PRIORITY - 1);
    }

    @Override
    public void run() {
        try {
            while (this.m_alive) {

                MUSQueuedMessage msg = this.m_queue.poll(this.m_queuewait, TimeUnit.MILLISECONDS);
                if (msg != null) {
                    try {
                    	this.m_server.processLogonMessage(msg.m_msg, (MUSUser) msg.m_user);
                    } catch (NullPointerException e) {
                        MUSLog.Log("Null pointer in MUSServerLoginQueue " + msg, MUSLog.kDeb);
                        MUSLog.Log(e, MUSLog.kDeb);
                    }
                }
            }
        } catch (InterruptedException e) {
            MUSLog.Log("MUSServerLoginQueue interrupted", MUSLog.kDeb);
            this.kill();
            
            // TODO Interrupt gets swallowed here. Will break the interrupt chain
        }
    }

    public boolean queue(final MUSQueuedMessage msg) {
    	
        try {
            if (this.m_alive) {
                if (!this.m_queue.offer(msg, this.m_queuewait, TimeUnit.MILLISECONDS)) {
                    MUSLog.Log("Could not queue login message", MUSLog.kDebWarn);
                    return false;
                }
            }

            return true;

        } catch (InterruptedException e) {
            MUSLog.Log("Could not queue login message ", MUSLog.kDebWarn);
            return false;
        }
    }

    public void kill() {

    	this.m_queue.clear();
    	this.m_alive = false;
    }
}
