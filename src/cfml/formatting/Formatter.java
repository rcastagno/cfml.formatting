package cfml.formatting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.EndTag;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.SourceFormatter;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.Tag;

import cfml.formatting.preferences.FormattingPreferences;
import cfml.parsing.cfmentat.tag.CFMLTagTypes;

public class Formatter {

	private static final String lineSeparator = System.getProperty("line.separator");
	private static String fCurrentIndent;
	private static int MAX_LENGTH = 0;
	private static int col;
	FormattingPreferences fPrefs;
	LineTrimmer lineTrimmer = new LineTrimmer();
	
	public Formatter(FormattingPreferences prefs) {
		fPrefs = prefs;
	}
	
	public String format(String contents, FormattingPreferences prefs, String currentIndent) {
		String indentation = prefs.getCanonicalIndent();
		String newLine = lineSeparator;
		CFMLTagTypes.register();
//		GenericStartTagTypeCf cfSave =  new GenericStartTagTypeCf("CFML if tag", "<cfsavecontent", ">", EndTagType.NORMAL, false, true, true) { };
//		cfSave.register();
		Source source = new Source(contents.replaceAll("\\r?\\n", newLine));
		System.err.println("Before:"+source.getAllElements(CFMLTagTypes.CFML_MAIL).size());
		//source.ignoreWhenParsing(source.getAllElements(HTMLElementName.SCRIPT));
		//source.ignoreWhenParsing(source.getAllElements(CFMLTagTypes.CFML_SAVECONTENT));
		//source.ignoreWhenParsing(source.getAllElements(CFMLTagTypes.CFML_SCRIPT));
		//source.ignoreWhenParsing(source.getAllElements(CFMLTagTypes.CFML_MAIL));
		System.err.println(source.getAllElements(CFMLTagTypes.CFML_MAIL).size());
//		source.ignoreWhenParsing(source.getAllElements(cfSave));
//		List<Element> els = source.getAllElements(CFMLTagTypes.CFML_SAVECONTENT);
//		 List<StartTag> tls = source.getAllStartTags();

		List<Element> elementList=source.getAllElements();
		for (Element element : elementList) {
			System.out.println("-------------------------------------------------------------------------------");
			System.out.println(element.getDebugInfo());
			if (element.getAttributes()!=null) System.out.println("XHTML StartTag:\n"+element.getStartTag().tidy(true));
			System.out.println("Source text with content:\n"+element);
		}
		System.out.println(source.getCacheDebugInfo());
		
		
		
		boolean enforceMaxLineWidth = prefs.getEnforceMaximumLineWidth();
		boolean tidyTags = prefs.tidyTags();
		boolean collapseWhitespace = prefs.collapseWhiteSpace();
		boolean indentAllElements = prefs.indentAllElements();
		boolean changeTagCase = prefs.changeTagCase();
		boolean changeTagCaseUpper = prefs.changeTagCaseUpper();
		boolean changeTagCaseLower = prefs.changeTagCaseLower();
		int maxLineWidth = prefs.getMaximumLineWidth();

		// displaySegments(source.getAllElements(HTMLElementName.SCRIPT));
		// source.fullSequentialParse();

		// java 5 req?
		// System.out.println("Unregistered start tags:");
		// displaySegments(source.getAllTags(StartTagType.UNREGISTERED));
		// System.out.println("Unregistered end tags:");
		// displaySegments(source.getAllTags(EndTagType.UNREGISTERED));

		SourceFormatter sourceFormatter = source.getSourceFormatter();
		sourceFormatter.setIndentString(indentation);
		sourceFormatter.setTidyTags(tidyTags);
		sourceFormatter.setIndentAllElements(indentAllElements);
		sourceFormatter.setCollapseWhiteSpace(collapseWhitespace);
		sourceFormatter.setNewLine(newLine);
		String results = sourceFormatter.toString();
		List oldCfmailStartTags=source.getAllStartTags(CFMLTagTypes.CFML_MAIL);
		Source formattedSource = new Source(results);
		OutputDocument outputDocument=new OutputDocument(source);
		List newCfmailStartTags=formattedSource.getAllStartTags(CFMLTagTypes.CFML_MAIL);
		int curTag = 0;
		for (Iterator i=newCfmailStartTags.iterator(); i.hasNext();) {
		    StartTag hyperlinkStartTag=(StartTag)i.next();
		    outputDocument.replace(hyperlinkStartTag.getTagContent(),((StartTag) oldCfmailStartTags.get(curTag)).getTagContent());
		    curTag++;
		}
		results=outputDocument.toString();
		if (changeTagCase) {
			if (changeTagCaseLower) {
				results = changeTagCase(results, false);
			} else {
				results = changeTagCase(results, true);
			}
		}
		if (prefs.getCloseTags()) {
			results = results.replaceAll("(?si)<(cfabort?.*?[^\\s$|/|]?)\\s?/?>", "<$1 />");
			results = results.replaceAll("(?si)<(cfargument?.*?[^\\s$|/|]?)\\s?/?>", "<$1 />");
			results = results.replaceAll("(?si)<(cfreturn?.*?[^\\s$|/|]?)\\s?/?>", "<$1 />");
			results = results.replaceAll("(?si)<(cfset?.*?[^\\s$|/|]?)\\s?/?>", "<$1 />");
			results = results.replaceAll("(?si)<(cfinput?.*?[^\\s$|/|]?)\\s?/?>", "<$1 />");
			results = results.replaceAll("(?si)<(cfimport?.*?[^\\s$|/|]?)\\s?/?>", "<$1 />");
			results = results.replaceAll("(?si)<(cfdump?.*?[^\\s$|/|]?)\\s?/?>", "<$1 />");
			results = results.replaceAll("(?si)<(cfthrow?.*?[^\\s$|/|]?)\\s?/?>", "<$1 />");
		}
		results = results.replaceAll("(?si)<(cfcomponent[^>]*)>", "<$1>" + newLine);
		results = results.replaceAll("(?si)(\\s+)<(/cfcomponent[^>]*)>", newLine + "$1<$2>");
		results = results.replaceAll("(?si)(\\s+)<(cffunction[^>]*)>", newLine + "$1<$2>");
		results = results.replaceAll("(?si)(\\s+)<(/cffunction[^>]*)>", "$1<$2>" + newLine);
		results = results.replaceAll("(?i)" + newLine + "{3}(\\s+)<(cffunction)", newLine + newLine + "$1<$2");
		results = results.replaceAll("(?si)(\\s+)<(/cffunction[^>]*)>"+ newLine + "{3}", "$1<$2>" + newLine + newLine);
		results = results.replaceAll("(?i)" + indentation + "<(cfelse)", "<$1");
		// indent to whatever the current level is
		String[] lines = results.split(newLine);
		StringBuffer indented = new StringBuffer();
		for (int x = 0; x < lines.length; x++) {
			indented.append(currentIndent);
			indented.append(lines[x]);
			indented.append(newLine);
		}
		// indented.setLength(indented.lastIndexOf(newLine));
		// return indented.toString();
		if (!enforceMaxLineWidth) {
			return indented.toString();
		} else {
			return lineTrimmer.formatLineLength(indented.toString(), maxLineWidth, fPrefs.getCanonicalIndent());
		}
	}

	public String changeTagCase(String contents, boolean uppercase) {
		Source source = new Source(contents);
		source.fullSequentialParse();
		OutputDocument outputDocument = new OutputDocument(source);
		List<Tag> tags = source.getAllTags();
		int pos = 0;
		for (Tag tag : tags) {
			Element tagElement = tag.getElement();
			if (tagElement == null) {
				System.out.println(tag.getName());
			} else {
				StartTag startTag = tagElement.getStartTag();
				Attributes attributes = startTag.getAttributes();
				if (attributes != null) {
					for (Attribute attribute : startTag.getAttributes()) {
						if (uppercase) {
							outputDocument.replace(attribute.getNameSegment(), attribute.getNameSegment().toString()
									.toUpperCase());
						} else {
							outputDocument.replace(attribute.getNameSegment(), attribute.getNameSegment().toString()
									.toLowerCase());
						}
					}
				}
				if (uppercase) {
					outputDocument.replace(tag.getNameSegment(), tag.getNameSegment().toString().toUpperCase());
				} else {
					outputDocument.replace(tag.getNameSegment(), tag.getNameSegment().toString().toLowerCase());
				}
				pos = tag.getEnd();
			}
		}
		return outputDocument.toString();
	}	

	public String format(String string) {
		return format(string,fPrefs,"");		
	}

	public String format(URL testFileURL) {
		try {
			return format(testFileURL.getContent().toString(),fPrefs,"");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "FILE NOT FOUND";
	}	
	

	public String format(InputStream inStream) {
		try {
			return format(convertStreamToString(inStream),fPrefs,"");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "FILE NOT FOUND";
	}

    public String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            StringBuilder sb = new StringBuilder();
            String line;

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } finally {
                is.close();
            }
            return sb.toString();
        } else {        
            return "";
        }
    }
	
}
