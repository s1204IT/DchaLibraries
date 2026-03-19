package com.mediatek.calendar.selectevent;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import com.android.calendar.agenda.AgendaListView;
import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.extension.IAgendaChoiceForExt;

public class EventsListView extends AgendaListView {
    private Context mContext;

    public EventsListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        LogUtil.v("EventsListView", "EventsListView inited");
        this.mContext = context;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        if (j == -1) {
            return;
        }
        shareSingleEvent(getEventIdByPosition(i));
    }

    private void shareSingleEvent(long j) {
        Uri uriWithAppendedId = ContentUris.withAppendedId(Uri.parse("content://com.mediatek.calendarimporter/events"), j);
        Intent intent = new Intent();
        intent.setData(uriWithAppendedId);
        LogUtil.d("EventsListView", "onItemClick(), Email selected calendar, uri=" + uriWithAppendedId);
        if (this.mContext instanceof IAgendaChoiceForExt) {
            ((IAgendaChoiceForExt) this.mContext).retSelectedEvent(intent);
        }
    }
}
