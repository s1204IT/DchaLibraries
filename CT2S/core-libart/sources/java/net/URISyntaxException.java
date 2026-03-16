package java.net;

public class URISyntaxException extends Exception {
    private static final long serialVersionUID = 2137979680897488891L;
    private int index;
    private String input;

    public URISyntaxException(String input, String reason, int index) {
        super(reason);
        if (input == null) {
            throw new NullPointerException("input == null");
        }
        if (reason == null) {
            throw new NullPointerException("reason == null");
        }
        if (index < -1) {
            throw new IllegalArgumentException("Bad index: " + index);
        }
        this.input = input;
        this.index = index;
    }

    public URISyntaxException(String input, String reason) {
        super(reason);
        if (input == null) {
            throw new NullPointerException("input == null");
        }
        if (reason == null) {
            throw new NullPointerException("reason == null");
        }
        this.input = input;
        this.index = -1;
    }

    public int getIndex() {
        return this.index;
    }

    public String getReason() {
        return super.getMessage();
    }

    public String getInput() {
        return this.input;
    }

    @Override
    public String getMessage() {
        String reason = super.getMessage();
        return this.index != -1 ? reason + " at index " + this.index + ": " + this.input : reason + ": " + this.input;
    }
}
