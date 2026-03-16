package com.android.okhttp;

public final class Failure {
    private final Throwable exception;
    private final Request request;

    private Failure(Builder builder) {
        this.request = builder.request;
        this.exception = builder.exception;
    }

    public Request request() {
        return this.request;
    }

    public Throwable exception() {
        return this.exception;
    }

    public static class Builder {
        private Throwable exception;
        private Request request;

        public Builder request(Request request) {
            this.request = request;
            return this;
        }

        public Builder exception(Throwable exception) {
            this.exception = exception;
            return this;
        }

        public Failure build() {
            return new Failure(this);
        }
    }
}
