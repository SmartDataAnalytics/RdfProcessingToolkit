package org.aksw.sparql_integrate.ngs.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
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
import org.aksw.jena_sparql_api.io.binseach.Seekable;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotParseException;

import com.github.jsonldjava.shaded.com.google.common.primitives.Ints;

import io.reactivex.exceptions.Exceptions;

public class MainPlaygroundScanTrig {

	public static void main(String[] args) throws IOException {
		// Pattern to find the start of named graphs in trig
		Pattern trigFwdPattern = Pattern.compile("@base|@prefix|(graph)?\\s*(<[^>]*>|_:[^-\\s]+)\\s*\\{", Pattern.CASE_INSENSITIVE);
		Pattern trigBwdPattern = Pattern.compile("esab@|xiferp@|\\{\\s*(>[^<]*<|[^-\\s]+:_)\\s*(hparg)?", Pattern.CASE_INSENSITIVE);

		// For the reversed pattern we may also re-use
		// https://github.com/vsch/reverse-regex
		
//		Path path = Paths.get("/home/raven/Projects/Eclipse/sparql-integrate-parent/ngs/one-week.trig");
		Path path = Paths.get("/home/raven/Projects/Eclipse/sparql-integrate-parent/ngs/small-sample.trig");

//		long cnt = RDFDataMgrRx.createFlowableQuads(() -> Files.newInputStream(path, StandardOpenOption.READ), Lang.TRIG, null)
//		.count()
//		.blockingGet();
//		
//		System.out.println(cnt);

//		long cnt = RDFDataMgrRx.createFlowableDatasets(() -> Files.newInputStream(path, StandardOpenOption.READ), Lang.TRIG, null)
//		.count()
//		.blockingGet();
//		
//		System.out.println(cnt);
		
		
		//PageManager pageManager = new PageManagerForByteBuffer(ByteBuffer.wrap(text));
		try(FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {
			PageManager pageManager = PageManagerForFileChannel.create(fileChannel);
			PageNavigator nav = new PageNavigator(pageManager);

			
			boolean isFwd = true;

			// Lets start from this position
			nav.setPos(2);
			long lineDisplacement[] = {0};

			//nav.setPos(10000);
			long absMatcherStartPos = nav.getPos();

			
			// The charSequence has a clone of nav so it has independent relative positioning
			CharSequence charSequence = new CharSequenceFromSeekable(nav.clone());			
			Matcher fwdMatcher = trigFwdPattern.matcher(charSequence);
			
			
			CharSequence reverseCharSequence = new ReverseCharSequenceFromSeekable(nav.clone());			
			Matcher bwdMatcher = trigBwdPattern.matcher(reverseCharSequence);
			
			
			Matcher m = isFwd ? fwdMatcher : bwdMatcher;
			
			
			// PageManager has a cached size - which is alot faster than fileChannel.size() (not that it really matters here)
			// We can find arbitrary matches within a segment using the region facility
			// The end argument is only an int, so we need to take care with sizes greater than 2GB
			
			long maxRegionLength = 10l * 1024l * 1024l * 1024l;
			int availableRegionLength = isFwd
					? Ints.saturatedCast(pageManager.getEndPos() - absMatcherStartPos)
					: Ints.saturatedCast(absMatcherStartPos + 1);

					
			int effectiveRegionLength = (int)Math.min(maxRegionLength, availableRegionLength);
			m.region(0, effectiveRegionLength);

			int matchCount = 0;
			while(m.find() && matchCount < 1000) {
				int start = m.start();
				int end = m.end();
				
				// The matcher yields absolute byte positions from the beginning of the byte sequence
				int matchPos = isFwd ? start : -end + 1;
				
				int absPos = (int)(absMatcherStartPos + matchPos);
				
				long matchPosDelta = Math.abs(absPos - nav.getPos());
				// Artificially create errors
				// absPos += 5;

				
				// Compute the line column displacement
				// This is the difference in line/column numbers between what the jena parser
				// will report and the actual location in the input
				
				// We may have multiple candidates in the same line - so the
				// displacement only increases if we actually hit a newline
				int lineDisplacementDelta = 0;
				Seekable lineSeeker = nav.clone().limitNext(matchPosDelta);
				long newlineBytePos = -1;
				while(true) {
					lineSeeker.posToNext((byte)'\n');
					if(lineSeeker.isPosAfterEnd()) {
						break;
					}

					// If we haven't reached the end then
					// posToNext has positioned us on a newline symbol.
					// Skip past it
					newlineBytePos = lineSeeker.getPos();
					lineSeeker.nextPos(1);
					
					// TODO Deal with different line endings such as \n\r
					++lineDisplacementDelta;
				}
				lineDisplacement[0] += lineDisplacementDelta;
				
				// If we started in the middle of a line, we have to go back
				// to a position that is before where the matcher started off
				if(lineDisplacementDelta == 0) {
					lineSeeker.prevPos(1);
					lineSeeker.posToPrev((byte)'\n');
					newlineBytePos = lineSeeker.getPos();
				}
				
				// We now need to get the chars for the line
				lineSeeker.setPos(newlineBytePos + 1);
				String lineStr = IOUtils.toString(
						Channels.newInputStream(lineSeeker),
						StandardCharsets.UTF_8);
				int colDisplacement = lineStr.length();
				
				// Find the column of the match in the last line
				//long colDisplacement = absPos - newlineCharPos;
				
				nav.setPos(absPos);

				System.out.println("Attempting pos: " + absPos);

				
				PageNavigator clonedNav = nav.clone();
				InputStream in = Channels.newInputStream(clonedNav);
				
//				BufferedReader r = new BufferedReader(new InputStreamReader(in));
//				r.lines().forEach(System.out::println);
//
//				if(true) {
//					return;
//				}
				
				
				
				int maxQuadCount = 3;
				long quadCount = RDFDataMgrRx.createFlowableDatasets(() -> in, Lang.TRIG, null)
					//.limit(maxQuadCount)
					.count()
					.doOnError(t -> {
						if(t instanceof RiotParseException) {
							RiotParseException rpe = (RiotParseException)t;
							t = new RiotParseException(rpe.getOriginalMessage(), lineDisplacement[0] + rpe.getLine(), colDisplacement + rpe.getCol());
						}
						Exceptions.propagate(t);
					})
					.doOnError(t -> t.printStackTrace())
					.onErrorReturnItem(-1l)
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
