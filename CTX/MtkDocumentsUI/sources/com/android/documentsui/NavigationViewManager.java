package com.android.documentsui;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.Toolbar;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.base.State;
import java.util.function.IntConsumer;

public class NavigationViewManager {
    private final Breadcrumb mBreadcrumb;
    private final DrawerController mDrawer;
    private final Environment mEnv;
    private final State mState;
    private final Toolbar mToolbar;

    interface Breadcrumb {
        void postUpdate();

        void setup(Environment environment, State state, IntConsumer intConsumer);

        void show(boolean z);
    }

    interface Environment {
        @Deprecated
        RootInfo getCurrentRoot();

        String getDrawerTitle();

        boolean isSearchExpanded();

        @Deprecated
        void refreshCurrentRootAndDirectory(int i);
    }

    public NavigationViewManager(DrawerController drawerController, Toolbar toolbar, State state, Environment environment, Breadcrumb breadcrumb) {
        this.mToolbar = toolbar;
        this.mDrawer = drawerController;
        this.mState = state;
        this.mEnv = environment;
        this.mBreadcrumb = breadcrumb;
        this.mBreadcrumb.setup(environment, state, new IntConsumer() {
            @Override
            public final void accept(int i) {
                this.f$0.onNavigationItemSelected(i);
            }
        });
        this.mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavigationViewManager.this.onNavigationIconClicked();
            }
        });
    }

    private void onNavigationIconClicked() {
        if (this.mDrawer.isPresent()) {
            this.mDrawer.setOpen(true);
        }
    }

    void onNavigationItemSelected(int i) {
        boolean z = false;
        while (this.mState.stack.size() > i + 1) {
            this.mState.stack.pop();
            z = true;
        }
        if (z) {
            this.mEnv.refreshCurrentRootAndDirectory(3);
        }
    }

    public void update() {
        if (this.mEnv.isSearchExpanded()) {
            this.mToolbar.setTitle((CharSequence) null);
            this.mBreadcrumb.show(false);
            return;
        }
        this.mDrawer.setTitle(this.mEnv.getDrawerTitle());
        this.mToolbar.setNavigationIcon(getActionBarIcon());
        this.mToolbar.setNavigationContentDescription(R.string.drawer_open);
        if (this.mState.stack.size() <= 1) {
            this.mBreadcrumb.show(false);
            String str = this.mEnv.getCurrentRoot().title;
            if (SharedMinimal.VERBOSE) {
                Log.v("NavigationViewManager", "New toolbar title is: " + str);
            }
            this.mToolbar.setTitle(str);
        } else {
            this.mBreadcrumb.show(true);
            this.mToolbar.setTitle((CharSequence) null);
            this.mBreadcrumb.postUpdate();
        }
        if (SharedMinimal.VERBOSE) {
            Log.v("NavigationViewManager", "Final toolbar title is: " + ((Object) this.mToolbar.getTitle()));
        }
    }

    private Drawable getActionBarIcon() {
        if (this.mDrawer.isPresent()) {
            return this.mToolbar.getContext().getDrawable(R.drawable.ic_hamburger);
        }
        return null;
    }

    void revealRootsDrawer(boolean z) {
        this.mDrawer.setOpen(z);
    }
}
