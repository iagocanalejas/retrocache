package com.andiag.retrocache.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by IagoCanalejas on 02/02/2017.
 * Annotation used to avoid a {@link retrofit2.http.GET} method to be cached.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Caching {

    boolean enabled() default true;

}
