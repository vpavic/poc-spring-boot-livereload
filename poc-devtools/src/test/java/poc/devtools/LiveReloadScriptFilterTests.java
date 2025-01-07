package poc.devtools;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class LiveReloadScriptFilterTests {

	@Test
	void htmlContainsLiveReloadScript() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.getWriter().write("<html><head><title>test</title></head><body></body></html>");
		response.setContentType(MediaType.TEXT_HTML_VALUE);
		LiveReloadScriptFilter filter = new LiveReloadScriptFilter(1234);
		filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
		assertThat(response.getContentAsString()).endsWith("<script src=\"/livereload.js?port=1234\"></script>");
	}

}
