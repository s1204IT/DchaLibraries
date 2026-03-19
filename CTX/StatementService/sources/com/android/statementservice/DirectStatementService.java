package com.android.statementservice;

import android.app.Service;
import android.content.Intent;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.ResultReceiver;
import android.util.Log;
import com.android.statementservice.retriever.AbstractAsset;
import com.android.statementservice.retriever.AbstractAssetMatcher;
import com.android.statementservice.retriever.AbstractStatementRetriever;
import com.android.statementservice.retriever.AssociationServiceException;
import com.android.statementservice.retriever.Relation;
import com.android.statementservice.retriever.Statement;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import org.json.JSONException;

public final class DirectStatementService extends Service {
    private static final String TAG = DirectStatementService.class.getSimpleName();
    private Handler mHandler;
    private HttpResponseCache mHttpResponseCache;
    private AbstractStatementRetriever mStatementRetriever;
    private HandlerThread mThread;

    @Override
    public void onCreate() {
        this.mThread = new HandlerThread("DirectStatementService thread", 10);
        this.mThread.start();
        onCreate(AbstractStatementRetriever.createDirectRetriever(this), this.mThread.getLooper(), getCacheDir());
    }

    public void onCreate(AbstractStatementRetriever abstractStatementRetriever, Looper looper, File file) {
        super.onCreate();
        this.mStatementRetriever = abstractStatementRetriever;
        this.mHandler = new Handler(looper);
        try {
            this.mHttpResponseCache = HttpResponseCache.install(new File(file, "request_cache"), 1048576L);
        } catch (IOException e) {
            Log.i(TAG, "HTTPS response cache installation failed:" + e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final HttpResponseCache httpResponseCache = this.mHttpResponseCache;
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (httpResponseCache != null) {
                        httpResponseCache.delete();
                    }
                } catch (IOException e) {
                    Log.i(DirectStatementService.TAG, "HTTP(S) response cache deletion failed:" + e);
                }
                Looper.myLooper().quit();
            }
        });
        this.mHttpResponseCache = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        super.onStartCommand(intent, i, i2);
        if (intent == null) {
            Log.e(TAG, "onStartCommand called with null intent");
            return 1;
        }
        if (intent.getAction().equals("com.android.statementservice.service.CHECK_ALL_ACTION")) {
            Bundle extras = intent.getExtras();
            ArrayList<String> stringArrayList = extras.getStringArrayList("com.android.statementservice.service.SOURCE_ASSET_DESCRIPTORS");
            String string = extras.getString("com.android.statementservice.service.TARGET_ASSET_DESCRIPTOR");
            String string2 = extras.getString("com.android.statementservice.service.RELATION");
            ResultReceiver resultReceiver = (ResultReceiver) extras.getParcelable("com.android.statementservice.service.RESULT_RECEIVER");
            if (resultReceiver == null) {
                Log.e(TAG, " Intent does not have extra com.android.statementservice.service.RESULT_RECEIVER");
                return 1;
            }
            if (stringArrayList == null) {
                Log.e(TAG, " Intent does not have extra com.android.statementservice.service.SOURCE_ASSET_DESCRIPTORS");
                resultReceiver.send(1, Bundle.EMPTY);
                return 1;
            }
            if (string == null) {
                Log.e(TAG, " Intent does not have extra com.android.statementservice.service.TARGET_ASSET_DESCRIPTOR");
                resultReceiver.send(1, Bundle.EMPTY);
                return 1;
            }
            if (string2 == null) {
                Log.e(TAG, " Intent does not have extra com.android.statementservice.service.RELATION");
                resultReceiver.send(1, Bundle.EMPTY);
                return 1;
            }
            this.mHandler.post(new ExceptionLoggingFutureTask(new IsAssociatedCallable(stringArrayList, string, string2, resultReceiver), TAG));
        } else {
            Log.e(TAG, "onStartCommand called with unsupported action: " + intent.getAction());
        }
        return 1;
    }

    private class IsAssociatedCallable implements Callable<Void> {
        private String mRelation;
        private ResultReceiver mResultReceiver;
        private List<String> mSources;
        private String mTarget;

        public IsAssociatedCallable(List<String> list, String str, String str2, ResultReceiver resultReceiver) {
            this.mSources = list;
            this.mTarget = str;
            this.mRelation = str2;
            this.mResultReceiver = resultReceiver;
        }

        private boolean verifyOneSource(AbstractAsset abstractAsset, AbstractAssetMatcher abstractAssetMatcher, Relation relation) throws AssociationServiceException {
            for (Statement statement : DirectStatementService.this.mStatementRetriever.retrieveStatements(abstractAsset).getStatements()) {
                if (relation.matches(statement.getRelation()) && abstractAssetMatcher.matches(statement.getTarget())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Void call() {
            Bundle bundle = new Bundle();
            ArrayList<String> arrayList = new ArrayList<>();
            try {
                AbstractAssetMatcher abstractAssetMatcherCreateMatcher = AbstractAssetMatcher.createMatcher(this.mTarget);
                Relation relationCreate = Relation.create(this.mRelation);
                Iterator<String> it = this.mSources.iterator();
                boolean z = true;
                while (it.hasNext()) {
                    try {
                        AbstractAsset abstractAssetCreate = AbstractAsset.create(it.next());
                        try {
                            if (!verifyOneSource(abstractAssetCreate, abstractAssetMatcherCreateMatcher, relationCreate)) {
                                arrayList.add(abstractAssetCreate.toJson());
                                z = false;
                            }
                        } catch (AssociationServiceException e) {
                            arrayList.add(abstractAssetCreate.toJson());
                            z = false;
                        }
                    } catch (AssociationServiceException e2) {
                        this.mResultReceiver.send(1, Bundle.EMPTY);
                        return null;
                    }
                }
                bundle.putBoolean("is_associated", z);
                bundle.putStringArrayList("failed_sources", arrayList);
                this.mResultReceiver.send(0, bundle);
                return null;
            } catch (AssociationServiceException | JSONException e3) {
                Log.e(DirectStatementService.TAG, "isAssociatedCallable failed with exception", e3);
                this.mResultReceiver.send(1, Bundle.EMPTY);
                return null;
            }
        }
    }
}
