package org.aksw.sparql_integrate.ngs.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.aksw.jena_sparql_api.io.binseach.BoyerMooreMatcherFactory;
import org.aksw.jena_sparql_api.io.binseach.PageManager;
import org.aksw.jena_sparql_api.io.binseach.PageManagerForFileChannel;
import org.aksw.jena_sparql_api.io.binseach.PageNavigator;
import org.aksw.jena_sparql_api.io.binseach.SeekableMatcher;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.apache.jena.riot.Lang;

public class MainPlaygroundScanTrig {

	public static void main(String[] args) throws IOException {
		Path path = Paths.get("/home/raven/Projects/Eclipse/sparql-integrate-parent/ngs/one-week.trig");

		//PageManager pageManager = new PageManagerForByteBuffer(ByteBuffer.wrap(text));
		try(FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {
			PageManager pageManager = PageManagerForFileChannel.create(fileChannel);

			PageNavigator nav = new PageNavigator(pageManager);
			
			// TODO Matcher needs support for horizon argument (maximum number of bytes to scan)
			SeekableMatcher findOpenCurlyBrace = BoyerMooreMatcherFactory.createFwd("{".getBytes()).newMatcher();
			SeekableMatcher findOpenAngularBracket = BoyerMooreMatcherFactory.createBwd("<".getBytes()).newMatcher();			
			
			while(findOpenCurlyBrace.find(nav)) {
				long candPos = nav.getPos();
				
				// TODO Matchers should probably return the relative position so
				// that we can seek without requiring the notion of an absolute position
				// if((relPos = matcher.find(nav)) != -1)  
				if(findOpenAngularBracket.find(nav)) {
				
					InputStream in = Channels.newInputStream(nav);
					int maxCount = 100;
					long count = RDFDataMgrRx.createFlowableQuads(() -> in, Lang.TRIG, null)
						.limit(maxCount)
						.count()
						.blockingGet();
				
					if(count != 0) {
						System.out.println("At pos " + candPos + " found " + count + "/" + maxCount + " quads");
					}
					nav.setPos(candPos + 1);
				}

			}
		}

	}
}
