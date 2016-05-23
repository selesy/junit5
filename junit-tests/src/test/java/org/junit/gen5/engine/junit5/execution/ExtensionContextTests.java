/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.junit5.execution;

import static org.junit.gen5.api.Assertions.assertEquals;
import static org.junit.gen5.api.Assertions.assertNull;
import static org.junit.gen5.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.gen5.api.Assertions;
import org.junit.gen5.api.Tag;
import org.junit.gen5.api.Test;
import org.junit.gen5.api.extension.ExtensionContext;
import org.junit.gen5.engine.EngineExecutionListener;
import org.junit.gen5.engine.TestDescriptor;
import org.junit.gen5.engine.UniqueId;
import org.junit.gen5.engine.junit5.descriptor.ClassBasedContainerExtensionContext;
import org.junit.gen5.engine.junit5.descriptor.ClassTestDescriptor;
import org.junit.gen5.engine.junit5.descriptor.MethodBasedTestExtensionContext;
import org.junit.gen5.engine.junit5.descriptor.MethodTestDescriptor;
import org.junit.gen5.engine.junit5.descriptor.NestedClassTestDescriptor;
import org.junit.gen5.engine.reporting.ReportEntry;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Microtests for implementors of {@linkplain ExtensionContext}:
 * {@linkplain ClassBasedContainerExtensionContext} and
 * {@linkplain MethodBasedTestExtensionContext}
 *
 * @since 5.0
 */
public class ExtensionContextTests {

	@Test
	public void fromClassTestDescriptor() {
		ClassTestDescriptor nestedClassDescriptor = nestedClassDescriptor();
		ClassTestDescriptor outerClassDescriptor = outerClassDescriptor(nestedClassDescriptor);

		ClassBasedContainerExtensionContext outerExtensionContext = new ClassBasedContainerExtensionContext(null, null,
			outerClassDescriptor);
		Assertions.assertAll("outerContext", //
			() -> assertEquals(OuterClass.class, outerExtensionContext.getTestClass().get()), //
			() -> assertEquals(outerClassDescriptor.getDisplayName(), outerExtensionContext.getDisplayName()), //
			() -> assertEquals(Optional.empty(), outerExtensionContext.getParent()) //
		);

		ClassBasedContainerExtensionContext nestedExtensionContext = new ClassBasedContainerExtensionContext(
			outerExtensionContext, null, nestedClassDescriptor);
		Assertions.assertSame(outerExtensionContext, nestedExtensionContext.getParent().get());
	}

	@Test
	public void tagsCanBeRetrievedInExtensionContext() {
		ClassTestDescriptor nestedClassDescriptor = nestedClassDescriptor();
		ClassTestDescriptor outerClassDescriptor = outerClassDescriptor(nestedClassDescriptor);
		MethodTestDescriptor methodTestDescriptor = methodDescriptor();
		outerClassDescriptor.addChild(methodTestDescriptor);

		ClassBasedContainerExtensionContext outerExtensionContext = new ClassBasedContainerExtensionContext(null, null,
			outerClassDescriptor);

		Assertions.assertAll("tags in outer class", //
			() -> assertEquals(1, outerExtensionContext.getTags().size()), //
			() -> assertTrue(outerExtensionContext.getTags().contains("outer-tag")));

		ClassBasedContainerExtensionContext nestedExtensionContext = new ClassBasedContainerExtensionContext(
			outerExtensionContext, null, nestedClassDescriptor);
		Assertions.assertAll("tags in nested class", //
			() -> assertEquals(2, nestedExtensionContext.getTags().size()), //
			() -> assertTrue(nestedExtensionContext.getTags().contains("outer-tag"), "outer-tag missing"), //
			() -> assertTrue(nestedExtensionContext.getTags().contains("nested-tag"), "nested-tag missing"));

		MethodBasedTestExtensionContext testExtensionContext = new MethodBasedTestExtensionContext(
			outerExtensionContext, null, methodTestDescriptor, new OuterClass());
		Assertions.assertAll("tags in method", //
			() -> assertEquals(2, testExtensionContext.getTags().size()), //
			() -> assertTrue(testExtensionContext.getTags().contains("outer-tag"), "outer-tag missing"), //
			() -> assertTrue(testExtensionContext.getTags().contains("method-tag"), "method-tag missing"));
	}

	@Test
	public void fromMethodTestDescriptor() {
		MethodTestDescriptor methodTestDescriptor = methodDescriptor();
		ClassTestDescriptor classTestDescriptor = outerClassDescriptor(methodTestDescriptor);

		ClassBasedContainerExtensionContext classExtensionContext = new ClassBasedContainerExtensionContext(null, null,
			classTestDescriptor);
		MethodBasedTestExtensionContext testExtensionContext = new MethodBasedTestExtensionContext(
			classExtensionContext, null, methodTestDescriptor, new OuterClass());
		Assertions.assertAll("methodContext", //
			() -> assertEquals(OuterClass.class, testExtensionContext.getTestClass().get()), //
			() -> assertEquals(methodTestDescriptor.getDisplayName(), testExtensionContext.getDisplayName()), //
			() -> assertEquals(classExtensionContext, testExtensionContext.getParent().get()), //
			() -> assertEquals(OuterClass.class, testExtensionContext.getTestInstance().getClass()) //
		);
	}

	@Test
	public void reportEntriesArePublishedToExecutionContext() {
		ClassTestDescriptor classTestDescriptor = outerClassDescriptor(null);
		EngineExecutionListener engineExecutionListener = Mockito.spy(EngineExecutionListener.class);
		ExtensionContext extensionContext = new ClassBasedContainerExtensionContext(null, engineExecutionListener,
			classTestDescriptor);

		Map<String, String> entry1 = Collections.singletonMap("key", "value");
		Map<String, String> entry2 = Collections.singletonMap("other key", "other value");

		extensionContext.publishReportEntry(entry1);
		extensionContext.publishReportEntry(entry2);

		ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass(ReportEntry.class);
		Mockito.verify(engineExecutionListener, Mockito.times(2)).reportingEntryPublished(
			Mockito.eq(classTestDescriptor), entryCaptor.capture());

		ReportEntry reportEntry1 = entryCaptor.getAllValues().get(0);
		ReportEntry reportEntry2 = entryCaptor.getAllValues().get(1);

		assertEquals(entry1, reportEntry1.getValues());
		assertEquals(entry2, reportEntry2.getValues());
	}

	@Test
	public void usingStore() {
		MethodTestDescriptor methodTestDescriptor = methodDescriptor();
		ClassTestDescriptor classTestDescriptor = outerClassDescriptor(methodTestDescriptor);
		ExtensionContext parentContext = new ClassBasedContainerExtensionContext(null, null, classTestDescriptor);
		MethodBasedTestExtensionContext childContext = new MethodBasedTestExtensionContext(parentContext, null,
			methodTestDescriptor, new OuterClass());

		ExtensionContext.Store childStore = childContext.getStore();
		ExtensionContext.Store parentStore = parentContext.getStore();

		final Object key1 = "key 1";
		final String value1 = "a value";
		childStore.put(key1, value1);
		assertEquals(value1, childStore.get(key1));
		assertEquals(value1, childStore.remove(key1));
		assertNull(childStore.get(key1));

		final Object key2 = "key 2";
		final String value2 = "other value";
		assertEquals(value2, childStore.getOrComputeIfAbsent(key2, key -> value2));
		assertEquals(value2, childStore.get(key2));

		final Object parentKey = "parent key";
		final String parentValue = "parent value";
		parentStore.put(parentKey, parentValue);
		assertEquals(parentValue, childStore.get(parentKey));
	}

	private ClassTestDescriptor nestedClassDescriptor() {
		return new NestedClassTestDescriptor(UniqueId.root("nested-class", "NestedClass"),
			OuterClass.NestedClass.class);
	}

	private ClassTestDescriptor outerClassDescriptor(TestDescriptor child) {
		ClassTestDescriptor classTestDescriptor = new ClassTestDescriptor(UniqueId.root("class", "OuterClass"),
			OuterClass.class);
		if (child != null)
			classTestDescriptor.addChild(child);
		return classTestDescriptor;
	}

	private MethodTestDescriptor methodDescriptor() {
		try {
			return new MethodTestDescriptor(UniqueId.root("method", "aMethod"), OuterClass.class,
				OuterClass.class.getDeclaredMethod("aMethod"));
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Tag("outer-tag")
	static class OuterClass {

		@Tag("nested-tag")
		class NestedClass {
		}

		@Tag("method-tag")
		void aMethod() {
		}
	}

}
