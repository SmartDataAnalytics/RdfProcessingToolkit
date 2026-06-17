package org.aksw.sparql_integrate.cli.main;

import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import org.aksw.jenax.web.server.boot.ServletBuilder;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServletRptServerStatus extends HttpServlet implements ServletBuilder {

    private AtomicBoolean serverReady = null;

    public static ServletRptServerStatus newBuilder() {
        return new ServletRptServerStatus();
    }

    @Override
    public WebApplicationInitializer build(GenericWebApplicationContext rootContext) {
        Objects.requireNonNull(serverReady, "Ready signal was not configured");
        return servletContext -> {
            ServletRegistration.Dynamic servlet = servletContext.addServlet("rptServerStatus", this);
            servlet.addMapping("/health");
            servlet.addMapping("/health/");
            servlet.setLoadOnStartup(1);
        };
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String accept = req.getHeader("Accept");
        boolean isSse = accept != null && accept.contains("text/event-stream");

        if (false && isSse) {
            resp.setContentType("text/event-stream;charset=utf-8");
            resp.setHeader("Cache-Control", "no-cache");
            resp.setHeader("Connection", "keep-alive");

            PrintWriter writer = resp.getWriter();
            writer.print("data: " + (serverReady.get() ? "ready" : "starting") + "\n\n");
            writer.flush();
            // XXX Would need to keep the connection open for SSE
        } else {
            resp.setContentType(MediaType.TEXT_PLAIN);
            PrintWriter writer = resp.getWriter();
            writer.println(serverReady.get() ? "ready" : "starting");
            writer.close();
        }
    }

    public ServletRptServerStatus setServerReady(AtomicBoolean serverReady) {
        this.serverReady = serverReady;
        return this;
    }

    public boolean isServerReady() {
        return serverReady.get();
    }
}
