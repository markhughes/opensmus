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

// Doesn't check user idle anymore. That is handled by Netty.
public class MUSIdleCheck extends Thread {

    private final MUSServer server;

    public MUSIdleCheck(MUSServer svr) {

        super("MUSIdleCheckThread");

        this.server = svr;
    }

    @Override
    public void run() {

        try {
            while (server.m_alive) {

                server.checkDatabaseConnections();

                server.ensureLoggerThreadIsAlive();

                if (server.m_props.getIntProperty("EnableServerStructureChecks") == 1)
                    server.checkStructure();

                // Minimum 30 seconds between idle checks
                if (server.idle > 30)
                    Thread.sleep(server.idle * 1000);
                else
                    Thread.sleep(30000);
            }
        }
        catch (InterruptedException e) {
            // TODO Why error. Will be issued by server shutdown
            MUSLog.Log("IdleCheck Error!", MUSLog.kDeb);

            // Dont swallow interrupt exceptions
            Thread.currentThread().interrupt();
        }
    }
}
