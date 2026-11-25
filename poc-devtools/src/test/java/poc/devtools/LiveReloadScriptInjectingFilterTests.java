package poc.devtools;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LiveReloadScriptInjectingFilterTests {

	private static final String SCRIPT_ELEMENT = "<script src=\"/livereload.js?port=1234\"></script>";

	private static final String HTML = "<!DOCTYPE html><html lang=\"en\"><head><title>test</title></head><body></body></html>";

	private final LiveReloadScriptInjectingFilter filter = new LiveReloadScriptInjectingFilter(1234);

	@ParameterizedTest
	@ValueSource(strings = { "<html><head><title>test</title></head><body></body></html>",
			"<html>\n\t<head><title>test</title></head><body></body></html>",
			"<html><head with=\"attribute\"><title>test</title></head><body></body></html>",
			"<html><HEAD><title>test</title></head><body></body></html>",
			"<html><title>test</title><body></body></html>" })
	void givenValidHtmlThenShouldInjectScriptElement(String responseBody) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilter(htmlRequest(), response, writing(MediaType.TEXT_HTML_VALUE, responseBody));
		assertThat(response.getContentLength()).isEqualTo(responseBody.length() + SCRIPT_ELEMENT.length());
		assertThat(response.getContentAsString()).containsOnlyOnce(SCRIPT_ELEMENT);
	}

	@ParameterizedTest
	@ValueSource(strings = { "text/html, application/xhtml+xml, */*;q=0.8",
			"text/html;q=0.9, application/json" })
	void givenBrowserNavigationAcceptHeaderThenShouldInjectScriptElement(String accept) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.ACCEPT, accept);
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilter(request, response, writing(MediaType.TEXT_HTML_VALUE, HTML));
		assertThat(response.getContentAsString()).containsOnlyOnce(SCRIPT_ELEMENT);
	}

	@ParameterizedTest
	@ValueSource(strings = { "*/*", "text/*", "image/avif,image/webp,*/*;q=0.8", "application/json", "text/html;q=0",
			"not a media type" })
	void givenRequestNotExplicitlyAcceptingHtmlThenShouldNotInjectScriptElement(String accept) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.ACCEPT, accept);
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilter(request, response, writing(MediaType.TEXT_HTML_VALUE, HTML));
		assertThat(response.getContentAsString()).isEqualTo(HTML);
	}

	@Test
	void givenNoAcceptHeaderThenShouldNotInjectScriptElement() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilter(new MockHttpServletRequest(), response, writing(MediaType.TEXT_HTML_VALUE, HTML));
		assertThat(response.getContentAsString()).isEqualTo(HTML);
	}

	@ParameterizedTest
	@ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XHTML_XML_VALUE })
	void givenIncompatibleContentTypeThenShouldNotInjectScriptElement(String contentType) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilter(htmlRequest(), response, writing(contentType, HTML));
		assertThat(response.getContentLength()).isEqualTo(HTML.length());
		assertThat(response.getContentAsString()).isEqualTo(HTML);
	}

	@Test
	void givenNoContentTypeThenShouldNotInjectScriptElement() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilter(htmlRequest(), response, writing(null, HTML));
		assertThat(response.getContentLength()).isEqualTo(HTML.length());
		assertThat(response.getContentAsString()).isEqualTo(HTML);
	}

	@Test
	void givenHtmlResponseThenShouldBufferContent() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			writing(MediaType.TEXT_HTML_VALUE, HTML).doFilter(filterRequest, filterResponse);
			filterResponse.flushBuffer();
			assertThat(response.getContentAsString()).isEmpty();
			assertThat(response.isCommitted()).isFalse();
		};
		this.filter.doFilter(htmlRequest(), response, filterChain);
		assertThat(response.getContentAsString()).containsOnlyOnce(SCRIPT_ELEMENT);
	}

	@ParameterizedTest
	@ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE })
	void givenNonHtmlResponseThenShouldWriteContentDirectly(String contentType) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			writing(contentType, HTML).doFilter(filterRequest, filterResponse);
			assertThat(response.getContentAsString()).isEqualTo(HTML);
			assertThat(response.getContentLength()).isEqualTo(HTML.length());
			filterResponse.flushBuffer();
			assertThat(response.isCommitted()).isTrue();
		};
		this.filter.doFilter(htmlRequest(), response, filterChain);
		assertThat(response.getContentAsString()).isEqualTo(HTML);
	}

	@Test
	void givenInvalidHtmlThenShouldNotInjectScriptElement() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		String responseBody = "<!DOCTYPE html><head></head><body></body>";
		this.filter.doFilter(htmlRequest(), response, writing(MediaType.TEXT_HTML_VALUE, responseBody));
		assertThat(response.getContentLength()).isEqualTo(responseBody.length());
		assertThat(response.getContentAsString()).isEqualTo(responseBody);
	}

	@Test
	void givenContextPathThenScriptElementShouldIncludeIt() throws Exception {
		MockHttpServletRequest request = htmlRequest();
		request.setContextPath("/ctx");
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilter(request, response, writing(MediaType.TEXT_HTML_VALUE, HTML));
		assertThat(response.getContentAsString())
			.containsOnlyOnce("<head><script src=\"/ctx/livereload.js?port=1234\"></script>");
	}

	@Test
	void givenNonAsciiUtf8ContentThenContentLengthShouldMatchBytes() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		String responseBody = "<html><head><title>Čćžšđ €</title></head><body>日本語</body></html>";
		this.filter.doFilter(htmlRequest(), response, writing("text/html;charset=UTF-8", responseBody));
		String expected = responseBody.replace("<head>", "<head>" + SCRIPT_ELEMENT);
		assertThat(response.getContentAsString()).isEqualTo(expected);
		assertThat(response.getContentLength()).isEqualTo(expected.getBytes(StandardCharsets.UTF_8).length);
		assertThat(response.getContentAsByteArray()).hasSize(response.getContentLength());
	}

	@Test
	void givenNonUtf8CharsetThenShouldPreserveContentEncoding() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		String responseBody = "<html><head><title>Café</title></head><body>Žluťoučký</body></html>";
		this.filter.doFilter(htmlRequest(), response, writing("text/html;charset=ISO-8859-2", responseBody));
		String expected = responseBody.replace("<head>", "<head>" + SCRIPT_ELEMENT);
		assertThat(response.getContentAsString()).isEqualTo(expected);
		assertThat(response.getContentLength()).isEqualTo(expected.length());
	}

	@Test
	void givenAsyncRequestThenShouldInjectScriptElementOnAsyncDispatch() throws Exception {
		MockHttpServletRequest request = htmlRequest();
		request.setAsyncSupported(true);
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<ServletResponse> asyncResponse = new AtomicReference<>();
		// initial dispatch: start async processing without writing the body (like Spring MVC's DeferredResult)
		FilterChain startAsync = (filterRequest, filterResponse) -> {
			StandardServletAsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(request,
					(HttpServletResponse) filterResponse);
			WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
			asyncManager.setAsyncWebRequest(asyncWebRequest);
			asyncWebRequest.startAsync();
			asyncResponse.set(request.getAsyncContext().getResponse());
		};
		this.filter.doFilter(request, response, startAsync);
		assertThat(response.isCommitted()).isFalse();
		assertThat(response.getContentAsString()).isEmpty();
		// async dispatch: the container passes the response given to startAsync
		request.setAsyncStarted(false);
		request.setDispatcherType(DispatcherType.ASYNC);
		this.filter.doFilter(request, asyncResponse.get(), writing(MediaType.TEXT_HTML_VALUE, HTML));
		assertThat(response.getContentAsString()).containsOnlyOnce("<head>" + SCRIPT_ELEMENT);
		assertThat(response.getContentLength()).isEqualTo(HTML.length() + SCRIPT_ELEMENT.length());
	}

	private static MockHttpServletRequest htmlRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE);
		return request;
	}

	private static FilterChain writing(String contentType, String body) {
		return (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			if (contentType != null) {
				filterResponse.setContentType(contentType);
			}
			String charset = filterResponse.getCharacterEncoding();
			filterResponse.setContentLength(body.getBytes(charset).length);
			filterResponse.getWriter().write(body);
		};
	}

}
