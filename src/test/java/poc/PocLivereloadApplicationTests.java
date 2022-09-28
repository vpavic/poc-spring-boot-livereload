package poc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PocLivereloadApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void htmlContainsLiveReloadScript() {
		String html = this.restTemplate.getForEntity("/", String.class).getBody();
		assertThat(html).contains("<script src=\"/webjars/livereload-js/dist/livereload.js?port=8080\">");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class Config {

		@Bean
		LiveReloadScriptFilter liveReloadScriptFilter() {
			return new LiveReloadScriptFilter(8080);
		}

	}

}
