package uk.ac.diamond.daq.persistence.annotation;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Searchable {
    String value();

    boolean key() default true;

    int priority() default -1;
}
