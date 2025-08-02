package org.aksw.sparql_integrate.cli.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import io.github.classgraph.ScanResult;

public class ClassPathResourceResolver {

    /** Try to resolve the glob to a list of resources on the class path. */
    public static List<String> resolve(String glob) {
        // Replace all unescaped backslashes
        // TODO Fix pattern
        String normalized = glob.replace('\\', '/');

        String prefix = longestLiteralPrefix(normalized);
        // Remove leading slashes from the prefix because ClassGraph resources don't start with a slash.
        normalized = normalized.replaceAll("^//+", "");

        List<String> result = new ArrayList<>();
        try (ScanResult scanResult = new ClassGraph().acceptPaths(prefix).scan()) {
            ResourceList resourceList;
            try {
                resourceList = scanResult.getResourcesMatchingWildcard(normalized);
                //ResourceList resourceList = scanResult.getAllResources();
                for (Resource r : resourceList) {
                    result.add(r.getPath());
                }
            } catch (PatternSyntaxException e) {
                // Invalid glob pattern - don't match anything.
            }
        }
        Collections.sort(result);
        return result;
    }

    /** Longest literal (meta-free) prefix of a Unix-style glob. */
    public static String longestLiteralPrefix(String glob) {
        if (glob == null) {
            throw new NullPointerException("glob");
        }

        int cut = StringUtils.indexOfAny(glob, '*', '?', '[');
        if (cut == -1) {
            cut = glob.length();
        }

        // Back up over any trailing slashes.
        int end = cut;
        while (end > 0 && glob.charAt(end - 1) == '/') {
            end--;
        }
        String prefix = glob.substring(0, end);
        return prefix;
    }

}
