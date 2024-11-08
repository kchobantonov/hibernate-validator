/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.ap.testmodel.annotationparameters;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @author Marko Bekhta
 * @author Guillaume Smet
 */
public class ValidGroupSequenceParameters {

	/**
	 * Case 1: some valid groups
	 */
	public static class Case1 {
		public interface Group1 {
		}

		public interface Group2 {
		}

		@GroupSequence(value = Group2.class)
		public interface Group3 {
		}

		@GroupSequence(value = { Group1.class, Group2.class })
		public interface Group4 {
		}

		@GroupSequence(value = { Group1.class, Group2.class, SomeBean.class })
		public class SomeBean {
		}

		@GroupSequence(value = { Group2.class, SomeOtherBean.class })
		public class SomeOtherBean {
		}

	}

	/**
	 * Case 2: Example from documentation
	 */
	public static class Case2 {
		public interface RentalChecks {
		}

		public interface CarChecks {
		}

		public class Car {
			@NotNull
			private String manufacturer;

			@NotNull
			@Size(min = 2, max = 14)
			private String licensePlate;

			@Min(2)
			private int seatCount;

			@AssertTrue(
					message = "The car has to pass the vehicle inspection first",
					groups = CarChecks.class
			)
			private boolean passedVehicleInspection;

			public Car(String manufacturer, String licencePlate, int seatCount) {
				this.manufacturer = manufacturer;
				this.licensePlate = licencePlate;
				this.seatCount = seatCount;
			}

			// getters and setters ...
		}

		@GroupSequence({ RentalChecks.class, CarChecks.class, RentalCar.class })
		public class RentalCar extends Car {
			@AssertFalse(message = "The car is currently rented out", groups = RentalChecks.class)
			private boolean rented;

			public RentalCar(String manufacturer, String licencePlate, int seatCount) {
				super( manufacturer, licencePlate, seatCount );
			}

			public boolean isRented() {
				return rented;
			}

			public void setRented(boolean rented) {
				this.rented = rented;
			}
		}
	}

	/**
	 * Case 3: Hierarchy of groups
	 */
	public static class Case3 {
		public interface Group1 {
		}

		public interface Group2 extends Group1 {
		}

		public interface Group3 {
		}

		@GroupSequence(value = { Group2.class, Group3.class })
		public interface GroupSequence1 {
		}
	}

	/**
	 * Case 4: Hierarchy of groups and group sequences
	 */
	public static class Case4 {
		public interface Group1 {
		}

		public interface Group2 {
		}

		@GroupSequence(value = Group1.class)
		public interface GroupSequence1 {
		}

		@GroupSequence(value = GroupSequence1.class)
		public interface GroupSequence2 {
		}

		@GroupSequence(value = { Group2.class, GroupSequence2.class })
		public interface GroupSequence3 {
		}

	}

}
