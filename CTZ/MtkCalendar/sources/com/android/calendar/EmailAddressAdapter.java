package com.android.calendar;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.common.contacts.BaseEmailAddressAdapter;
import com.android.mtkex.chips.AccountSpecifier;

public class EmailAddressAdapter extends BaseEmailAddressAdapter implements AccountSpecifier {
    private LayoutInflater mInflater;

    public EmailAddressAdapter(Context context) {
        super(context);
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    protected View inflateItemView(ViewGroup viewGroup) {
        return this.mInflater.inflate(R.layout.email_autocomplete_item, viewGroup, false);
    }

    @Override
    protected View inflateItemViewLoading(ViewGroup viewGroup) {
        return this.mInflater.inflate(R.layout.email_autocomplete_item_loading, viewGroup, false);
    }

    @Override
    protected void bindView(View view, String str, String str2, String str3, String str4) {
        TextView textView = (TextView) view.findViewById(R.id.text1);
        TextView textView2 = (TextView) view.findViewById(R.id.text2);
        textView.setText(str3);
        textView2.setText(str4);
    }

    @Override
    protected void bindViewLoading(View view, String str, String str2) {
        TextView textView = (TextView) view.findViewById(R.id.text1);
        Context context = getContext();
        Object[] objArr = new Object[1];
        if (!TextUtils.isEmpty(str2)) {
            str = str2;
        }
        objArr[0] = str;
        textView.setText(context.getString(R.string.directory_searching_fmt, objArr));
    }
}
