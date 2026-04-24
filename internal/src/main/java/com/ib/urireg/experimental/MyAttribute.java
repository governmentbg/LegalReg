package com.ib.urireg.experimental;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

@Target(FIELD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface MyAttribute {
	String label() default "none";
	String defaultText() default "none";
}
