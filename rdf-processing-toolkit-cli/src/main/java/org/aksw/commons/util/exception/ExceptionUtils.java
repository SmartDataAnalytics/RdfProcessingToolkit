package org.aksw.commons.util.exception;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ExceptionUtils {
    public static boolean isBrokenPipeException(Throwable t) {
        String str = org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage(t);
        boolean result = str != null && str.toLowerCase().contains("broken pipe");
        return result;
    }

    public static void rethrowIfNotBrokenPipe(Throwable t) {
        rethrowUnless(t, ExceptionUtils::isBrokenPipeException);
    }

    /**
     * Utility function to rethrow an exception unless a condition is met; in that case
     * it is silently swallowed.
     *
     * <pre>
     * rethrowUnless(e, ExceptionUtils::isBrokenPipeException, Foo:isIoException)
     * </pre>
     *
     * @param <T>
     * @param t
     * @param predicates
     */
    @SafeVarargs
    public static <T extends Throwable> void rethrowUnless(T t, Predicate<? super T> ... predicates) {
        boolean anyMatch = Arrays.asList(predicates).stream()
                .anyMatch(p -> p.test(t));

        if(!anyMatch) {
            throw new RuntimeException(t);
        }
    }

    @SafeVarargs
    public static <T extends Throwable> void forwardRootCauseUnless(T t, Consumer<? super Throwable> handler, Predicate<? super T> ... predicates) {
        boolean anyMatch = Arrays.asList(predicates).stream()
                .anyMatch(p -> p.test(t));

        if(!anyMatch) {
            Throwable rootCause = org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(t);
            handler.accept(rootCause);
        }
    }

    @SafeVarargs
    public static <T extends Throwable> void forwardRootCauseMessageUnless(T t, Consumer<? super String> handler, Predicate<? super T> ... predicates) {
        boolean anyMatch = Arrays.asList(predicates).stream()
                .anyMatch(p -> p.test(t));

        if(!anyMatch) {
            String rootCauseMsg = org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage(t);
            handler.accept(rootCauseMsg);
        }
    }

}
