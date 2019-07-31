package uk.ac.diamond.daq.peristence.annotation;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Listable {
    String value ();

    int priority () default -1;
}
