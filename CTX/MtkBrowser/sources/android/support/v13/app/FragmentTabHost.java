package android.support.v13.app;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.TabHost;
import java.util.ArrayList;

@Deprecated
public class FragmentTabHost extends TabHost implements TabHost.OnTabChangeListener {
    private boolean mAttached;
    private int mContainerId;
    private Context mContext;
    private FragmentManager mFragmentManager;
    private TabInfo mLastTab;
    private TabHost.OnTabChangeListener mOnTabChangeListener;
    private final ArrayList<TabInfo> mTabs;

    static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        String curTab;

        SavedState(Parcel parcel) {
            super(parcel);
            this.curTab = parcel.readString();
        }

        SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        public String toString() {
            return "FragmentTabHost.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " curTab=" + this.curTab + "}";
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeString(this.curTab);
        }
    }

    static final class TabInfo {
        final Bundle args;
        final Class<?> clss;
        Fragment fragment;
        final String tag;
    }

    private FragmentTransaction doTabChanged(String str, FragmentTransaction fragmentTransaction) {
        TabInfo tabInfo = null;
        int i = 0;
        while (i < this.mTabs.size()) {
            TabInfo tabInfo2 = this.mTabs.get(i);
            if (!tabInfo2.tag.equals(str)) {
                tabInfo2 = tabInfo;
            }
            i++;
            tabInfo = tabInfo2;
        }
        if (tabInfo == null) {
            throw new IllegalStateException("No tab known for tag " + str);
        }
        if (this.mLastTab != tabInfo) {
            if (fragmentTransaction == null) {
                fragmentTransaction = this.mFragmentManager.beginTransaction();
            }
            if (this.mLastTab != null && this.mLastTab.fragment != null) {
                fragmentTransaction.detach(this.mLastTab.fragment);
            }
            if (tabInfo != null) {
                if (tabInfo.fragment == null) {
                    tabInfo.fragment = Fragment.instantiate(this.mContext, tabInfo.clss.getName(), tabInfo.args);
                    fragmentTransaction.add(this.mContainerId, tabInfo.fragment, tabInfo.tag);
                } else {
                    fragmentTransaction.attach(tabInfo.fragment);
                }
            }
            this.mLastTab = tabInfo;
        }
        return fragmentTransaction;
    }

    @Override
    @Deprecated
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        String currentTabTag = getCurrentTabTag();
        FragmentTransaction fragmentTransactionBeginTransaction = null;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= this.mTabs.size()) {
                break;
            }
            TabInfo tabInfo = this.mTabs.get(i2);
            tabInfo.fragment = this.mFragmentManager.findFragmentByTag(tabInfo.tag);
            if (tabInfo.fragment != null && !tabInfo.fragment.isDetached()) {
                if (tabInfo.tag.equals(currentTabTag)) {
                    this.mLastTab = tabInfo;
                } else {
                    if (fragmentTransactionBeginTransaction == null) {
                        fragmentTransactionBeginTransaction = this.mFragmentManager.beginTransaction();
                    }
                    fragmentTransactionBeginTransaction.detach(tabInfo.fragment);
                }
            }
            i = i2 + 1;
        }
        this.mAttached = true;
        FragmentTransaction fragmentTransactionDoTabChanged = doTabChanged(currentTabTag, fragmentTransactionBeginTransaction);
        if (fragmentTransactionDoTabChanged != null) {
            fragmentTransactionDoTabChanged.commit();
            this.mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    @Deprecated
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mAttached = false;
    }

    @Override
    @Deprecated
    protected void onRestoreInstanceState(Parcelable parcelable) {
        if (!(parcelable instanceof SavedState)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        setCurrentTabByTag(savedState.curTab);
    }

    @Override
    @Deprecated
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.curTab = getCurrentTabTag();
        return savedState;
    }

    @Override
    @Deprecated
    public void onTabChanged(String str) {
        FragmentTransaction fragmentTransactionDoTabChanged;
        if (this.mAttached && (fragmentTransactionDoTabChanged = doTabChanged(str, null)) != null) {
            fragmentTransactionDoTabChanged.commit();
        }
        if (this.mOnTabChangeListener != null) {
            this.mOnTabChangeListener.onTabChanged(str);
        }
    }

    @Override
    @Deprecated
    public void setOnTabChangedListener(TabHost.OnTabChangeListener onTabChangeListener) {
        this.mOnTabChangeListener = onTabChangeListener;
    }

    @Override
    @Deprecated
    public void setup() {
        throw new IllegalStateException("Must call setup() that takes a Context and FragmentManager");
    }
}
