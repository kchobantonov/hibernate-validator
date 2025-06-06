/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.performance;

import java.util.Objects;
import java.util.stream.Stream;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Class containing main method to run all performance tests.
 *
 * @author Marko Bekhta
 * @author Guillaume Smet
 */
public final class BenchmarkRunner {

	private static final Stream<? extends Class<?>> DEFAULT_TEST_CLASSES = Stream.of(
			"org.hibernate.validator.performance.cascaded.CascadedValidation",
			"org.hibernate.validator.performance.cascaded.CascadedWithLotsOfItemsValidation",
			"org.hibernate.validator.performance.simple.SimpleValidation",
			"org.hibernate.validator.performance.statistical.StatisticalValidation",
			// Benchmarks specific to Bean Validation 2.0
			// Tests are located in a separate source folder only added for implementations compatible with BV 2.0
			"org.hibernate.validator.performance.multilevel.MultiLevelContainerValidation"
	).map( BenchmarkRunner::classForName ).filter( Objects::nonNull );

	private BenchmarkRunner() {
	}

	public static void main(String[] args) throws RunnerException, CommandLineOptionException {
		Options commandLineOptions = new CommandLineOptions( args );
		ChainedOptionsBuilder builder = new OptionsBuilder().parent( commandLineOptions );

		if ( !commandLineOptions.getResult().hasValue() ) {
			builder.result( "target/jmh-results.json" );
		}
		if ( !commandLineOptions.getResultFormat().hasValue() ) {
			builder.resultFormat( ResultFormatType.JSON );
		}
		if ( commandLineOptions.getIncludes().isEmpty() ) {
			DEFAULT_TEST_CLASSES.forEach( testClass -> builder.include( testClass.getName() ) );
		}

		Options opt = builder.build();
		new Runner( opt ).run();
	}

	private static Class<?> classForName(String qualifiedName) {
		try {
			return Class.forName( qualifiedName );
		}
		catch (ClassNotFoundException e) {
			// silently ignore the error
		}
		return null;
	}

}
