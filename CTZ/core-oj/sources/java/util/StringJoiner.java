package java.util;

public final class StringJoiner {
    private final String delimiter;
    private String emptyValue;
    private final String prefix;
    private final String suffix;
    private StringBuilder value;

    public StringJoiner(CharSequence charSequence) {
        this(charSequence, "", "");
    }

    public StringJoiner(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        Objects.requireNonNull(charSequence2, "The prefix must not be null");
        Objects.requireNonNull(charSequence, "The delimiter must not be null");
        Objects.requireNonNull(charSequence3, "The suffix must not be null");
        this.prefix = charSequence2.toString();
        this.delimiter = charSequence.toString();
        this.suffix = charSequence3.toString();
        this.emptyValue = this.prefix + this.suffix;
    }

    public StringJoiner setEmptyValue(CharSequence charSequence) {
        this.emptyValue = ((CharSequence) Objects.requireNonNull(charSequence, "The empty value must not be null")).toString();
        return this;
    }

    public String toString() {
        if (this.value == null) {
            return this.emptyValue;
        }
        if (this.suffix.equals("")) {
            return this.value.toString();
        }
        int length = this.value.length();
        StringBuilder sb = this.value;
        sb.append(this.suffix);
        String string = sb.toString();
        this.value.setLength(length);
        return string;
    }

    public StringJoiner add(CharSequence charSequence) {
        prepareBuilder().append(charSequence);
        return this;
    }

    public StringJoiner merge(StringJoiner stringJoiner) {
        Objects.requireNonNull(stringJoiner);
        if (stringJoiner.value != null) {
            prepareBuilder().append((CharSequence) stringJoiner.value, stringJoiner.prefix.length(), stringJoiner.value.length());
        }
        return this;
    }

    private StringBuilder prepareBuilder() {
        if (this.value != null) {
            this.value.append(this.delimiter);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(this.prefix);
            this.value = sb;
        }
        return this.value;
    }

    public int length() {
        return this.value != null ? this.value.length() + this.suffix.length() : this.emptyValue.length();
    }
}
