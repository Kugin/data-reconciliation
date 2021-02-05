package io.github.kugin.reconciliation.annotation;

import java.lang.annotation.*;

/**
 * @author Kugin
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckIdentity {
    String name() default "";
}
