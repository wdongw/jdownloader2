package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.JDUtilities;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;

public class UpPicoasisNet extends PluginForDecrypt {

	final static String host = "up.picoasis.net";
	private String version = "1.0.0.0";
	private Pattern patternSupported = getSupportPattern("http://up.picoasis.net/[+]");
	
    public UpPicoasisNet() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }
	
    @Override public String getCoder() { return "Botzi"; }
    @Override public String getHost() { return host; }
    @Override public String getPluginID() { return "up.Picoasis.net-1.0.0."; }
    @Override public String getPluginName() { return host; }
    @Override public Pattern getSupportedLinks() { return patternSupported; }
    @Override public String getVersion() { return version; }
    @Override public boolean isClipboardEnabled() { return true; }
    
    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String> decryptedLinks = new Vector<String>();
    		try {
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = getRequest(url);
 			
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_MAX, 1));
    			
    			decryptedLinks.add(JDUtilities.htmlDecode(getBetween(reqinfo.getHtmlCode(), "frameborder=\"no\" width=\"100%\" src=\"", "\"></iframe>")));
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			
    			//Decrypt abschliessen
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_FINISH, null));
    			step.setParameter(decryptedLinks);
    		}
    		catch(IOException e) {
    			e.printStackTrace();
    		}
    	}
    	return null;
    }
    
    @Override public boolean doBotCheck(File file) {        
        return false;
    }
}