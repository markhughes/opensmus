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

public class MUSKillUserTimer extends Thread {
    private MUSUser m_user;
    private int m_timeToKill;

    public MUSKillUserTimer(MUSUser oneuser, int timeToKill) {
        m_user = oneuser;
        m_timeToKill = timeToKill;
        start();
    }

    @Override
    public void run() {
        try {
            sleep(m_timeToKill);
            m_user.killMUSUser();
        } catch (InterruptedException e) {
            MUSLog.Log("KillUserTimer Error!", MUSLog.kDeb);
        }
    }
}

