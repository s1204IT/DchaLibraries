package com.android.gallery3d.filtershow.category;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.state.StatePanel;

public class MainPanel extends Fragment {
    private ImageButton bordersButton;
    private ImageButton filtersButton;
    private ImageButton geometryButton;
    private ImageButton looksButton;
    private LinearLayout mMainView;
    private int mCurrentSelected = -1;
    private int mPreviousToggleVersions = -1;

    private void selection(int i, boolean z) {
        if (z) {
            ((FilterShowActivity) getActivity()).setCurrentPanel(i);
        }
        switch (i) {
            case 0:
                this.looksButton.setSelected(z);
                break;
            case 1:
                this.bordersButton.setSelected(z);
                break;
            case 2:
                this.geometryButton.setSelected(z);
                break;
            case 3:
                this.filtersButton.setSelected(z);
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mMainView != null && this.mMainView.getParent() != null) {
            ((ViewGroup) this.mMainView.getParent()).removeView(this.mMainView);
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mMainView = (LinearLayout) layoutInflater.inflate(R.layout.filtershow_main_panel, (ViewGroup) null, false);
        this.looksButton = (ImageButton) this.mMainView.findViewById(R.id.fxButton);
        this.bordersButton = (ImageButton) this.mMainView.findViewById(R.id.borderButton);
        this.geometryButton = (ImageButton) this.mMainView.findViewById(R.id.geometryButton);
        this.filtersButton = (ImageButton) this.mMainView.findViewById(R.id.colorsButton);
        this.looksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainPanel.this.showPanel(0);
            }
        });
        this.bordersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainPanel.this.showPanel(1);
            }
        });
        this.geometryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainPanel.this.showPanel(2);
            }
        });
        this.filtersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainPanel.this.showPanel(3);
            }
        });
        FilterShowActivity filterShowActivity = (FilterShowActivity) getActivity();
        showImageStatePanel(filterShowActivity.isShowingImageStatePanel());
        showPanel(filterShowActivity.getCurrentPanel());
        return this.mMainView;
    }

    private boolean isRightAnimation(int i) {
        if (i < this.mCurrentSelected) {
            return false;
        }
        return true;
    }

    private void setCategoryFragment(CategoryPanel categoryPanel, boolean z) {
        FragmentTransaction fragmentTransactionBeginTransaction = getChildFragmentManager().beginTransaction();
        if (z) {
            fragmentTransactionBeginTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right);
        } else {
            fragmentTransactionBeginTransaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left);
        }
        fragmentTransactionBeginTransaction.replace(R.id.category_panel_container, categoryPanel, "CategoryPanel");
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
    }

    public void loadCategoryLookPanel(boolean z) {
        if (!z && this.mCurrentSelected == 0) {
            return;
        }
        boolean zIsRightAnimation = isRightAnimation(0);
        selection(this.mCurrentSelected, false);
        CategoryPanel categoryPanel = new CategoryPanel();
        categoryPanel.setAdapter(0);
        setCategoryFragment(categoryPanel, zIsRightAnimation);
        this.mCurrentSelected = 0;
        selection(this.mCurrentSelected, true);
    }

    public void loadCategoryBorderPanel() {
        if (this.mCurrentSelected == 1) {
            return;
        }
        boolean zIsRightAnimation = isRightAnimation(1);
        selection(this.mCurrentSelected, false);
        CategoryPanel categoryPanel = new CategoryPanel();
        categoryPanel.setAdapter(1);
        setCategoryFragment(categoryPanel, zIsRightAnimation);
        this.mCurrentSelected = 1;
        selection(this.mCurrentSelected, true);
    }

    public void loadCategoryGeometryPanel() {
        if (this.mCurrentSelected == 2 || MasterImage.getImage().getPreset() == null || MasterImage.getImage().hasTinyPlanet()) {
            return;
        }
        boolean zIsRightAnimation = isRightAnimation(2);
        selection(this.mCurrentSelected, false);
        CategoryPanel categoryPanel = new CategoryPanel();
        categoryPanel.setAdapter(2);
        setCategoryFragment(categoryPanel, zIsRightAnimation);
        this.mCurrentSelected = 2;
        selection(this.mCurrentSelected, true);
    }

    public void loadCategoryFiltersPanel() {
        if (this.mCurrentSelected == 3) {
            return;
        }
        boolean zIsRightAnimation = isRightAnimation(3);
        selection(this.mCurrentSelected, false);
        CategoryPanel categoryPanel = new CategoryPanel();
        categoryPanel.setAdapter(3);
        setCategoryFragment(categoryPanel, zIsRightAnimation);
        this.mCurrentSelected = 3;
        selection(this.mCurrentSelected, true);
    }

    public void loadCategoryVersionsPanel() {
        if (this.mCurrentSelected == 4) {
            return;
        }
        ((FilterShowActivity) getActivity()).updateVersions();
        boolean zIsRightAnimation = isRightAnimation(4);
        selection(this.mCurrentSelected, false);
        CategoryPanel categoryPanel = new CategoryPanel();
        categoryPanel.setAdapter(4);
        setCategoryFragment(categoryPanel, zIsRightAnimation);
        this.mCurrentSelected = 4;
        selection(this.mCurrentSelected, true);
    }

    public void showPanel(int i) {
        switch (i) {
            case 0:
                loadCategoryLookPanel(false);
                break;
            case 1:
                loadCategoryBorderPanel();
                break;
            case 2:
                loadCategoryGeometryPanel();
                break;
            case 3:
                loadCategoryFiltersPanel();
                break;
            case 4:
                loadCategoryVersionsPanel();
                break;
        }
    }

    public void setToggleVersionsPanelButton(ImageButton imageButton) {
        if (imageButton == null) {
            return;
        }
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MainPanel.this.mCurrentSelected == 4) {
                    MainPanel.this.showPanel(MainPanel.this.mPreviousToggleVersions);
                    return;
                }
                MainPanel.this.mPreviousToggleVersions = MainPanel.this.mCurrentSelected;
                MainPanel.this.showPanel(4);
            }
        });
    }

    public void showImageStatePanel(boolean z) {
        View viewFindViewById = this.mMainView.findViewById(R.id.state_panel_container);
        if (viewFindViewById == null) {
            viewFindViewById = ((FilterShowActivity) getActivity()).getMainStatePanelContainer(R.id.state_panel_container);
        } else {
            getChildFragmentManager().beginTransaction();
        }
        if (viewFindViewById == null) {
            return;
        }
        FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
        int i = this.mCurrentSelected;
        if (z) {
            viewFindViewById.setVisibility(0);
            StatePanel statePanel = new StatePanel();
            statePanel.setMainPanel(this);
            ((FilterShowActivity) getActivity()).updateVersions();
            fragmentTransactionBeginTransaction.replace(R.id.state_panel_container, statePanel, "StatePanel");
        } else {
            viewFindViewById.setVisibility(8);
            Fragment fragmentFindFragmentByTag = getChildFragmentManager().findFragmentByTag("StatePanel");
            if (fragmentFindFragmentByTag != null) {
                fragmentTransactionBeginTransaction.remove(fragmentFindFragmentByTag);
            }
            if (i == 4) {
                i = 0;
            }
        }
        this.mCurrentSelected = -1;
        showPanel(i);
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
    }

    public void setPreviousToggleVersion(int i) {
        this.mPreviousToggleVersions = i;
    }

    public int getPreviousToggleVersion() {
        return this.mPreviousToggleVersions;
    }
}
