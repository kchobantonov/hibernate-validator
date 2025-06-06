/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.ap.internal.checks;

import static javax.lang.model.util.ElementFilter.methodsIn;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import javax.lang.model.util.TypeKindVisitor8;
import javax.lang.model.util.Types;

import org.hibernate.validator.ap.internal.util.AnnotationApiHelper;
import org.hibernate.validator.ap.internal.util.CollectionHelper;
import org.hibernate.validator.ap.internal.util.TypeNames.BeanValidationTypes;

/**
 * Checks, that each constraint annotation type declares the members message(), groups() and payload() as
 * defined by the BV spec.
 *
 * @author Gunnar Morling
 */
public class AnnotationTypeMemberCheck extends AbstractConstraintCheck {

	private final AnnotationApiHelper annotationApiHelper;

	private final Types typeUtils;

	public AnnotationTypeMemberCheck(AnnotationApiHelper annotationApiHelper, Types typeUtils) {

		this.annotationApiHelper = annotationApiHelper;
		this.typeUtils = typeUtils;
	}

	@Override
	public Set<ConstraintCheckIssue> checkAnnotationType(TypeElement element, AnnotationMirror annotation) {

		Set<ConstraintCheckIssue> theValue = CollectionHelper.newHashSet();

		theValue.addAll( checkMessageAttribute( element ) );
		theValue.addAll( checkGroupsAttribute( element ) );
		theValue.addAll( checkPayloadAttribute( element ) );

		return theValue;
	}

	/**
	 * Checks that the given type element
	 * <p/>
	 * <ul>
	 * <li>has a method with name "message",</li>
	 * <li>the return type of this method is {@link String}.</li>
	 * </ul>
	 *
	 * @param element The element of interest.
	 *
	 * @return A possibly non-empty set of constraint check errors, never null.
	 */
	private Set<ConstraintCheckIssue> checkMessageAttribute(TypeElement element) {

		ExecutableElement messageMethod = getMethod( element, "message" );

		if ( messageMethod == null ) {
			return CollectionHelper.asSet(
					ConstraintCheckIssue.error( element, null, "CONSTRAINT_TYPE_MUST_DECLARE_MESSAGE_MEMBER" )
			);
		}

		if ( !typeUtils.isSameType(
				annotationApiHelper.getMirrorForType( String.class ), messageMethod.getReturnType()
		) ) {
			return CollectionHelper.asSet(
					ConstraintCheckIssue.error( messageMethod, null, "RETURN_TYPE_MUST_BE_STRING" )
			);
		}

		return Collections.emptySet();
	}

	/**
	 * Checks that the given type element
	 * <p/>
	 * <ul>
	 * <li>has a method with name "groups",</li>
	 * <li>the return type of this method is {@code Class&lt;?&gt;[]},</li>
	 * <li>the default value of this method is {@code {}}.</li>
	 * </ul>
	 *
	 * @param element The element of interest.
	 *
	 * @return A possibly non-empty set of constraint check errors, never null.
	 */
	private Set<ConstraintCheckIssue> checkGroupsAttribute(TypeElement element) {

		ExecutableElement groupsMethod = getMethod( element, "groups" );

		if ( groupsMethod == null ) {
			return CollectionHelper.asSet(
					ConstraintCheckIssue.error( element, null, "CONSTRAINT_TYPE_MUST_DECLARE_GROUPS_MEMBER" )
			);
		}

		DeclaredType type = getComponentTypeOfArrayReturnType( groupsMethod );

		if ( type == null ) {
			return CollectionHelper.asSet(
					ConstraintCheckIssue.error( groupsMethod, null, "RETURN_TYPE_MUST_BE_CLASS_ARRAY" )
			);
		}

		boolean typeHasNameClass = type.asElement().getSimpleName().contentEquals( "Class" );
		boolean typeHasExactlyOneTypeArgument = type.getTypeArguments().size() == 1;
		boolean typeArgumentIsUnboundWildcard = validateWildcardBounds( type.getTypeArguments().get( 0 ), null, null );

		if ( !( typeHasNameClass && typeHasExactlyOneTypeArgument && typeArgumentIsUnboundWildcard ) ) {
			return CollectionHelper.asSet(
					ConstraintCheckIssue.error( groupsMethod, null, "RETURN_TYPE_MUST_BE_CLASS_ARRAY" )
			);
		}

		if ( !isEmptyArray( groupsMethod.getDefaultValue() ) ) {
			return CollectionHelper.asSet(
					ConstraintCheckIssue.error( groupsMethod, null, "DEFAULT_VALUE_MUST_BE_EMPTY_ARRAY" )
			);
		}

		return Collections.emptySet();
	}

	/**
	 * Checks that the given type element
	 * <p/>
	 * <ul>
	 * <li>has a method with name "payload",</li>
	 * <li>the return type of this method is {@code Class&lt;? extends Payload&gt;[]},</li>
	 * <li>the default value of this method is {@code {}}.</li>
	 * </ul>
	 *
	 * @param element The element of interest.
	 *
	 * @return A possibly non-empty set of constraint check errors, never null.
	 */
	private Set<ConstraintCheckIssue> checkPayloadAttribute(TypeElement element) {

		ExecutableElement payloadMethod = getMethod( element, "payload" );

		if ( payloadMethod == null ) {
			return CollectionHelper.asSet(
					ConstraintCheckIssue.error( element, null, "CONSTRAINT_TYPE_MUST_DECLARE_PAYLOAD_MEMBER" )
			);
		}

		DeclaredType type = getComponentTypeOfArrayReturnType( payloadMethod );

		if ( type == null ) {
			return CollectionHelper.asSet(
					ConstraintCheckIssue.error( payloadMethod, null, "PAYLOAD_RETURN_TYPE_MUST_BE_CLASS_ARRAY" )
			);
		}

		boolean typeHasNameClass = type.asElement().getSimpleName().contentEquals( "Class" );
		boolean typeHasExactlyOneTypeArgument = type.getTypeArguments().size() == 1;
		boolean typeArgumentIsWildcardWithPayloadExtendsBound = validateWildcardBounds(
				type.getTypeArguments().get( 0 ),
				annotationApiHelper.getDeclaredTypeByName( BeanValidationTypes.PAYLOAD ),
				null
		);

		if ( !( typeHasNameClass && typeHasExactlyOneTypeArgument && typeArgumentIsWildcardWithPayloadExtendsBound ) ) {
			return CollectionHelper.asSet(
					ConstraintCheckIssue.error( payloadMethod, null, "PAYLOAD_RETURN_TYPE_MUST_BE_CLASS_ARRAY" )
			);
		}

		if ( !isEmptyArray( payloadMethod.getDefaultValue() ) ) {
			return CollectionHelper.asSet(
					ConstraintCheckIssue.error( payloadMethod, null, "PAYLOAD_DEFAULT_VALUE_MUST_BE_EMPTY_ARRAY" )
			);
		}

		return Collections.emptySet();
	}

	/**
	 * Returns the method of the given type with the given name.
	 *
	 * @param element The type of interest.
	 * @param name The name of the method which should be returned.
	 *
	 * @return The method of the given type with the given name or null if no such method exists.
	 */
	private ExecutableElement getMethod(TypeElement element, String name) {

		for ( ExecutableElement oneMethod : methodsIn( element.getEnclosedElements() ) ) {

			if ( oneMethod.getSimpleName().contentEquals( name ) ) {
				return oneMethod;
			}
		}

		return null;
	}

	/**
	 * Returns the component type of the array-typed return value of the given method.
	 *
	 * @param method The method of interest.
	 *
	 * @return The component type of the array-typed return value of the given method or null,
	 * if the given method has no array-typed return value.
	 */
	private DeclaredType getComponentTypeOfArrayReturnType(ExecutableElement method) {

		return method.getReturnType().accept(
				new TypeKindVisitor8<DeclaredType, Void>() {

					@Override
					public DeclaredType visitArray(ArrayType t, Void p) {

						return t.getComponentType().accept(
								new TypeKindVisitor8<DeclaredType, Void>() {

									@Override
									public DeclaredType visitDeclared(DeclaredType t, Void p) {
										return t;
									}

								}, null
						);
					}

				}, null
		);

	}

	/**
	 * Returns true, if the given type mirror is a wildcard type with the given extends and super bounds, false otherwise.
	 *
	 * @param type The type to check.
	 * @param expectedExtendsBound A mirror representing the expected extends bound.
	 * @param expectedSuperBound A mirror representing the expected super bound.
	 *
	 * @return True, if the given type mirror is a wildcard type with the given extends and super bounds, false otherwise.
	 */
	private boolean validateWildcardBounds(TypeMirror type, final TypeMirror expectedExtendsBound, final TypeMirror expectedSuperBound) {

		Boolean theValue = type.accept(
				new TypeKindVisitor8<Boolean, Void>() {

					@Override
					public Boolean visitWildcard(WildcardType t, Void p) {

						boolean extendsBoundMatches = ( t.getExtendsBound() == null
								? expectedExtendsBound == null
								: expectedExtendsBound != null
										&& typeUtils.isSameType( t.getExtendsBound(), expectedExtendsBound ) );
						boolean superBoundMatches = ( t.getSuperBound() == null
								? expectedSuperBound == null
								: expectedSuperBound != null
										&& typeUtils.isSameType( t.getSuperBound(), expectedSuperBound ) );

						return extendsBoundMatches && superBoundMatches;
					}

				}, null
		);

		return Boolean.TRUE.equals( theValue );
	}

	/**
	 * Checks whether the given annotation value is an empty array or not.
	 *
	 * @param annotationValue The annotation value of interest.
	 *
	 * @return True, if the given annotation value is an empty array, false otherwise.
	 */
	private boolean isEmptyArray(AnnotationValue annotationValue) {

		return annotationValue != null
				&& Boolean.TRUE.equals(
						annotationValue.accept( new SimpleAnnotationValueVisitor8<Boolean, Void>() {

							@Override
							public Boolean visitArray(List<? extends AnnotationValue> values, Void p) {
								return values.size() == 0;
							}

						}, null )
				);
	}

}
