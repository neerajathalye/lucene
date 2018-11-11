package tcd.ie.code;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
// import org.apache.lucene.store.RAMDirectory;

public class Index {

    // Directory where the search index will be saved
    private static String INDEX_DIRECTORY = "index";
    private static String CORPUS_DIRECTORY = "cranfield corpus";

    public static void main(String[] args) throws IOException {
        // Analyzer that is used to process TextField
        Analyzer analyzer = new StandardAnalyzer();

        // To store an index in memory
        // Directory directory = new RAMDirectory();
        // To store an index on disk
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        System.out.println("Enter 1 for default\nEnter 2 for Classic\nEnter 3 for BM25\n");
        Scanner in = new Scanner(System.in);
        int choice  = in.nextInt();

        switch (choice)
        {
            case 1:
                //do nothing
                break;
            case 2:
                config.setSimilarity(new ClassicSimilarity());
                break;
            case 3:
                config.setSimilarity(new BM25Similarity());
                break;
        }




        // Index opening mode
        // IndexWriterConfig.OpenMode.CREATE = create a new index
        // IndexWriterConfig.OpenMode.APPEND = open an existing index
        // IndexWriterConfig.OpenMode.CREATE_OR_APPEND = create an index if it
        // does not exist, otherwise it opens it
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        IndexWriter iwriter = new IndexWriter(directory, config);


        File file = new File(String.valueOf(Paths.get(CORPUS_DIRECTORY + "/cran.all.1400")));
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        String line;

        int docCount = 0;


        StringBuilder documentContents = new StringBuilder();
        ArrayList<StringBuilder> documentList = new ArrayList<>();
        int documentIndex = 1;


        HashMap<String, HashMap<String, String>> docmentsMap = new HashMap<>();

        while ((line = bufferedReader.readLine()) != null)
        {
            if(line.equals(".I 1"))
                continue;
            if (!line.contains(".I " + (documentIndex + 1)))
            {

                documentContents.append(line).append("\n");
            }
            else
            {
                docCount++;

                documentList.add(documentContents);

                String T, A, B, W;

                T = documentContents.substring((documentContents.indexOf(".T")+3), documentContents.indexOf(".A"));
                A = documentContents.substring((documentContents.indexOf(".A")+3), documentContents.indexOf(".B"));
                B = documentContents.substring((documentContents.indexOf(".B")+3), documentContents.indexOf(".W"));
                W = documentContents.substring((documentContents.indexOf(".W")+3), documentContents.length());


                //Creating the document
                Document doc = new Document();
                doc.add(new StringField("document-number", String.valueOf(docCount), Field.Store.YES));
                doc.add(new TextField("title", T, Field.Store.YES));
                doc.add(new TextField("author", A, Field.Store.YES));
                doc.add(new TextField("source", B, Field.Store.YES));
                doc.add(new TextField("content", W, Field.Store.YES));

                //Save the document to the index
                iwriter.addDocument(doc);


                documentIndex++;
                documentContents = new StringBuilder();

            }
        }

        System.out.println("Document Count: " + docCount);

        // Commit changes and close everything
        iwriter.close();
        directory.close();
    }
}
