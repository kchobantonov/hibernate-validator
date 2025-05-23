/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.internal.constraintvalidators.bv.time.future;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Check that the {@code java.time.LocalDateTime} passed is in the future.
 *
 * @author Guillaume Smet
 */
public class FutureValidatorForLocalDateTime extends AbstractFutureJavaTimeValidator<LocalDateTime> {

	@Override
	protected LocalDateTime getReferenceValue(Clock reference) {
		return LocalDateTime.now( reference );
	}

}
