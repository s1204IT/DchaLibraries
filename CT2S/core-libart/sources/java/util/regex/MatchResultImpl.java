package java.util.regex;

class MatchResultImpl implements MatchResult {
    private int[] offsets;
    private String text;

    MatchResultImpl(String text, int[] offsets) {
        this.text = text;
        this.offsets = (int[]) offsets.clone();
    }

    @Override
    public int end() {
        return end(0);
    }

    @Override
    public int end(int group) {
        return this.offsets[(group * 2) + 1];
    }

    @Override
    public String group() {
        return this.text.substring(start(), end());
    }

    @Override
    public String group(int group) {
        int from = this.offsets[group * 2];
        int to = this.offsets[(group * 2) + 1];
        if (from == -1 || to == -1) {
            return null;
        }
        return this.text.substring(from, to);
    }

    @Override
    public int groupCount() {
        return (this.offsets.length / 2) - 1;
    }

    @Override
    public int start() {
        return start(0);
    }

    @Override
    public int start(int group) {
        return this.offsets[group * 2];
    }
}
