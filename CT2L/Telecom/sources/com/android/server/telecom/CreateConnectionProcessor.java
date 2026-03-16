package com.android.server.telecom;

import android.content.Context;
import android.telecom.DisconnectCause;
import android.telecom.ParcelableConnection;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.TelephonyManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

final class CreateConnectionProcessor {
    private Iterator<CallAttemptRecord> mAttemptRecordIterator;
    private List<CallAttemptRecord> mAttemptRecords;
    private final Call mCall;
    private final Context mContext;
    private DisconnectCause mLastErrorDisconnectCause;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private final ConnectionServiceRepository mRepository;
    private CreateConnectionResponse mResponse;
    private boolean mShouldUseConnectionManager = true;
    private CreateConnectionTimeout mTimeout;

    private static class CallAttemptRecord {
        public final PhoneAccountHandle connectionManagerPhoneAccount;
        public final PhoneAccountHandle targetPhoneAccount;

        public CallAttemptRecord(PhoneAccountHandle phoneAccountHandle, PhoneAccountHandle phoneAccountHandle2) {
            this.connectionManagerPhoneAccount = phoneAccountHandle;
            this.targetPhoneAccount = phoneAccountHandle2;
        }

        public String toString() {
            return "CallAttemptRecord(" + Objects.toString(this.connectionManagerPhoneAccount) + "," + Objects.toString(this.targetPhoneAccount) + ")";
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof CallAttemptRecord)) {
                return false;
            }
            CallAttemptRecord callAttemptRecord = (CallAttemptRecord) obj;
            return Objects.equals(this.connectionManagerPhoneAccount, callAttemptRecord.connectionManagerPhoneAccount) && Objects.equals(this.targetPhoneAccount, callAttemptRecord.targetPhoneAccount);
        }
    }

    CreateConnectionProcessor(Call call, ConnectionServiceRepository connectionServiceRepository, CreateConnectionResponse createConnectionResponse, PhoneAccountRegistrar phoneAccountRegistrar, Context context) {
        this.mCall = call;
        this.mRepository = connectionServiceRepository;
        this.mResponse = createConnectionResponse;
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
        this.mContext = context;
    }

    boolean isProcessingComplete() {
        return this.mResponse == null;
    }

    boolean isCallTimedOut() {
        return this.mTimeout != null && this.mTimeout.isCallTimedOut();
    }

    void process() {
        Log.v(this, "process", new Object[0]);
        clearTimeout();
        this.mAttemptRecords = new ArrayList();
        if (this.mCall.getTargetPhoneAccount() != null) {
            this.mAttemptRecords.add(new CallAttemptRecord(this.mCall.getTargetPhoneAccount(), this.mCall.getTargetPhoneAccount()));
        }
        adjustAttemptsForConnectionManager();
        adjustAttemptsForEmergency();
        this.mAttemptRecordIterator = this.mAttemptRecords.iterator();
        attemptNextPhoneAccount();
    }

    boolean hasMorePhoneAccounts() {
        return this.mAttemptRecordIterator.hasNext();
    }

    void continueProcessingIfPossible(CreateConnectionResponse createConnectionResponse, DisconnectCause disconnectCause) {
        Log.v(this, "continueProcessingIfPossible", new Object[0]);
        this.mResponse = createConnectionResponse;
        this.mLastErrorDisconnectCause = disconnectCause;
        attemptNextPhoneAccount();
    }

    void abort() {
        Log.v(this, "abort", new Object[0]);
        CreateConnectionResponse createConnectionResponse = this.mResponse;
        this.mResponse = null;
        clearTimeout();
        ConnectionServiceWrapper connectionService = this.mCall.getConnectionService();
        if (connectionService != null) {
            connectionService.abort(this.mCall);
            this.mCall.clearConnectionService();
        }
        if (createConnectionResponse != null) {
            createConnectionResponse.handleCreateConnectionFailure(new DisconnectCause(2));
        }
    }

    private void attemptNextPhoneAccount() {
        CallAttemptRecord next;
        Log.v(this, "attemptNextPhoneAccount", new Object[0]);
        if (this.mAttemptRecordIterator.hasNext()) {
            next = this.mAttemptRecordIterator.next();
            if (!this.mPhoneAccountRegistrar.phoneAccountHasPermission(next.connectionManagerPhoneAccount)) {
                Log.w(this, "Connection mgr does not have BIND_CONNECTION_SERVICE for attempt: %s", next);
                attemptNextPhoneAccount();
                return;
            } else if (!next.connectionManagerPhoneAccount.equals(next.targetPhoneAccount) && !this.mPhoneAccountRegistrar.phoneAccountHasPermission(next.targetPhoneAccount)) {
                Log.w(this, "Target PhoneAccount does not have BIND_CONNECTION_SERVICE for attempt: %s", next);
                attemptNextPhoneAccount();
                return;
            }
        } else {
            next = null;
        }
        if (this.mResponse != null && next != null) {
            Log.i(this, "Trying attempt %s", next);
            PhoneAccountHandle phoneAccountHandle = next.connectionManagerPhoneAccount;
            ConnectionServiceWrapper service = this.mRepository.getService(phoneAccountHandle.getComponentName(), phoneAccountHandle.getUserHandle());
            if (service == null) {
                Log.i(this, "Found no connection service for attempt %s", next);
                attemptNextPhoneAccount();
                return;
            }
            this.mCall.setConnectionManagerPhoneAccount(next.connectionManagerPhoneAccount);
            this.mCall.setTargetPhoneAccount(next.targetPhoneAccount);
            this.mCall.setConnectionService(service);
            setTimeoutIfNeeded(service, next);
            Log.i(this, "Attempting to call from %s", service.getComponentName());
            service.createConnection(this.mCall, new Response(service));
            return;
        }
        Log.v(this, "attemptNextPhoneAccount, no more accounts, failing", new Object[0]);
        if (this.mResponse != null) {
            clearTimeout();
            this.mResponse.handleCreateConnectionFailure(this.mLastErrorDisconnectCause != null ? this.mLastErrorDisconnectCause : new DisconnectCause(1));
            this.mResponse = null;
            this.mCall.clearConnectionService();
        }
    }

    private void setTimeoutIfNeeded(ConnectionServiceWrapper connectionServiceWrapper, CallAttemptRecord callAttemptRecord) {
        clearTimeout();
        CreateConnectionTimeout createConnectionTimeout = new CreateConnectionTimeout(this.mContext, this.mPhoneAccountRegistrar, connectionServiceWrapper, this.mCall);
        if (createConnectionTimeout.isTimeoutNeededForCall(getConnectionServices(this.mAttemptRecords), callAttemptRecord.connectionManagerPhoneAccount)) {
            this.mTimeout = createConnectionTimeout;
            createConnectionTimeout.registerTimeout();
        }
    }

    private void clearTimeout() {
        if (this.mTimeout != null) {
            this.mTimeout.unregisterTimeout();
            this.mTimeout = null;
        }
    }

    private boolean shouldSetConnectionManager() {
        if (this.mShouldUseConnectionManager && this.mAttemptRecords.size() != 0) {
            if (this.mAttemptRecords.size() > 1) {
                Log.d(this, "shouldSetConnectionManager, error, mAttemptRecords should not have more than 1 record", new Object[0]);
                return false;
            }
            PhoneAccountHandle simCallManager = this.mPhoneAccountRegistrar.getSimCallManager();
            if (simCallManager == null) {
                return false;
            }
            PhoneAccountHandle phoneAccountHandle = this.mAttemptRecords.get(0).targetPhoneAccount;
            if (Objects.equals(simCallManager, phoneAccountHandle)) {
                return false;
            }
            return (this.mPhoneAccountRegistrar.getPhoneAccount(phoneAccountHandle).getCapabilities() & 4) != 0;
        }
        return false;
    }

    private void adjustAttemptsForConnectionManager() {
        if (shouldSetConnectionManager()) {
            CallAttemptRecord callAttemptRecord = new CallAttemptRecord(this.mPhoneAccountRegistrar.getSimCallManager(), this.mAttemptRecords.get(0).targetPhoneAccount);
            Log.v(this, "setConnectionManager, changing %s -> %s", this.mAttemptRecords.get(0), callAttemptRecord);
            this.mAttemptRecords.set(0, callAttemptRecord);
            return;
        }
        Log.v(this, "setConnectionManager, not changing", new Object[0]);
    }

    private void adjustAttemptsForEmergency() {
        List<PhoneAccount> list;
        if (TelephonyUtil.shouldProcessAsEmergency(this.mContext, this.mCall.getHandle())) {
            Log.i(this, "Emergency number detected", new Object[0]);
            this.mAttemptRecords.clear();
            List<PhoneAccount> allPhoneAccounts = this.mPhoneAccountRegistrar.getAllPhoneAccounts();
            if (allPhoneAccounts.isEmpty()) {
                ArrayList arrayList = new ArrayList();
                arrayList.add(TelephonyUtil.getDefaultEmergencyPhoneAccount());
                list = arrayList;
            } else {
                list = allPhoneAccounts;
            }
            PhoneAccountHandle targetPhoneAccount = null;
            if (((TelephonyManager) this.mContext.getSystemService("phone")).getMultiSimConfiguration() == TelephonyManager.MultiSimVariants.DSDS) {
                CallsManager callsManager = CallsManager.getInstance();
                Call firstCallWithState = callsManager.getFirstCallWithState(this.mCall, CallsManager.LIVE_CALL_STATES);
                if (firstCallWithState == null) {
                    firstCallWithState = callsManager.getFirstCallWithState(this.mCall, 6);
                }
                if (firstCallWithState != null) {
                    targetPhoneAccount = firstCallWithState.getTargetPhoneAccount();
                }
            }
            for (PhoneAccount phoneAccount : list) {
                if (phoneAccount.hasCapabilities(16) && phoneAccount.hasCapabilities(4)) {
                    PhoneAccountHandle accountHandle = phoneAccount.getAccountHandle();
                    Log.i(this, "Will try PSTN account %s for emergency", accountHandle);
                    if (accountHandle != null && accountHandle.equals(targetPhoneAccount)) {
                        Log.i(this, "It's a ongoing PSTN account %s in DSDS, try it firstly", accountHandle);
                        this.mAttemptRecords.add(0, new CallAttemptRecord(accountHandle, accountHandle));
                    } else {
                        this.mAttemptRecords.add(new CallAttemptRecord(accountHandle, accountHandle));
                    }
                }
            }
            PhoneAccountHandle simCallManager = this.mPhoneAccountRegistrar.getSimCallManager();
            if (this.mShouldUseConnectionManager && simCallManager != null) {
                PhoneAccount phoneAccount2 = this.mPhoneAccountRegistrar.getPhoneAccount(simCallManager);
                if (phoneAccount2.hasCapabilities(16)) {
                    CallAttemptRecord callAttemptRecord = new CallAttemptRecord(simCallManager, this.mPhoneAccountRegistrar.getDefaultOutgoingPhoneAccount(this.mCall.getHandle().getScheme()));
                    if (!this.mAttemptRecords.contains(callAttemptRecord)) {
                        Log.i(this, "Will try Connection Manager account %s for emergency", phoneAccount2);
                        this.mAttemptRecords.add(callAttemptRecord);
                    }
                }
            }
        }
    }

    private static Collection<PhoneAccountHandle> getConnectionServices(List<CallAttemptRecord> list) {
        HashSet hashSet = new HashSet();
        Iterator<CallAttemptRecord> it = list.iterator();
        while (it.hasNext()) {
            hashSet.add(it.next().connectionManagerPhoneAccount);
        }
        return hashSet;
    }

    private class Response implements CreateConnectionResponse {
        private final ConnectionServiceWrapper mService;

        Response(ConnectionServiceWrapper connectionServiceWrapper) {
            this.mService = connectionServiceWrapper;
        }

        @Override
        public void handleCreateConnectionSuccess(CallIdMapper callIdMapper, ParcelableConnection parcelableConnection) {
            if (CreateConnectionProcessor.this.mResponse == null) {
                this.mService.abort(CreateConnectionProcessor.this.mCall);
            } else {
                CreateConnectionProcessor.this.mResponse.handleCreateConnectionSuccess(callIdMapper, parcelableConnection);
                CreateConnectionProcessor.this.mResponse = null;
            }
        }

        private boolean shouldFallbackToNoConnectionManager(DisconnectCause disconnectCause) {
            ConnectionServiceWrapper connectionService;
            PhoneAccountHandle connectionManagerPhoneAccount = CreateConnectionProcessor.this.mCall.getConnectionManagerPhoneAccount();
            if (connectionManagerPhoneAccount == null || !connectionManagerPhoneAccount.equals(CreateConnectionProcessor.this.mPhoneAccountRegistrar.getSimCallManager()) || (connectionService = CreateConnectionProcessor.this.mCall.getConnectionService()) == null) {
                return false;
            }
            if (disconnectCause.getCode() == 10) {
                Log.d(CreateConnectionProcessor.this, "Connection manager declined to handle the call, falling back to not using a connection manager", new Object[0]);
                return true;
            }
            if (connectionService.isServiceValid("createConnection")) {
                return false;
            }
            Log.d(CreateConnectionProcessor.this, "Connection manager unbound while trying create a connection, falling back to not using a connection manager", new Object[0]);
            return true;
        }

        @Override
        public void handleCreateConnectionFailure(DisconnectCause disconnectCause) {
            Log.d(CreateConnectionProcessor.this, "Connection failed: (%s)", disconnectCause);
            CreateConnectionProcessor.this.mLastErrorDisconnectCause = disconnectCause;
            if (shouldFallbackToNoConnectionManager(disconnectCause)) {
                CreateConnectionProcessor.this.mShouldUseConnectionManager = false;
                CreateConnectionProcessor.this.process();
            } else {
                CreateConnectionProcessor.this.attemptNextPhoneAccount();
            }
        }
    }
}
