import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import java.io.*;
import java.util.*;
import static java.lang.System.exit;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.util.CoreMap;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexWriterConfig;


public class Watson {
    private static StandardAnalyzer analyzer = new StandardAnalyzer();
    private static Properties props = new Properties();

    public static void main(String[] args) throws FileNotFoundException, IOException {

        Watson watson = new Watson();
        FSDirectory index = FSDirectory.open(new File("IndexedWiki").toPath());
		
		// The following code is commented because it correspond to the index generation
        /*IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = null;
        try {
            w = new IndexWriter(index, config);
        } catch (IOException e) {
            e.printStackTrace();
            exit(1);
        }

        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");

        // Get every document
        File dir = new File("wiki-data");
        File[] files = dir.listFiles();
        if(files != null){
            int i=0;
            for(File f : files){
                watson.buildIndex(f,index,w);
                i++;
                System.out.println("Finished File: "+ i);
            }
            w.close();
        }*/

        // This code will open the index and return the MRR for the questions
        System.out.println("Built The index. Running through all questions from 'questions.txt' and getting MRR");
        File questions = new File("src/main/resources/questions.txt");
        Scanner sc = new Scanner(questions);
        int checker = 1;
        String query ="";
        String answer="";
        ArrayList<Float> mrrs = new ArrayList<>();
        while(sc.hasNextLine()) {
            // Account for title or newline
            if (checker == 1 || checker == 0) {
                checker++;
                sc.nextLine();
            }
            // Account for the query line
            else if (checker == 2) {
                query = sc.nextLine();
                query = query.toLowerCase();
                query = query.replaceAll("[^a-zA-Z0-9 ]", "");
                Sentence q = new Sentence(query);
                q.lemmas();
                checker++;
            }
            // Account for the answer line
            else if (checker == 3) {
                answer = sc.nextLine();
                answer = answer.toLowerCase();
                answer = answer.replaceAll("[^a-zA-Z0-9 ]", "");
                checker = 0;
                try {
                    mrrs.add(watson.answerQuery(query, index, answer));
                } catch (ParseException e) {
                    e.printStackTrace();
                    exit(1);
                }
                // Reset the variables after query
                answer = "";
                query = "";
            }
        }
		
        float totalMRR =0;
        for (Object mrr : mrrs) {
            totalMRR += (float) mrr;
        }
        totalMRR = totalMRR/100;
        System.out.println("MRR: "+totalMRR);
        System.out.println("Overall Score: "+totalMRR*100);
        sc.close();
    }

    private Directory buildIndex(File input,Directory index,IndexWriter w) throws FileNotFoundException {
        FileReader source = new FileReader(input);
        BufferedReader reader = new BufferedReader(source);
        String line;
        // The title of the Wiki Page
        String wikiTitle = "";
        // All the wiki page's info
        StringBuilder info = new StringBuilder();
        // Indicate if it's the first run of the program
        boolean firstTime = true;
        // Hash map to store each category
        HashMap<String, ArrayList<Category>> titleCats = new HashMap<>();
        HashMap<String, ArrayList<SubSection>> titleSubs = new HashMap<>();
        // Begin Indexing of the Wiki pages
        try {
            // Loop through the whole file
            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.equals("")) {
                    continue;
                }
                // Get the title of the page by checking for brackets [[ ]] and removing them
                // Indicates start of new document
                if (line.contains("[[") && line.contains("]]")){
                    // Skip over file names, media, and images
                    if(line.contains("[[File:") || line.contains("[[Media:") || line.contains("[[Image:") || line.contains("</ref>")) {
                        continue;
                    }
                    // Add the previous page, reset variables
                    if(!firstTime){
                        addDoc(w,wikiTitle,info.toString());
                        wikiTitle = "";
                        info.setLength(0);
                    }
                    line = line.substring(2);
                    line = line.substring(0, line.length() - 2);
                    // Lower case every title, and remove non-alphanumerics
                    line = line.replaceAll("[^a-zA-Z0-9 ]", "");
                    line = line.toLowerCase();
                    // Set the wikiTitle to the line
                    wikiTitle = line;
                    firstTime = false;
                    continue;
                }
                // After the title get the subsections,categories, and info
                // Get the subsection(s) of the page
                if (line.contains("==")) {
                    line = line.replaceAll("[^a-zA-Z0-9 ]", "");
                    titleSubs.putIfAbsent(wikiTitle, new ArrayList<SubSection>());
                    titleSubs.get(wikiTitle).add(new SubSection(wikiTitle, line.toLowerCase()));
                    continue;
                }
                // Get the categories of the page
                if (line.contains("CATEGORIES:")) {
                    line = line.substring(11);
                    String[] splitCats = line.split(",");
                    titleCats.putIfAbsent(wikiTitle, new ArrayList<Category>());
                    for (String splitCat : splitCats) {
                        splitCat = splitCat.replaceAll("[^a-zA-Z0-9 ]", "");
                        titleCats.get(wikiTitle).add(new Category(wikiTitle, splitCat.toLowerCase()));
                    }
                }
                // Otherwise it's the pages info
                else {
                    // Remove non-alphanumeric characters.
                    line = line.replaceAll("[^a-zA-Z0-9 ]", "");
                    // Account for line not having any alphanumeric txt
                    if(line.equals("") || !line.matches(".*\\w.*")){
                        continue;
                    }
                    // Lowercase and lemmatize the line
                    line = line.toLowerCase();

                    String procInfo = procInfo(line);
                    // Append to info
                    info.append(" ");
                    info.append(procInfo);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            exit(1);
        }
        return index;
    }

    // Method to process info
    private String procInfo(String info){
        String processedInfo = "";
        // Redwood is only to silence unnecessary nlp output
        RedwoodConfiguration.current().clear().apply();
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation anot = pipeline.process(info);
        List<CoreMap> tokenized = anot.get(CoreAnnotations.SentencesAnnotation.class);
        processedInfo +=tokenized.get(0);
        return processedInfo;
    }

    //Query the information stored in the index
    private float answerQuery(String query, Directory index,String answer) throws IOException, ParseException {
        Query q = new QueryParser("info",analyzer).parse(query);
        int hitsPerPage = 10;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        // Set similarity to BM25, doesn't change overall MRR
        //searcher.setSimilarity(new BM25Similarity());
        // Set similarity to Boolean Similarity, lowers overall MRR to .1883
        //searcher.setSimilarity(new BooleanSimilarity());
        TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;
        float mrr = 0;
        int position = 1;
        // Loop through hits and print values
        for (ScoreDoc hit : hits) {
            ResultClass result = new ResultClass();
            int docId = hit.doc;
            float score = hit.score;
            Document doc = searcher.doc(docId);
            result.DocName = doc;
            result.doc_score = score;
            // Calculate mrr if it matches one of the docs
            if(answer.contains(doc.get("docid"))){
                mrr = (float)1/position;
                return mrr;
            }
            position++;
        }
        reader.close();
        return mrr;
    }

    // Add Documents to the index
    private static void addDoc(IndexWriter w, String docID, String content) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("info", content, Field.Store.YES));
        doc.add(new StringField("docid", docID, Field.Store.YES));
        w.addDocument(doc);
    }
}