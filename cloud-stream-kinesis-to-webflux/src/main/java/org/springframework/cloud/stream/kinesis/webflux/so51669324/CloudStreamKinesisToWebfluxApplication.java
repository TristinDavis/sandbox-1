/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.kinesis.webflux.so51669324;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.services.kinesis.model.Record;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.UnicastProcessor;

@SpringBootApplication
@EnableBinding(Sink.class)
@RestController
public class CloudStreamKinesisToWebfluxApplication {

	private final UnicastProcessor<String> sseFluxProcessor = UnicastProcessor.create();

	private final Flux<String> sseFlux = this.sseFluxProcessor.share();

	private volatile ConnectableFlux<?> recordFlux;

	@GetMapping(value = "/sseFromKinesis", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> getSeeFromKinesis() {
		this.recordFlux.connect();
		return this.sseFlux;
	}

	@StreamListener(Sink.INPUT)
	public void kinesisSink(Flux<List<Record>> recordFlux) {
		this.recordFlux = recordFlux
				.flatMap(Flux::fromIterable)
				.map(record -> new String(record.getData().array(), StandardCharsets.UTF_8))
				.handle((message, sink) -> this.sseFluxProcessor.onNext(message))
				.publish();
	}


	public static void main(String[] args) {
		SpringApplication.run(CloudStreamKinesisToWebfluxApplication.class, args);
	}

}
