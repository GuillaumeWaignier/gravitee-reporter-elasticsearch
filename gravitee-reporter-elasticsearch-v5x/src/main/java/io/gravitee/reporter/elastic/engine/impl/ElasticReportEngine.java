/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.reporter.elastic.engine.impl;

import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.monitor.Monitor;
import io.gravitee.reporter.elastic.model.Protocol;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Elasticsearch report engine. 
 * 
 * Use client bulk processor and support connection strategy : 
 * <ul>
 * 		<li>{@link Protocol}.NODE</li>
 *  	<li>{@link Protocol}.TRANSPORT</li>
 * </ul>
 * 
 * @author ldassonville
 *
 */
public final class ElasticReportEngine extends AbstractElasticReportEngine {

	private final Logger LOGGER = LoggerFactory.getLogger(ElasticReportEngine.class);

	public final static String TYPE_REQUEST = "request";
	public final static String TYPE_HEALTH = "health";
	public final static String TYPE_MONITOR = "monitor";
	public final static String TYPE_LOG = "log";

	@Autowired
	private BulkProcessor bulkProcessor;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void report(Reportable reportable) {
		try {
			String indexName = getIndexName(reportable);

			if (reportable instanceof Metrics) {
				Metrics metrics = (Metrics) reportable;
				bulkProcessor.add(new IndexRequest(indexName, TYPE_REQUEST)
						.source(getSource(metrics)));

				if (metrics.getLog() != null) {
					bulkProcessor.add(new IndexRequest(indexName, TYPE_LOG)
						.source(getSource(metrics.getLog())));
				}
			} else if (reportable instanceof EndpointStatus) {
				bulkProcessor.add(new IndexRequest(indexName, TYPE_HEALTH)
						.source(getSource((EndpointStatus) reportable)));
			} else if (reportable instanceof Monitor) {
				bulkProcessor.add(new IndexRequest(indexName, TYPE_MONITOR)
						.source(getSource((Monitor) reportable)));
			}
		} catch (IOException ioe) {
			LOGGER.error("Unexpected error while indexing into ES", ioe);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start() {
		LOGGER.info("Starting Elastic reporter engine...");
		
		//TODO connectivity check
		
		LOGGER.info("Starting Elastic reporter engine... DONE");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() {
		LOGGER.info("Stopping Elastic reporter engine...");

		try {
			bulkProcessor.awaitClose(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error("Error while closing processor", e);
		}

		LOGGER.info("Stopping Elastic reporter engine... DONE");
	}
}
