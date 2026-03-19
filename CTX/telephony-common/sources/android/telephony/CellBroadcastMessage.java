package android.telephony;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

public class CellBroadcastMessage implements Parcelable {
    public static final Parcelable.Creator<CellBroadcastMessage> CREATOR = new Parcelable.Creator<CellBroadcastMessage>() {
        @Override
        public CellBroadcastMessage createFromParcel(Parcel parcel) {
            return new CellBroadcastMessage(parcel);
        }

        @Override
        public CellBroadcastMessage[] newArray(int i) {
            return new CellBroadcastMessage[i];
        }
    };
    public static final String SMS_CB_MESSAGE_EXTRA = "com.android.cellbroadcastreceiver.SMS_CB_MESSAGE";
    protected long mDeliveryTime;
    protected boolean mIsRead;
    protected SmsCbMessage mSmsCbMessage;
    protected int mSubId;

    public CellBroadcastMessage() {
        this.mSubId = 0;
    }

    public void setSubId(int i) {
        this.mSubId = i;
    }

    public int getSubId() {
        return this.mSubId;
    }

    public CellBroadcastMessage(SmsCbMessage smsCbMessage) {
        this.mSubId = 0;
        this.mSmsCbMessage = smsCbMessage;
        this.mDeliveryTime = System.currentTimeMillis();
        this.mIsRead = false;
    }

    private CellBroadcastMessage(SmsCbMessage smsCbMessage, long j, boolean z) {
        this.mSubId = 0;
        this.mSmsCbMessage = smsCbMessage;
        this.mDeliveryTime = j;
        this.mIsRead = z;
    }

    private CellBroadcastMessage(Parcel parcel) {
        this.mSubId = 0;
        this.mSmsCbMessage = new SmsCbMessage(parcel);
        this.mDeliveryTime = parcel.readLong();
        this.mIsRead = parcel.readInt() != 0;
        this.mSubId = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        this.mSmsCbMessage.writeToParcel(parcel, i);
        parcel.writeLong(this.mDeliveryTime);
        parcel.writeInt(this.mIsRead ? 1 : 0);
        parcel.writeInt(this.mSubId);
    }

    public static CellBroadcastMessage createFromCursor(Cursor cursor) {
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
        int i10 = cursor.getInt(cursor.getColumnIndexOrThrow("format"));
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
        int columnIndex3 = cursor.getColumnIndex("cid");
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
        return new CellBroadcastMessage(new SmsCbMessage(i10, i7, i8, smsCbLocation, i9, string2, string3, i11, smsCbEtwsInfo, smsCbCmasInfo), cursor.getLong(cursor.getColumnIndexOrThrow("date")), cursor.getInt(cursor.getColumnIndexOrThrow("read")) != 0);
    }

    public ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues(16);
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
            contentValues.put("cid", Integer.valueOf(location.getCid()));
        }
        contentValues.put("serial_number", Integer.valueOf(smsCbMessage.getSerialNumber()));
        contentValues.put("service_category", Integer.valueOf(smsCbMessage.getServiceCategory()));
        contentValues.put("language", smsCbMessage.getLanguageCode());
        contentValues.put("body", smsCbMessage.getMessageBody());
        contentValues.put("date", Long.valueOf(this.mDeliveryTime));
        contentValues.put("read", Boolean.valueOf(this.mIsRead));
        contentValues.put("format", Integer.valueOf(smsCbMessage.getMessageFormat()));
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
        return contentValues;
    }

    public void setIsRead(boolean z) {
        this.mIsRead = z;
    }

    public String getLanguageCode() {
        return this.mSmsCbMessage.getLanguageCode();
    }

    public int getServiceCategory() {
        return this.mSmsCbMessage.getServiceCategory();
    }

    public long getDeliveryTime() {
        return this.mDeliveryTime;
    }

    public String getMessageBody() {
        return this.mSmsCbMessage.getMessageBody();
    }

    public boolean isRead() {
        return this.mIsRead;
    }

    public int getSerialNumber() {
        return this.mSmsCbMessage.getSerialNumber();
    }

    public SmsCbCmasInfo getCmasWarningInfo() {
        return this.mSmsCbMessage.getCmasWarningInfo();
    }

    public SmsCbEtwsInfo getEtwsWarningInfo() {
        return this.mSmsCbMessage.getEtwsWarningInfo();
    }

    public boolean isPublicAlertMessage() {
        return this.mSmsCbMessage.isEmergencyMessage();
    }

    public boolean isEmergencyAlertMessage() {
        return this.mSmsCbMessage.isEmergencyMessage();
    }

    public boolean isEtwsMessage() {
        return this.mSmsCbMessage.isEtwsMessage();
    }

    public boolean isCmasMessage() {
        return this.mSmsCbMessage.isCmasMessage();
    }

    public int getCmasMessageClass() {
        if (this.mSmsCbMessage.isCmasMessage()) {
            return this.mSmsCbMessage.getCmasWarningInfo().getMessageClass();
        }
        return -1;
    }

    public boolean isEtwsPopupAlert() {
        SmsCbEtwsInfo etwsWarningInfo = this.mSmsCbMessage.getEtwsWarningInfo();
        return etwsWarningInfo != null && etwsWarningInfo.isPopupAlert();
    }

    public boolean isEtwsEmergencyUserAlert() {
        SmsCbEtwsInfo etwsWarningInfo = this.mSmsCbMessage.getEtwsWarningInfo();
        return etwsWarningInfo != null && etwsWarningInfo.isEmergencyUserAlert();
    }

    public boolean isEtwsTestMessage() {
        SmsCbEtwsInfo etwsWarningInfo = this.mSmsCbMessage.getEtwsWarningInfo();
        return etwsWarningInfo != null && etwsWarningInfo.getWarningType() == 3;
    }

    public String getDateString(Context context) {
        return DateUtils.formatDateTime(context, this.mDeliveryTime, 527121);
    }

    public String getSpokenDateString(Context context) {
        return DateUtils.formatDateTime(context, this.mDeliveryTime, 17);
    }
}
