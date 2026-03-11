package android.support.v13.app;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

@Deprecated
public abstract class FragmentPagerAdapter extends PagerAdapter {
    private FragmentTransaction mCurTransaction = null;
    private Fragment mCurrentPrimaryItem = null;
    private final FragmentManager mFragmentManager;

    @Deprecated
    public FragmentPagerAdapter(FragmentManager fragmentManager) {
        this.mFragmentManager = fragmentManager;
    }

    private static String makeFragmentName(int i, long j) {
        return "android:switcher:" + i + ":" + j;
    }

    @Override
    @Deprecated
    public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
        if (this.mCurTransaction == null) {
            this.mCurTransaction = this.mFragmentManager.beginTransaction();
        }
        this.mCurTransaction.detach((Fragment) obj);
    }

    @Override
    @Deprecated
    public void finishUpdate(ViewGroup viewGroup) {
        if (this.mCurTransaction != null) {
            this.mCurTransaction.commitAllowingStateLoss();
            this.mCurTransaction = null;
            this.mFragmentManager.executePendingTransactions();
        }
    }

    @Deprecated
    public abstract Fragment getItem(int i);

    @Deprecated
    public long getItemId(int i) {
        return i;
    }

    @Override
    @Deprecated
    public Object instantiateItem(ViewGroup viewGroup, int i) {
        if (this.mCurTransaction == null) {
            this.mCurTransaction = this.mFragmentManager.beginTransaction();
        }
        long itemId = getItemId(i);
        Fragment fragmentFindFragmentByTag = this.mFragmentManager.findFragmentByTag(makeFragmentName(viewGroup.getId(), itemId));
        if (fragmentFindFragmentByTag != null) {
            this.mCurTransaction.attach(fragmentFindFragmentByTag);
        } else {
            fragmentFindFragmentByTag = getItem(i);
            this.mCurTransaction.add(viewGroup.getId(), fragmentFindFragmentByTag, makeFragmentName(viewGroup.getId(), itemId));
        }
        if (fragmentFindFragmentByTag != this.mCurrentPrimaryItem) {
            fragmentFindFragmentByTag.setMenuVisibility(false);
            FragmentCompat.setUserVisibleHint(fragmentFindFragmentByTag, false);
        }
        return fragmentFindFragmentByTag;
    }

    @Override
    @Deprecated
    public boolean isViewFromObject(View view, Object obj) {
        return ((Fragment) obj).getView() == view;
    }

    @Override
    @Deprecated
    public void restoreState(Parcelable parcelable, ClassLoader classLoader) {
    }

    @Override
    @Deprecated
    public Parcelable saveState() {
        return null;
    }

    @Override
    @Deprecated
    public void setPrimaryItem(ViewGroup viewGroup, int i, Object obj) {
        Fragment fragment = (Fragment) obj;
        if (fragment != this.mCurrentPrimaryItem) {
            if (this.mCurrentPrimaryItem != null) {
                this.mCurrentPrimaryItem.setMenuVisibility(false);
                FragmentCompat.setUserVisibleHint(this.mCurrentPrimaryItem, false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
                FragmentCompat.setUserVisibleHint(fragment, true);
            }
            this.mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    @Deprecated
    public void startUpdate(ViewGroup viewGroup) {
        if (viewGroup.getId() != -1) {
            return;
        }
        throw new IllegalStateException("ViewPager with adapter " + this + " requires a view id");
    }
}
