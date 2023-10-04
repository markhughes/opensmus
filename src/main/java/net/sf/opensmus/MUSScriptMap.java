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
import java.io.*;

/**
 * This class maps movie names to server side script classes configured in the ScriptMap.cfg file.
 * Reserved for internal use of OpenSMUS.
 */
public class MUSScriptMap {

    /**
     * Properties set to store the script map table
     */
    public Properties m_props;

    /**
     * Constructor
     */
    public MUSScriptMap(boolean enablescripting) {

        m_props = new Properties();
        if (enablescripting) {
            MUSLog.Log("Server side scripting enabled", MUSLog.kSys);
            try {
                FileInputStream in = new FileInputStream("Scriptmap.cfg");
                m_props.load(in);
                in.close();
            } catch (FileNotFoundException e) {
                MUSLog.Log("Scriptmap config file not found", MUSLog.kDeb);
            } catch (IOException e) {
                MUSLog.Log("Error reading scriptmap file!", MUSLog.kDeb);
            }
        } else {
            MUSLog.Log("Server side scripting disabled", MUSLog.kSys);
        }
    }

    /**
     * Returns the name(s) of the server side script class configured for a particular movie.
     * If no entry matches the movie name in the script map the default ServerSideScript class is returned.
     * <BR>Reserved for internal use of OpenSMUS.
     */
    public String[] getScriptName(String prop) {

        prop = prop.toUpperCase();

        String defaultprop = m_props.getProperty("DEFAULT");
        if (defaultprop == null)
            defaultprop = "net.sf.opensmus.ServerSideScript";

        String[] scriptList;
        try {
            scriptList = getPropertyEntries(prop, ";"); // Same thing as getStringListProperty in MUSMovieProperties
        } catch (PropertyNotFoundException e) {
            scriptList = new String[]{defaultprop};
        }

        return (scriptList); // return m_props.getProperty(prop, defaultprop);
    }


    /**
     * Returns an array of Strings associated with the property specified by
     * propertyName. The method is used for entries that potentially have
     * multiple values separated by a particular character or string.
     * <p/>
     * Typically this will be used for space separated words:
     * String[] entries =
     * AppProperties.getInstance().getPropertyEntries("PropertyName", " ");
     *
     * @param propertyName    the name of the property
     * @param separatorString the string that separates each entry
     */
    public String[] getPropertyEntries(String propertyName, String separatorString) throws PropertyNotFoundException {
        String prop = m_props.getProperty(propertyName);

        if (prop == null) {
            throw new PropertyNotFoundException(propertyName);
        }

        StringTokenizer tokeniser = new StringTokenizer(prop, separatorString);
        String[] result = new String[tokeniser.countTokens()];
        int i = 0;

        while (tokeniser.hasMoreElements()) {
            result[i++] = tokeniser.nextToken();
        }

        return result;
    }
}

