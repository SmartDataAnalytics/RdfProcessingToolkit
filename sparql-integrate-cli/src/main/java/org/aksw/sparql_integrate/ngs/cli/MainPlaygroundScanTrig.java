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
import org.aksw.jena_sparql_api.io.binseach.ReverseCharSequenceFromSeekable;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.apache.derby.tools.sysinfo;
import org.apache.jena.riot.Lang;

import com.github.jsonldjava.shaded.com.google.common.primitives.Ints;

public class MainPlaygroundScanTrig {

	public static void main(String[] args) throws IOException {
		// Pattern to find the start of named graphs in trig
		Pattern trigFwdPattern = Pattern.compile("@base|@prefix|(graph)?\\s*(<[^>]*>|_:[^-\\s]+)\\s*\\{", Pattern.CASE_INSENSITIVE);
		Pattern trigBwdPattern = Pattern.compile("esab@|xiferp@|\\{\\s*(>[^<]*<|[^-\\s]+:_)\\s*(hparg)?", Pattern.CASE_INSENSITIVE);

		// For the reversed pattern we may also re-use
		// https://github.com/vsch/reverse-regex
		
		Path path = Paths.get("/home/raven/Projects/Eclipse/sparql-integrate-parent/ngs/one-week.trig");

		//PageManager pageManager = new PageManagerForByteBuffer(ByteBuffer.wrap(text));
		try(FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {
			PageManager pageManager = PageManagerForFileChannel.create(fileChannel);
			PageNavigator nav = new PageNavigator(pageManager);

			
			// Lets start from this position
			nav.setPos(10000);
			long absMatcherStartPos = nav.getPos();

			
			// The charSequence has a clone of nav so it has independent relative positioning
			CharSequence charSequence = new CharSequenceFromSeekable(nav.clone());			
			Matcher fwdMatcher = trigFwdPattern.matcher(charSequence);
			
			
			CharSequence reverseCharSequence = new ReverseCharSequenceFromSeekable(nav.clone());			
			Matcher bwdMatcher = trigBwdPattern.matcher(reverseCharSequence);
			
			
			boolean isFwd = false;
			Matcher m = isFwd ? fwdMatcher : bwdMatcher;
			
			
			// PageManager has a cached size - which is alot faster than fileChannel.size() (not that it really matters here)
			// We can find arbitrary matches within a segment using the region facility
			// The end argument is only an int, so we need to take care with sizes greater than 2GB
			
			int maxRegionLength = 10 * 1024;
			int availableRegionLength = isFwd
					? Ints.saturatedCast(pageManager.getEndPos())
					: Ints.saturatedCast(absMatcherStartPos + 1);
			
			int effectiveRegionLength = Math.min(maxRegionLength, availableRegionLength);
			m.region(0, effectiveRegionLength);

			int matchCount = 0;
			while(m.find() && matchCount < 10) {
				int start = m.start();
				int end = m.end();
				
				// The matcher yields absolute byte positions from the beginning of the byte sequence
				int matchPos = isFwd ? start : -end + 1;
				
				int absPos = (int)(absMatcherStartPos + matchPos); 
				nav.setPos(absPos);

				System.out.println("Attempting pos: " + absPos);
				
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
