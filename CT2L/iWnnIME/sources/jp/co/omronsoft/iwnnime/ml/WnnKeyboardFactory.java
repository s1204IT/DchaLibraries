package jp.co.omronsoft.iwnnime.ml;

public class WnnKeyboardFactory {
    private static final boolean DEBUG = false;
    private Keyboard mKeyboard = null;
    private IWnnIME mParent;
    private int mResourceId;

    public WnnKeyboardFactory(IWnnIME parent, int resourceId) {
        this.mParent = null;
        this.mResourceId = resourceId;
        this.mParent = parent;
    }

    public Keyboard getKeyboard(int mode, int condition) {
        return getKeyboard(mode, 0, condition);
    }

    public Keyboard getKeyboard(int mode, int keyboardType, int condition) {
        if (this.mKeyboard == null) {
            this.mKeyboard = new Keyboard(this.mParent, this.mResourceId, mode, keyboardType, condition);
        }
        return this.mKeyboard;
    }
}
