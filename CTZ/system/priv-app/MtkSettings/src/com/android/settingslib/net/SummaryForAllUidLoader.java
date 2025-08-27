package com.android.settingslib.net;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.INetworkStatsSession;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.RemoteException;

/* loaded from: classes.dex */
public class SummaryForAllUidLoader extends AsyncTaskLoader<NetworkStats> {
    private final Bundle mArgs;
    private final INetworkStatsSession mSession;

    public static Bundle buildArgs(NetworkTemplate networkTemplate, long j, long j2) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("template", networkTemplate);
        bundle.putLong("start", j);
        bundle.putLong("end", j2);
        return bundle;
    }

    public SummaryForAllUidLoader(Context context, INetworkStatsSession iNetworkStatsSession, Bundle bundle) {
        super(context);
        this.mSession = iNetworkStatsSession;
        this.mArgs = bundle;
    }

    @Override // android.content.Loader
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    /* JADX DEBUG: Method merged with bridge method: loadInBackground()Ljava/lang/Object; */
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // android.content.AsyncTaskLoader
    public NetworkStats loadInBackground() {
        try {
            return this.mSession.getSummaryForAllUid(this.mArgs.getParcelable("template"), this.mArgs.getLong("start"), this.mArgs.getLong("end"), false);
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override // android.content.Loader
    protected void onStopLoading() {
        super.onStopLoading();
        cancelLoad();
    }

    @Override // android.content.Loader
    protected void onReset() {
        super.onReset();
        cancelLoad();
    }
}
