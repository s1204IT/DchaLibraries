package com.android.bluetooth.opp;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import com.android.bluetooth.R;
import java.util.Date;

public class BluetoothOppTransferAdapter extends ResourceCursorAdapter {
    private Context mContext;

    public BluetoothOppTransferAdapter(Context context, int i, Cursor cursor) {
        super(context, i, cursor);
        this.mContext = context;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String string;
        Resources resources = context.getResources();
        ImageView imageView = (ImageView) view.findViewById(R.id.transfer_icon);
        int i = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
        int i2 = cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.DIRECTION));
        if (BluetoothShare.isStatusError(i)) {
            imageView.setImageResource(android.R.drawable.stat_notify_error);
        } else if (i2 == 0) {
            imageView.setImageResource(android.R.drawable.stat_sys_upload_done);
        } else {
            imageView.setImageResource(android.R.drawable.stat_sys_download_done);
        }
        TextView textView = (TextView) view.findViewById(R.id.transfer_title);
        String string2 = cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare.FILENAME_HINT));
        if (string2 == null) {
            string2 = this.mContext.getString(R.string.unknown_file);
        }
        textView.setText(string2);
        TextView textView2 = (TextView) view.findViewById(R.id.targetdevice);
        String deviceName = BluetoothOppManager.getInstance(context).getDeviceName(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare.DESTINATION))));
        textView2.setText(deviceName);
        long j = cursor.getLong(cursor.getColumnIndexOrThrow(BluetoothShare.TOTAL_BYTES));
        if (BluetoothShare.isStatusCompleted(i)) {
            TextView textView3 = (TextView) view.findViewById(R.id.complete_text);
            textView3.setVisibility(0);
            if (BluetoothShare.isStatusError(i)) {
                textView3.setText(BluetoothOppUtility.getStatusDescription(this.mContext, i, deviceName));
            } else {
                if (i2 == 1) {
                    string = resources.getString(R.string.download_success, Formatter.formatFileSize(this.mContext, j));
                } else {
                    string = resources.getString(R.string.upload_success, Formatter.formatFileSize(this.mContext, j));
                }
                textView3.setText(string);
            }
            long j2 = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
            Date date = new Date(j2);
            String str = DateUtils.isToday(j2) ? DateFormat.getTimeFormat(this.mContext).format(date) : DateFormat.getDateFormat(this.mContext).format(date);
            TextView textView4 = (TextView) view.findViewById(R.id.complete_date);
            textView4.setVisibility(0);
            textView4.setText(str);
        }
    }
}
