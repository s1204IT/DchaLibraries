package com.android.server.telecom;

import android.telecom.DisconnectCause;
import android.telecom.ParcelableConnection;
import com.android.internal.annotations.VisibleForTesting;

@VisibleForTesting
public interface CreateConnectionResponse {
    void handleCreateConnectionFailure(DisconnectCause disconnectCause);

    void handleCreateConnectionSuccess(CallIdMapper callIdMapper, ParcelableConnection parcelableConnection);
}
