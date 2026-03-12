package com.android.settings.intelligence.search;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.BenesseExtension;
import android.util.Log;
import android.view.View;
import java.util.List;

public class IntentSearchViewHolder extends SearchViewHolder {
    static final int REQUEST_CODE_NO_OP = 0;

    public IntentSearchViewHolder(View view) {
        super(view);
    }

    @Override
    public int getClickActionMetricName() {
        return 7;
    }

    @Override
    public void onBind(final SearchFragment searchFragment, final SearchResult searchResult) {
        super.onBind(searchFragment, searchResult);
        this.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchFragment.onSearchResultClicked(this, searchResult);
                if (BenesseExtension.getDchaState() != 0) {
                    return;
                }
                Intent intent = searchResult.payload.getIntent();
                if (searchResult instanceof AppSearchResult) {
                    searchFragment.getActivity().startActivity(intent);
                    return;
                }
                List<ResolveInfo> listQueryIntentActivities = searchFragment.getActivity().getPackageManager().queryIntentActivities(intent, 0);
                if (listQueryIntentActivities != null && !listQueryIntentActivities.isEmpty()) {
                    searchFragment.startActivityForResult(intent, 0);
                    return;
                }
                Log.e("IntentSearchViewHolder", "Cannot launch search result, title: " + ((Object) searchResult.title) + ", " + intent);
            }
        });
    }
}
