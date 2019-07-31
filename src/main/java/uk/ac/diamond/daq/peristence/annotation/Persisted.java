package uk.ac.diamond.daq.peristence.annotation;

import java.lang.annotation.*;

@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Persisted {
    boolean key() default false;
}
