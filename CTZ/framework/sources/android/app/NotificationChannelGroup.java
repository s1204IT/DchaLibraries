package android.app;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.proto.ProtoOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public final class NotificationChannelGroup implements Parcelable {
    private static final String ATT_BLOCKED = "blocked";
    private static final String ATT_DESC = "desc";
    private static final String ATT_ID = "id";
    private static final String ATT_NAME = "name";
    public static final Parcelable.Creator<NotificationChannelGroup> CREATOR = new Parcelable.Creator<NotificationChannelGroup>() {
        @Override
        public NotificationChannelGroup createFromParcel(Parcel parcel) {
            return new NotificationChannelGroup(parcel);
        }

        @Override
        public NotificationChannelGroup[] newArray(int i) {
            return new NotificationChannelGroup[i];
        }
    };
    private static final int MAX_TEXT_LENGTH = 1000;
    private static final String TAG_GROUP = "channelGroup";
    private boolean mBlocked;
    private List<NotificationChannel> mChannels = new ArrayList();
    private String mDescription;
    private final String mId;
    private CharSequence mName;

    public NotificationChannelGroup(String str, CharSequence charSequence) {
        this.mId = getTrimmedString(str);
        this.mName = charSequence != null ? getTrimmedString(charSequence.toString()) : null;
    }

    protected NotificationChannelGroup(Parcel parcel) {
        if (parcel.readByte() != 0) {
            this.mId = parcel.readString();
        } else {
            this.mId = null;
        }
        this.mName = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        if (parcel.readByte() != 0) {
            this.mDescription = parcel.readString();
        } else {
            this.mDescription = null;
        }
        parcel.readParcelableList(this.mChannels, NotificationChannel.class.getClassLoader());
        this.mBlocked = parcel.readBoolean();
    }

    private String getTrimmedString(String str) {
        if (str != null && str.length() > 1000) {
            return str.substring(0, 1000);
        }
        return str;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (this.mId != null) {
            parcel.writeByte((byte) 1);
            parcel.writeString(this.mId);
        } else {
            parcel.writeByte((byte) 0);
        }
        TextUtils.writeToParcel(this.mName, parcel, i);
        if (this.mDescription != null) {
            parcel.writeByte((byte) 1);
            parcel.writeString(this.mDescription);
        } else {
            parcel.writeByte((byte) 0);
        }
        parcel.writeParcelableList(this.mChannels, i);
        parcel.writeBoolean(this.mBlocked);
    }

    public String getId() {
        return this.mId;
    }

    public CharSequence getName() {
        return this.mName;
    }

    public String getDescription() {
        return this.mDescription;
    }

    public List<NotificationChannel> getChannels() {
        return this.mChannels;
    }

    public boolean isBlocked() {
        return this.mBlocked;
    }

    public void setDescription(String str) {
        this.mDescription = getTrimmedString(str);
    }

    public void setBlocked(boolean z) {
        this.mBlocked = z;
    }

    public void addChannel(NotificationChannel notificationChannel) {
        this.mChannels.add(notificationChannel);
    }

    public void setChannels(List<NotificationChannel> list) {
        this.mChannels = list;
    }

    public void populateFromXml(XmlPullParser xmlPullParser) {
        setDescription(xmlPullParser.getAttributeValue(null, ATT_DESC));
        setBlocked(safeBool(xmlPullParser, "blocked", false));
    }

    private static boolean safeBool(XmlPullParser xmlPullParser, String str, boolean z) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        return TextUtils.isEmpty(attributeValue) ? z : Boolean.parseBoolean(attributeValue);
    }

    public void writeXml(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.startTag(null, TAG_GROUP);
        xmlSerializer.attribute(null, "id", getId());
        if (getName() != null) {
            xmlSerializer.attribute(null, "name", getName().toString());
        }
        if (getDescription() != null) {
            xmlSerializer.attribute(null, ATT_DESC, getDescription().toString());
        }
        xmlSerializer.attribute(null, "blocked", Boolean.toString(isBlocked()));
        xmlSerializer.endTag(null, TAG_GROUP);
    }

    @SystemApi
    public JSONObject toJson() throws JSONException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("id", getId());
        jSONObject.put("name", getName());
        jSONObject.put(ATT_DESC, getDescription());
        jSONObject.put("blocked", isBlocked());
        return jSONObject;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        NotificationChannelGroup notificationChannelGroup = (NotificationChannelGroup) obj;
        if (isBlocked() != notificationChannelGroup.isBlocked()) {
            return false;
        }
        if (getId() == null ? notificationChannelGroup.getId() != null : !getId().equals(notificationChannelGroup.getId())) {
            return false;
        }
        if (getName() == null ? notificationChannelGroup.getName() != null : !getName().equals(notificationChannelGroup.getName())) {
            return false;
        }
        if (getDescription() == null ? notificationChannelGroup.getDescription() != null : !getDescription().equals(notificationChannelGroup.getDescription())) {
            return false;
        }
        if (getChannels() != null) {
            return getChannels().equals(notificationChannelGroup.getChannels());
        }
        if (notificationChannelGroup.getChannels() == null) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (((((((getId() != null ? getId().hashCode() : 0) * 31) + (getName() != null ? getName().hashCode() : 0)) * 31) + (getDescription() != null ? getDescription().hashCode() : 0)) * 31) + (isBlocked() ? 1 : 0))) + (getChannels() != null ? getChannels().hashCode() : 0);
    }

    public NotificationChannelGroup m13clone() {
        NotificationChannelGroup notificationChannelGroup = new NotificationChannelGroup(getId(), getName());
        notificationChannelGroup.setDescription(getDescription());
        notificationChannelGroup.setBlocked(isBlocked());
        notificationChannelGroup.setChannels(getChannels());
        return notificationChannelGroup;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NotificationChannelGroup{mId='");
        sb.append(this.mId);
        sb.append(DateFormat.QUOTE);
        sb.append(", mName=");
        sb.append((Object) this.mName);
        sb.append(", mDescription=");
        sb.append(!TextUtils.isEmpty(this.mDescription) ? "hasDescription " : "");
        sb.append(", mBlocked=");
        sb.append(this.mBlocked);
        sb.append(", mChannels=");
        sb.append(this.mChannels);
        sb.append('}');
        return sb.toString();
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1138166333441L, this.mId);
        protoOutputStream.write(1138166333442L, this.mName.toString());
        protoOutputStream.write(1138166333443L, this.mDescription);
        protoOutputStream.write(1133871366148L, this.mBlocked);
        Iterator<NotificationChannel> it = this.mChannels.iterator();
        while (it.hasNext()) {
            it.next().writeToProto(protoOutputStream, 2246267895813L);
        }
        protoOutputStream.end(jStart);
    }
}
