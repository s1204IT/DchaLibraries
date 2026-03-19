package java.util.stream;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Spliterator;
import java.util.concurrent.CountedCompleter;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.FindOps;
import java.util.stream.Sink;

final class FindOps {
    private FindOps() {
    }

    public static <T> TerminalOp<T, Optional<T>> makeRef(boolean z) {
        return new FindOp(z, StreamShape.REFERENCE, Optional.empty(), new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((Optional) obj).isPresent();
            }
        }, new Supplier() {
            @Override
            public final Object get() {
                return new FindOps.FindSink.OfRef();
            }
        });
    }

    public static TerminalOp<Integer, OptionalInt> makeInt(boolean z) {
        return new FindOp(z, StreamShape.INT_VALUE, OptionalInt.empty(), new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((OptionalInt) obj).isPresent();
            }
        }, new Supplier() {
            @Override
            public final Object get() {
                return new FindOps.FindSink.OfInt();
            }
        });
    }

    public static TerminalOp<Long, OptionalLong> makeLong(boolean z) {
        return new FindOp(z, StreamShape.LONG_VALUE, OptionalLong.empty(), new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((OptionalLong) obj).isPresent();
            }
        }, new Supplier() {
            @Override
            public final Object get() {
                return new FindOps.FindSink.OfLong();
            }
        });
    }

    public static TerminalOp<Double, OptionalDouble> makeDouble(boolean z) {
        return new FindOp(z, StreamShape.DOUBLE_VALUE, OptionalDouble.empty(), new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((OptionalDouble) obj).isPresent();
            }
        }, new Supplier() {
            @Override
            public final Object get() {
                return new FindOps.FindSink.OfDouble();
            }
        });
    }

    private static final class FindOp<T, O> implements TerminalOp<T, O> {
        final O emptyValue;
        final boolean mustFindFirst;
        final Predicate<O> presentPredicate;
        private final StreamShape shape;
        final Supplier<TerminalSink<T, O>> sinkSupplier;

        FindOp(boolean z, StreamShape streamShape, O o, Predicate<O> predicate, Supplier<TerminalSink<T, O>> supplier) {
            this.mustFindFirst = z;
            this.shape = streamShape;
            this.emptyValue = o;
            this.presentPredicate = predicate;
            this.sinkSupplier = supplier;
        }

        @Override
        public int getOpFlags() {
            return StreamOpFlag.IS_SHORT_CIRCUIT | (this.mustFindFirst ? 0 : StreamOpFlag.NOT_ORDERED);
        }

        @Override
        public StreamShape inputShape() {
            return this.shape;
        }

        @Override
        public <S> O evaluateSequential(PipelineHelper<T> pipelineHelper, Spliterator<S> spliterator) {
            O o = (O) ((TerminalSink) pipelineHelper.wrapAndCopyInto(this.sinkSupplier.get(), spliterator)).get();
            return o != null ? o : this.emptyValue;
        }

        @Override
        public <P_IN> O evaluateParallel(PipelineHelper<T> pipelineHelper, Spliterator<P_IN> spliterator) {
            return new FindTask(this, pipelineHelper, spliterator).invoke();
        }
    }

    private static abstract class FindSink<T, O> implements TerminalSink<T, O> {
        boolean hasValue;
        T value;

        FindSink() {
        }

        @Override
        public void accept(T t) {
            if (!this.hasValue) {
                this.hasValue = true;
                this.value = t;
            }
        }

        @Override
        public boolean cancellationRequested() {
            return this.hasValue;
        }

        static final class OfRef<T> extends FindSink<T, Optional<T>> {
            OfRef() {
            }

            @Override
            public Optional<T> get() {
                if (this.hasValue) {
                    return Optional.of(this.value);
                }
                return null;
            }
        }

        static final class OfInt extends FindSink<Integer, OptionalInt> implements Sink.OfInt {
            OfInt() {
            }

            @Override
            public void accept(Integer num) {
                super.accept(num);
            }

            @Override
            public void accept(int i) {
                accept(Integer.valueOf(i));
            }

            @Override
            public OptionalInt get() {
                if (this.hasValue) {
                    return OptionalInt.of(((Integer) this.value).intValue());
                }
                return null;
            }
        }

        static final class OfLong extends FindSink<Long, OptionalLong> implements Sink.OfLong {
            OfLong() {
            }

            @Override
            public void accept(Long l) {
                super.accept(l);
            }

            @Override
            public void accept(long j) {
                accept(Long.valueOf(j));
            }

            @Override
            public OptionalLong get() {
                if (this.hasValue) {
                    return OptionalLong.of(((Long) this.value).longValue());
                }
                return null;
            }
        }

        static final class OfDouble extends FindSink<Double, OptionalDouble> implements Sink.OfDouble {
            OfDouble() {
            }

            @Override
            public void accept(Double d) {
                super.accept(d);
            }

            @Override
            public void accept(double d) {
                accept(Double.valueOf(d));
            }

            @Override
            public OptionalDouble get() {
                if (this.hasValue) {
                    return OptionalDouble.of(((Double) this.value).doubleValue());
                }
                return null;
            }
        }
    }

    private static final class FindTask<P_IN, P_OUT, O> extends AbstractShortCircuitTask<P_IN, P_OUT, O, FindTask<P_IN, P_OUT, O>> {
        private final FindOp<P_OUT, O> op;

        FindTask(FindOp<P_OUT, O> findOp, PipelineHelper<P_OUT> pipelineHelper, Spliterator<P_IN> spliterator) {
            super(pipelineHelper, spliterator);
            this.op = findOp;
        }

        FindTask(FindTask<P_IN, P_OUT, O> findTask, Spliterator<P_IN> spliterator) {
            super(findTask, spliterator);
            this.op = findTask.op;
        }

        @Override
        protected FindTask<P_IN, P_OUT, O> makeChild(Spliterator<P_IN> spliterator) {
            return new FindTask<>(this, spliterator);
        }

        @Override
        protected O getEmptyResult() {
            return this.op.emptyValue;
        }

        private void foundResult(O o) {
            if (isLeftmostNode()) {
                shortCircuit(o);
            } else {
                cancelLaterNodes();
            }
        }

        @Override
        protected O doLeaf() {
            O o = (O) ((TerminalSink) this.helper.wrapAndCopyInto(this.op.sinkSupplier.get(), this.spliterator)).get();
            if (!this.op.mustFindFirst) {
                if (o != null) {
                    shortCircuit(o);
                }
                return null;
            }
            if (o == null) {
                return null;
            }
            foundResult(o);
            return o;
        }

        @Override
        public void onCompletion(CountedCompleter<?> countedCompleter) {
            if (this.op.mustFindFirst) {
                FindTask findTask = (FindTask) this.leftChild;
                FindTask findTask2 = null;
                while (true) {
                    FindTask findTask3 = findTask2;
                    findTask2 = findTask;
                    if (findTask2 != findTask3) {
                        O localResult = findTask2.getLocalResult();
                        if (localResult == null || !this.op.presentPredicate.test(localResult)) {
                            findTask = (FindTask) this.rightChild;
                        } else {
                            setLocalResult(localResult);
                            foundResult(localResult);
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            super.onCompletion(countedCompleter);
        }
    }
}
