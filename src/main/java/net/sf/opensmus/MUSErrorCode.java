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
 * Class representing a MultiUserServer error code.
 */
public class MUSErrorCode extends Exception {

    public final static int NoError = 0;
    public final static int Unknown = -2147216223;
    public final static int InvalidMovieID = -2147216222;
    public final static int InvalidUserID = -2147216221;
    public final static int InvalidPassword = -2147216220;
    public final static int IncomingDataLost = -2147216219;
    public final static int InvalidServerName = -2147216218;
    public final static int NoConnectionsAvailable = -2147216217;
    public final static int BadParameter = -2147216216;
    public final static int NoSocketManager = -2147216215;
    public final static int NoCurrentConnection = -2147216214;
    public final static int NoWaitingMessage = -2147216213;
    public final static int BadConnectionID = -2147216212;
    public final static int WrongNumberOfParams = -2147216211;
    public final static int UnknownInternalError = -2147216210;
    public final static int ConnectionRefused = -2147216209;
    public final static int MessageTooLarge = -2147216208;
    public final static int InvalidMessageFormat = -2147216207;
    public final static int InvalidMessageLength = -2147216206;
    public final static int MessageMissing = -2147216205;
    public final static int ServerInitializationFailed = -2147216204;
    public final static int ServerSendFailed = -2147216203;
    public final static int ServerCloseFailed = -2147216202;
    public final static int ConnectionDuplicate = -2147216201;
    public final static int InvalidNumberOfMessageRecipients = -2147216200;
    public final static int InvalidMessageRecipient = -2147216199;
    public final static int InvalidMessage = -2147216198;
    public final static int ServerInternalError = -2147216197;
    public final static int ErrorJoiningGroup = -2147216196;
    public final static int ErrorLeavingGroup = -2147216195;
    public final static int InvalidGroupName = -2147216194;
    public final static int InvalidServerCommand = -2147216193;
    public final static int NotPermittedWithUserLevel = -2147216192;
    public final static int DatabaseError = -2147216191;
    public final static int InvalidServerInitFile = -2147216190;
    public final static int DatabaseWrite = -2147216189;
    public final static int DatabaseRead = -2147216188;
    public final static int DatabaseUserIDNotFound = -2147216187;
    public final static int DatabaseAddUser = -2147216186;
    public final static int DatabaseLocked = -2147216185;
    public final static int DatabaseDataRecordNotUnique = -2147216184;
    public final static int DatabaseNoCurrentRecord = -2147216183;
    public final static int DatabaseRecordNotExists = -2147216182;
    public final static int DatabaseMovedPastLimits = -2147216181;
    public final static int DatabaseDataNotFound = -2147216180;
    public final static int DatabaseNoCurrentTag = -2147216179;
    public final static int DatabaseNoCurrentDB = -2147216178;
    public final static int DatabaseNoConfigurationFile = -2147216177;
    public final static int DatabaseRecordNotLocked = -2147216176;
    public final static int OperationNotAllowed = -2147216175;
    public final static int RequestedDataNotFound = -2147216174;
    public final static int MessageContainsErrorInfo = -2147216173;
    public final static int DataConcurrencyError = -2147216172;
    public final static int UDPSocketError = -2147216171;

    public int m_errCode = 0;

    public MUSErrorCode(int msg) {
        super("MUSErrorCode > " + msg);
        m_errCode = msg;

    }

}
