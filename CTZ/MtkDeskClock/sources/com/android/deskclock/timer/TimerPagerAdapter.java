package com.android.deskclock.timer;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v13.app.FragmentCompat;
import android.support.v4.view.PagerAdapter;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.data.TimerListener;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class TimerPagerAdapter extends PagerAdapter implements TimerListener {
    private Fragment mCurrentPrimaryItem;
    private FragmentTransaction mCurrentTransaction;
    private final FragmentManager mFragmentManager;
    private final Map<Integer, TimerItemFragment> mFragments = new ArrayMap();

    public TimerPagerAdapter(FragmentManager fragmentManager) {
        this.mFragmentManager = fragmentManager;
    }

    @Override
    public int getCount() {
        return getTimers().size();
    }

    @Override
    public boolean isViewFromObject(View view, Object obj) {
        return ((Fragment) obj).getView() == view;
    }

    @Override
    public int getItemPosition(Object obj) {
        int iIndexOf = getTimers().indexOf(((TimerItemFragment) obj).getTimer());
        if (iIndexOf == -1) {
            return -2;
        }
        return iIndexOf;
    }

    @Override
    @SuppressLint({"CommitTransaction"})
    public Fragment instantiateItem(ViewGroup viewGroup, int i) {
        if (this.mCurrentTransaction == null) {
            this.mCurrentTransaction = this.mFragmentManager.beginTransaction();
        }
        Timer timer = getTimers().get(i);
        String str = getClass().getSimpleName() + timer.getId();
        TimerItemFragment timerItemFragmentNewInstance = (TimerItemFragment) this.mFragmentManager.findFragmentByTag(str);
        if (timerItemFragmentNewInstance != null) {
            this.mCurrentTransaction.attach(timerItemFragmentNewInstance);
        } else {
            timerItemFragmentNewInstance = TimerItemFragment.newInstance(timer);
            this.mCurrentTransaction.add(viewGroup.getId(), timerItemFragmentNewInstance, str);
        }
        if (timerItemFragmentNewInstance != this.mCurrentPrimaryItem) {
            setItemVisible(timerItemFragmentNewInstance, false);
        }
        this.mFragments.put(Integer.valueOf(timer.getId()), timerItemFragmentNewInstance);
        return timerItemFragmentNewInstance;
    }

    @Override
    @SuppressLint({"CommitTransaction"})
    public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
        TimerItemFragment timerItemFragment = (TimerItemFragment) obj;
        if (this.mCurrentTransaction == null) {
            this.mCurrentTransaction = this.mFragmentManager.beginTransaction();
        }
        this.mFragments.remove(Integer.valueOf(timerItemFragment.getTimerId()));
        this.mCurrentTransaction.remove(timerItemFragment);
    }

    @Override
    public void setPrimaryItem(ViewGroup viewGroup, int i, Object obj) {
        Fragment fragment = (Fragment) obj;
        if (fragment != this.mCurrentPrimaryItem) {
            if (this.mCurrentPrimaryItem != null) {
                setItemVisible(this.mCurrentPrimaryItem, false);
            }
            this.mCurrentPrimaryItem = fragment;
            if (this.mCurrentPrimaryItem != null) {
                setItemVisible(this.mCurrentPrimaryItem, true);
            }
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
    public void timerAdded(Timer timer) {
        notifyDataSetChanged();
    }

    @Override
    public void timerRemoved(Timer timer) {
        notifyDataSetChanged();
    }

    @Override
    public void timerUpdated(Timer timer, Timer timer2) {
        TimerItemFragment timerItemFragment = this.mFragments.get(Integer.valueOf(timer2.getId()));
        if (timerItemFragment != null) {
            timerItemFragment.updateTime();
        }
    }

    boolean updateTime() {
        Iterator<TimerItemFragment> it = this.mFragments.values().iterator();
        boolean zUpdateTime = false;
        while (it.hasNext()) {
            zUpdateTime |= it.next().updateTime();
        }
        return zUpdateTime;
    }

    Timer getTimer(int i) {
        return getTimers().get(i);
    }

    private List<Timer> getTimers() {
        return DataModel.getDataModel().getTimers();
    }

    private static void setItemVisible(Fragment fragment, boolean z) {
        FragmentCompat.setMenuVisibility(fragment, z);
        FragmentCompat.setUserVisibleHint(fragment, z);
    }
}
