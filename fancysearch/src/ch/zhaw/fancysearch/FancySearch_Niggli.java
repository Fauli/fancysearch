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

import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class FancySearch_Niggli {

	public static String[] queryNr = new String[50];
	public static String[] queryText = new String[50];
	public static String systemName = "fancySearch-niggli";

	public static void main(String[] args) throws IOException, ParseException {
		// 0. Specify the analyzer for tokenizing text.
		// The same analyzer should be used for indexing and searching
		//StandardAnalyzer analyzer0 = new StandardAnalyzer(Version.LUCENE_42);
		@SuppressWarnings("deprecation")
		SnowballAnalyzer analyzer0 = new SnowballAnalyzer(Version.LUCENE_42,"English");
		
		// 1. create the index
		Directory index0 = new RAMDirectory();
		// Directory index = FSDirectory.open(new
		// File("data/index/index_test1")); // disk index storage

		IndexWriterConfig config0 = new IndexWriterConfig(Version.LUCENE_42,
				analyzer0);

		IndexWriter w0 = new IndexWriter(index0, config0);

		// read irg_collection.xml, add all documents
		readXMLIntoIndexWriter(w0, "data/irg_collection.xml");

		w0.close();

		// Index written

		// --------------------------------------------------------------------------
		// niggli's test; - Snowball 
		// --------------------------------------------------------------------------
		// I know it's ugly but so waht
		@SuppressWarnings("deprecation")
		IndexReader indexReader = IndexReader.open(index0);
		Terms terms = SlowCompositeReaderWrapper.wrap(indexReader)
				.terms("text");
		System.out.println("before: " + terms.getSumTotalTermFreq());
		//TreeMap<String, Integer> treeMap = new TreeMap<String, Integer>();
		TermsEnum termEnum0 = terms.iterator(null);
		int total = 0; 
		while (termEnum0.next() != null) {
			total++; 
		}
		System.out.println("Terms in Index Before: " + total);
		total = total / 5 ;

		HashMap<String, Integer> map = new HashMap<String, Integer>();
		ValueComparator bvc = new ValueComparator(map);
		TreeMap<String, Integer> sorted_map = new TreeMap<String, Integer>(bvc);
		
		//init toDelete already with default StopWordList
		List<String> toDelete = new ArrayList<String>(Arrays.asList(
			      "a", "an", "and", "are", "as", "at", "be", "but", "by",
			      "for", "if", "in", "into", "is", "it",
			      "no", "not", "of", "on", "or", "such",
			      "that", "the", "their", "then", "there", "these",
			      "they", "this", "to", "was", "will", "with"
			    ));
		
		TermsEnum termEnum = terms.iterator(null);
		int iTermCount = 0; 
		while (termEnum.next() != null) {
			iTermCount++; 
			BytesRef text = termEnum.term();
			int freq = (int) termEnum.totalTermFreq();
			// int docFreq = (int) termEnum.docFreq();
			// System.out.println("Text:" + text.utf8ToString() + " Freq:" +
			// freq
			// + " DocFreq:" + docFreq);

			if (text.utf8ToString().length() >= 2 ) {
				if (iTermCount > (total * 2)){
					if (iTermCount < (total * 3)){
						toDelete.add(text.utf8ToString());
					} else {
						map.put("" + text.utf8ToString(), freq);
					}
				} else {
					map.put("" + text.utf8ToString(), freq);
				}
			} else {
				// if smaller than 2 delete term
				// System.out.println(text.utf8ToString());
				// map.put("" + text.utf8ToString(), freq);
				toDelete.add(text.utf8ToString());
			}
		}
		indexReader.close();
		
		//add desired stuff to toDelete
		

		sorted_map.putAll(map);
		//System.out.println("TermCountBefore: "+ iTermCount);
		System.out.println("Total: " + total);
		System.out.println("Unsorted: " + map.size());
		System.out.println("Sorted: " + sorted_map.size());
		System.out.println("ToDelete: " + toDelete.size());


		// write second Index
		@SuppressWarnings("deprecation")
		CharArraySet stopSet = new CharArraySet(Version.LUCENE_CURRENT, toDelete, false);
		
		
		//StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_42,stopSet);
		@SuppressWarnings("deprecation")
		SnowballAnalyzer analyzer = new SnowballAnalyzer(Version.LUCENE_42,"English",stopSet);
		
		Directory index = new RAMDirectory();
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42,
				analyzer);
		IndexWriter w = new IndexWriter(index, config);
		readXMLIntoIndexWriter(w, "data/irg_collection.xml");
		w.close();

		
		@SuppressWarnings("deprecation")
		IndexReader indexReader1 = IndexReader.open(index);
		Terms terms1 = SlowCompositeReaderWrapper.wrap(indexReader1).terms(
				"text");
		System.out.println("after: " + terms1.getSumTotalTermFreq());
		indexReader1.close();

		// --------------------------------------------------------------------------
		// --------------------------------------------------------------------------
		// --------------------------------------------------------------------------

		queryNr = new String[50];
		queryText = new String[50];

		// int queryNr = 1;

		readXMLIntoQueryArrays("data/irg_queries.xml");

		StringBuilder sb = new StringBuilder();

		for (int queryCounter = 0; queryCounter < 50; queryCounter++) {
			// 2. query
			String querystr = queryText[queryCounter].replaceAll("\"|\\n|/n",
					" ");

			// the "title" arg specifies the default field to use
			// when no field is explicitly specified in the query.
			Query q = new QueryParser(Version.LUCENE_42, "text", analyzer)
					.parse(querystr);

			// 3. search
			int hitsPerPage = 1000;
			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			TopScoreDocCollector collector = TopScoreDocCollector.create(
					hitsPerPage, true);
			searcher.search(q, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;

			// 4. display results
			System.out.println(".. done with query " + queryNr[queryCounter]);

			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				Document d = searcher.doc(docId);
				sb.append(queryNr[queryCounter] + " " + "Q0 "
						+ d.get("recordId") + " " + (i + 1) + " "
						+ hits[i].score + " " + systemName + "\n");

			}

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
		System.out.println("... all done :-)");

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

class ValueComparator implements Comparator<String> {
	Map<String, Integer> base;

	public ValueComparator(Map<String, Integer> base) {
		this.base = base;
	}

	// Note: this comparator imposes orderings that are inconsistent with
	// equals.
	public int compare(String a, String b) {
		if (base.get(a) >= base.get(b)) {
			return -1;
		} else {
			return 1;
		} // returning 0 would merge keys
	}
}