package poc.devtools;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LiveReloadScriptInjectingFilter extends OncePerRequestFilter {

	private static final Pattern htmlHeadTagsPattern = Pattern
			.compile("<html(?![^>]*/>)[^>]*>(\\s+)?(<head(?![^>]*/>)[^>]*>)?", Pattern.CASE_INSENSITIVE);

	private final String scriptPath;

	LiveReloadScriptInjectingFilter(int liveReloadPort) {
		this.scriptPath = "/livereload.js?port=" + liveReloadPort;
	}

	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		// the response wrapped on the initial dispatch is processed once async handling completes
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		HttpServletResponse responseToUse = response;
		if (!isAsyncDispatch(request) && acceptsHtml(request)) {
			responseToUse = new HtmlContentCachingResponseWrapper(response);
		}
		filterChain.doFilter(request, responseToUse);
		if (!isAsyncStarted(request)) {
			updateResponse(request, responseToUse);
		}
	}

	private void updateResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HtmlContentCachingResponseWrapper responseWrapper = WebUtils.getNativeResponse(response,
				HtmlContentCachingResponseWrapper.class);
		if (responseWrapper != null) {
			if (isHtml(responseWrapper)) {
				injectScript(request, responseWrapper);
			}
			responseWrapper.copyBodyToResponse();
		}
	}

	private static boolean acceptsHtml(HttpServletRequest request) {
		String accept = request.getHeader(HttpHeaders.ACCEPT);
		if (accept == null) {
			return false;
		}
		try {
			// explicit text/html only - wildcards (*/*, text/*) are sent by browsers for all kinds of subresources
			return MediaType.parseMediaTypes(accept)
				.stream()
				.anyMatch((mediaType) -> MediaType.TEXT_HTML.equalsTypeAndSubtype(mediaType)
						&& mediaType.getQualityValue() > 0);
		}
		catch (InvalidMediaTypeException ex) {
			return false;
		}
	}

	private static boolean isHtml(HttpServletResponse response) {
		String contentType = response.getContentType();
		if (contentType == null) {
			return false;
		}
		try {
			return MediaType.TEXT_HTML.equalsTypeAndSubtype(MediaType.parseMediaType(contentType));
		}
		catch (InvalidMediaTypeException ex) {
			return false;
		}
	}

	private void injectScript(HttpServletRequest request, ContentCachingResponseWrapper responseWrapper)
			throws IOException {
		Charset charset = Charset.forName(responseWrapper.getCharacterEncoding());
		String content = new String(responseWrapper.getContentAsByteArray(), charset);
		Matcher matcher = htmlHeadTagsPattern.matcher(content);
		if (matcher.find()) {
			String scriptElement = "<script src=\"" + request.getContextPath() + this.scriptPath + "\"></script>";
			String modifiedContent = new StringBuilder(content).insert(matcher.end(), scriptElement).toString();
			// copyBodyToResponse sets Content-Length based on the (byte) size of the cached content
			responseWrapper.resetBuffer();
			responseWrapper.getOutputStream().write(modifiedContent.getBytes(charset));
		}
	}

	/**
	 * Caches the content of HTML responses only, while other responses are written directly to the underlying
	 * response.
	 */
	private static final class HtmlContentCachingResponseWrapper extends ContentCachingResponseWrapper {

		private boolean contentTypeResolved;

		private boolean passThrough;

		HtmlContentCachingResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			return isPassThrough() ? getResponse().getOutputStream() : super.getOutputStream();
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			return isPassThrough() ? getResponse().getWriter() : super.getWriter();
		}

		@Override
		public void flushBuffer() throws IOException {
			if (isPassThrough()) {
				getResponse().flushBuffer();
			}
		}

		// the content type is expected to be set before the body is written, so decide on first access
		private boolean isPassThrough() {
			if (!this.contentTypeResolved) {
				this.contentTypeResolved = true;
				this.passThrough = !isHtml(this);
				String contentLength = getHeader(HttpHeaders.CONTENT_LENGTH);
				if (this.passThrough && contentLength != null) {
					getResponse().setContentLengthLong(Long.parseLong(contentLength));
				}
			}
			return this.passThrough;
		}

	}

}
