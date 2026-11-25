package poc.devtools;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Serves the LiveReload.js script. Registered with an exact mapping, so that it's available regardless of how the
 * application maps its other servlets (exact mappings take precedence over path and default mappings).
 */
class LiveReloadScriptServlet extends HttpServlet {

	static final String PATH = "/livereload.js";

	private static final String SCRIPT_LOCATION = "livereload/livereload.js";

	private final byte[] script;

	LiveReloadScriptServlet() {
		this.script = loadScript();
	}

	private static byte[] loadScript() {
		try (InputStream inputStream = new ClassPathResource(SCRIPT_LOCATION).getInputStream()) {
			return inputStream.readAllBytes();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to load LiveReload.js script from " + SCRIPT_LOCATION, ex);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/javascript;charset=UTF-8");
		response.setContentLength(this.script.length);
		response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
		response.getOutputStream().write(this.script);
	}

}
