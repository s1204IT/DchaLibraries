package org.junit.rules;

import java.util.concurrent.TimeUnit;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class Timeout implements TestRule {
    private final TimeUnit timeUnit;
    private final long timeout;

    public static Builder builder() {
        return new Builder();
    }

    @Deprecated
    public Timeout(int i) {
        this(i, TimeUnit.MILLISECONDS);
    }

    public Timeout(long j, TimeUnit timeUnit) {
        this.timeout = j;
        this.timeUnit = timeUnit;
    }

    protected Timeout(Builder builder) {
        this.timeout = builder.getTimeout();
        this.timeUnit = builder.getTimeUnit();
    }

    public static Timeout millis(long j) {
        return new Timeout(j, TimeUnit.MILLISECONDS);
    }

    public static Timeout seconds(long j) {
        return new Timeout(j, TimeUnit.SECONDS);
    }

    protected final long getTimeout(TimeUnit timeUnit) {
        return timeUnit.convert(this.timeout, this.timeUnit);
    }

    protected Statement createFailOnTimeoutStatement(Statement statement) throws Exception {
        return FailOnTimeout.builder().withTimeout(this.timeout, this.timeUnit).build(statement);
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        try {
            return createFailOnTimeoutStatement(statement);
        } catch (Exception e) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    throw new RuntimeException("Invalid parameters for Timeout", e);
                }
            };
        }
    }

    public static class Builder {
        private boolean lookForStuckThread = false;
        private long timeout = 0;
        private TimeUnit timeUnit = TimeUnit.SECONDS;

        protected Builder() {
        }

        public Builder withTimeout(long j, TimeUnit timeUnit) {
            this.timeout = j;
            this.timeUnit = timeUnit;
            return this;
        }

        protected long getTimeout() {
            return this.timeout;
        }

        protected TimeUnit getTimeUnit() {
            return this.timeUnit;
        }

        public Timeout build() {
            return new Timeout(this);
        }
    }
}
