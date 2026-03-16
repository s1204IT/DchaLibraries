package com.android.calendar;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.common.contacts.BaseEmailAddressAdapter;
import com.android.ex.chips.AccountSpecifier;

public class EmailAddressAdapter extends BaseEmailAddressAdapter implements AccountSpecifier {
    private LayoutInflater mInflater;

    public EmailAddressAdapter(Context context) {
        super(context);
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    protected View inflateItemView(ViewGroup parent) {
        return this.mInflater.inflate(R.layout.email_autocomplete_item, parent, false);
    }

    @Override
    protected View inflateItemViewLoading(ViewGroup parent) {
        return this.mInflater.inflate(R.layout.email_autocomplete_item_loading, parent, false);
    }

    @Override
    protected void bindView(View view, String directoryType, String directoryName, String displayName, String emailAddress) {
        TextView text1 = (TextView) view.findViewById(R.id.text1);
        TextView text2 = (TextView) view.findViewById(R.id.text2);
        text1.setText(displayName);
        text2.setText(emailAddress);
    }

    @Override
    protected void bindViewLoading(View view, String directoryType, String directoryName) {
        TextView text1 = (TextView) view.findViewById(R.id.text1);
        Context context = getContext();
        Object[] objArr = new Object[1];
        if (!TextUtils.isEmpty(directoryName)) {
            directoryType = directoryName;
        }
        objArr[0] = directoryType;
        String text = context.getString(R.string.directory_searching_fmt, objArr);
        text1.setText(text);
    }
}
