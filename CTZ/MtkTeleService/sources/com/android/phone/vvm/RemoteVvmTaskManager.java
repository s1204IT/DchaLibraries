package com.android.phone.vvm;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.VisualVoicemailSms;
import android.text.TextUtils;
import com.android.phone.Assert;
import com.android.phone.R;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class RemoteVvmTaskManager extends Service {
    private RemoteServiceConnection mConnection;
    private Messenger mMessenger;
    private int mTaskReferenceCount;

    static int access$010(RemoteVvmTaskManager remoteVvmTaskManager) {
        int i = remoteVvmTaskManager.mTaskReferenceCount;
        remoteVvmTaskManager.mTaskReferenceCount = i - 1;
        return i;
    }

    static void startCellServiceConnected(Context context, PhoneAccountHandle phoneAccountHandle) {
        Intent intent = new Intent("ACTION_START_CELL_SERVICE_CONNECTED", null, context, RemoteVvmTaskManager.class);
        intent.putExtra("data_phone_account_handle", phoneAccountHandle);
        context.startService(intent);
    }

    static void startSmsReceived(Context context, VisualVoicemailSms visualVoicemailSms, String str) {
        Intent intent = new Intent("ACTION_START_SMS_RECEIVED", null, context, RemoteVvmTaskManager.class);
        intent.putExtra("data_phone_account_handle", visualVoicemailSms.getPhoneAccountHandle());
        intent.putExtra("data_sms", visualVoicemailSms);
        intent.putExtra("target_package", str);
        context.startService(intent);
    }

    static void startSimRemoved(Context context, PhoneAccountHandle phoneAccountHandle) {
        Intent intent = new Intent("ACTION_START_SIM_REMOVED", null, context, RemoteVvmTaskManager.class);
        intent.putExtra("data_phone_account_handle", phoneAccountHandle);
        context.startService(intent);
    }

    static boolean hasRemoteService(Context context, int i, String str) {
        return getRemotePackage(context, i, str) != null;
    }

    public static ComponentName getRemotePackage(Context context, int i) {
        return getRemotePackage(context, i, null);
    }

    public static ComponentName getRemotePackage(Context context, int i, String str) {
        ComponentName broadcastPackage = getBroadcastPackage(context);
        if (broadcastPackage != null) {
            return broadcastPackage;
        }
        Intent intentNewBindIntent = newBindIntent(context);
        TelecomManager telecomManager = (TelecomManager) context.getSystemService(TelecomManager.class);
        ArrayList<String> arrayList = new ArrayList();
        arrayList.add(telecomManager.getDefaultDialerPackage());
        PersistableBundle configForSubId = ((CarrierConfigManager) context.getSystemService(CarrierConfigManager.class)).getConfigForSubId(i);
        arrayList.add(configForSubId.getString("carrier_vvm_package_name_string"));
        String[] stringArray = configForSubId.getStringArray("carrier_vvm_package_name_string_array");
        if (stringArray != null && stringArray.length > 0) {
            for (String str2 : stringArray) {
                arrayList.add(str2);
            }
        }
        arrayList.add(context.getResources().getString(R.string.system_visual_voicemail_client));
        arrayList.add(telecomManager.getSystemDialerPackage());
        for (String str3 : arrayList) {
            if (!TextUtils.isEmpty(str3)) {
                intentNewBindIntent.setPackage(str3);
                ResolveInfo resolveInfoResolveService = context.getPackageManager().resolveService(intentNewBindIntent, 131072);
                if (resolveInfoResolveService == null) {
                    continue;
                } else if (resolveInfoResolveService.serviceInfo == null) {
                    VvmLog.w("RemoteVvmTaskManager", "Component " + resolveInfoResolveService.getComponentInfo() + " is not a service, ignoring");
                } else if (!"android.permission.BIND_VISUAL_VOICEMAIL_SERVICE".equals(resolveInfoResolveService.serviceInfo.permission)) {
                    VvmLog.w("RemoteVvmTaskManager", "package " + resolveInfoResolveService.serviceInfo.packageName + " does not enforce BIND_VISUAL_VOICEMAIL_SERVICE, ignoring");
                } else {
                    if (str != null && !TextUtils.equals(str3, str)) {
                        VvmLog.w("RemoteVvmTaskManager", "target package " + str + " is no longer the active VisualVoicemailService, ignoring");
                    }
                    return resolveInfoResolveService.getComponentInfo().getComponentName();
                }
            }
        }
        return null;
    }

    private static ComponentName getBroadcastPackage(Context context) {
        Intent intent = new Intent("com.android.phone.vvm.ACTION_VISUAL_VOICEMAIL_SERVICE_EVENT");
        intent.setPackage(((TelecomManager) context.getSystemService(TelecomManager.class)).getDefaultDialerPackage());
        List<ResolveInfo> listQueryBroadcastReceivers = context.getPackageManager().queryBroadcastReceivers(intent, 131072);
        if (listQueryBroadcastReceivers == null || listQueryBroadcastReceivers.isEmpty()) {
            return null;
        }
        return listQueryBroadcastReceivers.get(0).getComponentInfo().getComponentName();
    }

    @Override
    public void onCreate() {
        Assert.isMainThread();
        this.mMessenger = new Messenger(new Handler() {
            @Override
            public void handleMessage(Message message) {
                Assert.isMainThread();
                if (message.what == 4) {
                    RemoteVvmTaskManager.access$010(RemoteVvmTaskManager.this);
                    RemoteVvmTaskManager.this.checkReference();
                } else {
                    VvmLog.wtf("RemoteVvmTaskManager", "unexpected message " + message.what);
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        byte b;
        Assert.isMainThread();
        this.mTaskReferenceCount++;
        ComponentName remotePackage = getRemotePackage(this, PhoneAccountHandleConverter.toSubId((PhoneAccountHandle) intent.getExtras().getParcelable("data_phone_account_handle")), intent.getStringExtra("target_package"));
        if (remotePackage == null) {
            VvmLog.i("RemoteVvmTaskManager", "No service to handle " + intent.getAction() + ", ignoring");
            checkReference();
            return 2;
        }
        String action = intent.getAction();
        int iHashCode = action.hashCode();
        if (iHashCode != 747910770) {
            if (iHashCode != 1637547949) {
                b = (iHashCode == 1936369192 && action.equals("ACTION_START_CELL_SERVICE_CONNECTED")) ? (byte) 0 : (byte) -1;
            } else if (action.equals("ACTION_START_SMS_RECEIVED")) {
                b = 1;
            }
        } else if (action.equals("ACTION_START_SIM_REMOVED")) {
            b = 2;
        }
        switch (b) {
            case 0:
                send(remotePackage, 1, intent.getExtras());
                return 2;
            case 1:
                send(remotePackage, 2, intent.getExtras());
                return 2;
            case 2:
                send(remotePackage, 3, intent.getExtras());
                return 2;
            default:
                Assert.fail("Unexpected action +" + intent.getAction());
                return 2;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int getTaskId() {
        return 1;
    }

    private class RemoteServiceConnection implements ServiceConnection {
        private boolean mConnected;
        private Messenger mRemoteMessenger;
        private final Queue<Message> mTaskQueue;

        private RemoteServiceConnection() {
            this.mTaskQueue = new LinkedList();
        }

        public void enqueue(Message message) {
            this.mTaskQueue.add(message);
            if (this.mConnected) {
                runQueue();
            }
        }

        public boolean isConnected() {
            return this.mConnected;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            this.mRemoteMessenger = new Messenger(iBinder);
            this.mConnected = true;
            runQueue();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            RemoteVvmTaskManager.this.mConnection = null;
            this.mConnected = false;
            this.mRemoteMessenger = null;
            VvmLog.e("RemoteVvmTaskManager", "Service disconnected, " + RemoteVvmTaskManager.this.mTaskReferenceCount + " tasks dropped.");
            RemoteVvmTaskManager.this.mTaskReferenceCount = 0;
            RemoteVvmTaskManager.this.checkReference();
        }

        private void runQueue() {
            Assert.isMainThread();
            Message messagePoll = this.mTaskQueue.poll();
            while (messagePoll != null) {
                messagePoll.replyTo = RemoteVvmTaskManager.this.mMessenger;
                messagePoll.arg1 = RemoteVvmTaskManager.this.getTaskId();
                try {
                    this.mRemoteMessenger.send(messagePoll);
                } catch (RemoteException e) {
                    VvmLog.e("RemoteVvmTaskManager", "Error sending message to remote service", e);
                }
                messagePoll = this.mTaskQueue.poll();
            }
        }
    }

    private void send(ComponentName componentName, int i, Bundle bundle) {
        Assert.isMainThread();
        if (getBroadcastPackage(this) != null) {
            VvmLog.i("RemoteVvmTaskManager", "sending broadcast " + i + " to " + componentName);
            Intent intent = new Intent("com.android.phone.vvm.ACTION_VISUAL_VOICEMAIL_SERVICE_EVENT");
            intent.putExtras(bundle);
            intent.putExtra("what", i);
            intent.setComponent(componentName);
            sendBroadcast(intent);
            return;
        }
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.setData(new Bundle(bundle));
        if (this.mConnection == null) {
            this.mConnection = new RemoteServiceConnection();
        }
        this.mConnection.enqueue(messageObtain);
        if (!this.mConnection.isConnected()) {
            Intent intentNewBindIntent = newBindIntent(this);
            intentNewBindIntent.setComponent(componentName);
            VvmLog.i("RemoteVvmTaskManager", "Binding to " + intentNewBindIntent.getComponent());
            bindService(intentNewBindIntent, this.mConnection, 1);
        }
    }

    private void checkReference() {
        if (this.mConnection != null && this.mTaskReferenceCount == 0) {
            unbindService(this.mConnection);
            this.mConnection = null;
        }
    }

    private static Intent newBindIntent(Context context) {
        Intent intent = new Intent();
        intent.setAction("android.telephony.VisualVoicemailService");
        return intent;
    }
}
