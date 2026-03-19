package com.android.server.telecom;

import android.content.Context;
import android.telecom.DisconnectCause;
import android.telecom.Log;
import android.telecom.ParcelableConnection;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.telecom.CallsManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@VisibleForTesting
public class CreateConnectionProcessor implements CreateConnectionResponse {
    private Iterator<CallAttemptRecord> mAttemptRecordIterator;
    private List<CallAttemptRecord> mAttemptRecords;
    private final Call mCall;
    private CreateConnectionResponse mCallResponse;
    private int mConnectionAttempt;
    private final Context mContext;
    private DisconnectCause mLastErrorDisconnectCause;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private final ConnectionServiceRepository mRepository;
    private ConnectionServiceWrapper mService;
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

    @VisibleForTesting
    public CreateConnectionProcessor(Call call, ConnectionServiceRepository connectionServiceRepository, CreateConnectionResponse createConnectionResponse, PhoneAccountRegistrar phoneAccountRegistrar, Context context) {
        Log.v(this, "CreateConnectionProcessor created for Call = %s", new Object[]{call});
        this.mCall = call;
        this.mRepository = connectionServiceRepository;
        this.mCallResponse = createConnectionResponse;
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
        this.mContext = context;
        this.mConnectionAttempt = 0;
    }

    boolean isProcessingComplete() {
        return this.mCallResponse == null;
    }

    boolean isCallTimedOut() {
        return this.mTimeout != null && this.mTimeout.isCallTimedOut();
    }

    public int getConnectionAttempt() {
        return this.mConnectionAttempt;
    }

    @VisibleForTesting
    public void process() {
        Log.v(this, "process", new Object[0]);
        clearTimeout();
        this.mAttemptRecords = new ArrayList();
        if (this.mCall.getTargetPhoneAccount() != null) {
            this.mAttemptRecords.add(new CallAttemptRecord(this.mCall.getTargetPhoneAccount(), this.mCall.getTargetPhoneAccount()));
        }
        if (!this.mCall.isSelfManaged()) {
            adjustAttemptsForConnectionManager();
            adjustAttemptsForEmergency(this.mCall.getTargetPhoneAccount());
        }
        this.mAttemptRecordIterator = this.mAttemptRecords.iterator();
        attemptNextPhoneAccount();
    }

    boolean hasMorePhoneAccounts() {
        return this.mAttemptRecordIterator.hasNext();
    }

    void continueProcessingIfPossible(CreateConnectionResponse createConnectionResponse, DisconnectCause disconnectCause) {
        Log.v(this, "continueProcessingIfPossible", new Object[0]);
        this.mCallResponse = createConnectionResponse;
        this.mLastErrorDisconnectCause = disconnectCause;
        attemptNextPhoneAccount();
    }

    void abort() {
        Log.v(this, "abort", new Object[0]);
        CreateConnectionResponse createConnectionResponse = this.mCallResponse;
        this.mCallResponse = null;
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
            if (!this.mPhoneAccountRegistrar.phoneAccountRequiresBindPermission(next.connectionManagerPhoneAccount)) {
                Log.w(this, "Connection mgr does not have BIND_TELECOM_CONNECTION_SERVICE for attempt: %s", new Object[]{next});
                attemptNextPhoneAccount();
                return;
            } else if (!next.connectionManagerPhoneAccount.equals(next.targetPhoneAccount) && !this.mPhoneAccountRegistrar.phoneAccountRequiresBindPermission(next.targetPhoneAccount)) {
                Log.w(this, "Target PhoneAccount does not have BIND_TELECOM_CONNECTION_SERVICE for attempt: %s", new Object[]{next});
                attemptNextPhoneAccount();
                return;
            }
        } else {
            next = null;
        }
        if (this.mCallResponse != null && next != null) {
            Log.i(this, "Trying attempt %s", new Object[]{next});
            PhoneAccountHandle phoneAccountHandle = next.connectionManagerPhoneAccount;
            this.mService = this.mRepository.getService(phoneAccountHandle.getComponentName(), phoneAccountHandle.getUserHandle());
            if (this.mService == null) {
                Log.i(this, "Found no connection service for attempt %s", new Object[]{next});
                attemptNextPhoneAccount();
                return;
            }
            this.mConnectionAttempt++;
            this.mCall.setConnectionManagerPhoneAccount(next.connectionManagerPhoneAccount);
            this.mCall.setTargetPhoneAccount(next.targetPhoneAccount);
            this.mCall.setConnectionService(this.mService);
            setTimeoutIfNeeded(this.mService, next);
            if (this.mCall.isIncoming()) {
                this.mService.createConnection(this.mCall, this);
                return;
            } else {
                this.mCall.getConnectionServiceFocusManager().requestFocus(this.mCall, new CallsManager.RequestCallback(new CallsManager.PendingAction() {
                    @Override
                    public void performAction() {
                        Log.d(this, "perform create connection", new Object[0]);
                        CreateConnectionProcessor.this.mService.createConnection(CreateConnectionProcessor.this.mCall, CreateConnectionProcessor.this);
                    }
                }));
                return;
            }
        }
        Log.v(this, "attemptNextPhoneAccount, no more accounts, failing", new Object[0]);
        notifyCallConnectionFailure(this.mLastErrorDisconnectCause != null ? this.mLastErrorDisconnectCause : new DisconnectCause(1));
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
        if (this.mAttemptRecords.size() == 0) {
            return false;
        }
        if (this.mAttemptRecords.size() > 1) {
            Log.d(this, "shouldSetConnectionManager, error, mAttemptRecords should not have more than 1 record", new Object[0]);
            return false;
        }
        PhoneAccountHandle simCallManagerFromCall = this.mPhoneAccountRegistrar.getSimCallManagerFromCall(this.mCall);
        if (simCallManagerFromCall == null) {
            return false;
        }
        PhoneAccountHandle phoneAccountHandle = this.mAttemptRecords.get(0).targetPhoneAccount;
        if (Objects.equals(simCallManagerFromCall, phoneAccountHandle)) {
            return false;
        }
        PhoneAccount phoneAccountUnchecked = this.mPhoneAccountRegistrar.getPhoneAccountUnchecked(phoneAccountHandle);
        if (phoneAccountUnchecked != null) {
            return (phoneAccountUnchecked.getCapabilities() & 4) != 0;
        }
        Log.d(this, "shouldSetConnectionManager, phone account not found", new Object[0]);
        return false;
    }

    private void adjustAttemptsForConnectionManager() {
        if (shouldSetConnectionManager()) {
            CallAttemptRecord callAttemptRecord = new CallAttemptRecord(this.mPhoneAccountRegistrar.getSimCallManagerFromCall(this.mCall), this.mAttemptRecords.get(0).targetPhoneAccount);
            Log.v(this, "setConnectionManager, changing %s -> %s", new Object[]{this.mAttemptRecords.get(0), callAttemptRecord});
            this.mAttemptRecords.add(0, callAttemptRecord);
            return;
        }
        Log.v(this, "setConnectionManager, not changing", new Object[0]);
    }

    private void adjustAttemptsForEmergency(PhoneAccountHandle phoneAccountHandle) {
        PhoneAccount phoneAccountUnchecked;
        if (this.mCall.isEmergencyCall()) {
            Log.i(this, "Emergency number detected", new Object[0]);
            this.mAttemptRecords.clear();
            List<PhoneAccount> allPhoneAccountsOfCurrentUser = this.mPhoneAccountRegistrar.getAllPhoneAccountsOfCurrentUser();
            if (allPhoneAccountsOfCurrentUser.isEmpty()) {
                allPhoneAccountsOfCurrentUser = new ArrayList<>();
                allPhoneAccountsOfCurrentUser.add(TelephonyUtil.getDefaultEmergencyPhoneAccount());
            }
            PhoneAccount phoneAccountUnchecked2 = this.mPhoneAccountRegistrar.getPhoneAccountUnchecked(phoneAccountHandle);
            if (phoneAccountUnchecked2 != null && phoneAccountUnchecked2.hasCapabilities(16) && phoneAccountUnchecked2.hasCapabilities(4)) {
                Log.i(this, "Will try PSTN account %s for emergency", new Object[]{phoneAccountUnchecked2.getAccountHandle()});
                this.mAttemptRecords.add(new CallAttemptRecord(phoneAccountHandle, phoneAccountHandle));
            }
            TelephonyUtil.sortSimPhoneAccounts(this.mContext, allPhoneAccountsOfCurrentUser);
            if (this.mAttemptRecords.isEmpty()) {
                Iterator<PhoneAccount> it = allPhoneAccountsOfCurrentUser.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    PhoneAccount next = it.next();
                    if (next.hasCapabilities(16) && next.hasCapabilities(4)) {
                        PhoneAccountHandle accountHandle = next.getAccountHandle();
                        Log.i(this, "Will try PSTN account %s for emergency", new Object[]{accountHandle});
                        this.mAttemptRecords.add(new CallAttemptRecord(accountHandle, accountHandle));
                        break;
                    }
                }
            }
            PhoneAccountHandle simCallManagerOfCurrentUser = this.mPhoneAccountRegistrar.getSimCallManagerOfCurrentUser();
            if (simCallManagerOfCurrentUser != null && (phoneAccountUnchecked = this.mPhoneAccountRegistrar.getPhoneAccountUnchecked(simCallManagerOfCurrentUser)) != null && phoneAccountUnchecked.hasCapabilities(16)) {
                CallAttemptRecord callAttemptRecord = new CallAttemptRecord(simCallManagerOfCurrentUser, this.mPhoneAccountRegistrar.getOutgoingPhoneAccountForSchemeOfCurrentUser(this.mCall.getHandle() == null ? null : this.mCall.getHandle().getScheme()));
                if (!this.mAttemptRecords.contains(callAttemptRecord)) {
                    Log.i(this, "Will try Connection Manager account %s for emergency", new Object[]{phoneAccountUnchecked});
                    this.mAttemptRecords.add(callAttemptRecord);
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

    private void notifyCallConnectionFailure(DisconnectCause disconnectCause) {
        if (this.mCallResponse != null) {
            clearTimeout();
            this.mCallResponse.handleCreateConnectionFailure(disconnectCause);
            this.mCallResponse = null;
            this.mCall.clearConnectionService();
        }
    }

    @Override
    public void handleCreateConnectionSuccess(CallIdMapper callIdMapper, ParcelableConnection parcelableConnection) {
        if (this.mCallResponse == null) {
            this.mService.abort(this.mCall);
            return;
        }
        this.mCallResponse.handleCreateConnectionSuccess(callIdMapper, parcelableConnection);
        if (parcelableConnection != null && parcelableConnection.getHandle() != null) {
            parcelableConnection.getHandle().getSchemeSpecificPart();
        }
        if (this.mCall == null || !this.mCall.isEmergencyCall()) {
            this.mCallResponse = null;
        }
    }

    private boolean shouldFailCallIfConnectionManagerFails(DisconnectCause disconnectCause) {
        PhoneAccountHandle connectionManagerPhoneAccount = this.mCall.getConnectionManagerPhoneAccount();
        if (connectionManagerPhoneAccount == null || !connectionManagerPhoneAccount.equals(this.mPhoneAccountRegistrar.getSimCallManagerFromCall(this.mCall))) {
            return false;
        }
        ConnectionServiceWrapper connectionService = this.mCall.getConnectionService();
        if (connectionService == null) {
            return true;
        }
        if (disconnectCause.getCode() == 10) {
            Log.d(this, "Connection manager declined to handle the call, falling back to not using a connection manager", new Object[0]);
            return false;
        }
        if (!connectionService.isServiceValid("createConnection")) {
            Log.d(this, "Connection manager unbound while trying create a connection, falling back to not using a connection manager", new Object[0]);
            return false;
        }
        Log.d(this, "Connection Manager denied call with the following error: " + disconnectCause.getReason() + ". Not falling back to SIM.", new Object[0]);
        return true;
    }

    @Override
    public void handleCreateConnectionFailure(DisconnectCause disconnectCause) {
        Log.d(this, "Connection failed: (%s)", new Object[]{disconnectCause});
        if (shouldFailCallIfConnectionManagerFails(disconnectCause)) {
            notifyCallConnectionFailure(disconnectCause);
        } else {
            this.mLastErrorDisconnectCause = disconnectCause;
            attemptNextPhoneAccount();
        }
    }
}
