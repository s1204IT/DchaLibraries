package com.android.server.telecom;

import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telecom.CallAudioState;
import android.telecom.ConnectionRequest;
import android.telecom.DisconnectCause;
import android.telecom.GatewayInfo;
import android.telecom.Log;
import android.telecom.Logging.Session;
import android.telecom.ParcelableConference;
import android.telecom.ParcelableConnection;
import android.telecom.PhoneAccountHandle;
import android.telecom.StatusHints;
import android.telecom.VideoProfile;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telecom.IConnectionService;
import com.android.internal.telecom.IConnectionServiceAdapter;
import com.android.internal.telecom.IVideoProvider;
import com.android.internal.telecom.RemoteServiceCallback;
import com.android.server.telecom.CallIdMapper;
import com.android.server.telecom.ConnectionServiceFocusManager;
import com.android.server.telecom.PhoneAccountRegistrar;
import com.android.server.telecom.ServiceBinder;
import com.android.server.telecom.TelecomSystem;
import com.mediatek.internal.telecom.IMtkConnectionService;
import com.mediatek.internal.telecom.IMtkConnectionServiceAdapter;
import com.mediatek.server.telecom.MtkTelecomGlobals;
import com.mediatek.server.telecom.ext.ExtensionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@VisibleForTesting
public class ConnectionServiceWrapper extends ServiceBinder implements ConnectionServiceFocusManager.ConnectionServiceFocus {
    private final Adapter mAdapter;
    private final AppOpsManager mAppOpsManager;
    private ServiceBinder.Binder2 mBinder;
    private final CallIdMapper mCallIdMapper;
    private final CallsManager mCallsManager;
    private ConnectionServiceFocusManager.ConnectionServiceFocusListener mConnSvrFocusListener;
    private final ConnectionServiceRepository mConnectionServiceRepository;
    private MtkAdapter mMtkAdapter;
    private IMtkConnectionService mMtkServiceInterface;
    private final Map<String, CreateConnectionResponse> mPendingResponses;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private IConnectionService mServiceInterface;

    @Override
    public boolean isMtkServiceValid(String str) {
        return super.isMtkServiceValid(str);
    }

    @Override
    @VisibleForTesting
    public boolean isServiceValid(String str) {
        return super.isServiceValid(str);
    }

    private final class Adapter extends IConnectionServiceAdapter.Stub {
        private Adapter() {
        }

        public void handleCreateConnectionComplete(String str, ConnectionRequest connectionRequest, ParcelableConnection parcelableConnection, Session.Info info) {
            Log.startSession(info, "CSW.hCCC");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("handleCreateConnectionComplete %s", str);
                        ConnectionServiceWrapper.this.handleCreateConnectionComplete(str, connectionRequest, parcelableConnection);
                        if (ConnectionServiceWrapper.this.mServiceInterface != null) {
                            ConnectionServiceWrapper.this.logOutgoing("createConnectionComplete %s", str);
                            try {
                                ConnectionServiceWrapper.this.mServiceInterface.createConnectionComplete(str, Log.getExternalSession());
                            } catch (RemoteException e) {
                            }
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setActive(String str, Session.Info info) {
            Log.startSession(info, "CSW.sA");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setActive %s", str);
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            ConnectionServiceWrapper.this.mCallsManager.markCallAsActive(call);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setRinging(String str, Session.Info info) {
            Log.startSession(info, "CSW.sR");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setRinging %s", str);
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            ConnectionServiceWrapper.this.mCallsManager.markCallAsRinging(call);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setVideoProvider(String str, IVideoProvider iVideoProvider, Session.Info info) {
            Log.startSession(info, "CSW.sVP");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setVideoProvider %s", str);
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.setVideoProvider(iVideoProvider);
                        }
                    }
                } catch (Throwable th) {
                    Log.e(ConnectionServiceWrapper.this, th, "", new Object[0]);
                    throw th;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setDialing(String str, Session.Info info) {
            Log.startSession(info, "CSW.sD");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setDialing %s", str);
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            ConnectionServiceWrapper.this.mCallsManager.markCallAsDialing(call);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setPulling(String str, Session.Info info) {
            Log.startSession(info, "CSW.sP");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setPulling %s", str);
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            ConnectionServiceWrapper.this.mCallsManager.markCallAsPulling(call);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setDisconnected(String str, DisconnectCause disconnectCause, Session.Info info) {
            Log.startSession(info, "CSW.sDc");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setDisconnected %s %s", str, disconnectCause);
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        Log.d(this, "disconnect call %s %s", new Object[]{disconnectCause, call});
                        if (call != null) {
                            ConnectionServiceWrapper.this.mCallsManager.markCallAsDisconnected(call, disconnectCause);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setOnHold(String str, Session.Info info) {
            Log.startSession(info, "CSW.sOH");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setOnHold %s", str);
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            ConnectionServiceWrapper.this.mCallsManager.markCallAsOnHold(call);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setRingbackRequested(String str, boolean z, Session.Info info) {
            Log.startSession(info, "CSW.SRR");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setRingbackRequested %s %b", str, Boolean.valueOf(z));
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.setRingbackRequested(z);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void removeCall(String str, Session.Info info) {
            Log.startSession(info, "CSW.rC");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("removeCall %s", str);
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            if (call.isAlive()) {
                                ConnectionServiceWrapper.this.mCallsManager.markCallAsDisconnected(call, new DisconnectCause(3));
                            } else {
                                ConnectionServiceWrapper.this.mCallsManager.markCallAsRemoved(call);
                            }
                        }
                    }
                } catch (Throwable th) {
                    Log.e(ConnectionServiceWrapper.this, th, "", new Object[0]);
                    throw th;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setConnectionCapabilities(String str, int i, Session.Info info) {
            Log.startSession(info, "CSW.sCC");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setConnectionCapabilities %s %d", str, Integer.valueOf(i));
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.setConnectionCapabilities(i);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setConnectionProperties(String str, int i, Session.Info info) {
            Log.startSession("CSW.sCP");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setConnectionProperties %s %d", str, Integer.valueOf(i));
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.setConnectionProperties(i);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setIsConferenced(String str, String str2, Session.Info info) {
            Log.startSession(info, "CSW.sIC");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setIsConferenced %s %s", str, str2);
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            if (str2 == null) {
                                Log.d(this, "unsetting parent: %s", new Object[]{str2});
                                call.setParentAndChildCall(null);
                            } else {
                                call.setParentAndChildCall(ConnectionServiceWrapper.this.mCallIdMapper.getCall(str2));
                            }
                        }
                    }
                } catch (Throwable th) {
                    Log.e(ConnectionServiceWrapper.this, th, "", new Object[0]);
                    throw th;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setConferenceMergeFailed(String str, Session.Info info) {
            Log.startSession(info, "CSW.sCMF");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setConferenceMergeFailed %s", str);
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.onConnectionEvent("android.telecom.event.CALL_MERGE_FAILED", null);
                        } else {
                            Log.w(this, "setConferenceMergeFailed, unknown call id: %s", new Object[]{str});
                        }
                    }
                } catch (Throwable th) {
                    Log.e(ConnectionServiceWrapper.this, th, "", new Object[0]);
                    throw th;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void addConferenceCall(String str, ParcelableConference parcelableConference, Session.Info info) {
            Log.startSession(info, "CSW.aCC");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        if (ConnectionServiceWrapper.this.mCallIdMapper.getCall(str) != null) {
                            Log.w(this, "Attempting to add a conference call using an existing call id %s", new Object[]{str});
                            return;
                        }
                        ConnectionServiceWrapper.this.logIncoming("addConferenceCall %s %s [%s]", str, parcelableConference, parcelableConference.getConnectionIds());
                        Iterator it = parcelableConference.getConnectionIds().iterator();
                        boolean z = false;
                        while (it.hasNext()) {
                            if (ConnectionServiceWrapper.this.mCallIdMapper.getCall((String) it.next()) != null) {
                                z = true;
                            }
                        }
                        if (!z && parcelableConference.getConnectionIds().size() > 0) {
                            Log.d(this, "Attempting to add a conference with no valid calls", new Object[0]);
                            return;
                        }
                        PhoneAccountHandle phoneAccount = null;
                        if (parcelableConference != null && parcelableConference.getPhoneAccount() != null) {
                            phoneAccount = parcelableConference.getPhoneAccount();
                        }
                        Bundle extras = parcelableConference.getExtras();
                        Call alreadyAddedConnection = ConnectionServiceWrapper.this.mCallsManager.getAlreadyAddedConnection((extras == null || !extras.containsKey("android.telecom.extra.ORIGINAL_CONNECTION_ID")) ? str : extras.getString("android.telecom.extra.ORIGINAL_CONNECTION_ID"));
                        if (alreadyAddedConnection == null || ConnectionServiceWrapper.this.mCallIdMapper.getCall(str) != null) {
                            alreadyAddedConnection = ConnectionServiceWrapper.this.mCallsManager.createConferenceCall(str, phoneAccount, parcelableConference);
                            ConnectionServiceWrapper.this.mCallIdMapper.addCall(alreadyAddedConnection, str);
                            alreadyAddedConnection.setConnectionService(ConnectionServiceWrapper.this);
                        } else {
                            ConnectionServiceWrapper.this.mCallIdMapper.addCall(alreadyAddedConnection, str);
                            alreadyAddedConnection.replaceConnectionService(ConnectionServiceWrapper.this);
                        }
                        Log.d(this, "adding children to conference %s phAcc %s", new Object[]{parcelableConference.getConnectionIds(), phoneAccount});
                        for (String str2 : parcelableConference.getConnectionIds()) {
                            Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str2);
                            Log.d(this, "found child: %s", new Object[]{str2});
                            if (call != null) {
                                call.setParentAndChildCall(alreadyAddedConnection);
                            }
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void onPostDialWait(String str, String str2, Session.Info info) throws RemoteException {
            Log.startSession(info, "CSW.oPDW");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("onPostDialWait %s %s", str, str2);
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.onPostDialWait(str2);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void onPostDialChar(String str, char c, Session.Info info) throws RemoteException {
            Log.startSession(info, "CSW.oPDC");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("onPostDialChar %s %s", str, Character.valueOf(c));
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.onPostDialChar(c);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void queryRemoteConnectionServices(RemoteServiceCallback remoteServiceCallback, Session.Info info) {
            UserHandle callingUserHandle = Binder.getCallingUserHandle();
            Log.startSession(info, "CSW.qRCS");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("queryRemoteConnectionServices %s", remoteServiceCallback);
                        ConnectionServiceWrapper.this.queryRemoteConnectionServices(callingUserHandle, remoteServiceCallback);
                    }
                } catch (Throwable th) {
                    Log.e(ConnectionServiceWrapper.this, th, "", new Object[0]);
                    throw th;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setVideoState(String str, int i, Session.Info info) {
            Log.startSession(info, "CSW.sVS");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setVideoState %s %d", str, Integer.valueOf(i));
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.setVideoState(i);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setIsVoipAudioMode(String str, boolean z, Session.Info info) {
            Log.startSession(info, "CSW.sIVAM");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setIsVoipAudioMode %s %b", str, Boolean.valueOf(z));
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.setIsVoipAudioMode(z);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setAudioRoute(String str, int i, String str2, Session.Info info) {
            Log.startSession(info, "CSW.sAR");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setAudioRoute %s %s", str, CallAudioState.audioRouteToString(i));
                        ConnectionServiceWrapper.this.mCallsManager.setAudioRoute(i, str2);
                    }
                } catch (Throwable th) {
                    Log.e(ConnectionServiceWrapper.this, th, "", new Object[0]);
                    throw th;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setStatusHints(String str, StatusHints statusHints, Session.Info info) {
            Log.startSession(info, "CSW.sSH");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setStatusHints %s %s", str, statusHints);
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.setStatusHints(statusHints);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void putExtras(String str, Bundle bundle, Session.Info info) {
            Log.startSession(info, "CSW.pE");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        Bundle.setDefusable(bundle, true);
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.putExtras(1, bundle);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void removeExtras(String str, List<String> list, Session.Info info) {
            Log.startSession(info, "CSW.rE");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("removeExtra %s %s", str, list);
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.removeExtras(1, list);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setAddress(String str, Uri uri, int i, Session.Info info) {
            Log.startSession(info, "CSW.sA");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setAddress %s %s %d", str, uri, Integer.valueOf(i));
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.setHandle(uri, i);
                        }
                    }
                } catch (Throwable th) {
                    Log.e(ConnectionServiceWrapper.this, th, "", new Object[0]);
                    throw th;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setCallerDisplayName(String str, String str2, int i, Session.Info info) {
            Log.startSession(info, "CSW.sCDN");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("setCallerDisplayName %s %s %d", str, str2, Integer.valueOf(i));
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.setCallerDisplayName(str2, i);
                        }
                    }
                } catch (Throwable th) {
                    Log.e(ConnectionServiceWrapper.this, th, "", new Object[0]);
                    throw th;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void setConferenceableConnections(String str, List<String> list, Session.Info info) {
            Log.startSession(info, "CSW.sCC");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            ConnectionServiceWrapper.this.logIncoming("setConferenceableConnections %s %s", str, list);
                            ArrayList arrayList = new ArrayList(list.size());
                            Iterator<String> it = list.iterator();
                            while (it.hasNext()) {
                                Call call2 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(it.next());
                                if (call2 != null && call2 != call) {
                                    arrayList.add(call2);
                                }
                            }
                            call.setConferenceableCalls(arrayList);
                        }
                    }
                } catch (Throwable th) {
                    Log.e(ConnectionServiceWrapper.this, th, "", new Object[0]);
                    throw th;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void addExistingConnection(String str, ParcelableConnection parcelableConnection, Session.Info info) {
            PhoneAccountHandle phoneAccountHandle;
            Log.startSession(info, "CSW.aEC");
            UserHandle callingUserHandle = Binder.getCallingUserHandle();
            PhoneAccountHandle phoneAccount = parcelableConnection.getPhoneAccount();
            if (phoneAccount != null) {
                ConnectionServiceWrapper.this.mAppOpsManager.checkPackage(Binder.getCallingUid(), phoneAccount.getComponentName().getPackageName());
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        if (ConnectionServiceWrapper.this.isServiceValid("addExistingConnection")) {
                            PhoneAccountHandle phoneAccountHandle2 = null;
                            for (PhoneAccountHandle phoneAccountHandle3 : ConnectionServiceWrapper.this.mPhoneAccountRegistrar.getCallCapablePhoneAccounts(null, false, callingUserHandle)) {
                                if (phoneAccountHandle3.equals(phoneAccount)) {
                                    phoneAccountHandle2 = phoneAccountHandle3;
                                }
                                phoneAccountHandle2 = phoneAccountHandle2;
                            }
                            if (phoneAccountHandle2 == null && phoneAccount != null && phoneAccount.equals(ConnectionServiceWrapper.this.mPhoneAccountRegistrar.getSimCallManager(callingUserHandle))) {
                                phoneAccountHandle2 = phoneAccount;
                            }
                            if (phoneAccountHandle2 == null && parcelableConnection.getHandle() != null && ConnectionServiceWrapper.this.mCallsManager.getPhoneNumberUtilsAdapter().isLocalEmergencyNumber(MtkTelecomGlobals.getInstance().getContext(), parcelableConnection.getHandle().getSchemeSpecificPart())) {
                                Log.i(ConnectionServiceWrapper.this, "[addExistingConnection]Use PhoneAccount specified by Connection in case of ECC: " + phoneAccount, new Object[0]);
                                phoneAccountHandle = phoneAccount;
                            } else {
                                phoneAccountHandle = phoneAccountHandle2;
                            }
                            if (phoneAccountHandle != null) {
                                ConnectionServiceWrapper.this.logIncoming("addExistingConnection %s %s", str, parcelableConnection);
                                Bundle extras = parcelableConnection.getExtras();
                                Call alreadyAddedConnection = ConnectionServiceWrapper.this.mCallsManager.getAlreadyAddedConnection((extras == null || !extras.containsKey("android.telecom.extra.ORIGINAL_CONNECTION_ID")) ? str : extras.getString("android.telecom.extra.ORIGINAL_CONNECTION_ID"));
                                if (alreadyAddedConnection != null && ConnectionServiceWrapper.this.mCallIdMapper.getCall(str) == null) {
                                    ConnectionServiceWrapper.this.mCallIdMapper.addCall(alreadyAddedConnection, str);
                                    alreadyAddedConnection.replaceConnectionService(ConnectionServiceWrapper.this);
                                } else {
                                    Call callCreateCallForExistingConnection = ConnectionServiceWrapper.this.mCallsManager.createCallForExistingConnection(str, parcelableConnection);
                                    ConnectionServiceWrapper.this.mCallIdMapper.addCall(callCreateCallForExistingConnection, str);
                                    callCreateCallForExistingConnection.setConnectionService(ConnectionServiceWrapper.this);
                                }
                            } else {
                                Log.e(this, new RemoteException("The PhoneAccount being used is not currently registered with Telecom."), "Unable to addExistingConnection.", new Object[0]);
                            }
                        }
                    }
                } catch (Throwable th) {
                    Log.e(ConnectionServiceWrapper.this, th, "", new Object[0]);
                    throw th;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void onConnectionEvent(String str, String str2, Bundle bundle, Session.Info info) {
            Log.startSession(info, "CSW.oCE");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        Bundle.setDefusable(bundle, true);
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.onConnectionEvent(str2, bundle);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void onRttInitiationSuccess(String str, Session.Info info) throws RemoteException {
        }

        public void onRttInitiationFailure(String str, int i, Session.Info info) throws RemoteException {
            Log.startSession(info, "CSW.oRIF");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.onRttConnectionFailure(i);
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void onRttSessionRemotelyTerminated(String str, Session.Info info) throws RemoteException {
        }

        public void onRemoteRttRequest(String str, Session.Info info) throws RemoteException {
            Log.startSession(info, "CSW.oRRR");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.onRemoteRttRequest();
                        }
                    }
                } finally {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void onPhoneAccountChanged(String str, PhoneAccountHandle phoneAccountHandle, Session.Info info) throws RemoteException {
            if (phoneAccountHandle != null) {
                ConnectionServiceWrapper.this.mAppOpsManager.checkPackage(Binder.getCallingUid(), phoneAccountHandle.getComponentName().getPackageName());
            }
            Log.startSession(info, "CSW.oPAC");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str);
                        if (call != null) {
                            call.setTargetPhoneAccount(phoneAccountHandle);
                        }
                    }
                } catch (Throwable th) {
                    Log.e(ConnectionServiceWrapper.this, th, "", new Object[0]);
                    throw th;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void onConnectionServiceFocusReleased(Session.Info info) throws RemoteException {
            Log.startSession(info, "CSW.oCSFR");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.mConnSvrFocusListener.onConnectionServiceReleased(ConnectionServiceWrapper.this);
                    }
                } catch (Throwable th) {
                    Log.e(ConnectionServiceWrapper.this, th, "", new Object[0]);
                    throw th;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }
    }

    ConnectionServiceWrapper(ComponentName componentName, ConnectionServiceRepository connectionServiceRepository, PhoneAccountRegistrar phoneAccountRegistrar, CallsManager callsManager, Context context, TelecomSystem.SyncRoot syncRoot, UserHandle userHandle) {
        super("android.telecom.ConnectionService", componentName, context, syncRoot, userHandle);
        this.mAdapter = new Adapter();
        this.mCallIdMapper = new CallIdMapper(new CallIdMapper.ICallInfo() {
            @Override
            public final String getCallId(Call call) {
                return call.getConnectionId();
            }
        });
        this.mPendingResponses = new HashMap();
        this.mBinder = new ServiceBinder.Binder2();
        this.mMtkAdapter = new MtkAdapter();
        this.mConnectionServiceRepository = connectionServiceRepository;
        phoneAccountRegistrar.addListener(new PhoneAccountRegistrar.Listener() {
        });
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
        this.mCallsManager = callsManager;
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
    }

    private void addConnectionServiceAdapter(IConnectionServiceAdapter iConnectionServiceAdapter) {
        if (isServiceValid("addConnectionServiceAdapter")) {
            try {
                logOutgoing("addConnectionServiceAdapter %s", iConnectionServiceAdapter);
                this.mServiceInterface.addConnectionServiceAdapter(iConnectionServiceAdapter, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    private void removeConnectionServiceAdapter(IConnectionServiceAdapter iConnectionServiceAdapter) {
        if (isServiceValid("removeConnectionServiceAdapter")) {
            try {
                logOutgoing("removeConnectionServiceAdapter %s", iConnectionServiceAdapter);
                this.mServiceInterface.removeConnectionServiceAdapter(iConnectionServiceAdapter, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    @VisibleForTesting
    public void createConnection(final Call call, final CreateConnectionResponse createConnectionResponse) {
        Log.d(this, "createConnection(%s) via %s.", new Object[]{call, getComponentName()});
        if (call.isConferenceInvitation()) {
            createConferenceConnection(call, createConnectionResponse);
        } else {
            this.mBinder.bind(new ServiceBinder.BindCallback() {
                @Override
                public void onSuccess() {
                    String callId = ConnectionServiceWrapper.this.mCallIdMapper.getCallId(call);
                    if (callId != null) {
                        ConnectionServiceWrapper.this.mPendingResponses.put(callId, createConnectionResponse);
                        GatewayInfo gatewayInfo = call.getGatewayInfo();
                        Bundle intentExtras = call.getIntentExtras();
                        if (gatewayInfo != null && gatewayInfo.getGatewayProviderPackageName() != null && gatewayInfo.getOriginalAddress() != null) {
                            intentExtras = (Bundle) intentExtras.clone();
                            intentExtras.putString("android.telecom.extra.GATEWAY_PROVIDER_PACKAGE", gatewayInfo.getGatewayProviderPackageName());
                            intentExtras.putParcelable("android.telecom.extra.GATEWAY_ORIGINAL_ADDRESS", gatewayInfo.getOriginalAddress());
                        }
                        if (call.isIncoming() && ConnectionServiceWrapper.this.mCallsManager.getEmergencyCallHelper().getLastEmergencyCallTimeMillis() > 0) {
                            if (intentExtras == call.getIntentExtras()) {
                                intentExtras = (Bundle) intentExtras.clone();
                            }
                            intentExtras.putLong("android.telecom.extra.LAST_EMERGENCY_CALLBACK_TIME_MILLIS", ConnectionServiceWrapper.this.mCallsManager.getEmergencyCallHelper().getLastEmergencyCallTimeMillis());
                        }
                        if (call.isIncoming() && call.getHandoverSourceCall() != null) {
                            intentExtras.putBoolean("android.telecom.extra.IS_HANDOVER", true);
                            intentExtras.putParcelable("android.telecom.extra.HANDOVER_FROM_PHONE_ACCOUNT", call.getHandoverSourceCall().getTargetPhoneAccount());
                        }
                        ExtensionManager.getDigitsUtilExt().putExtrasForConnectionRequest(intentExtras, call);
                        Log.addEvent(call, "START_CONNECTION", Log.piiHandle(call.getHandle()));
                        try {
                            ConnectionServiceWrapper.this.mServiceInterface.createConnection(call.getConnectionManagerPhoneAccount(), callId, new ConnectionRequest.Builder().setAccountHandle(call.getTargetPhoneAccount()).setAddress(call.getHandle()).setExtras(intentExtras).setVideoState(call.getVideoState()).setTelecomCallId(callId).setShouldShowIncomingCallUi(!ConnectionServiceWrapper.this.mCallsManager.shouldShowSystemIncomingCallUi(call)).setRttPipeFromInCall(call.getInCallToCsRttPipeForCs()).setRttPipeToInCall(call.getCsToInCallRttPipeForCs()).build(), call.shouldAttachToExistingConnection(), call.isUnknown(), Log.getExternalSession());
                            return;
                        } catch (RemoteException e) {
                            Log.e(this, e, "Failure to createConnection -- %s", new Object[]{ConnectionServiceWrapper.this.getComponentName()});
                            ((CreateConnectionResponse) ConnectionServiceWrapper.this.mPendingResponses.remove(callId)).handleCreateConnectionFailure(new DisconnectCause(1, e.toString()));
                            return;
                        }
                    }
                    createConnectionResponse.handleCreateConnectionFailure(new DisconnectCause(1));
                    Log.w(this, "createConnection stop, callId is null", new Object[0]);
                }

                @Override
                public void onFailure() {
                    Log.e(this, new Exception(), "Failure to call %s", new Object[]{ConnectionServiceWrapper.this.getComponentName()});
                    createConnectionResponse.handleCreateConnectionFailure(new DisconnectCause(1));
                }
            }, call);
        }
    }

    void createConnectionFailed(final Call call) {
        Log.d(this, "createConnectionFailed(%s) via %s.", new Object[]{call, getComponentName()});
        this.mBinder.bind(new ServiceBinder.BindCallback() {
            @Override
            public void onSuccess() {
                String callId = ConnectionServiceWrapper.this.mCallIdMapper.getCallId(call);
                if (callId != null && ConnectionServiceWrapper.this.isServiceValid("createConnectionFailed")) {
                    Log.addEvent(call, "CREATE_CONNECTION_FAILED", Log.piiHandle(call.getHandle()));
                    try {
                        ConnectionServiceWrapper.this.logOutgoing("createConnectionFailed %s", callId);
                        ConnectionServiceWrapper.this.mServiceInterface.createConnectionFailed(call.getConnectionManagerPhoneAccount(), callId, new ConnectionRequest(call.getTargetPhoneAccount(), call.getHandle(), call.getIntentExtras(), call.getVideoState(), callId, false), call.isIncoming(), Log.getExternalSession());
                        call.setDisconnectCause(new DisconnectCause(4));
                        call.disconnect();
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override
            public void onFailure() {
                Log.w(this, "onFailure - could not bind to CS for call %s", new Object[]{call.getId()});
            }
        }, call);
    }

    void handoverFailed(final Call call, final int i) {
        Log.d(this, "handoverFailed(%s) via %s.", new Object[]{call, getComponentName()});
        this.mBinder.bind(new ServiceBinder.BindCallback() {
            @Override
            public void onSuccess() {
                String callId = ConnectionServiceWrapper.this.mCallIdMapper.getCallId(call);
                if (callId != null && ConnectionServiceWrapper.this.isServiceValid("handoverFailed")) {
                    Log.addEvent(call, "HANDOVER_FAILED", Log.piiHandle(call.getHandle()));
                    try {
                        ConnectionServiceWrapper.this.mServiceInterface.handoverFailed(callId, new ConnectionRequest(call.getTargetPhoneAccount(), call.getHandle(), call.getIntentExtras(), call.getVideoState(), callId, false), i, Log.getExternalSession());
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override
            public void onFailure() {
                Log.w(this, "onFailure - could not bind to CS for call %s", new Object[]{call.getId()});
            }
        }, call);
    }

    void handoverComplete(final Call call) {
        Log.d(this, "handoverComplete(%s) via %s.", new Object[]{call, getComponentName()});
        this.mBinder.bind(new ServiceBinder.BindCallback() {
            @Override
            public void onSuccess() {
                String callId = ConnectionServiceWrapper.this.mCallIdMapper.getCallId(call);
                if (callId != null && ConnectionServiceWrapper.this.isServiceValid("handoverComplete")) {
                    try {
                        ConnectionServiceWrapper.this.mServiceInterface.handoverComplete(callId, Log.getExternalSession());
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override
            public void onFailure() {
                Log.w(this, "onFailure - could not bind to CS for call %s", new Object[]{call.getId()});
            }
        }, call);
    }

    void abort(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("abort")) {
            try {
                logOutgoing("abort %s", callId);
                this.mServiceInterface.abort(callId, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
        removeCall(call, new DisconnectCause(2));
    }

    void silence(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("silence")) {
            try {
                logOutgoing("silence %s", callId);
                this.mServiceInterface.silence(callId, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void hold(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("hold")) {
            try {
                logOutgoing("hold %s", callId);
                this.mServiceInterface.hold(callId, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void unhold(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("unhold")) {
            try {
                logOutgoing("unhold %s", callId);
                this.mServiceInterface.unhold(callId, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    @VisibleForTesting
    public void onCallAudioStateChanged(Call call, CallAudioState callAudioState) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("onCallAudioStateChanged")) {
            try {
                logOutgoing("onCallAudioStateChanged %s %s", callId, callAudioState);
                if (this.mServiceInterface != null) {
                    this.mServiceInterface.onCallAudioStateChanged(callId, callAudioState, Log.getExternalSession());
                }
            } catch (RemoteException e) {
            }
        }
    }

    void disconnect(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("disconnect")) {
            try {
                logOutgoing("disconnect %s", callId);
                this.mServiceInterface.disconnect(callId, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void answer(Call call, int i) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("answer")) {
            try {
                logOutgoing("answer %s %d", callId, Integer.valueOf(i));
                if (VideoProfile.isAudioOnly(i)) {
                    this.mServiceInterface.answer(callId, Log.getExternalSession());
                } else {
                    this.mServiceInterface.answerVideo(callId, i, Log.getExternalSession());
                }
            } catch (RemoteException e) {
            }
        }
    }

    void deflect(Call call, Uri uri) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("deflect")) {
            try {
                logOutgoing("deflect %s", callId);
                this.mServiceInterface.deflect(callId, uri, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void reject(Call call, boolean z, String str) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("reject")) {
            try {
                logOutgoing("reject %s", callId);
                if (z && call.can(4194304)) {
                    this.mServiceInterface.rejectWithMessage(callId, str, Log.getExternalSession());
                } else {
                    this.mServiceInterface.reject(callId, Log.getExternalSession());
                }
            } catch (RemoteException e) {
            }
        }
    }

    void playDtmfTone(Call call, char c) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("playDtmfTone")) {
            try {
                logOutgoing("playDtmfTone %s %c", callId, Character.valueOf(c));
                this.mServiceInterface.playDtmfTone(callId, c, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void stopDtmfTone(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("stopDtmfTone")) {
            try {
                logOutgoing("stopDtmfTone %s", callId);
                this.mServiceInterface.stopDtmfTone(callId, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void addCall(Call call) {
        if (this.mCallIdMapper.getCallId(call) == null) {
            this.mCallIdMapper.addCall(call);
        }
    }

    void removeCall(Call call) {
        removeCall(call, new DisconnectCause(1));
    }

    void removeCall(String str, DisconnectCause disconnectCause) {
        CreateConnectionResponse createConnectionResponseRemove = this.mPendingResponses.remove(str);
        if (createConnectionResponseRemove != null) {
            createConnectionResponseRemove.handleCreateConnectionFailure(disconnectCause);
        }
        this.mCallIdMapper.removeCall(str);
    }

    void removeCall(Call call, DisconnectCause disconnectCause) {
        CreateConnectionResponse createConnectionResponseRemove = this.mPendingResponses.remove(this.mCallIdMapper.getCallId(call));
        if (createConnectionResponseRemove != null) {
            createConnectionResponseRemove.handleCreateConnectionFailure(disconnectCause);
        }
        this.mCallIdMapper.removeCall(call);
    }

    void onPostDialContinue(Call call, boolean z) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("onPostDialContinue")) {
            try {
                logOutgoing("onPostDialContinue %s %b", callId, Boolean.valueOf(z));
                this.mServiceInterface.onPostDialContinue(callId, z, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void conference(Call call, Call call2) {
        String callId = this.mCallIdMapper.getCallId(call);
        String callId2 = this.mCallIdMapper.getCallId(call2);
        if (callId != null && callId2 != null && isServiceValid("conference")) {
            try {
                logOutgoing("conference %s %s", callId, callId2);
                this.mServiceInterface.conference(callId, callId2, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void splitFromConference(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("splitFromConference")) {
            try {
                logOutgoing("splitFromConference %s", callId);
                this.mServiceInterface.splitFromConference(callId, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void mergeConference(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("mergeConference")) {
            try {
                logOutgoing("mergeConference %s", callId);
                this.mServiceInterface.mergeConference(callId, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void swapConference(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("swapConference")) {
            try {
                logOutgoing("swapConference %s", callId);
                this.mServiceInterface.swapConference(callId, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void pullExternalCall(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("pullExternalCall")) {
            try {
                logOutgoing("pullExternalCall %s", callId);
                this.mServiceInterface.pullExternalCall(callId, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void sendCallEvent(Call call, String str, Bundle bundle) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("sendCallEvent")) {
            try {
                logOutgoing("sendCallEvent %s %s", callId, str);
                this.mServiceInterface.sendCallEvent(callId, str, bundle, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void onExtrasChanged(Call call, Bundle bundle) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("onExtrasChanged")) {
            try {
                logOutgoing("onExtrasChanged %s %s", callId, bundle);
                this.mServiceInterface.onExtrasChanged(callId, bundle, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void startRtt(Call call, ParcelFileDescriptor parcelFileDescriptor, ParcelFileDescriptor parcelFileDescriptor2) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("startRtt")) {
            try {
                logOutgoing("startRtt: %s %s %s", callId, parcelFileDescriptor, parcelFileDescriptor2);
                this.mServiceInterface.startRtt(callId, parcelFileDescriptor, parcelFileDescriptor2, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void stopRtt(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("stopRtt")) {
            try {
                logOutgoing("stopRtt: %s", callId);
                this.mServiceInterface.stopRtt(callId, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    void respondToRttRequest(Call call, ParcelFileDescriptor parcelFileDescriptor, ParcelFileDescriptor parcelFileDescriptor2) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("respondToRttRequest")) {
            try {
                logOutgoing("respondToRttRequest: %s %s %s", callId, parcelFileDescriptor, parcelFileDescriptor2);
                this.mServiceInterface.respondToRttUpgradeRequest(callId, parcelFileDescriptor, parcelFileDescriptor2, Log.getExternalSession());
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    protected void setServiceInterface(IBinder iBinder) {
        this.mServiceInterface = IConnectionService.Stub.asInterface(iBinder);
        Log.v(this, "Adding Connection Service Adapter.", new Object[0]);
        addConnectionServiceAdapter(this.mAdapter);
    }

    @Override
    protected void removeServiceInterface() {
        Log.v(this, "Removing Connection Service Adapter.", new Object[0]);
        removeConnectionServiceAdapter(this.mAdapter);
        handleConnectionServiceDeath();
        this.mCallsManager.handleConnectionServiceDeath(this);
        this.mServiceInterface = null;
    }

    @Override
    public void connectionServiceFocusLost() {
        if (this.mConnSvrFocusListener != null) {
            this.mConnSvrFocusListener.onConnectionServiceReleased(this);
        }
        this.mBinder.bind(new ServiceBinder.BindCallback() {
            @Override
            public void onSuccess() {
                try {
                    ConnectionServiceWrapper.this.mServiceInterface.connectionServiceFocusLost(Log.getExternalSession());
                } catch (RemoteException e) {
                    Log.d(this, "failed to inform the focus lost event", new Object[0]);
                }
            }

            @Override
            public void onFailure() {
            }
        }, null);
    }

    @Override
    public void connectionServiceFocusGained() {
        this.mBinder.bind(new ServiceBinder.BindCallback() {
            @Override
            public void onSuccess() {
                try {
                    ConnectionServiceWrapper.this.mServiceInterface.connectionServiceFocusGained(Log.getExternalSession());
                } catch (RemoteException e) {
                    Log.d(this, "failed to inform the focus gained event", new Object[0]);
                }
            }

            @Override
            public void onFailure() {
            }
        }, null);
    }

    @Override
    public void setConnectionServiceFocusListener(ConnectionServiceFocusManager.ConnectionServiceFocusListener connectionServiceFocusListener) {
        this.mConnSvrFocusListener = connectionServiceFocusListener;
    }

    private void handleCreateConnectionComplete(String str, ConnectionRequest connectionRequest, ParcelableConnection parcelableConnection) {
        if (parcelableConnection.getState() == 6) {
            Call call = this.mCallIdMapper.getCall(str);
            if (call != null && (call.getState() == 3 || call.getState() == 1)) {
                Log.d(this, "disconnect call when create connection complete %s %s", new Object[]{parcelableConnection.getDisconnectCause(), call});
                this.mCallsManager.markCallAsDisconnected(call, parcelableConnection.getDisconnectCause());
                this.mCallsManager.markCallAsRemoved(call);
                return;
            } else {
                Log.d(this, "remove Call when create connection complete", new Object[0]);
                removeCall(str, parcelableConnection.getDisconnectCause());
                return;
            }
        }
        if (this.mPendingResponses.containsKey(str)) {
            Call call2 = this.mCallIdMapper.getCall(str);
            if (call2 != null && call2.isEmergencyCall()) {
                this.mPendingResponses.get(str).handleCreateConnectionSuccess(this.mCallIdMapper, parcelableConnection);
            } else {
                this.mPendingResponses.remove(str).handleCreateConnectionSuccess(this.mCallIdMapper, parcelableConnection);
            }
        }
    }

    private void handleConnectionServiceDeath() {
        if (!this.mPendingResponses.isEmpty()) {
            CreateConnectionResponse[] createConnectionResponseArr = (CreateConnectionResponse[]) this.mPendingResponses.values().toArray(new CreateConnectionResponse[this.mPendingResponses.values().size()]);
            this.mPendingResponses.clear();
            for (CreateConnectionResponse createConnectionResponse : createConnectionResponseArr) {
                createConnectionResponse.handleCreateConnectionFailure(new DisconnectCause(1, "CS_DEATH"));
            }
        }
        this.mCallIdMapper.clear();
        if (this.mConnSvrFocusListener != null) {
            this.mConnSvrFocusListener.onConnectionServiceDeath(this);
        }
    }

    private void logIncoming(String str, Object... objArr) {
        Log.d(this, "ConnectionService -> Telecom[" + this.mComponentName.flattenToShortString() + "]: " + str, objArr);
    }

    private void logOutgoing(String str, Object... objArr) {
        Log.d(this, "Telecom -> ConnectionService[" + this.mComponentName.flattenToShortString() + "]: " + str, objArr);
    }

    private void queryRemoteConnectionServices(UserHandle userHandle, final RemoteServiceCallback remoteServiceCallback) {
        PhoneAccountHandle simCallManager = this.mPhoneAccountRegistrar.getSimCallManager(userHandle);
        Log.d(this, "queryRemoteConnectionServices finds simCallManager = %s", new Object[]{simCallManager});
        if (simCallManager == null || !simCallManager.getComponentName().equals(getComponentName())) {
            noRemoteServices(remoteServiceCallback);
            return;
        }
        final Set<ConnectionServiceWrapper> setNewSetFromMap = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
        for (PhoneAccountHandle phoneAccountHandle : this.mPhoneAccountRegistrar.getSimPhoneAccounts(userHandle)) {
            ConnectionServiceWrapper service = this.mConnectionServiceRepository.getService(phoneAccountHandle.getComponentName(), phoneAccountHandle.getUserHandle());
            if (service != null) {
                setNewSetFromMap.add(service);
            }
        }
        final ArrayList arrayList = new ArrayList();
        final ArrayList arrayList2 = new ArrayList();
        Log.v(this, "queryRemoteConnectionServices, simServices = %s", new Object[]{setNewSetFromMap});
        for (final ConnectionServiceWrapper connectionServiceWrapper : setNewSetFromMap) {
            if (connectionServiceWrapper != this) {
                connectionServiceWrapper.mBinder.bind(new ServiceBinder.BindCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(this, "Adding simService %s", new Object[]{connectionServiceWrapper.getComponentName()});
                        if (connectionServiceWrapper.mServiceInterface == null) {
                            Log.w(this, "queryRemoteConnectionServices: simService %s died - Skipping.", new Object[]{connectionServiceWrapper.getComponentName()});
                        } else {
                            arrayList.add(connectionServiceWrapper.getComponentName());
                            arrayList2.add(connectionServiceWrapper.mServiceInterface.asBinder());
                        }
                        maybeComplete();
                    }

                    @Override
                    public void onFailure() {
                        Log.d(this, "Failed simService %s", new Object[]{connectionServiceWrapper.getComponentName()});
                        ConnectionServiceWrapper.this.noRemoteServices(remoteServiceCallback);
                    }

                    private void maybeComplete() {
                        if (arrayList.size() == setNewSetFromMap.size()) {
                            ConnectionServiceWrapper.this.setRemoteServices(remoteServiceCallback, arrayList, arrayList2);
                        }
                    }
                }, null);
            }
        }
    }

    private void setRemoteServices(RemoteServiceCallback remoteServiceCallback, List<ComponentName> list, List<IBinder> list2) {
        try {
            remoteServiceCallback.onResult(list, list2);
        } catch (RemoteException e) {
            Log.e(this, e, "Contacting ConnectionService %s", new Object[]{getComponentName()});
        }
    }

    private void noRemoteServices(RemoteServiceCallback remoteServiceCallback) {
        setRemoteServices(remoteServiceCallback, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    }

    @Override
    protected void setMtkServiceInterface(IBinder iBinder) {
        this.mMtkServiceInterface = IMtkConnectionService.Stub.asInterface(iBinder);
        Log.v(this, "Adding MTK Connection Service Adapter.", new Object[0]);
        addMtkConnectionServiceAdapter(this.mMtkAdapter);
    }

    @Override
    protected void removeMtkServiceInterface() {
        Log.v(this, "Removing MTK Connection Service Adapter.", new Object[0]);
        removeMtkConnectionServiceAdapter(this.mMtkAdapter);
        this.mMtkServiceInterface = null;
    }

    private final class MtkAdapter extends IMtkConnectionServiceAdapter.Stub {
        private MtkAdapter() {
        }

        public void handleCreateConferenceComplete(String str, ConnectionRequest connectionRequest, ParcelableConference parcelableConference, DisconnectCause disconnectCause) throws RemoteException {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    synchronized (ConnectionServiceWrapper.this.mLock) {
                        ConnectionServiceWrapper.this.logIncoming("handleCreateConferenceComplete %s", connectionRequest);
                        if (str != null) {
                            ConnectionServiceWrapper.this.handleCreateConferenceComplete(str, connectionRequest, parcelableConference, disconnectCause);
                        } else {
                            Log.w(this, "handleCreateConferenceComplete, unknown conference id: %s", new Object[]{str});
                        }
                    }
                } catch (Throwable th) {
                    Log.e(ConnectionServiceWrapper.this, th, "", new Object[0]);
                    throw th;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private void addMtkConnectionServiceAdapter(IMtkConnectionServiceAdapter iMtkConnectionServiceAdapter) {
        if (isMtkServiceValid("addMtkConnectionServiceAdapter")) {
            try {
                logOutgoing("addMtkConnectionServiceAdapter %s", iMtkConnectionServiceAdapter);
                this.mMtkServiceInterface.addMtkConnectionServiceAdapter(iMtkConnectionServiceAdapter);
            } catch (RemoteException e) {
                Log.e(this, e, "Failed to addMtkConnectionServiceAdapter via IMtkConnectionService.addMtkConnectionServiceAdapter", new Object[0]);
            }
        }
    }

    private void removeMtkConnectionServiceAdapter(IMtkConnectionServiceAdapter iMtkConnectionServiceAdapter) {
        if (isMtkServiceValid("removeMtkConnectionServiceAdapter")) {
            try {
                logOutgoing("removeMtkConnectionServiceAdapter %s", iMtkConnectionServiceAdapter);
                this.mMtkServiceInterface.clearMtkConnectionServiceAdapter();
            } catch (RemoteException e) {
                Log.e(this, e, "Failed to removeMtkConnectionServiceAdapter via IMtkConnectionService.clearMtkConnectionServiceAdapter", new Object[0]);
            }
        }
    }

    void disconnectWithPendingAction(Call call, String str) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("disconnectWithPendingAction")) {
            try {
                logOutgoing("disconnectWithPendingAction %s pending action: %s", callId, str);
                this.mMtkServiceInterface.handleOrderedOperation(callId, "mediatek.telecom.operation.DISCONNECT_CALL", str);
            } catch (RemoteException e) {
                Log.e(this, e, "disconnectWithPendingAction handleOrderedOperation", new Object[0]);
            }
        }
    }

    void hangupAll(Call call) {
        if (isServiceValid("hangupAll")) {
            try {
                logOutgoing("hangupAll %s", this.mCallIdMapper.getCallId(call));
                this.mMtkServiceInterface.hangupAll(this.mCallIdMapper.getCallId(call));
            } catch (RemoteException e) {
                Log.e(this, e, "hangupAll", new Object[0]);
            }
        }
    }

    void createConferenceConnection(final Call call, final CreateConnectionResponse createConnectionResponse) {
        Log.d(this, "createConferenceConnection(%s) via %s.", new Object[]{call, getComponentName()});
        this.mBinder.bind(new ServiceBinder.BindCallback() {
            @Override
            public void onSuccess() {
                String callId = ConnectionServiceWrapper.this.mCallIdMapper.getCallId(call);
                ConnectionServiceWrapper.this.mPendingResponses.put(callId, createConnectionResponse);
                GatewayInfo gatewayInfo = call.getGatewayInfo();
                Bundle intentExtras = call.getIntentExtras();
                if (gatewayInfo != null && gatewayInfo.getGatewayProviderPackageName() != null && gatewayInfo.getOriginalAddress() != null) {
                    intentExtras = (Bundle) intentExtras.clone();
                    intentExtras.putString("android.telecom.extra.GATEWAY_PROVIDER_PACKAGE", gatewayInfo.getGatewayProviderPackageName());
                    intentExtras.putParcelable("android.telecom.extra.GATEWAY_ORIGINAL_ADDRESS", gatewayInfo.getOriginalAddress());
                }
                if (call.isIncoming() && ConnectionServiceWrapper.this.mCallsManager.getEmergencyCallHelper().getLastEmergencyCallTimeMillis() > 0) {
                    if (intentExtras == call.getIntentExtras()) {
                        intentExtras = (Bundle) intentExtras.clone();
                    }
                    intentExtras.putLong("android.telecom.extra.LAST_EMERGENCY_CALLBACK_TIME_MILLIS", ConnectionServiceWrapper.this.mCallsManager.getEmergencyCallHelper().getLastEmergencyCallTimeMillis());
                }
                try {
                    ConnectionServiceWrapper.this.mMtkServiceInterface.createConference(call.getConnectionManagerPhoneAccount(), callId, new ConnectionRequest.Builder().setAccountHandle(call.getTargetPhoneAccount()).setAddress(call.getHandle()).setExtras(intentExtras).setVideoState(call.getVideoState()).setTelecomCallId(callId).setShouldShowIncomingCallUi(!ConnectionServiceWrapper.this.mCallsManager.shouldShowSystemIncomingCallUi(call)).setRttPipeFromInCall(call.getInCallToCsRttPipeForCs()).setRttPipeToInCall(call.getCsToInCallRttPipeForCs()).build(), call.getConferenceInvitationNumbers(), call.isIncoming(), Log.getExternalSession());
                } catch (RemoteException e) {
                    Log.e(this, e, "Failure to createConnection -- %s", new Object[]{ConnectionServiceWrapper.this.getComponentName()});
                    ((CreateConnectionResponse) ConnectionServiceWrapper.this.mPendingResponses.remove(callId)).handleCreateConnectionFailure(new DisconnectCause(1, e.toString()));
                }
            }

            @Override
            public void onFailure() {
                Log.e(this, new Exception(), "Failure to call %s", new Object[]{ConnectionServiceWrapper.this.getComponentName()});
                createConnectionResponse.handleCreateConnectionFailure(new DisconnectCause(1));
            }
        }, call);
    }

    private void handleCreateConferenceComplete(String str, ConnectionRequest connectionRequest, ParcelableConference parcelableConference, DisconnectCause disconnectCause) {
        if (parcelableConference.getState() == 6) {
            removeCall(str, disconnectCause);
        } else if (this.mPendingResponses.containsKey(str)) {
            this.mPendingResponses.remove(str);
            this.mCallIdMapper.getCall(connectionRequest.getTelecomCallId()).handleCreateConferenceSuccess(parcelableConference);
        }
    }

    public void inviteNumbersToConference(Call call, List<String> list) {
        if (isServiceValid("inviteNumbersToConference")) {
            try {
                logOutgoing("inviteNumbersToConference %s", this.mCallIdMapper.getCallId(call));
                this.mMtkServiceInterface.inviteConferenceParticipants(this.mCallIdMapper.getCallId(call), list);
            } catch (RemoteException e) {
                Log.e(this, e, "inviteNumbersToConference", new Object[0]);
            }
        }
    }

    void explicitCallTransfer(Call call) {
        if (isServiceValid("explicitCallTransfer")) {
            try {
                logOutgoing("explicitCallTransfer %s", this.mCallIdMapper.getCallId(call));
                this.mMtkServiceInterface.explicitCallTransfer(this.mCallIdMapper.getCallId(call));
            } catch (RemoteException e) {
                Log.e(this, e, "explicitCallTransfer", new Object[0]);
            }
        }
    }

    void explicitCallTransfer(Call call, String str, int i) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("explicitCallTransfer")) {
            try {
                logOutgoing("explicitCallTransfer %s %s %d", callId, str, Integer.valueOf(i));
                this.mMtkServiceInterface.blindAssuredEct(callId, str, i);
            } catch (RemoteException e) {
                Log.e(this, e, "blindAssuredEct", new Object[0]);
            }
        }
    }

    void deviceSwitch(Call call, String str, String str2) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("deviceSwitch")) {
            try {
                logOutgoing("deviceSwitch %s %s %s", callId, str, str2);
                this.mMtkServiceInterface.deviceSwitch(callId, str, str2);
            } catch (RemoteException e) {
                Log.e(this, e, "deviceSwitch", new Object[0]);
            }
        }
    }

    void cancelDeviceSwitch(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("cancelDeviceSwitch")) {
            try {
                logOutgoing("cancelDeviceSwitch %s", callId);
                this.mMtkServiceInterface.cancelDeviceSwitch(callId);
            } catch (RemoteException e) {
                Log.e(this, e, "cancelDeviceSwitch", new Object[0]);
            }
        }
    }
}
