package com.android.settings.applications;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.accessibility.ListDialogPreference;

/* loaded from: classes.dex */
public class AppDomainsPreference extends ListDialogPreference {
    private int mNumEntries;

    public AppDomainsPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setDialogLayoutResource(R.layout.app_domains_dialog);
        setListItemLayoutResource(R.layout.app_domains_item);
    }

    @Override // com.android.settings.accessibility.ListDialogPreference
    public void setTitles(CharSequence[] charSequenceArr) {
        this.mNumEntries = charSequenceArr != null ? charSequenceArr.length : 0;
        super.setTitles(charSequenceArr);
    }

    @Override // com.android.settings.accessibility.ListDialogPreference, android.support.v7.preference.Preference
    public CharSequence getSummary() {
        int i;
        Context context = getContext();
        if (this.mNumEntries == 0) {
            return context.getString(R.string.domain_urls_summary_none);
        }
        CharSequence summary = super.getSummary();
        if (this.mNumEntries == 1) {
            i = R.string.domain_urls_summary_one;
        } else {
            i = R.string.domain_urls_summary_some;
        }
        return context.getString(i, summary);
    }

    @Override // com.android.settings.accessibility.ListDialogPreference
    protected void onBindListItem(View view, int i) {
        CharSequence titleAt = getTitleAt(i);
        if (titleAt != null) {
            ((TextView) view.findViewById(R.id.domain_name)).setText(titleAt);
        }
    }
}
