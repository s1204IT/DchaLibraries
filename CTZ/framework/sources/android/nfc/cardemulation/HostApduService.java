package android.nfc.cardemulation;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public abstract class HostApduService extends Service {
    public static final int DEACTIVATION_DESELECTED = 1;
    public static final int DEACTIVATION_LINK_LOSS = 0;
    public static final String KEY_DATA = "data";
    public static final int MSG_COMMAND_APDU = 0;
    public static final int MSG_DEACTIVATED = 2;
    public static final int MSG_RESPONSE_APDU = 1;
    public static final int MSG_UNHANDLED = 3;
    public static final String SERVICE_INTERFACE = "android.nfc.cardemulation.action.HOST_APDU_SERVICE";
    public static final String SERVICE_META_DATA = "android.nfc.cardemulation.host_apdu_service";
    static final String TAG = "ApduService";
    Messenger mNfcService = null;
    final Messenger mMessenger = new Messenger(new MsgHandler());

    public abstract void onDeactivated(int i);

    public abstract byte[] processCommandApdu(byte[] bArr, Bundle bundle);

    final class MsgHandler extends Handler {
        MsgHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    Bundle data = message.getData();
                    if (data != null) {
                        if (HostApduService.this.mNfcService == null) {
                            HostApduService.this.mNfcService = message.replyTo;
                        }
                        byte[] byteArray = data.getByteArray("data");
                        if (byteArray != null) {
                            byte[] bArrProcessCommandApdu = HostApduService.this.processCommandApdu(byteArray, null);
                            if (bArrProcessCommandApdu != null) {
                                if (HostApduService.this.mNfcService == null) {
                                    Log.e(HostApduService.TAG, "Response not sent; service was deactivated.");
                                } else {
                                    Message messageObtain = Message.obtain((Handler) null, 1);
                                    Bundle bundle = new Bundle();
                                    bundle.putByteArray("data", bArrProcessCommandApdu);
                                    messageObtain.setData(bundle);
                                    messageObtain.replyTo = HostApduService.this.mMessenger;
                                    try {
                                        HostApduService.this.mNfcService.send(messageObtain);
                                    } catch (RemoteException e) {
                                        Log.e("TAG", "Response not sent; RemoteException calling into NfcService.");
                                        return;
                                    }
                                }
                            }
                        } else {
                            Log.e(HostApduService.TAG, "Received MSG_COMMAND_APDU without data.");
                        }
                        break;
                    }
                    break;
                case 1:
                    if (HostApduService.this.mNfcService == null) {
                        Log.e(HostApduService.TAG, "Response not sent; service was deactivated.");
                    } else {
                        try {
                            message.replyTo = HostApduService.this.mMessenger;
                            HostApduService.this.mNfcService.send(message);
                        } catch (RemoteException e2) {
                            Log.e(HostApduService.TAG, "RemoteException calling into NfcService.");
                            return;
                        }
                    }
                    break;
                case 2:
                    HostApduService.this.mNfcService = null;
                    HostApduService.this.onDeactivated(message.arg1);
                    break;
                case 3:
                    if (HostApduService.this.mNfcService == null) {
                        Log.e(HostApduService.TAG, "notifyUnhandled not sent; service was deactivated.");
                    } else {
                        try {
                            message.replyTo = HostApduService.this.mMessenger;
                            HostApduService.this.mNfcService.send(message);
                        } catch (RemoteException e3) {
                            Log.e(HostApduService.TAG, "RemoteException calling into NfcService.");
                            return;
                        }
                    }
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
        }
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return this.mMessenger.getBinder();
    }

    public final void sendResponseApdu(byte[] bArr) {
        Message messageObtain = Message.obtain((Handler) null, 1);
        Bundle bundle = new Bundle();
        bundle.putByteArray("data", bArr);
        messageObtain.setData(bundle);
        try {
            this.mMessenger.send(messageObtain);
        } catch (RemoteException e) {
            Log.e("TAG", "Local messenger has died.");
        }
    }

    public final void notifyUnhandled() {
        try {
            this.mMessenger.send(Message.obtain((Handler) null, 3));
        } catch (RemoteException e) {
            Log.e("TAG", "Local messenger has died.");
        }
    }
}
