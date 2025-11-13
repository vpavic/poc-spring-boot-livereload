package poc.devtools;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.devtools.autoconfigure.DevToolsProperties;
import org.springframework.boot.devtools.autoconfigure.LocalDevToolsAutoConfiguration;
import org.springframework.boot.devtools.restart.ConditionalOnInitializedRestarter;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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

	@Configuration(proxyBeanMethods = false)
	static class LiveReloadConfiguration implements WebMvcConfigurer {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/livereload.*js").addResourceLocations("classpath:/livereload/");
		}

	}

}
