/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.internal.constraintvalidators.bv.number.sign;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Positive;

import org.hibernate.validator.internal.constraintvalidators.bv.number.InfinityNumberComparatorHelper;

/**
 * Check that the number being validated is positive.
 *
 * @author Hardy Ferentschik
 * @author Xavier Sosnovsky
 * @author Guillaume Smet
 * @author Marko Bekhta
 */
public class PositiveValidatorForFloat implements ConstraintValidator<Positive, Float> {

	@Override
	public boolean isValid(Float value, ConstraintValidatorContext context) {
		// null values are valid
		if ( value == null ) {
			return true;
		}

		return NumberSignHelper.signum( value, InfinityNumberComparatorHelper.LESS_THAN ) > 0;
	}
}
