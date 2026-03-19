package com.android.systemui.screenshot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.Toast;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.util.NotificationChannels;

class GlobalScreenshot {
    private ImageView mBackgroundView;
    private float mBgPadding;
    private float mBgPaddingScale;
    private MediaActionSound mCameraSound;
    private Context mContext;
    private Display mDisplay;
    private Matrix mDisplayMatrix;
    private DisplayMetrics mDisplayMetrics;
    private int mNotificationIconSize;
    private NotificationManager mNotificationManager;
    private final int mPreviewHeight;
    private final int mPreviewWidth;
    private AsyncTask<Void, Void, Void> mSaveInBgTask;
    private Bitmap mScreenBitmap;
    private AnimatorSet mScreenshotAnimation;
    private ImageView mScreenshotFlash;
    private View mScreenshotLayout;
    private ScreenshotSelectorView mScreenshotSelectorView;
    private ImageView mScreenshotView;
    private WindowManager.LayoutParams mWindowLayoutParams;
    private WindowManager mWindowManager;

    public GlobalScreenshot(Context context) {
        int dimensionPixelSize;
        Resources resources = context.getResources();
        this.mContext = context;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mDisplayMatrix = new Matrix();
        this.mScreenshotLayout = layoutInflater.inflate(R.layout.global_screenshot, (ViewGroup) null);
        this.mBackgroundView = (ImageView) this.mScreenshotLayout.findViewById(R.id.global_screenshot_background);
        this.mScreenshotView = (ImageView) this.mScreenshotLayout.findViewById(R.id.global_screenshot);
        this.mScreenshotFlash = (ImageView) this.mScreenshotLayout.findViewById(R.id.global_screenshot_flash);
        this.mScreenshotSelectorView = (ScreenshotSelectorView) this.mScreenshotLayout.findViewById(R.id.global_screenshot_selector);
        this.mScreenshotLayout.setFocusable(true);
        this.mScreenshotSelectorView.setFocusable(true);
        this.mScreenshotSelectorView.setFocusableInTouchMode(true);
        this.mScreenshotLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
        this.mWindowLayoutParams = new WindowManager.LayoutParams(-1, -1, 0, 0, 2036, 525568, -3);
        this.mWindowLayoutParams.setTitle("ScreenshotAnimation");
        this.mWindowLayoutParams.layoutInDisplayCutoutMode = 1;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
        this.mDisplayMetrics = new DisplayMetrics();
        this.mDisplay.getRealMetrics(this.mDisplayMetrics);
        this.mNotificationIconSize = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
        this.mBgPadding = resources.getDimensionPixelSize(R.dimen.global_screenshot_bg_padding);
        this.mBgPaddingScale = this.mBgPadding / this.mDisplayMetrics.widthPixels;
        try {
            dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.notification_panel_width);
        } catch (Resources.NotFoundException e) {
            dimensionPixelSize = 0;
        }
        this.mPreviewWidth = dimensionPixelSize <= 0 ? this.mDisplayMetrics.widthPixels : dimensionPixelSize;
        this.mPreviewHeight = resources.getDimensionPixelSize(R.dimen.notification_max_height);
        this.mCameraSound = new MediaActionSound();
        this.mCameraSound.load(0);
    }

    private void saveScreenshotInWorkerThread(Runnable runnable) {
        SaveImageInBackgroundData saveImageInBackgroundData = new SaveImageInBackgroundData();
        saveImageInBackgroundData.context = this.mContext;
        saveImageInBackgroundData.image = this.mScreenBitmap;
        saveImageInBackgroundData.iconSize = this.mNotificationIconSize;
        saveImageInBackgroundData.finisher = runnable;
        saveImageInBackgroundData.previewWidth = this.mPreviewWidth;
        saveImageInBackgroundData.previewheight = this.mPreviewHeight;
        if (this.mSaveInBgTask != null) {
            this.mSaveInBgTask.cancel(false);
        }
        this.mSaveInBgTask = new SaveImageInBackgroundTask(this.mContext, saveImageInBackgroundData, this.mNotificationManager).execute(new Void[0]);
    }

    private void takeScreenshot(Runnable runnable, boolean z, boolean z2, Rect rect) {
        this.mScreenBitmap = SurfaceControl.screenshot(rect, rect.width(), rect.height(), this.mDisplay.getRotation());
        if (this.mScreenBitmap == null) {
            notifyScreenshotError(this.mContext, this.mNotificationManager, R.string.screenshot_failed_to_capture_text);
            runnable.run();
        } else {
            this.mScreenBitmap.setHasAlpha(false);
            this.mScreenBitmap.prepareToDraw();
            startAnimation(runnable, this.mDisplayMetrics.widthPixels, this.mDisplayMetrics.heightPixels, z, z2);
        }
    }

    void takeScreenshot(Runnable runnable, boolean z, boolean z2) {
        this.mDisplay.getRealMetrics(this.mDisplayMetrics);
        takeScreenshot(runnable, z, z2, new Rect(0, 0, this.mDisplayMetrics.widthPixels, this.mDisplayMetrics.heightPixels));
    }

    void takeScreenshotPartial(final Runnable runnable, final boolean z, final boolean z2) {
        if (this.mScreenshotLayout.isAttachedToWindow()) {
            return;
        }
        this.mWindowManager.addView(this.mScreenshotLayout, this.mWindowLayoutParams);
        this.mScreenshotSelectorView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ScreenshotSelectorView screenshotSelectorView = (ScreenshotSelectorView) view;
                switch (motionEvent.getAction()) {
                    case 0:
                        screenshotSelectorView.startSelection((int) motionEvent.getX(), (int) motionEvent.getY());
                        break;
                    case 1:
                        screenshotSelectorView.setVisibility(8);
                        GlobalScreenshot.this.mWindowManager.removeView(GlobalScreenshot.this.mScreenshotLayout);
                        final Rect selectionRect = screenshotSelectorView.getSelectionRect();
                        if (selectionRect != null && selectionRect.width() != 0 && selectionRect.height() != 0) {
                            GlobalScreenshot.this.mScreenshotLayout.post(new Runnable() {
                                @Override
                                public void run() {
                                    GlobalScreenshot.this.takeScreenshot(runnable, z, z2, selectionRect);
                                }
                            });
                        }
                        screenshotSelectorView.stopSelection();
                        break;
                    case 2:
                        screenshotSelectorView.updateSelection((int) motionEvent.getX(), (int) motionEvent.getY());
                        break;
                }
                return true;
            }
        });
        this.mScreenshotLayout.post(new Runnable() {
            @Override
            public void run() {
                GlobalScreenshot.this.mScreenshotSelectorView.setVisibility(0);
                GlobalScreenshot.this.mScreenshotSelectorView.requestFocus();
            }
        });
    }

    void stopScreenshot() {
        if (this.mScreenshotSelectorView.getSelectionRect() != null) {
            this.mWindowManager.removeView(this.mScreenshotLayout);
            this.mScreenshotSelectorView.stopSelection();
        }
    }

    private void startAnimation(final Runnable runnable, int i, int i2, boolean z, boolean z2) {
        if (((PowerManager) this.mContext.getSystemService("power")).isPowerSaveMode()) {
            Toast.makeText(this.mContext, R.string.screenshot_saved_title, 0).show();
        }
        this.mScreenshotView.setImageBitmap(this.mScreenBitmap);
        this.mScreenshotLayout.requestFocus();
        if (this.mScreenshotAnimation != null) {
            if (this.mScreenshotAnimation.isStarted()) {
                this.mScreenshotAnimation.end();
            }
            this.mScreenshotAnimation.removeAllListeners();
        }
        this.mWindowManager.addView(this.mScreenshotLayout, this.mWindowLayoutParams);
        ValueAnimator valueAnimatorCreateScreenshotDropInAnimation = createScreenshotDropInAnimation();
        ValueAnimator valueAnimatorCreateScreenshotDropOutAnimation = createScreenshotDropOutAnimation(i, i2, z, z2);
        this.mScreenshotAnimation = new AnimatorSet();
        this.mScreenshotAnimation.playSequentially(valueAnimatorCreateScreenshotDropInAnimation, valueAnimatorCreateScreenshotDropOutAnimation);
        this.mScreenshotAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                GlobalScreenshot.this.saveScreenshotInWorkerThread(runnable);
                GlobalScreenshot.this.mWindowManager.removeView(GlobalScreenshot.this.mScreenshotLayout);
                GlobalScreenshot.this.mScreenBitmap = null;
                GlobalScreenshot.this.mScreenshotView.setImageBitmap(null);
            }
        });
        this.mScreenshotLayout.post(new Runnable() {
            @Override
            public void run() {
                GlobalScreenshot.this.mCameraSound.play(0);
                GlobalScreenshot.this.mScreenshotView.setLayerType(2, null);
                GlobalScreenshot.this.mScreenshotView.buildLayer();
                GlobalScreenshot.this.mScreenshotAnimation.start();
            }
        });
    }

    private ValueAnimator createScreenshotDropInAnimation() {
        final Interpolator interpolator = new Interpolator() {
            @Override
            public float getInterpolation(float f) {
                if (f <= 0.60465115f) {
                    return (float) Math.sin(3.141592653589793d * ((double) (f / 0.60465115f)));
                }
                return 0.0f;
            }
        };
        final Interpolator interpolator2 = new Interpolator() {
            @Override
            public float getInterpolation(float f) {
                if (f < 0.30232558f) {
                    return 0.0f;
                }
                return (f - 0.60465115f) / 0.39534885f;
            }
        };
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.setDuration(430L);
        valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                GlobalScreenshot.this.mBackgroundView.setAlpha(0.0f);
                GlobalScreenshot.this.mBackgroundView.setVisibility(0);
                GlobalScreenshot.this.mScreenshotView.setAlpha(0.0f);
                GlobalScreenshot.this.mScreenshotView.setTranslationX(0.0f);
                GlobalScreenshot.this.mScreenshotView.setTranslationY(0.0f);
                GlobalScreenshot.this.mScreenshotView.setScaleX(GlobalScreenshot.this.mBgPaddingScale + 1.0f);
                GlobalScreenshot.this.mScreenshotView.setScaleY(1.0f + GlobalScreenshot.this.mBgPaddingScale);
                GlobalScreenshot.this.mScreenshotView.setVisibility(0);
                GlobalScreenshot.this.mScreenshotFlash.setAlpha(0.0f);
                GlobalScreenshot.this.mScreenshotFlash.setVisibility(0);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                GlobalScreenshot.this.mScreenshotFlash.setVisibility(8);
            }
        });
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                float interpolation = (1.0f + GlobalScreenshot.this.mBgPaddingScale) - (interpolator2.getInterpolation(fFloatValue) * 0.27499998f);
                GlobalScreenshot.this.mBackgroundView.setAlpha(interpolator2.getInterpolation(fFloatValue) * 0.5f);
                GlobalScreenshot.this.mScreenshotView.setAlpha(fFloatValue);
                GlobalScreenshot.this.mScreenshotView.setScaleX(interpolation);
                GlobalScreenshot.this.mScreenshotView.setScaleY(interpolation);
                GlobalScreenshot.this.mScreenshotFlash.setAlpha(interpolator.getInterpolation(fFloatValue));
            }
        });
        return valueAnimatorOfFloat;
    }

    private ValueAnimator createScreenshotDropOutAnimation(int i, int i2, boolean z, boolean z2) {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.setStartDelay(500L);
        valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                GlobalScreenshot.this.mBackgroundView.setVisibility(8);
                GlobalScreenshot.this.mScreenshotView.setVisibility(8);
                GlobalScreenshot.this.mScreenshotView.setLayerType(0, null);
            }
        });
        if (!z || !z2) {
            valueAnimatorOfFloat.setDuration(320L);
            valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    float f = (0.725f + GlobalScreenshot.this.mBgPaddingScale) - (0.125f * fFloatValue);
                    float f2 = 1.0f - fFloatValue;
                    GlobalScreenshot.this.mBackgroundView.setAlpha(0.5f * f2);
                    GlobalScreenshot.this.mScreenshotView.setAlpha(f2);
                    GlobalScreenshot.this.mScreenshotView.setScaleX(f);
                    GlobalScreenshot.this.mScreenshotView.setScaleY(f);
                }
            });
        } else {
            final Interpolator interpolator = new Interpolator() {
                @Override
                public float getInterpolation(float f) {
                    if (f < 0.8604651f) {
                        return (float) (1.0d - Math.pow(1.0f - (f / 0.8604651f), 2.0d));
                    }
                    return 1.0f;
                }
            };
            float f = (i - (this.mBgPadding * 2.0f)) / 2.0f;
            float f2 = (i2 - (this.mBgPadding * 2.0f)) / 2.0f;
            final PointF pointF = new PointF((-f) + (f * 0.45f), (-f2) + (0.45f * f2));
            valueAnimatorOfFloat.setDuration(430L);
            valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    float interpolation = (0.725f + GlobalScreenshot.this.mBgPaddingScale) - (interpolator.getInterpolation(fFloatValue) * 0.27500004f);
                    GlobalScreenshot.this.mBackgroundView.setAlpha((1.0f - fFloatValue) * 0.5f);
                    GlobalScreenshot.this.mScreenshotView.setAlpha(1.0f - interpolator.getInterpolation(fFloatValue));
                    GlobalScreenshot.this.mScreenshotView.setScaleX(interpolation);
                    GlobalScreenshot.this.mScreenshotView.setScaleY(interpolation);
                    GlobalScreenshot.this.mScreenshotView.setTranslationX(pointF.x * fFloatValue);
                    GlobalScreenshot.this.mScreenshotView.setTranslationY(fFloatValue * pointF.y);
                }
            });
        }
        return valueAnimatorOfFloat;
    }

    static void notifyScreenshotError(Context context, NotificationManager notificationManager, int i) {
        Resources resources = context.getResources();
        String string = resources.getString(i);
        Notification.Builder color = new Notification.Builder(context, NotificationChannels.ALERTS).setTicker(resources.getString(R.string.screenshot_failed_title)).setContentTitle(resources.getString(R.string.screenshot_failed_title)).setContentText(string).setSmallIcon(R.drawable.stat_notify_image_error).setWhen(System.currentTimeMillis()).setVisibility(1).setCategory("err").setAutoCancel(true).setColor(context.getColor(android.R.color.car_colorPrimary));
        Intent intentCreateAdminSupportIntent = ((DevicePolicyManager) context.getSystemService("device_policy")).createAdminSupportIntent("policy_disable_screen_capture");
        if (intentCreateAdminSupportIntent != null) {
            color.setContentIntent(PendingIntent.getActivityAsUser(context, 0, intentCreateAdminSupportIntent, 67108864, null, UserHandle.CURRENT));
        }
        SystemUI.overrideNotificationAppName(context, color, true);
        notificationManager.notify(1, new Notification.BigTextStyle(color).bigText(string).build());
    }

    public static class ScreenshotActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                ActivityManager.getService().closeSystemDialogs("screenshot");
            } catch (RemoteException e) {
            }
            Intent intentAddFlags = (Intent) intent.getParcelableExtra("android:screenshot_sharing_intent");
            String string = context.getResources().getString(R.string.config_screenshotEditor);
            if (intentAddFlags.getAction() == "android.intent.action.EDIT" && string != null && string.length() > 0) {
                intentAddFlags.setComponent(ComponentName.unflattenFromString(string));
                ((NotificationManager) context.getSystemService("notification")).cancel(1);
            } else {
                intentAddFlags = Intent.createChooser(intentAddFlags, null, PendingIntent.getBroadcast(context, 0, new Intent(context, (Class<?>) TargetChosenReceiver.class), 1342177280).getIntentSender()).addFlags(268468224);
            }
            ActivityOptions activityOptionsMakeBasic = ActivityOptions.makeBasic();
            activityOptionsMakeBasic.setDisallowEnterPictureInPictureWhileLaunching(true);
            context.startActivityAsUser(intentAddFlags, activityOptionsMakeBasic.toBundle(), UserHandle.CURRENT);
        }
    }

    public static class TargetChosenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ((NotificationManager) context.getSystemService("notification")).cancel(1);
        }
    }

    public static class DeleteScreenshotReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.hasExtra("android:screenshot_uri_id")) {
                return;
            }
            NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
            Uri uri = Uri.parse(intent.getStringExtra("android:screenshot_uri_id"));
            notificationManager.cancel(1);
            new DeleteImageInBackgroundTask(context).execute(uri);
        }
    }
}
