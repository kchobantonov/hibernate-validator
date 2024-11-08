/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.ap.testmodel.annotationparameters;

import jakarta.validation.constraints.Size;

/**
 * @author Marko Bekhta
 */
public class ValidGroupsParameters {

	@Size(groups = { })
	private String string;

	@Size(groups = { Group1.class })
	private String string1;

	@Size(groups = { Group1.class, Group2.class })
	private String string2;

	@Size(groups = { InvalidGroup1.class })
	private String string3;

	@Size(groups = { InvalidGroup1.class, InvalidGroup2.class })
	private String string4;

	@Size(groups = { Group1.class, InvalidGroup1.class, InvalidGroup2.class })
	private String string5;

	@Size.List(
		{
				@Size(groups = {
						Group1.class,
						InvalidGroup1.class,
						InvalidGroup2.class
				}),
				@Size(groups = {
						InvalidGroup1.class,
						InvalidGroup2.class
				}),
				@Size(groups = {
						Group1.class,
						Group2.class
				})
		}
	)
	private String string6;

	@Size(groups = { int.class })
	private String string7;

	public interface Group1 {
	}

	public interface Group2 {
	}

	public class InvalidGroup1 {
	}

	public class InvalidGroup2 {
	}
}
