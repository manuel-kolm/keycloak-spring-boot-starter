package org.springframework.keycloak.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.CompositeName;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;
import javax.sql.DataSource;

import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.keycloak.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ KeycloakServerProperties.class })
@SpringBootApplication(exclude = LiquibaseAutoConfiguration.class)
public class EmbeddedKeycloakAutoConfig {

	private static final Logger LOG = LoggerFactory.getLogger(EmbeddedKeycloakAutoConfig.class);

	@Bean
	protected ApplicationListener<ApplicationReadyEvent> onApplicationReadyEventListener(
			ServerProperties serverProperties, KeycloakServerProperties keycloakServerProperties) {

		return (evt) -> {
			Integer port = serverProperties.getPort();
			String keycloakContextPath = keycloakServerProperties.getContextPath();

			LOG.info("Embedded Keycloak started: http://localhost:{}{} to use keycloak", port, keycloakContextPath);
		};
	}

	@Bean
	protected ServletRegistrationBean<HttpServlet30Dispatcher> keycloakJaxRsApplication(
			KeycloakServerProperties keycloakServerProperties, DataSource dataSource) throws Exception {

		EmbeddedKeycloakApplication.keycloakServerProperties = keycloakServerProperties;

		mockJndiEnvironment(dataSource);

		ServletRegistrationBean<HttpServlet30Dispatcher> servlet = new ServletRegistrationBean<>(
				new HttpServlet30Dispatcher());
		servlet.addInitParameter("javax.ws.rs.Application", EmbeddedKeycloakApplication.class.getName());
		servlet.addInitParameter(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX,
				keycloakServerProperties.getContextPath());
		servlet.addInitParameter(ResteasyContextParameters.RESTEASY_USE_CONTAINER_FORM_PARAMS, "true");
		servlet.addUrlMappings(keycloakServerProperties.getContextPath() + "/*");
		servlet.setLoadOnStartup(1);
		servlet.setAsyncSupported(true);

		return servlet;
	}

	@Bean
	protected FilterRegistrationBean<EmbeddedKeycloakRequestFilter> keycloakSessionManagement(
			KeycloakServerProperties keycloakServerProperties) {

		FilterRegistrationBean<EmbeddedKeycloakRequestFilter> filter = new FilterRegistrationBean<>();
		filter.setName("Keycloak Session Management");
		filter.setFilter(new EmbeddedKeycloakRequestFilter());
		filter.addUrlPatterns(keycloakServerProperties.getContextPath() + "/*");

		return filter;
	}

	@Bean("fixedThreadPool")
	protected ExecutorService fixedThreadPool() {
		return Executors.newFixedThreadPool(5);
	}

	@Bean
	@ConditionalOnMissingBean(name = "springBootPlatform")
	protected SimplePlatformProvider springBootPlatform() {
		return (SimplePlatformProvider) Platform.getPlatform();
	}

	private void mockJndiEnvironment(DataSource dataSource) throws NamingException {
		NamingManager.setInitialContextFactoryBuilder((env) -> (environment) -> new InitialContext() {

			@Override
			public Object lookup(Name name) {
				return lookup(name.toString());
			}

			@Override
			public Object lookup(String name) {

				if ("spring/datasource".equals(name)) {
					return dataSource;
				} else if (name.startsWith("java:jboss/ee/concurrency/executor/")) {
					return fixedThreadPool();
				}

				return null;
			}

			@Override
			public NameParser getNameParser(String name) {
				return CompositeName::new;
			}

			@Override
			public void close() {
				// nothing
			}
		});
	}
}