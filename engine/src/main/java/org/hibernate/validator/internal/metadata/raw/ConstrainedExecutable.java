/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.internal.metadata.raw;

import static org.hibernate.validator.internal.util.CollectionHelper.newArrayList;
import static org.hibernate.validator.internal.util.CollectionHelper.newHashSet;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.metadata.ConstraintDescriptor;

import org.hibernate.validator.internal.metadata.aggregated.CascadingMetaDataBuilder;
import org.hibernate.validator.internal.metadata.core.MetaConstraint;
import org.hibernate.validator.internal.properties.Callable;
import org.hibernate.validator.internal.util.CollectionHelper;
import org.hibernate.validator.internal.util.logging.Log;
import org.hibernate.validator.internal.util.logging.LoggerFactory;
import org.hibernate.validator.internal.util.stereotypes.Immutable;

/**
 * Represents a method or constructor of a Java type and all its associated
 * meta-data relevant in the context of bean validation, for instance the
 * constraints at its parameters or return value.
 *
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
public class ConstrainedExecutable extends AbstractConstrainedElement {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	private final Callable callable;

	/**
	 * Constrained-related meta data for this executable's parameters.
	 */
	@Immutable
	private final List<ConstrainedParameter> parameterMetaData;

	private final boolean hasParameterConstraints;

	@Immutable
	private final Set<MetaConstraint<?>> crossParameterConstraints;

	/**
	 * Creates a new executable meta data object for a parameter-less executable.
	 *
	 * @param source The source of meta data.
	 * @param callable The represented executable.
	 * @param returnValueConstraints Type arguments constraints, if any.
	 * @param typeArgumentConstraints The type argument constraints on the return value of the represented executable,
	 * if any.
	 * @param cascadingMetaDataBuilder The cascaded validation metadata for this element and its container elements.
	 */
	public ConstrainedExecutable(
			ConfigurationSource source,
			Callable callable,
			Set<MetaConstraint<?>> returnValueConstraints,
			Set<MetaConstraint<?>> typeArgumentConstraints,
			CascadingMetaDataBuilder cascadingMetaDataBuilder) {
		this(
				source,
				callable,
				Collections.<ConstrainedParameter>emptyList(),
				Collections.<MetaConstraint<?>>emptySet(),
				returnValueConstraints,
				typeArgumentConstraints,
				cascadingMetaDataBuilder
		);
	}

	/**
	 * Creates a new executable meta data object.
	 *
	 * @param source The source of meta data.
	 * @param callable The represented executable.
	 * @param parameterMetaData A list with parameter meta data. The length must correspond with the number of
	 * parameters of the represented executable. So this list may be empty (in case of a parameterless executable), but
	 * never {@code null}.
	 * @param crossParameterConstraints the cross parameter constraints
	 * @param returnValueConstraints The return value constraints of the represented executable, if any.
	 * @param typeArgumentConstraints The type argument constraints on the return value of the represented executable,
	 * if any.
	 * @param cascadingMetaDataBuilder The cascaded validation metadata for this element and its container elements.
	 */
	public ConstrainedExecutable(
			ConfigurationSource source,
			Callable callable,
			List<ConstrainedParameter> parameterMetaData,
			Set<MetaConstraint<?>> crossParameterConstraints,
			Set<MetaConstraint<?>> returnValueConstraints,
			Set<MetaConstraint<?>> typeArgumentConstraints,
			CascadingMetaDataBuilder cascadingMetaDataBuilder) {
		super(
				source,
				callable.getConstrainedElementKind(),
				returnValueConstraints,
				typeArgumentConstraints,
				cascadingMetaDataBuilder
		);

		this.callable = callable;

		if ( parameterMetaData.size() != callable.getParameterCount() ) {
			throw LOG.getInvalidLengthOfParameterMetaDataListException(
					callable,
					callable.getParameterCount(),
					parameterMetaData.size()
			);
		}

		this.crossParameterConstraints = CollectionHelper.toImmutableSet( crossParameterConstraints );
		this.parameterMetaData = CollectionHelper.toImmutableList( parameterMetaData );
		this.hasParameterConstraints = hasParameterConstraints( parameterMetaData ) || !crossParameterConstraints.isEmpty();
	}

	/**
	 * Constraint meta data for the specified parameter.
	 *
	 * @param parameterIndex The index in this executable's parameter array of the parameter of
	 * interest.
	 *
	 * @return Meta data for the specified parameter. Will never be {@code null}.
	 *
	 * @throws IllegalArgumentException In case this executable doesn't have a parameter with the
	 * specified index.
	 */
	public ConstrainedParameter getParameterMetaData(int parameterIndex) {
		if ( parameterIndex < 0 || parameterIndex > parameterMetaData.size() - 1 ) {
			throw LOG.getInvalidExecutableParameterIndexException(
					callable,
					parameterIndex
			);
		}

		return parameterMetaData.get( parameterIndex );
	}

	/**
	 * Returns meta data for all parameters of the represented executable.
	 *
	 * @return A list with parameter meta data. The length corresponds to the
	 *         number of parameters of the executable represented by this meta data
	 *         object, so an empty list may be returned (in case of a
	 *         parameterless executable), but never {@code null}.
	 */
	public List<ConstrainedParameter> getAllParameterMetaData() {
		return parameterMetaData;
	}

	public Set<MetaConstraint<?>> getCrossParameterConstraints() {
		return crossParameterConstraints;
	}

	/**
	 * Whether the represented executable is constrained or not. This is the case if
	 * it has at least one constrained parameter, at least one parameter marked
	 * for cascaded validation, at least one cross-parameter constraint, at
	 * least one return value constraint or if the return value is marked for
	 * cascaded validation.
	 *
	 * @return {@code True} if this executable is constrained by any means,
	 *         {@code false} otherwise.
	 */
	@Override
	public boolean isConstrained() {
		return super.isConstrained() || hasParameterConstraints;
	}

	/**
	 * Whether this executable has at least one cascaded parameter or at least one
	 * parameter with constraints or at least one cross-parameter constraint.
	 *
	 * @return {@code True}, if this executable is parameter-constrained by any
	 *         means, {@code false} otherwise.
	 */
	public boolean hasParameterConstraints() {
		return hasParameterConstraints;
	}

	public Callable getCallable() {
		return callable;
	}

	@Override
	public String toString() {
		return "ConstrainedExecutable [executable=" + callable
				+ ", parameterMetaData=" + parameterMetaData
				+ ", hasParameterConstraints=" + hasParameterConstraints + "]";
	}

	private boolean hasParameterConstraints(List<ConstrainedParameter> parameterMetaData) {
		for ( ConstrainedParameter oneParameter : parameterMetaData ) {
			if ( oneParameter.isConstrained() ) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Whether this and the given other executable have the same parameter
	 * constraints.
	 *
	 * @param other The other executable to check.
	 *
	 * @return True if this and the other executable have the same parameter
	 *         constraints (including cross- parameter constraints and parameter
	 *         cascades), false otherwise.
	 */
	public boolean isEquallyParameterConstrained(ConstrainedExecutable other) {
		if ( !getDescriptors( crossParameterConstraints ).equals( getDescriptors( other.crossParameterConstraints ) ) ) {
			return false;
		}

		int i = 0;
		for ( ConstrainedParameter parameter : parameterMetaData ) {
			ConstrainedParameter otherParameter = other.getParameterMetaData( i );
			// FIXME: how to deal with method overriding with type overloading of one of the parameters?
			if ( !parameter.getCascadingMetaDataBuilder().equals( otherParameter.getCascadingMetaDataBuilder() )
					|| !getDescriptors( parameter.getConstraints() ).equals( getDescriptors( otherParameter.getConstraints() ) ) ) {
				return false;
			}
			i++;
		}

		return true;
	}

	/**
	 * Creates a new constrained executable object by merging this and the given
	 * other executable. Both executables must have the same location, i.e.
	 * represent the same executable on the same type.
	 *
	 * @param other The executable to merge.
	 *
	 * @return A merged executable.
	 */
	public ConstrainedExecutable merge(ConstrainedExecutable other) {
		ConfigurationSource mergedSource = ConfigurationSource.max( source, other.source );

		List<ConstrainedParameter> mergedParameterMetaData = newArrayList( parameterMetaData.size() );
		int i = 0;
		for ( ConstrainedParameter parameter : parameterMetaData ) {
			mergedParameterMetaData.add( parameter.merge( other.getParameterMetaData( i ) ) );
			i++;
		}

		Set<MetaConstraint<?>> mergedCrossParameterConstraints = newHashSet( crossParameterConstraints );
		mergedCrossParameterConstraints.addAll( other.crossParameterConstraints );

		Set<MetaConstraint<?>> mergedReturnValueConstraints = newHashSet( constraints );
		mergedReturnValueConstraints.addAll( other.constraints );

		Set<MetaConstraint<?>> mergedTypeArgumentConstraints = new HashSet<>( typeArgumentConstraints );
		mergedTypeArgumentConstraints.addAll( other.typeArgumentConstraints );

		CascadingMetaDataBuilder mergedCascadingMetaDataBuilder = cascadingMetaDataBuilder.merge( other.cascadingMetaDataBuilder );

		return new ConstrainedExecutable(
				mergedSource,
				callable,
				mergedParameterMetaData,
				mergedCrossParameterConstraints,
				mergedReturnValueConstraints,
				mergedTypeArgumentConstraints,
				mergedCascadingMetaDataBuilder
		);
	}

	private Set<ConstraintDescriptor<?>> getDescriptors(Iterable<MetaConstraint<?>> constraints) {
		Set<ConstraintDescriptor<?>> descriptors = newHashSet();

		for ( MetaConstraint<?> constraint : constraints ) {
			descriptors.add( constraint.getDescriptor() );
		}

		return descriptors;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + callable.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals( obj ) ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		ConstrainedExecutable other = (ConstrainedExecutable) obj;
		if ( !callable.equals( other.callable ) ) {
			return false;
		}
		return true;
	}
}
