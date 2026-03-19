package com.android.server.telecom.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.provider.BlockedNumberContract;
import android.telephony.PhoneNumberUtils;
import android.text.SpannableString;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.android.server.telecom.R;

public class BlockedNumbersAdapter extends SimpleCursorAdapter {
    public BlockedNumbersAdapter(Context context, int i, Cursor cursor, String[] strArr, int[] iArr, int i2) {
        super(context, i, cursor, strArr, iArr, i2);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        super.bindView(view, context, cursor);
        final String string = cursor.getString(cursor.getColumnIndex("original_number"));
        final String number = BlockedNumbersUtil.formatNumber(string);
        TextView textView = (TextView) view.findViewById(R.id.blocked_number);
        SpannableString spannableString = new SpannableString(number);
        PhoneNumberUtils.addTtsSpan(spannableString, 0, spannableString.length());
        textView.setText(spannableString);
        view.findViewById(R.id.delete_blocked_number).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                BlockedNumbersAdapter.this.showDeleteBlockedNumberDialog(context, string, number);
            }
        });
    }

    private void showDeleteBlockedNumberDialog(final Context context, final String str, String str2) {
        String string = context.getString(R.string.unblock_dialog_body, str2);
        int iIndexOf = string.indexOf(str2);
        SpannableString spannableString = new SpannableString(string);
        PhoneNumberUtils.addTtsSpan(spannableString, iIndexOf, str2.length() + iIndexOf);
        new AlertDialog.Builder(context).setMessage(spannableString).setPositiveButton(R.string.unblock_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                BlockedNumbersAdapter.this.deleteBlockedNumber(context, str);
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        }).create().show();
    }

    private void deleteBlockedNumber(Context context, String str) {
        context.getContentResolver().delete(BlockedNumberContract.BlockedNumbers.CONTENT_URI, "original_number=?", new String[]{str});
        BlockedNumbersUtil.showToastWithFormattedNumber(this.mContext, R.string.blocked_numbers_number_unblocked_message, str);
    }
}
