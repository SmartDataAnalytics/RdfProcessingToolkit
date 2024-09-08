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

public class ServletLdvConfigJs extends HttpServlet implements ServletBuilder {

    private String dbEngine;

    public static ServletLdvConfigJs newBuilder() {
        return new ServletLdvConfigJs();
    }

    @Override
    public WebApplicationInitializer build(GenericWebApplicationContext rootContext) {
        return servletContext -> {
            ServletRegistration.Dynamic servlet = servletContext.addServlet("dbEngineSetting", this);
            servlet.addMapping("/dbEngineSetting");
            servlet.addMapping("/dbEngineSetting/");
            servlet.addMapping("/view/_/js2/config.js");
            servlet.setLoadOnStartup(1);
        };
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter writer = resp.getWriter();
        if ("/view/_/js2/config.js".equals(req.getServletPath())) {
            resp.setContentType("text/javascript;charset=utf-8");
            writer.println("""
(() => {
  const ldvConfig = {
    endpointUrl: '/sparql',
    endpointOptions: {
      mode: 'cors',
      credentials: 'same-origin',
      method: 'POST',
    },
    datasetBase: window.location.origin,
    exploreUrl: '@EXPLORE_URL@',
    graphLookup: '@GRAPH_LOOKUP@',
    reverseEnabled: '@SHOW_INVERSE@',
    labelLang: 'en',
    labelLangChoice: ['en', 'de', 'nl', 'fr'],
    infer: false,
    fileOnly: 'yes',
    generated: 'yes'
  }

  window.ldvConfig = ldvConfig
})()
""".replace("@SHOW_INVERSE@", "binsearch".equals(this.getDbEngine()) ? "no" : "yes"));
        } else {
            resp.setContentType(MediaType.TEXT_PLAIN);
            writer.println(this.getDbEngine());
        }
        writer.close();
    }

    public ServletBuilder setDbEngine(String dbEngine) {
        this.dbEngine = dbEngine;
        return this;
    }

    public String getDbEngine() {
        return dbEngine;
    }
}
