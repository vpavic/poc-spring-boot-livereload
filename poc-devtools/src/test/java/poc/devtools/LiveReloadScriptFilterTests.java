package poc.devtools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class LiveReloadScriptFilterTests {

	@ParameterizedTest
	@ValueSource(strings = { MediaType.TEXT_HTML_VALUE, "text/html; charset=utf-8" })
	void givenHtmlCompatibleContentTypeThenResponseShouldContainScript(String contentType) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.getWriter().write("<html><head><title>test</title></head><body></body></html>");
		response.setContentType(contentType);
		LiveReloadScriptFilter filter = new LiveReloadScriptFilter(1234);
		filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
		assertThat(response.getContentAsString()).endsWith("<script src=\"/livereload.js?port=1234\"></script>");
	}

	@ParameterizedTest
	@ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE })
	void givenNonHtmlCompatibleContentTypeThenResponseShouldNotContainScript(String contentType) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.getWriter().write("{}");
		response.setContentType(contentType);
		LiveReloadScriptFilter filter = new LiveReloadScriptFilter(1234);
		filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
		assertThat(response.getContentAsString()).doesNotContain("<script src=\"/livereload.js?port=1234\"></script>");
	}

	@Test
	void givenNoContentTypeThenResponseShouldNotContainScript() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.getWriter().write("test");
		LiveReloadScriptFilter filter = new LiveReloadScriptFilter(1234);
		filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
		assertThat(response.getContentAsString()).doesNotContain("<script src=\"/livereload.js?port=1234\"></script>");
	}

	@Test
	void givenResponseWriterAccessNotAllowedThenResponseShouldNotContainScript() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setWriterAccessAllowed(false);
		response.setContentType(MediaType.TEXT_HTML_VALUE);
		LiveReloadScriptFilter filter = new LiveReloadScriptFilter(1234);
		filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
		assertThat(response.getContentAsString()).doesNotContain("<script src=\"/livereload.js?port=1234\"></script>");
	}

}
