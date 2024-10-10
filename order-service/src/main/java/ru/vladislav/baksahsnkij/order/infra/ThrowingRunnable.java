package ru.vladislav.baksahsnkij.order.infra;

public interface ThrowingRunnable<E extends Throwable> extends Runnable {
    static <E extends Throwable> ThrowingRunnable<E> throwingRunnable(ThrowingRunnable<E> throwingRunnable) {
        return throwingRunnable;
    }

    void tryRun() throws E;

    @Override
    default void run() {
        try {
            tryRun();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
