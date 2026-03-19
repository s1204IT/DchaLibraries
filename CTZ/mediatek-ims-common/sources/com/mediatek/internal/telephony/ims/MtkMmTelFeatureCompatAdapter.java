package com.mediatek.internal.telephony.ims;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.RemoteException;
import android.telephony.ims.ImsReasonInfo;
import android.util.Log;
import com.android.ims.ImsManager;
import com.android.ims.internal.IImsRegistrationListener;
import com.android.internal.telephony.ims.MmTelFeatureCompatAdapter;
import com.android.internal.telephony.ims.MmTelInterfaceAdapter;
import com.mediatek.ims.internal.MtkImsManager;

public class MtkMmTelFeatureCompatAdapter extends MmTelFeatureCompatAdapter {
    private static final String TAG = "MtkMmTelFeatureCompat";
    private MtkImsManager mImsManager;
    private int mSlotId;

    public MtkMmTelFeatureCompatAdapter(Context context, int i, MmTelInterfaceAdapter mmTelInterfaceAdapter) {
        super(context, i, mmTelInterfaceAdapter);
        this.mSlotId = -1;
        this.mImsManager = null;
        this.mSlotId = i;
        this.mImsManager = (MtkImsManager) ImsManager.getInstance(context, i);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Log.i(MtkMmTelFeatureCompatAdapter.TAG, "onReceive");
                if (intent.getAction().equals("com.android.ims.IMS_INCOMING_CALL")) {
                    Log.i(MtkMmTelFeatureCompatAdapter.TAG, "onReceive : incoming call intent. mSessionId:" + MtkMmTelFeatureCompatAdapter.this.mSessionId);
                    String stringExtra = intent.getStringExtra("android:imsCallID");
                    if (MtkMmTelFeatureCompatAdapter.this.mSessionId == MtkMmTelFeatureCompatAdapter.getImsSessionId(intent)) {
                        try {
                            MtkMmTelFeatureCompatAdapter.this.notifyIncomingCallSession(MtkMmTelFeatureCompatAdapter.this.mCompatFeature.getPendingCallSession(MtkMmTelFeatureCompatAdapter.this.mSessionId, stringExtra), intent.getExtras());
                            return;
                        } catch (RemoteException e) {
                            Log.w(MtkMmTelFeatureCompatAdapter.TAG, "onReceive: Couldn't get Incoming call session.");
                            return;
                        }
                    }
                    Log.w(MtkMmTelFeatureCompatAdapter.TAG, "onReceive : Service id is mismatched the incoming call intent");
                }
            }
        };
        this.mListener = new IImsRegistrationListener.Stub() {
            public void registrationConnected() throws RemoteException {
            }

            public void registrationProgressing() throws RemoteException {
            }

            public void registrationConnectedWithRadioTech(int i2) throws RemoteException {
            }

            public void registrationProgressingWithRadioTech(int i2) throws RemoteException {
            }

            public void registrationDisconnected(ImsReasonInfo imsReasonInfo) throws RemoteException {
            }

            public void registrationResumed() throws RemoteException {
            }

            public void registrationSuspended() throws RemoteException {
            }

            public void registrationServiceCapabilityChanged(int i2, int i3) throws RemoteException {
                MtkMmTelFeatureCompatAdapter.this.mImsManager.notifyRegServiceCapabilityChangedEvent(i3);
            }

            public void registrationFeatureCapabilityChanged(int i2, int[] iArr, int[] iArr2) throws RemoteException {
                MtkMmTelFeatureCompatAdapter.this.notifyCapabilitiesStatusChanged(MtkMmTelFeatureCompatAdapter.this.convertCapabilities(iArr));
            }

            public void voiceMessageCountUpdate(int i2) throws RemoteException {
                MtkMmTelFeatureCompatAdapter.this.notifyVoiceMessageCountUpdate(i2);
            }

            public void registrationAssociatedUriChanged(Uri[] uriArr) throws RemoteException {
            }

            public void registrationChangeFailed(int i2, ImsReasonInfo imsReasonInfo) throws RemoteException {
            }
        };
    }

    private static int getImsSessionId(Intent intent) {
        if (intent == null) {
            return -1;
        }
        return intent.getIntExtra("android:imsServiceId", -1);
    }
}
