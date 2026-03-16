package jp.co.omronsoft.android.decoemojimanager.interfacedata;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.common.speech.LoggingEvents;

public class DecoEmojiModInfo implements Parcelable {
    public static final Parcelable.Creator<DecoEmojiModInfo> CREATOR = new Parcelable.Creator<DecoEmojiModInfo>() {
        @Override
        public DecoEmojiModInfo createFromParcel(Parcel in) {
            return new DecoEmojiModInfo(in);
        }

        @Override
        public DecoEmojiModInfo[] newArray(int size) {
            return new DecoEmojiModInfo[size];
        }
    };
    private String mCategory;
    private String[] mName;
    private String[] mNote;
    private byte[] mPart;
    private String[] mTags;
    private String[] mTagsElement;
    private String mType;
    private String mUri;

    public DecoEmojiModInfo() {
        this.mName = new String[10];
        this.mPart = new byte[10];
        this.mNote = new String[10];
        this.mCategory = new String();
        this.mType = new String();
        this.mTags = new String[100];
        this.mTagsElement = new String[100];
    }

    public DecoEmojiModInfo(Parcel in) {
        this.mName = new String[10];
        this.mPart = new byte[10];
        this.mNote = new String[10];
        this.mCategory = new String();
        this.mType = new String();
        this.mTags = new String[100];
        this.mTagsElement = new String[100];
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mUri);
        dest.writeStringArray(this.mName);
        dest.writeByteArray(this.mPart);
        dest.writeStringArray(this.mNote);
        dest.writeString(this.mCategory);
        dest.writeString(this.mType);
        dest.writeStringArray(this.mTags);
        dest.writeStringArray(this.mTagsElement);
    }

    public void readFromParcel(Parcel in) {
        this.mUri = in.readString();
        in.readStringArray(this.mName);
        in.readByteArray(this.mPart);
        in.readStringArray(this.mNote);
        this.mCategory = in.readString();
        this.mType = in.readString();
        in.readStringArray(this.mTags);
        in.readStringArray(this.mTagsElement);
    }

    public String getUri() {
        return this.mUri;
    }

    public void setUri(String uri) {
        this.mUri = uri;
    }

    public void setName(int idx, String name) {
        this.mName[idx] = name;
    }

    public void setPart(int idx, byte part) {
        this.mPart[idx] = part;
    }

    public String getName(int idx) {
        return this.mName[idx];
    }

    public byte getPart(int idx) {
        return this.mPart[idx];
    }

    public void setNote(int idx, String note) {
        this.mNote[idx] = note;
    }

    public String getNote(int idx) {
        return this.mNote[idx];
    }

    public void setCategory(String category) {
        this.mCategory = category;
    }

    public String getCategory() {
        return this.mCategory;
    }

    public void setEmojiType(String emojiType) {
        this.mType = emojiType;
    }

    public String getEmojiType() {
        return this.mType;
    }

    public void setTags(int idx, String tagName) {
        this.mTags[idx] = tagName;
    }

    public String getTags(int idx) {
        return this.mTags[idx];
    }

    public void setTagsElement(int idx, String tagElement) {
        this.mTagsElement[idx] = tagElement;
    }

    public String getTagsElement(int idx) {
        return this.mTagsElement[idx];
    }

    public void sortParts() {
        String[] saveName = new String[10];
        byte[] savePart = new byte[10];
        String[] saveNote = new String[10];
        int roopNum = 0;
        for (int i = 0; i < 10; i++) {
            saveName[i] = LoggingEvents.EXTRA_CALLING_APP_NAME;
            saveNote[i] = LoggingEvents.EXTRA_CALLING_APP_NAME;
        }
        for (int i2 = 0; i2 < 10; i2++) {
            if (!this.mName[i2].equals(LoggingEvents.EXTRA_CALLING_APP_NAME)) {
                saveName[roopNum] = this.mName[i2];
                savePart[roopNum] = this.mPart[i2];
                saveNote[roopNum] = this.mNote[i2];
                roopNum++;
            }
        }
        for (int i3 = 0; i3 < 10; i3++) {
            this.mName[i3] = saveName[i3];
            this.mPart[i3] = savePart[i3];
            this.mNote[i3] = saveNote[i3];
        }
    }
}
