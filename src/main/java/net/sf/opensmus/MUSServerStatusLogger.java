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
 *	
 *
 */
public final class MUSServerStatusLogger extends Thread {
	
	/**
	 * Minimum 30 seconds between idle checks
	 */
	public static final int MINIMUM_DELAY_INSECONDS = 30;
	
    private final MUSServer server;
    
    private final int interval;
    
    private final int logLevel = MUSLog.kSrv;

    /**
     * @param svr
     * @param winterval
     */
    public MUSServerStatusLogger(final MUSServer svr, int winterval) {
    	
    	super("ServerStatusLoggerThread");
    	
        this.server = svr;
        this.interval = winterval > MINIMUM_DELAY_INSECONDS ? winterval : MINIMUM_DELAY_INSECONDS;
        
        if (svr == null)
        	throw new IllegalArgumentException("Parameter 'svr' must not be null");
    }

    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
    	
        try {
            while (this.server.m_alive) {

            	// Log the traffic statistics of the server
                this.logServerStatus(">Server state at " + server.timeString() + " \n  " + server.m_clientlist.size() + " users connected");
                this.logServerStatus(" >Traffic since last state report: \n  in - " + server.in_bytes + " bytes\n  out - " + server.out_bytes + " bytes");
                this.logServerStatus(" >Messages since last state report: \n  in - " + server.in_msg + " msgs\n  out - " + server.out_msg + " msgs\n  discarded - " + server.drop_msg + " msgs");
                
                // Clear the traffic statistics of the server
                this.resetServerTrafficStatistics();

                server.ensureThreadsAreAlive();

                Thread.sleep(this.interval * 1000);
            }
        }
        catch (InterruptedException e) {
        	// TODO Why error. Will be issued by server shutdown
            MUSLog.Log("ServerStatusLogger Error!", MUSLog.kDeb);
            
            // Dont swallow interrupt exceptions
            Thread.currentThread().interrupt();
        }
    }
    
    private void logServerStatus(final String logMessage) {
    	MUSLog.Log(logMessage, this.logLevel);
    }
    
    private void resetServerTrafficStatistics() {
    	
        server.in_bytes = 0;
        server.out_bytes = 0;
        server.in_msg = 0;
        server.out_msg = 0;
        server.drop_msg = 0;
   }
}
