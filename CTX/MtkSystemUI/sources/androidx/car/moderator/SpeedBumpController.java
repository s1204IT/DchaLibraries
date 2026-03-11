package androidx.car.moderator;

import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.car.settings.CarConfigurationManager;
import android.car.settings.SpeedBumpConfiguration;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.car.R;

class SpeedBumpController {
    private final Car mCar;
    private CarUxRestrictionsManager mCarUxRestrictionsManager;
    private final Context mContext;
    private final int mLockOutMessageDurationMs;
    private final ImageView mLockoutImageView;
    private final View mLockoutMessageView;
    private final ContentRateLimiter mContentRateLimiter = new ContentRateLimiter(0.5d, 5.0d, 600);
    private boolean mInteractionPermitted = true;
    private final Handler mHandler = new Handler();
    private final ServiceConnection mServiceConnection = new AnonymousClass3();

    SpeedBumpController(SpeedBumpView speedBumpView) {
        this.mContext = speedBumpView.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(this.mContext);
        this.mLockoutMessageView = layoutInflater.inflate(R.layout.lock_out_message, (ViewGroup) speedBumpView, false);
        this.mLockoutImageView = (ImageView) this.mLockoutMessageView.findViewById(R.id.lock_out_drawable);
        this.mLockOutMessageDurationMs = this.mContext.getResources().getInteger(R.integer.speed_bump_lock_out_duration_ms);
        this.mCar = Car.createCar(this.mContext, this.mServiceConnection);
        this.mContentRateLimiter.setUnlimitedMode(true);
    }

    void start() {
        try {
            if (this.mCar != null && !this.mCar.isConnected()) {
                this.mCar.connect();
            }
        } catch (IllegalStateException e) {
            Log.w("SpeedBumpController", "start(); cannot connect to Car");
        }
    }

    void stop() {
        if (this.mCarUxRestrictionsManager != null) {
            try {
                this.mCarUxRestrictionsManager.unregisterListener();
            } catch (CarNotConnectedException e) {
                Log.w("SpeedBumpController", "stop(); cannot unregister listener.");
            }
            this.mCarUxRestrictionsManager = null;
        }
        try {
            if (this.mCar != null && this.mCar.isConnected()) {
                this.mCar.disconnect();
            }
        } catch (IllegalStateException e2) {
            Log.w("SpeedBumpController", "stop(); cannot disconnect from Car.");
        }
    }

    View getLockoutMessageView() {
        return this.mLockoutMessageView;
    }

    boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == 3 || action == 1) {
            boolean nextActionPermitted = this.mContentRateLimiter.tryAcquire();
            if (this.mInteractionPermitted && !nextActionPermitted) {
                this.mInteractionPermitted = false;
                showLockOutMessage();
                return true;
            }
        }
        return this.mInteractionPermitted;
    }

    private void showLockOutMessage() {
        if (this.mLockoutMessageView.getVisibility() == 0) {
            return;
        }
        Animation lockOutMessageIn = AnimationUtils.loadAnimation(this.mContext, R.anim.lock_out_message_in);
        lockOutMessageIn.setAnimationListener(new AnonymousClass1());
        this.mLockoutMessageView.clearAnimation();
        this.mLockoutMessageView.startAnimation(lockOutMessageIn);
        ((AnimatedVectorDrawable) this.mLockoutImageView.getDrawable()).start();
    }

    class AnonymousClass1 implements Animation.AnimationListener {
        AnonymousClass1() {
        }

        @Override
        public void onAnimationStart(Animation animation) {
            SpeedBumpController.this.mLockoutMessageView.setVisibility(0);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            Handler handler = SpeedBumpController.this.mHandler;
            final SpeedBumpController speedBumpController = SpeedBumpController.this;
            handler.postDelayed(new Runnable() {
                @Override
                public final void run() {
                    speedBumpController.hideLockOutMessage();
                }
            }, SpeedBumpController.this.mLockOutMessageDurationMs);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }

    public void hideLockOutMessage() {
        if (this.mLockoutMessageView.getVisibility() != 0) {
            return;
        }
        Animation lockOutMessageOut = AnimationUtils.loadAnimation(this.mContext, R.anim.lock_out_message_out);
        lockOutMessageOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                SpeedBumpController.this.mLockoutMessageView.setVisibility(8);
                SpeedBumpController.this.mInteractionPermitted = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        this.mLockoutMessageView.startAnimation(lockOutMessageOut);
    }

    public void updateUnlimitedModeEnabled(CarUxRestrictions restrictions) {
        this.mContentRateLimiter.setUnlimitedMode(!restrictions.isRequiresDistractionOptimization());
    }

    class AnonymousClass3 implements ServiceConnection {
        AnonymousClass3() {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                SpeedBumpController.this.mCarUxRestrictionsManager = (CarUxRestrictionsManager) SpeedBumpController.this.mCar.getCarManager("uxrestriction");
                CarUxRestrictionsManager carUxRestrictionsManager = SpeedBumpController.this.mCarUxRestrictionsManager;
                final SpeedBumpController speedBumpController = SpeedBumpController.this;
                carUxRestrictionsManager.registerListener(new CarUxRestrictionsManager.OnUxRestrictionsChangedListener() {
                    @Override
                    public final void onUxRestrictionsChanged(CarUxRestrictions carUxRestrictions) {
                        speedBumpController.updateUnlimitedModeEnabled(carUxRestrictions);
                    }
                });
                SpeedBumpController.this.updateUnlimitedModeEnabled(SpeedBumpController.this.mCarUxRestrictionsManager.getCurrentCarUxRestrictions());
                CarConfigurationManager configManager = (CarConfigurationManager) SpeedBumpController.this.mCar.getCarManager("configuration");
                SpeedBumpConfiguration speedBumpConfiguration = configManager.getSpeedBumpConfiguration();
                SpeedBumpController.this.mContentRateLimiter.setAcquiredPermitsRate(speedBumpConfiguration.getAcquiredPermitsPerSecond());
                SpeedBumpController.this.mContentRateLimiter.setMaxStoredPermits(speedBumpConfiguration.getMaxPermitPool());
                SpeedBumpController.this.mContentRateLimiter.setPermitFillDelay(speedBumpConfiguration.getPermitFillDelay());
            } catch (CarNotConnectedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            SpeedBumpController.this.mCarUxRestrictionsManager = null;
        }
    }
}
