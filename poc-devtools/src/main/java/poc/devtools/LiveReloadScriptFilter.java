package poc.devtools;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

class LiveReloadScriptFilter extends OncePerRequestFilter {

	private final String scriptSnippet;

	LiveReloadScriptFilter(int liveReloadPort) {
		this.scriptSnippet = String.format("<script src=\"/livereload.js?port=%d\"></script>", liveReloadPort);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		ContentCachingResponseWrapper responseToUse = new ContentCachingResponseWrapper(response);
		filterChain.doFilter(request, responseToUse);
		String contentType = responseToUse.getContentType();
		if ((contentType != null) && MediaType.TEXT_HTML.isCompatibleWith(MediaType.parseMediaType(contentType))) {
			String content = new String(responseToUse.getContentAsByteArray(), StandardCharsets.UTF_8);
			String modifiedContent = content.replaceFirst("<head>", "<head>" + scriptSnippet);
			response.setContentLength(modifiedContent.length());
			response.getWriter().write(modifiedContent);
		}
		else {
			responseToUse.copyBodyToResponse();
		}
	}

}
