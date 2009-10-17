// ConfigAppearance_p.java 
// -----------------------
// part of YaCy
// (C) by Michael Peter Christen; mc@yacy.net
// first published on http://www.anomic.de
// Frankfurt, Germany, 2004
//
// This File is contributed by Alexander Schier
// last change: 29.12.2004
// extended by Michael Christen, 4.7.2008
//
//$LastChangedDate$
//$LastChangedRevision$
//$LastChangedBy$
//
// LICENSE
// 
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import net.yacy.kelondro.data.meta.DigestURI;
import net.yacy.kelondro.util.FileUtils;

import de.anomic.crawler.retrieval.HTTPLoader;
import de.anomic.data.listManager;
import de.anomic.http.client.Client;
import de.anomic.http.server.HeaderFramework;
import de.anomic.http.server.RequestHeader;
import de.anomic.search.Switchboard;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;
import java.util.Collections;

public class ConfigAppearance_p {

    private final static String SKIN_FILENAME_FILTER = "^.*\\.css$";

    public static serverObjects respond(final RequestHeader header, final serverObjects post, final serverSwitch env) {
        final serverObjects prop = new serverObjects();
        final Switchboard sb = (Switchboard) env;
        final String skinPath = new File(env.getRootPath(), env.getConfig("skinPath", "DATA/SKINS")).toString();

        // Fallback
        prop.put("currentskin", "");
        prop.put("status", "0"); // nothing

        List<String> skinFiles = listManager.getDirListing(skinPath, SKIN_FILENAME_FILTER);
        if (skinFiles == null) {
            return prop;
        }

        if (post != null) {
            String selectedSkin = post.get("skin");

            if (post.containsKey("use_button") && selectedSkin != null) {
                /* Only change skin if filename is contained in list of filesnames
                 * read from the skin directory. This is very important to prevent
                 * directory traversal attacks!
                 */
                if (skinFiles.contains(selectedSkin)) {
                    changeSkin(sb, skinPath, selectedSkin);
                }
                
            }

            if (post.containsKey("delete_button")) {

                /* Only delete file if filename is contained in list of filesname
                 * read from the skin directory. This is very important to prevent
                 * directory traversal attacks!
                 */
                if (skinFiles.contains(selectedSkin)) {
                    final File skinfile = new File(skinPath, selectedSkin);
                    FileUtils.deletedelete(skinfile);
                }

            }
            if (post.containsKey("install_button")) {
                // load skin from URL
                final String url = post.get("url");
                final File skinFile = new File(skinPath, url.substring(url.lastIndexOf("/"), url.length()));

                Iterator<String> it;
                try {
                    final DigestURI u = new DigestURI(url, null);
                    final RequestHeader reqHeader = new RequestHeader();
                    reqHeader.put(HeaderFramework.USER_AGENT, HTTPLoader.yacyUserAgent);
                    it = FileUtils.strings(Client.wget(u.toString(), reqHeader, 10000));
                } catch (final IOException e) {
                    prop.put("status", "1");// unable to get URL
                    prop.put("status_url", url);
                    return prop;
                }
                try {
                    final BufferedWriter bw = new BufferedWriter(new PrintWriter(new FileWriter(skinFile)));

                    while (it.hasNext()) {
                        bw.write(it.next() + "\n");
                    }
                    bw.close();
                } catch (final IOException e) {
                    prop.put("status", "2");// error saving the skin
                    return prop;
                }
                if (post.containsKey("use_skin") && (post.get("use_skin", "")).equals("on")) {
                    changeSkin(sb, skinPath, url.substring(url.lastIndexOf("/"), url.length()));
                }
            }
            
        }

        // reread skins
        skinFiles = listManager.getDirListing(skinPath, SKIN_FILENAME_FILTER);
        Collections.sort(skinFiles);
        int count = 0;
        for (String skinFile : skinFiles) {
            if (skinFile.endsWith(".css")) {
                prop.put("skinlist_" + count + "_file", skinFile);
                prop.put("skinlist_" + count + "_name", skinFile.substring(0, skinFile.length() - 4));
                count++;
            }
        }
        prop.put("skinlist", count);
        prop.putHTML("currentskin", env.getConfig("currentSkin", "default"));
        return prop;
    }

    private static boolean changeSkin(final Switchboard sb, final String skinPath, final String skin) {
        final File htdocsDir = new File(sb.getConfigPath("htDocsPath", "DATA/HTDOCS"), "env");
        final File styleFile = new File(htdocsDir, "style.css");
        final File skinFile = new File(skinPath, skin);

        styleFile.getParentFile().mkdirs();
        try {
            FileUtils.copy(skinFile, styleFile);
            sb.setConfig("currentSkin", skin.substring(0, skin.length() - 4));
            return true;
        } catch (final IOException e) {
            return false;
        }
    }
}
