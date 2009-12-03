/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool 
          with fuzzy matching, translation memory, keyword search, 
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2000-2006 Keith Godfrey and Maxym Mykhalchuk
           (C) 2005-06 Henry Pijffers
           (C) 2006 Martin Wunderlich
           (C) 2006-2007 Didier Briel
           (C) 2008 Martin Fleurke
           (C) 2009 Alex Buloichik
               Home page: http://www.omegat.org/
               Support center: http://groups.yahoo.com/group/OmegaT/

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
**************************************************************************/

package org.omegat.core.data;

import java.util.ArrayList;
import java.util.List;

import org.omegat.core.segmentation.Rule;
import org.omegat.core.segmentation.Segmenter;
import org.omegat.filters2.IFilter;
import org.omegat.filters2.IParseCallback;
import org.omegat.util.Language;
import org.omegat.util.StaticUtils;

/**
 * Process one entry on parse source file.
 * 
 * @author Maxym Mykhalchuk
 * @author Henry Pijffers
 * @author Alex Buloichik <alex73mail@gmail.com>
 */
public abstract class ParseEntry implements IParseCallback {

    private final ProjectProperties m_config;

    public ParseEntry(final ProjectProperties m_config) {
        this.m_config = m_config;
    }

    /**
     * This method is called by filters to add new entry in OmegaT after read it
     * from source file.
     * 
     * @param id
     *            ID of entry, if format supports it
     * @param source
     *            Translatable source string
     * @param translation
     *            translation of the source string, if format supports it
     * @param isFuzzy
     *            flag for fuzzy translation. If a translation is fuzzy, it is 
     *            not added to the projects TMX, but it is added to the 
     *            generated 'reference' TMX, a special TMX that is used as 
     *            extra refrence during translation.
     * @param comment
     *            entry's comment, if format supports it
     * @param filter
     *            filter which produces entry
     */
    public void addEntry(String id, String source, String translation,
            boolean isFuzzy, String comment, IFilter filter) {
        // replacing all occurrences of single CR (\r) or CRLF (\r\n) by LF (\n)
        // this is reversed at the end of the method
        // fix for bug 1462566
        boolean crlf = source.indexOf("\r\n") > 0;
        if (crlf)
            source = source.replace("\\r\\n", "\n");
        boolean cr = source.indexOf("\r") > 0;
        if (cr)
            source = source.replace("\\r", "\n");

        // Some special space handling: skip leading and trailing whitespace
        // and non-breaking-space
        int len = source.length();
        int b = 0;
        while (b < len
                && (Character.isWhitespace(source.charAt(b)) || source.charAt(b) == '\u00A0')) {
            b++;
        }

        int e = len - 1;
        while (e >= b
                && (Character.isWhitespace(source.charAt(e)) || source.charAt(e) == '\u00A0')) {
            e--;
        }

        source = StaticUtils.fixChars(source.substring(b, e + 1));

        String segTranslation = isFuzzy ? null : translation;
        
        if (m_config.isSentenceSegmentingEnabled()) {
            List<StringBuffer> spaces = new ArrayList<StringBuffer>();
            List<Rule> brules = new ArrayList<Rule>();
            Language sourceLang = m_config.getSourceLanguage();
            List<String> segments = Segmenter.segment(sourceLang, source,
                    spaces, brules);
            if (segments.size() == 1) {
                addSegment(id, (short) 0, segments.get(0), segTranslation,
                        comment);
            } else {
                for (short i = 0; i < segments.size(); i++) {
                    String onesrc = segments.get(i);
                    addSegment(id, i, onesrc, null, comment);
                }
            }
        } else {
            addSegment(id, (short) 0, source, segTranslation, comment);
        }
        if (translation != null) {
            // Add systematically the TU as a legacy TMX
            String tmxSource;
            if (isFuzzy) {
                tmxSource = "[" + filter.getFuzzyMark() + "] " + source;
            } else {
                tmxSource = source;
            }
            addFileTMXEntry(tmxSource, translation);
        }
    }

    /**
     * Adds the source and translation to the generated 'reference TMX', a 
     * special TMX that is used as extra refrence during translation.
     */
    public abstract void addFileTMXEntry(String source, String translation);

    /**
     * Adds a segment to the project. If a translation is given, it it added to 
     * the projects TMX.
     * @param id
     *            ID of entry, if format supports it
     * @param segmentIndex
     *            Number of the segment-part of the original source string.
     * @param segmentSource
     *            Translatable source string
     * @param segmentTranslation
     *            non fuzzy translation of the source string, if format supports it
     * @param comment
     *            entry's comment, if format supports it
     */
    protected abstract void addSegment(String id, short segmentIndex,
            String segmentSource, String segmentTranslation, String comment);
}
