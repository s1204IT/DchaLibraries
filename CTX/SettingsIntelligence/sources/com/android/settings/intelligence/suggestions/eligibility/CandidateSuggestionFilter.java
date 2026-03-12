package com.android.settings.intelligence.suggestions.eligibility;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.intelligence.R;
import com.android.settings.intelligence.suggestions.model.CandidateSuggestion;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CandidateSuggestionFilter {
    private static CandidateSuggestionFilter sChecker;
    private static ExecutorService sExecutorService;

    public static CandidateSuggestionFilter getInstance() {
        if (sChecker == null) {
            sChecker = new CandidateSuggestionFilter();
            sExecutorService = Executors.newCachedThreadPool();
        }
        return sChecker;
    }

    public synchronized List<CandidateSuggestion> filterCandidates(Context context, List<CandidateSuggestion> list) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        ArrayList<CandidateFilterTask> arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        if (list == null) {
            return arrayList2;
        }
        Iterator<CandidateSuggestion> it = list.iterator();
        while (it.hasNext()) {
            CandidateFilterTask candidateFilterTask = new CandidateFilterTask(context, it.next());
            sExecutorService.execute(candidateFilterTask);
            arrayList.add(candidateFilterTask);
        }
        for (CandidateFilterTask candidateFilterTask2 : arrayList) {
            try {
                CandidateSuggestion candidateSuggestion = candidateFilterTask2.get(context.getResources().getInteger(R.integer.check_task_timeout_ms), TimeUnit.MILLISECONDS);
                if (candidateSuggestion != null) {
                    arrayList2.add(candidateSuggestion);
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Log.w("CandidateSuggestionFilter", "Error checking completion state for " + candidateFilterTask2.getId());
            }
        }
        Log.d("CandidateSuggestionFilter", "filterCandidates duration: " + (System.currentTimeMillis() - jCurrentTimeMillis));
        return arrayList2;
    }

    static class CandidateFilterTask extends FutureTask<CandidateSuggestion> {
        private final String mId;

        public CandidateFilterTask(Context context, CandidateSuggestion candidateSuggestion) {
            super(new GetSuggestionStatusCallable(context, candidateSuggestion));
            this.mId = candidateSuggestion.getId();
        }

        public String getId() {
            return this.mId;
        }

        static class GetSuggestionStatusCallable implements Callable<CandidateSuggestion> {
            static final String CONTENT_PROVIDER_INTENT_ACTION = "com.android.settings.action.SUGGESTION_STATE_PROVIDER";
            private final CandidateSuggestion mCandidate;
            private final Context mContext;

            public GetSuggestionStatusCallable(Context context, CandidateSuggestion candidateSuggestion) {
                this.mContext = context.getApplicationContext();
                this.mCandidate = candidateSuggestion;
            }

            @Override
            public CandidateSuggestion call() throws Exception {
                List<ResolveInfo> listQueryIntentContentProviders = this.mContext.getPackageManager().queryIntentContentProviders(new Intent(CONTENT_PROVIDER_INTENT_ACTION).setPackage(this.mCandidate.getComponent().getPackageName()), 0);
                if (listQueryIntentContentProviders == null || listQueryIntentContentProviders.isEmpty()) {
                    return this.mCandidate;
                }
                ProviderInfo providerInfo = listQueryIntentContentProviders.get(0).providerInfo;
                if (providerInfo == null || TextUtils.isEmpty(providerInfo.authority)) {
                    return null;
                }
                Bundle bundleCall = this.mContext.getContentResolver().call(new Uri.Builder().scheme("content").authority(providerInfo.authority).build(), "getSuggestionState", (String) null, buildGetSuggestionStateExtras(this.mCandidate));
                boolean z = bundleCall.getBoolean("candidate_is_complete", false);
                Log.d("CandidateSuggestionFilter", "Suggestion state result " + bundleCall);
                if (z) {
                    return null;
                }
                return this.mCandidate;
            }

            static Bundle buildGetSuggestionStateExtras(CandidateSuggestion candidateSuggestion) {
                Bundle bundle = new Bundle();
                bundle.putString("candidate_id", candidateSuggestion.getId());
                bundle.putParcelable("android.intent.extra.COMPONENT_NAME", candidateSuggestion.getComponent());
                return bundle;
            }
        }
    }
}
