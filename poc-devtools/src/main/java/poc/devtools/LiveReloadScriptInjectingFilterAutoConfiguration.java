package poc.devtools;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.devtools.autoconfigure.DevToolsProperties;
import org.springframework.boot.devtools.autoconfigure.LocalDevToolsAutoConfiguration;
import org.springframework.boot.devtools.restart.ConditionalOnInitializedRestarter;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = LocalDevToolsAutoConfiguration.class)
@ConditionalOnInitializedRestarter
@ConditionalOnProperty(prefix = "spring.devtools.livereload", name = "enabled", matchIfMissing = true)
@ConditionalOnWebApplication(type = Type.SERVLET)
class LiveReloadScriptInjectingFilterAutoConfiguration {

	@Bean
	@RestartScope
	LiveReloadScriptInjectingFilter liveReloadScriptInjectingFilter(DevToolsProperties properties) {
		return new LiveReloadScriptInjectingFilter(properties.getLivereload().getPort());
	}

	@Bean
	ServletRegistrationBean<LiveReloadScriptServlet> liveReloadScriptServletRegistration() {
		ServletRegistrationBean<LiveReloadScriptServlet> registration = new ServletRegistrationBean<>(
				new LiveReloadScriptServlet(), LiveReloadScriptServlet.PATH);
		registration.setName("liveReloadScriptServlet");
		return registration;
	}

}
