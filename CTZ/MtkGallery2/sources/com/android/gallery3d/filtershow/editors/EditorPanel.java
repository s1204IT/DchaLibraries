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
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.state.StatePanel;

public class EditorPanel extends Fragment {
    private Editor mEditor;
    private int mEditorID;
    private LinearLayout mMainView;

    public void setEditor(int i) {
        this.mEditorID = i;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mEditor = ((FilterShowActivity) activity).getEditor(this.mEditorID);
    }

    public void cancelCurrentFilter() {
        MasterImage image = MasterImage.getImage();
        image.onHistoryItemClick(image.getHistory().undo());
        ((FilterShowActivity) getActivity()).invalidateViews();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        FilterShowActivity filterShowActivity = (FilterShowActivity) getActivity();
        if (this.mMainView != null) {
            if (this.mMainView.getParent() != null) {
                ((ViewGroup) this.mMainView.getParent()).removeView(this.mMainView);
            }
            showImageStatePanel(filterShowActivity.isShowingImageStatePanel());
            return this.mMainView;
        }
        this.mMainView = (LinearLayout) layoutInflater.inflate(R.layout.filtershow_editor_panel, (ViewGroup) null);
        View viewFindViewById = this.mMainView.findViewById(R.id.panelAccessoryViewList);
        View viewFindViewById2 = this.mMainView.findViewById(R.id.controlArea);
        ImageButton imageButton = (ImageButton) this.mMainView.findViewById(R.id.cancelFilter);
        ImageButton imageButton2 = (ImageButton) this.mMainView.findViewById(R.id.applyFilter);
        Button button = (Button) this.mMainView.findViewById(R.id.applyEffect);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditorPanel.this.cancelCurrentFilter();
                EditorPanel.this.mEditor.finalCancelCalled();
                ((FilterShowActivity) EditorPanel.this.getActivity()).backToMain();
            }
        });
        Button button2 = (Button) this.mMainView.findViewById(R.id.toggle_state);
        this.mEditor = filterShowActivity.getEditor(this.mEditorID);
        if (this.mEditor != null) {
            this.mEditor.setUpEditorUI(viewFindViewById, viewFindViewById2, button, button2);
            this.mEditor.reflectCurrentFilter();
            if (this.mEditor.useUtilityPanel()) {
                this.mEditor.openUtilityPanel((LinearLayout) viewFindViewById);
            }
        }
        imageButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FilterShowActivity filterShowActivity2 = (FilterShowActivity) EditorPanel.this.getActivity();
                EditorPanel.this.mEditor.finalApplyCalled();
                filterShowActivity2.backToMain();
            }
        });
        showImageStatePanel(filterShowActivity.isShowingImageStatePanel());
        return this.mMainView;
    }

    @Override
    public void onDetach() {
        if (this.mEditor != null) {
            this.mEditor.detach();
        }
        super.onDetach();
    }

    public void showImageStatePanel(boolean z) {
        boolean z2;
        View viewFindViewById = this.mMainView.findViewById(R.id.state_panel_container);
        if (viewFindViewById == null) {
            viewFindViewById = ((FilterShowActivity) getActivity()).getMainStatePanelContainer(R.id.state_panel_container);
            z2 = false;
        } else {
            getChildFragmentManager().beginTransaction();
            z2 = true;
        }
        if (viewFindViewById == null) {
            return;
        }
        FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
        Fragment fragmentFindFragmentByTag = getActivity().getSupportFragmentManager().findFragmentByTag("MainPanel");
        if (fragmentFindFragmentByTag == null || (fragmentFindFragmentByTag instanceof MainPanel)) {
            fragmentTransactionBeginTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        if (z) {
            viewFindViewById.setVisibility(0);
            fragmentTransactionBeginTransaction.replace(R.id.state_panel_container, new StatePanel(), "StatePanel");
        } else {
            Fragment fragmentFindFragmentByTag2 = getChildFragmentManager().findFragmentByTag("StatePanel");
            if (z2) {
                fragmentFindFragmentByTag2 = getFragmentManager().findFragmentByTag("StatePanel");
            }
            if (fragmentFindFragmentByTag2 != null) {
                fragmentTransactionBeginTransaction.remove(fragmentFindFragmentByTag2);
            }
        }
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
    }
}
