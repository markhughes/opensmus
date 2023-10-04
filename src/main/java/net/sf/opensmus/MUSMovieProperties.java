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

public class MUSMovieProperties extends MUSServerProperties {

    public Properties movieProps;

    public MUSMovieProperties(String moviename, Properties serverdef) {
        movieProps = (Properties) serverdef.clone();
        movieProps.put("NotifyDisconnect", "default");
        movieProps.put("GroupSizeLimits", "default");

        try {
            FileInputStream in = new FileInputStream(moviename.toUpperCase() + ".cfg");
            MUSLog.Log("Reading movie configuration file " + moviename.toUpperCase() + ".cfg", MUSLog.kSys);
            movieProps.load(in);
            in.close();
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
            MUSLog.Log("Error reading movie configuration file!", MUSLog.kSys);
        }
    }

    // Needed to access movie specific settings
    @Override
    public String getProperty(String prop) {
        return movieProps.getProperty(prop);
    }

    // Needed to access movie specific settings
    @Override
    public String[] getStringListProperty(String prop) {

        StringTokenizer st = new StringTokenizer(movieProps.getProperty(prop), ";");
        String[] result = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            result[i] = st.nextToken();
            i++;
        }
        return result;
    }

    public static String parseGroupSizeName(String fullgroupsizeinfo) {
        return fullgroupsizeinfo.substring(0, fullgroupsizeinfo.indexOf(":"));
    }

    public static Integer parseGroupSizeLimit(String fullgroupsizeinfo) {
        try {
            String limitstr = fullgroupsizeinfo.substring(fullgroupsizeinfo.indexOf(":") + 1, fullgroupsizeinfo.length());
            return Integer.parseInt(limitstr);
        } catch (NumberFormatException e) {
            MUSLog.Log("Bad GroupSizeLimit specified", MUSLog.kSys);
            return 0;
        }
    }

}
