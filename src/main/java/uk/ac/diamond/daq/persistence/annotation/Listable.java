package uk.ac.diamond.daq.persistence.annotation;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Listable {
    String value();

    boolean key() default true;

    int priority() default -1;
}
