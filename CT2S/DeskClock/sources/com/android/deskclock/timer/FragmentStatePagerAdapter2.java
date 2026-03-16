package com.android.deskclock.timer;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v13.app.FragmentCompat;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

public abstract class FragmentStatePagerAdapter2 extends PagerAdapter {
    private final FragmentManager mFragmentManager;
    private FragmentTransaction mCurTransaction = null;
    private SparseArrayCompat<Fragment> mFragments = new SparseArrayCompat<>();
    private Fragment mCurrentPrimaryItem = null;

    public abstract Fragment getItem(int i);

    public FragmentStatePagerAdapter2(FragmentManager fm) {
        this.mFragmentManager = fm;
    }

    @Override
    public void startUpdate(ViewGroup container) {
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment existing = this.mFragments.get(position);
        if (existing == null) {
            if (this.mCurTransaction == null) {
                this.mCurTransaction = this.mFragmentManager.beginTransaction();
            }
            Fragment fragment = getItem(position);
            if (fragment != this.mCurrentPrimaryItem) {
                setItemVisible(fragment, false);
            }
            this.mFragments.put(position, fragment);
            this.mCurTransaction.add(container.getId(), fragment);
            return fragment;
        }
        return existing;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment) object;
        if (this.mCurTransaction == null) {
            this.mCurTransaction = this.mFragmentManager.beginTransaction();
        }
        this.mFragments.delete(position);
        this.mCurTransaction.remove(fragment);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment) object;
        if (fragment != this.mCurrentPrimaryItem) {
            if (this.mCurrentPrimaryItem != null) {
                setItemVisible(this.mCurrentPrimaryItem, false);
            }
            if (fragment != null) {
                setItemVisible(fragment, true);
            }
            this.mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (this.mCurTransaction != null) {
            this.mCurTransaction.commitAllowingStateLoss();
            this.mCurTransaction = null;
            this.mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment) object).getView() == view;
    }

    public void setItemVisible(Fragment item, boolean visible) {
        FragmentCompat.setMenuVisibility(item, visible);
        FragmentCompat.setUserVisibleHint(item, visible);
    }

    @Override
    public void notifyDataSetChanged() {
        SparseArrayCompat<Fragment> newFragments = new SparseArrayCompat<>(this.mFragments.size());
        for (int i = 0; i < this.mFragments.size(); i++) {
            int oldPos = this.mFragments.keyAt(i);
            Fragment f = this.mFragments.valueAt(i);
            int newPos = getItemPosition(f);
            if (newPos != -2) {
                int pos = newPos >= 0 ? newPos : oldPos;
                newFragments.put(pos, f);
            }
        }
        this.mFragments = newFragments;
        super.notifyDataSetChanged();
    }
}
