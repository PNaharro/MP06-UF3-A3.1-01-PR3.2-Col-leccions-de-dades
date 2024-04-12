package cat.iesesteveterradas;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class PR32CreateMain {

    private static final Logger logger = Logger.getLogger(PR32CreateMain.class.getName());

    public static void main(String[] args) {
        try {
            // Configurar el archivo de registro
            FileHandler fileHandler = new FileHandler("./data/logs/PR32CreateMain.java.log");
            logger.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);

            // Conexión a la base de datos MongoDB
            var mongoClient = MongoClients.create("mongodb://root:example@localhost:27017");
            MongoDatabase database = mongoClient.getDatabase("yourDatabaseName");
            MongoCollection<Document> collection = database.getCollection("yourCollectionName");

            // Parsear el archivo XML
            File file = new File("data/Posts.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            org.w3c.dom.Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            // Obtener la lista de nodos 'post'
            NodeList nodeList = doc.getElementsByTagName("row");

            // Crear un nuevo documento XML para almacenar las preguntas seleccionadas
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document resultDoc = builder.newDocument();
            Element rootElement = resultDoc.createElement("posts");
            resultDoc.appendChild(rootElement);

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    // Obtener los datos relevantes de cada pregunta
                    String viewCountStr = element.getAttribute("ViewCount");
                    int viewCount = 0; // Valor predeterminado en caso de que el atributo esté vacío
                    if (!viewCountStr.isEmpty()) {
                        viewCount = Integer.parseInt(viewCountStr);
                    }
                    String title = decodeEntities(element.getAttribute("Title"));
                    String body = decodeEntities(element.getAttribute("Body"));

                    // Insertar en MongoDB si el post es una pregunta y cumple los requisitos
                    if (element.getAttribute("PostTypeId").equals("1")) {
                        Document pregunta = new Document("Id", element.getAttribute("Id"))
                                .append("PostTypeId", element.getAttribute("PostTypeId"))
                                .append("AcceptedanswerId", element.getAttribute("AcceptedanswerId"))
                                .append("CreationDate", element.getAttribute("CreationDate"))
                                .append("Score", element.getAttribute("Score"))
                                .append("ViewCount", viewCount)
                                .append("Body", body)
                                .append("OwnerUserId", element.getAttribute("OwnerUserId"))
                                .append("LastActivityDate", element.getAttribute("LastActivityDate"))
                                .append("Title", title)
                                .append("Tags", element.getAttribute("Tags"));

                        collection.insertOne(pregunta);

                        // Agregar la pregunta al documento XML de resultados
                        Element postElement = resultDoc.createElement("post");
                        postElement.setAttribute("Id", element.getAttribute("Id"));
                        postElement.setAttribute("Title", title);
                        postElement.setAttribute("Body", body);
                        postElement.setAttribute("ViewCount", String.valueOf(viewCount));
                        rootElement.appendChild(postElement);
                    }
                }
            }

            // Guardar el documento XML de resultados en un archivo
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(resultDoc);
            StreamResult streamResult = new StreamResult(new File("data/SelectedPosts.xml"));
            transformer.transform(domSource, streamResult);

            mongoClient.close(); // Cerrar la conexión a MongoDB
            logger.log(Level.INFO, "Proceso completado. Resultados guardados en SelectedPosts.xml");
            System.out.println("Proceso completado. Resultados guardados en SelectedPosts.xml");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error en el proceso:", e);
            e.printStackTrace();
        }
    }

    // Método para decodificar entidades HTML
    private static String decodeEntities(String text) {
        return text.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&amp;", "&");
    }
}
