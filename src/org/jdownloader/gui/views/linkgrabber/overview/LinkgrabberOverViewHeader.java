package org.jdownloader.gui.views.linkgrabber.overview;

import java.awt.Dimension;

import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.appwork.swing.components.ExtButton;
import org.jdownloader.controlling.AggregatedCrawlerNumbers;
import org.jdownloader.gui.components.CheckboxMenuItem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.overviewpanel.DataEntry;
import org.jdownloader.gui.views.linkgrabber.properties.AbstractPanelHeader;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public class LinkgrabberOverViewHeader extends AbstractPanelHeader {

    private LinkgrabberOverview overview;

    public LinkgrabberOverViewHeader(LinkgrabberOverview loverView) {
        super(_GUI._.LinkgrabberOverViewHeader_LinkgrabberOverViewHeader_(), NewTheme.I().getIcon("download", 16));
        this.overview = loverView;

    }

    protected void onCloseAction() {
    }

    @Override
    protected void onSettings(ExtButton options) {
        JPopupMenu pu = new JPopupMenu();
        CheckboxMenuItem total = new CheckboxMenuItem(_GUI._.OverViewHeader_actionPerformed_total_(), CFG_GUI.OVERVIEW_PANEL_TOTAL_INFO_VISIBLE);
        CheckboxMenuItem filtered = new CheckboxMenuItem(_GUI._.OverViewHeader_actionPerformed_visible_only_(), CFG_GUI.OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE);
        CheckboxMenuItem selected = new CheckboxMenuItem(_GUI._.OverViewHeader_actionPerformed_selected_(), CFG_GUI.OVERVIEW_PANEL_SELECTED_INFO_VISIBLE);
        pu.add(new CheckboxMenuItem(_GUI._.OverViewHeader_disabled(), CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_INCLUDE_DISABLED_LINKS));

        pu.add(new CheckboxMenuItem(_GUI._.OverViewHeader_actionPerformed_smart_(), CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE, total, filtered, selected));

        pu.add(new JSeparator(JSeparator.HORIZONTAL));
        pu.add(total);
        pu.add(filtered);
        pu.add(selected);

        pu.add(new JSeparator(JSeparator.HORIZONTAL));

        for (DataEntry<AggregatedCrawlerNumbers> de : overview.createDataEntries()) {
            if (de.getVisibleKeyHandler() != null) {
                pu.add(new CheckboxMenuItem(de.getPopupLabel(), de.getVisibleKeyHandler()));
            }

        }
        int[] insets = LAFOptions.getInstance().getPopupBorderInsets();

        Dimension pref = pu.getPreferredSize();
        // pref.width = positionComp.getWidth() + ((Component)
        // e.getSource()).getWidth() + insets[1] + insets[3];
        // pu.setPreferredSize(new Dimension(optionsgetWidth() + insets[1] + insets[3], (int) pref.getHeight()));

        pu.show(options, -insets[1], -pu.getPreferredSize().height + insets[2]);
    }

}
