package com.android.managedprovisioning.provisioning;

import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.DialogBuilder;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.SetupGlifLayoutActivity;
import com.android.managedprovisioning.common.SimpleDialog;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.CustomizationParams;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.preprovisioning.EncryptionController;
import com.android.setupwizardlib.GlifLayout;
import java.util.Iterator;
import java.util.Objects;

public class ProvisioningActivity extends SetupGlifLayoutActivity implements SimpleDialog.SimpleDialogListener, ProvisioningManagerCallback {
    private AnimatedVectorDrawable mAnimatedVectorDrawable;
    private final Animatable2.AnimationCallback mAnimationCallback;
    private ProvisioningParams mParams;
    private ProvisioningManager mProvisioningManager;
    private Handler mUiThreadHandler;

    class AnonymousClass1 extends Animatable2.AnimationCallback {
        AnonymousClass1() {
        }

        @Override
        public void onAnimationEnd(Drawable drawable) {
            super.onAnimationEnd(drawable);
            Handler handler = ProvisioningActivity.this.mUiThreadHandler;
            final AnimatedVectorDrawable animatedVectorDrawable = ProvisioningActivity.this.mAnimatedVectorDrawable;
            Objects.requireNonNull(animatedVectorDrawable);
            handler.post(new Runnable() {
                @Override
                public final void run() {
                    animatedVectorDrawable.start();
                }
            });
        }
    }

    public ProvisioningActivity() {
        this(null, new Utils());
    }

    public ProvisioningActivity(ProvisioningManager provisioningManager, Utils utils) {
        super(utils);
        this.mUiThreadHandler = new Handler();
        this.mAnimationCallback = new AnonymousClass1();
        this.mProvisioningManager = provisioningManager;
    }

    private ProvisioningManager getProvisioningManager() {
        if (this.mProvisioningManager == null) {
            this.mProvisioningManager = ProvisioningManager.getInstance(this);
        }
        return this.mProvisioningManager;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mParams = (ProvisioningParams) getIntent().getParcelableExtra("provisioningParams");
        initializeUi(this.mParams);
        if (bundle == null || !bundle.getBoolean("ProvisioningStarted")) {
            getProvisioningManager().maybeStartProvisioning(this.mParams);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("ProvisioningStarted", true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isAnyDialogAdded()) {
            getProvisioningManager().registerListener(this);
        }
        if (this.mAnimatedVectorDrawable != null) {
            this.mAnimatedVectorDrawable.registerAnimationCallback(this.mAnimationCallback);
            this.mAnimatedVectorDrawable.reset();
            this.mAnimatedVectorDrawable.start();
        }
    }

    private boolean isAnyDialogAdded() {
        return isDialogAdded("ErrorDialogOk") || isDialogAdded("ErrorDialogReset") || isDialogAdded("CancelProvisioningDialogOk") || isDialogAdded("CancelProvisioningDialogReset");
    }

    @Override
    public void onPause() {
        getProvisioningManager().unregisterListener(this);
        if (this.mAnimatedVectorDrawable != null) {
            this.mAnimatedVectorDrawable.stop();
            this.mAnimatedVectorDrawable.unregisterAnimationCallback(this.mAnimationCallback);
        }
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (this.mParams.skipUserConsent) {
            return;
        }
        showCancelProvisioningDialog();
    }

    @Override
    public void preFinalizationCompleted() {
        ProvisionLogger.logi("ProvisioningActivity pre-finalization completed");
        setResult(-1);
        maybeLaunchNfcUserSetupCompleteIntent();
        finish();
    }

    private void maybeLaunchNfcUserSetupCompleteIntent() {
        if (this.mParams != null && this.mParams.isNfc) {
            Intent intentAddFlags = new Intent("android.app.action.STATE_USER_SETUP_COMPLETE").addFlags(268435456);
            PackageManager packageManager = getPackageManager();
            ComponentName componentName = null;
            Iterator<ResolveInfo> it = packageManager.queryIntentActivities(intentAddFlags, 0).iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ResolveInfo next = it.next();
                if (next.activityInfo != null) {
                    if (!"android.permission.BIND_DEVICE_ADMIN".equals(next.activityInfo.permission)) {
                        ProvisionLogger.loge("Component " + next.activityInfo.getComponentName() + " is not protected by android.permission.BIND_DEVICE_ADMIN");
                    } else if (packageManager.checkPermission("android.permission.DISPATCH_PROVISIONING_MESSAGE", next.activityInfo.packageName) != 0) {
                        ProvisionLogger.loge("Package " + next.activityInfo.packageName + " does not have android.permission.DISPATCH_PROVISIONING_MESSAGE");
                    } else {
                        componentName = next.activityInfo.getComponentName();
                        break;
                    }
                }
            }
            if (componentName == null) {
                ProvisionLogger.logw("No activity accepts intent ACTION_STATE_USER_SETUP_COMPLETE");
                return;
            }
            intentAddFlags.setComponent(componentName);
            startActivity(intentAddFlags);
            ProvisionLogger.logi("Launched ACTION_STATE_USER_SETUP_COMPLETE with component " + componentName);
        }
    }

    @Override
    public void progressUpdate(int i) {
    }

    @Override
    public void error(int i, int i2, boolean z) {
        showDialog(new SimpleDialog.Builder().setTitle(Integer.valueOf(i)).setMessage(i2).setCancelable(false).setPositiveButtonMessage(z ? R.string.reset : R.string.device_owner_error_ok), z ? "ErrorDialogReset" : "ErrorDialogOk");
    }

    @Override
    protected void showDialog(DialogBuilder dialogBuilder, String str) {
        getProvisioningManager().unregisterListener(this);
        super.showDialog(dialogBuilder, str);
    }

    @Override
    protected int getMetricsCategory() {
        return 519;
    }

    private void showCancelProvisioningDialog() {
        int i;
        boolean zIsDeviceOwnerAction = getUtils().isDeviceOwnerAction(this.mParams.provisioningAction);
        String str = zIsDeviceOwnerAction ? "CancelProvisioningDialogReset" : "CancelProvisioningDialogOk";
        int i2 = zIsDeviceOwnerAction ? R.string.reset : R.string.profile_owner_cancel_ok;
        int i3 = zIsDeviceOwnerAction ? R.string.device_owner_cancel_cancel : R.string.profile_owner_cancel_cancel;
        if (zIsDeviceOwnerAction) {
            i = R.string.this_will_reset_take_back_first_screen;
        } else {
            i = R.string.profile_owner_cancel_message;
        }
        SimpleDialog.Builder positiveButtonMessage = new SimpleDialog.Builder().setCancelable(false).setMessage(i).setNegativeButtonMessage(i3).setPositiveButtonMessage(i2);
        if (zIsDeviceOwnerAction) {
            positiveButtonMessage.setTitle(Integer.valueOf(R.string.stop_setup_reset_device_question));
        }
        showDialog(positiveButtonMessage, str);
    }

    private void onProvisioningAborted() {
        setResult(0);
        finish();
    }

    @Override
    public void onNegativeButtonClick(DialogFragment dialogFragment) {
        byte b;
        String tag = dialogFragment.getTag();
        int iHashCode = tag.hashCode();
        if (iHashCode != -165848128) {
            b = (iHashCode == 933350667 && tag.equals("CancelProvisioningDialogOk")) ? (byte) 0 : (byte) -1;
        } else if (tag.equals("CancelProvisioningDialogReset")) {
            b = 1;
        }
        switch (b) {
            case 0:
            case EncryptionController.NOTIFICATION_ID:
                dialogFragment.dismiss();
                break;
            default:
                SimpleDialog.throwButtonClickHandlerNotImplemented(dialogFragment);
                break;
        }
        getProvisioningManager().registerListener(this);
    }

    @Override
    public void onPositiveButtonClick(DialogFragment dialogFragment) {
        byte b;
        String tag = dialogFragment.getTag();
        int iHashCode = tag.hashCode();
        if (iHashCode != -1597292065) {
            if (iHashCode != -327175348) {
                if (iHashCode != -165848128) {
                    b = (iHashCode == 933350667 && tag.equals("CancelProvisioningDialogOk")) ? (byte) 0 : (byte) -1;
                } else if (tag.equals("CancelProvisioningDialogReset")) {
                    b = 1;
                }
            } else if (tag.equals("ErrorDialogOk")) {
                b = 2;
            }
        } else if (tag.equals("ErrorDialogReset")) {
            b = 3;
        }
        switch (b) {
            case 0:
                getProvisioningManager().cancelProvisioning();
                onProvisioningAborted();
                break;
            case EncryptionController.NOTIFICATION_ID:
                getUtils().sendFactoryResetBroadcast(this, "DO provisioning cancelled by user");
                onProvisioningAborted();
                break;
            case 2:
                onProvisioningAborted();
                break;
            case 3:
                getUtils().sendFactoryResetBroadcast(this, "Error during DO provisioning");
                onProvisioningAborted();
                break;
            default:
                SimpleDialog.throwButtonClickHandlerNotImplemented(dialogFragment);
                break;
        }
    }

    private void initializeUi(ProvisioningParams provisioningParams) {
        boolean zIsDeviceOwnerAction = getUtils().isDeviceOwnerAction(provisioningParams.provisioningAction);
        int i = zIsDeviceOwnerAction ? R.string.setup_work_device : R.string.setting_up_workspace;
        int i2 = zIsDeviceOwnerAction ? R.string.setup_device_progress : R.string.setup_profile_progress;
        CustomizationParams customizationParamsCreateInstance = CustomizationParams.createInstance(this.mParams, this, this.mUtils);
        initializeLayoutParams(R.layout.progress, Integer.valueOf(i), customizationParamsCreateInstance.mainColor, customizationParamsCreateInstance.statusBarColor);
        setTitle(i2);
        GlifLayout glifLayout = (GlifLayout) findViewById(R.id.setup_wizard_layout);
        TextView textView = (TextView) glifLayout.findViewById(R.id.description);
        ImageView imageView = (ImageView) glifLayout.findViewById(R.id.animation);
        if (zIsDeviceOwnerAction) {
            textView.setText(R.string.device_owner_description);
            imageView.setImageResource(R.drawable.enterprise_do_animation);
        } else {
            textView.setText(R.string.work_profile_description);
            imageView.setImageResource(R.drawable.enterprise_wp_animation);
        }
        this.mAnimatedVectorDrawable = (AnimatedVectorDrawable) imageView.getDrawable();
    }
}
