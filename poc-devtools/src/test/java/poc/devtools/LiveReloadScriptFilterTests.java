package poc.devtools;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.FileCopyUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LiveReloadScriptFilterTests {

	private final LiveReloadScriptFilter filter = new LiveReloadScriptFilter(1234);

	@ParameterizedTest
	@ValueSource(strings = { MediaType.TEXT_HTML_VALUE, "text/html; charset=utf-8" })
	void givenHtmlCompatibleContentTypeThenResponseShouldContainScript(String contentType) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		byte[] responseBody = "<html><head><title>test</title></head><body></body></html>"
				.getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			filterResponse.setContentType(contentType);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		this.filter.doFilter(new MockHttpServletRequest(), response, filterChain);
		assertThat(response.getContentAsString()).contains("<head><script src=\"/livereload.js?port=1234\"></script>");
	}

	@ParameterizedTest
	@ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE })
	void givenNonHtmlCompatibleContentTypeThenResponseShouldNotContainScript(String contentType) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		byte[] responseBody = "{}".getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			filterResponse.setContentType(contentType);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		this.filter.doFilter(new MockHttpServletRequest(), response, filterChain);
		assertThat(response.getContentAsString()).doesNotContain("<script src=\"/livereload.js?port=1234\"></script>");
	}

	@Test
	void givenNoContentTypeThenResponseShouldNotContainScript() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		byte[] responseBody = "test".getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		this.filter.doFilter(new MockHttpServletRequest(), response, filterChain);
		assertThat(response.getContentAsString()).doesNotContain("<script src=\"/livereload.js?port=1234\"></script>");
	}

}
