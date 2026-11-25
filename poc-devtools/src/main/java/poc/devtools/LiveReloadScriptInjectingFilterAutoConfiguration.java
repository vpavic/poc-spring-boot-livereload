package poc.devtools;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.devtools.autoconfigure.DevToolsProperties;
import org.springframework.boot.devtools.autoconfigure.LocalDevToolsAutoConfiguration;
import org.springframework.boot.devtools.restart.ConditionalOnInitializedRestarter;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;

@AutoConfiguration(after = LocalDevToolsAutoConfiguration.class)
@ConditionalOnInitializedRestarter
@ConditionalOnBooleanProperty("spring.devtools.livereload.enabled")
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(OncePerRequestFilter.class)
class LiveReloadScriptInjectingFilterAutoConfiguration {

	@Bean
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
