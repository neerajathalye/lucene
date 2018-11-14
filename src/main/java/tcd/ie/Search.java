package tcd.ie;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

public class Search {

    // the location of the search index
    private static String INDEX_DIRECTORY = "index";
    private static String TOPIC_DIRECTORY = "topic";
    private static String RESULT_DIRECTORY = "result";

    // Limit the number of search results we get
    private static int MAX_RESULTS = 1000;

    public static void main(String[] args) throws IOException, ParseException {
        // Analyzer used by the query parser.
        // Must be the same as the one used when creating the index
        Analyzer analyzer = new StandardAnalyzer();

        // Open the folder that contains our search index
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));

        // create objects to read and search across the index
        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher isearcher = new IndexSearcher(ireader);

        isearcher.setSimilarity(new BM25Similarity());

        String singleFieldResultFileName = "singleFieldResults.test";
        String multiFieldResultsFileName = "multiFieldResults.test";

        // Create the query parser. The default search field is "content", but
        // we can use this to search across any field

        QueryParser singleFieldQueryParser = new QueryParser("text", analyzer);
        QueryParser multiFieldQueryParser = new MultiFieldQueryParser(new String[]{"headline", "text", "subject"}, analyzer);

        ArrayList<String> queryList;

        queryList = getQueryList();

        String queryString = "";

        for (String s : queryList) {
            queryString = s.trim();
//
            if (queryString.length() > 0)
            {
                // parse the query with the parser
                Query sTerm = singleFieldQueryParser.parse(queryString); // for Single field
                Query mTerm = multiFieldQueryParser.parse(queryString); // for Multiple fields

                // Get the set of results
                ScoreDoc[] sHits = isearcher.search(sTerm, MAX_RESULTS).scoreDocs; // for Single field
                ScoreDoc[] mHits = isearcher.search(mTerm, MAX_RESULTS).scoreDocs; // for Multiple fields

                File sFile = new File(RESULT_DIRECTORY + "/" + singleFieldResultFileName);
                FileWriter sWriter = new FileWriter(sFile, true);

                File mFile = new File(RESULT_DIRECTORY + "/" + multiFieldResultsFileName);
                FileWriter mWriter = new FileWriter(mFile, true);

                // Print the results
//                System.out.println("Documents: " + hits.length);
                for (int i = 0; i < sHits.length; i++)
                {
                    Document hitDoc = isearcher.doc(sHits[i].doc);
                    String result = String.valueOf(queryList.indexOf(s)+ 401) + " 0 " + String.valueOf(hitDoc.get("document-number")) + " 0 " + String.valueOf(sHits[i].score) + " SINGLE";

                    sWriter.write(result + "\n");
                }

                sWriter.close();

                for (int i = 0; i < mHits.length; i++)
                {
                    Document hitDoc = isearcher.doc(mHits[i].doc);
                    String result = String.valueOf(queryList.indexOf(s)+ 401) + " 0 " + String.valueOf(hitDoc.get("document-number")) + " 0 " + String.valueOf(mHits[i].score) + " MULTI";

                    mWriter.write(result + "\n");
                }

                mWriter.close();
            }

        }
            // close everything and quit

            ireader.close();
            directory.close();
        }

    private static ArrayList<String> getQueryList()throws IOException
    {

        File file = new File(TOPIC_DIRECTORY + "/topics.401-450");

        System.out.println(file.getAbsolutePath());

        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        String line;

        ArrayList<String> queryList = new ArrayList<>();

        StringBuilder query = new StringBuilder();

        int queryCount = 0;

        while ((line = bufferedReader.readLine()) != null)
        {
            if(line.contains("<top>"))
                query = new StringBuilder();
            else if (!line.contains("</top>"))
            {
                query.append(line).append(" ");
            }
            else
            {
                String description = query.substring(query.indexOf("<desc> Description: ") + 20, query.indexOf("<narr>"));
                queryList.add(description); //Using description as the query
                query = new StringBuilder();
                queryCount++;
            }
        }


        return queryList;
    }
}