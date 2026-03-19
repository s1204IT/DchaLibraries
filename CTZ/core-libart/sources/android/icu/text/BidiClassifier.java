package android.icu.text;

public class BidiClassifier {
    protected Object context;

    public BidiClassifier(Object obj) {
        this.context = obj;
    }

    public void setContext(Object obj) {
        this.context = obj;
    }

    public Object getContext() {
        return this.context;
    }

    public int classify(int i) {
        return 23;
    }
}
