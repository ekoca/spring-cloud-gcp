/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.autoconfigure.spanner;

import java.io.IOException;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.SessionPoolOptions;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.SpannerOptions.Builder;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.UsageTrackingHeaderProvider;
import org.springframework.cloud.gcp.data.spanner.core.SpannerMutationFactory;
import org.springframework.cloud.gcp.data.spanner.core.SpannerMutationFactoryImpl;
import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerDatabaseAdminTemplate;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.MappingSpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides Spring Data classes to use with Google Spanner.
 *
 * @author Chengyuan Zhao
 */
@Configuration
@AutoConfigureAfter(GcpContextAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.cloud.gcp.spanner.enabled", matchIfMissing = true)
@ConditionalOnClass({ SpannerMappingContext.class, SpannerOperations.class,
		SpannerMutationFactory.class, SpannerConverter.class })
@EnableConfigurationProperties(GcpSpannerProperties.class)
public class GcpSpannerAutoConfiguration {

	private final String projectId;

	private final String instanceId;

	private final String databaseName;

	private final Credentials credentials;

	private final int numRpcChannels;

	private final int prefetchChunks;

	private final int minSessions;

	private final int maxSessions;

	private final int maxIdleSessions;

	private final float writeSessionsFraction;

	private final int keepAliveIntervalMinutes;

	public GcpSpannerAutoConfiguration(GcpSpannerProperties gcpSpannerProperties,
			GcpProjectIdProvider projectIdProvider,
			CredentialsProvider credentialsProvider) throws IOException {
		this.credentials = (gcpSpannerProperties.getCredentials().hasKey()
				? new DefaultCredentialsProvider(gcpSpannerProperties)
				: credentialsProvider).getCredentials();
		this.projectId = gcpSpannerProperties.getProjectId() != null
				? gcpSpannerProperties.getProjectId()
				: projectIdProvider.getProjectId();
		this.instanceId = gcpSpannerProperties.getInstanceId();
		this.databaseName = gcpSpannerProperties.getDatabase();
		this.numRpcChannels = gcpSpannerProperties.getNumRpcChannels();
		this.prefetchChunks = gcpSpannerProperties.getPrefetchChunks();
		this.minSessions = gcpSpannerProperties.getMinSessions();
		this.maxSessions = gcpSpannerProperties.getMaxSessions();
		this.maxIdleSessions = gcpSpannerProperties.getMaxIdleSessions();
		this.writeSessionsFraction = gcpSpannerProperties.getWriteSessionsFraction();
		this.keepAliveIntervalMinutes = gcpSpannerProperties
				.getKeepAliveIntervalMinutes();
	}

	@Bean
	@ConditionalOnMissingBean
	public SpannerOptions spannerOptions(SessionPoolOptions sessionPoolOptions) {
		Builder builder = SpannerOptions.newBuilder()
				.setProjectId(this.projectId)
				.setHeaderProvider(new UsageTrackingHeaderProvider(this.getClass()))
				.setCredentials(this.credentials);
		if (this.numRpcChannels >= 0) {
			builder.setNumChannels(this.numRpcChannels);
		}
		if (this.prefetchChunks >= 0) {
			builder.setPrefetchChunks(this.prefetchChunks);
		}
		builder.setSessionPoolOption(sessionPoolOptions);
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public SessionPoolOptions sessionPoolOptions() {
		SessionPoolOptions.Builder builder = SessionPoolOptions.newBuilder();
		if (this.minSessions >= 0) {
			builder.setMinSessions(this.minSessions);
		}

		if (this.maxSessions >= 0) {
			builder.setMaxSessions(this.maxSessions);
		}

		if (this.maxIdleSessions >= 0) {
			builder.setMaxIdleSessions(this.maxIdleSessions);
		}

		if (this.writeSessionsFraction >= 0) {
			builder.setWriteSessionsFraction(this.writeSessionsFraction);
		}

		if (this.keepAliveIntervalMinutes >= 0) {
			builder.setKeepAliveIntervalMinutes(this.keepAliveIntervalMinutes);
		}
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public DatabaseId databaseId() {
		return DatabaseId.of(this.projectId, this.instanceId, this.databaseName);
	}

	@Bean
	@ConditionalOnMissingBean
	public Spanner spanner(SpannerOptions spannerOptions) {
		return spannerOptions.getService();
	}

	@Bean
	@ConditionalOnMissingBean
	public DatabaseClient spannerDatabaseClient(Spanner spanner, DatabaseId databaseId) {
		return spanner.getDatabaseClient(databaseId);
	}

	@Bean
	@ConditionalOnMissingBean
	public SpannerMappingContext spannerMappingContext() {
		return new SpannerMappingContext();
	}

	@Bean
	@ConditionalOnMissingBean
	public SpannerTemplate spannerTemplate(DatabaseClient databaseClient,
			SpannerMappingContext mappingContext, SpannerConverter spannerConverter,
			SpannerMutationFactory spannerMutationFactory) {
		return new SpannerTemplate(databaseClient, mappingContext, spannerConverter,
				spannerMutationFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	public SpannerConverter spannerConverter(SpannerMappingContext mappingContext) {
		return new MappingSpannerConverter(mappingContext);
	}

	@Bean
	@ConditionalOnMissingBean
	public SpannerMutationFactory spannerMutationFactory(
			SpannerConverter spannerConverter,
			SpannerMappingContext spannerMappingContext) {
		return new SpannerMutationFactoryImpl(spannerConverter, spannerMappingContext);
	}

	@Bean
	@ConditionalOnMissingBean
	public SpannerSchemaUtils spannerSchemaUtils(
			SpannerMappingContext spannerMappingContext,
			SpannerConverter spannerConverter) {
		return new SpannerSchemaUtils(spannerMappingContext, spannerConverter);
	}

	@Bean
	@ConditionalOnMissingBean
	public SpannerDatabaseAdminTemplate spannerDatabaseAdminTemplate(
			Spanner spanner, DatabaseId databaseId) {
		return new SpannerDatabaseAdminTemplate(spanner.getDatabaseAdminClient(),
				databaseId);
	}
}
