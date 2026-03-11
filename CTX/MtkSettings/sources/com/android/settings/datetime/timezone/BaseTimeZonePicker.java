package com.android.settings.datetime.timezone;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.datetime.timezone.BaseTimeZoneAdapter;
import com.android.settings.datetime.timezone.model.TimeZoneData;
import com.android.settings.datetime.timezone.model.TimeZoneDataLoader;
import java.util.Locale;

public abstract class BaseTimeZonePicker extends InstrumentedFragment implements SearchView.OnQueryTextListener {
    private BaseTimeZoneAdapter mAdapter;
    private final boolean mDefaultExpandSearch;
    private RecyclerView mRecyclerView;
    private final boolean mSearchEnabled;
    private final int mSearchHintResId;
    private SearchView mSearchView;
    private TimeZoneData mTimeZoneData;
    private final int mTitleResId;

    public interface OnListItemClickListener<T extends BaseTimeZoneAdapter.AdapterItem> {
        void onListItemClick(T t);
    }

    protected abstract BaseTimeZoneAdapter createAdapter(TimeZoneData timeZoneData);

    protected BaseTimeZonePicker(int i, int i2, boolean z, boolean z2) {
        this.mTitleResId = i;
        this.mSearchHintResId = i2;
        this.mSearchEnabled = z;
        this.mDefaultExpandSearch = z2;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        getActivity().setTitle(this.mTitleResId);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.time_zone_items_list, viewGroup, false);
        this.mRecyclerView = (RecyclerView) viewInflate.findViewById(R.id.recycler_view);
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), 1, false));
        this.mRecyclerView.setAdapter(this.mAdapter);
        getLoaderManager().initLoader(0, null, new TimeZoneDataLoader.LoaderCreator(getContext(), new TimeZoneDataLoader.OnDataReadyCallback() {
            @Override
            public final void onTimeZoneDataReady(TimeZoneData timeZoneData) {
                this.f$0.onTimeZoneDataReady(timeZoneData);
            }
        }));
        return viewInflate;
    }

    public void onTimeZoneDataReady(TimeZoneData timeZoneData) {
        if (this.mTimeZoneData == null && timeZoneData != null) {
            this.mTimeZoneData = timeZoneData;
            this.mAdapter = createAdapter(this.mTimeZoneData);
            if (this.mRecyclerView != null) {
                this.mRecyclerView.setAdapter(this.mAdapter);
            }
        }
    }

    protected Locale getLocale() {
        return getContext().getResources().getConfiguration().getLocales().get(0);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        if (this.mSearchEnabled) {
            menuInflater.inflate(R.menu.time_zone_base_search_menu, menu);
            MenuItem menuItemFindItem = menu.findItem(R.id.time_zone_search_menu);
            this.mSearchView = (SearchView) menuItemFindItem.getActionView();
            this.mSearchView.setQueryHint(getText(this.mSearchHintResId));
            this.mSearchView.setOnQueryTextListener(this);
            if (this.mDefaultExpandSearch) {
                menuItemFindItem.expandActionView();
                this.mSearchView.setIconified(false);
                this.mSearchView.setActivated(true);
                this.mSearchView.setQuery("", true);
            }
            TextView textView = (TextView) this.mSearchView.findViewById(android.R.id.marquee);
            textView.setPadding(0, textView.getPaddingTop(), 0, textView.getPaddingBottom());
            View viewFindViewById = this.mSearchView.findViewById(android.R.id.low_light);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) viewFindViewById.getLayoutParams();
            layoutParams.setMarginStart(0);
            layoutParams.setMarginEnd(0);
            viewFindViewById.setLayoutParams(layoutParams);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String str) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String str) {
        if (this.mAdapter != null) {
            this.mAdapter.getFilter().filter(str);
            return false;
        }
        return false;
    }
}
