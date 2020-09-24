package org.aksw.sparql_integrate.cli.main;

/**
 * Depending on the arguments the determined main output type is among this enum.
 * See also the detectOutputMode method.
 *
 * @author raven
 *
 */
public enum OutputMode
{
    UNKOWN,
    QUAD,
    TRIPLE,
    BINDING,
    JSON
}