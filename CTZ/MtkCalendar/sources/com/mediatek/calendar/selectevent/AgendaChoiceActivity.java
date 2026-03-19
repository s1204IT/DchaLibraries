package com.mediatek.calendar.selectevent;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import com.android.calendar.CalendarController;
import com.android.calendar.R;
import com.mediatek.calendar.extension.IAgendaChoiceForExt;

public class AgendaChoiceActivity extends Activity implements IAgendaChoiceForExt {
    private CalendarController mController;

    @Override
    protected void onCreate(Bundle bundle) {
        long jCurrentTimeMillis;
        super.onCreate(bundle);
        this.mController = CalendarController.getInstance(this);
        setContentView(R.layout.agenda_choice);
        if (bundle != null) {
            jCurrentTimeMillis = bundle.getLong("other_app_request_time");
        } else {
            jCurrentTimeMillis = System.currentTimeMillis();
        }
        setFragments(jCurrentTimeMillis);
    }

    private void setFragments(long j) {
        FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
        fragmentTransactionBeginTransaction.replace(R.id.agenda_choice_frame, new EventSelectionFragment(j));
        fragmentTransactionBeginTransaction.commit();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putLong("other_app_request_time", this.mController.getTime());
    }

    @Override
    public void retSelectedEvent(Intent intent) {
        setResult(-1, intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CalendarController.removeInstance(this);
    }
}
