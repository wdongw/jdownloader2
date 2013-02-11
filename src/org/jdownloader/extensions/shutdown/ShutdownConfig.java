package org.jdownloader.extensions.shutdown;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;

public interface ShutdownConfig extends ExtensionConfigInterface {
    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry("Forcing Shutdown works only on some systems.")
    boolean isForceShutdownEnabled();

    void setForceShutdownEnabled(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isForceForMacInstalled();

    void setForceForMacInstalled(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry("If enabled, JD will shut down the system after downloads have finished")
    boolean isShutdownActive();

    void setShutdownActive(boolean b);

    @DefaultEnumValue("SHUTDOWN")
    @AboutConfig
    Mode getShutdownMode();

    void setShutdownMode(Mode mode);

    @DefaultBooleanValue(true)
    @AboutConfig
    @RequiresRestart
    @DescriptionForConfigEntry("If enabled, The Main Toolbar will contain a button to quickly enable or disable Shutdown")
    boolean isToolbarButtonEnabled();

    void setToolbarButtonEnabled(boolean b);

    @DefaultIntValue(60)
    @AboutConfig
    int getCountdownTime();

    void setCountdownTime(int seconds);

    @DefaultBooleanValue(true)
    @AboutConfig
    @DescriptionForConfigEntry("If you want the 'Shutdown enabled' flag to be disabled in a new session, then disable this flag")
    boolean isShutdownActiveByDefaultEnabled();

    void setShutdownActiveByDefaultEnabled(boolean b);

}
