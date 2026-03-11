package com.android.settings.net;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.INetworkStatsSession;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.RemoteException;
import com.android.settings.DataUsageSummary;

public class ChartDataLoader extends AsyncTaskLoader<ChartData> {
    private final Bundle mArgs;
    private final INetworkStatsSession mSession;

    public static Bundle buildArgs(NetworkTemplate template, DataUsageSummary.AppItem app) {
        return buildArgs(template, app, 10);
    }

    public static Bundle buildArgs(NetworkTemplate template, DataUsageSummary.AppItem app, int fields) {
        Bundle args = new Bundle();
        args.putParcelable("template", template);
        args.putParcelable("app", app);
        args.putInt("fields", fields);
        return args;
    }

    public ChartDataLoader(Context context, INetworkStatsSession session, Bundle args) {
        super(context);
        this.mSession = session;
        this.mArgs = args;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    @Override
    public ChartData loadInBackground() {
        NetworkTemplate template = (NetworkTemplate) this.mArgs.getParcelable("template");
        DataUsageSummary.AppItem app = (DataUsageSummary.AppItem) this.mArgs.getParcelable("app");
        int fields = this.mArgs.getInt("fields");
        try {
            return loadInBackground(template, app, fields);
        } catch (RemoteException e) {
            throw new RuntimeException("problem reading network stats", e);
        }
    }

    private ChartData loadInBackground(NetworkTemplate template, DataUsageSummary.AppItem app, int fields) throws RemoteException {
        ChartData data = new ChartData();
        data.network = this.mSession.getHistoryForNetwork(template, fields);
        if (app != null) {
            int size = app.uids.size();
            for (int i = 0; i < size; i++) {
                int uid = app.uids.keyAt(i);
                data.detailDefault = collectHistoryForUid(template, uid, 0, data.detailDefault);
                data.detailForeground = collectHistoryForUid(template, uid, 1, data.detailForeground);
            }
            if (size > 0) {
                data.detail = new NetworkStatsHistory(data.detailForeground.getBucketDuration());
                data.detail.recordEntireHistory(data.detailDefault);
                data.detail.recordEntireHistory(data.detailForeground);
            } else {
                data.detailDefault = new NetworkStatsHistory(3600000L);
                data.detailForeground = new NetworkStatsHistory(3600000L);
                data.detail = new NetworkStatsHistory(3600000L);
            }
        }
        return data;
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();
        cancelLoad();
    }

    private NetworkStatsHistory collectHistoryForUid(NetworkTemplate template, int uid, int set, NetworkStatsHistory existing) throws RemoteException {
        NetworkStatsHistory history = this.mSession.getHistoryForUid(template, uid, set, 0, 10);
        if (existing == null) {
            return history;
        }
        existing.recordEntireHistory(history);
        return existing;
    }
}
