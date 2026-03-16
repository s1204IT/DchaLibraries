package jp.co.omronsoft.android.decoemojimanager.interfacedata;

import android.os.Parcel;
import android.os.Parcelable;

public class DecoEmojiCategoryInfo implements Parcelable {
    public static final Parcelable.Creator<DecoEmojiCategoryInfo> CREATOR = new Parcelable.Creator<DecoEmojiCategoryInfo>() {
        @Override
        public DecoEmojiCategoryInfo createFromParcel(Parcel in) {
            return new DecoEmojiCategoryInfo(in);
        }

        @Override
        public DecoEmojiCategoryInfo[] newArray(int size) {
            return new DecoEmojiCategoryInfo[size];
        }
    };
    private int mCategory_id;
    private String mCategory_name_eng;
    private String mCategory_name_jpn;
    private int mCategory_preset_id;
    private int mPageCnt;

    public DecoEmojiCategoryInfo() {
    }

    public DecoEmojiCategoryInfo(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mCategory_id);
        dest.writeString(this.mCategory_name_jpn);
        dest.writeString(this.mCategory_name_eng);
        dest.writeInt(this.mPageCnt);
    }

    public void readFromParcel(Parcel in) {
        this.mCategory_id = in.readInt();
        this.mCategory_name_jpn = in.readString();
        this.mCategory_name_eng = in.readString();
        this.mPageCnt = in.readInt();
    }

    public int getCategoryId() {
        return this.mCategory_id;
    }

    public String getCategoryName_jpn() {
        return this.mCategory_name_jpn;
    }

    public String getCategoryName_eng() {
        return this.mCategory_name_eng;
    }

    public int getCategoryPresetId() {
        return this.mCategory_preset_id;
    }

    public int getPageCnt() {
        return this.mPageCnt;
    }

    public void setCategoryId(int category_id) {
        this.mCategory_id = category_id;
    }

    public void setCategoryName_jpn(String category_name) {
        this.mCategory_name_jpn = category_name;
    }

    public void setCategoryName_eng(String category_name) {
        this.mCategory_name_eng = category_name;
    }

    public void setCategoryPresetId(int category_preset_id) {
        this.mCategory_preset_id = category_preset_id;
    }

    public void setPageCnt(int pagecnt) {
        this.mPageCnt = pagecnt;
    }
}
