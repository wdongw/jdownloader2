//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "worldoffiles.net" }, urls = { "http://(www\\.)?worldoffiles\\.net/getfile/[a-z0-9]+/[^<>\"/]+\\.html" }, flags = { 0 })
public class WorldOfFilesNet extends PluginForDecrypt {

    public WorldOfFilesNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        br.setFollowRedirects(false);
        if (br.containsHTML(">File was removed from filehosting|class=\"deleted\"")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String fileID = br.getRegex("name=\"file_id\" value=\"(\\d+)\"").getMatch(0);
        if (fileID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String captchaURL = br.getRegex("\"(/captcha/\\d+)\"").getMatch(0);
        if (captchaURL != null) {
            for (int i = 1; i <= 3; i++) {
                final String code = getCaptchaCode("http://www.worldoffiles.net" + captchaURL, param);
                br.postPage("http://www.worldoffiles.net/go", "captcha=" + code + "&file_id=" + fileID);
                if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("worldoffiles.net/")) continue;
                break;
            }
            if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("worldoffiles.net/")) throw new DecrypterException(DecrypterException.CAPTCHA);
        } else {
            br.postPage("http://www.worldoffiles.net/go", "file_id=" + fileID);
        }
        final String finallink = br.getRedirectLocation();
        if (finallink == null || finallink.contains("worldoffiles.net/")) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
