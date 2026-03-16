package jp.co.omronsoft.iwnnime.ml.decoemoji;

import jp.co.omronsoft.android.decoemojimanager.interfacedata.DecoEmojiAttrInfo;

public class DecoEmojiOperation {
    private DecoEmojiAttrInfo mDecoEmojiAttrInfo;
    private boolean mIsUpdatePreferenceId;
    private int mType;

    public DecoEmojiOperation(DecoEmojiAttrInfo decoemojiattrinfo, int type, boolean isUpdatePreferenceId) {
        this.mDecoEmojiAttrInfo = decoemojiattrinfo;
        this.mType = type;
        this.mIsUpdatePreferenceId = isUpdatePreferenceId;
    }

    public DecoEmojiAttrInfo[] getDecoEmojiAttrInfo() {
        return new DecoEmojiAttrInfo[]{this.mDecoEmojiAttrInfo};
    }

    public int getType() {
        return this.mType;
    }

    public boolean isUpdatePreferenceId() {
        return this.mIsUpdatePreferenceId;
    }
}
