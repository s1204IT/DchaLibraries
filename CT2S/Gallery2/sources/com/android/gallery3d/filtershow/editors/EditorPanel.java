package com.android.gallery3d.filtershow.editors;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.category.MainPanel;
import com.android.gallery3d.filtershow.history.HistoryManager;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.state.StatePanel;

public class EditorPanel extends Fragment {
    private Editor mEditor;
    private int mEditorID;
    private LinearLayout mMainView;

    public void setEditor(int editor) {
        this.mEditorID = editor;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        FilterShowActivity filterShowActivity = (FilterShowActivity) activity;
        this.mEditor = filterShowActivity.getEditor(this.mEditorID);
    }

    public void cancelCurrentFilter() {
        MasterImage masterImage = MasterImage.getImage();
        HistoryManager adapter = masterImage.getHistory();
        int position = adapter.undo();
        masterImage.onHistoryItemClick(position);
        ((FilterShowActivity) getActivity()).invalidateViews();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FilterShowActivity activity = (FilterShowActivity) getActivity();
        if (this.mMainView != null) {
            if (this.mMainView.getParent() != null) {
                ViewGroup parent = (ViewGroup) this.mMainView.getParent();
                parent.removeView(this.mMainView);
            }
            showImageStatePanel(activity.isShowingImageStatePanel());
            return this.mMainView;
        }
        this.mMainView = (LinearLayout) inflater.inflate(R.layout.filtershow_editor_panel, (ViewGroup) null);
        View actionControl = this.mMainView.findViewById(R.id.panelAccessoryViewList);
        View editControl = this.mMainView.findViewById(R.id.controlArea);
        ImageButton cancelButton = (ImageButton) this.mMainView.findViewById(R.id.cancelFilter);
        ImageButton applyButton = (ImageButton) this.mMainView.findViewById(R.id.applyFilter);
        Button editTitle = (Button) this.mMainView.findViewById(R.id.applyEffect);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditorPanel.this.cancelCurrentFilter();
                FilterShowActivity activity2 = (FilterShowActivity) EditorPanel.this.getActivity();
                activity2.backToMain();
            }
        });
        Button toggleState = (Button) this.mMainView.findViewById(R.id.toggle_state);
        this.mEditor = activity.getEditor(this.mEditorID);
        if (this.mEditor != null) {
            this.mEditor.setUpEditorUI(actionControl, editControl, editTitle, toggleState);
            this.mEditor.reflectCurrentFilter();
            if (this.mEditor.useUtilityPanel()) {
                this.mEditor.openUtilityPanel((LinearLayout) actionControl);
            }
        }
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FilterShowActivity activity2 = (FilterShowActivity) EditorPanel.this.getActivity();
                EditorPanel.this.mEditor.finalApplyCalled();
                activity2.backToMain();
            }
        });
        showImageStatePanel(activity.isShowingImageStatePanel());
        return this.mMainView;
    }

    @Override
    public void onDetach() {
        if (this.mEditor != null) {
            this.mEditor.detach();
        }
        super.onDetach();
    }

    public void showImageStatePanel(boolean show) {
        View container = this.mMainView.findViewById(R.id.state_panel_container);
        boolean child = false;
        if (container == null) {
            FilterShowActivity activity = (FilterShowActivity) getActivity();
            container = activity.getMainStatePanelContainer(R.id.state_panel_container);
        } else {
            getChildFragmentManager().beginTransaction();
            child = true;
        }
        if (container != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            Fragment panel = getActivity().getSupportFragmentManager().findFragmentByTag("MainPanel");
            if (panel == null || (panel instanceof MainPanel)) {
                transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
            }
            if (show) {
                container.setVisibility(0);
                StatePanel statePanel = new StatePanel();
                transaction.replace(R.id.state_panel_container, statePanel, "StatePanel");
            } else {
                Fragment statePanel2 = getChildFragmentManager().findFragmentByTag("StatePanel");
                if (child) {
                    statePanel2 = getFragmentManager().findFragmentByTag("StatePanel");
                }
                if (statePanel2 != null) {
                    transaction.remove(statePanel2);
                }
            }
            transaction.commit();
        }
    }
}
