package com.android.deskclock;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.PopupMenuCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;

public class DeskClockFragment extends Fragment {
    protected ImageButton mFab;
    protected ImageButton mLeftButton;
    protected ImageButton mRightButton;

    public void onPageChanged(int page) {
    }

    public void onFabClick(View view) {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        if (activity instanceof DeskClock) {
            DeskClock deskClockActivity = (DeskClock) activity;
            this.mFab = deskClockActivity.getFab();
            this.mLeftButton = deskClockActivity.getLeftButton();
            this.mRightButton = deskClockActivity.getRightButton();
        }
    }

    public void setFabAppearance() {
    }

    public void setLeftRightButtonAppearance() {
    }

    public void onLeftButtonClick(View view) {
    }

    public void onRightButtonClick(View view) {
    }

    public void setupFakeOverflowMenuButton(View menuButton) {
        final PopupMenu fakeOverflow = new PopupMenu(menuButton.getContext(), menuButton) {
            @Override
            public void show() {
                DeskClockFragment.this.getActivity().onPrepareOptionsMenu(getMenu());
                super.show();
            }
        };
        fakeOverflow.inflate(R.menu.desk_clock_menu);
        fakeOverflow.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return DeskClockFragment.this.getActivity().onOptionsItemSelected(item);
            }
        });
        menuButton.setOnTouchListener(PopupMenuCompat.getDragToOpenListener(fakeOverflow));
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fakeOverflow.show();
            }
        });
    }
}
