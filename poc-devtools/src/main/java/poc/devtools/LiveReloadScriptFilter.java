package poc.devtools;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

class LiveReloadScriptFilter extends OncePerRequestFilter {

	private final String scriptSnippet;

	LiveReloadScriptFilter(int liveReloadPort) {
		this.scriptSnippet = String.format("<script src=\"/livereload.js?port=%d\"></script>", liveReloadPort);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		filterChain.doFilter(request, response);
		String contentType = response.getContentType();
		if ((contentType != null) && MediaType.TEXT_HTML.isCompatibleWith(MediaType.parseMediaType(contentType))) {
			response.getWriter().write(this.scriptSnippet);
		}
	}

}
