package io.github.kugin.reconciliation.before;

import io.github.kugin.reconciliation.domain.CheckEntry;

import java.util.List;

/**
 * @author Kugin
 */
@FunctionalInterface
public interface ResourceLoader {
    List<CheckEntry> load(String date);
}