package com.android.contacts.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;

public class GroupDetailDisplayUtils {
    public static View getNewGroupSourceView(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        return inflater.inflate(R.layout.group_source_button, (ViewGroup) null);
    }

    public static void bindGroupSourceView(Context context, View view, String accountTypeString, String dataSet) {
        AccountTypeManager accountTypeManager = AccountTypeManager.getInstance(context);
        AccountType accountType = accountTypeManager.getAccountType(accountTypeString, dataSet);
        TextView label = (TextView) view.findViewById(android.R.id.title);
        if (label == null) {
            throw new IllegalStateException("Group source view must contain a TextView with idandroid.R.id.label");
        }
        label.setText(accountType.getViewGroupLabel(context));
        ImageView accountIcon = (ImageView) view.findViewById(android.R.id.icon);
        if (accountIcon == null) {
            throw new IllegalStateException("Group source view must contain an ImageView with idandroid.R.id.icon");
        }
        accountIcon.setImageDrawable(accountType.getDisplayIcon(context));
    }
}
