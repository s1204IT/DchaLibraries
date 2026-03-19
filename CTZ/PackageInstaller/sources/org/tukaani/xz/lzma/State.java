package org.tukaani.xz.lzma;

final class State {
    private int state;

    State() {
    }

    void reset() {
        this.state = 0;
    }

    int get() {
        return this.state;
    }

    void updateLiteral() {
        if (this.state <= 3) {
            this.state = 0;
        } else if (this.state <= 9) {
            this.state -= 3;
        } else {
            this.state -= 6;
        }
    }

    void updateMatch() {
        this.state = this.state >= 7 ? 10 : 7;
    }

    void updateLongRep() {
        this.state = this.state < 7 ? 8 : 11;
    }

    void updateShortRep() {
        this.state = this.state < 7 ? 9 : 11;
    }

    boolean isLiteral() {
        return this.state < 7;
    }
}
