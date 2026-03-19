package com.android.documentsui;

import android.app.Activity;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toolbar;
import com.android.documentsui.ItemDragListener;
import com.android.documentsui.base.Display;
import com.android.documentsui.base.SharedMinimal;

public abstract class DrawerController implements DrawerLayout.DrawerListener {
    public static Activity mActivity;

    public abstract boolean isPresent();

    public abstract void setOpen(boolean z);

    abstract void setTitle(String str);

    public abstract void update();

    public static DrawerController create(Activity activity, ActivityConfig activityConfig) {
        DrawerLayout drawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
        mActivity = activity;
        if (drawerLayout == null) {
            return new DummyDrawerController();
        }
        View viewFindViewById = activity.findViewById(R.id.drawer_roots);
        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.roots_toolbar);
        viewFindViewById.getLayoutParams().width = calculateDrawerWidth(activity);
        return new RuntimeDrawerController(drawerLayout, viewFindViewById, new ActionBarDrawerToggle(activity, drawerLayout, R.drawable.ic_hamburger, R.string.drawer_open, R.string.drawer_close), toolbar, activityConfig);
    }

    private static int calculateDrawerWidth(Activity activity) {
        float fScreenWidth = Display.screenWidth(activity) - Display.actionBarHeight(activity);
        float dimension = activity.getResources().getDimension(R.dimen.max_drawer_width);
        if (fScreenWidth > dimension) {
            fScreenWidth = dimension;
        }
        int i = (int) fScreenWidth;
        if (SharedMinimal.DEBUG) {
            Log.d("DrawerController", "Calculated drawer width:" + (i / Display.density(activity)));
        }
        return i;
    }

    private static final class RuntimeDrawerController extends DrawerController implements ItemDragListener.DragHost {
        static final boolean $assertionsDisabled = false;
        private View mDrawer;
        private DrawerLayout mLayout;
        private final ActionBarDrawerToggle mToggle;
        private Toolbar mToolbar;

        public RuntimeDrawerController(DrawerLayout drawerLayout, View view, ActionBarDrawerToggle actionBarDrawerToggle, Toolbar toolbar, ActivityConfig activityConfig) {
            this.mToolbar = toolbar;
            this.mLayout = drawerLayout;
            this.mDrawer = view;
            this.mToggle = actionBarDrawerToggle;
            this.mLayout.setDrawerListener(this);
            if (activityConfig.dragAndDropEnabled()) {
                drawerLayout.findViewById(R.id.drawer_edge).setOnDragListener(new ItemDragListener(this, 750));
            }
        }

        @Override
        public void runOnUiThread(Runnable runnable) {
            this.mDrawer.post(runnable);
        }

        @Override
        public void setDropTargetHighlight(View view, boolean z) {
            view.setBackgroundColor(z ? R.color.item_doc_background_selected : android.R.color.transparent);
        }

        @Override
        public void onDragEntered(View view) {
        }

        @Override
        public void onDragExited(View view) {
        }

        @Override
        public void onViewHovered(View view) {
            setOpen(true);
        }

        @Override
        public void onDragEnded() {
        }

        @Override
        public void setOpen(boolean z) {
            if (z) {
                View currentFocus = mActivity.getCurrentFocus();
                if (currentFocus != null) {
                    ((InputMethodManager) mActivity.getSystemService("input_method")).hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }
                this.mLayout.openDrawer(this.mDrawer);
                return;
            }
            this.mLayout.closeDrawer(this.mDrawer);
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        void setTitle(String str) {
            this.mToolbar.setTitle(str);
        }

        @Override
        public void update() {
            this.mToggle.syncState();
        }

        @Override
        public void onDrawerSlide(View view, float f) {
            this.mToggle.onDrawerSlide(view, f);
        }

        @Override
        public void onDrawerOpened(View view) {
            this.mToggle.onDrawerOpened(view);
        }

        @Override
        public void onDrawerClosed(View view) {
            this.mToggle.onDrawerClosed(view);
        }

        @Override
        public void onDrawerStateChanged(int i) {
            this.mToggle.onDrawerStateChanged(i);
        }
    }

    private static final class DummyDrawerController extends DrawerController {
        private DummyDrawerController() {
        }

        @Override
        public void setOpen(boolean z) {
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        void setTitle(String str) {
        }

        @Override
        public void update() {
        }

        @Override
        public void onDrawerSlide(View view, float f) {
        }

        @Override
        public void onDrawerOpened(View view) {
        }

        @Override
        public void onDrawerClosed(View view) {
        }

        @Override
        public void onDrawerStateChanged(int i) {
        }
    }
}
