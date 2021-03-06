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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "apunkabollywood.net" }, urls = { "http://(www\\.)?apunkabollywood\\.(net|us)/browser/category/view/\\d+" }, flags = { 0 })
public class ApunkaBollyWoodNet extends PluginForDecrypt {

    public ApunkaBollyWoodNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("apunkabollywood.net/", "apunkabollywood.us/");
        br.getPage(parameter);
        if (br.containsHTML(">500 \\- Server Error<") || br.getRedirectLocation() != null) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String[] links = br.getRegex("<li><a href=\"(http://(www\\.)?apunkabollywood\\.us/browser/category/view/\\d+)").getColumn(0);
        // ><a href="http://www.apunkabollywood.us/browser/download/get/65152/Track 06 (ApunKaBollywood.com).html"> Track 06 </a><small>(9.5
        // MB)</small>
        final String[][] downloadLinks = br.getRegex("\"(http://(www\\.)?apunkabollywood\\.us/browser/download/get/\\d+/[^<>\"/]*?\\.html)\">([^<>\"]*?)</a><small>\\((\\d+(\\.\\d+)? MB)\\)</small>").getMatches();
        if ((links == null || links.length == 0) && (downloadLinks == null || downloadLinks.length == 0)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (links != null && links.length != 0) {
            for (final String singleLink : links) {
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        } else if (downloadLinks != null && downloadLinks.length != 0) {
            final String fpName = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName("Album: " + new Regex(parameter, "(\\d+)$").getMatch(0));
            if (fpName != null) {
                fp.setName(Encoding.htmlDecode(fpName.trim()));
            }
            for (final String downloadLink[] : downloadLinks) {
                br.getPage(downloadLink[0]);
                final String finallink = br.getRegex("<div id=\"DownloadBox\">[\t\n\r ]+<H1><a href=\"(http://[^<>\"]*?)\"").getMatch(0);
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
                dl.setFinalFileName(Encoding.htmlDecode(downloadLink[2].trim()) + ".mp3");
                dl.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(downloadLink[3].trim())));
                dl.setAvailable(true);
                fp.add(dl);
                try {
                    distribute(dl);
                } catch (final Throwable e) {
                    // Not available in old Stable
                }
                decryptedLinks.add(dl);
            }
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}