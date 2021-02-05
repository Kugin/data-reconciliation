package io.github.kugin.reconciliation.before;

/**
 * @author Kugin
 */
@FunctionalInterface
public interface CheckPre {
    boolean check(String date);
}
