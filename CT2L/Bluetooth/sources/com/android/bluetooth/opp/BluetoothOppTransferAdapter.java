package com.android.bluetooth.opp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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

    public BluetoothOppTransferAdapter(Context context, int layout, Cursor c) {
        super(context, layout, c);
        this.mContext = context;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String completeText;
        Resources r = context.getResources();
        ImageView iv = (ImageView) view.findViewById(R.id.transfer_icon);
        int status = cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.STATUS));
        int dir = cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.DIRECTION));
        if (BluetoothShare.isStatusError(status)) {
            iv.setImageResource(android.R.drawable.stat_notify_error);
        } else if (dir == 0) {
            iv.setImageResource(android.R.drawable.stat_sys_upload_done);
        } else {
            iv.setImageResource(android.R.drawable.stat_sys_download_done);
        }
        TextView tv = (TextView) view.findViewById(R.id.transfer_title);
        String title = cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare.FILENAME_HINT));
        if (title == null) {
            title = this.mContext.getString(R.string.unknown_file);
        }
        tv.setText(title);
        TextView tv2 = (TextView) view.findViewById(R.id.targetdevice);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        int destinationColumnId = cursor.getColumnIndexOrThrow(BluetoothShare.DESTINATION);
        BluetoothDevice remoteDevice = adapter.getRemoteDevice(cursor.getString(destinationColumnId));
        String deviceName = BluetoothOppManager.getInstance(context).getDeviceName(remoteDevice);
        tv2.setText(deviceName);
        long totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(BluetoothShare.TOTAL_BYTES));
        if (BluetoothShare.isStatusCompleted(status)) {
            TextView tv3 = (TextView) view.findViewById(R.id.complete_text);
            tv3.setVisibility(0);
            if (BluetoothShare.isStatusError(status)) {
                tv3.setText(BluetoothOppUtility.getStatusDescription(this.mContext, status, deviceName));
            } else {
                if (dir == 1) {
                    completeText = r.getString(R.string.download_success, Formatter.formatFileSize(this.mContext, totalBytes));
                } else {
                    completeText = r.getString(R.string.upload_success, Formatter.formatFileSize(this.mContext, totalBytes));
                }
                tv3.setText(completeText);
            }
            int dateColumnId = cursor.getColumnIndexOrThrow("timestamp");
            long time = cursor.getLong(dateColumnId);
            Date d = new Date(time);
            CharSequence str = DateUtils.isToday(time) ? DateFormat.getTimeFormat(this.mContext).format(d) : DateFormat.getDateFormat(this.mContext).format(d);
            TextView tv4 = (TextView) view.findViewById(R.id.complete_date);
            tv4.setVisibility(0);
            tv4.setText(str);
        }
    }
}
