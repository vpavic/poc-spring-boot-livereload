package poc.devtools;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

class LiveReloadScriptFilter extends OncePerRequestFilter {

	private final String scriptSnippet;

	LiveReloadScriptFilter(int liveReloadPort) {
		this.scriptSnippet = String.format(
				"<script src=\"/webjars/livereload-js/dist/livereload.js?port=%d\"></script>", liveReloadPort);
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
