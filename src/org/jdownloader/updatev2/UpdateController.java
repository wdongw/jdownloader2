package org.jdownloader.updatev2;

import java.awt.Color;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import jd.controlling.proxy.ProxyController;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.UIOManager;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.locator.RememberRelativeLocator;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class UpdateController implements UpdateCallbackInterface {
    private static final UpdateController INSTANCE = new UpdateController();

    /**
     * get the only existing instance of UpdateController. This is a singleton
     * 
     * @return
     */
    public static UpdateController getInstance() {
        return UpdateController.INSTANCE;
    }

    private UpdateProgress icon;
    private LogSource      logger;

    public LogSource getLogger() {
        return logger;
    }

    private UpdateSettings settings;

    /**
     * Create a new instance of UpdateController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private UpdateController() {
        confirmedThreads = new HashSet<Thread>();
        eventSender = new UpdaterEventSender();
        logger = LogController.getInstance().getLogger(UpdateController.class.getName());
        settings = JsonConfig.create(UpdateSettings.class);

    }

    private UpdateHandler      handler;
    private boolean            running;
    private HashSet<Thread>    confirmedThreads;
    private String             appid;
    private String             updaterid;
    private UpdaterEventSender eventSender;
    private Icon               statusIcon;
    private String             statusLabel;
    private double             statusProgress = -1;

    public UpdateHandler getHandler() {
        return handler;
    }

    public void setHandler(UpdateHandler handler, ConfigInterface updaterSetup, String appid, String updaterid) {
        this.handler = handler;
        LogSource newLogger = handler.getLogger();
        if (newLogger != null) {
            if (logger != null)
                logger.close();
            logger = newLogger;
        }

        this.appid = appid;
        this.updaterid = updaterid;
        handler.startIntervalChecker();
        // UpdateAction.getInstance().setEnabled(true);

    }

    private synchronized boolean isThreadConfirmed() {
        return confirmedThreads.contains(Thread.currentThread());
    }

    private synchronized void setUpdateConfirmed(boolean b) {
        if (b) {
            confirmedThreads.add(Thread.currentThread());
        } else {
            confirmedThreads.remove(Thread.currentThread());
        }
        // cleanup
        for (Iterator<Thread> it = confirmedThreads.iterator(); it.hasNext();) {
            Thread th = it.next();
            if (!th.isAlive())
                it.remove();
        }

    }

    @Override
    public void updateGuiIcon(ImageIcon icon) {
        this.statusIcon = (Icon) icon;
        eventSender.fireEvent(new UpdateStatusUpdateEvent(this, statusLabel, statusIcon, statusProgress));
    }

    @Override
    public void updateGuiText(String text) {
        lazyGetIcon().setTitle(text);
        this.statusLabel = text;
        eventSender.fireEvent(new UpdateStatusUpdateEvent(this, statusLabel, statusIcon, statusProgress));
    }

    @Override
    public void updateGuiProgress(double progress) {
        this.statusProgress = progress;
        lazyGetIcon().setIndeterminate(progress < 0);
        lazyGetIcon().setValue((int) progress);
        eventSender.fireEvent(new UpdateStatusUpdateEvent(this, statusLabel, statusIcon, statusProgress));
    }

    public String getAppID() {
        UpdateHandler lhandler = handler;
        if (lhandler == null) {
            return "NotConnected";
        }
        return lhandler.getAppID();
    }

    public void runUpdateChecker(boolean manually) {
        UpdateHandler lhandler = handler;
        if (lhandler == null) {
            return;
        }
        lhandler.runUpdateCheck(manually);
    }

    @Override
    public void setRunning(boolean b) {
        this.running = b;
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (running) {
                    if (icon != null && lazyGetIcon().getParent() != null) {
                        //
                        return;
                    }
                    lazyGetIcon().setIndeterminate(true);
                    lazyGetIcon().setTitle(_GUI._.JDUpdater_JDUpdater_object_icon());
                    lazyGetIcon().setDescription(null);
                    JDGui.getInstance().getStatusBar().addProcessIndicator(icon);

                } else {
                    lazyGetIcon().setIndeterminate(false);
                    JDGui.getInstance().getStatusBar().removeProcessIndicator(icon);
                }
            }
        };

    }

    protected IconedProcessIndicator lazyGetIcon() {
        if (icon != null)
            return icon;

        icon = new EDTHelper<UpdateProgress>() {

            @Override
            public UpdateProgress edtRun() {
                if (icon != null)
                    return icon;
                UpdateProgress icon = new UpdateProgress();
                ((org.appwork.swing.components.circlebar.ImagePainter) icon.getValueClipPainter()).setBackground(Color.LIGHT_GRAY);
                ((org.appwork.swing.components.circlebar.ImagePainter) icon.getValueClipPainter()).setForeground(Color.GREEN);
                icon.setTitle(_GUI._.JDUpdater_JDUpdater_object_icon());
                icon.setEnabled(true);
                icon.addMouseListener(new MouseListener() {

                    @Override
                    public void mouseReleased(MouseEvent e) {
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {

                        // JDUpdater.getInstance().startUpdate(false);
                    }
                });
                return icon;
            }
        }.getReturnValue();

        return icon;
    }

    public boolean isRunning() {
        return running;
    }

    public void setGuiVisible(boolean b) {
        UpdateHandler lhandler = handler;
        if (lhandler != null)
            lhandler.setGuiVisible(b, true);
    }

    @Override
    public boolean handleException(Exception e) {
        return false;
    }

    public void setGuiToFront(JFrame mainFrame) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                UpdateHandler lhandler = handler;
                if (lhandler != null && lhandler.isGuiVisible()) {
                    lhandler.setGuiVisible(true, true);

                }
            }
        };

    }

    @Override
    public void onGuiVisibilityChanged(final Window window, boolean oldValue, boolean newValue) {
        // if (!oldValue && newValue && window != null) {
        // new EDTRunner() {
        //
        // @Override
        // protected void runInEDT() {
        // if (JDGui.getInstance().getMainFrame().isVisible()) {
        // Point ret = SwingUtils.getCenter(JDGui.getInstance().getMainFrame(), window);
        // window.setLocation(ret);
        // }
        // }
        // };
        // }
    }

    @Override
    public org.appwork.utils.swing.locator.Locator getGuiLocator() {
        if (JDGui.getInstance().getMainFrame() != null) {
            //
            return new RememberRelativeLocator("Updater", JDGui.getInstance().getMainFrame());
        }
        return null;

    }

    @Override
    public boolean doContinueLoopStarted() {
        return true;
    }

    @Override
    public boolean doContinueUpdateAvailable(boolean app, boolean updater, long appDownloadSize, long updaterDownloadSize, int appRevision, int updaterRevision, int appDestRevision, int updaterDestRevision) {

        if (!settings.isDoAskBeforeDownloadingAnUpdate())
            return true;
        if (isThreadConfirmed())
            return true;
        try {
            if (app && appDownloadSize < 0 || updater && updaterDownloadSize < 0) {
                confirm(0, _UPDATE._.confirmdialog_new_update_available_frametitle(), _UPDATE._.confirmdialog_new_update_available_message(), _UPDATE._.confirmdialog_new_update_available_answer_now(), _UPDATE._.confirmdialog_new_update_available_answer_later());

            } else {

                long download = 0;
                if (app) {
                    download += appDownloadSize;

                }
                if (updater) {
                    download += updaterDownloadSize;
                }
                confirm(0, _UPDATE._.confirmdialog_new_update_available_frametitle(), _UPDATE._.confirmdialog_new_update_available_message_sized(SizeFormatter.formatBytes(download)), _UPDATE._.confirmdialog_new_update_available_answer_now(), _UPDATE._.confirmdialog_new_update_available_answer_later());

            }

            // setUpdateConfirmed(true);
            return true;
        } catch (DialogClosedException e) {
            Log.exception(Level.WARNING, e);

        } catch (DialogCanceledException e) {
            Log.exception(Level.WARNING, e);

        }
        return false;

    }

    @Override
    public boolean doContinuePackageAvailable(boolean app, boolean updater, long appDownloadSize, long updaterDownloadSize, int appRevision, int updaterRevision, int appDestRevision, int updaterDestRevision) {
        return true;
    }

    @Override
    public boolean doContinueReadyForExtracting(boolean app, boolean updater, File fileclient, File fileself) {
        return true;
    }

    @Override
    public void onResults(boolean app, boolean updater, int clientRevision, int clientDestRevision, int selfRevision, int selfDestRevision, File awfFileclient, File awfFileSelf, File selfWOrkingDir, boolean jdlaunched) throws InterruptedException, IOException {
        try {
            logger.info("onResult");

            if (handler.hasPendingSelfupdate()) {
                fireUpdatesAvailable(false, handler.createAWFInstallLog());
                if (!isThreadConfirmed()) {
                    if (!handler.isGuiVisible() && settings.isDoNotAskJustInstallOnNextStartupEnabled())
                        return;
                    logger.info("ASK for installing selfupdate");

                    confirm(UIOManager.LOGIC_COUNTDOWN, _UPDATE._.confirmdialog_new_update_available_frametitle(), _UPDATE._.confirmdialog_new_update_available_for_install_message(), _UPDATE._.confirmdialog_new_update_available_answer_now_install(), _UPDATE._.confirmdialog_new_update_available_answer_later_install());

                    setUpdateConfirmed(true);
                    handler.setGuiVisible(true, true);
                }
                logger.info("Run Installing Updates");
                UpdateController.getInstance().installUpdates(null);
                return;
            }

            // no need to do this if we have a selfupdate pending
            InstallLog awfoverview = handler.createAWFInstallLog();
            logger.info(JSonStorage.toString(awfoverview));
            if (awfoverview.getSourcePackages().size() == 0) {
                logger.info("Nothing to install");
                // Thread.sleep(1000);
                handler.setGuiFinished(null);
                if (settings.isAutohideGuiIfThereAreNoUpdatesEnabled())
                    handler.setGuiVisible(false, false);
                fireUpdatesAvailable(false, null);
                return;
            }
            if (awfoverview.getModifiedFiles().size() == 0) {
                // empty package
                logger.info("Nothing to install2");
                UpdateController.getInstance().installUpdates(awfoverview);
                handler.setGuiFinished(null);
                if (settings.isAutohideGuiIfThereAreNoUpdatesEnabled())
                    handler.setGuiVisible(false, false);
                fireUpdatesAvailable(false, null);
                return;
            }
            if (awfoverview.getModifiedRestartRequiredFiles().size() == 0) {
                logger.info("Only directs");
                // can install direct
                if (!settings.isDoNotAskToInstallPlugins()) {
                    logger.info("ask to install plugins");
                    confirm(UIOManager.LOGIC_COUNTDOWN, _UPDATE._.confirmdialog_new_update_available_frametitle(), _UPDATE._.confirmdialog_new_update_available_for_install_message_plugin(), _UPDATE._.confirmdialog_new_update_available_answer_now_install(), _UPDATE._.confirmdialog_new_update_available_answer_later_install());

                }
                logger.info("run install");
                UpdateController.getInstance().installUpdates(awfoverview);
                logger.info("start scanner");
                new Thread("PluginScanner") {
                    public void run() {
                        HostPluginController.getInstance().invalidateCache();
                        CrawlerPluginController.invalidateCache();
                        HostPluginController.getInstance().ensureLoaded();
                        CrawlerPluginController.getInstance().ensureLoaded();
                    }
                }.start();
                logger.info("set gui finished");
                handler.setGuiFinished(_UPDATE._.updatedplugins());

                if (settings.isAutohideGuiIfSilentUpdatesWereInstalledEnabled())
                    handler.setGuiVisible(false, false);
                fireUpdatesAvailable(false, null);
                return;

            }
            fireUpdatesAvailable(false, awfoverview);
            // we need at least one restart
            if (isThreadConfirmed()) {
                installUpdates(awfoverview);
                fireUpdatesAvailable(false, null);
            } else {

                if (!handler.isGuiVisible() && settings.isDoNotAskJustInstallOnNextStartupEnabled())
                    return;
                List<String> rInstalls = handler.getRequestedInstalls();
                List<String> ruInstalls = handler.getRequestedUnInstalls();
                if (rInstalls.size() > 0 || ruInstalls.size() > 0) {
                    confirm(UIOManager.LOGIC_COUNTDOWN, _UPDATE._.confirmdialog_new_update_available_frametitle_extensions(), _UPDATE._.confirmdialog_new_update_available_for_install_message_extensions(rInstalls.size(), ruInstalls.size()), _UPDATE._.confirmdialog_new_update_available_answer_now_install(), _UPDATE._.confirmdialog_new_update_available_answer_later_install());

                } else {
                    confirm(UIOManager.LOGIC_COUNTDOWN, _UPDATE._.confirmdialog_new_update_available_frametitle(), _UPDATE._.confirmdialog_new_update_available_for_install_message(), _UPDATE._.confirmdialog_new_update_available_answer_now_install(), _UPDATE._.confirmdialog_new_update_available_answer_later_install());
                }
                setUpdateConfirmed(true);
                handler.setGuiVisible(true, true);

                UpdateController.getInstance().installUpdates(awfoverview);
                fireUpdatesAvailable(false, null);
            }
        } catch (DialogNoAnswerException e) {
            logger.log(e);
            handler.setGuiVisible(false, false);
        }
    }

    // public static final String UPDATE = "update";
    // public static final String SELFTEST = "selftest";
    // public static final String SELFUPDATE_ERROR = "selfupdateerror";
    // public static final String AFTER_SELF_UPDATE = "afterupdate";

    // public static final String OK = "OK";

    private void fireUpdatesAvailable(boolean self, InstallLog installLog) {

        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Type.UPDATES_AVAILABLE, self, installLog));
    }

    public UpdaterEventSender getEventSender() {
        return eventSender;
    }

    private void confirm(int flags, String title, String message, String ok, String no) throws DialogCanceledException, DialogClosedException {

        final ConfirmUpdateDialog cd = new ConfirmUpdateDialog(flags, title, message, null, ok, no) {

            @Override
            protected Window getDesiredRootFrame() {
                if (handler == null)
                    return null;
                return handler.getGuiFrame();
            }

        };

        Dialog.getInstance().showDialog(cd);
        if (cd.isClosedBySkipUntilNextRestart()) {
            handler.stopIntervalChecker();
            throw new DialogCanceledException(0);
        }

    }

    public boolean hasPendingUpdates() {
        if (handler == null)
            return false;
        return handler.hasPendingUpdates();
    }

    public void installUpdates(InstallLog log) {
        handler.installPendingUpdates(log);
        handler.clearInstallLogs();
    }

    @Override
    public Process runExeAsynch(List<String> call, File root) throws IOException {

        call.addAll(RestartController.getInstance().getFilteredRestartParameters());
        final ProcessBuilder pb = ProcessBuilderFactory.create(call);
        pb.directory(root);
        Process process = pb.start();
        logger.logAsynch(process.getErrorStream());
        logger.logAsynch(process.getInputStream());
        return process;
    }

    public boolean isExtensionInstalled(String id) {
        return handler != null && handler.isExtensionInstalled(id);
    }

    public boolean isHandlerSet() {
        return handler != null;
    }

    public void runExtensionUnInstallation(String id) throws InterruptedException {
        handler.uninstallExtension(id);
    }

    public void runExtensionInstallation(String id) throws InterruptedException {
        handler.installExtension(id);
    }

    public void waitForUpdate() throws InterruptedException {
        handler.waitForUpdate();
    }

    public String[] listExtensionIds() throws IOException {
        return handler.getOptionalsList();
    }

    @Override
    public HTTPProxy updateProxyAuth(int retries, HTTPProxy usedProxy, List<String> proxyAuths, URL url) {
        return ProxyController.getInstance().updateProxyAuthForUpdater(retries, usedProxy, proxyAuths, url);
    }

    @Override
    public List<HTTPProxy> selectProxy(URL url) {
        ArrayList<HTTPProxy> ret = new ArrayList<HTTPProxy>();
        List<HTTPProxy> lst = ProxyController.getInstance().getProxiesForUpdater(url);
        for (HTTPProxy p : lst) {
            ret.add(new ProxyClone(p));
        }
        return ret;
    }

}
