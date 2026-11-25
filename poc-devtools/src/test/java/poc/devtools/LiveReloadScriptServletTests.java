package poc.devtools;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class LiveReloadScriptServletTests {

	private final LiveReloadScriptServlet servlet = new LiveReloadScriptServlet();

	@Test
	void givenGetRequestThenShouldServeScript() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.servlet.service(new MockHttpServletRequest("GET", "/livereload.js"), response);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		assertThat(response.getContentType()).isEqualTo("text/javascript;charset=UTF-8");
		assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-cache");
		assertThat(response.getContentAsByteArray()).isEqualTo(bundledScript());
		assertThat(response.getContentLength()).isEqualTo(bundledScript().length);
	}

	private static byte[] bundledScript() throws IOException {
		try (InputStream inputStream = new ClassPathResource("livereload/livereload.js").getInputStream()) {
			return inputStream.readAllBytes();
		}
	}

}
