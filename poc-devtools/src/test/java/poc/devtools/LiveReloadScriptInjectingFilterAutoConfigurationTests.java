package poc.devtools;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.autoconfigure.DevToolsProperties;
import org.springframework.boot.devtools.restart.Restarter;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;

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

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(DevToolsProperties.class)
	static class DevToolsPropertiesConfiguration {

	}

}
