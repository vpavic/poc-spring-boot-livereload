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

class LiveReloadScriptInjectingFilterTests {

	private static final String SCRIPT_ELEMENT = "<script src=\"/livereload.js?port=1234\"></script>";

	private final LiveReloadScriptInjectingFilter filter = new LiveReloadScriptInjectingFilter(1234);

	@ParameterizedTest
	@ValueSource(strings = { MediaType.TEXT_HTML_VALUE, "text/html; charset=utf-8" })
	void givenHtmlCompatibleContentTypeThenShouldInjectScriptElement(String contentType) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		byte[] responseBody = "<!DOCTYPE html><html lang=\"en\"><head><title>test</title></head><body></body></html>"
				.getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			filterResponse.setContentType(contentType);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		this.filter.doFilter(new MockHttpServletRequest(), response, filterChain);
		assertThat(response.getContentLength()).isEqualTo(responseBody.length + SCRIPT_ELEMENT.length());
		assertThat(response.getContentAsString()).containsOnlyOnce("<head>" + SCRIPT_ELEMENT);
	}

	@ParameterizedTest
	@ValueSource(strings = { "<html><head><title>test</title></head><body></body></html>",
			"<html>\n\t<head><title>test</title></head><body></body></html>",
			"<html><head with=\"attribute\"><title>test</title></head><body></body></html>",
			"<html><HEAD><title>test</title></head><body></body></html>",
			"<html><title>test</title><body></body></html>" })
	void givenValidHtmlThenShouldInjectScriptElement(String htmlContent) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		byte[] responseBody = htmlContent.getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			filterResponse.setContentType(MediaType.TEXT_HTML_VALUE);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		this.filter.doFilter(new MockHttpServletRequest(), response, filterChain);
		assertThat(response.getContentLength()).isEqualTo(responseBody.length + SCRIPT_ELEMENT.length());
		assertThat(response.getContentAsString()).containsOnlyOnce(SCRIPT_ELEMENT);
	}

	@ParameterizedTest
	@ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE })
	void givenIncompatibleContentTypeThenShouldNotInjectScriptElement(String contentType) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		byte[] responseBody = "{}".getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			filterResponse.setContentType(contentType);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		this.filter.doFilter(new MockHttpServletRequest(), response, filterChain);
		assertThat(response.getContentLength()).isEqualTo(responseBody.length);
		assertThat(response.getContentAsString()).doesNotContain(SCRIPT_ELEMENT);
	}

	@Test
	void givenNoContentTypeThenShouldNotInjectScriptElement() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		byte[] responseBody = "test".getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		this.filter.doFilter(new MockHttpServletRequest(), response, filterChain);
		assertThat(response.getContentLength()).isEqualTo(responseBody.length);
		assertThat(response.getContentAsString()).doesNotContain(SCRIPT_ELEMENT);
	}

	@Test
	void givenInvalidHtmlThenShouldNotInjectScriptElement() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		byte[] responseBody = "<!DOCTYPE html><head></head><body></body>".getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			filterResponse.setContentType(MediaType.TEXT_HTML_VALUE);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		this.filter.doFilter(new MockHttpServletRequest(), response, filterChain);
		assertThat(response.getContentLength()).isEqualTo(responseBody.length);
		assertThat(response.getContentAsString()).doesNotContain(SCRIPT_ELEMENT);
	}

}
