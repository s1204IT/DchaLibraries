package com.android.systemui.recent;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.TimeInterpolator;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.TaskStackBuilder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewRootImpl;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.statusbar.StatusBarPanel;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import java.util.ArrayList;

public class RecentsPanelView extends FrameLayout implements Animator.AnimatorListener, AdapterView.OnItemClickListener, RecentsCallback, StatusBarPanel {
    static final boolean DEBUG;
    private boolean mAnimateIconOfFirstTask;
    private boolean mCallUiHiddenBeforeNextReload;
    private boolean mFitThumbnailToXY;
    private boolean mHighEndGfx;
    private ViewHolder mItemToAnimateInWhenWindowAnimationIsFinished;
    private TaskDescriptionAdapter mListAdapter;
    private PopupMenu mPopup;
    private int mRecentItemLayoutId;
    private ArrayList<TaskDescription> mRecentTaskDescriptions;
    private RecentTasksLoader mRecentTasksLoader;
    private RecentsScrollView mRecentsContainer;
    private View mRecentsNoApps;
    private View mRecentsScrim;
    private boolean mShowing;
    private int mThumbnailWidth;
    private boolean mWaitingForWindowAnimation;
    private boolean mWaitingToShow;
    private long mWindowAnimationStartTime;

    public interface RecentsScrollView {
        void drawFadedEdges(Canvas canvas, int i, int i2, int i3, int i4);

        View findViewForTask(int i);

        int numItemsInOneScreenful();

        void setAdapter(TaskDescriptionAdapter taskDescriptionAdapter);

        void setCallback(RecentsCallback recentsCallback);

        void setMinSwipeAlpha(float f);

        void setOnScrollListener(Runnable runnable);
    }

    static {
        DEBUG = PhoneStatusBar.DEBUG;
    }

    private final class OnLongClickDelegate implements View.OnLongClickListener {
        View mOtherView;

        OnLongClickDelegate(View other) {
            this.mOtherView = other;
        }

        @Override
        public boolean onLongClick(View v) {
            return this.mOtherView.performLongClick();
        }
    }

    static final class ViewHolder {
        View calloutLine;
        TextView descriptionView;
        ImageView iconView;
        TextView labelView;
        boolean loadedThumbnailAndIcon;
        TaskDescription taskDescription;
        View thumbnailView;
        Drawable thumbnailViewDrawable;
        ImageView thumbnailViewImage;

        ViewHolder() {
        }
    }

    final class TaskDescriptionAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public TaskDescriptionAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            if (RecentsPanelView.this.mRecentTaskDescriptions != null) {
                return RecentsPanelView.this.mRecentTaskDescriptions.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return Integer.valueOf(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public View createView(ViewGroup parent) {
            View convertView = this.mInflater.inflate(RecentsPanelView.this.mRecentItemLayoutId, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.thumbnailView = convertView.findViewById(R.id.app_thumbnail);
            holder.thumbnailViewImage = (ImageView) convertView.findViewById(R.id.app_thumbnail_image);
            RecentsPanelView.this.updateThumbnail(holder, RecentsPanelView.this.mRecentTasksLoader.getDefaultThumbnail(), false, false);
            holder.iconView = (ImageView) convertView.findViewById(R.id.app_icon);
            holder.iconView.setImageDrawable(RecentsPanelView.this.mRecentTasksLoader.getDefaultIcon());
            holder.labelView = (TextView) convertView.findViewById(R.id.app_label);
            holder.calloutLine = convertView.findViewById(R.id.recents_callout_line);
            holder.descriptionView = (TextView) convertView.findViewById(R.id.app_description);
            convertView.setTag(holder);
            return convertView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = createView(parent);
            }
            ViewHolder holder = (ViewHolder) convertView.getTag();
            int index = (RecentsPanelView.this.mRecentTaskDescriptions.size() - position) - 1;
            TaskDescription td = (TaskDescription) RecentsPanelView.this.mRecentTaskDescriptions.get(index);
            holder.labelView.setText(td.getLabel());
            holder.thumbnailView.setContentDescription(td.getLabel());
            holder.loadedThumbnailAndIcon = td.isLoaded();
            if (td.isLoaded()) {
                RecentsPanelView.this.updateThumbnail(holder, td.getThumbnail(), true, false);
                RecentsPanelView.this.updateIcon(holder, td.getIcon(), true, false);
            }
            if (index == 0 && RecentsPanelView.this.mAnimateIconOfFirstTask) {
                ViewHolder oldHolder = RecentsPanelView.this.mItemToAnimateInWhenWindowAnimationIsFinished;
                if (oldHolder != null) {
                    oldHolder.iconView.setAlpha(1.0f);
                    oldHolder.iconView.setTranslationX(0.0f);
                    oldHolder.iconView.setTranslationY(0.0f);
                    oldHolder.labelView.setAlpha(1.0f);
                    oldHolder.labelView.setTranslationX(0.0f);
                    oldHolder.labelView.setTranslationY(0.0f);
                    if (oldHolder.calloutLine != null) {
                        oldHolder.calloutLine.setAlpha(1.0f);
                        oldHolder.calloutLine.setTranslationX(0.0f);
                        oldHolder.calloutLine.setTranslationY(0.0f);
                    }
                }
                RecentsPanelView.this.mItemToAnimateInWhenWindowAnimationIsFinished = holder;
                int translation = -RecentsPanelView.this.getResources().getDimensionPixelSize(R.dimen.status_bar_recents_app_icon_translate_distance);
                Configuration config = RecentsPanelView.this.getResources().getConfiguration();
                if (config.orientation == 1) {
                    if (RecentsPanelView.this.getLayoutDirection() == 1) {
                        translation = -translation;
                    }
                    holder.iconView.setAlpha(0.0f);
                    holder.iconView.setTranslationX(translation);
                    holder.labelView.setAlpha(0.0f);
                    holder.labelView.setTranslationX(translation);
                    holder.calloutLine.setAlpha(0.0f);
                    holder.calloutLine.setTranslationX(translation);
                } else {
                    holder.iconView.setAlpha(0.0f);
                    holder.iconView.setTranslationY(translation);
                }
                if (!RecentsPanelView.this.mWaitingForWindowAnimation) {
                    RecentsPanelView.this.animateInIconOfFirstTask();
                }
            }
            holder.thumbnailView.setTag(td);
            holder.thumbnailView.setOnLongClickListener(RecentsPanelView.this.new OnLongClickDelegate(convertView));
            holder.taskDescription = td;
            return convertView;
        }

        public void recycleView(View v) {
            ViewHolder holder = (ViewHolder) v.getTag();
            RecentsPanelView.this.updateThumbnail(holder, RecentsPanelView.this.mRecentTasksLoader.getDefaultThumbnail(), false, false);
            holder.iconView.setImageDrawable(RecentsPanelView.this.mRecentTasksLoader.getDefaultIcon());
            holder.iconView.setVisibility(4);
            holder.iconView.animate().cancel();
            holder.labelView.setText((CharSequence) null);
            holder.labelView.animate().cancel();
            holder.thumbnailView.setContentDescription(null);
            holder.thumbnailView.setTag(null);
            holder.thumbnailView.setOnLongClickListener(null);
            holder.thumbnailView.setVisibility(4);
            holder.iconView.setAlpha(1.0f);
            holder.iconView.setTranslationX(0.0f);
            holder.iconView.setTranslationY(0.0f);
            holder.labelView.setAlpha(1.0f);
            holder.labelView.setTranslationX(0.0f);
            holder.labelView.setTranslationY(0.0f);
            if (holder.calloutLine != null) {
                holder.calloutLine.setAlpha(1.0f);
                holder.calloutLine.setTranslationX(0.0f);
                holder.calloutLine.setTranslationY(0.0f);
                holder.calloutLine.animate().cancel();
            }
            holder.taskDescription = null;
            holder.loadedThumbnailAndIcon = false;
        }
    }

    public RecentsPanelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentsPanelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        updateValuesFromResources();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecentsPanelView, defStyle, 0);
        this.mRecentItemLayoutId = a.getResourceId(0, 0);
        this.mRecentTasksLoader = RecentTasksLoader.getInstance(context);
        a.recycle();
    }

    public int numItemsInOneScreenful() {
        return this.mRecentsContainer.numItemsInOneScreenful();
    }

    private boolean pointInside(int x, int y, View v) {
        int l = v.getLeft();
        int r = v.getRight();
        int t = v.getTop();
        int b = v.getBottom();
        return x >= l && x < r && y >= t && y < b;
    }

    @Override
    public boolean isInContentArea(int x, int y) {
        return pointInside(x, y, (View) this.mRecentsContainer);
    }

    public void show(boolean show) {
        show(show, null, false, false);
    }

    public void show(boolean show, ArrayList<TaskDescription> recentTaskDescriptions, boolean firstScreenful, boolean animateIconOfFirstTask) {
        if (show && this.mCallUiHiddenBeforeNextReload) {
            onUiHidden();
            recentTaskDescriptions = null;
            this.mAnimateIconOfFirstTask = false;
            this.mWaitingForWindowAnimation = false;
        } else {
            this.mAnimateIconOfFirstTask = animateIconOfFirstTask;
            this.mWaitingForWindowAnimation = animateIconOfFirstTask;
        }
        if (show) {
            this.mWaitingToShow = true;
            refreshRecentTasksList(recentTaskDescriptions, firstScreenful);
            showIfReady();
            return;
        }
        showImpl(false);
    }

    private void showIfReady() {
        if (this.mWaitingToShow && this.mRecentTaskDescriptions != null) {
            showImpl(true);
        }
    }

    static void sendCloseSystemWindows(Context context, String reason) {
        if (ActivityManagerNative.isSystemReady()) {
            try {
                ActivityManagerNative.getDefault().closeSystemDialogs(reason);
            } catch (RemoteException e) {
            }
        }
    }

    private void showImpl(boolean show) {
        sendCloseSystemWindows(getContext(), "recentapps");
        this.mShowing = show;
        if (show) {
            boolean noApps = this.mRecentTaskDescriptions != null && this.mRecentTaskDescriptions.size() == 0;
            this.mRecentsNoApps.setAlpha(1.0f);
            this.mRecentsNoApps.setVisibility(noApps ? 0 : 4);
            onAnimationEnd(null);
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
            return;
        }
        this.mWaitingToShow = false;
        this.mCallUiHiddenBeforeNextReload = true;
        if (this.mPopup != null) {
            this.mPopup.dismiss();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewRootImpl root = getViewRootImpl();
        if (root != null) {
            root.setDrawDuringWindowsAnimating(true);
        }
    }

    public void onUiHidden() {
        this.mCallUiHiddenBeforeNextReload = false;
        if (!this.mShowing && this.mRecentTaskDescriptions != null) {
            onAnimationEnd(null);
            clearRecentTasksList();
        }
    }

    @Override
    public void dismiss() {
        ((RecentsActivity) getContext()).dismissAndGoHome();
    }

    public void dismissAndGoBack() {
        ((RecentsActivity) getContext()).dismissAndGoBack();
    }

    @Override
    public void onAnimationCancel(Animator animation) {
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (this.mShowing) {
            LayoutTransition transitioner = new LayoutTransition();
            ((ViewGroup) this.mRecentsContainer).setLayoutTransition(transitioner);
            createCustomAnimations(transitioner);
            return;
        }
        ((ViewGroup) this.mRecentsContainer).setLayoutTransition(null);
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
    }

    @Override
    public void onAnimationStart(Animator animation) {
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    public void updateValuesFromResources() {
        Resources res = getContext().getResources();
        this.mThumbnailWidth = Math.round(res.getDimension(R.dimen.status_bar_recents_thumbnail_width));
        this.mFitThumbnailToXY = res.getBoolean(R.bool.config_recents_thumbnail_image_fits_to_xy);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mRecentsContainer = (RecentsScrollView) findViewById(R.id.recents_container);
        this.mRecentsContainer.setOnScrollListener(new Runnable() {
            @Override
            public void run() {
                RecentsPanelView.this.invalidate();
            }
        });
        this.mListAdapter = new TaskDescriptionAdapter(getContext());
        this.mRecentsContainer.setAdapter(this.mListAdapter);
        this.mRecentsContainer.setCallback(this);
        this.mRecentsScrim = findViewById(R.id.recents_bg_protect);
        this.mRecentsNoApps = findViewById(R.id.recents_no_apps);
        if (this.mRecentsScrim != null) {
            this.mHighEndGfx = ActivityManager.isHighEndGfx();
            if (!this.mHighEndGfx) {
                this.mRecentsScrim.setBackground(null);
            } else if (this.mRecentsScrim.getBackground() instanceof BitmapDrawable) {
                ((BitmapDrawable) this.mRecentsScrim.getBackground()).setTileModeY(Shader.TileMode.REPEAT);
            }
        }
    }

    public void setMinSwipeAlpha(float minAlpha) {
        this.mRecentsContainer.setMinSwipeAlpha(minAlpha);
    }

    private void createCustomAnimations(LayoutTransition transitioner) {
        transitioner.setDuration(200L);
        transitioner.setStartDelay(1, 0L);
        transitioner.setAnimator(3, null);
    }

    private void updateIcon(ViewHolder h, Drawable icon, boolean show, boolean anim) {
        if (icon != null) {
            h.iconView.setImageDrawable(icon);
            if (show && h.iconView.getVisibility() != 0) {
                if (anim) {
                    h.iconView.setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.recent_appear));
                }
                h.iconView.setVisibility(0);
            }
        }
    }

    private void updateThumbnail(ViewHolder h, Drawable thumbnail, boolean show, boolean anim) {
        if (thumbnail != null) {
            h.thumbnailViewImage.setImageDrawable(thumbnail);
            if (h.thumbnailViewDrawable == null || h.thumbnailViewDrawable.getIntrinsicWidth() != thumbnail.getIntrinsicWidth() || h.thumbnailViewDrawable.getIntrinsicHeight() != thumbnail.getIntrinsicHeight()) {
                if (this.mFitThumbnailToXY) {
                    h.thumbnailViewImage.setScaleType(ImageView.ScaleType.FIT_XY);
                } else {
                    Matrix scaleMatrix = new Matrix();
                    float scale = this.mThumbnailWidth / thumbnail.getIntrinsicWidth();
                    scaleMatrix.setScale(scale, scale);
                    h.thumbnailViewImage.setScaleType(ImageView.ScaleType.MATRIX);
                    h.thumbnailViewImage.setImageMatrix(scaleMatrix);
                }
            }
            if (show && h.thumbnailView.getVisibility() != 0) {
                if (anim) {
                    h.thumbnailView.setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.recent_appear));
                }
                h.thumbnailView.setVisibility(0);
            }
            h.thumbnailViewDrawable = thumbnail;
        }
    }

    void onTaskThumbnailLoaded(TaskDescription td) {
        synchronized (td) {
            if (this.mRecentsContainer != null) {
                ViewGroup container = (ViewGroup) this.mRecentsContainer;
                if (container instanceof RecentsScrollView) {
                    container = (ViewGroup) container.findViewById(R.id.recents_linear_layout);
                }
                for (int i = 0; i < container.getChildCount(); i++) {
                    View v = container.getChildAt(i);
                    if (v.getTag() instanceof ViewHolder) {
                        ViewHolder h = (ViewHolder) v.getTag();
                        if (!h.loadedThumbnailAndIcon && h.taskDescription == td) {
                            updateIcon(h, td.getIcon(), true, false);
                            updateThumbnail(h, td.getThumbnail(), true, false);
                            h.loadedThumbnailAndIcon = true;
                        }
                    }
                }
            }
        }
        showIfReady();
    }

    private void animateInIconOfFirstTask() {
        if (this.mItemToAnimateInWhenWindowAnimationIsFinished != null && !this.mRecentTasksLoader.isFirstScreenful()) {
            int timeSinceWindowAnimation = (int) (System.currentTimeMillis() - this.mWindowAnimationStartTime);
            int startDelay = Math.max(0, Math.min(150 - timeSinceWindowAnimation, 150));
            ViewHolder holder = this.mItemToAnimateInWhenWindowAnimationIsFinished;
            TimeInterpolator cubic = new DecelerateInterpolator(1.5f);
            FirstFrameAnimatorHelper.initializeDrawListener(holder.iconView);
            View[] arr$ = {holder.iconView, holder.labelView, holder.calloutLine};
            for (View v : arr$) {
                if (v != null) {
                    ViewPropertyAnimator vpa = v.animate().translationX(0.0f).translationY(0.0f).alpha(1.0f).setStartDelay(startDelay).setDuration(250L).setInterpolator(cubic);
                    new FirstFrameAnimatorHelper(vpa, v);
                }
            }
            this.mItemToAnimateInWhenWindowAnimationIsFinished = null;
            this.mAnimateIconOfFirstTask = false;
        }
    }

    public void onWindowAnimationStart() {
        this.mWaitingForWindowAnimation = false;
        this.mWindowAnimationStartTime = System.currentTimeMillis();
        animateInIconOfFirstTask();
    }

    public void clearRecentTasksList() {
        if (this.mRecentTaskDescriptions != null) {
            this.mRecentTasksLoader.cancelLoadingThumbnailsAndIcons(this);
            onTaskLoadingCancelled();
        }
    }

    public void onTaskLoadingCancelled() {
        if (this.mRecentTaskDescriptions != null) {
            this.mRecentTaskDescriptions = null;
            this.mListAdapter.notifyDataSetInvalidated();
        }
    }

    public void refreshViews() {
        this.mListAdapter.notifyDataSetInvalidated();
        updateUiElements();
        showIfReady();
    }

    public void refreshRecentTasksList() {
        refreshRecentTasksList(null, false);
    }

    private void refreshRecentTasksList(ArrayList<TaskDescription> recentTasksList, boolean firstScreenful) {
        if (this.mRecentTaskDescriptions == null && recentTasksList != null) {
            onTasksLoaded(recentTasksList, firstScreenful);
        } else {
            this.mRecentTasksLoader.loadTasksInBackground();
        }
    }

    public void onTasksLoaded(ArrayList<TaskDescription> tasks, boolean firstScreenful) {
        if (this.mRecentTaskDescriptions == null) {
            this.mRecentTaskDescriptions = new ArrayList<>(tasks);
        } else {
            this.mRecentTaskDescriptions.addAll(tasks);
        }
        if (((RecentsActivity) getContext()).isActivityShowing()) {
            refreshViews();
        }
    }

    private void updateUiElements() {
        String recentAppsAccessibilityDescription;
        int items = this.mRecentTaskDescriptions != null ? this.mRecentTaskDescriptions.size() : 0;
        ((View) this.mRecentsContainer).setVisibility(items > 0 ? 0 : 8);
        int numRecentApps = this.mRecentTaskDescriptions != null ? this.mRecentTaskDescriptions.size() : 0;
        if (numRecentApps == 0) {
            recentAppsAccessibilityDescription = getResources().getString(R.string.status_bar_no_recent_apps);
        } else {
            recentAppsAccessibilityDescription = getResources().getQuantityString(R.plurals.status_bar_accessibility_recent_apps, numRecentApps, Integer.valueOf(numRecentApps));
        }
        setContentDescription(recentAppsAccessibilityDescription);
    }

    public boolean simulateClick(int persistentTaskId) {
        View v = this.mRecentsContainer.findViewForTask(persistentTaskId);
        if (v == null) {
            return false;
        }
        handleOnClick(v);
        return true;
    }

    @Override
    public void handleOnClick(View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        TaskDescription ad = holder.taskDescription;
        Context context = view.getContext();
        ActivityManager am = (ActivityManager) context.getSystemService("activity");
        Bitmap bm = null;
        boolean usingDrawingCache = true;
        if (holder.thumbnailViewDrawable instanceof BitmapDrawable) {
            bm = ((BitmapDrawable) holder.thumbnailViewDrawable).getBitmap();
            if (bm.getWidth() == holder.thumbnailViewImage.getWidth() && bm.getHeight() == holder.thumbnailViewImage.getHeight()) {
                usingDrawingCache = false;
            }
        }
        if (usingDrawingCache) {
            holder.thumbnailViewImage.setDrawingCacheEnabled(true);
            bm = holder.thumbnailViewImage.getDrawingCache();
        }
        Bundle opts = bm != null ? ActivityOptions.makeThumbnailScaleUpAnimation(holder.thumbnailViewImage, bm, 0, 0, null).toBundle() : null;
        show(false);
        if (ad.taskId >= 0) {
            am.moveTaskToFront(ad.taskId, 1, opts);
        } else {
            Intent intent = ad.intent;
            intent.addFlags(269500416);
            if (DEBUG) {
                Log.v("RecentsPanelView", "Starting activity " + intent);
            }
            try {
                context.startActivityAsUser(intent, opts, new UserHandle(ad.userId));
            } catch (ActivityNotFoundException e) {
                Log.e("RecentsPanelView", "Error launching activity " + intent, e);
            } catch (SecurityException e2) {
                Log.e("RecentsPanelView", "Recents does not have the permission to launch " + intent, e2);
            }
        }
        if (usingDrawingCache) {
            holder.thumbnailViewImage.setDrawingCacheEnabled(false);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        handleOnClick(view);
    }

    @Override
    public void handleSwipe(View view) {
        TaskDescription ad = ((ViewHolder) view.getTag()).taskDescription;
        if (ad == null) {
            Log.v("RecentsPanelView", "Not able to find activity description for swiped task; view=" + view + " tag=" + view.getTag());
            return;
        }
        if (DEBUG) {
            Log.v("RecentsPanelView", "Jettison " + ((Object) ad.getLabel()));
        }
        this.mRecentTaskDescriptions.remove(ad);
        this.mRecentTasksLoader.remove(ad);
        if (this.mRecentTaskDescriptions.size() == 0) {
            dismissAndGoBack();
        }
        ActivityManager am = (ActivityManager) getContext().getSystemService("activity");
        if (am != null) {
            am.removeTask(ad.persistentTaskId);
            setContentDescription(getContext().getString(R.string.accessibility_recents_item_dismissed, ad.getLabel()));
            sendAccessibilityEvent(4);
            setContentDescription(null);
        }
    }

    private void startApplicationDetailsActivity(String packageName, int userId) {
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", packageName, null));
        intent.setComponent(intent.resolveActivity(getContext().getPackageManager()));
        TaskStackBuilder.create(getContext()).addNextIntentWithParentStack(intent).startActivities(null, new UserHandle(userId));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mPopup != null) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public void handleLongPress(final View selectedView, View anchorView, final View thumbnailView) {
        thumbnailView.setSelected(true);
        Context context = getContext();
        if (anchorView == null) {
            anchorView = selectedView;
        }
        PopupMenu popup = new PopupMenu(context, anchorView);
        this.mPopup = popup;
        popup.getMenuInflater().inflate(R.menu.recent_popup_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.recent_remove_item) {
                    ((ViewGroup) RecentsPanelView.this.mRecentsContainer).removeViewInLayout(selectedView);
                } else {
                    if (item.getItemId() != R.id.recent_inspect_item) {
                        return false;
                    }
                    ViewHolder viewHolder = (ViewHolder) selectedView.getTag();
                    if (viewHolder != null) {
                        TaskDescription ad = viewHolder.taskDescription;
                        RecentsPanelView.this.startApplicationDetailsActivity(ad.packageName, ad.userId);
                        RecentsPanelView.this.show(false);
                    } else {
                        throw new IllegalStateException("Oops, no tag on view " + selectedView);
                    }
                }
                return true;
            }
        });
        popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                thumbnailView.setSelected(false);
                RecentsPanelView.this.mPopup = null;
            }
        });
        popup.show();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        int paddingLeft = getPaddingLeft();
        boolean offsetRequired = isPaddingOffsetRequired();
        if (offsetRequired) {
            paddingLeft += getLeftPaddingOffset();
        }
        int left = getScrollX() + paddingLeft;
        int right = (((getRight() + left) - getLeft()) - getPaddingRight()) - paddingLeft;
        int top = getScrollY() + getFadeTop(offsetRequired);
        int bottom = top + getFadeHeight(offsetRequired);
        if (offsetRequired) {
            right += getRightPaddingOffset();
            bottom += getBottomPaddingOffset();
        }
        this.mRecentsContainer.drawFadedEdges(canvas, left, right, top, bottom);
    }
}
