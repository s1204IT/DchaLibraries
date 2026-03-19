package com.android.services.telephony;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.services.telephony.PstnPhoneCapabilitiesNotifier;
import com.android.services.telephony.TelecomAccountRegistry;
import com.mediatek.phone.ext.ExtensionManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class TelecomAccountRegistry {
    private static final boolean DBG = false;
    private static final int DEFAULT_SIM_ICON = 2131165314;
    private static final String GROUP_PREFIX = "group_";
    private static TelecomAccountRegistry sInstance;
    protected final Context mContext;
    protected final SubscriptionManager mSubscriptionManager;
    protected final TelecomManager mTelecomManager;
    private TelephonyConnectionService mTelephonyConnectionService;
    protected final TelephonyManager mTelephonyManager;
    private SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            TelecomAccountRegistry.this.tearDownAccounts();
            TelecomAccountRegistry.this.setupAccounts();
        }
    };
    private final BroadcastReceiver mUserSwitchedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(this, "User changed, re-registering phone accounts.", new Object[0]);
            UserHandle userHandle = new UserHandle(intent.getIntExtra("android.intent.extra.user_handle", 0));
            TelecomAccountRegistry.this.mIsPrimaryUser = UserManager.get(TelecomAccountRegistry.this.mContext).getPrimaryUser().getUserHandle().equals(userHandle);
            TelecomAccountRegistry.this.tearDownAccounts();
            TelecomAccountRegistry.this.setupAccounts();
        }
    };
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            int state = serviceState.getState();
            if (state == 0 && TelecomAccountRegistry.this.mServiceState != state) {
                TelecomAccountRegistry.this.tearDownAccounts();
                TelecomAccountRegistry.this.setupAccounts();
            }
            TelecomAccountRegistry.this.mServiceState = state;
        }
    };
    protected List<AccountEntry> mAccounts = new LinkedList();
    protected final Object mAccountsLock = new Object();
    private List<Phone> mNoAccountPhones = new LinkedList();
    private List<PstnNoAccountUnknownCallNotifier> mNoAccountUnknownCallNotifiers = new LinkedList();
    private int mServiceState = 3;
    protected boolean mIsPrimaryUser = true;
    private List<PstnIncomingCallNotifier> mIncomingCallNotifiers = new ArrayList();

    public class AccountEntry implements PstnPhoneCapabilitiesNotifier.Listener {
        public PhoneAccount mAccount;
        private PersistableBundle mCarrierConfigBundle;
        protected PstnIncomingCallNotifier mIncomingCallNotifier;
        private boolean mIsDummy;
        private boolean mIsEmergency;
        protected boolean mIsManageImsConferenceCallSupported;
        protected boolean mIsMergeCallSupported;
        protected boolean mIsMergeImsCallSupported;
        protected boolean mIsMergeOfWifiCallsAllowedWhenVoWifiOff;
        protected boolean mIsShowPreciseFailedCause;
        protected boolean mIsVideoCapable;
        protected boolean mIsVideoConferencingSupported;
        protected boolean mIsVideoPauseSupported;
        protected boolean mIsVideoPresenceSupported;
        public final Phone mPhone;
        private final PstnPhoneCapabilitiesNotifier mPhoneCapabilitiesNotifier;

        public AccountEntry(Phone phone, boolean z, boolean z2) {
            this.mPhone = phone;
            this.mIsEmergency = z;
            this.mIsDummy = z2;
            this.mAccount = registerPstnPhoneAccount(z, z2);
            Log.i(this, "Registered phoneAccount: %s with handle: %s", this.mAccount, this.mAccount.getAccountHandle());
            this.mPhoneCapabilitiesNotifier = new PstnPhoneCapabilitiesNotifier(this.mPhone, this);
        }

        void teardown() {
            this.mPhoneCapabilitiesNotifier.teardown();
        }

        protected PhoneAccount registerPstnPhoneAccount(boolean z, boolean z2) {
            Icon iconCreateWithBitmap;
            CharSequence string;
            int simSlotIndex;
            int iconTint;
            String string2;
            int i;
            int i2;
            String string3;
            String networkOperatorName;
            int i3;
            boolean zIsVtEnabledByPlatform;
            Bundle bundle;
            boolean z3;
            Bundle bundle2;
            String[] mergedSubscriberIds;
            boolean z4;
            String str = z2 ? "Dummy " : "";
            PhoneAccountHandle phoneAccountHandleMakePstnPhoneAccountHandleWithPrefix = PhoneUtils.makePstnPhoneAccountHandleWithPrefix(this.mPhone, str, z);
            int subId = this.mPhone.getSubId();
            this.mCarrierConfigBundle = PhoneGlobals.getInstance().getCarrierConfigForSubId(subId);
            String subscriberId = this.mPhone.getSubscriberId();
            String line1Number = TelecomAccountRegistry.this.mTelephonyManager.getLine1Number(subId);
            if (line1Number == null) {
                line1Number = "";
            }
            String line1Number2 = this.mPhone.getLine1Number();
            if (line1Number2 == null) {
                line1Number2 = "";
            }
            SubscriptionInfo activeSubscriptionInfo = TelecomAccountRegistry.this.mSubscriptionManager.getActiveSubscriptionInfo(subId);
            if (!z) {
                if (TelecomAccountRegistry.this.mTelephonyManager.getPhoneCount() == 1) {
                    networkOperatorName = TelecomAccountRegistry.this.mTelephonyManager.getNetworkOperatorName();
                    string3 = networkOperatorName;
                } else {
                    if (activeSubscriptionInfo != null) {
                        string = activeSubscriptionInfo.getDisplayName();
                        simSlotIndex = activeSubscriptionInfo.getSimSlotIndex();
                        iconTint = activeSubscriptionInfo.getIconTint();
                        iconCreateWithBitmap = Icon.createWithBitmap(activeSubscriptionInfo.createIconBitmap(TelecomAccountRegistry.this.mContext));
                    } else {
                        iconCreateWithBitmap = null;
                        string = null;
                        simSlotIndex = -1;
                        iconTint = 0;
                    }
                    if (SubscriptionManager.isValidSlotIndex(simSlotIndex)) {
                        string2 = Integer.toString(simSlotIndex);
                    } else {
                        string2 = TelecomAccountRegistry.this.mContext.getResources().getString(R.string.unknown);
                    }
                    if (TextUtils.isEmpty(string)) {
                        Log.w(this, "Could not get a display name for subid: %d", Integer.valueOf(subId));
                        string = TelecomAccountRegistry.this.mContext.getResources().getString(R.string.sim_description_default, string2);
                    }
                    i = simSlotIndex;
                    i2 = iconTint;
                    string3 = str + TelecomAccountRegistry.this.mContext.getResources().getString(R.string.sim_description_default, string2);
                    networkOperatorName = str + ((Object) string);
                    i3 = TelecomAccountRegistry.this.mContext.getResources().getBoolean(R.bool.config_pstnCanPlaceEmergencyCalls) ? 54 : 38;
                    this.mIsVideoCapable = this.mPhone.isVideoEnabled();
                    zIsVtEnabledByPlatform = ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId()).isVtEnabledByPlatform();
                    if (!TelecomAccountRegistry.this.mIsPrimaryUser) {
                        Log.i(this, "Disabling video calling for secondary user.", new Object[0]);
                        this.mIsVideoCapable = false;
                        zIsVtEnabledByPlatform = false;
                    }
                    if (this.mIsVideoCapable) {
                        i3 |= 8;
                    }
                    if (zIsVtEnabledByPlatform) {
                        i3 |= 1024;
                    }
                    this.mIsVideoPresenceSupported = isCarrierVideoPresenceSupported();
                    if (this.mIsVideoCapable && this.mIsVideoPresenceSupported) {
                        i3 |= 256;
                    }
                    if (this.mIsVideoCapable && isCarrierEmergencyVideoCallsAllowed()) {
                        i3 |= 512;
                    }
                    this.mIsVideoPauseSupported = isCarrierVideoPauseSupported();
                    bundle = new Bundle();
                    if (isCarrierInstantLetteringSupported()) {
                        i3 |= 64;
                        bundle.putAll(getPhoneAccountExtras());
                    }
                    z3 = TelecomAccountRegistry.this.mContext.getResources().getBoolean(R.bool.config_support_handover_from);
                    if (z3 && !z) {
                        bundle.putBoolean("android.telecom.extra.SUPPORTS_HANDOVER_FROM", z3);
                    }
                    if (TelecomAccountRegistry.this.mContext.getResources().getBoolean(R.bool.config_support_telephony_audio_device) && !z && isCarrierUseCallRecordingTone()) {
                        bundle.putBoolean("android.telecom.extra.PLAY_CALL_RECORDING_TONE", true);
                    }
                    if (PhoneGlobals.getInstance().phoneMgr.isRttEnabled()) {
                        i3 |= 4096;
                    }
                    bundle.putBoolean("android.telecom.extra.SUPPORTS_VIDEO_CALLING_FALLBACK", TelecomAccountRegistry.this.mContext.getResources().getBoolean(R.bool.config_support_video_calling_fallback));
                    if (i != -1) {
                        bundle.putString("android.telecom.extra.SORT_ORDER", String.valueOf(i));
                    }
                    this.mIsMergeCallSupported = isCarrierMergeCallSupported();
                    this.mIsMergeImsCallSupported = isCarrierMergeImsCallSupported();
                    this.mIsVideoConferencingSupported = isCarrierVideoConferencingSupported();
                    this.mIsMergeOfWifiCallsAllowedWhenVoWifiOff = isCarrierMergeOfWifiCallsAllowedWhenVoWifiOff();
                    this.mIsManageImsConferenceCallSupported = isCarrierManageImsConferenceCallSupported();
                    this.mIsShowPreciseFailedCause = isCarrierShowPreciseFailedCause();
                    if (z && TelecomAccountRegistry.this.mContext.getResources().getBoolean(R.bool.config_emergency_account_emergency_calls_only)) {
                        i3 |= 128;
                    }
                    if (iconCreateWithBitmap != null) {
                        Resources resources = TelecomAccountRegistry.this.mContext.getResources();
                        Drawable drawable = resources.getDrawable(R.drawable.ic_multi_sim, null);
                        drawable.setTint(resources.getColor(R.color.default_sim_icon_tint_color, null));
                        drawable.setTintMode(PorterDuff.Mode.SRC_ATOP);
                        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmapCreateBitmap);
                        bundle2 = bundle;
                        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        drawable.draw(canvas);
                        iconCreateWithBitmap = Icon.createWithBitmap(bitmapCreateBitmap);
                    } else {
                        bundle2 = bundle;
                    }
                    String str2 = "";
                    mergedSubscriberIds = TelecomAccountRegistry.this.mTelephonyManager.getMergedSubscriberIds();
                    if (mergedSubscriberIds == null && subscriberId != null && !z) {
                        for (String str3 : mergedSubscriberIds) {
                            if (str3.equals(subscriberId)) {
                                z4 = true;
                                break;
                            }
                        }
                        z4 = false;
                    } else {
                        z4 = false;
                    }
                    if (z4) {
                        str2 = TelecomAccountRegistry.GROUP_PREFIX + line1Number;
                        Log.i(this, "Adding Merged Account with group: " + Log.pii(str2), new Object[0]);
                    }
                    PhoneAccount phoneAccountBuild = PhoneAccount.builder(phoneAccountHandleMakePstnPhoneAccountHandleWithPrefix, networkOperatorName).setAddress(Uri.fromParts("tel", line1Number, null)).setSubscriptionAddress(Uri.fromParts("tel", line1Number2, null)).setCapabilities(i3).setIcon(iconCreateWithBitmap).setHighlightColor(i2).setShortDescription(string3).setSupportedUriSchemes(Arrays.asList("tel", "voicemail")).setExtras(bundle2).setGroupId(str2).build();
                    TelecomAccountRegistry.this.registerIfChanged(phoneAccountBuild);
                    return phoneAccountBuild;
                }
            } else {
                networkOperatorName = TelecomAccountRegistry.this.mContext.getResources().getString(R.string.sim_label_emergency_calls);
                string3 = TelecomAccountRegistry.this.mContext.getResources().getString(R.string.sim_description_emergency_calls);
            }
            i = -1;
            iconCreateWithBitmap = null;
            i2 = 0;
            if (TelecomAccountRegistry.this.mContext.getResources().getBoolean(R.bool.config_pstnCanPlaceEmergencyCalls)) {
            }
            this.mIsVideoCapable = this.mPhone.isVideoEnabled();
            zIsVtEnabledByPlatform = ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId()).isVtEnabledByPlatform();
            if (!TelecomAccountRegistry.this.mIsPrimaryUser) {
            }
            if (this.mIsVideoCapable) {
            }
            if (zIsVtEnabledByPlatform) {
            }
            this.mIsVideoPresenceSupported = isCarrierVideoPresenceSupported();
            if (this.mIsVideoCapable) {
                i3 |= 256;
            }
            if (this.mIsVideoCapable) {
                i3 |= 512;
            }
            this.mIsVideoPauseSupported = isCarrierVideoPauseSupported();
            bundle = new Bundle();
            if (isCarrierInstantLetteringSupported()) {
            }
            z3 = TelecomAccountRegistry.this.mContext.getResources().getBoolean(R.bool.config_support_handover_from);
            if (z3) {
                bundle.putBoolean("android.telecom.extra.SUPPORTS_HANDOVER_FROM", z3);
            }
            if (TelecomAccountRegistry.this.mContext.getResources().getBoolean(R.bool.config_support_telephony_audio_device)) {
                bundle.putBoolean("android.telecom.extra.PLAY_CALL_RECORDING_TONE", true);
            }
            if (PhoneGlobals.getInstance().phoneMgr.isRttEnabled()) {
            }
            bundle.putBoolean("android.telecom.extra.SUPPORTS_VIDEO_CALLING_FALLBACK", TelecomAccountRegistry.this.mContext.getResources().getBoolean(R.bool.config_support_video_calling_fallback));
            if (i != -1) {
            }
            this.mIsMergeCallSupported = isCarrierMergeCallSupported();
            this.mIsMergeImsCallSupported = isCarrierMergeImsCallSupported();
            this.mIsVideoConferencingSupported = isCarrierVideoConferencingSupported();
            this.mIsMergeOfWifiCallsAllowedWhenVoWifiOff = isCarrierMergeOfWifiCallsAllowedWhenVoWifiOff();
            this.mIsManageImsConferenceCallSupported = isCarrierManageImsConferenceCallSupported();
            this.mIsShowPreciseFailedCause = isCarrierShowPreciseFailedCause();
            if (z) {
                i3 |= 128;
            }
            if (iconCreateWithBitmap != null) {
            }
            String str22 = "";
            mergedSubscriberIds = TelecomAccountRegistry.this.mTelephonyManager.getMergedSubscriberIds();
            if (mergedSubscriberIds == null) {
                z4 = false;
            }
            if (z4) {
            }
            PhoneAccount phoneAccountBuild2 = PhoneAccount.builder(phoneAccountHandleMakePstnPhoneAccountHandleWithPrefix, networkOperatorName).setAddress(Uri.fromParts("tel", line1Number, null)).setSubscriptionAddress(Uri.fromParts("tel", line1Number2, null)).setCapabilities(i3).setIcon(iconCreateWithBitmap).setHighlightColor(i2).setShortDescription(string3).setSupportedUriSchemes(Arrays.asList("tel", "voicemail")).setExtras(bundle2).setGroupId(str22).build();
            TelecomAccountRegistry.this.registerIfChanged(phoneAccountBuild2);
            return phoneAccountBuild2;
        }

        public PhoneAccountHandle getPhoneAccountHandle() {
            if (this.mAccount != null) {
                return this.mAccount.getAccountHandle();
            }
            return null;
        }

        protected boolean isCarrierVideoPauseSupported() {
            return this.mCarrierConfigBundle != null && this.mCarrierConfigBundle.getBoolean("support_pause_ims_video_calls_bool");
        }

        protected boolean isCarrierVideoPresenceSupported() {
            return this.mCarrierConfigBundle != null && this.mCarrierConfigBundle.getBoolean("use_rcs_presence_bool");
        }

        private boolean isCarrierInstantLetteringSupported() {
            return this.mCarrierConfigBundle != null && this.mCarrierConfigBundle.getBoolean("carrier_instant_lettering_available_bool");
        }

        protected boolean isCarrierMergeCallSupported() {
            return this.mCarrierConfigBundle != null && this.mCarrierConfigBundle.getBoolean("support_conference_call_bool");
        }

        protected boolean isCarrierMergeImsCallSupported() {
            return this.mCarrierConfigBundle != null && this.mCarrierConfigBundle.getBoolean("support_ims_conference_call_bool");
        }

        private boolean isCarrierEmergencyVideoCallsAllowed() {
            return this.mCarrierConfigBundle != null && this.mCarrierConfigBundle.getBoolean("allow_emergency_video_calls_bool");
        }

        protected boolean isCarrierVideoConferencingSupported() {
            return this.mCarrierConfigBundle != null && this.mCarrierConfigBundle.getBoolean("support_video_conference_call_bool");
        }

        protected boolean isCarrierMergeOfWifiCallsAllowedWhenVoWifiOff() {
            return this.mCarrierConfigBundle != null && this.mCarrierConfigBundle.getBoolean("allow_merge_wifi_calls_when_vowifi_off_bool");
        }

        protected boolean isCarrierManageImsConferenceCallSupported() {
            return this.mCarrierConfigBundle != null && this.mCarrierConfigBundle.getBoolean("support_manage_ims_conference_call_bool");
        }

        protected boolean isCarrierShowPreciseFailedCause() {
            return this.mCarrierConfigBundle != null && this.mCarrierConfigBundle.getBoolean("show_precise_failed_cause_bool");
        }

        private boolean isCarrierUseCallRecordingTone() {
            return this.mCarrierConfigBundle != null && this.mCarrierConfigBundle.getBoolean("play_call_recording_tone_bool");
        }

        private Bundle getPhoneAccountExtras() {
            int i = this.mCarrierConfigBundle.getInt("carrier_instant_lettering_length_limit_int");
            String string = this.mCarrierConfigBundle.getString("carrier_instant_lettering_encoding_string");
            Bundle bundle = new Bundle();
            bundle.putInt("android.telecom.extra.CALL_SUBJECT_MAX_LENGTH", i);
            bundle.putString("android.telecom.extra.CALL_SUBJECT_CHARACTER_ENCODING", string);
            return bundle;
        }

        @Override
        public void onVideoCapabilitiesChanged(boolean z) {
            this.mIsVideoCapable = z;
            synchronized (TelecomAccountRegistry.this.mAccountsLock) {
                if (TelecomAccountRegistry.this.mAccounts.contains(this)) {
                    this.mAccount = registerPstnPhoneAccount(this.mIsEmergency, this.mIsDummy);
                }
            }
        }

        public void updateRttCapability() {
            if (PhoneGlobals.getInstance().phoneMgr.isRttEnabled() != this.mAccount.hasCapabilities(4096)) {
                this.mAccount = registerPstnPhoneAccount(this.mIsEmergency, this.mIsDummy);
            }
        }

        public boolean isVideoPauseSupported() {
            return this.mIsVideoCapable && this.mIsVideoPauseSupported;
        }

        public boolean isMergeCallSupported() {
            return this.mIsMergeCallSupported;
        }

        public boolean isMergeImsCallSupported() {
            return this.mIsMergeImsCallSupported;
        }

        public boolean isVideoConferencingSupported() {
            return this.mIsVideoConferencingSupported;
        }

        public boolean isMergeOfWifiCallsAllowedWhenVoWifiOff() {
            return this.mIsMergeOfWifiCallsAllowedWhenVoWifiOff;
        }

        public boolean isManageImsConferenceCallSupported() {
            return this.mIsManageImsConferenceCallSupported;
        }

        public boolean isShowPreciseFailedCause() {
            return this.mIsShowPreciseFailedCause;
        }
    }

    protected TelecomAccountRegistry(Context context) {
        this.mContext = context;
        this.mTelecomManager = TelecomManager.from(context);
        this.mTelephonyManager = TelephonyManager.from(context);
        this.mSubscriptionManager = SubscriptionManager.from(context);
    }

    public static final synchronized TelecomAccountRegistry getInstance(Context context) {
        if (sInstance == null && context != null) {
            sInstance = new TelecomAccountRegistry(context);
            sInstance = (TelecomAccountRegistry) ExtensionManager.getDigitsUtilExt().replaceTelecomAccountRegistry(sInstance, context);
        }
        return sInstance;
    }

    void setTelephonyConnectionService(TelephonyConnectionService telephonyConnectionService) {
        this.mTelephonyConnectionService = telephonyConnectionService;
    }

    TelephonyConnectionService getTelephonyConnectionService() {
        return this.mTelephonyConnectionService;
    }

    boolean isVideoPauseSupported(PhoneAccountHandle phoneAccountHandle) {
        synchronized (this.mAccountsLock) {
            for (AccountEntry accountEntry : this.mAccounts) {
                if (accountEntry.getPhoneAccountHandle().equals(phoneAccountHandle)) {
                    return accountEntry.isVideoPauseSupported();
                }
            }
            return false;
        }
    }

    boolean isMergeCallSupported(PhoneAccountHandle phoneAccountHandle) {
        synchronized (this.mAccountsLock) {
            for (AccountEntry accountEntry : this.mAccounts) {
                if (accountEntry.getPhoneAccountHandle().equals(phoneAccountHandle)) {
                    return accountEntry.isMergeCallSupported();
                }
            }
            return false;
        }
    }

    boolean isVideoConferencingSupported(PhoneAccountHandle phoneAccountHandle) {
        synchronized (this.mAccountsLock) {
            for (AccountEntry accountEntry : this.mAccounts) {
                if (accountEntry.getPhoneAccountHandle().equals(phoneAccountHandle)) {
                    return accountEntry.isVideoConferencingSupported();
                }
            }
            return false;
        }
    }

    boolean isMergeOfWifiCallsAllowedWhenVoWifiOff(final PhoneAccountHandle phoneAccountHandle) {
        synchronized (this.mAccountsLock) {
            Optional<AccountEntry> optionalFindFirst = this.mAccounts.stream().filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ((TelecomAccountRegistry.AccountEntry) obj).getPhoneAccountHandle().equals(phoneAccountHandle);
                }
            }).findFirst();
            if (optionalFindFirst.isPresent()) {
                return optionalFindFirst.get().isMergeOfWifiCallsAllowedWhenVoWifiOff();
            }
            return false;
        }
    }

    boolean isMergeImsCallSupported(PhoneAccountHandle phoneAccountHandle) {
        synchronized (this.mAccountsLock) {
            for (AccountEntry accountEntry : this.mAccounts) {
                if (accountEntry.getPhoneAccountHandle().equals(phoneAccountHandle)) {
                    return accountEntry.isMergeImsCallSupported();
                }
            }
            return false;
        }
    }

    public boolean isManageImsConferenceCallSupported(PhoneAccountHandle phoneAccountHandle) {
        synchronized (this.mAccountsLock) {
            for (AccountEntry accountEntry : this.mAccounts) {
                if (accountEntry.getPhoneAccountHandle().equals(phoneAccountHandle)) {
                    return accountEntry.isManageImsConferenceCallSupported();
                }
            }
            return false;
        }
    }

    boolean isShowPreciseFailedCause(PhoneAccountHandle phoneAccountHandle) {
        synchronized (this.mAccountsLock) {
            for (AccountEntry accountEntry : this.mAccounts) {
                if (accountEntry.getPhoneAccountHandle().equals(phoneAccountHandle)) {
                    return accountEntry.isShowPreciseFailedCause();
                }
            }
            return false;
        }
    }

    SubscriptionManager getSubscriptionManager() {
        return this.mSubscriptionManager;
    }

    Uri getAddress(PhoneAccountHandle phoneAccountHandle) {
        synchronized (this.mAccountsLock) {
            for (AccountEntry accountEntry : this.mAccounts) {
                if (accountEntry.getPhoneAccountHandle().equals(phoneAccountHandle)) {
                    return accountEntry.mAccount.getAddress();
                }
            }
            return null;
        }
    }

    void setupOnBoot() {
        SubscriptionManager.from(this.mContext).addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mTelephonyManager.listen(this.mPhoneStateListener, 1);
        this.mContext.registerReceiver(this.mUserSwitchedReceiver, new IntentFilter("android.intent.action.USER_SWITCHED"));
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("rtt_calling_mode"), false, new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean z) {
                synchronized (TelecomAccountRegistry.this.mAccountsLock) {
                    Iterator<AccountEntry> it = TelecomAccountRegistry.this.mAccounts.iterator();
                    while (it.hasNext()) {
                        it.next().updateRttCapability();
                    }
                }
            }
        });
        this.mIncomingCallNotifiers.clear();
        for (Phone phone : PhoneFactory.getPhones()) {
            Log.i(this, "new PstnIncomingCallNotifier for %s", phone);
            this.mIncomingCallNotifiers.add(new PstnIncomingCallNotifier(phone));
        }
    }

    boolean hasAccountEntryForPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        synchronized (this.mAccountsLock) {
            Iterator<AccountEntry> it = this.mAccounts.iterator();
            while (it.hasNext()) {
                if (it.next().getPhoneAccountHandle().equals(phoneAccountHandle)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void cleanupPhoneAccounts() {
        List<PhoneAccountHandle> callCapablePhoneAccounts;
        ComponentName componentName = new ComponentName(this.mContext, (Class<?>) TelephonyConnectionService.class);
        if (!this.mContext.getResources().getBoolean(R.bool.config_emergency_account_emergency_calls_only)) {
            callCapablePhoneAccounts = this.mTelecomManager.getCallCapablePhoneAccounts(true);
        } else {
            callCapablePhoneAccounts = this.mTelecomManager.getAllPhoneAccountHandles();
        }
        for (PhoneAccountHandle phoneAccountHandle : callCapablePhoneAccounts) {
            if (componentName.equals(phoneAccountHandle.getComponentName()) && !hasAccountEntryForPhoneAccount(phoneAccountHandle)) {
                Log.i(this, "Unregistering phone account %s.", phoneAccountHandle);
                this.mTelecomManager.unregisterPhoneAccount(phoneAccountHandle);
            }
        }
    }

    protected void setupAccounts() {
        Phone[] phones = PhoneFactory.getPhones();
        Log.d(this, "Found %d phones.  Attempting to register.", Integer.valueOf(phones.length));
        boolean z = this.mContext.getResources().getBoolean(R.bool.config_pstn_phone_accounts_enabled);
        synchronized (this.mAccountsLock) {
            if (z) {
                try {
                    for (Phone phone : phones) {
                        int subId = phone.getSubId();
                        Log.d(this, "Phone with subscription id %d", Integer.valueOf(subId));
                        if (subId >= 0 && phone.getFullIccSerialNumber() != null) {
                            this.mAccounts.add(new AccountEntry(phone, false, false));
                        } else {
                            this.mNoAccountPhones.add(phone);
                        }
                    }
                    addVirtualPhoneAccountEntries(this.mAccounts);
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (this.mAccounts.isEmpty()) {
                this.mAccounts.add(new AccountEntry(PhoneFactory.getDefaultPhone(), true, false));
                this.mNoAccountPhones.remove(PhoneFactory.getDefaultPhone());
            }
            Iterator<Phone> it = this.mNoAccountPhones.iterator();
            while (it.hasNext()) {
                this.mNoAccountUnknownCallNotifiers.add(new PstnNoAccountUnknownCallNotifier(it.next()));
            }
        }
        cleanupPhoneAccounts();
        PhoneAccountHandle userSelectedOutgoingPhoneAccount = this.mTelecomManager.getUserSelectedOutgoingPhoneAccount();
        ComponentName componentName = new ComponentName(this.mContext, (Class<?>) TelephonyConnectionService.class);
        if (userSelectedOutgoingPhoneAccount != null && componentName.equals(userSelectedOutgoingPhoneAccount.getComponentName()) && !hasAccountEntryForPhoneAccount(userSelectedOutgoingPhoneAccount)) {
            String id = userSelectedOutgoingPhoneAccount.getId();
            if (!TextUtils.isEmpty(id) && TextUtils.isDigitsOnly(id)) {
                PhoneAccountHandle phoneAccountHandleMakePstnPhoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(PhoneGlobals.getPhone(Integer.parseInt(id)));
                if (hasAccountEntryForPhoneAccount(phoneAccountHandleMakePstnPhoneAccountHandle)) {
                    this.mTelecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccountHandleMakePstnPhoneAccountHandle);
                }
            }
        }
    }

    protected void tearDownAccounts() {
        synchronized (this.mAccountsLock) {
            Iterator<AccountEntry> it = this.mAccounts.iterator();
            while (it.hasNext()) {
                it.next().teardown();
            }
            this.mAccounts.clear();
            this.mNoAccountPhones.clear();
            Iterator<PstnNoAccountUnknownCallNotifier> it2 = this.mNoAccountUnknownCallNotifiers.iterator();
            while (it2.hasNext()) {
                it2.next().teardown();
            }
            this.mNoAccountUnknownCallNotifiers.clear();
        }
    }

    protected void addVirtualPhoneAccountEntries(List<AccountEntry> list) {
    }

    protected PstnIncomingCallNotifier makePstnIncomingCallNotifier(AccountEntry accountEntry, Phone phone, PhoneAccountHandle phoneAccountHandle) {
        return new PstnIncomingCallNotifier(phone);
    }

    private void registerIfChanged(PhoneAccount phoneAccount) {
        if (phoneAccount == null || !isValidPhoneAccount(phoneAccount)) {
            Log.i(this, "[registerIfChanged], account is null or invalid account", new Object[0]);
        } else if (!compareValidAccount(this.mTelecomManager.getPhoneAccount(phoneAccount.getAccountHandle()), phoneAccount)) {
            Log.i(this, "[registerIfChanged]Account changed.", new Object[0]);
            this.mTelecomManager.registerPhoneAccount(phoneAccount);
        }
    }

    private boolean isValidPhoneAccount(PhoneAccount phoneAccount) {
        if (phoneAccount == null || phoneAccount.getAccountHandle() == null) {
            return false;
        }
        if (TextUtils.equals(phoneAccount.getAccountHandle().getId(), "null")) {
            Log.w(this, "[isValidPhoneAccount]Invalid id null", new Object[0]);
            return false;
        }
        return true;
    }

    private boolean compareValidAccount(PhoneAccount phoneAccount, PhoneAccount phoneAccount2) {
        return phoneAccount != null && phoneAccount2 != null && Objects.equals(Integer.valueOf(phoneAccount.getCapabilities()), Integer.valueOf(phoneAccount2.getCapabilities())) && Objects.equals(Integer.valueOf(phoneAccount.getHighlightColor()), Integer.valueOf(phoneAccount2.getHighlightColor())) && Objects.equals(phoneAccount.getLabel(), phoneAccount2.getLabel()) && Objects.equals(phoneAccount.getShortDescription(), phoneAccount2.getShortDescription()) && Objects.equals(phoneAccount.getAddress(), phoneAccount2.getAddress()) && Objects.equals(phoneAccount.getSubscriptionAddress(), phoneAccount2.getSubscriptionAddress()) && Objects.equals(phoneAccount.getSupportedUriSchemes(), phoneAccount2.getSupportedUriSchemes()) && Objects.equals(phoneAccount.getAccountHandle(), phoneAccount2.getAccountHandle()) && Objects.equals(phoneAccount.getGroupId(), phoneAccount2.getGroupId()) && compareValidAccountExtras(phoneAccount.getExtras(), phoneAccount2.getExtras());
    }

    private boolean compareValidAccountExtras(Bundle bundle, Bundle bundle2) {
        return bundle != null && bundle2 != null && Objects.equals(Integer.valueOf(bundle.getInt("android.telecom.extra.CALL_SUBJECT_MAX_LENGTH")), Integer.valueOf(bundle2.getInt("android.telecom.extra.CALL_SUBJECT_MAX_LENGTH"))) && Objects.equals(bundle.getString("android.telecom.extra.CALL_SUBJECT_CHARACTER_ENCODING"), bundle2.getString("android.telecom.extra.CALL_SUBJECT_CHARACTER_ENCODING")) && Objects.equals(Boolean.valueOf(bundle.getBoolean("android.telecom.extra.SUPPORTS_HANDOVER_FROM")), Boolean.valueOf(bundle2.getBoolean("android.telecom.extra.SUPPORTS_HANDOVER_FROM"))) && Objects.equals(Boolean.valueOf(bundle.getBoolean("android.telecom.extra.SUPPORTS_VIDEO_CALLING_FALLBACK")), Boolean.valueOf(bundle2.getBoolean("android.telecom.extra.SUPPORTS_VIDEO_CALLING_FALLBACK"))) && Objects.equals(bundle.getString("android.telecom.extra.SORT_ORDER"), bundle2.getString("android.telecom.extra.SORT_ORDER")) && Objects.equals(Boolean.valueOf(bundle.getBoolean("android.telecom.extra.PLAY_CALL_RECORDING_TONE")), Boolean.valueOf(bundle2.getBoolean("android.telecom.extra.PLAY_CALL_RECORDING_TONE")));
    }
}
