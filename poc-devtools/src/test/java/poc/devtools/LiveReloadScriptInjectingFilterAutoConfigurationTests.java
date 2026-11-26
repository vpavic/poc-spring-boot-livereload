package poc.devtools;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.autoconfigure.DevToolsProperties;
import org.springframework.boot.devtools.restart.Restarter;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

class LiveReloadScriptInjectingFilterAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(LiveReloadScriptInjectingFilterAutoConfiguration.class))
		.withUserConfiguration(DevToolsPropertiesConfiguration.class);

	@BeforeEach
	void initializeRestarter() {
		Restarter.initialize(new String[0], false, (thread) -> new URL[0], false);
	}

	@AfterEach
	void clearRestarter() {
		Restarter.clearInstance();
	}

	@Test
	void givenLiveReloadEnabledThenShouldConfigureFilterAndScriptServlet() {
		this.contextRunner.withPropertyValues("spring.devtools.livereload.enabled=true").run((context) -> {
			assertThat(context).hasSingleBean(LiveReloadScriptInjectingFilter.class);
			String[] names = context.getBeanNamesForType(
					ResolvableType.forClassWithGenerics(ServletRegistrationBean.class, LiveReloadScriptServlet.class));
			assertThat(names).hasSize(1);
			ServletRegistrationBean<?> registration = context.getBean(names[0], ServletRegistrationBean.class);
			assertThat(registration.getServlet()).isInstanceOf(LiveReloadScriptServlet.class);
			assertThat(registration.getUrlMappings()).containsExactly("/livereload.js");
		});
	}

	@Test
	void givenCustomLiveReloadPortThenShouldUseItInScriptElement() {
		this.contextRunner
			.withPropertyValues("spring.devtools.livereload.enabled=true", "spring.devtools.livereload.port=1234")
			.run((context) -> {
				MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
				request.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE);
				MockHttpServletResponse response = new MockHttpServletResponse();
				FilterChain filterChain = (filterRequest, filterResponse) -> {
					filterResponse.setContentType(MediaType.TEXT_HTML_VALUE);
					filterResponse.getWriter().write("<html><head></head></html>");
				};
				context.getBean(LiveReloadScriptInjectingFilter.class).doFilter(request, response, filterChain);
				assertThat(response.getContentAsString())
					.isEqualTo("<html><head><script src=\"/livereload.js?port=1234\"></script></head></html>");
			});
	}

	@Test
	void givenLiveReloadNotConfiguredThenShouldNotConfigureFilter() {
		this.contextRunner.run((context) -> assertThat(context)
			.doesNotHaveBean(LiveReloadScriptInjectingFilterAutoConfiguration.class));
	}

	@Test
	void givenRestarterNotInitializedThenShouldNotConfigureFilter() {
		Restarter.clearInstance();
		this.contextRunner.withPropertyValues("spring.devtools.livereload.enabled=true")
			.run((context) -> assertThat(context)
				.doesNotHaveBean(LiveReloadScriptInjectingFilterAutoConfiguration.class));
	}

	@Test
	void givenReactiveWebApplicationThenShouldNotConfigureFilter() {
		new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LiveReloadScriptInjectingFilterAutoConfiguration.class))
			.withUserConfiguration(DevToolsPropertiesConfiguration.class)
			.withPropertyValues("spring.devtools.livereload.enabled=true")
			.run((context) -> assertThat(context)
				.doesNotHaveBean(LiveReloadScriptInjectingFilterAutoConfiguration.class));
	}

	@Test
	void givenSpringWebNotPresentThenShouldNotConfigureFilter() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(OncePerRequestFilter.class))
			.withPropertyValues("spring.devtools.livereload.enabled=true")
			.run((context) -> assertThat(context)
				.doesNotHaveBean(LiveReloadScriptInjectingFilterAutoConfiguration.class));
	}

	@Test
	void givenSpringMvcNotPresentThenShouldConfigureFilter() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("org.springframework.web.servlet"))
			.withPropertyValues("spring.devtools.livereload.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(LiveReloadScriptInjectingFilter.class));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(DevToolsProperties.class)
	static class DevToolsPropertiesConfiguration {

	}

}
