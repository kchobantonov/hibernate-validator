/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.cdi.interceptor.spi;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;

import org.hibernate.validator.cdi.interceptor.internal.MethodValidated;

/**
 * An interceptor which performs a validation of the Bean Validation constraints specified at the parameters and/or return
 * values of intercepted methods using the method validation functionality provided by Hibernate Validator.
 *
 * @author Gunnar Morling
 * @author Hardy Ferentschik
 */
@MethodValidated
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_AFTER + 800)
public class ValidationInterceptor implements Serializable {

	private static final long serialVersionUID = 604440259030722151L;

	/**
	 * The validator to be used for method validation.
	 * <p>
	 * Although the concrete validator is not necessarily serializable (and HV's implementation indeed isn't) it is still
	 * alright to have it as non-transient field here. Upon passivation not the validator itself will be serialized, but the
	 * proxy injected here, which in turn is serializable.
	 * </p>
	 */
	@Inject
	private Validator validator;

	/**
	 * Validates the Bean Validation constraints specified at the parameters and/or return value of the intercepted method.
	 *
	 * @param ctx The context of the intercepted method invocation.
	 *
	 * @return The result of the method invocation.
	 *
	 * @throws Exception Any exception caused by the intercepted method invocation. A {@link ConstraintViolationException}
	 * in case at least one constraint violation occurred either during parameter or return value validation.
	 */
	@AroundInvoke
	public Object validateMethodInvocation(InvocationContext ctx) throws Exception {
		ExecutableValidator executableValidator = validator.forExecutables();
		Set<ConstraintViolation<Object>> violations = executableValidator.validateParameters(
				ctx.getTarget(),
				ctx.getMethod(),
				ctx.getParameters()
		);

		if ( !violations.isEmpty() ) {
			throw new ConstraintViolationException(
					getMessage( ctx.getMethod(), ctx.getParameters(), violations ),
					violations
			);
		}

		Object result = ctx.proceed();

		violations = executableValidator.validateReturnValue(
				ctx.getTarget(),
				ctx.getMethod(),
				result
		);

		if ( !violations.isEmpty() ) {
			throw new ConstraintViolationException(
					getMessage( ctx.getMethod(), ctx.getParameters(), violations ),
					violations
			);
		}

		return result;
	}

	/**
	 * Validates the Bean Validation constraints specified at the parameters and/or return value of the intercepted constructor.
	 *
	 * @param ctx The context of the intercepted constructor invocation.
	 *
	 * @throws Exception Any exception caused by the intercepted constructor invocation. A {@link ConstraintViolationException}
	 * in case at least one constraint violation occurred either during parameter or return value validation.
	 */
	@AroundConstruct
	public void validateConstructorInvocation(InvocationContext ctx) throws Exception {
		ExecutableValidator executableValidator = validator.forExecutables();
		Set<? extends ConstraintViolation<?>> violations = executableValidator.validateConstructorParameters(
				ctx.getConstructor(),
				ctx.getParameters()
		);

		if ( !violations.isEmpty() ) {
			throw new ConstraintViolationException(
					getMessage( ctx.getConstructor(), ctx.getParameters(), violations ),
					violations
			);
		}

		ctx.proceed();
		Object createdObject = ctx.getTarget();

		violations = validator.forExecutables().validateConstructorReturnValue(
				ctx.getConstructor(),
				createdObject
		);

		if ( !violations.isEmpty() ) {
			throw new ConstraintViolationException(
					getMessage( ctx.getConstructor(), ctx.getParameters(), violations ),
					violations
			);
		}
	}

	private String getMessage(Member member, Object[] args, Set<? extends ConstraintViolation<?>> violations) {

		StringBuilder message = new StringBuilder();
		message.append( violations.size() );
		message.append( " constraint violation(s) occurred during method validation." );
		message.append( "\nConstructor or Method: " );
		message.append( member );
		message.append( "\nArgument values: " );
		message.append( Arrays.toString( args ) );
		message.append( "\nConstraint violations: " );

		int i = 1;
		for ( ConstraintViolation<?> constraintViolation : violations ) {
			Path.Node leafNode = getLeafNode( constraintViolation );

			message.append( "\n (" );
			message.append( i );
			message.append( ")" );
			message.append( " Kind: " );
			message.append( leafNode.getKind() );
			if ( leafNode.getKind() == ElementKind.PARAMETER ) {
				message.append( "\n parameter index: " );
				message.append( leafNode.as( Path.ParameterNode.class ).getParameterIndex() );
			}
			message.append( "\n message: " );
			message.append( constraintViolation.getMessage() );
			message.append( "\n root bean: " );
			message.append( constraintViolation.getRootBean() );
			message.append( "\n property path: " );
			message.append( constraintViolation.getPropertyPath() );
			message.append( "\n constraint: " );
			message.append( constraintViolation.getConstraintDescriptor().getAnnotation() );

			i++;
		}

		return message.toString();
	}

	private Path.Node getLeafNode(ConstraintViolation<?> constraintViolation) {
		Iterator<Path.Node> nodes = constraintViolation.getPropertyPath().iterator();
		Path.Node leafNode = null;
		while ( nodes.hasNext() ) {
			leafNode = nodes.next();
		}
		return leafNode;
	}
}
