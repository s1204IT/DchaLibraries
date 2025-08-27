package com.android.launcher3.allapps;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Process;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.widget.Switch;
import com.android.launcher3.compat.UserManagerCompat;
import java.util.Iterator;

/* loaded from: classes.dex */
public class WorkModeSwitch extends Switch {
    public WorkModeSwitch(Context context) {
        super(context);
    }

    public WorkModeSwitch(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public WorkModeSwitch(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override // android.widget.Switch, android.widget.CompoundButton, android.widget.Checkable
    public void setChecked(boolean z) {
    }

    @Override // android.widget.Switch, android.widget.CompoundButton, android.widget.Checkable
    public void toggle() {
        trySetQuietModeEnabledToAllProfilesAsync(isChecked());
    }

    private void setCheckedInternal(boolean z) {
        super.setChecked(z);
    }

    public void refresh() {
        setCheckedInternal(!UserManagerCompat.getInstance(getContext()).isAnyProfileQuietModeEnabled());
        setEnabled(true);
    }

    /* JADX WARN: Type inference failed for: r0v0, types: [com.android.launcher3.allapps.WorkModeSwitch$1] */
    private void trySetQuietModeEnabledToAllProfilesAsync(final boolean z) {
        new AsyncTask<Void, Void, Boolean>() { // from class: com.android.launcher3.allapps.WorkModeSwitch.1
            @Override // android.os.AsyncTask
            protected void onPreExecute() {
                super.onPreExecute();
                WorkModeSwitch.this.setEnabled(false);
            }

            /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
            @Override // android.os.AsyncTask
            protected Boolean doInBackground(Void... voidArr) {
                Iterator<UserHandle> it = UserManagerCompat.getInstance(WorkModeSwitch.this.getContext()).getUserProfiles().iterator();
                boolean z2 = false;
                while (it.hasNext()) {
                    if (!Process.myUserHandle().equals(it.next())) {
                        z2 |= !r5.requestQuietModeEnabled(z, r2);
                    }
                }
                return Boolean.valueOf(z2);
            }

            /* JADX DEBUG: Method merged with bridge method: onPostExecute(Ljava/lang/Object;)V */
            @Override // android.os.AsyncTask
            protected void onPostExecute(Boolean bool) {
                if (bool.booleanValue()) {
                    WorkModeSwitch.this.setEnabled(true);
                }
            }
        }.execute(new Void[0]);
    }
}
