package android.app;

import android.os.Bundle;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import com.android.internal.R;

@Deprecated
public class TabActivity extends ActivityGroup {
    private String mDefaultTab = null;
    private int mDefaultTabIndex = -1;
    private TabHost mTabHost;

    public void setDefaultTab(String str) {
        this.mDefaultTab = str;
        this.mDefaultTabIndex = -1;
    }

    public void setDefaultTab(int i) {
        this.mDefaultTab = null;
        this.mDefaultTabIndex = i;
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        ensureTabHost();
        String string = bundle.getString("currentTab");
        if (string != null) {
            this.mTabHost.setCurrentTabByTag(string);
        }
        if (this.mTabHost.getCurrentTab() < 0) {
            if (this.mDefaultTab != null) {
                this.mTabHost.setCurrentTabByTag(this.mDefaultTab);
            } else if (this.mDefaultTabIndex >= 0) {
                this.mTabHost.setCurrentTab(this.mDefaultTabIndex);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);
        ensureTabHost();
        if (this.mTabHost.getCurrentTab() == -1) {
            this.mTabHost.setCurrentTab(0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        String currentTabTag = this.mTabHost.getCurrentTabTag();
        if (currentTabTag != null) {
            bundle.putString("currentTab", currentTabTag);
        }
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        this.mTabHost = (TabHost) findViewById(16908306);
        if (this.mTabHost == null) {
            throw new RuntimeException("Your content must have a TabHost whose id attribute is 'android.R.id.tabhost'");
        }
        this.mTabHost.setup(getLocalActivityManager());
    }

    private void ensureTabHost() {
        if (this.mTabHost == null) {
            setContentView(R.layout.tab_content);
        }
    }

    @Override
    protected void onChildTitleChanged(Activity activity, CharSequence charSequence) {
        View currentTabView;
        if (getLocalActivityManager().getCurrentActivity() == activity && (currentTabView = this.mTabHost.getCurrentTabView()) != null && (currentTabView instanceof TextView)) {
            ((TextView) currentTabView).setText(charSequence);
        }
    }

    public TabHost getTabHost() {
        ensureTabHost();
        return this.mTabHost;
    }

    public TabWidget getTabWidget() {
        return this.mTabHost.getTabWidget();
    }
}
