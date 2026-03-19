package mediatek.telephony;

import android.content.ContentValues;
import android.database.Cursor;
import android.telephony.CellBroadcastMessage;
import android.telephony.SmsCbCmasInfo;
import android.telephony.SmsCbEtwsInfo;
import android.telephony.SmsCbLocation;
import android.telephony.SmsCbMessage;
import com.mediatek.internal.telephony.cat.BipUtils;
import com.mediatek.internal.telephony.ppl.IPplSmsFilter;

public class MtkCellBroadcastMessage extends CellBroadcastMessage {
    private MtkCellBroadcastMessage(int i, SmsCbMessage smsCbMessage, long j, boolean z) {
        this.mSubId = i;
        this.mSmsCbMessage = smsCbMessage;
        this.mDeliveryTime = j;
        this.mIsRead = z;
    }

    public static MtkCellBroadcastMessage createFromCursor(Cursor cursor) {
        String string;
        int i;
        int i2;
        SmsCbEtwsInfo smsCbEtwsInfo;
        SmsCbCmasInfo smsCbCmasInfo;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7 = cursor.getInt(cursor.getColumnIndexOrThrow("geo_scope"));
        int i8 = cursor.getInt(cursor.getColumnIndexOrThrow("serial_number"));
        int i9 = cursor.getInt(cursor.getColumnIndexOrThrow("service_category"));
        String string2 = cursor.getString(cursor.getColumnIndexOrThrow("language"));
        String string3 = cursor.getString(cursor.getColumnIndexOrThrow("body"));
        int i10 = cursor.getInt(cursor.getColumnIndexOrThrow(IPplSmsFilter.KEY_FORMAT));
        int i11 = cursor.getInt(cursor.getColumnIndexOrThrow("priority"));
        int columnIndex = cursor.getColumnIndex("plmn");
        int i12 = -1;
        if (columnIndex != -1 && !cursor.isNull(columnIndex)) {
            string = cursor.getString(columnIndex);
        } else {
            string = null;
        }
        int columnIndex2 = cursor.getColumnIndex("lac");
        if (columnIndex2 != -1 && !cursor.isNull(columnIndex2)) {
            i = cursor.getInt(columnIndex2);
        } else {
            i = -1;
        }
        int columnIndex3 = cursor.getColumnIndex(BipUtils.KEY_QOS_CID);
        if (columnIndex3 != -1 && !cursor.isNull(columnIndex3)) {
            i2 = cursor.getInt(columnIndex3);
        } else {
            i2 = -1;
        }
        SmsCbLocation smsCbLocation = new SmsCbLocation(string, i, i2);
        int columnIndex4 = cursor.getColumnIndex("etws_warning_type");
        if (columnIndex4 != -1 && !cursor.isNull(columnIndex4)) {
            smsCbEtwsInfo = new SmsCbEtwsInfo(cursor.getInt(columnIndex4), false, false, false, (byte[]) null);
        } else {
            smsCbEtwsInfo = null;
        }
        int columnIndex5 = cursor.getColumnIndex("cmas_message_class");
        if (columnIndex5 != -1 && !cursor.isNull(columnIndex5)) {
            int i13 = cursor.getInt(columnIndex5);
            int columnIndex6 = cursor.getColumnIndex("cmas_category");
            if (columnIndex6 != -1 && !cursor.isNull(columnIndex6)) {
                i3 = cursor.getInt(columnIndex6);
            } else {
                i3 = -1;
            }
            int columnIndex7 = cursor.getColumnIndex("cmas_response_type");
            if (columnIndex7 != -1 && !cursor.isNull(columnIndex7)) {
                i4 = cursor.getInt(columnIndex7);
            } else {
                i4 = -1;
            }
            int columnIndex8 = cursor.getColumnIndex("cmas_severity");
            if (columnIndex8 != -1 && !cursor.isNull(columnIndex8)) {
                i5 = cursor.getInt(columnIndex8);
            } else {
                i5 = -1;
            }
            int columnIndex9 = cursor.getColumnIndex("cmas_urgency");
            if (columnIndex9 != -1 && !cursor.isNull(columnIndex9)) {
                i6 = cursor.getInt(columnIndex9);
            } else {
                i6 = -1;
            }
            int columnIndex10 = cursor.getColumnIndex("cmas_certainty");
            if (columnIndex10 != -1 && !cursor.isNull(columnIndex10)) {
                i12 = cursor.getInt(columnIndex10);
            }
            smsCbCmasInfo = new SmsCbCmasInfo(i13, i3, i4, i5, i6, i12);
        } else {
            smsCbCmasInfo = null;
        }
        return new MtkCellBroadcastMessage(cursor.getInt(cursor.getColumnIndexOrThrow("sub_id")), new SmsCbMessage(i10, i7, i8, smsCbLocation, i9, string2, string3, i11, smsCbEtwsInfo, smsCbCmasInfo), cursor.getLong(cursor.getColumnIndexOrThrow("date")), cursor.getInt(cursor.getColumnIndexOrThrow("read")) != 0);
    }

    public ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues(17);
        SmsCbMessage smsCbMessage = this.mSmsCbMessage;
        contentValues.put("geo_scope", Integer.valueOf(smsCbMessage.getGeographicalScope()));
        SmsCbLocation location = smsCbMessage.getLocation();
        if (location.getPlmn() != null) {
            contentValues.put("plmn", location.getPlmn());
        }
        if (location.getLac() != -1) {
            contentValues.put("lac", Integer.valueOf(location.getLac()));
        }
        if (location.getCid() != -1) {
            contentValues.put(BipUtils.KEY_QOS_CID, Integer.valueOf(location.getCid()));
        }
        contentValues.put("serial_number", Integer.valueOf(smsCbMessage.getSerialNumber()));
        contentValues.put("service_category", Integer.valueOf(smsCbMessage.getServiceCategory()));
        contentValues.put("language", smsCbMessage.getLanguageCode());
        contentValues.put("body", smsCbMessage.getMessageBody());
        contentValues.put("date", Long.valueOf(this.mDeliveryTime));
        contentValues.put("read", Boolean.valueOf(this.mIsRead));
        contentValues.put(IPplSmsFilter.KEY_FORMAT, Integer.valueOf(smsCbMessage.getMessageFormat()));
        contentValues.put("priority", Integer.valueOf(smsCbMessage.getMessagePriority()));
        SmsCbEtwsInfo etwsWarningInfo = this.mSmsCbMessage.getEtwsWarningInfo();
        if (etwsWarningInfo != null) {
            contentValues.put("etws_warning_type", Integer.valueOf(etwsWarningInfo.getWarningType()));
        }
        SmsCbCmasInfo cmasWarningInfo = this.mSmsCbMessage.getCmasWarningInfo();
        if (cmasWarningInfo != null) {
            contentValues.put("cmas_message_class", Integer.valueOf(cmasWarningInfo.getMessageClass()));
            contentValues.put("cmas_category", Integer.valueOf(cmasWarningInfo.getCategory()));
            contentValues.put("cmas_response_type", Integer.valueOf(cmasWarningInfo.getResponseType()));
            contentValues.put("cmas_severity", Integer.valueOf(cmasWarningInfo.getSeverity()));
            contentValues.put("cmas_urgency", Integer.valueOf(cmasWarningInfo.getUrgency()));
            contentValues.put("cmas_certainty", Integer.valueOf(cmasWarningInfo.getCertainty()));
        }
        contentValues.put("sub_id", Integer.valueOf(this.mSubId));
        return contentValues;
    }
}
