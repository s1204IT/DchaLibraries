package com.mediatek.calendar.selectevent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.calendar.R;
import com.android.calendar.agenda.AgendaFragment;
import com.android.calendar.agenda.AgendaListView;
import com.mediatek.calendar.LogUtil;

public class EventSelectionFragment extends AgendaFragment {
    public EventSelectionFragment() {
        this(0L);
    }

    public EventSelectionFragment(long j) {
        super(j, false);
        LogUtil.v("EventSelectionFragment", "EventSelectionFragment created");
    }

    @Override
    protected View extInflateFragmentView(LayoutInflater layoutInflater) {
        LogUtil.v("EventSelectionFragment", "mtk_event_selection_fagment inflated");
        return layoutInflater.inflate(R.layout.mtk_event_selection_fragment, (ViewGroup) null);
    }

    @Override
    protected AgendaListView extFindListView(View view) {
        LogUtil.v("EventSelectionFragment", "found EventsListView");
        return (AgendaListView) view.findViewById(R.id.mtk_events_list);
    }
}
