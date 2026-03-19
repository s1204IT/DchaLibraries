package com.android.managedprovisioning.task;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.task.CrossProfileIntentFilter;
import java.util.Arrays;
import java.util.List;

public class CrossProfileIntentFiltersSetter {
    private final PackageManager mPackageManager;
    private final UserManager mUserManager;
    private static final CrossProfileIntentFilter EMERGENCY_CALL_MIME = new CrossProfileIntentFilter.Builder(0, 2, false).addAction("android.intent.action.CALL_EMERGENCY").addAction("android.intent.action.CALL_PRIVILEGED").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.BROWSABLE").addDataType("vnd.android.cursor.item/phone").addDataType("vnd.android.cursor.item/phone_v2").addDataType("vnd.android.cursor.item/person").addDataType("vnd.android.cursor.dir/calls").addDataType("vnd.android.cursor.item/calls").build();
    private static final CrossProfileIntentFilter EMERGENCY_CALL_DATA = new CrossProfileIntentFilter.Builder(0, 2, false).addAction("android.intent.action.CALL_EMERGENCY").addAction("android.intent.action.CALL_PRIVILEGED").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.BROWSABLE").addDataScheme("tel").addDataScheme("sip").addDataScheme("voicemail").build();
    private static final CrossProfileIntentFilter DIAL_MIME = new CrossProfileIntentFilter.Builder(0, 4, false).addAction("android.intent.action.DIAL").addAction("android.intent.action.VIEW").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.BROWSABLE").addDataType("vnd.android.cursor.item/phone").addDataType("vnd.android.cursor.item/phone_v2").addDataType("vnd.android.cursor.item/person").addDataType("vnd.android.cursor.dir/calls").addDataType("vnd.android.cursor.item/calls").build();
    private static final CrossProfileIntentFilter DIAL_DATA = new CrossProfileIntentFilter.Builder(0, 4, false).addAction("android.intent.action.DIAL").addAction("android.intent.action.VIEW").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.BROWSABLE").addDataScheme("tel").addDataScheme("sip").addDataScheme("voicemail").build();
    private static final CrossProfileIntentFilter DIAL_RAW = new CrossProfileIntentFilter.Builder(0, 4, false).addAction("android.intent.action.DIAL").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.BROWSABLE").build();
    private static final CrossProfileIntentFilter CALL_BUTTON = new CrossProfileIntentFilter.Builder(0, 4, false).addAction("android.intent.action.CALL_BUTTON").addCategory("android.intent.category.DEFAULT").build();
    private static final CrossProfileIntentFilter SMS_MMS = new CrossProfileIntentFilter.Builder(0, 2, false).addAction("android.intent.action.VIEW").addAction("android.intent.action.SENDTO").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.BROWSABLE").addDataScheme("sms").addDataScheme("smsto").addDataScheme("mms").addDataScheme("mmsto").build();
    private static final CrossProfileIntentFilter MOBILE_NETWORK_SETTINGS = new CrossProfileIntentFilter.Builder(0, 2, false).addAction("android.settings.DATA_ROAMING_SETTINGS").addAction("android.settings.NETWORK_OPERATOR_SETTINGS").addCategory("android.intent.category.DEFAULT").build();

    @VisibleForTesting
    static final CrossProfileIntentFilter HOME = new CrossProfileIntentFilter.Builder(0, 2, false).addAction("android.intent.action.MAIN").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.HOME").build();
    private static final CrossProfileIntentFilter GET_CONTENT = new CrossProfileIntentFilter.Builder(0, 0, true).addAction("android.intent.action.GET_CONTENT").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.OPENABLE").addDataType("*/*").build();
    private static final CrossProfileIntentFilter OPEN_DOCUMENT = new CrossProfileIntentFilter.Builder(0, 0, true).addAction("android.intent.action.OPEN_DOCUMENT").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.OPENABLE").addDataType("*/*").build();
    private static final CrossProfileIntentFilter ACTION_PICK_DATA = new CrossProfileIntentFilter.Builder(0, 0, true).addAction("android.intent.action.PICK").addCategory("android.intent.category.DEFAULT").addDataType("*/*").build();
    private static final CrossProfileIntentFilter ACTION_PICK_RAW = new CrossProfileIntentFilter.Builder(0, 0, true).addAction("android.intent.action.PICK").addCategory("android.intent.category.DEFAULT").build();
    private static final CrossProfileIntentFilter RECOGNIZE_SPEECH = new CrossProfileIntentFilter.Builder(0, 0, false).addAction("android.speech.action.RECOGNIZE_SPEECH").addCategory("android.intent.category.DEFAULT").build();
    private static final CrossProfileIntentFilter MEDIA_CAPTURE = new CrossProfileIntentFilter.Builder(0, 0, true).addAction("android.media.action.IMAGE_CAPTURE").addAction("android.media.action.IMAGE_CAPTURE_SECURE").addAction("android.media.action.VIDEO_CAPTURE").addAction("android.provider.MediaStore.RECORD_SOUND").addAction("android.media.action.STILL_IMAGE_CAMERA").addAction("android.media.action.STILL_IMAGE_CAMERA_SECURE").addAction("android.media.action.VIDEO_CAMERA").addCategory("android.intent.category.DEFAULT").build();
    private static final CrossProfileIntentFilter SET_ALARM = new CrossProfileIntentFilter.Builder(0, 0, false).addAction("android.intent.action.SET_ALARM").addAction("android.intent.action.SHOW_ALARMS").addAction("android.intent.action.SET_TIMER").addCategory("android.intent.category.DEFAULT").build();

    @VisibleForTesting
    static final CrossProfileIntentFilter ACTION_SEND = new CrossProfileIntentFilter.Builder(1, 0, true).addAction("android.intent.action.SEND").addAction("android.intent.action.SEND_MULTIPLE").addCategory("android.intent.category.DEFAULT").addDataType("*/*").build();
    private static final CrossProfileIntentFilter USB_DEVICE_ATTACHED = new CrossProfileIntentFilter.Builder(1, 0, false).addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED").addAction("android.hardware.usb.action.USB_ACCESSORY_ATTACHED").addCategory("android.intent.category.DEFAULT").build();

    @VisibleForTesting
    static final List<CrossProfileIntentFilter> FILTERS = Arrays.asList(EMERGENCY_CALL_MIME, EMERGENCY_CALL_DATA, DIAL_MIME, DIAL_DATA, DIAL_RAW, CALL_BUTTON, SMS_MMS, SET_ALARM, MEDIA_CAPTURE, RECOGNIZE_SPEECH, ACTION_PICK_RAW, ACTION_PICK_DATA, OPEN_DOCUMENT, GET_CONTENT, USB_DEVICE_ATTACHED, ACTION_SEND, HOME, MOBILE_NETWORK_SETTINGS);

    public static class RestrictionChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int intExtra;
            UserInfo profileParent;
            if (!"android.app.action.DATA_SHARING_RESTRICTION_CHANGED".equals(intent.getAction()) || (intExtra = intent.getIntExtra("android.intent.extra.USER_ID", -10000)) == -10000 || (profileParent = ((UserManager) context.getSystemService(UserManager.class)).getProfileParent(intExtra)) == null) {
                return;
            }
            ProvisionLogger.logd("Resetting cross-profile intent filters on restriction change");
            new CrossProfileIntentFiltersSetter(context).resetFilters(profileParent.id);
            context.sendBroadcastAsUser(new Intent("android.app.action.DATA_SHARING_RESTRICTION_APPLIED"), UserHandle.of(intExtra));
        }
    }

    public CrossProfileIntentFiltersSetter(Context context) {
        this(context.getPackageManager(), (UserManager) context.getSystemService("user"));
    }

    @VisibleForTesting
    CrossProfileIntentFiltersSetter(PackageManager packageManager, UserManager userManager) {
        this.mPackageManager = (PackageManager) Preconditions.checkNotNull(packageManager);
        this.mUserManager = (UserManager) Preconditions.checkNotNull(userManager);
    }

    public void setFilters(int i, int i2) {
        ProvisionLogger.logd("Setting cross-profile intent filters");
        boolean zHasUserRestriction = this.mUserManager.hasUserRestriction("no_sharing_into_profile", UserHandle.of(i2));
        for (CrossProfileIntentFilter crossProfileIntentFilter : FILTERS) {
            if (!zHasUserRestriction || !crossProfileIntentFilter.letsPersonalDataIntoProfile) {
                if (crossProfileIntentFilter.direction == 0) {
                    this.mPackageManager.addCrossProfileIntentFilter(crossProfileIntentFilter.filter, i2, i, crossProfileIntentFilter.flags);
                } else {
                    this.mPackageManager.addCrossProfileIntentFilter(crossProfileIntentFilter.filter, i, i2, crossProfileIntentFilter.flags);
                }
            }
        }
    }

    public void resetFilters(int i) {
        List<UserInfo> profiles = this.mUserManager.getProfiles(i);
        if (profiles.size() <= 1) {
            return;
        }
        this.mPackageManager.clearCrossProfileIntentFilters(i);
        for (UserInfo userInfo : profiles) {
            if (userInfo.isManagedProfile()) {
                this.mPackageManager.clearCrossProfileIntentFilters(userInfo.id);
                setFilters(i, userInfo.id);
            }
        }
    }
}
