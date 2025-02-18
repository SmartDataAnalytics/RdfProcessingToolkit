package org.aksw.sparql_integrate.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import org.aksw.jenax.web.server.boot.ServletBuilder;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.GenericWebApplicationContext;

import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ServletGraphQlSchema extends HttpServlet implements ServletBuilder {

    protected String content;
    protected String contentType;

    public static ServletGraphQlSchema newBuilder() {
        return new ServletGraphQlSchema();
    }

    @Override
    public WebApplicationInitializer build(GenericWebApplicationContext rootContext) {
        return servletContext -> {
            ServletRegistration.Dynamic servlet = servletContext.addServlet("conf_graphql", this);
            servlet.addMapping("/conf/graphql");
            servlet.addMapping("/conf/graphql/");
            // servlet.addMapping("/view/_/js2/config.js");
            servlet.setLoadOnStartup(1);
        };
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (contentType != null) {
            resp.setContentType(contentType);
        }

        try (PrintWriter writer = resp.getWriter()) {
    //        if ("/view/_/js2/config.js".equals(req.getServletPath())) {
    //            resp.setContentType("text/javascript;charset=utf-8");
    //            writer.println("""
    //        } else {
    //            resp.setContentType(MediaType.TEXT_PLAIN);
    //            writer.println(this.getDbEngine());
    //        }
            writer.println(content);
            writer.flush();
        }
    }

    public ServletGraphQlSchema setContent(String content) {
        this.content = content;
        return this;
    }

    public ServletGraphQlSchema setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }
}
