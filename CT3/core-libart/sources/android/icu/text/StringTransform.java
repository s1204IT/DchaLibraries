package android.icu.text;

public interface StringTransform extends Transform<String, String> {
    @Override
    String transform(String str);
}
