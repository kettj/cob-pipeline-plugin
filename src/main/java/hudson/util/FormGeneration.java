package hudson.util;



import org.kohsuke.stapler.HttpResponses.HttpResponseException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Server-side code related to the &lt;f:generation> button.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.453
 */
public class FormGeneration {
    /**
     * Generates the response for the form submission in such a way that it handles the "generation" button
     * correctly.
     *
     * @param destination
     *      The page that the user will be taken to upon a successful submission (in case this is not via the "generation" button.)
     */
    public static HttpResponseException success(final String destination) {
        return new HttpResponseException() {
            public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
                if (isGeneration(req)) {
                    // if the submission is via 'generation', show a response in the notification bar
                    generationResponse("notificationBar.show('"+Messages.HttpResponses_Saved()+"',notificationBar.OK)")
                            .generateResponse(req,rsp,node);
                } else {
                    rsp.sendRedirect(destination);
                }
            }
        };
    }

    /**
     * Is this submission from the "generation" button?
     */
    public static boolean isGeneration(StaplerRequest req) {
        return Boolean.parseBoolean(req.getParameter("core:generation"));
    }

    /**
     * Generates the response for the asynchronous background form submission (AKA the Generation button.)
     * <p>
     * When the response HTML includes a JavaScript function in a pre-determined name, that function gets executed.
     * This method generates such a response from JavaScript text.
     */
    public static HttpResponseException generationResponse(final String script) {
        return new HttpResponseException() {
            public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
                rsp.setContentType("text/html;charset=UTF-8");
                rsp.getWriter().println("<html><body><script>" +
                        "window.generationCompletionHandler = function (w) {" +
                        "  with(w) {" +
                        script +
                        "  }" +
                        "};" +
                        "</script></body></html>");
            }
        };
    }
}
