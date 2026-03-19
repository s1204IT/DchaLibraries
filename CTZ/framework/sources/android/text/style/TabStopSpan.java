package android.text.style;

public interface TabStopSpan extends ParagraphStyle {
    int getTabStop();

    public static class Standard implements TabStopSpan {
        private int mTabOffset;

        public Standard(int i) {
            this.mTabOffset = i;
        }

        @Override
        public int getTabStop() {
            return this.mTabOffset;
        }
    }
}
