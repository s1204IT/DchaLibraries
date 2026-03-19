package com.android.deskclock;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v13.app.FragmentCompat;
import android.support.v4.view.PagerAdapter;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import com.android.deskclock.uidata.UiDataModel;
import java.util.Map;

final class FragmentTabPagerAdapter extends PagerAdapter {
    private Fragment mCurrentPrimaryItem;
    private FragmentTransaction mCurrentTransaction;
    private final DeskClock mDeskClock;
    private final Map<UiDataModel.Tab, DeskClockFragment> mFragmentCache = new ArrayMap(getCount());
    private final FragmentManager mFragmentManager;

    FragmentTabPagerAdapter(DeskClock deskClock) {
        this.mDeskClock = deskClock;
        this.mFragmentManager = deskClock.getFragmentManager();
    }

    @Override
    public int getCount() {
        return UiDataModel.getUiDataModel().getTabCount();
    }

    DeskClockFragment getDeskClockFragment(int i) {
        UiDataModel.Tab tabAt = UiDataModel.getUiDataModel().getTabAt(i);
        DeskClockFragment deskClockFragment = this.mFragmentCache.get(tabAt);
        if (deskClockFragment != null) {
            return deskClockFragment;
        }
        DeskClockFragment deskClockFragment2 = (DeskClockFragment) this.mFragmentManager.findFragmentByTag(tabAt.name());
        if (deskClockFragment2 != null) {
            deskClockFragment2.setFabContainer(this.mDeskClock);
            this.mFragmentCache.put(tabAt, deskClockFragment2);
            return deskClockFragment2;
        }
        DeskClockFragment deskClockFragment3 = (DeskClockFragment) Fragment.instantiate(this.mDeskClock, tabAt.getFragmentClassName());
        deskClockFragment3.setFabContainer(this.mDeskClock);
        this.mFragmentCache.put(tabAt, deskClockFragment3);
        return deskClockFragment3;
    }

    @Override
    public void startUpdate(ViewGroup viewGroup) {
        if (viewGroup.getId() == -1) {
            throw new IllegalStateException("ViewPager with adapter " + this + " has no id");
        }
    }

    @Override
    public Object instantiateItem(ViewGroup viewGroup, int i) {
        if (this.mCurrentTransaction == null) {
            this.mCurrentTransaction = this.mFragmentManager.beginTransaction();
        }
        UiDataModel.Tab tabAt = UiDataModel.getUiDataModel().getTabAt(i);
        Fragment fragmentFindFragmentByTag = this.mFragmentManager.findFragmentByTag(tabAt.name());
        if (fragmentFindFragmentByTag != null) {
            this.mCurrentTransaction.attach(fragmentFindFragmentByTag);
        } else {
            fragmentFindFragmentByTag = getDeskClockFragment(i);
            this.mCurrentTransaction.add(viewGroup.getId(), fragmentFindFragmentByTag, tabAt.name());
        }
        if (fragmentFindFragmentByTag != this.mCurrentPrimaryItem) {
            FragmentCompat.setMenuVisibility(fragmentFindFragmentByTag, false);
            FragmentCompat.setUserVisibleHint(fragmentFindFragmentByTag, false);
        }
        return fragmentFindFragmentByTag;
    }

    @Override
    public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
        if (this.mCurrentTransaction == null) {
            this.mCurrentTransaction = this.mFragmentManager.beginTransaction();
        }
        DeskClockFragment deskClockFragment = (DeskClockFragment) obj;
        deskClockFragment.setFabContainer(null);
        this.mCurrentTransaction.detach(deskClockFragment);
    }

    @Override
    public void setPrimaryItem(ViewGroup viewGroup, int i, Object obj) {
        Fragment fragment = (Fragment) obj;
        if (fragment != this.mCurrentPrimaryItem) {
            if (this.mCurrentPrimaryItem != null) {
                FragmentCompat.setMenuVisibility(this.mCurrentPrimaryItem, false);
                FragmentCompat.setUserVisibleHint(this.mCurrentPrimaryItem, false);
            }
            if (fragment != null) {
                FragmentCompat.setMenuVisibility(fragment, true);
                FragmentCompat.setUserVisibleHint(fragment, true);
            }
            this.mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(ViewGroup viewGroup) {
        if (this.mCurrentTransaction != null) {
            this.mCurrentTransaction.commitAllowingStateLoss();
            this.mCurrentTransaction = null;
            this.mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object obj) {
        return ((Fragment) obj).getView() == view;
    }
}
