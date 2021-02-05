package io.github.kugin.reconciliation;

import io.github.kugin.reconciliation.domain.CheckAdapter;
import io.github.kugin.reconciliation.domain.CheckEntry;
import io.github.kugin.reconciliation.entry.TestA;
import io.github.kugin.reconciliation.entry.TestB;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author Kugin
 */
public class CheckEntryTest {
    @Test
    public void testAnnotation() {
        TestA a = new TestA("a", "etst", 13d);
        TestB b = new TestB("b", "tee", 13d);
        CheckEntry entryA = CheckEntry.wrap(a, "name", Arrays.asList("amount"));
        CheckEntry entryB = CheckEntry.wrap(b);
        Assert.assertNotNull(entryA.getCheckData());
        Assert.assertNotNull(entryB.getCheckData());
    }

    @Test
    public void testAdapter() {
        TestA a = new TestA("a", "etst", 13d);
        TestB b = new TestB("b", "tee", 13d);

        List<CheckAdapter> list = Arrays.asList(a, b);
        list.forEach(e -> {
            System.out.println(e.getKey());
            System.out.println(e.getCheckData());
        });

        CheckEntry entryA = new CheckEntry(a);
        CheckEntry entryB = new CheckEntry(b);
        Assert.assertNotNull(entryA.getCheckData());
    }
}