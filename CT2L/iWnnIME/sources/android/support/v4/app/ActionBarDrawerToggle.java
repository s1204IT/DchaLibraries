package android.support.v4.app;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;

public class ActionBarDrawerToggle implements DrawerLayout.DrawerListener {
    private static final int ID_HOME = 16908332;
    private static final ActionBarDrawerToggleImpl IMPL;
    private final Activity mActivity;
    private final Delegate mActivityImpl;
    private final int mCloseDrawerContentDescRes;
    private Drawable mDrawerImage;
    private final int mDrawerImageResource;
    private final DrawerLayout mDrawerLayout;
    private final int mOpenDrawerContentDescRes;
    private Object mSetIndicatorInfo;
    private SlideDrawable mSlider;
    private boolean mDrawerIndicatorEnabled = true;
    private Drawable mThemeImage = getThemeUpIndicator();

    private interface ActionBarDrawerToggleImpl {
        Drawable getThemeUpIndicator(Activity activity);

        Object setActionBarDescription(Object obj, Activity activity, int i);

        Object setActionBarUpIndicator(Object obj, Activity activity, Drawable drawable, int i);
    }

    public interface Delegate {
        Drawable getThemeUpIndicator();

        void setActionBarDescription(int i);

        void setActionBarUpIndicator(Drawable drawable, int i);
    }

    public interface DelegateProvider {
        Delegate getDrawerToggleDelegate();
    }

    private static class ActionBarDrawerToggleImplBase implements ActionBarDrawerToggleImpl {
        private ActionBarDrawerToggleImplBase() {
        }

        @Override
        public Drawable getThemeUpIndicator(Activity activity) {
            return null;
        }

        @Override
        public Object setActionBarUpIndicator(Object info, Activity activity, Drawable themeImage, int contentDescRes) {
            return info;
        }

        @Override
        public Object setActionBarDescription(Object info, Activity activity, int contentDescRes) {
            return info;
        }
    }

    private static class ActionBarDrawerToggleImplHC implements ActionBarDrawerToggleImpl {
        private ActionBarDrawerToggleImplHC() {
        }

        @Override
        public Drawable getThemeUpIndicator(Activity activity) {
            return ActionBarDrawerToggleHoneycomb.getThemeUpIndicator(activity);
        }

        @Override
        public Object setActionBarUpIndicator(Object info, Activity activity, Drawable themeImage, int contentDescRes) {
            return ActionBarDrawerToggleHoneycomb.setActionBarUpIndicator(info, activity, themeImage, contentDescRes);
        }

        @Override
        public Object setActionBarDescription(Object info, Activity activity, int contentDescRes) {
            return ActionBarDrawerToggleHoneycomb.setActionBarDescription(info, activity, contentDescRes);
        }
    }

    static {
        int version = Build.VERSION.SDK_INT;
        if (version >= 11) {
            IMPL = new ActionBarDrawerToggleImplHC();
        } else {
            IMPL = new ActionBarDrawerToggleImplBase();
        }
    }

    public ActionBarDrawerToggle(Activity activity, DrawerLayout drawerLayout, int drawerImageRes, int openDrawerContentDescRes, int closeDrawerContentDescRes) {
        this.mActivity = activity;
        this.mDrawerLayout = drawerLayout;
        this.mDrawerImageResource = drawerImageRes;
        this.mOpenDrawerContentDescRes = openDrawerContentDescRes;
        this.mCloseDrawerContentDescRes = closeDrawerContentDescRes;
        this.mDrawerImage = activity.getResources().getDrawable(drawerImageRes);
        this.mSlider = new SlideDrawable(this.mDrawerImage);
        this.mSlider.setOffsetBy(0.33333334f);
        if (activity instanceof DelegateProvider) {
            this.mActivityImpl = ((DelegateProvider) activity).getDrawerToggleDelegate();
        } else {
            this.mActivityImpl = null;
        }
    }

    public void syncState() {
        if (this.mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.mSlider.setOffset(1.0f);
        } else {
            this.mSlider.setOffset(0.0f);
        }
        if (this.mDrawerIndicatorEnabled) {
            setActionBarUpIndicator(this.mSlider, this.mDrawerLayout.isDrawerOpen(GravityCompat.START) ? this.mOpenDrawerContentDescRes : this.mCloseDrawerContentDescRes);
        }
    }

    public void setDrawerIndicatorEnabled(boolean enable) {
        if (enable != this.mDrawerIndicatorEnabled) {
            if (enable) {
                setActionBarUpIndicator(this.mSlider, this.mDrawerLayout.isDrawerOpen(GravityCompat.START) ? this.mOpenDrawerContentDescRes : this.mCloseDrawerContentDescRes);
            } else {
                setActionBarUpIndicator(this.mThemeImage, 0);
            }
            this.mDrawerIndicatorEnabled = enable;
        }
    }

    public boolean isDrawerIndicatorEnabled() {
        return this.mDrawerIndicatorEnabled;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        this.mThemeImage = getThemeUpIndicator();
        this.mDrawerImage = this.mActivity.getResources().getDrawable(this.mDrawerImageResource);
        syncState();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null && item.getItemId() == 16908332 && this.mDrawerIndicatorEnabled) {
            if (this.mDrawerLayout.isDrawerVisible(GravityCompat.START)) {
                this.mDrawerLayout.closeDrawer(GravityCompat.START);
            } else {
                this.mDrawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
        float glyphOffset;
        float glyphOffset2 = this.mSlider.getOffset();
        if (slideOffset > 0.5f) {
            glyphOffset = Math.max(glyphOffset2, Math.max(0.0f, slideOffset - 0.5f) * 2.0f);
        } else {
            glyphOffset = Math.min(glyphOffset2, slideOffset * 2.0f);
        }
        this.mSlider.setOffset(glyphOffset);
    }

    @Override
    public void onDrawerOpened(View drawerView) {
        this.mSlider.setOffset(1.0f);
        if (this.mDrawerIndicatorEnabled) {
            setActionBarDescription(this.mOpenDrawerContentDescRes);
        }
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        this.mSlider.setOffset(0.0f);
        if (this.mDrawerIndicatorEnabled) {
            setActionBarDescription(this.mCloseDrawerContentDescRes);
        }
    }

    @Override
    public void onDrawerStateChanged(int newState) {
    }

    Drawable getThemeUpIndicator() {
        return this.mActivityImpl != null ? this.mActivityImpl.getThemeUpIndicator() : IMPL.getThemeUpIndicator(this.mActivity);
    }

    void setActionBarUpIndicator(Drawable upDrawable, int contentDescRes) {
        if (this.mActivityImpl != null) {
            this.mActivityImpl.setActionBarUpIndicator(upDrawable, contentDescRes);
        } else {
            this.mSetIndicatorInfo = IMPL.setActionBarUpIndicator(this.mSetIndicatorInfo, this.mActivity, upDrawable, contentDescRes);
        }
    }

    void setActionBarDescription(int contentDescRes) {
        if (this.mActivityImpl != null) {
            this.mActivityImpl.setActionBarDescription(contentDescRes);
        } else {
            this.mSetIndicatorInfo = IMPL.setActionBarDescription(this.mSetIndicatorInfo, this.mActivity, contentDescRes);
        }
    }

    private static class SlideDrawable extends Drawable implements Drawable.Callback {
        private float mOffset;
        private float mOffsetBy;
        private final Rect mTmpRect = new Rect();
        private Drawable mWrapped;

        public SlideDrawable(Drawable wrapped) {
            this.mWrapped = wrapped;
        }

        public void setOffset(float offset) {
            this.mOffset = offset;
            invalidateSelf();
        }

        public float getOffset() {
            return this.mOffset;
        }

        public void setOffsetBy(float offsetBy) {
            this.mOffsetBy = offsetBy;
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            this.mWrapped.copyBounds(this.mTmpRect);
            canvas.save();
            canvas.translate(this.mOffsetBy * this.mTmpRect.width() * (-this.mOffset), 0.0f);
            this.mWrapped.draw(canvas);
            canvas.restore();
        }

        @Override
        public void setChangingConfigurations(int configs) {
            this.mWrapped.setChangingConfigurations(configs);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mWrapped.getChangingConfigurations();
        }

        @Override
        public void setDither(boolean dither) {
            this.mWrapped.setDither(dither);
        }

        @Override
        public void setFilterBitmap(boolean filter) {
            this.mWrapped.setFilterBitmap(filter);
        }

        @Override
        public void setAlpha(int alpha) {
            this.mWrapped.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            this.mWrapped.setColorFilter(cf);
        }

        @Override
        public void setColorFilter(int color, PorterDuff.Mode mode) {
            this.mWrapped.setColorFilter(color, mode);
        }

        @Override
        public void clearColorFilter() {
            this.mWrapped.clearColorFilter();
        }

        @Override
        public boolean isStateful() {
            return this.mWrapped.isStateful();
        }

        @Override
        public boolean setState(int[] stateSet) {
            return this.mWrapped.setState(stateSet);
        }

        @Override
        public int[] getState() {
            return this.mWrapped.getState();
        }

        @Override
        public Drawable getCurrent() {
            return this.mWrapped.getCurrent();
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            return super.setVisible(visible, restart);
        }

        @Override
        public int getOpacity() {
            return this.mWrapped.getOpacity();
        }

        @Override
        public Region getTransparentRegion() {
            return this.mWrapped.getTransparentRegion();
        }

        @Override
        protected boolean onStateChange(int[] state) {
            this.mWrapped.setState(state);
            return super.onStateChange(state);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            this.mWrapped.setBounds(bounds);
        }

        @Override
        public int getIntrinsicWidth() {
            return this.mWrapped.getIntrinsicWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            return this.mWrapped.getIntrinsicHeight();
        }

        @Override
        public int getMinimumWidth() {
            return this.mWrapped.getMinimumWidth();
        }

        @Override
        public int getMinimumHeight() {
            return this.mWrapped.getMinimumHeight();
        }

        @Override
        public boolean getPadding(Rect padding) {
            return this.mWrapped.getPadding(padding);
        }

        @Override
        public Drawable.ConstantState getConstantState() {
            return super.getConstantState();
        }

        @Override
        public void invalidateDrawable(Drawable who) {
            if (who == this.mWrapped) {
                invalidateSelf();
            }
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            if (who == this.mWrapped) {
                scheduleSelf(what, when);
            }
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            if (who == this.mWrapped) {
                unscheduleSelf(what);
            }
        }
    }
}
