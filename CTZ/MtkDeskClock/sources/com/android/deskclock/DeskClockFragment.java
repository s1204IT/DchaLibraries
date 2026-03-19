package com.android.deskclock;

import android.app.Fragment;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.ImageView;
import com.android.deskclock.FabContainer;
import com.android.deskclock.uidata.UiDataModel;

public abstract class DeskClockFragment extends Fragment implements FabContainer, FabController {
    private FabContainer mFabContainer;
    private final UiDataModel.Tab mTab;

    public DeskClockFragment(UiDataModel.Tab tab) {
        this.mTab = tab;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isTabSelected()) {
            updateFab(9);
        }
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public void onLeftButtonClick(@NonNull Button button) {
    }

    @Override
    public void onRightButtonClick(@NonNull Button button) {
    }

    @Override
    public void onMorphFab(@NonNull ImageView imageView) {
    }

    protected void onAppColorChanged(@ColorInt int i) {
    }

    public final void setFabContainer(FabContainer fabContainer) {
        this.mFabContainer = fabContainer;
    }

    @Override
    public final void updateFab(@FabContainer.UpdateFabFlag int i) {
        if (this.mFabContainer != null) {
            this.mFabContainer.updateFab(i);
        }
    }

    public final boolean isTabSelected() {
        return UiDataModel.getUiDataModel().getSelectedTab() == this.mTab;
    }

    public final void selectTab() {
        UiDataModel.getUiDataModel().setSelectedTab(this.mTab);
    }

    public final void setTabScrolledToTop(boolean z) {
        UiDataModel.getUiDataModel().setTabScrolledToTop(this.mTab, z);
    }
}
