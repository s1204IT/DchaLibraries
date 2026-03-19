package com.android.internal.telephony;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.util.NotificationChannelController;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CarrierServiceStateTracker extends Handler {
    protected static final int CARRIER_EVENT_BASE = 100;
    protected static final int CARRIER_EVENT_DATA_DEREGISTRATION = 104;
    protected static final int CARRIER_EVENT_DATA_REGISTRATION = 103;
    protected static final int CARRIER_EVENT_VOICE_DEREGISTRATION = 102;
    protected static final int CARRIER_EVENT_VOICE_REGISTRATION = 101;
    private static final String LOG_TAG = "CSST";
    public static final int NOTIFICATION_EMERGENCY_NETWORK = 1001;
    public static final int NOTIFICATION_PREF_NETWORK = 1000;
    private static final int UNINITIALIZED_DELAY_VALUE = -1;
    private Phone mPhone;
    private ServiceStateTracker mSST;
    private final Map<Integer, NotificationType> mNotificationTypeMap = new HashMap();
    private int mPreviousSubId = -1;
    private ContentObserver mPrefNetworkModeObserver = new ContentObserver(this) {
        @Override
        public void onChange(boolean z) {
            CarrierServiceStateTracker.this.handlePrefNetworkModeChanged();
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            PersistableBundle configForSubId = ((CarrierConfigManager) context.getSystemService("carrier_config")).getConfigForSubId(CarrierServiceStateTracker.this.mPhone.getSubId());
            Iterator it = CarrierServiceStateTracker.this.mNotificationTypeMap.entrySet().iterator();
            while (it.hasNext()) {
                ((NotificationType) ((Map.Entry) it.next()).getValue()).setDelay(configForSubId);
            }
            CarrierServiceStateTracker.this.handleConfigChanges();
        }
    };

    public interface NotificationType {
        int getDelay();

        Notification.Builder getNotificationBuilder();

        int getTypeId();

        boolean sendMessage();

        void setDelay(PersistableBundle persistableBundle);
    }

    public CarrierServiceStateTracker(Phone phone, ServiceStateTracker serviceStateTracker) {
        this.mPhone = phone;
        this.mSST = serviceStateTracker;
        phone.getContext().registerReceiver(this.mBroadcastReceiver, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"));
        SubscriptionManager.from(this.mPhone.getContext()).addOnSubscriptionsChangedListener(new SubscriptionManager.OnSubscriptionsChangedListener(getLooper()) {
            @Override
            public void onSubscriptionsChanged() {
                int subId = CarrierServiceStateTracker.this.mPhone.getSubId();
                if (CarrierServiceStateTracker.this.mPreviousSubId != subId) {
                    CarrierServiceStateTracker.this.mPreviousSubId = subId;
                    CarrierServiceStateTracker.this.registerPrefNetworkModeObserver();
                }
            }
        });
        registerNotificationTypes();
        registerPrefNetworkModeObserver();
    }

    @VisibleForTesting
    public ContentObserver getContentObserver() {
        return this.mPrefNetworkModeObserver;
    }

    private void registerPrefNetworkModeObserver() {
        int subId = this.mPhone.getSubId();
        unregisterPrefNetworkModeObserver();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            this.mPhone.getContext().getContentResolver().registerContentObserver(Settings.Global.getUriFor("preferred_network_mode" + subId), true, this.mPrefNetworkModeObserver);
        }
    }

    private void unregisterPrefNetworkModeObserver() {
        this.mPhone.getContext().getContentResolver().unregisterContentObserver(this.mPrefNetworkModeObserver);
    }

    @VisibleForTesting
    public Map<Integer, NotificationType> getNotificationTypeMap() {
        return this.mNotificationTypeMap;
    }

    private void registerNotificationTypes() {
        this.mNotificationTypeMap.put(1000, new PrefNetworkNotification(1000));
        this.mNotificationTypeMap.put(1001, new EmergencyNetworkNotification(1001));
    }

    @Override
    public void handleMessage(Message message) {
        int i = message.what;
        switch (i) {
            case 101:
            case 102:
            case CARRIER_EVENT_DATA_REGISTRATION:
            case CARRIER_EVENT_DATA_DEREGISTRATION:
                handleConfigChanges();
                break;
            default:
                switch (i) {
                    case 1000:
                    case 1001:
                        Rlog.d(LOG_TAG, "sending notification after delay: " + message.what);
                        NotificationType notificationType = this.mNotificationTypeMap.get(Integer.valueOf(message.what));
                        if (notificationType != null) {
                            sendNotification(notificationType);
                        }
                        break;
                }
                break;
        }
    }

    private boolean isPhoneStillRegistered() {
        return this.mSST.mSS == null || this.mSST.mSS.getVoiceRegState() == 0 || this.mSST.mSS.getDataRegState() == 0;
    }

    private boolean isPhoneVoiceRegistered() {
        return this.mSST.mSS == null || this.mSST.mSS.getVoiceRegState() == 0;
    }

    private boolean isPhoneRegisteredForWifiCalling() {
        Rlog.d(LOG_TAG, "isPhoneRegisteredForWifiCalling: " + this.mPhone.isWifiCallingEnabled());
        return this.mPhone.isWifiCallingEnabled();
    }

    @VisibleForTesting
    public boolean isRadioOffOrAirplaneMode() {
        try {
            return (this.mSST.isRadioOn() && Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "airplane_mode_on", 0) == 0) ? false : true;
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "Unable to get AIRPLACE_MODE_ON.");
            return true;
        }
    }

    private boolean isGlobalMode() {
        try {
            ContentResolver contentResolver = this.mPhone.getContext().getContentResolver();
            StringBuilder sb = new StringBuilder();
            sb.append("preferred_network_mode");
            sb.append(this.mPhone.getSubId());
            return Settings.Global.getInt(contentResolver, sb.toString(), Phone.PREFERRED_NT_MODE) == 10;
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "Unable to get PREFERRED_NETWORK_MODE.");
            return true;
        }
    }

    private void handleConfigChanges() {
        Iterator<Map.Entry<Integer, NotificationType>> it = this.mNotificationTypeMap.entrySet().iterator();
        while (it.hasNext()) {
            evaluateSendingMessageOrCancelNotification(it.next().getValue());
        }
    }

    private void handlePrefNetworkModeChanged() {
        NotificationType notificationType = this.mNotificationTypeMap.get(1000);
        if (notificationType != null) {
            evaluateSendingMessageOrCancelNotification(notificationType);
        }
    }

    private void evaluateSendingMessageOrCancelNotification(NotificationType notificationType) {
        if (evaluateSendingMessage(notificationType)) {
            Message messageObtainMessage = obtainMessage(notificationType.getTypeId(), null);
            Rlog.i(LOG_TAG, "starting timer for notifications." + notificationType.getTypeId());
            sendMessageDelayed(messageObtainMessage, (long) getDelay(notificationType));
            return;
        }
        cancelNotification(notificationType.getTypeId());
        Rlog.i(LOG_TAG, "canceling notifications: " + notificationType.getTypeId());
    }

    @VisibleForTesting
    public boolean evaluateSendingMessage(NotificationType notificationType) {
        return notificationType.sendMessage();
    }

    @VisibleForTesting
    public int getDelay(NotificationType notificationType) {
        return notificationType.getDelay();
    }

    @VisibleForTesting
    public Notification.Builder getNotificationBuilder(NotificationType notificationType) {
        return notificationType.getNotificationBuilder();
    }

    @VisibleForTesting
    public NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService("notification");
    }

    @VisibleForTesting
    public void sendNotification(NotificationType notificationType) {
        if (!evaluateSendingMessage(notificationType)) {
            return;
        }
        Context context = this.mPhone.getContext();
        Notification.Builder notificationBuilder = getNotificationBuilder(notificationType);
        notificationBuilder.setWhen(System.currentTimeMillis()).setAutoCancel(true).setSmallIcon(R.drawable.stat_sys_warning).setColor(context.getResources().getColor(R.color.car_colorPrimary));
        getNotificationManager(context).notify(notificationType.getTypeId(), notificationBuilder.build());
    }

    public void cancelNotification(int i) {
        Context context = this.mPhone.getContext();
        removeMessages(i);
        getNotificationManager(context).cancel(i);
    }

    public void dispose() {
        unregisterPrefNetworkModeObserver();
    }

    public class PrefNetworkNotification implements NotificationType {
        private int mDelay = -1;
        private final int mTypeId;

        PrefNetworkNotification(int i) {
            this.mTypeId = i;
        }

        @Override
        public void setDelay(PersistableBundle persistableBundle) {
            if (persistableBundle == null) {
                Rlog.e(CarrierServiceStateTracker.LOG_TAG, "bundle is null");
                return;
            }
            this.mDelay = persistableBundle.getInt("network_notification_delay_int");
            Rlog.i(CarrierServiceStateTracker.LOG_TAG, "reading time to delay notification pref network: " + this.mDelay);
        }

        @Override
        public int getDelay() {
            return this.mDelay;
        }

        @Override
        public int getTypeId() {
            return this.mTypeId;
        }

        @Override
        public boolean sendMessage() {
            Rlog.i(CarrierServiceStateTracker.LOG_TAG, "PrefNetworkNotification: sendMessage() w/values: ," + CarrierServiceStateTracker.this.isPhoneStillRegistered() + "," + this.mDelay + "," + CarrierServiceStateTracker.this.isGlobalMode() + "," + CarrierServiceStateTracker.this.mSST.isRadioOn());
            if (this.mDelay == -1 || CarrierServiceStateTracker.this.isPhoneStillRegistered() || CarrierServiceStateTracker.this.isGlobalMode() || CarrierServiceStateTracker.this.isRadioOffOrAirplaneMode()) {
                return false;
            }
            return true;
        }

        @Override
        public Notification.Builder getNotificationBuilder() {
            Context context = CarrierServiceStateTracker.this.mPhone.getContext();
            Intent intent = new Intent("android.settings.DATA_ROAMING_SETTINGS");
            intent.putExtra("expandable", true);
            PendingIntent activity = PendingIntent.getActivity(context, 0, intent, 1140850688);
            CharSequence text = context.getText(R.string.config_systemWifiCoexManager);
            CharSequence text2 = context.getText(R.string.config_systemSpeechRecognizer);
            return new Notification.Builder(context).setContentTitle(text).setStyle(new Notification.BigTextStyle().bigText(text2)).setContentText(text2).setChannel(NotificationChannelController.CHANNEL_ID_ALERT).setContentIntent(activity);
        }
    }

    public class EmergencyNetworkNotification implements NotificationType {
        private int mDelay = -1;
        private final int mTypeId;

        EmergencyNetworkNotification(int i) {
            this.mTypeId = i;
        }

        @Override
        public void setDelay(PersistableBundle persistableBundle) {
            if (persistableBundle == null) {
                Rlog.e(CarrierServiceStateTracker.LOG_TAG, "bundle is null");
                return;
            }
            this.mDelay = persistableBundle.getInt("emergency_notification_delay_int");
            Rlog.i(CarrierServiceStateTracker.LOG_TAG, "reading time to delay notification emergency: " + this.mDelay);
        }

        @Override
        public int getDelay() {
            return this.mDelay;
        }

        @Override
        public int getTypeId() {
            return this.mTypeId;
        }

        @Override
        public boolean sendMessage() {
            Rlog.i(CarrierServiceStateTracker.LOG_TAG, "EmergencyNetworkNotification: sendMessage() w/values: ," + CarrierServiceStateTracker.this.isPhoneVoiceRegistered() + "," + this.mDelay + "," + CarrierServiceStateTracker.this.isPhoneRegisteredForWifiCalling() + "," + CarrierServiceStateTracker.this.mSST.isRadioOn());
            if (this.mDelay == -1 || CarrierServiceStateTracker.this.isPhoneVoiceRegistered() || !CarrierServiceStateTracker.this.isPhoneRegisteredForWifiCalling()) {
                return false;
            }
            return true;
        }

        @Override
        public Notification.Builder getNotificationBuilder() {
            Context context = CarrierServiceStateTracker.this.mPhone.getContext();
            CharSequence text = context.getText(R.string.config_customMediaKeyDispatcher);
            CharSequence text2 = context.getText(R.string.config_systemContacts);
            return new Notification.Builder(context).setContentTitle(text).setStyle(new Notification.BigTextStyle().bigText(text2)).setContentText(text2).setChannel(NotificationChannelController.CHANNEL_ID_WFC);
        }
    }
}
