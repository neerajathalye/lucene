package tcd.ie;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
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

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
// import org.apache.lucene.store.RAMDirectory;

public class Index {

    // Directory where the search index will be saved
    private static String INDEX_DIRECTORY = "index";
    private static String CORPUS_DIRECTORY = "corpus";

    public static void main(String[] args) throws IOException {

        File ftDirectory = new File(String.valueOf(Paths.get(CORPUS_DIRECTORY + "/ft")));
        File fr94Directory = new File(String.valueOf(Paths.get(CORPUS_DIRECTORY + "/fr94")));
        File fbisDirectory = new File(String.valueOf(Paths.get(CORPUS_DIRECTORY + "/fbis")));
        File latimesDirectory = new File(String.valueOf(Paths.get(CORPUS_DIRECTORY + "/latimes")));

        ArrayList<File> ftList = addFilesToList(ftDirectory);
        ArrayList<File> fr94List = addFilesToList(fr94Directory);
        ArrayList<File> fbisList = addFilesToList(fbisDirectory);
        ArrayList<File> latimesList = addFilesToList(latimesDirectory);


        ftList.removeIf(e->e.getParentFile().getAbsolutePath().equals(ftDirectory.getAbsolutePath())); //removes files with parent as ft(readfrcg and readmeft)

        fr94List.removeIf(e->e.getParentFile().getAbsolutePath().equals(fr94Directory.getAbsolutePath())); //removes files with parent as fr94(.DS_STORE and readchg and readmefr)

        fbisList.removeIf(e->e.getName().contains(".txt")); //removes files  with .txt extension(readchg and readmefb)

        latimesList.removeIf(e->e.getName().contains(".txt")); //removes files with .txt extension(readchg and readmela)

        ArrayList<Document> parsedDocumentList = new ArrayList<>();

        parsedDocumentList.addAll(parseFT(ftList));
        parsedDocumentList.addAll(parseLATimes(latimesList));
        parsedDocumentList.addAll(parseFBIS(fbisList));
        parsedDocumentList.addAll(parseFR94(fr94List));


        // Analyzer that is used to process TextField
        Analyzer analyzer = new StandardAnalyzer();

        // To store an index in memory
        // Directory directory = new RAMDirectory();
        // To store an index on disk
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        config.setSimilarity(new BM25Similarity());
        // Index opening mode
        // IndexWriterConfig.OpenMode.CREATE = create a new index
        // IndexWriterConfig.OpenMode.APPEND = open an existing index
        // IndexWriterConfig.OpenMode.CREATE_OR_APPEND = create an index if it
        // does not exist, otherwise it opens it
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        IndexWriter iWriter = new IndexWriter(directory, config);

        long count = 0;

        for(Document d : parsedDocumentList)
        {
            count++;
            System.out.println("INDEXING: " + d.get("document-number"));
            iWriter.addDocument(d);
        }

        System.out.println("Index Created.");


        // Commit changes and close everything
        iWriter.close();
        directory.close();


    }

    private static ArrayList<Document> parseFT(ArrayList<File> ftList) throws IOException {

        ArrayList<Document> documentArrayList = new ArrayList<>();
        StringBuilder documentContents = new StringBuilder();
        for(File f : ftList)
        {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = null;

            while ((line = reader.readLine()) != null)
            {
                if(line.contains("<DOC>"))
                    documentContents = new StringBuilder();
                else
                {
                    if(!line.contains("</DOC>"))
                        documentContents.append(line);
                    else {
                        String docNo = documentContents.substring((documentContents.indexOf("<DOCNO>") + 7), documentContents.indexOf("</DOCNO>")); // Number added to index is length of the tag
//                        String profile = documentContents.substring((documentContents.indexOf("<PROFILE>") + 9), documentContents.indexOf("</PROFILE>"));
                        String date = documentContents.substring((documentContents.indexOf("<DATE>") + 6), documentContents.indexOf("</DATE>"));
                        String headline = documentContents.substring((documentContents.indexOf("<HEADLINE>") + 10), documentContents.indexOf("</HEADLINE>"));
                        String byline = null;
                        if(documentContents.toString().contains("<BYLINE>"))
                            byline = documentContents.substring((documentContents.indexOf("<BYLINE>") + 8), documentContents.indexOf("</BYLINE>"));
                        else
                            byline = "";
                        String text = documentContents.substring((documentContents.indexOf("<TEXT>") + 6), documentContents.indexOf("</TEXT>"));
//                        String publisher = documentContents.substring((documentContents.indexOf("<PUB>") + 5), documentContents.indexOf("</PUB>"));
//                        String page = documentContents.substring((documentContents.indexOf("<PAGE>") + 6), documentContents.indexOf("</PAGE>"));

                        // Creating the document
                        Document doc = new Document();
                        doc.add(new TextField("document-number", docNo, Field.Store.YES));
//                        doc.add(new TextField("profile", profile, Field.Store.YES));
                        doc.add(new TextField("date", date, Field.Store.YES));
                        doc.add(new TextField("headline", headline, Field.Store.YES));
                        doc.add(new TextField("byline", byline, Field.Store.YES));
                        doc.add(new TextField("text", text, Field.Store.YES));
//                        doc.add(new TextField("publisher", publisher, Field.Store.YES));
//                        doc.add(new TextField("page", page, Field.Store.YES));
                        doc.add(new TextField("subject", "", Field.Store.YES)); // Blank since there is no subject in ft

                        documentArrayList.add(doc);

                    }
                }
            }
        }
        System.out.println("Parsed FT List size: " + documentArrayList.size());

        return documentArrayList;

    }
    private static ArrayList<Document> parseLATimes(ArrayList<File> list) throws IOException {

        ArrayList<Document> documentArrayList = new ArrayList<>();
        StringBuilder documentContents = new StringBuilder();
        for(File f : list)
        {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = null;

            int documentNumber = 0;

            while ((line = reader.readLine()) != null)
            {
                if(line.contains("<DOC>"))
                    documentContents = new StringBuilder();
                else
                {
                    if(!line.contains("</DOC>") && !line.contains("<P>") && !line.contains("</P>"))
                        documentContents.append(line);
                    else if(line.contains("<P>") || line.contains("</P>"));
                        //don nothing
                    else {
                        String docNo = documentContents.substring((documentContents.indexOf("<DOCNO>") + 7), documentContents.indexOf("</DOCNO>")); // Number added to index is length of the tag
//                        String docId = documentContents.substring((documentContents.indexOf("<DOCID>") + 7), documentContents.indexOf("</DOCID>"));
                        String date = documentContents.substring((documentContents.indexOf("<DATE>") + 6), documentContents.indexOf("</DATE>"));

//                        String section = null;
//                        if(documentContents.toString().contains("<SECTION>"))
//                            section = documentContents.substring((documentContents.indexOf("<SECTION>") + 9), documentContents.indexOf("</SECTION>"));
//                        else
//                            section = "";

//                        String length = null;
//                        if(documentContents.toString().contains("<LENGTH>"))
//                            length = documentContents.substring((documentContents.indexOf("<LENGTH>") + 8), documentContents.indexOf("</LENGTH>"));
//                        else
//                            length = "";

                        String headline = null;
                        if(documentContents.toString().contains("<HEADLINE>"))
                            headline = documentContents.substring((documentContents.indexOf("<HEADLINE>") + 10), documentContents.indexOf("</HEADLINE>"));
                        else
                            headline = "";

                        String byline = null;
                        if(documentContents.toString().contains("<BYLINE>"))
                            byline = documentContents.substring((documentContents.indexOf("<BYLINE>") + 8), documentContents.indexOf("</BYLINE>"));
                        else
                            byline = "";

//                        String type = null;
//                        if(documentContents.toString().contains("<TYPE>"))
//                            type = documentContents.substring((documentContents.indexOf("<TYPE>") + 6), documentContents.indexOf("</TYPE>"));
//                        else
//                            type = "";

                        String subject = null;
                        if(documentContents.toString().contains("<SUBJECT>"))
                            subject = documentContents.substring((documentContents.indexOf("<SUBJECT>") + 9), documentContents.indexOf("</SUBJECT>"));
                        else
                            subject = "";

//                        String publisher = null;
//                        if(documentContents.toString().contains("<PUB>"))
//                            publisher = documentContents.substring((documentContents.indexOf("<PUB>") + 5), documentContents.indexOf("</PUB>"));
//                        else
//                            publisher = "";

//                        String page = null;
//                        if(documentContents.toString().contains("<PAGE>"))
//                            page = documentContents.substring((documentContents.indexOf("<PAGE>") + 6), documentContents.indexOf("</PAGE>"));
//                        else
//                            page = "";

                        String text = null;
                        if(documentContents.toString().contains("<TEXT>"))
                            text = documentContents.substring((documentContents.indexOf("<TEXT>") + 6), documentContents.indexOf("</TEXT>"));
                        else
                            text = "";

                        // Creating the document
                        Document doc = new Document();
                        doc.add(new TextField("document-number", docNo, Field.Store.YES));
//                        doc.add(new TextField("document-id", docId, Field.Store.YES));
                        doc.add(new TextField("date", date, Field.Store.YES));
                        doc.add(new TextField("headline", headline, Field.Store.YES));
                        doc.add(new TextField("byline", byline, Field.Store.YES));
//                        doc.add(new TextField("section", section, Field.Store.YES));
//                        doc.add(new TextField("length", length, Field.Store.YES));
//                        doc.add(new TextField("type", type, Field.Store.YES));
                        doc.add(new TextField("subject", subject, Field.Store.YES));
                        doc.add(new TextField("text", text, Field.Store.YES));
//                        doc.add(new TextField("publisher", publisher, Field.Store.YES));
//                        doc.add(new TextField("page", page, Field.Store.YES));

                        documentArrayList.add(doc);

                    }
                }
            }
        }
        System.out.println("Parsed LATimes List size: " + documentArrayList.size());

        return documentArrayList;
    }

    private static ArrayList<Document> parseFBIS(ArrayList<File> list) throws IOException {


        ArrayList<Document> documentArrayList = new ArrayList<>();

        StringBuilder documentContents = new StringBuilder();
        for(File f : list)
        {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = null;

            while ((line = reader.readLine()) != null)
            {
                if(line.contains("<DOC>"))
                    documentContents = new StringBuilder();
                else
                {
                    if(!line.contains("</DOC>"))
                        documentContents.append(line);
                    else {
                        String docNo = documentContents.substring((documentContents.indexOf("<DOCNO>") + 7), documentContents.indexOf("</DOCNO>")); // Number added to index is length of the tag
//                        String headercode = documentContents.substring((documentContents.indexOf("<HT>") + 4), documentContents.indexOf("</HT>"));

                        String date = documentContents.substring((documentContents.indexOf("<DATE1>") + 7), documentContents.indexOf("</DATE1>"));
                        String headline = documentContents.substring((documentContents.indexOf("<TI>") + 4), documentContents.indexOf("</TI>"));
//                        String ADType = documentContents.substring((documentContents.indexOf("</DATE1>") + 8), documentContents.indexOf("<H3>"));
                        String text = documentContents.substring((documentContents.indexOf("<TEXT>") + 6), documentContents.indexOf("</TEXT>"));

//                        String Headline2 = null;
//                        if(documentContents.toString().contains("<H2>"))
//                            Headline2 = documentContents.substring((documentContents.indexOf("<H2>") + 4), documentContents.indexOf("</H2>"));
//                        else
//                            Headline2 = "";


                        // Creating the document
                        Document doc = new Document();
                        doc.add(new TextField("document-number", docNo, Field.Store.YES));
//                        doc.add(new TextField("header-code", headercode, Field.Store.YES));
                        doc.add(new TextField("date", date, Field.Store.YES));
                        doc.add(new TextField("headline", headline, Field.Store.YES));
                        doc.add(new TextField("byline", "", Field.Store.YES)); //Blank
                        doc.add(new TextField("subject", "", Field.Store.YES)); //Blank
//                        doc.add(new TextField("Article & Doc type", ADType, Field.Store.YES));
                        doc.add(new TextField("text", text, Field.Store.YES));
//                        doc.add(new TextField("Headline2", Headline2, Field.Store.YES));

                        documentArrayList.add(doc);
                    }
                }
            }
        }
        System.out.println("Parsed FBIS List size: " + documentArrayList.size());

        return documentArrayList;
    }

    private static ArrayList<Document> parseFR94(ArrayList<File> list) throws IOException {


        ArrayList<Document> documentArrayList = new ArrayList<>();

        StringBuilder documentContents = new StringBuilder();
        for(File f : list)
        {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = null;

            while ((line = reader.readLine()) != null)
            {
                if(line.contains("<DOC>"))
                    documentContents = new StringBuilder();
                else
                {
                    if(!line.contains("</DOC>") && !line.contains("<!--") && !line.contains("-->"))
                        documentContents.append(line);
                    else if(line.contains("</DOC>"))
                    {
                        String docNo = documentContents.substring((documentContents.indexOf("<DOCNO>") + 7), documentContents.indexOf("</DOCNO>")); // Number added to index is length of the tag
                        String date = null;
                        if(documentContents.toString().contains("<DATE>"))
                            date = documentContents.substring((documentContents.indexOf("<DATE>") + 6), documentContents.indexOf("</DATE>"));
                        else
                            date = "";

                        String text = documentContents.substring((documentContents.indexOf("<TEXT>") + 6), documentContents.indexOf("</TEXT>"));

                        // Creating the document
                        Document doc = new Document();
                        doc.add(new TextField("document-number", docNo, Field.Store.YES));
                        doc.add(new TextField("date", date, Field.Store.YES));
                        doc.add(new TextField("headline", "", Field.Store.YES)); //Blank
                        doc.add(new TextField("byline", "", Field.Store.YES)); //Blank
                        doc.add(new TextField("subject", "", Field.Store.YES)); //Blank
                        doc.add(new TextField("text", text, Field.Store.YES));

                        documentArrayList.add(doc);
                    }
                    else if(line.contains("<!--") || line.contains("-->")); //Ignores all comments in the document
                    //don nothing
                }
            }
        }
        System.out.println("Parsed FR94 List size: " + documentArrayList.size());

        return documentArrayList;
    }


    public static ArrayList<File> addFilesToList(File dir) {

        ArrayList<File> files = new ArrayList<>();

        Collection fileCollection = FileUtils.listFiles(dir, new RegexFileFilter("^(.*?)"), DirectoryFileFilter.DIRECTORY);

        Iterator iterator = fileCollection.iterator();

        while (iterator.hasNext())
            files.add((File) iterator.next());

        return files;
    }


}
