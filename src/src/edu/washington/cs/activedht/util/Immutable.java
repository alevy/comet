package edu.washington.cs.activedht.util;


import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for an immutable parameter, field, or local variable.
 * Such a parameter will not change its contents in the current context. 
 * 
 * @author roxana
 */
@Target({FIELD, PARAMETER, LOCAL_VARIABLE})
@Retention(RetentionPolicy.SOURCE)
public @interface Immutable { }

