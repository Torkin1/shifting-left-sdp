package it.torkin.dataminer.rest.parsing;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy; 

/**
 * Fields annotated with @Hide will be ignored
 * by GSON library
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Hide {}