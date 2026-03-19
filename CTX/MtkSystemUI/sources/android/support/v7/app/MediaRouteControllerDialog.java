package android.support.v7.app;

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.util.ObjectsCompat;
import android.support.v7.app.OverlayListView;
import android.support.v7.graphics.Palette;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.mediarouter.R;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MediaRouteControllerDialog extends AlertDialog {
    private Interpolator mAccelerateDecelerateInterpolator;
    final AccessibilityManager mAccessibilityManager;
    int mArtIconBackgroundColor;
    Bitmap mArtIconBitmap;
    boolean mArtIconIsLoaded;
    Bitmap mArtIconLoadedBitmap;
    Uri mArtIconUri;
    private ImageView mArtView;
    private boolean mAttachedToWindow;
    private final MediaRouterCallback mCallback;
    private ImageButton mCloseButton;
    Context mContext;
    MediaControllerCallback mControllerCallback;
    private boolean mCreated;
    private FrameLayout mCustomControlLayout;
    private View mCustomControlView;
    FrameLayout mDefaultControlLayout;
    MediaDescriptionCompat mDescription;
    private LinearLayout mDialogAreaLayout;
    private int mDialogContentWidth;
    private Button mDisconnectButton;
    private View mDividerView;
    private FrameLayout mExpandableAreaLayout;
    private Interpolator mFastOutSlowInInterpolator;
    FetchArtTask mFetchArtTask;
    private MediaRouteExpandCollapseButton mGroupExpandCollapseButton;
    int mGroupListAnimationDurationMs;
    Runnable mGroupListFadeInAnimation;
    private int mGroupListFadeInDurationMs;
    private int mGroupListFadeOutDurationMs;
    private List<MediaRouter.RouteInfo> mGroupMemberRoutes;
    Set<MediaRouter.RouteInfo> mGroupMemberRoutesAdded;
    Set<MediaRouter.RouteInfo> mGroupMemberRoutesAnimatingWithBitmap;
    private Set<MediaRouter.RouteInfo> mGroupMemberRoutesRemoved;
    boolean mHasPendingUpdate;
    private Interpolator mInterpolator;
    boolean mIsGroupExpanded;
    boolean mIsGroupListAnimating;
    boolean mIsGroupListAnimationPending;
    private Interpolator mLinearOutSlowInInterpolator;
    MediaControllerCompat mMediaController;
    private LinearLayout mMediaMainControlLayout;
    boolean mPendingUpdateAnimationNeeded;
    private ImageButton mPlaybackControlButton;
    private RelativeLayout mPlaybackControlLayout;
    final MediaRouter.RouteInfo mRoute;
    MediaRouter.RouteInfo mRouteInVolumeSliderTouched;
    private TextView mRouteNameTextView;
    final MediaRouter mRouter;
    PlaybackStateCompat mState;
    private Button mStopCastingButton;
    private TextView mSubtitleView;
    private TextView mTitleView;
    VolumeChangeListener mVolumeChangeListener;
    private boolean mVolumeControlEnabled;
    private LinearLayout mVolumeControlLayout;
    VolumeGroupAdapter mVolumeGroupAdapter;
    OverlayListView mVolumeGroupList;
    private int mVolumeGroupListItemHeight;
    private int mVolumeGroupListItemIconSize;
    private int mVolumeGroupListMaxHeight;
    private final int mVolumeGroupListPaddingTop;
    SeekBar mVolumeSlider;
    Map<MediaRouter.RouteInfo, SeekBar> mVolumeSliderMap;
    static final boolean DEBUG = Log.isLoggable("MediaRouteCtrlDialog", 3);
    static final int CONNECTION_TIMEOUT_MILLIS = (int) TimeUnit.SECONDS.toMillis(30);

    public MediaRouteControllerDialog(Context context) {
        this(context, 0);
    }

    public MediaRouteControllerDialog(Context context, int theme) {
        Context context2 = MediaRouterThemeHelper.createThemedDialogContext(context, theme, true);
        super(context2, MediaRouterThemeHelper.createThemedDialogStyle(context2));
        this.mVolumeControlEnabled = true;
        this.mGroupListFadeInAnimation = new Runnable() {
            @Override
            public void run() {
                MediaRouteControllerDialog.this.startGroupListFadeInAnimation();
            }
        };
        this.mContext = getContext();
        this.mControllerCallback = new MediaControllerCallback();
        this.mRouter = MediaRouter.getInstance(this.mContext);
        this.mCallback = new MediaRouterCallback();
        this.mRoute = this.mRouter.getSelectedRoute();
        setMediaSession(this.mRouter.getMediaSessionToken());
        this.mVolumeGroupListPaddingTop = this.mContext.getResources().getDimensionPixelSize(R.dimen.mr_controller_volume_group_list_padding_top);
        this.mAccessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        if (Build.VERSION.SDK_INT >= 21) {
            this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context2, R.interpolator.mr_linear_out_slow_in);
            this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context2, R.interpolator.mr_fast_out_slow_in);
        }
        this.mAccelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();
    }

    private MediaRouter.RouteGroup getGroup() {
        if (this.mRoute instanceof MediaRouter.RouteGroup) {
            return (MediaRouter.RouteGroup) this.mRoute;
        }
        return null;
    }

    public View onCreateMediaControlView(Bundle savedInstanceState) {
        return null;
    }

    private void setMediaSession(MediaSessionCompat.Token sessionToken) {
        if (this.mMediaController != null) {
            this.mMediaController.unregisterCallback(this.mControllerCallback);
            this.mMediaController = null;
        }
        if (sessionToken == null || !this.mAttachedToWindow) {
            return;
        }
        try {
            this.mMediaController = new MediaControllerCompat(this.mContext, sessionToken);
        } catch (RemoteException e) {
            Log.e("MediaRouteCtrlDialog", "Error creating media controller in setMediaSession.", e);
        }
        if (this.mMediaController != null) {
            this.mMediaController.registerCallback(this.mControllerCallback);
        }
        MediaMetadataCompat metadata = this.mMediaController == null ? null : this.mMediaController.getMetadata();
        this.mDescription = metadata == null ? null : metadata.getDescription();
        this.mState = this.mMediaController != null ? this.mMediaController.getPlaybackState() : null;
        updateArtIconIfNeeded();
        update(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setContentView(R.layout.mr_controller_material_dialog_b);
        findViewById(android.R.id.button3).setVisibility(8);
        ClickListener listener = new ClickListener();
        this.mExpandableAreaLayout = (FrameLayout) findViewById(R.id.mr_expandable_area);
        this.mExpandableAreaLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaRouteControllerDialog.this.dismiss();
            }
        });
        this.mDialogAreaLayout = (LinearLayout) findViewById(R.id.mr_dialog_area);
        this.mDialogAreaLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        int color = MediaRouterThemeHelper.getButtonTextColor(this.mContext);
        this.mDisconnectButton = (Button) findViewById(android.R.id.button2);
        this.mDisconnectButton.setText(R.string.mr_controller_disconnect);
        this.mDisconnectButton.setTextColor(color);
        this.mDisconnectButton.setOnClickListener(listener);
        this.mStopCastingButton = (Button) findViewById(android.R.id.button1);
        this.mStopCastingButton.setText(R.string.mr_controller_stop_casting);
        this.mStopCastingButton.setTextColor(color);
        this.mStopCastingButton.setOnClickListener(listener);
        this.mRouteNameTextView = (TextView) findViewById(R.id.mr_name);
        this.mCloseButton = (ImageButton) findViewById(R.id.mr_close);
        this.mCloseButton.setOnClickListener(listener);
        this.mCustomControlLayout = (FrameLayout) findViewById(R.id.mr_custom_control);
        this.mDefaultControlLayout = (FrameLayout) findViewById(R.id.mr_default_control);
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PendingIntent pi;
                if (MediaRouteControllerDialog.this.mMediaController != null && (pi = MediaRouteControllerDialog.this.mMediaController.getSessionActivity()) != null) {
                    try {
                        pi.send();
                        MediaRouteControllerDialog.this.dismiss();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e("MediaRouteCtrlDialog", pi + " was not sent, it had been canceled.");
                    }
                }
            }
        };
        this.mArtView = (ImageView) findViewById(R.id.mr_art);
        this.mArtView.setOnClickListener(onClickListener);
        findViewById(R.id.mr_control_title_container).setOnClickListener(onClickListener);
        this.mMediaMainControlLayout = (LinearLayout) findViewById(R.id.mr_media_main_control);
        this.mDividerView = findViewById(R.id.mr_control_divider);
        this.mPlaybackControlLayout = (RelativeLayout) findViewById(R.id.mr_playback_control);
        this.mTitleView = (TextView) findViewById(R.id.mr_control_title);
        this.mSubtitleView = (TextView) findViewById(R.id.mr_control_subtitle);
        this.mPlaybackControlButton = (ImageButton) findViewById(R.id.mr_control_playback_ctrl);
        this.mPlaybackControlButton.setOnClickListener(listener);
        this.mVolumeControlLayout = (LinearLayout) findViewById(R.id.mr_volume_control);
        this.mVolumeControlLayout.setVisibility(8);
        this.mVolumeSlider = (SeekBar) findViewById(R.id.mr_volume_slider);
        this.mVolumeSlider.setTag(this.mRoute);
        this.mVolumeChangeListener = new VolumeChangeListener();
        this.mVolumeSlider.setOnSeekBarChangeListener(this.mVolumeChangeListener);
        this.mVolumeGroupList = (OverlayListView) findViewById(R.id.mr_volume_group_list);
        this.mGroupMemberRoutes = new ArrayList();
        this.mVolumeGroupAdapter = new VolumeGroupAdapter(this.mVolumeGroupList.getContext(), this.mGroupMemberRoutes);
        this.mVolumeGroupList.setAdapter((ListAdapter) this.mVolumeGroupAdapter);
        this.mGroupMemberRoutesAnimatingWithBitmap = new HashSet();
        MediaRouterThemeHelper.setMediaControlsBackgroundColor(this.mContext, this.mMediaMainControlLayout, this.mVolumeGroupList, getGroup() != null);
        MediaRouterThemeHelper.setVolumeSliderColor(this.mContext, (MediaRouteVolumeSlider) this.mVolumeSlider, this.mMediaMainControlLayout);
        this.mVolumeSliderMap = new HashMap();
        this.mVolumeSliderMap.put(this.mRoute, this.mVolumeSlider);
        this.mGroupExpandCollapseButton = (MediaRouteExpandCollapseButton) findViewById(R.id.mr_group_expand_collapse);
        this.mGroupExpandCollapseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaRouteControllerDialog.this.mIsGroupExpanded = !MediaRouteControllerDialog.this.mIsGroupExpanded;
                if (MediaRouteControllerDialog.this.mIsGroupExpanded) {
                    MediaRouteControllerDialog.this.mVolumeGroupList.setVisibility(0);
                }
                MediaRouteControllerDialog.this.loadInterpolator();
                MediaRouteControllerDialog.this.updateLayoutHeight(true);
            }
        });
        loadInterpolator();
        this.mGroupListAnimationDurationMs = this.mContext.getResources().getInteger(R.integer.mr_controller_volume_group_list_animation_duration_ms);
        this.mGroupListFadeInDurationMs = this.mContext.getResources().getInteger(R.integer.mr_controller_volume_group_list_fade_in_duration_ms);
        this.mGroupListFadeOutDurationMs = this.mContext.getResources().getInteger(R.integer.mr_controller_volume_group_list_fade_out_duration_ms);
        this.mCustomControlView = onCreateMediaControlView(savedInstanceState);
        if (this.mCustomControlView != null) {
            this.mCustomControlLayout.addView(this.mCustomControlView);
            this.mCustomControlLayout.setVisibility(0);
        }
        this.mCreated = true;
        updateLayout();
    }

    void updateLayout() {
        int width = MediaRouteDialogHelper.getDialogWidth(this.mContext);
        getWindow().setLayout(width, -2);
        View decorView = getWindow().getDecorView();
        this.mDialogContentWidth = (width - decorView.getPaddingLeft()) - decorView.getPaddingRight();
        Resources res = this.mContext.getResources();
        this.mVolumeGroupListItemIconSize = res.getDimensionPixelSize(R.dimen.mr_controller_volume_group_list_item_icon_size);
        this.mVolumeGroupListItemHeight = res.getDimensionPixelSize(R.dimen.mr_controller_volume_group_list_item_height);
        this.mVolumeGroupListMaxHeight = res.getDimensionPixelSize(R.dimen.mr_controller_volume_group_list_max_height);
        this.mArtIconBitmap = null;
        this.mArtIconUri = null;
        updateArtIconIfNeeded();
        update(false);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAttachedToWindow = true;
        this.mRouter.addCallback(MediaRouteSelector.EMPTY, this.mCallback, 2);
        setMediaSession(this.mRouter.getMediaSessionToken());
    }

    @Override
    public void onDetachedFromWindow() {
        this.mRouter.removeCallback(this.mCallback);
        setMediaSession(null);
        this.mAttachedToWindow = false;
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 25 || keyCode == 24) {
            this.mRoute.requestUpdateVolume(keyCode == 25 ? -1 : 1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 25 || keyCode == 24) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    void update(boolean animate) {
        if (this.mRouteInVolumeSliderTouched != null) {
            this.mHasPendingUpdate = true;
            this.mPendingUpdateAnimationNeeded |= animate;
            return;
        }
        this.mHasPendingUpdate = false;
        this.mPendingUpdateAnimationNeeded = false;
        if (!this.mRoute.isSelected() || this.mRoute.isDefaultOrBluetooth()) {
            dismiss();
            return;
        }
        if (!this.mCreated) {
            return;
        }
        this.mRouteNameTextView.setText(this.mRoute.getName());
        this.mDisconnectButton.setVisibility(this.mRoute.canDisconnect() ? 0 : 8);
        if (this.mCustomControlView == null && this.mArtIconIsLoaded) {
            if (isBitmapRecycled(this.mArtIconLoadedBitmap)) {
                Log.w("MediaRouteCtrlDialog", "Can't set artwork image with recycled bitmap: " + this.mArtIconLoadedBitmap);
            } else {
                this.mArtView.setImageBitmap(this.mArtIconLoadedBitmap);
                this.mArtView.setBackgroundColor(this.mArtIconBackgroundColor);
            }
            clearLoadedBitmap();
        }
        updateVolumeControlLayout();
        updatePlaybackControlLayout();
        updateLayoutHeight(animate);
    }

    private boolean isBitmapRecycled(Bitmap bitmap) {
        return bitmap != null && bitmap.isRecycled();
    }

    private boolean canShowPlaybackControlLayout() {
        return this.mCustomControlView == null && !(this.mDescription == null && this.mState == null);
    }

    private int getMainControllerHeight(boolean showPlaybackControl) {
        if (showPlaybackControl || this.mVolumeControlLayout.getVisibility() == 0) {
            int height = 0 + this.mMediaMainControlLayout.getPaddingTop() + this.mMediaMainControlLayout.getPaddingBottom();
            if (showPlaybackControl) {
                height += this.mPlaybackControlLayout.getMeasuredHeight();
            }
            if (this.mVolumeControlLayout.getVisibility() == 0) {
                height += this.mVolumeControlLayout.getMeasuredHeight();
            }
            if (showPlaybackControl && this.mVolumeControlLayout.getVisibility() == 0) {
                return height + this.mDividerView.getMeasuredHeight();
            }
            return height;
        }
        return 0;
    }

    private void updateMediaControlVisibility(boolean canShowPlaybackControlLayout) {
        int i = 0;
        this.mDividerView.setVisibility((this.mVolumeControlLayout.getVisibility() == 0 && canShowPlaybackControlLayout) ? 0 : 8);
        LinearLayout linearLayout = this.mMediaMainControlLayout;
        if (this.mVolumeControlLayout.getVisibility() == 8 && !canShowPlaybackControlLayout) {
            i = 8;
        }
        linearLayout.setVisibility(i);
    }

    void updateLayoutHeight(final boolean animate) {
        this.mDefaultControlLayout.requestLayout();
        ViewTreeObserver observer = this.mDefaultControlLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                MediaRouteControllerDialog.this.mDefaultControlLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                if (MediaRouteControllerDialog.this.mIsGroupListAnimating) {
                    MediaRouteControllerDialog.this.mIsGroupListAnimationPending = true;
                } else {
                    MediaRouteControllerDialog.this.updateLayoutHeightInternal(animate);
                }
            }
        });
    }

    void updateLayoutHeightInternal(boolean animate) {
        Bitmap art;
        int oldHeight = getLayoutHeight(this.mMediaMainControlLayout);
        setLayoutHeight(this.mMediaMainControlLayout, -1);
        updateMediaControlVisibility(canShowPlaybackControlLayout());
        View decorView = getWindow().getDecorView();
        decorView.measure(View.MeasureSpec.makeMeasureSpec(getWindow().getAttributes().width, 1073741824), 0);
        setLayoutHeight(this.mMediaMainControlLayout, oldHeight);
        int artViewHeight = 0;
        if (this.mCustomControlView == null && (this.mArtView.getDrawable() instanceof BitmapDrawable) && (art = ((BitmapDrawable) this.mArtView.getDrawable()).getBitmap()) != null) {
            artViewHeight = getDesiredArtHeight(art.getWidth(), art.getHeight());
            this.mArtView.setScaleType(art.getWidth() >= art.getHeight() ? ImageView.ScaleType.FIT_XY : ImageView.ScaleType.FIT_CENTER);
        }
        int mainControllerHeight = getMainControllerHeight(canShowPlaybackControlLayout());
        int volumeGroupListCount = this.mGroupMemberRoutes.size();
        int expandedGroupListHeight = getGroup() == null ? 0 : this.mVolumeGroupListItemHeight * getGroup().getRoutes().size();
        if (volumeGroupListCount > 0) {
            expandedGroupListHeight += this.mVolumeGroupListPaddingTop;
        }
        int visibleGroupListHeight = this.mIsGroupExpanded ? Math.min(expandedGroupListHeight, this.mVolumeGroupListMaxHeight) : 0;
        int desiredControlLayoutHeight = Math.max(artViewHeight, visibleGroupListHeight) + mainControllerHeight;
        Rect visibleRect = new Rect();
        decorView.getWindowVisibleDisplayFrame(visibleRect);
        int nonControlViewHeight = this.mDialogAreaLayout.getMeasuredHeight() - this.mDefaultControlLayout.getMeasuredHeight();
        int maximumControlViewHeight = visibleRect.height() - nonControlViewHeight;
        if (this.mCustomControlView == null && artViewHeight > 0 && desiredControlLayoutHeight <= maximumControlViewHeight) {
            this.mArtView.setVisibility(0);
            setLayoutHeight(this.mArtView, artViewHeight);
        } else {
            if (getLayoutHeight(this.mVolumeGroupList) + this.mMediaMainControlLayout.getMeasuredHeight() >= this.mDefaultControlLayout.getMeasuredHeight()) {
                this.mArtView.setVisibility(8);
            }
            artViewHeight = 0;
            desiredControlLayoutHeight = visibleGroupListHeight + mainControllerHeight;
        }
        if (canShowPlaybackControlLayout() && desiredControlLayoutHeight <= maximumControlViewHeight) {
            this.mPlaybackControlLayout.setVisibility(0);
        } else {
            this.mPlaybackControlLayout.setVisibility(8);
        }
        updateMediaControlVisibility(this.mPlaybackControlLayout.getVisibility() == 0);
        int mainControllerHeight2 = getMainControllerHeight(this.mPlaybackControlLayout.getVisibility() == 0);
        int desiredControlLayoutHeight2 = Math.max(artViewHeight, visibleGroupListHeight) + mainControllerHeight2;
        if (desiredControlLayoutHeight2 > maximumControlViewHeight) {
            visibleGroupListHeight -= desiredControlLayoutHeight2 - maximumControlViewHeight;
            desiredControlLayoutHeight2 = maximumControlViewHeight;
        }
        this.mMediaMainControlLayout.clearAnimation();
        this.mVolumeGroupList.clearAnimation();
        this.mDefaultControlLayout.clearAnimation();
        if (animate) {
            animateLayoutHeight(this.mMediaMainControlLayout, mainControllerHeight2);
            animateLayoutHeight(this.mVolumeGroupList, visibleGroupListHeight);
            animateLayoutHeight(this.mDefaultControlLayout, desiredControlLayoutHeight2);
        } else {
            setLayoutHeight(this.mMediaMainControlLayout, mainControllerHeight2);
            setLayoutHeight(this.mVolumeGroupList, visibleGroupListHeight);
            setLayoutHeight(this.mDefaultControlLayout, desiredControlLayoutHeight2);
        }
        setLayoutHeight(this.mExpandableAreaLayout, visibleRect.height());
        rebuildVolumeGroupList(animate);
    }

    void updateVolumeGroupItemHeight(View item) {
        LinearLayout container = (LinearLayout) item.findViewById(R.id.volume_item_container);
        setLayoutHeight(container, this.mVolumeGroupListItemHeight);
        View icon = item.findViewById(R.id.mr_volume_item_icon);
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        lp.width = this.mVolumeGroupListItemIconSize;
        lp.height = this.mVolumeGroupListItemIconSize;
        icon.setLayoutParams(lp);
    }

    private void animateLayoutHeight(final View view, final int targetHeight) {
        final int startValue = getLayoutHeight(view);
        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                int height = startValue - ((int) ((startValue - targetHeight) * interpolatedTime));
                MediaRouteControllerDialog.setLayoutHeight(view, height);
            }
        };
        anim.setDuration(this.mGroupListAnimationDurationMs);
        if (Build.VERSION.SDK_INT >= 21) {
            anim.setInterpolator(this.mInterpolator);
        }
        view.startAnimation(anim);
    }

    void loadInterpolator() {
        if (Build.VERSION.SDK_INT >= 21) {
            this.mInterpolator = this.mIsGroupExpanded ? this.mLinearOutSlowInInterpolator : this.mFastOutSlowInInterpolator;
        } else {
            this.mInterpolator = this.mAccelerateDecelerateInterpolator;
        }
    }

    private void updateVolumeControlLayout() {
        if (isVolumeControlAvailable(this.mRoute)) {
            if (this.mVolumeControlLayout.getVisibility() == 8) {
                this.mVolumeControlLayout.setVisibility(0);
                this.mVolumeSlider.setMax(this.mRoute.getVolumeMax());
                this.mVolumeSlider.setProgress(this.mRoute.getVolume());
                this.mGroupExpandCollapseButton.setVisibility(getGroup() != null ? 0 : 8);
                return;
            }
            return;
        }
        this.mVolumeControlLayout.setVisibility(8);
    }

    private void rebuildVolumeGroupList(boolean animate) {
        List<MediaRouter.RouteInfo> routes = getGroup() == null ? null : getGroup().getRoutes();
        if (routes == null) {
            this.mGroupMemberRoutes.clear();
            this.mVolumeGroupAdapter.notifyDataSetChanged();
            return;
        }
        if (MediaRouteDialogHelper.listUnorderedEquals(this.mGroupMemberRoutes, routes)) {
            this.mVolumeGroupAdapter.notifyDataSetChanged();
            return;
        }
        HashMap<MediaRouter.RouteInfo, Rect> previousRouteBoundMap = animate ? MediaRouteDialogHelper.getItemBoundMap(this.mVolumeGroupList, this.mVolumeGroupAdapter) : null;
        HashMap<MediaRouter.RouteInfo, BitmapDrawable> previousRouteBitmapMap = animate ? MediaRouteDialogHelper.getItemBitmapMap(this.mContext, this.mVolumeGroupList, this.mVolumeGroupAdapter) : null;
        this.mGroupMemberRoutesAdded = MediaRouteDialogHelper.getItemsAdded(this.mGroupMemberRoutes, routes);
        this.mGroupMemberRoutesRemoved = MediaRouteDialogHelper.getItemsRemoved(this.mGroupMemberRoutes, routes);
        this.mGroupMemberRoutes.addAll(0, this.mGroupMemberRoutesAdded);
        this.mGroupMemberRoutes.removeAll(this.mGroupMemberRoutesRemoved);
        this.mVolumeGroupAdapter.notifyDataSetChanged();
        if (animate && this.mIsGroupExpanded && this.mGroupMemberRoutesAdded.size() + this.mGroupMemberRoutesRemoved.size() > 0) {
            animateGroupListItems(previousRouteBoundMap, previousRouteBitmapMap);
        } else {
            this.mGroupMemberRoutesAdded = null;
            this.mGroupMemberRoutesRemoved = null;
        }
    }

    private void animateGroupListItems(final Map<MediaRouter.RouteInfo, Rect> previousRouteBoundMap, final Map<MediaRouter.RouteInfo, BitmapDrawable> previousRouteBitmapMap) {
        this.mVolumeGroupList.setEnabled(false);
        this.mVolumeGroupList.requestLayout();
        this.mIsGroupListAnimating = true;
        ViewTreeObserver observer = this.mVolumeGroupList.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                MediaRouteControllerDialog.this.mVolumeGroupList.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                MediaRouteControllerDialog.this.animateGroupListItemsInternal(previousRouteBoundMap, previousRouteBitmapMap);
            }
        });
    }

    void animateGroupListItemsInternal(Map<MediaRouter.RouteInfo, Rect> previousRouteBoundMap, Map<MediaRouter.RouteInfo, BitmapDrawable> previousRouteBitmapMap) {
        boolean listenerRegistered;
        Animation.AnimationListener listener;
        OverlayListView.OverlayObject object;
        if (this.mGroupMemberRoutesAdded == null || this.mGroupMemberRoutesRemoved == null) {
            return;
        }
        int groupSizeDelta = this.mGroupMemberRoutesAdded.size() - this.mGroupMemberRoutesRemoved.size();
        boolean listenerRegistered2 = false;
        Animation.AnimationListener listener2 = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                MediaRouteControllerDialog.this.mVolumeGroupList.startAnimationAll();
                MediaRouteControllerDialog.this.mVolumeGroupList.postDelayed(MediaRouteControllerDialog.this.mGroupListFadeInAnimation, MediaRouteControllerDialog.this.mGroupListAnimationDurationMs);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
        int first = this.mVolumeGroupList.getFirstVisiblePosition();
        for (int i = 0; i < this.mVolumeGroupList.getChildCount(); i++) {
            View view = this.mVolumeGroupList.getChildAt(i);
            int position = first + i;
            MediaRouter.RouteInfo route = this.mVolumeGroupAdapter.getItem(position);
            Rect previousBounds = previousRouteBoundMap.get(route);
            int currentTop = view.getTop();
            int previousTop = previousBounds != null ? previousBounds.top : (this.mVolumeGroupListItemHeight * groupSizeDelta) + currentTop;
            AnimationSet animSet = new AnimationSet(true);
            if (this.mGroupMemberRoutesAdded != null && this.mGroupMemberRoutesAdded.contains(route)) {
                previousTop = currentTop;
                Animation alphaAnim = new AlphaAnimation(0.0f, 0.0f);
                alphaAnim.setDuration(this.mGroupListFadeInDurationMs);
                animSet.addAnimation(alphaAnim);
            }
            Animation translationAnim = new TranslateAnimation(0.0f, 0.0f, previousTop - currentTop, 0.0f);
            translationAnim.setDuration(this.mGroupListAnimationDurationMs);
            animSet.addAnimation(translationAnim);
            animSet.setFillAfter(true);
            animSet.setFillEnabled(true);
            animSet.setInterpolator(this.mInterpolator);
            if (!listenerRegistered2) {
                listenerRegistered2 = true;
                animSet.setAnimationListener(listener2);
            }
            view.clearAnimation();
            view.startAnimation(animSet);
            previousRouteBoundMap.remove(route);
            previousRouteBitmapMap.remove(route);
        }
        for (Map.Entry<MediaRouter.RouteInfo, BitmapDrawable> item : previousRouteBitmapMap.entrySet()) {
            final MediaRouter.RouteInfo route2 = item.getKey();
            BitmapDrawable bitmap = item.getValue();
            Rect bounds = previousRouteBoundMap.get(route2);
            if (this.mGroupMemberRoutesRemoved.contains(route2)) {
                listenerRegistered = listenerRegistered2;
                listener = listener2;
                object = new OverlayListView.OverlayObject(bitmap, bounds).setAlphaAnimation(1.0f, 0.0f).setDuration(this.mGroupListFadeOutDurationMs).setInterpolator(this.mInterpolator);
            } else {
                listenerRegistered = listenerRegistered2;
                listener = listener2;
                int deltaY = this.mVolumeGroupListItemHeight * groupSizeDelta;
                OverlayListView.OverlayObject object2 = new OverlayListView.OverlayObject(bitmap, bounds).setTranslateYAnimation(deltaY).setDuration(this.mGroupListAnimationDurationMs).setInterpolator(this.mInterpolator).setAnimationEndListener(new OverlayListView.OverlayObject.OnAnimationEndListener() {
                    @Override
                    public void onAnimationEnd() {
                        MediaRouteControllerDialog.this.mGroupMemberRoutesAnimatingWithBitmap.remove(route2);
                        MediaRouteControllerDialog.this.mVolumeGroupAdapter.notifyDataSetChanged();
                    }
                });
                this.mGroupMemberRoutesAnimatingWithBitmap.add(route2);
                object = object2;
            }
            this.mVolumeGroupList.addOverlayObject(object);
            listener2 = listener;
            listenerRegistered2 = listenerRegistered;
        }
    }

    void startGroupListFadeInAnimation() {
        clearGroupListAnimation(true);
        this.mVolumeGroupList.requestLayout();
        ViewTreeObserver observer = this.mVolumeGroupList.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                MediaRouteControllerDialog.this.mVolumeGroupList.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                MediaRouteControllerDialog.this.startGroupListFadeInAnimationInternal();
            }
        });
    }

    void startGroupListFadeInAnimationInternal() {
        if (this.mGroupMemberRoutesAdded != null && this.mGroupMemberRoutesAdded.size() != 0) {
            fadeInAddedRoutes();
        } else {
            finishAnimation(true);
        }
    }

    void finishAnimation(boolean animate) {
        this.mGroupMemberRoutesAdded = null;
        this.mGroupMemberRoutesRemoved = null;
        this.mIsGroupListAnimating = false;
        if (this.mIsGroupListAnimationPending) {
            this.mIsGroupListAnimationPending = false;
            updateLayoutHeight(animate);
        }
        this.mVolumeGroupList.setEnabled(true);
    }

    private void fadeInAddedRoutes() {
        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                MediaRouteControllerDialog.this.finishAnimation(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
        boolean listenerRegistered = false;
        int first = this.mVolumeGroupList.getFirstVisiblePosition();
        for (int i = 0; i < this.mVolumeGroupList.getChildCount(); i++) {
            View view = this.mVolumeGroupList.getChildAt(i);
            int position = first + i;
            MediaRouter.RouteInfo route = this.mVolumeGroupAdapter.getItem(position);
            if (this.mGroupMemberRoutesAdded.contains(route)) {
                Animation alphaAnim = new AlphaAnimation(0.0f, 1.0f);
                alphaAnim.setDuration(this.mGroupListFadeInDurationMs);
                alphaAnim.setFillEnabled(true);
                alphaAnim.setFillAfter(true);
                if (!listenerRegistered) {
                    listenerRegistered = true;
                    alphaAnim.setAnimationListener(listener);
                }
                view.clearAnimation();
                view.startAnimation(alphaAnim);
            }
        }
    }

    void clearGroupListAnimation(boolean exceptAddedRoutes) {
        int first = this.mVolumeGroupList.getFirstVisiblePosition();
        for (int i = 0; i < this.mVolumeGroupList.getChildCount(); i++) {
            View view = this.mVolumeGroupList.getChildAt(i);
            int position = first + i;
            MediaRouter.RouteInfo route = this.mVolumeGroupAdapter.getItem(position);
            if (!exceptAddedRoutes || this.mGroupMemberRoutesAdded == null || !this.mGroupMemberRoutesAdded.contains(route)) {
                LinearLayout container = (LinearLayout) view.findViewById(R.id.volume_item_container);
                container.setVisibility(0);
                AnimationSet animSet = new AnimationSet(true);
                Animation alphaAnim = new AlphaAnimation(1.0f, 1.0f);
                alphaAnim.setDuration(0L);
                animSet.addAnimation(alphaAnim);
                Animation translationAnim = new TranslateAnimation(0.0f, 0.0f, 0.0f, 0.0f);
                translationAnim.setDuration(0L);
                animSet.setFillAfter(true);
                animSet.setFillEnabled(true);
                view.clearAnimation();
                view.startAnimation(animSet);
            }
        }
        this.mVolumeGroupList.stopAnimationAll();
        if (!exceptAddedRoutes) {
            finishAnimation(false);
        }
    }

    private void updatePlaybackControlLayout() {
        if (canShowPlaybackControlLayout()) {
            CharSequence title = this.mDescription == null ? null : this.mDescription.getTitle();
            boolean isPlaying = true;
            boolean hasTitle = !TextUtils.isEmpty(title);
            CharSequence subtitle = this.mDescription != null ? this.mDescription.getSubtitle() : null;
            boolean hasSubtitle = !TextUtils.isEmpty(subtitle);
            boolean showTitle = false;
            boolean showSubtitle = false;
            if (this.mRoute.getPresentationDisplayId() != -1) {
                this.mTitleView.setText(R.string.mr_controller_casting_screen);
                showTitle = true;
            } else if (this.mState == null || this.mState.getState() == 0) {
                this.mTitleView.setText(R.string.mr_controller_no_media_selected);
                showTitle = true;
            } else if (!hasTitle && !hasSubtitle) {
                this.mTitleView.setText(R.string.mr_controller_no_info_available);
                showTitle = true;
            } else {
                if (hasTitle) {
                    this.mTitleView.setText(title);
                    showTitle = true;
                }
                if (hasSubtitle) {
                    this.mSubtitleView.setText(subtitle);
                    showSubtitle = true;
                }
            }
            this.mTitleView.setVisibility(showTitle ? 0 : 8);
            this.mSubtitleView.setVisibility(showSubtitle ? 0 : 8);
            if (this.mState != null) {
                if (this.mState.getState() != 6 && this.mState.getState() != 3) {
                    isPlaying = false;
                }
                Context playbackControlButtonContext = this.mPlaybackControlButton.getContext();
                boolean visible = true;
                int iconDrawableAttr = 0;
                int iconDescResId = 0;
                if (isPlaying && isPauseActionSupported()) {
                    iconDrawableAttr = R.attr.mediaRoutePauseDrawable;
                    iconDescResId = R.string.mr_controller_pause;
                } else if (isPlaying && isStopActionSupported()) {
                    iconDrawableAttr = R.attr.mediaRouteStopDrawable;
                    iconDescResId = R.string.mr_controller_stop;
                } else if (!isPlaying && isPlayActionSupported()) {
                    iconDrawableAttr = R.attr.mediaRoutePlayDrawable;
                    iconDescResId = R.string.mr_controller_play;
                } else {
                    visible = false;
                }
                this.mPlaybackControlButton.setVisibility(visible ? 0 : 8);
                if (visible) {
                    this.mPlaybackControlButton.setImageResource(MediaRouterThemeHelper.getThemeResource(playbackControlButtonContext, iconDrawableAttr));
                    this.mPlaybackControlButton.setContentDescription(playbackControlButtonContext.getResources().getText(iconDescResId));
                }
            }
        }
    }

    private boolean isPlayActionSupported() {
        return (this.mState.getActions() & 516) != 0;
    }

    private boolean isPauseActionSupported() {
        return (this.mState.getActions() & 514) != 0;
    }

    private boolean isStopActionSupported() {
        return (this.mState.getActions() & 1) != 0;
    }

    boolean isVolumeControlAvailable(MediaRouter.RouteInfo route) {
        return this.mVolumeControlEnabled && route.getVolumeHandling() == 1;
    }

    private static int getLayoutHeight(View view) {
        return view.getLayoutParams().height;
    }

    static void setLayoutHeight(View view, int height) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.height = height;
        view.setLayoutParams(lp);
    }

    private static boolean uriEquals(Uri uri1, Uri uri2) {
        if (uri1 != null && uri1.equals(uri2)) {
            return true;
        }
        if (uri1 == null && uri2 == null) {
            return true;
        }
        return false;
    }

    int getDesiredArtHeight(int originalWidth, int originalHeight) {
        return originalWidth >= originalHeight ? (int) (((this.mDialogContentWidth * originalHeight) / originalWidth) + 0.5f) : (int) (((this.mDialogContentWidth * 9.0f) / 16.0f) + 0.5f);
    }

    void updateArtIconIfNeeded() {
        if (this.mCustomControlView != null || !isIconChanged()) {
            return;
        }
        if (this.mFetchArtTask != null) {
            this.mFetchArtTask.cancel(true);
        }
        this.mFetchArtTask = new FetchArtTask();
        this.mFetchArtTask.execute(new Void[0]);
    }

    void clearLoadedBitmap() {
        this.mArtIconIsLoaded = false;
        this.mArtIconLoadedBitmap = null;
        this.mArtIconBackgroundColor = 0;
    }

    private boolean isIconChanged() {
        Bitmap newBitmap = this.mDescription == null ? null : this.mDescription.getIconBitmap();
        Uri newUri = this.mDescription != null ? this.mDescription.getIconUri() : null;
        Bitmap oldBitmap = this.mFetchArtTask == null ? this.mArtIconBitmap : this.mFetchArtTask.getIconBitmap();
        Uri oldUri = this.mFetchArtTask == null ? this.mArtIconUri : this.mFetchArtTask.getIconUri();
        if (oldBitmap != newBitmap) {
            return true;
        }
        if (oldBitmap == null && !uriEquals(oldUri, newUri)) {
            return true;
        }
        return false;
    }

    private final class MediaRouterCallback extends MediaRouter.Callback {
        MediaRouterCallback() {
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            MediaRouteControllerDialog.this.update(false);
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            MediaRouteControllerDialog.this.update(true);
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            SeekBar volumeSlider = MediaRouteControllerDialog.this.mVolumeSliderMap.get(route);
            int volume = route.getVolume();
            if (MediaRouteControllerDialog.DEBUG) {
                Log.d("MediaRouteCtrlDialog", "onRouteVolumeChanged(), route.getVolume:" + volume);
            }
            if (volumeSlider != null && MediaRouteControllerDialog.this.mRouteInVolumeSliderTouched != route) {
                volumeSlider.setProgress(volume);
            }
        }
    }

    private final class MediaControllerCallback extends MediaControllerCompat.Callback {
        MediaControllerCallback() {
        }

        @Override
        public void onSessionDestroyed() {
            if (MediaRouteControllerDialog.this.mMediaController != null) {
                MediaRouteControllerDialog.this.mMediaController.unregisterCallback(MediaRouteControllerDialog.this.mControllerCallback);
                MediaRouteControllerDialog.this.mMediaController = null;
            }
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            MediaRouteControllerDialog.this.mState = state;
            MediaRouteControllerDialog.this.update(false);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            MediaRouteControllerDialog.this.mDescription = metadata == null ? null : metadata.getDescription();
            MediaRouteControllerDialog.this.updateArtIconIfNeeded();
            MediaRouteControllerDialog.this.update(false);
        }
    }

    private final class ClickListener implements View.OnClickListener {
        ClickListener() {
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == 16908313 || id == 16908314) {
                if (MediaRouteControllerDialog.this.mRoute.isSelected()) {
                    MediaRouteControllerDialog.this.mRouter.unselect(id == 16908313 ? 2 : 1);
                }
                MediaRouteControllerDialog.this.dismiss();
                return;
            }
            if (id == R.id.mr_control_playback_ctrl) {
                if (MediaRouteControllerDialog.this.mMediaController != null && MediaRouteControllerDialog.this.mState != null) {
                    int i = MediaRouteControllerDialog.this.mState.getState() != 3 ? 0 : 1;
                    int actionDescResId = 0;
                    if (i != 0 && MediaRouteControllerDialog.this.isPauseActionSupported()) {
                        MediaRouteControllerDialog.this.mMediaController.getTransportControls().pause();
                        actionDescResId = R.string.mr_controller_pause;
                    } else if (i != 0 && MediaRouteControllerDialog.this.isStopActionSupported()) {
                        MediaRouteControllerDialog.this.mMediaController.getTransportControls().stop();
                        actionDescResId = R.string.mr_controller_stop;
                    } else if (i == 0 && MediaRouteControllerDialog.this.isPlayActionSupported()) {
                        MediaRouteControllerDialog.this.mMediaController.getTransportControls().play();
                        actionDescResId = R.string.mr_controller_play;
                    }
                    if (MediaRouteControllerDialog.this.mAccessibilityManager != null && MediaRouteControllerDialog.this.mAccessibilityManager.isEnabled() && actionDescResId != 0) {
                        AccessibilityEvent event = AccessibilityEvent.obtain(16384);
                        event.setPackageName(MediaRouteControllerDialog.this.mContext.getPackageName());
                        event.setClassName(getClass().getName());
                        event.getText().add(MediaRouteControllerDialog.this.mContext.getString(actionDescResId));
                        MediaRouteControllerDialog.this.mAccessibilityManager.sendAccessibilityEvent(event);
                        return;
                    }
                    return;
                }
                return;
            }
            if (id == R.id.mr_close) {
                MediaRouteControllerDialog.this.dismiss();
            }
        }
    }

    private class VolumeChangeListener implements SeekBar.OnSeekBarChangeListener {
        private final Runnable mStopTrackingTouch = new Runnable() {
            @Override
            public void run() {
                if (MediaRouteControllerDialog.this.mRouteInVolumeSliderTouched != null) {
                    MediaRouteControllerDialog.this.mRouteInVolumeSliderTouched = null;
                    if (MediaRouteControllerDialog.this.mHasPendingUpdate) {
                        MediaRouteControllerDialog.this.update(MediaRouteControllerDialog.this.mPendingUpdateAnimationNeeded);
                    }
                }
            }
        };

        VolumeChangeListener() {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (MediaRouteControllerDialog.this.mRouteInVolumeSliderTouched != null) {
                MediaRouteControllerDialog.this.mVolumeSlider.removeCallbacks(this.mStopTrackingTouch);
            }
            MediaRouteControllerDialog.this.mRouteInVolumeSliderTouched = (MediaRouter.RouteInfo) seekBar.getTag();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            MediaRouteControllerDialog.this.mVolumeSlider.postDelayed(this.mStopTrackingTouch, 500L);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                MediaRouter.RouteInfo route = (MediaRouter.RouteInfo) seekBar.getTag();
                if (MediaRouteControllerDialog.DEBUG) {
                    Log.d("MediaRouteCtrlDialog", "onProgressChanged(): calling MediaRouter.RouteInfo.requestSetVolume(" + progress + ")");
                }
                route.requestSetVolume(progress);
            }
        }
    }

    private class VolumeGroupAdapter extends ArrayAdapter<MediaRouter.RouteInfo> {
        final float mDisabledAlpha;

        public VolumeGroupAdapter(Context context, List<MediaRouter.RouteInfo> objects) {
            super(context, 0, objects);
            this.mDisabledAlpha = MediaRouterThemeHelper.getDisabledAlpha(context);
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.mr_controller_volume_item, parent, false);
            } else {
                MediaRouteControllerDialog.this.updateVolumeGroupItemHeight(v);
            }
            MediaRouter.RouteInfo route = getItem(position);
            if (route != null) {
                boolean isEnabled = route.isEnabled();
                TextView routeName = (TextView) v.findViewById(R.id.mr_name);
                routeName.setEnabled(isEnabled);
                routeName.setText(route.getName());
                MediaRouteVolumeSlider volumeSlider = (MediaRouteVolumeSlider) v.findViewById(R.id.mr_volume_slider);
                MediaRouterThemeHelper.setVolumeSliderColor(parent.getContext(), volumeSlider, MediaRouteControllerDialog.this.mVolumeGroupList);
                volumeSlider.setTag(route);
                MediaRouteControllerDialog.this.mVolumeSliderMap.put(route, volumeSlider);
                volumeSlider.setHideThumb(!isEnabled);
                volumeSlider.setEnabled(isEnabled);
                if (isEnabled) {
                    if (MediaRouteControllerDialog.this.isVolumeControlAvailable(route)) {
                        volumeSlider.setMax(route.getVolumeMax());
                        volumeSlider.setProgress(route.getVolume());
                        volumeSlider.setOnSeekBarChangeListener(MediaRouteControllerDialog.this.mVolumeChangeListener);
                    } else {
                        volumeSlider.setMax(100);
                        volumeSlider.setProgress(100);
                        volumeSlider.setEnabled(false);
                    }
                }
                ImageView volumeItemIcon = (ImageView) v.findViewById(R.id.mr_volume_item_icon);
                volumeItemIcon.setAlpha(isEnabled ? 255 : (int) (255.0f * this.mDisabledAlpha));
                LinearLayout container = (LinearLayout) v.findViewById(R.id.volume_item_container);
                container.setVisibility(MediaRouteControllerDialog.this.mGroupMemberRoutesAnimatingWithBitmap.contains(route) ? 4 : 0);
                if (MediaRouteControllerDialog.this.mGroupMemberRoutesAdded != null && MediaRouteControllerDialog.this.mGroupMemberRoutesAdded.contains(route)) {
                    Animation alphaAnim = new AlphaAnimation(0.0f, 0.0f);
                    alphaAnim.setDuration(0L);
                    alphaAnim.setFillEnabled(true);
                    alphaAnim.setFillAfter(true);
                    v.clearAnimation();
                    v.startAnimation(alphaAnim);
                }
            }
            return v;
        }
    }

    private class FetchArtTask extends AsyncTask<Void, Void, Bitmap> {
        private int mBackgroundColor;
        private final Bitmap mIconBitmap;
        private final Uri mIconUri;
        private long mStartTimeMillis;

        FetchArtTask() {
            Bitmap bitmap = MediaRouteControllerDialog.this.mDescription == null ? null : MediaRouteControllerDialog.this.mDescription.getIconBitmap();
            if (MediaRouteControllerDialog.this.isBitmapRecycled(bitmap)) {
                Log.w("MediaRouteCtrlDialog", "Can't fetch the given art bitmap because it's already recycled.");
                bitmap = null;
            }
            this.mIconBitmap = bitmap;
            this.mIconUri = MediaRouteControllerDialog.this.mDescription != null ? MediaRouteControllerDialog.this.mDescription.getIconUri() : null;
        }

        public Bitmap getIconBitmap() {
            return this.mIconBitmap;
        }

        public Uri getIconUri() {
            return this.mIconUri;
        }

        @Override
        protected void onPreExecute() {
            this.mStartTimeMillis = SystemClock.uptimeMillis();
            MediaRouteControllerDialog.this.clearLoadedBitmap();
        }

        @Override
        protected Bitmap doInBackground(Void... voidArr) {
            Bitmap bitmapDecodeStream = null;
            Rect rect = null;
            Object[] objArr = 0;
            Object[] objArr2 = null;
            Object[] objArr3 = 0;
            Object[] objArr4 = null;
            Object[] objArr5 = 0;
            Object[] objArr6 = 0;
            Object[] objArr7 = 0;
            Object[] objArr8 = 0;
            Object[] objArr9 = 0;
            Object[] objArr10 = 0;
            if (this.mIconBitmap != null) {
                bitmapDecodeStream = this.mIconBitmap;
            } else if (this.mIconUri != null) {
                try {
                    try {
                        try {
                            InputStream inputStreamOpenInputStreamByScheme = openInputStreamByScheme(this.mIconUri);
                            InputStream inputStream = inputStreamOpenInputStreamByScheme;
                            if (inputStreamOpenInputStreamByScheme == null) {
                                Log.w("MediaRouteCtrlDialog", "Unable to open: " + this.mIconUri);
                                return objArr6 == true ? 1 : 0;
                            }
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeStream(inputStream, rect, options);
                            if (options.outWidth == 0 || options.outHeight == 0) {
                                if (inputStream != null) {
                                    try {
                                        inputStream.close();
                                    } catch (IOException e) {
                                    }
                                }
                                return objArr10 == true ? 1 : 0;
                            }
                            try {
                                inputStream.reset();
                            } catch (IOException e2) {
                                inputStream.close();
                                InputStream inputStreamOpenInputStreamByScheme2 = openInputStreamByScheme(this.mIconUri);
                                inputStream = inputStreamOpenInputStreamByScheme2;
                                if (inputStreamOpenInputStreamByScheme2 == null) {
                                    Log.w("MediaRouteCtrlDialog", "Unable to open: " + this.mIconUri);
                                    if (inputStream != null) {
                                        try {
                                            inputStream.close();
                                        } catch (IOException e3) {
                                        }
                                    }
                                    return objArr9 == true ? 1 : 0;
                                }
                            }
                            options.inJustDecodeBounds = false;
                            options.inSampleSize = Math.max(1, Integer.highestOneBit(options.outHeight / MediaRouteControllerDialog.this.getDesiredArtHeight(options.outWidth, options.outHeight)));
                            if (isCancelled()) {
                                if (inputStream != null) {
                                    try {
                                        inputStream.close();
                                    } catch (IOException e4) {
                                    }
                                }
                                return objArr7 == true ? 1 : 0;
                            }
                            bitmapDecodeStream = BitmapFactory.decodeStream(inputStream, objArr8 == true ? 1 : 0, options);
                            if (inputStream != null) {
                                inputStream.close();
                            }
                        } catch (IOException e5) {
                        }
                    } catch (IOException e6) {
                        Log.w("MediaRouteCtrlDialog", "Unable to open: " + this.mIconUri, e6);
                        if (objArr2 != null) {
                            (objArr == true ? 1 : 0).close();
                        }
                    }
                } finally {
                    if (objArr4 != null) {
                        try {
                            (objArr3 == true ? 1 : 0).close();
                        } catch (IOException e7) {
                        }
                    }
                }
            }
            if (MediaRouteControllerDialog.this.isBitmapRecycled(bitmapDecodeStream)) {
                Log.w("MediaRouteCtrlDialog", "Can't use recycled bitmap: " + bitmapDecodeStream);
                return objArr5 == true ? 1 : 0;
            }
            if (bitmapDecodeStream != null && bitmapDecodeStream.getWidth() < bitmapDecodeStream.getHeight()) {
                Palette paletteGenerate = new Palette.Builder(bitmapDecodeStream).maximumColorCount(1).generate();
                this.mBackgroundColor = paletteGenerate.getSwatches().isEmpty() ? 0 : paletteGenerate.getSwatches().get(0).getRgb();
            }
            return bitmapDecodeStream;
        }

        @Override
        protected void onPostExecute(Bitmap art) {
            MediaRouteControllerDialog.this.mFetchArtTask = null;
            if (!ObjectsCompat.equals(MediaRouteControllerDialog.this.mArtIconBitmap, this.mIconBitmap) || !ObjectsCompat.equals(MediaRouteControllerDialog.this.mArtIconUri, this.mIconUri)) {
                MediaRouteControllerDialog.this.mArtIconBitmap = this.mIconBitmap;
                MediaRouteControllerDialog.this.mArtIconLoadedBitmap = art;
                MediaRouteControllerDialog.this.mArtIconUri = this.mIconUri;
                MediaRouteControllerDialog.this.mArtIconBackgroundColor = this.mBackgroundColor;
                MediaRouteControllerDialog.this.mArtIconIsLoaded = true;
                long elapsedTimeMillis = SystemClock.uptimeMillis() - this.mStartTimeMillis;
                MediaRouteControllerDialog.this.update(elapsedTimeMillis > 120);
            }
        }

        private InputStream openInputStreamByScheme(Uri uri) throws IOException {
            InputStream stream;
            String scheme = uri.getScheme().toLowerCase();
            if ("android.resource".equals(scheme) || "content".equals(scheme) || "file".equals(scheme)) {
                stream = MediaRouteControllerDialog.this.mContext.getContentResolver().openInputStream(uri);
            } else {
                URL url = new URL(uri.toString());
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(MediaRouteControllerDialog.CONNECTION_TIMEOUT_MILLIS);
                conn.setReadTimeout(MediaRouteControllerDialog.CONNECTION_TIMEOUT_MILLIS);
                stream = conn.getInputStream();
            }
            if (stream == null) {
                return null;
            }
            return new BufferedInputStream(stream);
        }
    }
}
