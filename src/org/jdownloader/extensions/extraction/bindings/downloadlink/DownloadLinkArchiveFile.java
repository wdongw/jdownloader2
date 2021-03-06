package org.jdownloader.extensions.extraction.bindings.downloadlink;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageView;
import jd.plugins.PluginProgress;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.FileCreationManager.DeleteOption;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionProgress;
import org.jdownloader.extensions.extraction.ExtractionStatus;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class DownloadLinkArchiveFile implements ArchiveFile {

    private List<DownloadLink> downloadLinks;
    private String             name;
    private String             filePath;
    private long               size;
    private Archive            archive;

    public DownloadLinkArchiveFile(DownloadLink link) {
        downloadLinks = new CopyOnWriteArrayList<DownloadLink>();
        downloadLinks.add(link);
        name = new File(link.getFileOutput(false, true)).getName();
        filePath = link.getFileOutput(false, true);
        size = link.getView().getBytesTotalEstimated();
    }

    public String toString() {
        return "DownloadLink: " + filePath + " Complete:" + isComplete();
    }

    @Override
    public int hashCode() {
        return downloadLinks.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof DownloadLinkArchiveFile)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        // this equals is used by the build method of ExtractionExtension. If we have one matching link, the archivefile matches as well
        for (DownloadLink dl : ((DownloadLinkArchiveFile) obj).downloadLinks) {
            if (downloadLinks.contains(dl)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isComplete() {
        for (DownloadLink downloadLink : downloadLinks) {
            if ((SkipReason.FILE_EXISTS.equals(downloadLink.getSkipReason()) || FinalLinkState.FAILED_EXISTS.equals(downloadLink.getFinalLinkState()) || FinalLinkState.CheckFinished(downloadLink.getFinalLinkState())) && new File(filePath).exists()) {
                return true;
            }
        }
        return false;
    }

    public String getFilePath() {
        return filePath;
    }

    public void deleteFile(DeleteOption option) {

        DownloadWatchDog.getInstance().delete(downloadLinks, option);
    }

    public List<DownloadLink> getDownloadLinks() {
        return downloadLinks;

    }

    public String getName() {
        return name;
    }

    public void setStatus(ExtractionController controller, ExtractionStatus status) {
        for (DownloadLink downloadLink : downloadLinks) {
            downloadLink.setExtractionStatus(status);
            final PluginProgress progress = downloadLink.getPluginProgress();
            if (progress != null && progress instanceof ExtractionProgress) {
                ((ExtractionProgress) progress).setMessage(status.getExplanation());
            }
        }
    }

    public void setMessage(ExtractionController controller, String text) {
        for (DownloadLink downloadLink : downloadLinks) {
            final PluginProgress progress = downloadLink.getPluginProgress();
            if (progress != null && progress instanceof ExtractionProgress) {
                ((ExtractionProgress) progress).setMessage(text);
            }
        }
    }

    public void setProgress(ExtractionController controller, long value, long max, Color color) {
        final PluginProgress progress = controller.getExtractionProgress();
        progress.updateValues(value, max);
        progress.setColor(color);
        for (DownloadLink downloadLink : downloadLinks) {
            if (value <= 0 && max <= 0) {
                downloadLink.addPluginProgress(progress);
            } else {
                if (!downloadLink.hasPluginProgress(progress)) {
                    downloadLink.addPluginProgress(progress);
                }
                if (downloadLink.getPluginProgress() == progress) {
                    final FilePackageView view = downloadLink.getParentNode().getView();
                    if (view != null) {
                        view.requestUpdate();
                    }
                }
            }
        }
    }

    @Override
    public long getFileSize() {
        return size;
    }

    @Override
    public void deleteLink() {
        final java.util.List<DownloadLink> list = new ArrayList<DownloadLink>(downloadLinks);
        DownloadController.getInstance().removeChildren(list);
    }

    public void addMirror(DownloadLink link) {
        downloadLinks.add(link);
        size = Math.max(link.getView().getBytesTotal(), size);
    }

    public void setProperty(String key, Object value) {
        for (DownloadLink downloadLink : downloadLinks) {
            downloadLink.setProperty(key, value);
        }
    }

    public Object getProperty(String key) {
        for (DownloadLink downloadLink : downloadLinks) {
            if (downloadLink.hasProperty(key)) {
                return downloadLink.getProperty(key);
            }
        }
        return null;
    }

    public AvailableStatus getAvailableStatus() {
        AvailableStatus ret = null;
        for (DownloadLink downloadLink : downloadLinks) {
            switch (downloadLink.getAvailableStatus()) {
            case TRUE:
                return downloadLink.getAvailableStatus();
            case UNCHECKED:
                ret = AvailableStatus.UNCHECKED;
                break;
            case UNCHECKABLE:
                if (ret != AvailableStatus.UNCHECKED) {
                    ret = AvailableStatus.UNCHECKABLE;
                }
                break;
            case FALSE:
                if (ret == null) {
                    ret = AvailableStatus.FALSE;
                }
                break;
            }
        }
        return ret;
    }

    @Override
    public void onCleanedUp(final ExtractionController controller) {
        for (final DownloadLink downloadLink : downloadLinks) {
            switch (CFG_GENERAL.CFG.getCleanupAfterDownloadAction()) {
            case CLEANUP_IMMEDIATELY:
                DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

                    @Override
                    protected Void run() throws RuntimeException {
                        controller.getLogger().info("Remove Link " + downloadLink.getView().getDisplayName() + " because Finished and CleanupImmediately and Extrating finished!");
                        java.util.List<DownloadLink> remove = new ArrayList<DownloadLink>();
                        remove.add(downloadLink);
                        if (DownloadController.getInstance().askForRemoveVetos(remove)) {
                            DownloadController.getInstance().removeChildren(remove);
                        } else {
                            controller.getLogger().info("Remove Link " + downloadLink.getView().getDisplayName() + " failed because of removeVetos!");
                        }
                        return null;
                    }
                });
                break;
            case CLEANUP_AFTER_PACKAGE_HAS_FINISHED:
                DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

                    @Override
                    protected Void run() throws RuntimeException {
                        controller.getLogger().info("Remove Package " + downloadLink.getView().getDisplayName() + " because Finished and CleanupImmediately and Extrating finished!");
                        FilePackage fp = downloadLink.getFilePackage();
                        if (fp.getControlledBy() != null) {
                            DownloadController.removePackageIfFinished(controller.getLogger(), fp);
                        } else {
                            controller.getLogger().info("Cannot remove. Package has no controller");
                        }
                        return null;
                    }

                });
                break;
            case CLEANUP_ONCE_AT_STARTUP:
            case NEVER:
                controller.getLogger().info(CFG_GENERAL.CFG.getCleanupAfterDownloadAction() + "");
            }
        }
    }

    @Override
    public void setArchive(Archive archive) {
        this.archive = archive;
        if (archive != null && archive.getFactory() != null) {
            for (DownloadLink downloadLink : downloadLinks) {
                downloadLink.setArchiveID(archive.getFactory().getID());
            }
        }

    }

    public Archive getArchive() {
        return archive;
    }

    @Override
    public boolean exists() {
        return new File(filePath).exists();
    }

    @Override
    public void notifyChanges(Object type) {
        for (DownloadLink link : getDownloadLinks()) {
            link.firePropertyChanged(DownloadLinkProperty.Property.ARCHIVE, type);
        }
    }

    @Override
    public void removePluginProgress(ExtractionController controller) {
        for (final DownloadLink downloadLink : downloadLinks) {
            downloadLink.removePluginProgress(controller.getExtractionProgress());
        }
    }

}
