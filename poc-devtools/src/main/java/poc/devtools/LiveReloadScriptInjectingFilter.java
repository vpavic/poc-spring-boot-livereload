package poc.devtools;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

class LiveReloadScriptInjectingFilter extends OncePerRequestFilter {

	private static final Pattern htmlHeadTagsPattern = Pattern
			.compile("<html(?![^>]*/>)[^>]*>(\\s+)?(<head(?![^>]*/>)[^>]*>)?", Pattern.CASE_INSENSITIVE);

	private final String scriptElement;

	LiveReloadScriptInjectingFilter(int liveReloadPort) {
		this.scriptElement = String.format("<script src=\"/livereload.js?port=%d\"></script>", liveReloadPort);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		HttpServletResponse responseToUse = response;
		if (shouldWrapRequest(request)) {
			responseToUse = new ContentCachingResponseWrapper(response);
		}
		filterChain.doFilter(request, responseToUse);
		if (responseToUse instanceof ContentCachingResponseWrapper responseWrapper) {
			if (shouldInjectScript(responseToUse)) {
				String content = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
				String modifiedContent = htmlHeadTagsPattern.matcher(content)
						.replaceFirst((matchResult) -> matchResult.group() + this.scriptElement);
				if (!modifiedContent.equals(content)) {
					response.setContentLength(modifiedContent.length());
					response.getWriter().write(modifiedContent);
					return;
				}
			}
			responseWrapper.copyBodyToResponse();
		}
	}

	private boolean shouldWrapRequest(HttpServletRequest request) {
		String contentType = request.getHeader(HttpHeaders.ACCEPT);
		return (contentType != null) && MediaType.TEXT_HTML.isCompatibleWith(MediaType.parseMediaType(contentType));
	}

	private boolean shouldInjectScript(HttpServletResponse response) {
		String contentType = response.getContentType();
		return (contentType != null) && MediaType.TEXT_HTML.isCompatibleWith(MediaType.parseMediaType(contentType));
	}

}
