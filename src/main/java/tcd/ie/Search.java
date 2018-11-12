package tcd.ie;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
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



        System.out.println("Enter 1 for default\nEnter 2 for Classic\nEnter 3 for BM25\n");
        Scanner in = new Scanner(System.in);
        int choice  = in.nextInt();

        String resultsFileName = null;

        switch (choice)
        {
            case 1:
                //do nothing
                resultsFileName = "defaultresults.test";
                break;
            case 2:
                isearcher.setSimilarity(new ClassicSimilarity());
                resultsFileName = "classicresults.test";
                break;
            case 3:
                isearcher.setSimilarity(new BM25Similarity());
                resultsFileName = "bm25results.test";
                break;
        }

        // Create the query parser. The default search field is "content", but
        // we can use this to search across any field
        QueryParser parser = new QueryParser("content", analyzer);

        ArrayList<String> queryList;

        queryList = getQueryList();

        String queryString = "";
//        Scanner scanner = new Scanner(System.in);

        for(String s : queryList)
        {
            queryString = s.trim();

            if (queryString.length() > 0)
            {
                // parse the query with the parser
                Query term = parser.parse(queryString);

                // Get the set of results
                ScoreDoc[] hits = isearcher.search(term, MAX_RESULTS).scoreDocs;

                File file = new File(resultsFileName);
                FileWriter writer = new FileWriter(file, true);

                // Print the results
//                System.out.println("Documents: " + hits.length);
                for (int i = 0; i < hits.length; i++)
                {
                    Document hitDoc = isearcher.doc(hits[i].doc);
                    String result = String.valueOf(queryList.indexOf(s)+1) + " 0 " + String.valueOf(hitDoc.get("document-number")) + " 0 " + String.valueOf(hits[i].score) + " STANDARD";

                    writer.write(result + "\n");
//                    System.out.println(result);

//                    System.out.println((i+1) + ") "
//
////                            + "Query Number: " + (queryList.indexOf(s)+1)
//                            + "DNo.: " + hitDoc.get("document-number")
//                            + " Title: " + hitDoc.get("title")
//                            + " Score: " + hits[i].score);
                }

//                System.out.println("Done");
                writer.close();
            }

        }
        // close everything and quit

        ireader.close();
        directory.close();
    }

    private static ArrayList<String> getQueryList() throws IOException {

        File file = new File(String.valueOf(Paths.get("cran.qry")));
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        String line;

        ArrayList<String> queryList = new ArrayList<>();

        StringBuilder query = new StringBuilder();

        int queryCount = 0;

        while ((line = bufferedReader.readLine()) != null)
        {
            if (!line.contains(".I"))
            {
                query.append(line).append(" ");
            }
            else
            {
                queryList.add(query.toString());
                query = new StringBuilder();
                queryCount++;
            }
        }
        queryList.add(query.toString());
        queryCount++;

//        System.out.println("QUERY: " + queryCount);
//
//        System.out.println(queryList.get(queryList.size()-1));

        return queryList;
    }
}