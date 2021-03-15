/*
 * Copyright 2013-2018 the original author or authors.
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

package org.springframework.cloud.sleuth.instrument.web.multiple;

import brave.Span;
import brave.Tracer;
import brave.propagation.ExtraFieldPropagation;
import brave.sampler.Sampler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.cloud.sleuth.util.ArrayListSpanReporter;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.BDDAssertions.then;
import static org.awaitility.Awaitility.await;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {
		"spring.application.name=multiplehopsintegrationtests",
		"spring.sleuth.http.legacy.enabled=true"
        })
@SpringBootTest(classes = MultipleHopsIntegrationTests.Config.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("baggage")
public class MultipleHopsIntegrationTests {

	@Autowired Tracer tracer;
	@Autowired ArrayListSpanReporter reporter;
	@Autowired RestTemplate restTemplate;
	@Autowired Config config;
	@Autowired DemoApplication application;

	@BeforeEach
    void setup() {
		this.reporter.clear();
	}

	@Test
    void should_prepare_spans_for_export() throws Exception {
		this.restTemplate.getForObject("http://localhost:" + this.config.port + "/greeting", String.class);

		await().atMost(5, SECONDS).untilAsserted(() -> {
			then(this.reporter.getSpans()).hasSize(14);
		});
		then(this.reporter.getSpans().stream().map(zipkin2.Span::name)
				.collect(toList())).containsAll(asList("http:/greeting", "send"));
		then(this.reporter.getSpans().stream().map(zipkin2.Span::kind)
				// no server kind due to test constraints
				.collect(toList())).containsAll(asList(zipkin2.Span.Kind.CONSUMER,
				zipkin2.Span.Kind.PRODUCER, zipkin2.Span.Kind.SERVER));
		then(this.reporter.getSpans().stream()
				.map(span -> span.tags().get("channel"))
				.filter(Objects::nonNull)
				.distinct()
				.collect(toList()))
				.hasSize(3)
				.containsAll(asList("words", "counts", "greetings"));
	}

	// issue #237 - baggage
	@Test
    void should_propagate_the_baggage() throws Exception {
		//tag::baggage[]
		Span initialSpan = this.tracer.nextSpan().name("span").start();
		try (Tracer.SpanInScope ws = this.tracer.withSpanInScope(initialSpan)) {
			ExtraFieldPropagation.set("foo", "bar");
			ExtraFieldPropagation.set("UPPER_CASE", "someValue");
			//end::baggage[]
		}

		try (Tracer.SpanInScope ws = this.tracer.withSpanInScope(initialSpan)) {
			//tag::baggage_tag[]
			initialSpan.tag("foo",
					ExtraFieldPropagation.get(initialSpan.context(), "foo"));
			initialSpan.tag("UPPER_CASE",
					ExtraFieldPropagation.get(initialSpan.context(), "UPPER_CASE"));
			//end::baggage_tag[]

			HttpHeaders headers = new HttpHeaders();
			headers.put("baggage-baz", Collections.singletonList("baz"));
			headers.put("baggage-bizarreCASE", Collections.singletonList("value"));
			RequestEntity requestEntity = new RequestEntity(headers, HttpMethod.GET,
					URI.create("http://localhost:" + this.config.port + "/greeting"));
			this.restTemplate.exchange(requestEntity, String.class);
		} finally {
			initialSpan.finish();
		}
		await().atMost(5, SECONDS).untilAsserted(() -> {
			then(this.reporter.getSpans()).isNotEmpty();
		});

		then(this.application.allSpans()).as("All have foo")
				.allMatch(span -> "bar".equals(baggage(span, "foo")));
		then(this.application.allSpans()).as("All have UPPER_CASE")
				.allMatch(span -> "someValue".equals(baggage(span, "UPPER_CASE")));
		then(this.application.allSpans()
				.stream()
				.filter(span -> "baz".equals(baggage(span, "baz")))
				.collect(Collectors.toList()))
				.as("Someone has baz")
				.isNotEmpty();
		then(this.reporter.getSpans()
				.stream()
				.filter(span -> span.tags().containsKey("foo") && span.tags().containsKey("UPPER_CASE"))
				.collect(Collectors.toList()))
				.as("Someone has foo and UPPER_CASE tags")
				.isNotEmpty();
		then(this.application.allSpans()
				.stream()
				.filter(span -> "value".equals(baggage(span, "bizarreCASE")))
				.collect(Collectors.toList()))
				.isNotEmpty();
	}

	private String baggage(Span span, String name) {
		return ExtraFieldPropagation.get(span.context(), name);
	}

	@Configuration
	@SpringBootApplication(exclude = JmxAutoConfiguration.class)
	public static class Config implements
			ApplicationListener<ServletWebServerInitializedEvent> {
		int port;

		@Override
		public void onApplicationEvent(ServletWebServerInitializedEvent event) {
			this.port = event.getSource().getPort();
		}

		@Bean
		RestTemplate restTemplate() {
			return new RestTemplate();
		}

		@Bean ArrayListSpanReporter arrayListSpanAccumulator() {
			return new ArrayListSpanReporter();
		}

		@Bean Sampler defaultTraceSampler() {
			return Sampler.ALWAYS_SAMPLE;
		}
	}
}
