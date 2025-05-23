/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.test.internal.engine.methodvalidation.service;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Cross-parameter constraint that checks that two date parameters are in the
 * correct order. Applies to methods with the signature
 * {@code methodName(DateMidnight start, DateMidnight end)}.
 *
 * @author Gunnar Morling
 */
@Target({ METHOD, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConsistentDateParametersValidator.class)
@Documented
public @interface ConsistentDateParameters {
	String message() default "{ConsistentDateParameters.message}";

	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default { };
}
