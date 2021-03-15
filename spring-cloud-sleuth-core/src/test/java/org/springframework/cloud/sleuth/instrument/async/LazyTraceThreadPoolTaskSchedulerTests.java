/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument.async;

import brave.Tracing;
import brave.propagation.StrictScopeDecorator;
import brave.propagation.ThreadLocalCurrentTraceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.sleuth.DefaultSpanNamer;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;

@RunWith(MockitoJUnitRunner.class)
public class LazyTraceThreadPoolTaskSchedulerTests {

	Tracing tracing = Tracing.newBuilder()
			.currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder()
					.addScopeDecorator(StrictScopeDecorator.create()).build())
			.build();

	@Mock
	BeanFactory beanFactory;

	@Mock
	ThreadPoolTaskScheduler delegate;

	LazyTraceThreadPoolTaskScheduler executor;

	@BeforeEach
    void setup() {
		this.executor = new LazyTraceThreadPoolTaskScheduler(beanFactory(),
				this.delegate);
	}

	BeanFactory beanFactory() {
		BDDMockito.given(this.beanFactory.getBean(Tracing.class))
				.willReturn(this.tracing);
		BDDMockito.given(this.beanFactory.getBean(SpanNamer.class))
				.willReturn(new DefaultSpanNamer());
		return this.beanFactory;
	}

	@Test
    void setPoolSize() {
		this.executor.setPoolSize(10);

		BDDMockito.then(this.delegate).should().setPoolSize(10);
	}

	@Test
    void setRemoveOnCancelPolicy() {
		this.executor.setRemoveOnCancelPolicy(true);

		BDDMockito.then(this.delegate).should().setRemoveOnCancelPolicy(true);
	}

	@Test
    void setErrorHandler() {
		ErrorHandler handler = (throwable) -> {
		};
		this.executor.setErrorHandler(handler);

		BDDMockito.then(this.delegate).should().setErrorHandler(handler);
	}

	@Test
    void getScheduledExecutor() {
		this.executor.getScheduledExecutor();

		BDDMockito.then(this.delegate).should().getScheduledExecutor();
	}

	@Test
    void getPoolSize() {
		this.executor.getPoolSize();

		BDDMockito.then(this.delegate).should().getPoolSize();
	}

	@Test
    void isRemoveOnCancelPolicy() {
		this.executor.setRemoveOnCancelPolicy(true);

		BDDMockito.then(this.delegate).should().setRemoveOnCancelPolicy(true);
	}

	@Test
    void getActiveCount() {
		this.executor.getActiveCount();

		BDDMockito.then(this.delegate).should().getActiveCount();
	}

	@Test
    void execute() {
		Runnable r = () -> {
		};
		this.executor.execute(r);

		BDDMockito.then(this.delegate).should().execute(r);
	}

	@Test
    void execute1() {
		Runnable r = () -> {
		};
		this.executor.execute(r, 10L);

		BDDMockito.then(this.delegate).should()
				.execute(BDDMockito.any(TraceRunnable.class), BDDMockito.eq(10L));
	}

	@Test
    void submit() {
		Runnable c = () -> {
		};
		this.executor.submit(c);

		BDDMockito.then(this.delegate).should()
				.submit(BDDMockito.any(TraceRunnable.class));
	}

	@Test
    void submit1() {
		Callable c = () -> null;
		this.executor.submit(c);

		BDDMockito.then(this.delegate).should()
				.submit(BDDMockito.any(TraceCallable.class));
	}

	@Test
    void submitListenable() {
		Runnable c = () -> {
		};
		this.executor.submitListenable(c);

		BDDMockito.then(this.delegate).should()
				.submitListenable(BDDMockito.any(TraceRunnable.class));
	}

	@Test
    void submitListenable1() {
		Callable c = () -> null;
		this.executor.submitListenable(c);

		BDDMockito.then(this.delegate).should()
				.submitListenable(BDDMockito.any(TraceCallable.class));
	}

	@Test
    void prefersShortLivedTasks() {
		this.executor.prefersShortLivedTasks();

		BDDMockito.then(this.delegate).should().prefersShortLivedTasks();
	}

	@Test
    void schedule() {
		Runnable c = () -> {
		};
		Trigger trigger = triggerContext -> null;
		this.executor.schedule(c, trigger);

		BDDMockito.then(this.delegate).should()
				.schedule(BDDMockito.any(TraceRunnable.class), BDDMockito.eq(trigger));
	}

	@Test
    void schedule1() {
		Runnable c = () -> {
		};
		Date date = new Date();
		this.executor.schedule(c, date);

		BDDMockito.then(this.delegate).should()
				.schedule(BDDMockito.any(TraceRunnable.class), BDDMockito.eq(date));
	}

	@Test
    void scheduleAtFixedRate() {
		Runnable c = () -> {
		};
		Date date = new Date();
		this.executor.scheduleAtFixedRate(c, date, 10L);

		BDDMockito.then(this.delegate).should().scheduleAtFixedRate(
				BDDMockito.any(TraceRunnable.class), BDDMockito.eq(date),
				BDDMockito.eq(10L));
	}

	@Test
    void scheduleAtFixedRate1() {
		Runnable c = () -> {
		};
		this.executor.scheduleAtFixedRate(c, 10L);

		BDDMockito.then(this.delegate).should().scheduleAtFixedRate(
				BDDMockito.any(TraceRunnable.class), BDDMockito.eq(10L));
	}

	@Test
    void scheduleWithFixedDelay() {
		Runnable c = () -> {
		};
		Date date = new Date();
		this.executor.scheduleWithFixedDelay(c, date, 10L);

		BDDMockito.then(this.delegate).should().scheduleWithFixedDelay(
				BDDMockito.any(TraceRunnable.class), BDDMockito.eq(date),
				BDDMockito.eq(10L));
	}

	@Test
    void scheduleWithFixedDelay1() {
		Runnable c = () -> {
		};
		this.executor.scheduleWithFixedDelay(c, 10L);

		BDDMockito.then(this.delegate).should().scheduleWithFixedDelay(
				BDDMockito.any(TraceRunnable.class), BDDMockito.eq(10L));
	}

	@Test
    void scheduleWithFixedDelay2() {
		Runnable c = () -> {
		};
		Instant instant = Instant.now();
		Duration duration = Duration.ZERO;
		this.executor.scheduleWithFixedDelay(c, instant, duration);

		BDDMockito.then(this.delegate).should().scheduleWithFixedDelay(
				BDDMockito.any(TraceRunnable.class), BDDMockito.eq(instant),
				BDDMockito.eq(duration));
	}

	@Test
    void scheduleWithFixedDelay3() {
		Runnable c = () -> {
		};
		Duration duration = Duration.ZERO;
		this.executor.scheduleWithFixedDelay(c, duration);

		BDDMockito.then(this.delegate).should().scheduleWithFixedDelay(
				BDDMockito.any(TraceRunnable.class), BDDMockito.eq(duration));
	}

	@Test
    void setThreadFactory() {
		ThreadFactory threadFactory = r -> null;
		this.executor.setThreadFactory(threadFactory);

		BDDMockito.then(this.delegate).should().setThreadFactory(threadFactory);
	}

	@Test
    void setThreadNamePrefix() {
		this.executor.setThreadNamePrefix("foo");

		BDDMockito.then(this.delegate).should().setThreadNamePrefix("foo");
	}

	@Test
    void setRejectedExecutionHandler() {
		RejectedExecutionHandler handler = (r, executor1) -> {
		};
		this.executor.setRejectedExecutionHandler(handler);

		BDDMockito.then(this.delegate).should().setRejectedExecutionHandler(handler);
	}

	@Test
    void setWaitForTasksToCompleteOnShutdown() {
		this.executor.setWaitForTasksToCompleteOnShutdown(true);

		BDDMockito.then(this.delegate).should().setWaitForTasksToCompleteOnShutdown(true);
	}

	@Test
    void setAwaitTerminationSeconds() {
		this.executor.setAwaitTerminationSeconds(10);

		BDDMockito.then(this.delegate).should().setAwaitTerminationSeconds(10);
	}

	@Test
    void setBeanName() {
		this.executor.setBeanName("foo");

		BDDMockito.then(this.delegate).should().setBeanName("foo");
	}

	@Test
    void afterPropertiesSet() {
		this.executor.afterPropertiesSet();

		BDDMockito.then(this.delegate).should().afterPropertiesSet();
	}

	@Test
    void initialize() {
		this.executor.initialize();

		BDDMockito.then(this.delegate).should().initialize();
	}

	@Test
    void destroy() {
		this.executor.destroy();

		BDDMockito.then(this.delegate).should().destroy();
	}

	@Test
    void shutdown() {
		this.executor.shutdown();

		BDDMockito.then(this.delegate).should().shutdown();
	}

	@Test
    void newThread() {
		Runnable runnable = () -> {
		};
		this.executor.newThread(runnable);

		BDDMockito.then(this.delegate).should().newThread(runnable);
	}

	@Test
    void getThreadNamePrefix() {
		this.executor.getThreadNamePrefix();

		BDDMockito.then(this.delegate).should().getThreadNamePrefix();
	}

	@Test
    void setThreadPriority() {
		this.executor.setThreadPriority(10);

		BDDMockito.then(this.delegate).should().setThreadPriority(10);
	}

	@Test
    void getThreadPriority() {
		this.executor.getThreadPriority();

		BDDMockito.then(this.delegate).should().getThreadPriority();
	}

	@Test
    void setDaemon() {
		this.executor.setDaemon(true);

		BDDMockito.then(this.delegate).should().setDaemon(true);
	}

	@Test
    void isDaemon() {
		this.executor.isDaemon();

		BDDMockito.then(this.delegate).should().isDaemon();
	}

	@Test
    void setThreadGroupName() {
		this.executor.setThreadGroupName("foo");

		BDDMockito.then(this.delegate).should().setThreadGroupName("foo");
	}

	@Test
    void setThreadGroup() {
		ThreadGroup threadGroup = new ThreadGroup("foo");
		this.executor.setThreadGroup(threadGroup);

		BDDMockito.then(this.delegate).should().setThreadGroup(threadGroup);
	}

	@Test
    void getThreadGroup() {
		this.executor.getThreadGroup();

		BDDMockito.then(this.delegate).should().getThreadGroup();
	}

	@Test
    void createThread() {
		Runnable r = () -> {
		};
		this.executor.createThread(r);

		BDDMockito.then(this.delegate).should().createThread(r);
	}

	@Test
    void schedule2() {
		Runnable r = () -> {
		};
		Instant instant = Instant.now();
		this.executor.schedule(r, instant);

		BDDMockito.then(this.delegate).should()
				.schedule(BDDMockito.any(TraceRunnable.class), BDDMockito.eq(instant));
	}

	@Test
    void scheduleAtFixedRate2() {
		Runnable r = () -> {
		};
		Instant instant = Instant.now();
		Duration duration = Duration.ZERO;
		this.executor.scheduleAtFixedRate(r, instant, duration);

		BDDMockito.then(this.delegate).should().scheduleAtFixedRate(
				BDDMockito.any(TraceRunnable.class), BDDMockito.eq(instant),
				BDDMockito.eq(duration));
	}

	@Test
    void scheduleAtFixedRate3() {
		Runnable r = () -> {
		};
		Duration duration = Duration.ZERO;
		this.executor.scheduleAtFixedRate(r, duration);

		BDDMockito.then(this.delegate).should().scheduleAtFixedRate(
				BDDMockito.any(TraceRunnable.class), BDDMockito.eq(duration));
	}

}
