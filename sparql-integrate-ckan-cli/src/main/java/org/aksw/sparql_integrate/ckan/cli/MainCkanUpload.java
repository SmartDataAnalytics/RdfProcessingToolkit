package org.aksw.sparql_integrate.ckan.cli;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.aksw.sparql_integrate.ckan.core.CkanRdfDatasetProcessor;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import eu.trentorise.opendata.jackan.CkanClient;

public class MainCkanUpload {
	@Configuration
	public static class ConfigSparqlIntegrate {

		@Bean
		public ApplicationRunner applicationRunner() {
			return args -> {
				String apiKey = Optional.ofNullable(args.getOptionValues("apiKey")).orElse(Collections.emptyList()).stream().findFirst()
						.orElseThrow(() -> new RuntimeException("API key must be given"));

				String host = Optional.ofNullable(args.getOptionValues("host")).orElse(Collections.emptyList()).stream().findFirst()
						.orElseThrow(() -> new RuntimeException("Host must be given"));
				
		        CkanClient ckanClient = new CkanClient(host, apiKey);

		        Collection<String> datasetSources = Optional.ofNullable(args.getNonOptionArgs())
		        		.orElseThrow(() -> new RuntimeException("No dataset sources specified"));
		        
		        for(String datasetSource : datasetSources) {
		        	Dataset dataset = RDFDataMgr.loadDataset(datasetSource);
		        
		        	CkanRdfDatasetProcessor processor = new CkanRdfDatasetProcessor(ckanClient);
		        	processor.process(dataset);
		        }
			};
		}
	}

	public static void main(String[] args) {
		try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder().sources(ConfigSparqlIntegrate.class)
				.bannerMode(Banner.Mode.OFF)
				// If true, Desktop.isDesktopSupported() will return false, meaning we can't
				// launch a browser
				.headless(false).web(false).run(args)) {
		}
	}
}

//List<String> ds = ckanClient.getDatasetList(10, 0);
//
//for (String s : ds) {
//	System.out.println();
//	System.out.println("DATASET: " + s);
//	CkanDataset d = ckanClient.getDataset(s);
//	System.out.println("  RESOURCES:");
//	for (CkanResource r : d.getResources()) {
//		System.out.println("    " + r.getName());
//		System.out.println("    FORMAT: " + r.getFormat());
//		System.out.println("       URL: " + r.getUrl());
//	}
//}

