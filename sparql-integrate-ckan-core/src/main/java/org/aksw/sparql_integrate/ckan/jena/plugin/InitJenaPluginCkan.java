package org.aksw.sparql_integrate.ckan.jena.plugin;

import org.apache.jena.system.JenaSubsystemLifecycle;

public class InitJenaPluginCkan
	implements JenaSubsystemLifecycle {

	public void start() {
		JenaPluginCkan.init();
	}

	@Override
	public void stop() {
	}
}
