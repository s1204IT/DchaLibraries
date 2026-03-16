package jp.co.omronsoft.android.text;

class EmojiCacheKey {
    private static final boolean DEBUG_EMOJI = false;
    private static final String TAG = "EmojiDrawable";
    public int first;
    public int second;
    public String uri;

    public EmojiCacheKey() {
        this(0, 0);
    }

    public EmojiCacheKey(int code, int fontLevel) {
    }

    public EmojiCacheKey(String uri) {
    }

    public void setData(int code, int fontLevel) {
    }

    public void setData(String uri) {
    }

    public int hashCode() {
        return 0;
    }

    public boolean equals(Object obj) {
        return DEBUG_EMOJI;
    }
}
