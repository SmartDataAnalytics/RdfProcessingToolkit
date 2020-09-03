package org.aksw.sparql_integrate.cli;

import java.io.OutputStream;

// Disable derby.log file - https://stackoverflow.com/questions/1004327/getting-rid-of-derby-log
public class DerbyUtil {
    public static final OutputStream DEV_NULL = new OutputStream() {
        public void write(int b) {}
    };
}