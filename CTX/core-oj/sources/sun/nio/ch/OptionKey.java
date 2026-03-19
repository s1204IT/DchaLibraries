package sun.nio.ch;

class OptionKey {
    private int level;
    private int name;

    OptionKey(int i, int i2) {
        this.level = i;
        this.name = i2;
    }

    int level() {
        return this.level;
    }

    int name() {
        return this.name;
    }
}
