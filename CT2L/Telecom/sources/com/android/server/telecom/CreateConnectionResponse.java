package com.android.server.telecom;

import android.telecom.DisconnectCause;
import android.telecom.ParcelableConnection;

interface CreateConnectionResponse {
    void handleCreateConnectionFailure(DisconnectCause disconnectCause);

    void handleCreateConnectionSuccess(CallIdMapper callIdMapper, ParcelableConnection parcelableConnection);
}
