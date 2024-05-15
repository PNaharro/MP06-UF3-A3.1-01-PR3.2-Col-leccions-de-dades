package cat.iesesteveterradas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

public class PR32QueryMain {
    private static final Logger logger = LoggerFactory.getLogger(PR32QueryMain.class);

    public static void main(String[] args) {
        try (var mongoClient = MongoClients.create("mongodb://elTeuUsuari:laTeuaContrasenya@localhost:27017")) {
            MongoDatabase database = mongoClient.getDatabase("PR32");
            MongoCollection<Document> collection = database.getCollection("Posts");
            logger.info("Connected to MongoDB");

            // Ejercicio 1: Calcular el promedio de ViewCount y consultar documentos con ViewCount mayor que el promedio
            double averageViewCount = calculateAverageViewCount(collection);
            FindIterable<Document> result1 = queryByViewCountGreaterThanAverage(collection, averageViewCount);

            // Generar PDF para el Ejercicio 1
            createPDF(result1, "informe1.pdf", doc -> doc.getString("title") + "; ViewCounts: " + doc.getInteger("viewCount"));

            // Ejercicio 2: Buscar documentos que contengan ciertas palabras en el título
            List<String> wordsToSearch = Arrays.asList("pug", "wig", "yak", "nap", "jig", "mug", "zap", "gag", "oaf", "elf", "hat", "D&D");
            FindIterable<Document> result2 = queryByTitleContainingWords(collection, wordsToSearch);

            // Generar PDF para el Ejercicio 2
            createPDF(result2, "informe2.pdf", doc -> doc.getString("title"));

        } catch (Exception e) {
            logger.error("An error occurred: ", e);
        }
    }

    private static double calculateAverageViewCount(MongoCollection<Document> collection) {
        List<Document> allDocuments = new ArrayList<>();
        try (MongoCursor<Document> allCursor = collection.find().iterator()) {
            while (allCursor.hasNext()) {
                allDocuments.add(allCursor.next());
            }
        }
        double totalViewCount = 0;
        for (Document doc : allDocuments) {
            totalViewCount += doc.getInteger("viewCount", 0);
        }
        double averageViewCount = totalViewCount / allDocuments.size();
        logger.info("Average viewCount: " + averageViewCount);
        return averageViewCount;
    }

    private static FindIterable<Document> queryByViewCountGreaterThanAverage(MongoCollection<Document> collection, double averageViewCount) {
        Document query = new Document("viewCount", new Document("$gt", averageViewCount));
        FindIterable<Document> result = collection.find(query);
        logger.info("Query 1 done");
        return result;
    }

    private static FindIterable<Document> queryByTitleContainingWords(MongoCollection<Document> collection, List<String> wordsToSearch) {
        String regexPattern = String.join("|", wordsToSearch);
        Document query = new Document("title", new Document("$regex", regexPattern));
        FindIterable<Document> result = collection.find(query);
        logger.info("Query 2 done");
        return result;
    }

    private static void createPDF(FindIterable<Document> documents, String fileName, TextExtractor textExtractor) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            // Inicializar contenido del PDF
            try (PDPageContentStream contents = new PDPageContentStream(doc, page)) {
                contents.beginText();
                contents.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contents.setLeading(14.5f);
                contents.newLineAtOffset(25, 750);

                // Agregar contenido al PDF
                for (Document docMongoDB : documents) {
                    String text = textExtractor.extractText(docMongoDB);
                    if (text != null && !text.isEmpty()) {
                        logger.info("Adding text to PDF: " + text);
                        contents.showText(text);
                        contents.newLine();
                    } else {
                        logger.warn("Empty or null text found in document: " + docMongoDB.toJson());
                    }
                }
                contents.endText();
            }

            // Guardar el PDF
            String outputPath = System.getProperty("user.dir") + "/data/out/" + fileName;
            doc.save(outputPath);
            logger.info("PDF created: " + outputPath);
        } catch (IOException e) {
            logger.error("Error creating PDF: ", e);
        }
    }

    @FunctionalInterface
    interface TextExtractor {
        String extractText(Document document);
    }
}
