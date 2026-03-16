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

    private void selection(int position, boolean value) {
        if (value) {
            FilterShowActivity activity = (FilterShowActivity) getActivity();
            activity.setCurrentPanel(position);
        }
        switch (position) {
            case 0:
                this.looksButton.setSelected(value);
                break;
            case 1:
                this.bordersButton.setSelected(value);
                break;
            case 2:
                this.geometryButton.setSelected(value);
                break;
            case 3:
                this.filtersButton.setSelected(value);
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mMainView != null && this.mMainView.getParent() != null) {
            ViewGroup parent = (ViewGroup) this.mMainView.getParent();
            parent.removeView(this.mMainView);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mMainView = (LinearLayout) inflater.inflate(R.layout.filtershow_main_panel, (ViewGroup) null, false);
        this.looksButton = (ImageButton) this.mMainView.findViewById(R.id.fxButton);
        this.bordersButton = (ImageButton) this.mMainView.findViewById(R.id.borderButton);
        this.geometryButton = (ImageButton) this.mMainView.findViewById(R.id.geometryButton);
        this.filtersButton = (ImageButton) this.mMainView.findViewById(R.id.colorsButton);
        this.looksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainPanel.this.showPanel(0);
            }
        });
        this.bordersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainPanel.this.showPanel(1);
            }
        });
        this.geometryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainPanel.this.showPanel(2);
            }
        });
        this.filtersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainPanel.this.showPanel(3);
            }
        });
        FilterShowActivity activity = (FilterShowActivity) getActivity();
        showImageStatePanel(activity.isShowingImageStatePanel());
        showPanel(activity.getCurrentPanel());
        return this.mMainView;
    }

    private boolean isRightAnimation(int newPos) {
        return newPos >= this.mCurrentSelected;
    }

    private void setCategoryFragment(CategoryPanel category, boolean fromRight) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        if (fromRight) {
            transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right);
        } else {
            transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left);
        }
        transaction.replace(R.id.category_panel_container, category, "CategoryPanel");
        transaction.commitAllowingStateLoss();
    }

    public void loadCategoryLookPanel(boolean force) {
        if (force || this.mCurrentSelected != 0) {
            boolean fromRight = isRightAnimation(0);
            selection(this.mCurrentSelected, false);
            CategoryPanel categoryPanel = new CategoryPanel();
            categoryPanel.setAdapter(0);
            setCategoryFragment(categoryPanel, fromRight);
            this.mCurrentSelected = 0;
            selection(this.mCurrentSelected, true);
        }
    }

    public void loadCategoryBorderPanel() {
        if (this.mCurrentSelected != 1) {
            boolean fromRight = isRightAnimation(1);
            selection(this.mCurrentSelected, false);
            CategoryPanel categoryPanel = new CategoryPanel();
            categoryPanel.setAdapter(1);
            setCategoryFragment(categoryPanel, fromRight);
            this.mCurrentSelected = 1;
            selection(this.mCurrentSelected, true);
        }
    }

    public void loadCategoryGeometryPanel() {
        if (this.mCurrentSelected != 2 && !MasterImage.getImage().hasTinyPlanet()) {
            boolean fromRight = isRightAnimation(2);
            selection(this.mCurrentSelected, false);
            CategoryPanel categoryPanel = new CategoryPanel();
            categoryPanel.setAdapter(2);
            setCategoryFragment(categoryPanel, fromRight);
            this.mCurrentSelected = 2;
            selection(this.mCurrentSelected, true);
        }
    }

    public void loadCategoryFiltersPanel() {
        if (this.mCurrentSelected != 3) {
            boolean fromRight = isRightAnimation(3);
            selection(this.mCurrentSelected, false);
            CategoryPanel categoryPanel = new CategoryPanel();
            categoryPanel.setAdapter(3);
            setCategoryFragment(categoryPanel, fromRight);
            this.mCurrentSelected = 3;
            selection(this.mCurrentSelected, true);
        }
    }

    public void loadCategoryVersionsPanel() {
        if (this.mCurrentSelected != 4) {
            FilterShowActivity activity = (FilterShowActivity) getActivity();
            activity.updateVersions();
            boolean fromRight = isRightAnimation(4);
            selection(this.mCurrentSelected, false);
            CategoryPanel categoryPanel = new CategoryPanel();
            categoryPanel.setAdapter(4);
            setCategoryFragment(categoryPanel, fromRight);
            this.mCurrentSelected = 4;
            selection(this.mCurrentSelected, true);
        }
    }

    public void showPanel(int currentPanel) {
        switch (currentPanel) {
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

    public void setToggleVersionsPanelButton(ImageButton button) {
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (MainPanel.this.mCurrentSelected == 4) {
                        MainPanel.this.showPanel(MainPanel.this.mPreviousToggleVersions);
                        return;
                    }
                    MainPanel.this.mPreviousToggleVersions = MainPanel.this.mCurrentSelected;
                    MainPanel.this.showPanel(4);
                }
            });
        }
    }

    public void showImageStatePanel(boolean show) {
        View container = this.mMainView.findViewById(R.id.state_panel_container);
        if (container == null) {
            FilterShowActivity activity = (FilterShowActivity) getActivity();
            container = activity.getMainStatePanelContainer(R.id.state_panel_container);
        } else {
            getChildFragmentManager().beginTransaction();
        }
        if (container != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            int currentPanel = this.mCurrentSelected;
            if (show) {
                container.setVisibility(0);
                StatePanel statePanel = new StatePanel();
                statePanel.setMainPanel(this);
                FilterShowActivity activity2 = (FilterShowActivity) getActivity();
                activity2.updateVersions();
                transaction.replace(R.id.state_panel_container, statePanel, "StatePanel");
            } else {
                container.setVisibility(8);
                Fragment statePanel2 = getChildFragmentManager().findFragmentByTag("StatePanel");
                if (statePanel2 != null) {
                    transaction.remove(statePanel2);
                }
                if (currentPanel == 4) {
                    currentPanel = 0;
                }
            }
            this.mCurrentSelected = -1;
            showPanel(currentPanel);
            transaction.commit();
        }
    }
}
