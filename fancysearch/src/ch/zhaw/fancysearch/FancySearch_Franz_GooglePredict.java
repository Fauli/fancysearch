package ch.zhaw.fancysearch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class FancySearch_Franz_GooglePredict {

	public static String[] queryNr = new String[50];
	public static String[] queryText = new String[50];
	public static String systemName = "fancySearch-franz";
	public static Map<String, String> cache = new HashMap<String, String>();
	public static int lookups, cacheHits;
	


	public static void main(String[] args) throws IOException, ParseException {
		
		//init toDelete already with default StopWordList
		List<String> toDelete = new ArrayList<String>(Arrays.asList(
			      "a", "an", "and", "are", "as", "at", "be", "but", "by",
			      "for", "if", "in", "into", "is", "it",
			      "no", "not", "of", "on", "or", "such",
			      "that", "the", "their", "then", "there", "these",
			      "they", "this", "to", "was", "will", "with"
			    ));

		// 0. Specify the analyzer for tokenizing text.
		// The same analyzer should be used for indexing and searching
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_42);

		// 1. create the index
		Directory index = new RAMDirectory();

		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42,
				analyzer);

		IndexWriter w = new IndexWriter(index, config);

		// read irg_collection.xml, add all documents
		readXMLIntoIndexWriter(w, "data/irg_collection.xml");

		w.close();

		// Index written

		queryNr = new String[50];
		queryText = new String[50];

		// int queryNr = 1;

		readXMLIntoQueryArrays("data/irg_queries.xml");

		System.setProperty("org.apache.lucene.maxClauseCount", "4096");
		BooleanQuery.setMaxClauseCount(4096);

		StringBuilder sb = new StringBuilder();
		System.out.println(". are lookups");
		for (int queryCounter = 0; queryCounter < 50; queryCounter++) {
			// 2. query
			System.out.print((queryCounter + 1) + ": " + queryNr[queryCounter]
					+ " ");
			List<String> querystrs = convertToSingleWords(queryText[queryCounter]
					.replaceAll("\"|\\n|/n", " "));
			StringBuilder sb2 = new StringBuilder();
			ArrayList<ScoreDoc> collectedList = new ArrayList<ScoreDoc>();
			HashMap<ScoreDoc,Float> finalList = new HashMap<ScoreDoc, Float>();

			// the "title" arg specifies the default field to use
			// when no field is explicitly specified in the query.
			cache.put("", "");
			ScoreDoc[] hits = null;
			IndexReader reader = null;
			IndexSearcher searcher = null;
			collectedList.clear();
			System.out.println("------" + queryText[queryCounter] + "-----");
			for (String srchWord : querystrs) {
//				System.out
//						.println("Doing single search for: " + srchWord + ".");
				if (srchWord.equals("") || srchWord.length() == 0) {
					//System.out.println("skipping");
					continue;
				} else if (toDelete.contains(srchWord)) {
					//System.out.println("Its a stopword: "+srchWord);
					continue;
				}
				
				//System.out.println("looking for: "+srchWord);
				Query q = new QueryParser(Version.LUCENE_42, "text", analyzer)
						.parse(QueryParser.escape(srchWord));

				// 3. search
				int hitsPerPage = 1000;
				reader = DirectoryReader.open(index);
				searcher = new IndexSearcher(reader);
				TopScoreDocCollector collector = TopScoreDocCollector.create(
						hitsPerPage, true);
				searcher.search(q, collector);
				for (ScoreDoc scoreDoc : collector.topDocs().scoreDocs) {
					//collectedList.add(scoreDoc);
					finalList.put(scoreDoc, scoreDoc.score);
					//System.out.println("Adding result to list: "+scoreDoc.toString());
				}
				hits = collector.topDocs().scoreDocs;
				
				
				
			}
			
			// Sort the Hash via values
			System.out.println("presort: "+finalList);
			ValueComparatore bvc =  new ValueComparatore(finalList);
			TreeMap<ScoreDoc,Float> sorted_map = new TreeMap<ScoreDoc,Float>(bvc);
			sorted_map.putAll(finalList);
			System.out.println("after sort: "+sorted_map);
			

			// 4. display results
			System.out.println(".. done, elements: "+sorted_map.size());
			
			int i = 0;
			for (ScoreDoc scoreDoc : sorted_map.keySet()) {
				if(i < 1000){
				Document d = searcher.doc(scoreDoc.doc);
				sb.append(queryNr[queryCounter] + " " + "Q0 "
						+ d.get("recordId") + " " + (i + 1) + " "
						+ scoreDoc.score + " " + systemName + "\n");
				i++;
				} else {
					continue;
				}
				
			}


//			for (int i = 0; i < hits.length; ++i) {
//				int docId = hits[i].doc;
//				Document d = searcher.doc(docId);
//				sb.append(queryNr[queryCounter] + " " + "Q0 "
//						+ d.get("recordId") + " " + (i + 1) + " "
//						+ hits[i].score + " " + systemName + "\n");
//
//			}

			// reader can only be closed when there
			// is no need to access the documents any more.
			reader.close();
		}

		// System.out.println(sb.toString());
		Writer writer = null;

		try {
			writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream("data/output/" + systemName + ".txt"),
					"utf-8"));
			writer.write(sb.toString());
		} catch (IOException ex) {
			// report
			ex.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (Exception ex) {
			}
		}
		System.out.println("Lookups: " + lookups + " Cache hits: " + cacheHits
				+ "Ratio (L/CH) " + (float) lookups / cacheHits);

	}

	private static List<String> convertToSingleWords(String query) {
		String[] words = query.split(" ");
		StringBuilder sb = new StringBuilder();
		List<String> results = new ArrayList<String>();

		try {
			results = Arrays.asList(query.split(" "));

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return results;

	}

	private static String getSlangWord(String html, String word) {
		String str = "";
		// System.out.println(html);
		try {
			System.out.println(word + " working html: " + html);
			if (html.contains("Including results for")) {
				str = html.split("Including results for\">")[1]
						.split("Show only")[0].replaceAll("<.*?>", "")
						.replaceAll(" +", " ");
				System.out.println("CORRECTED: " + word + " String is:" + str);
			} else {
				str = word;
			}
			// replace(")", " ").replace("("," ").replaceAll(
			// "<br/>|<br />|\"|\\n|/n|=|<(.*?)>|/N|/|-", " ").trim().split(" ",
			// 1024).toString();
		} catch (Exception ex) {

		}
		return str;
	}

	private static void readXMLIntoQueryArrays(String path) {

		try {

			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			org.w3c.dom.Document doc = docBuilder.parse(new File(path));

			doc.getDocumentElement().normalize();

			NodeList listOfDOCs = doc.getElementsByTagName("DOC");
			int totalDOCs = listOfDOCs.getLength();
			System.out.println("Total no of queries : " + totalDOCs);

			for (int s = 0; s < listOfDOCs.getLength(); s++) {
				// create doc, fill below
				// Document documentToAdd = new Document();

				Node firstDOCNode = listOfDOCs.item(s);
				if (firstDOCNode.getNodeType() == Node.ELEMENT_NODE) {

					Element firstDOCElement = (Element) firstDOCNode;

					// -------
					NodeList recordIDList = firstDOCElement
							.getElementsByTagName("recordId");
					Element recordIDElement = (Element) recordIDList.item(0);

					NodeList textrIDList = recordIDElement.getChildNodes();
					queryNr[s] = ((Node) textrIDList.item(0)).getNodeValue()
							.trim();

					// -------
					NodeList textList = firstDOCElement
							.getElementsByTagName("text");
					Element textElement = (Element) textList.item(0);

					NodeList textTextList = textElement.getChildNodes();
					queryText[s] = ((Node) textTextList.item(0)).getNodeValue()
							.trim();

				}

			}

		} catch (SAXParseException err) {
			System.out.println("** Parsing error" + ", line "
					+ err.getLineNumber() + ", uri " + err.getSystemId());
			System.out.println(" " + err.getMessage());

		} catch (SAXException e) {
			Exception x = e.getException();
			((x == null) ? e : x).printStackTrace();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private static void readXMLIntoIndexWriter(IndexWriter w, String path) {
		try {

			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			org.w3c.dom.Document doc = docBuilder.parse(new File(path));

			doc.getDocumentElement().normalize();

			NodeList listOfDOCs = doc.getElementsByTagName("DOC");
			int totalDOCs = listOfDOCs.getLength();
			System.out.println("Total no of documents to index : " + totalDOCs);

			for (int s = 0; s < listOfDOCs.getLength(); s++) {
				// create doc, fill below
				Document documentToAdd = new Document();

				Node firstDOCNode = listOfDOCs.item(s);
				if (firstDOCNode.getNodeType() == Node.ELEMENT_NODE) {

					Element firstDOCElement = (Element) firstDOCNode;

					// -------
					NodeList recordIDList = firstDOCElement
							.getElementsByTagName("recordId");
					Element recordIDElement = (Element) recordIDList.item(0);

					NodeList textrIDList = recordIDElement.getChildNodes();
					documentToAdd.add(new StringField("recordId",
							((Node) textrIDList.item(0)).getNodeValue().trim(),
							Field.Store.YES));

					// -------
					NodeList textList = firstDOCElement
							.getElementsByTagName("text");
					Element textElement = (Element) textList.item(0);

					NodeList textTextList = textElement.getChildNodes();

					documentToAdd
							.add(new TextField("text", ((Node) textTextList
									.item(0)).getNodeValue().trim(),
									Field.Store.YES));

					w.addDocument(documentToAdd);

				}

			}

		} catch (SAXParseException err) {
			System.out.println("** Parsing error" + ", line "
					+ err.getLineNumber() + ", uri " + err.getSystemId());
			System.out.println(" " + err.getMessage());

		} catch (SAXException e) {
			Exception x = e.getException();
			((x == null) ? e : x).printStackTrace();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}

class ValueComparatore implements Comparator<ScoreDoc> {

    Map<ScoreDoc, Float> base;
    public ValueComparatore(Map<ScoreDoc, Float> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.    
    public int compare(ScoreDoc a, ScoreDoc b) {
//    	System.out.println("comparing a and b");
        if (a.score >= b.score) {
            return -1;
        } else {
//        	System.out.println("a was bigger");
            return 1;
            
        } // returning 0 would merge keys
    }
}