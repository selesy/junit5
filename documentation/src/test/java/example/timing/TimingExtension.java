/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package example.timing;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.junit.gen5.api.extension.AfterTestMethodCallback;
import org.junit.gen5.api.extension.BeforeTestMethodCallback;
import org.junit.gen5.api.extension.ExtensionContext.Namespace;
import org.junit.gen5.api.extension.ExtensionContext.Store;
import org.junit.gen5.api.extension.TestExtensionContext;

/**
 * Simple extension that <em>times</em> the execution of test methods and
 * logs the results at {@code INFO} level.
 *
 * @since 5.0
 */
public class TimingExtension implements BeforeTestMethodCallback, AfterTestMethodCallback {

	private static final Logger LOG = Logger.getLogger(TimingExtension.class.getName());

	@Override
	public void beforeTestMethod(TestExtensionContext context) throws Exception {
		getStore(context).put(context.getTestMethod().get(), System.currentTimeMillis());
	}

	@Override
	public void afterTestMethod(TestExtensionContext context) throws Exception {
		Method testMethod = context.getTestMethod().get();
		long start = (long) getStore(context).remove(testMethod);
		long duration = System.currentTimeMillis() - start;

		LOG.info(() -> String.format("Method [%s] took %s ms.", testMethod, duration));
	}

	private Store getStore(TestExtensionContext context) {
		return context.getStore(Namespace.of(getClass(), context));
	}

}
