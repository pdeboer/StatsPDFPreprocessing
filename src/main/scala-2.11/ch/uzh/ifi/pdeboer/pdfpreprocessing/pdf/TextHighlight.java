package ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf;

/*
 * Copyright 2014 J. Kuiper
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;

import java.awt.*;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class implements the methods highlight and highlightDefault which will add a highlight to the PDF based on a
 * Pattern or String. The idea is to extend the PDFTextStripper and override the methods that write to the Output to
 * instead write to a TextCache that keeps data on the position of the TextPositions. From this information we can then
 * derive bounding boxes (and quads) that can be used to write the annotations. See the main method for example usage
 *
 * @author J. Kuiper <me@joelkuiper.eu>
 */
public class TextHighlight extends PDFTextStripper {

	public TextCache textCache;
	private float verticalTolerance = 5;
	private float heightModifier = (float) 1.250;
	private boolean inParagraph;

	/**
	 * Instantiate a new object. This object will load properties from PDFTextAnnotator.properties and will apply
	 * encoding-specific conversions to the output text.
	 *
	 * @param encoding The encoding that the output will be written in.
	 * @throws IOException If there is an error reading the properties.
	 */
	public TextHighlight(final String encoding) throws IOException {
		super(encoding);
	}

	/**
	 * Computes a series of bounding boxes (PDRectangle) from a list of TextPositions. It will create a new bounding box
	 * if the vertical tolerance is exceeded
	 *
	 * @param positions
	 * @throws IOException
	 */
	public List<PDRectangle> getTextBoundingBoxes(final List<TextPosition> positions) {
		final List<PDRectangle> boundingBoxes = new ArrayList<PDRectangle>();

		float lowerLeftX = -1, lowerLeftY = -1, upperRightX = -1, upperRightY = -1;
		boolean first = true;
		for (int i = 0; i < positions.size(); i++) {
			final TextPosition position = positions.get(i);
			if (position == null) {
				continue;
			}
			final Matrix textPos = position.getTextPos();
			final float height = position.getHeight() * getHeightModifier();
			if (first) {
				lowerLeftX = textPos.getXPosition();
				upperRightX = lowerLeftX + position.getWidth();

				lowerLeftY = textPos.getYPosition();
				upperRightY = lowerLeftY + height;
				first = false;
				continue;
			}

			// we are still on the same line
			if (Math.abs(textPos.getYPosition() - lowerLeftY) <= getVerticalTolerance()) {
				upperRightX = textPos.getXPosition() + position.getWidth();
				upperRightY = textPos.getYPosition() + height;
			} else {
				final PDRectangle boundingBox = boundingBox(lowerLeftX, lowerLeftY, upperRightX,
						upperRightY);
				boundingBoxes.add(boundingBox);

				// new line
				lowerLeftX = textPos.getXPosition();
				upperRightX = lowerLeftX + position.getWidth();

				lowerLeftY = textPos.getYPosition();
				upperRightY = lowerLeftY + height;
			}
		}
		if (!(lowerLeftX == -1 && lowerLeftY == -1 && upperRightX == -1 && upperRightY == -1)) {
			final PDRectangle boundingBox = boundingBox(lowerLeftX, lowerLeftY, upperRightX,
					upperRightY);
			boundingBoxes.add(boundingBox);
		}
		return boundingBoxes;
	}

	private PDRectangle boundingBox(final float lowerLeftX, final float lowerLeftY,
									final float upperRightX, final float upperRightY) {
		final PDRectangle boundingBox = new PDRectangle();
		boundingBox.setLowerLeftX(lowerLeftX);
		boundingBox.setLowerLeftY(lowerLeftY);
		boundingBox.setUpperRightX(upperRightX);
		boundingBox.setUpperRightY(upperRightY);
		return boundingBox;
	}


	public void highlight(final Pattern searchText, final Pattern markingPattern, Color color, int pageNr) {
		if (textCache == null || document == null) {
			throw new IllegalArgumentException("TextCache was not initialized");
		}

		final List<PDPage> pages = document.getDocumentCatalog().getAllPages();

		try {
			boolean found = false;

			final PDPage page = pages.get(pageNr - 1);
			PDPageContentStream contentStream = new PDPageContentStream(document, page, true, true);

			PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
			graphicsState.setNonStrokingAlphaConstant(0.5f);
			PDResources resources = page.findResources();
			Map graphicsStateDictionary = resources.getGraphicsStates();
			if (graphicsStateDictionary == null) {
				// There is no graphics state dictionary in the resources dictionary, create one.
				graphicsStateDictionary = new TreeMap();
			}
			graphicsStateDictionary.put("highlights", graphicsState);
			resources.setGraphicsStates(graphicsStateDictionary);

			for (Match searchMatch : textCache.match(pageNr, searchText)) {
				if (textCache.match(searchMatch.positions, markingPattern).size() > 0) {
					for (Match markingMatch : textCache.match(searchMatch.positions, markingPattern)) {
						if (markupMatch(color, contentStream, markingMatch)) {
							found = true;
						}
					}
				} else {
					System.out.println("Cannot highlight: " + markingPattern.pattern() + " on page " + (pageNr - 1));
				}
				if (found) {
					break;
				}
			}
			contentStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		} catch (Error e1) {
			e1.printStackTrace();
			throw e1;
		}
	}


	private boolean markupMatch(Color color, PDPageContentStream contentStream, Match markingMatch) throws IOException {
		final List<PDRectangle> textBoundingBoxes = getTextBoundingBoxes(markingMatch.positions);

		if (textBoundingBoxes.size() > 0) {
			contentStream.appendRawCommands("/highlights gs\n");
			contentStream.setNonStrokingColor(color);
			for (PDRectangle textBoundingBox : textBoundingBoxes) {
				contentStream.fillRect(textBoundingBox.getLowerLeftX(), textBoundingBox.getLowerLeftY(),
						Math.max(Math.abs(textBoundingBox.getUpperRightX() - textBoundingBox.getLowerLeftX()), 10), 10);
			}
			return true;
		}
		return false;
	}

	/**
	 * The vertical tolerance determines whether a character is still on the same line
	 */
	public float getVerticalTolerance() {
		return verticalTolerance;
	}

	/**
	 * The height modifier is applied to the font height, it allows the annotations to be changed by a certain factor
	 */
	public float getHeightModifier() {
		return heightModifier;
	}

	/*
	 * The following methods are overwritten from the PDTextStripper
	 */
	public void initialize(final PDDocument pdf) throws IOException {
		try {
			resetEngine();
			document = pdf;
			textCache = new TextCache();

			if (getAddMoreFormatting()) {
				setParagraphEnd(getLineSeparator());
				setPageStart(getLineSeparator());
				setArticleStart(getLineSeparator());
				setArticleEnd(getLineSeparator());
			}
			startDocument(pdf);
			processPages(pdf.getDocumentCatalog().getAllPages());
			endDocument(pdf);
		} catch (Exception e) {
			e.printStackTrace();
		} catch (Error e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void resetEngine() {
		super.resetEngine();
		textCache = null;
	}

	/**
	 * Start a new article, which is typically defined as a column on a single page (also referred to as a bead).
	 * Default implementation is to do nothing. Subclasses may provide additional information.
	 *
	 * @param isltr true if primary direction of text is left to right.
	 * @throws IOException If there is any error writing to the stream.
	 */
	@Override
	protected void startArticle(final boolean isltr) throws IOException {
		final String articleStart = getArticleStart();
		textCache.append(articleStart, null);
	}

	/**
	 * End an article. Default implementation is to do nothing. Subclasses may provide additional information.
	 *
	 * @throws IOException If there is any error writing to the stream.
	 */
	@Override
	protected void endArticle() throws IOException {
		final String articleEnd = getArticleEnd();
		textCache.append(articleEnd, null);
	}

	/**
	 * Start a new page. Default implementation is to do nothing. Subclasses may provide additional information.
	 *
	 * @param page The page we are about to process.
	 * @throws IOException If there is any error writing to the stream.
	 */
	@Override
	protected void startPage(final PDPage page) throws IOException {
		// default is to do nothing.
	}

	/**
	 * End a page. Default implementation is to do nothing. Subclasses may provide additional information.
	 *
	 * @param page The page we are about to process.
	 * @throws IOException If there is any error writing to the stream.
	 */
	@Override
	protected void endPage(final PDPage page) throws IOException {
		// default is to do nothing
	}

	/**
	 * Write the page separator value to the text cache.
	 *
	 * @throws IOException If there is a problem writing out the pageseparator to the document.
	 */
	@Override
	protected void writePageSeperator() {
		final String pageSeparator = getPageSeparator();
		textCache.append(pageSeparator, null);
	}

	/**
	 * Write the line separator value to the text cache.
	 *
	 * @throws IOException If there is a problem writing out the lineseparator to the document.
	 */
	@Override
	protected void writeLineSeparator() {
		final String lineSeparator = getLineSeparator();
		textCache.append(lineSeparator, null);
	}

	/**
	 * Write the word separator value to the text cache.
	 *
	 * @throws IOException If there is a problem writing out the wordseparator to the document.
	 */
	@Override
	protected void writeWordSeparator() {
		final String wordSeparator = getWordSeparator();
		textCache.append(wordSeparator, null);
	}

	/**
	 * Write the string in TextPosition to the text cache.
	 *
	 * @param text The text to write to the stream.
	 */
	@Override
	protected void writeCharacters(final TextPosition text) {
		final String character = text.getCharacter();
		textCache.append(character, text);

	}

	/**
	 * Write a string to the text cache. The default implementation will ignore the <code>text</code> and just calls
	 * {@link #writeCharacters(TextPosition)} .
	 *
	 * @param text          The text to write to the stream.
	 * @param textPositions The TextPositions belonging to the text.
	 */
	@Override
	protected void writeString(final String text, final List<TextPosition> textPositions) {
		for (final TextPosition textPosition : textPositions) {
			writeCharacters(textPosition);
		}
	}

	/**
	 * writes the paragraph separator string to the text cache.
	 *
	 * @throws IOException
	 */
	@Override
	protected void writeParagraphSeparator() {
		writeParagraphEnd();
		writeParagraphStart();
	}

	/**
	 * Write something (if defined) at the start of a paragraph.
	 *
	 * @throws IOException
	 */
	@Override
	protected void writeParagraphStart() {
		if (inParagraph) {
			writeParagraphEnd();
			inParagraph = false;
		}

		final String paragraphStart = getParagraphStart();
		textCache.append(paragraphStart, null);
		inParagraph = true;
	}

	/**
	 * Write something (if defined) at the end of a paragraph.
	 *
	 * @throws IOException
	 */
	@Override
	protected void writeParagraphEnd() {
		final String paragraphEnd = getParagraphEnd();
		textCache.append(paragraphEnd, null);

		inParagraph = false;
	}

	/**
	 * Write something (if defined) at the start of a page.
	 *
	 * @throws IOException
	 */
	@Override
	protected void writePageStart() {
		final String pageStart = getPageStart();
		textCache.append(pageStart, null);
	}

	/**
	 * Write something (if defined) at the start of a page.
	 *
	 * @throws IOException
	 */
	@Override
	protected void writePageEnd() {
		final String pageEnd = getPageEnd();
		textCache.append(pageEnd, null);
	}

	@Override
	public String getText(final PDDocument doc) throws IOException {
		throw new IllegalArgumentException("Not applicable for TextHighlight");
	}

	@Override
	public String getText(final COSDocument doc) throws IOException {
		throw new IllegalArgumentException("Not applicable for TextHighlight");
	}

	@Override
	public void writeText(final COSDocument doc, final Writer outputStream) throws IOException {
		throw new IllegalArgumentException("Not applicable for TextHighlight");
	}

	@Override
	public void writeText(final PDDocument doc, final Writer outputStream) throws IOException {
		throw new IllegalArgumentException("Not applicable for TextHighlight");
	}

	/**
	 * Internal utility class that keeps a mapping from the text contents to their TextPositions. This is needed to
	 * compute bounding boxes. The data is stored on a per-page basis (keyed on the 1-based pageNo)
	 */
	public class TextCache {
		private final Map<Integer, StringBuilder> texts = new HashMap<Integer, StringBuilder>();
		private final Map<Integer, ArrayList<TextPosition>> positions = new HashMap<Integer, ArrayList<TextPosition>>();

		private StringBuilder obtainStringBuilder(final Integer pageNo) {
			StringBuilder sb = texts.get(pageNo);
			if (sb == null) {
				sb = new StringBuilder();
				texts.put(pageNo, sb);
			}
			return sb;
		}

		private ArrayList<TextPosition> obtainTextPositions(final Integer pageNo) {
			ArrayList<TextPosition> textPositions = positions.get(pageNo);
			if (textPositions == null) {
				textPositions = new ArrayList<TextPosition>();
				positions.put(pageNo, textPositions);
			}
			return textPositions;
		}

		public String getText(final Integer pageNo) {
			return obtainStringBuilder(pageNo).toString();
		}

		public List<TextPosition> getTextPositions(final Integer pageNo) {
			return obtainTextPositions(pageNo);
		}

		public void append(final String str, final TextPosition pos) {
			final int currentPage = getCurrentPageNo();
			final ArrayList<TextPosition> positions = obtainTextPositions(currentPage);
			final StringBuilder sb = obtainStringBuilder(currentPage);

			for (int i = 0; i < str.length(); i++) {
				sb.append(str.charAt(i));
				positions.add(pos);
			}
		}

		/**
		 * Given a page and a pattern it will return a list of matches for that pattern. A Match is a tuple of <String,
		 * List<TextPositions>>
		 *
		 * @param pageNo
		 * @param pattern
		 * @return list of matches
		 */
		public List<Match> match(final Integer pageNo, final Pattern pattern) {
			return match(getTextPositions(pageNo), this.getText(pageNo), pattern);
		}

		public List<Match> match(List<TextPosition> textPositions, final Pattern pattern) {
			StringBuilder sb = new StringBuilder(textPositions.size() * 2);
			for (TextPosition textPosition : textPositions) {
				if (textPosition != null) sb.append(textPosition.getCharacter());
			}
			return match(textPositions, sb.toString(), pattern);
		}

		public List<Match> match(List<TextPosition> textPositions, String text, final Pattern pattern) {
			try {
				final Matcher matcher = pattern.matcher(text);
				final List<Match> matches = new ArrayList<Match>();

				while (matcher.find()) {
					final List<TextPosition> elements = textPositions.subList(
							matcher.start(), matcher.end());
					matches.add(new Match(matcher.group(), elements));
				}
				return matches;
			} catch (Error e) {
				System.out.println("An error occurred while searching for: " + pattern.toString());
				e.printStackTrace();
				final List<Match> emptyList = new ArrayList<Match>();
				return emptyList;
			} catch (Exception e1) {
				System.out.println("An exception occurred while seraching for: " + pattern.toString());
				e1.printStackTrace();
				final List<Match> emptyList = new ArrayList<Match>();
				return emptyList;
			}
		}
	}

}