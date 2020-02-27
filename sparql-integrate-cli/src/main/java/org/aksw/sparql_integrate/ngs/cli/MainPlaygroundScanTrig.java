package org.aksw.sparql_integrate.ngs.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aksw.jena_sparql_api.io.binseach.CharSequenceFromSeekable;
import org.aksw.jena_sparql_api.io.binseach.PageManager;
import org.aksw.jena_sparql_api.io.binseach.PageManagerForFileChannel;
import org.aksw.jena_sparql_api.io.binseach.PageNavigator;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.apache.jena.riot.Lang;

public class MainPlaygroundScanTrig {

	public static void main(String[] args) throws IOException {
		// Pattern to find the start of named graphs in trig
		Pattern trigStartPattern = Pattern.compile("@base|@prefix|(graph)?\\s*(<[^>]*>|_:[^-\\s])\\s*\\{");

		
		Path path = Paths.get("/home/raven/Projects/Eclipse/sparql-integrate-parent/ngs/one-week.trig");

		//PageManager pageManager = new PageManagerForByteBuffer(ByteBuffer.wrap(text));
		try(FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {
			PageManager pageManager = PageManagerForFileChannel.create(fileChannel);
			PageNavigator nav = new PageNavigator(pageManager);

			CharSequence charSequence = new CharSequenceFromSeekable(nav);
			
			Matcher m = trigStartPattern.matcher(charSequence);
			int matchCount = 0;
			while(m.find() && matchCount < 5) {
				int start = m.start();
				nav.nextPos(start);

				long absPos = nav.getPos();
				
				PageNavigator clonedNav = nav.clone();
				InputStream in = Channels.newInputStream(clonedNav);
				int maxQuadCount = 100;
				long quadCount = RDFDataMgrRx.createFlowableQuads(() -> in, Lang.TRIG, null)
					.limit(maxQuadCount)
					.count()
					.blockingGet();
			
				if(quadCount != 0) {
					++matchCount;
					System.out.println("Candidate start pos " + absPos + " yeld " + quadCount + "/" + maxQuadCount + " quads");
				}

			}

			System.out.println("Ended at " + nav.getPos());
		}

	}
}
