package com.android.gallery3d.filtershow.state;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.category.MainPanel;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.util.FilterShowHelper;

public class StatePanel extends Fragment {
    private MainPanel mMainPanel;
    private LinearLayout mMainView;
    private ImageButton mToggleVersionsPanel;
    private StatePanelTrack track;

    public void setMainPanel(MainPanel mainPanel) {
        this.mMainPanel = mainPanel;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mMainView = (LinearLayout) layoutInflater.inflate(R.layout.filtershow_state_panel_new, (ViewGroup) null);
        this.track = (StatePanelTrack) this.mMainView.findViewById(R.id.listStates);
        this.track.setAdapter(MasterImage.getImage().getState());
        this.mToggleVersionsPanel = (ImageButton) this.mMainView.findViewById(R.id.toggleVersionsPanel);
        if (FilterShowHelper.shouldUseVersions()) {
            if (this.mToggleVersionsPanel.getVisibility() == 8 || this.mToggleVersionsPanel.getVisibility() == 4) {
                this.mToggleVersionsPanel.setVisibility(0);
                this.mToggleVersionsPanel.setImageBitmap(null);
            }
            if (this.mMainPanel != null) {
                this.mMainPanel.setToggleVersionsPanelButton(this.mToggleVersionsPanel);
            } else if (this.mToggleVersionsPanel != null) {
                this.mToggleVersionsPanel.setVisibility(8);
            }
        } else {
            this.mToggleVersionsPanel.setVisibility(8);
        }
        return this.mMainView;
    }
}
