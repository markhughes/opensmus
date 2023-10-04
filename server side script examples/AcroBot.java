/*
    ACROMANIA
    Server side script demo for OpenSMUS
    Based on the classic IRC game Acrophobia <http://en.wikipedia.org/wiki/Acrophobia_(game)>
 */

import java.util.*;

import net.sf.opensmus.*;

public class AcroBot extends ServerSideScript {

    final static short MAXANSWERLENGTH = 70; // Max length of submitted answers
    final static short MAXHOSTLENGTH = 8; // Max length of host submitted acros
    final static short MINACROLENGTH = 4; // Shortest randomly generated acro
    final static short VOTETIME = 45; // Seconds allotted to players voting on acros
    final static short DELAYTIME = 10; // Seconds between game rounds
    final static String MSGSUBJECT = "PM"; // The subject of the SMUS messages. Change to match your client protocol.

    double acroTime = 1.0; // Modifier to adjust timing

    private Timer m_timer; // Timer used to schedule TimerTasks
    private Map<TimerTask, Object> m_timerTasks; // List of TimerTasks being run

    Map<String, AcroGame> m_games; // List of all running games (one for each group)
    Random generator;

    ServerGroup allUsers;

    // ****************************
    // ***** OpenSMUS methods *****
    // ****************************
    public void scriptCreate() {

        generator = new Random();
        m_timer = new Timer();
        m_timerTasks = Collections.synchronizedMap(new WeakHashMap<TimerTask, Object>());

        m_games = new HashMap<String, AcroGame>();

        try {
            // Add the script as a regular user, so we can receive messages to our username. Not just "system.script"
            MUSMovie mov = (MUSMovie) serverMovie();
            mov.addUser(this);

            allUsers = serverMovie().getServerGroup("@AllUsers");
            // Join the @AllUsers group as a user
            allUsers.addUser(this);

        } catch (Exception e) {
            serverObject().put(e.toString());
        }

        serverObject().put("AcroBot script created");
    }


    public void incomingMessage(ServerUser user, MUSMessage msg) {

        // If you only want to handle specific message, define the subject here.
        if (!msg.m_subject.toString().equals(MSGSUBJECT)) return;

        // Ignore messages from ourself
        if (user.name().equalsIgnoreCase(this.name())) return;

        // Gets the content portion of the message
        LValue cont = msg.m_msgContent;
        // Ignore messages that aren't simple Strings
        if (cont.getType() != LValue.vt_String) return;

        // Split up the words in the command
        String cmd = cont.toString();
        String[] splitcmd = cmd.split("\\s");

        // Figure out in which group the user sending the command is located
        Vector userGroups = user.getGroupNames();
        userGroups.remove("@AllUsers"); // Remove the group all users are in.

        String groupName = "";
        if (userGroups.size() > 0) {
            // Assume the first group is to be used if the user is in more than one.
            groupName = userGroups.get(0).toString().substring(1); // Remove the leading "@"
        }

        // Check if the sender is in a group with a game running or not
        AcroGame whatGame = m_games.get(groupName);

        // Check if it's a command
        if (cmd.startsWith("!")) {
            // Check the 2 public commands

            if (cmd.compareToIgnoreCase("!help") == 0) {
                doShowHelp(user);
                return;
            }

            if (cmd.compareToIgnoreCase("!rules") == 0) {
                doShowRules(user);
                return;
            }

            // Check for op priveleges
            if (isOp(user)) {
                // Check the 2 commands used to start a game.
                if (splitcmd[0].compareToIgnoreCase("!start") == 0 || (cmd.compareToIgnoreCase("!startcustom") == 0)) {
                    if (splitcmd.length > 1) { // Group name included
                        groupName = cmd.substring(cmd.indexOf(' ') + 1);
                    }

                    // Check if there's a game created for this group
                    whatGame = m_games.get(groupName);

                    if (whatGame == null) {
                        // Create new game

                        // Check if the group exists
                        ServerGroup groupObject;
                        try {
                            groupObject = this.serverMovie().getServerGroup("@" + groupName);

                        } catch (GroupNotFoundException e) {
                            sendMessage(user, "No group named '" + groupName + "'");
                            return;
                        }

                        whatGame = new AcroGame(groupObject);
                    } else {
                        if (whatGame.gameState != AcroGame.IDLE) {
                            sendMessage(user, "There's already a game running in '" + groupName + "'");
                            return;
                        }
                    }

                    if (cmd.compareToIgnoreCase("!startcustom") == 0) {
                        whatGame.customGame = true;
                        whatGame.customHost = user;
                    }
                    whatGame.initGame();
                    return;
                }

                if (splitcmd[0].compareToIgnoreCase("!stop") == 0) {

                    if (splitcmd.length > 1) { // Group name included
                        groupName = cmd.substring(cmd.indexOf(' ') + 1);

                        // Check if the group exists
                        ServerGroup groupObject;
                        try {
                            groupObject = this.serverMovie().getServerGroup("@" + groupName);

                            // Check if there's a game created for this group
                            whatGame = m_games.get(groupName);

                        } catch (GroupNotFoundException e) {
                            sendMessage(user, "No group named '" + groupName + "'");
                            return;
                        }
                    }

                    if (whatGame == null) {
                        sendMessage(user, "No game running in '" + groupName + "'");
                        return;
                    }

                    // There is a game running. Go ahead and stop it.
                    whatGame.gameReset();
                    sendMessage(whatGame.group, "This game has been stopped by " + user.name());
                    m_games.remove(groupName);

                    return;
                }

                // All the remaining commands require a running game.
                if (whatGame == null) {
                    sendMessage(user, "There is no game running where you are.");
                    return;
                }

                if (splitcmd[0].compareToIgnoreCase("!setacro") == 0) {

                    if (splitcmd.length < 2) {
                        sendMessage(user, "Include the letters for the acro to use.");
                        return;
                    }

                    whatGame.doSetAcro(user, splitcmd[1]);
                    return;
                }

                if (splitcmd[0].compareToIgnoreCase("!settime") == 0) {
                    if (splitcmd.length < 2) {
                        sendMessage(user, "Include the time modifier.");
                        return;
                    }

                    double modifier = 1.0;
                    try {
                        modifier = Double.parseDouble(splitcmd[1]);
                    }
                    catch (Exception e) {
                        //
                    }
                    acroTime = modifier;
                    sendMessage(user, "Time modifier set to " + modifier);
                    return;
                }

                if (cmd.compareToIgnoreCase("!showanswers") == 0) {
                    whatGame.doShowAnswers(user);
                    return;
                }

                if (cmd.compareToIgnoreCase("!loop") == 0) {
                    whatGame.loopingGame = !whatGame.loopingGame;
                    if (whatGame.loopingGame) {
                        sendMessage(user, "Looping mode enabled.");
                    } else {
                        sendMessage(user, "Looping mode disabled.");
                    }
                    return;
                }
            } // End of op check
        } // End of command check

        if (whatGame != null) whatGame.doCheckPrivate(user, cmd);

    }

    public void doShowHelp(ServerUser name) {

        String help;

        if (isOp(name)) {
            help = "ACROMANIA BOT COMMANDS\n" +
                    "!rules       - Displays game rules.\n" +
                    "!start       - Starts a game. (Accepts optional group name parameter.)\n" +
                    "!stop        - Stops a game currently in progress.\n" +
                    "!startcustom - Starts a game of Acromania. Host must !setacro each round.\n" +
                    "!setacro     - Used to set the acronym for the next round. Can be used only during a !startcustom game.\n" +
                    "!showanswers - Shows who has entered which answer.\n" +
                    "!loop        - Toggle game auto restart on/off.\n" +
                    "!settime     - Adjusts timing. Takes a parameter between 0.2 - 2.0, acting as a multiplier to adjust round lengths."
                    ;
        } else {
            help = "ACROMANIA BOT COMMANDS\n" +
                    "!help    - Displays this help message.\n" +
                    "!rules   - Displays game rules."
                    ;
        }

        sendMessage(name, help);
    }


    public void doShowRules(ServerUser user) {

        String help = "ACROMANIA RULES:\n" +
                "Each game consists of 10 rounds. At the start of each round a randomly generated acronym will be displayed.\n" +
                "PM me a phrase that matches the letters provided. Then vote for your favorite phrase.\n" +
                "You must submit an acro each round to be able to vote during that round. Points will be given as follows:\n" +
                "+1 point for each vote that your acro receives\n" +
                "+1 bonus point if you voted for the winning acro\n" +
                "+2 bonus points for the fastest acro that received a vote\n" +
                "+5 bonus points for receiving the most votes for the round\n" +
                "After votes are tallied, all submitted acros are displayed along with the # of votes received + the bonus points received. \n" +
                "Players marked with an asterisk (*) voted for the winning acro.\n" +
                "NO POINTS are given to players that did not vote.\n" +
                "Win the game by earning the most points in 10 rounds.";

        sendMessage(user, help);
    }

    // Overrides ServerUser interface method
    public String name() {
        // Returns a meaningful name for our bot user
        return "AcroBot";
    }

    // Called when a group is deleted in this movie
    public void groupDelete(ServerGroup grp) {

        // Check if the group had a game running
        String groupName = grp.name().substring(1); // Remove leading "@"

        AcroGame game = m_games.remove(groupName);
        if (game != null) {
            game.gameReset();
        }
    }

    // Called when this script is unloaded.
    public void scriptDelete() {

        serverObject().put("AcroBot script deleted");
        for (AcroGame game : m_games.values()) {
            game.gameReset();
        }
        cancelTasks();

        try {
            // Leave the group
            allUsers = serverMovie().getServerGroup("@AllUsers");
            allUsers.removeUser(this);
            // Leave the movie
            MUSMovie mov = (MUSMovie) serverMovie();
            mov.removeUser(this);
        } catch (Exception e) {
            serverObject().put(e.toString());
        }
    }

    public void sendMessage(ServerUser recipient, String content) {

        MUSMessage msg = createMessage(new LString(content));
        msg.m_recptID.addElement(new MUSMsgHeaderString(recipient.name()));
        recipient.sendMessage(msg);
    }

    public void sendMessage(ServerGroup recipient, String content) {

        MUSMessage msg = createMessage(new LString(content));
        msg.m_recptID.addElement(new MUSMsgHeaderString(recipient.name()));
        recipient.sendMessage(msg);
    }

    public MUSMessage createMessage(LValue content) {

        MUSMessage msg = new MUSMessage();
        msg.m_errCode = 0;
        msg.m_timeStamp = this.serverObject().timeStamp();
        msg.m_subject = new MUSMsgHeaderString(MSGSUBJECT);
        // Set ourselves as the sender
        msg.m_senderID = new MUSMsgHeaderString(this.name());
        msg.m_recptID = new MUSMsgHeaderStringList();

        // Add the message contents
        msg.m_msgContent = content;

        return msg;
    }


    // Checks if this user is allowed to admin the game
    // Adjust to your own needs.
    boolean isOp(ServerUser user) {
        return user.userLevel() >= 50;
    }


    // ***************************
    // ***** Utility methods *****
    // ***************************

    // Sorts a map by value in DESCENDING order

    static <K, V> Map<K, V> sortMapByValue(Map<K, V> map) {

        List myList = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(myList, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue()); // o2 vs o1 = Descending
            }
        });

        Map result = new LinkedHashMap<K, V>();
        for (Object a : myList) {
            Map.Entry<K, V> entry = (Map.Entry<K, V>) a;
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }


    //  Make a pretty string from a list, e.g. "Adam, Bertil, Ceasar & David"
    public static String createList(Collection<String> myList) {

        StringBuilder temp = new StringBuilder(128);

        int a = 0;
        for (Iterator<String> iter = myList.iterator(); iter.hasNext(); a++) {

            temp.append(iter.next());

            if (a == myList.size() - 1) {
                // Nothing
            } else if (a == myList.size() - 2)
                temp.append(" & ");
            else
                temp.append(", ");
        }
        return temp.toString();
    }


    /**
     * Formats a String to a particular length by padding it with a provided character
     * at its tail.
     *
     * @param fragment Fragment to pad
     * @param length   Length to pad to
     * @param padding  String to pad with (usually 1 character)
     * @return Padded string
     */
    public static String formatString(String fragment, int length, String padding) {

        if (fragment == null)
            fragment = "";
        if (fragment.length() > length)
            fragment = fragment.substring(0, length);
        else {
            for (int i = fragment.length(); i < length; i++)
                fragment = fragment + padding;
        }
        return fragment;
    }


    public ServerUser grabAndRemove(ArrayList<ServerUser> list) {

        if (list.isEmpty()) {
            return null;
        } else {
            int r = ((int) Math.random() * list.size());
            ServerUser grabbed;

            grabbed = list.get(r);
            list.remove(r);
            return grabbed;
        }
    }


    /**
     * Schedules a TimerTask to occur once at a future time.  TimerTask is part of
     * the package java.util.  The only method that a subclass of TimerTask must
     * override is public void run().
     *
     * @param task    TimerTask to be executed
     * @param delayms Length of time before execution, in milliseconds
     */
    public void scheduleTask(TimerTask task, long delayms) {

        m_timerTasks.put(task, null);
        m_timer.schedule(task, delayms);
    }


    /**
     * Cancels a TimerTask cleanly.  You may cancel an individual TimerTask by using
     * task.cancel() The reference to the TimerTask is automatically freed
     * when it is no longer referenced elsewhere in the application to prevent a memory leak.
     *
     * @param task The TimerTask to cancel
     * @return true if this task is scheduled for one-time execution and has not yet run, or
     *         this task is scheduled for repeated execution. Returns false if the task was scheduled
     *         for one-time execution and has already run, or if the task was never scheduled, or if
     *         the task was already cancelled, or if task is null. (Loosely speaking, this method
     *         returns true if it prevents one or more scheduled executions from taking place.)
     */
    public boolean cancelTask(TimerTask task) {

        m_timerTasks.remove(task);
        if (task != null) {
            return task.cancel();
        }
        return false;
    }

    /**
     * Cancels all pending TimerTasks.  You could cancel an individual TimerTask by using
     * task.cancel()  Or Use cancelTask(TimerTask) instead.  Note that if you cancel a TimerTask
     * and it has already been cancelled, nothing will happen.
     */
    public void cancelTasks() {

        synchronized (m_timerTasks) {
            Iterator<TimerTask> iter = m_timerTasks.keySet().iterator();
            while (iter.hasNext()) {
                TimerTask task = iter.next();
                if (task != null)
                    task.cancel();
                iter.remove();
            }
        }
    }


    // ****************************
    // ******** Game class ********
    // ****************************

    class AcroGame {

        // Game states
        final static short STARTING = -1; // Pregame preparations
        final static short IDLE = 0;   // Not playing
        final static short ENTERING = 1; // Players entering acros
        final static short VOTING = 2;
        final static short SUBMITTING = 3; // Waiting for host to submit acro

        int gameState = IDLE;

        ServerGroup group; // What group this game is connected to
        int round = 1;
        String currentAcro = "";
        int votes[];

        HashMap<ServerUser, String> playerAnswers = new LinkedHashMap<ServerUser, String>();
        HashMap<ServerUser, Integer> playerVotes = new HashMap<ServerUser, Integer>();
        HashMap<ServerUser, Integer> playerScores = new HashMap<ServerUser, Integer>();
        HashMap<ServerUser, Integer> playerOrder = new HashMap<ServerUser, Integer>();
        HashMap<ServerUser, Integer> acroDisplay = new HashMap<ServerUser, Integer>();
        ArrayList<ServerUser> playerNames = new ArrayList<ServerUser>();

        boolean customGame = false; // Is set to true if each rounds acro will be submitted by host
        boolean loopingGame = false; // If the game will automatically restart at the end of the last round

        ServerUser customHost;

        TimerTask currentTask; // To keep track of the current task so we can cancel it if the game is stopped.


        public AcroGame(ServerGroup group) {

            serverObject().put("AcroGame created " + group.name());
            this.group = group;
            // Store this instance in the global list so we can keep track of it
            m_games.put(group.name().substring(1), this);
        }

        public void initGame() {

            gameState = STARTING;

            if (customGame) {
                gameState = SUBMITTING;

                sendMessage(group, "ACROMANIA BEGINS! Your host will submit acronyms - prepare your wit! PM me with !rules to learn how to play.");
                sendMessage(customHost, "Custom Game Initalized. Send !setacro LETTERS to set the letters for Round #1.");
            } else {
                sendMessage(group, "ACROMANIA BEGINS! Random acronyms will be generated - prepare your wit! PM me with !rules to learn how to play.");
                TimerTask preStart = new TimerTask() {
                    public void run() {
                        setUpShow();
                    }
                };
                currentTask = preStart;
                scheduleTask(preStart, DELAYTIME * 1000);
            }
        }

        // Host submitted acro
        public void doSetAcro(ServerUser user, String submittedAcro) {

            if (customGame) {
                if (gameState == SUBMITTING) {
                    submittedAcro = submittedAcro.trim();
                    submittedAcro = submittedAcro.toUpperCase();
                    submittedAcro = submittedAcro.replaceAll(" ", "");
                    if (submittedAcro.length() > MAXHOSTLENGTH) {
                        sendMessage(user, "Please submit an acronym " + MAXHOSTLENGTH + " characters or less.");
                    } else {
                        currentAcro = submittedAcro;
                        setUpShow();
                    }
                } else {
                    sendMessage(user, "Round is not complete, please wait to submit next acronym.");
                }
            } else {
                sendMessage(user, "Game is currently running in regular mode (!start). Host-submitted acronyms are not allowed.");
            }
        }

        public void gameReset() {

            customGame = false;
            round = 1;
            gameState = IDLE;
            currentAcro = "";

            cancelTask(currentTask);

            // customHost = "";

            playerScores.clear();
            playerAnswers.clear();
            playerVotes.clear();
            playerOrder.clear();
            acroDisplay.clear();
        }

        public void doShowAnswers(ServerUser name) {

            if (gameState == VOTING) {
                Iterator<ServerUser> it = playerAnswers.keySet().iterator();
                ServerUser player;
                String answer;
                while (it.hasNext()) {
                    player = it.next();
                    answer = playerAnswers.get(player);
                    sendMessage(name, player.name() + ":  " + answer);
                }
            } else {
                sendMessage(name, "Currently the game isn't in the voting stage.");
            }
        }

        public void setUpShow() {

            gameState = ENTERING;

            if (!customGame) {
                int length = Math.abs(generator.nextInt()) % 3 + MINACROLENGTH;
                currentAcro = generateAcro(length);
            } // otherwise, the curAcro global has already been set by doSetAcro

            // Convert LETTERS to L E T T E R S
            StringBuffer displayAcro = new StringBuffer(currentAcro.length() * 2 - 1);
            // Init with spaces
            for (int y = 0; y < currentAcro.length() * 2 - 1; y++) {
                displayAcro.append(' ');
            }

            char letters[] = currentAcro.toCharArray();
            int a = 0;
            for (char letter : letters) {
                displayAcro.setCharAt(a, letter);
                a += 2;
            }

            sendMessage(group, "TO ENTER, PM me a phrase that matches the challenge letters!");
            sendMessage(group, "ACROMANIA Challenge #" + round + ": " + displayAcro.toString());

            TimerTask end = new TimerTask() {
                public void run() {
                    gameState = VOTING;
                    sendMessage(group, "ACROMANIA Entries: ");
                    int i = 0;
                    while (!playerNames.isEmpty()) {
                        i++;
                        ServerUser curPlayer = grabAndRemove(playerNames);
                        sendMessage(group, "--- " + i + ": " + playerAnswers.get(curPlayer));
                        acroDisplay.put(curPlayer, i);
                    }
                    votes = new int[i];

                    if (playerAnswers.size() > 0) {
                        sendMessage(group, "VOTE: PM me the # of your favorite phrase!");
                    } else {
                        sendMessage(group, "--- 0 entries submitted.");
                    }
                    setUpVotes();
                }
            };
            currentTask = end;

            // When the length of the acronym is 4 or fewer letters, you have 60 seconds to submit an answer.
            // 5 or 6 letters give you 90 seconds. Finally, if the acronym is 7 or more letters, you have 2 minutes.
            int time = 60;
            int al = currentAcro.length();
            if (al == 5 || al == 6) {
                time = 90;
            } else if (al >= 7) {
                time = 120;
            }
            time = (int) (time * acroTime);
            scheduleTask(end, time * 1000);
        }

        public String getPlural(Integer count, String word) {

            if (count > 1 || count == 0) {
                word += "s";
            } else {
                word += " ";
            }
            return count + " " + word;
        }

        public void setUpVotes() {

            TimerTask vote = new TimerTask() {
                public void run() {

                    String fastestPlayer = "";

                    List<String> winners = new ArrayList<String>();
                    int mostVotes = 0;
                    int numVotes = 0;
                    int i = 0;

                    numVotes = votes.length;
                    // Determine the highest # of votes any acro received
                    for (i = 0; i < numVotes; i++) {
                        if (votes[i] > mostVotes) {
                            mostVotes = votes[i];
                        }
                    }

                    // Determine the fastest ACRO that received at least one vote
                    int curAcro = 0;
                    int curOrder = 0;
                    int fastest = 100;
                    ServerUser curPlayer;
                    Set<ServerUser> acroSet = acroDisplay.keySet();
                    Iterator<ServerUser> acroIT = acroSet.iterator();

                    while (acroIT.hasNext()) {
                        curPlayer = acroIT.next();
                        curAcro = acroDisplay.get(curPlayer);
                        curOrder = playerOrder.get(curPlayer);
                        if ((curOrder < fastest) && votes[curAcro - 1] > 0) {
                            fastest = curOrder;
                            fastestPlayer = curPlayer.name();
                        }
                    }

                    int playerScore = 0;
                    int playerBonus = 0;
                    int playerVoteCount = 0;
                    int playerTotal = 0;
                    int votedForVotes = 0;
                    sendMessage(group, "ROUND " + round + " RESULTS: ");

                    acroIT = acroSet.iterator();
                    while (acroIT.hasNext()) {
                        curPlayer = acroIT.next();
                        curAcro = acroDisplay.get(curPlayer);
                        playerVoteCount = votes[curAcro - 1];

                        String playerVotedWinner = "-";
                        String playerNotes = "";

                        // Calculate bonus points
                        playerBonus = 0;

                        // +5 pts for receiving the most votes (round winner)
                        if (playerVoteCount == mostVotes) {
                            playerBonus += 5;

                            winners.add(curPlayer.name());
                        }

                        // +1 pt for voting for round winner
                        if (playerVotes.containsKey(curPlayer)) {
                            votedForVotes = votes[playerVotes.get(curPlayer) - 1];
                            if (votedForVotes == mostVotes) {
                                playerBonus += 1;
                                playerVotedWinner = "*";
                            }
                        }

                        // +2 pts if this was the fastest entry with any votes
                        if (curPlayer.name().equals(fastestPlayer)) {
                            playerBonus += 2;
                        }

                        // Update players running score, only if they voted
                        if (playerVotes.containsKey(curPlayer)) {
                            if (!playerScores.containsKey(curPlayer)) {
                                playerScore = 0;
                            } else {
                                playerScore = playerScores.get(curPlayer);
                            }
                            // Score for round = bonus + number of votes their acro received
                            playerScore += playerBonus + playerVoteCount;
                            playerScores.put(curPlayer, playerScore);
                        } else {
                            playerNotes += " [NOVOTE/NOSCORE]";
                        }
                        playerTotal = playerVoteCount + playerBonus;
                        sendMessage(group, playerVotedWinner + " " + formatString(curPlayer.name(), 14, " ") + " " + getPlural(playerTotal, "pt") + " (" + getPlural(playerVoteCount, "vote") + "): " + playerAnswers.get(curPlayer) + playerNotes);
                    }
                    if (winners.isEmpty()) {
                        sendMessage(group, "ROUND WINNER: None!");
                    } else {
                        // Prettify output with correct plural/singular forms.
                        String prefix, suffix;
                        if (winners.size() > 1) {
                            prefix = "ROUND WINNERS: ";
                            suffix = "s.";
                        } else {
                            prefix = "ROUND WINNER: ";
                            suffix = ".";
                        }

                        sendMessage(group, "* = These players voted for the winner" + suffix);
                        if (!fastestPlayer.equals("")) {
                            sendMessage(group, prefix + createList(winners) + " (most votes), " + fastestPlayer + " (fastest acro with a vote)");
                        } else {
                            sendMessage(group, prefix + createList(winners) + " with the most votes");
                        }
                    }

                    playerAnswers.clear();
                    playerVotes.clear();
                    playerOrder.clear();
                    acroDisplay.clear();

                    round++;
                    if (round > 10) {
                        gameOver();
                    } else {
                        if (customGame) {
                            gameState = SUBMITTING;
                            sendMessage(customHost, "Send !setacro LETTERS to set the letters for the next round.");
                        } else {
                            TimerTask preStart = new TimerTask() {
                                public void run() {
                                    setUpShow();
                                }
                            };
                            currentTask = preStart;
                            scheduleTask(preStart, DELAYTIME * 1000);
                        }
                    }
                }
            };
            currentTask = vote;
            scheduleTask(vote, VOTETIME * 1000);
        }

        public void gameOver() {

            TimerTask game = new TimerTask() {
                public void run() {

                    sendMessage(group, "GAME OVER! FINAL SCORES:");
                    Set<ServerUser> scoringPlayers = sortMapByValue(playerScores).keySet();
                    for (ServerUser curUser : scoringPlayers) {
                        sendMessage(group, "--- " + formatString(curUser.name(), 14, " ") + ": " + playerScores.get(curUser));
                    }

                    gameReset();

                    if (loopingGame) {
                        customGame = false; // Looping games can't have a host
                        // Schedule a new game
                        TimerTask startAgain = new TimerTask() {
                            public void run() {
                                initGame();
                            }
                        };
                        currentTask = startAgain;
                        scheduleTask(startAgain, 2 * DELAYTIME * 1000);
                    } else {
                        // Not looping. Remove this game instance.
                        m_games.remove(group.name());
                    }
                }
            };
            currentTask = game;
            scheduleTask(game, (DELAYTIME / 2) * 1000);
        }

        // Handle acro submissions & voting
        public void doCheckPrivate(ServerUser user, String message) {

            if (gameState == ENTERING) {
                // Check if the answer is valid
                String pieces[] = message.split(" +");

                if (pieces.length != currentAcro.length()) {
                    sendMessage(user, "You must use the correct number of words!");
                    return;
                }

                if (message.length() > MAXANSWERLENGTH) {
                    sendMessage(user, "You have submitted a too long acronym. It must be " + MAXANSWERLENGTH + " characters or less.");
                    return;
                }

                for (int a = 0; a < pieces.length; a++) {
                    if (pieces[a].length() == 0 || pieces[a].toLowerCase().charAt(0) != currentAcro.substring(a, a + 1).toLowerCase().charAt(0)) {
                        sendMessage(user, "You have submitted an invalid acronym. It must match the letters given.");
                        return;
                    }
                }

                // Valid acro submitted

                if (!playerAnswers.containsKey(user)) {
                    sendMessage(user, "Your answer has been recorded.");
                    playerNames.add(user);
                } else {
                    playerAnswers.remove(user);
                    playerOrder.remove(user);
                    sendMessage(user, "Your answer has been changed.");
                }

                playerAnswers.put(user, message);
                playerOrder.put(user, playerAnswers.size());

            } else if (gameState == VOTING) {
                int vote = 0;
                try {
                    vote = Integer.parseInt(message);
                } catch (Exception e) {
                }

                if (vote < 1 || vote > playerAnswers.size()) {
                    sendMessage(user, "Please enter a valid vote.");
                    return;
                }

                // Has the user submitted a reply this round?
                if (!playerAnswers.containsKey(user)) {
                    sendMessage(user, "Only players who submitted an entry may vote this round.");
                    return;
                }

                // Check if voting on own reply
                if (acroDisplay.get(user) == vote) {
                    sendMessage(user, "You cannot vote for your own entry.");
                    return;
                }

                votes[vote - 1]++; // Increase vote count for this acro
                if (playerVotes.containsKey(user)) { // Check if the user has already voted
                    int lastVote = playerVotes.get(user);
                    votes[lastVote - 1]--; // Remove previous vote
                    playerVotes.remove(user);
                    sendMessage(user, "Your vote has been changed.");
                } else {
                    sendMessage(user, "Your vote has been counted.");
                }
                playerVotes.put(user, vote); // Store the user so we know he has voted

                if (playerVotes.size() == playerAnswers.size()) {
                    // If all the players who submitted answers have voted, the voting period will be cut short.
                    currentTask.cancel();
                    currentTask.run();
                }
            }
        }

        // Generate random acro
        public String generateAcro(int size) {

            StringBuffer acro = new StringBuffer(size);

            for (int i = 0; i < size; i++) { //  * 2,
                int x = Math.abs(generator.nextInt()) % 72;
                if (x > -1 && x < 3) acro.append('A');
                else if (x > 2 && x < 6) acro.append('B');
                else if (x > 5 && x < 9) acro.append('C');
                else if (x > 8 && x < 12) acro.append('D');
                else if (x > 11 && x < 15) acro.append('E');
                else if (x > 14 && x < 18) acro.append('F');
                else if (x > 17 && x < 21) acro.append('G');
                else if (x > 20 && x < 24) acro.append('H');
                else if (x > 23 && x < 27) acro.append('I');
                else if (x > 26 && x < 30) acro.append('J');
                else if (x > 29 && x < 31) acro.append('K');  // third as likely
                else if (x > 30 && x < 34) acro.append('L');
                else if (x > 33 && x < 37) acro.append('M');
                else if (x > 36 && x < 40) acro.append('N');
                else if (x > 39 && x < 43) acro.append('O');
                else if (x > 42 && x < 46) acro.append('P');
                else if (x > 45 && x < 49) acro.append('Q');
                else if (x > 48 && x < 52) acro.append('R');
                else if (x > 51 && x < 55) acro.append('S');
                else if (x > 54 && x < 59) acro.append('T');
                else if (x > 58 && x < 62) acro.append('U');
                else if (x > 61 && x < 64) acro.append('V'); // two-third as likely
                else if (x > 63 && x < 67) acro.append('W');
                else if (x > 66 && x < 68) acro.append('X'); // third as likely
                else if (x > 67 && x < 71) acro.append('Y');
                else if (x > 70 && x < 72) acro.append('Z'); // third as likely
            }

            return acro.toString();
        }
    }
}