package android.app;

import android.R;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

@Deprecated
public class TabActivity extends ActivityGroup {
    private String mDefaultTab = null;
    private int mDefaultTabIndex = -1;
    private TabHost mTabHost;

    public void setDefaultTab(String tag) {
        this.mDefaultTab = tag;
        this.mDefaultTabIndex = -1;
    }

    public void setDefaultTab(int index) {
        this.mDefaultTab = null;
        this.mDefaultTabIndex = index;
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        ensureTabHost();
        String cur = state.getString("currentTab");
        if (cur != null) {
            this.mTabHost.setCurrentTabByTag(cur);
        }
        if (this.mTabHost.getCurrentTab() >= 0) {
            return;
        }
        if (this.mDefaultTab != null) {
            this.mTabHost.setCurrentTabByTag(this.mDefaultTab);
        } else {
            if (this.mDefaultTabIndex < 0) {
                return;
            }
            this.mTabHost.setCurrentTab(this.mDefaultTabIndex);
        }
    }

    @Override
    protected void onPostCreate(Bundle icicle) {
        super.onPostCreate(icicle);
        ensureTabHost();
        if (this.mTabHost.getCurrentTab() != -1) {
            return;
        }
        this.mTabHost.setCurrentTab(0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String currentTabTag = this.mTabHost.getCurrentTabTag();
        if (currentTabTag == null) {
            return;
        }
        outState.putString("currentTab", currentTabTag);
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        this.mTabHost = (TabHost) findViewById(R.id.tabhost);
        if (this.mTabHost == null) {
            throw new RuntimeException("Your content must have a TabHost whose id attribute is 'android.R.id.tabhost'");
        }
        this.mTabHost.setup(getLocalActivityManager());
    }

    private void ensureTabHost() {
        if (this.mTabHost != null) {
            return;
        }
        setContentView(17367274);
    }

    @Override
    protected void onChildTitleChanged(Activity childActivity, CharSequence title) {
        ?? currentTabView;
        if (getLocalActivityManager().getCurrentActivity() != childActivity || (currentTabView = this.mTabHost.getCurrentTabView()) == 0 || !(currentTabView instanceof TextView)) {
            return;
        }
        currentTabView.setText(title);
    }

    public TabHost getTabHost() {
        ensureTabHost();
        return this.mTabHost;
    }

    public TabWidget getTabWidget() {
        return this.mTabHost.getTabWidget();
    }
}
