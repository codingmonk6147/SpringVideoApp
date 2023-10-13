package com.springaws.SpringVideoApp;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@Service
public class VideoStreamService {
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String VIDEO_CONTENT = "video/";

    private S3Client getClient() {

        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider
                        .create(AwsBasicCredentials.create("accesskey", "secretkey")))
                .region(Region.AP_SOUTH_1)
                .build();
    }

    // Places a new video into an Amazon S3 bucket.
    public void putVideo(byte[] bytes, String bucketName, String fileName, String description) {

        // Create an S3 client (an object that allows us to interact with Amazon S3).
        S3Client s3 = getClient();

        try {
            // Set the tags to apply to the object.
            String theTags = "name="+fileName+"&description="+description;
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .tagging(theTags)
                    .build();

            s3.putObject(putOb, RequestBody.fromBytes(bytes));

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    // Returns a schema that describes all tags for all videos in the given bucket.
    public String getTags(String bucketName){
        S3Client s3 = getClient();

        try {
            // Prepare a request to list the objects (files) in a specified S3 bucket.
            ListObjectsRequest listObjects = ListObjectsRequest.builder()
                    .bucket(bucketName)
                    .build();

            // Send the list objects request to Amazon S3 and get a response.
            ListObjectsResponse res = s3.listObjects(listObjects);

            // Send the list objects request to Amazon S3 and get a response.
            List<S3Object> objects = res.contents();


            // Send the list objects request to Amazon S3 and get a response.
            List<String> keys = new ArrayList<>();

            // Loop through the list of S3 objects.
            for (S3Object myValue: objects) {

                // Get the key (identifier) of each object, which is used to retrieve tags.
                String key = myValue.key(); // We need the key to get the tags.

                // Prepare a request to get the tags associated with this object.
                GetObjectTaggingRequest getTaggingRequest = GetObjectTaggingRequest.builder()
                        .key(key)
                        .bucket(bucketName)
                        .build();

                // Send the request to Amazon S3 and get the response containing tags.
                GetObjectTaggingResponse tags = s3.getObjectTagging(getTaggingRequest);

                // Get the list of tags associated with the object.
                List<Tag> tagSet= tags.tagSet();

                // Get the list of tags associated with the object.
                for (Tag tag : tagSet) {
                    keys.add(tag.value());
                }
            }

            // Process the list of keys (e.g., modify or sort them) and convert to a string
            List<Tags> tagList = modList(keys);
            return convertToString(toXml(tagList));

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return "";
    }

    // Return a List where each element is a Tags object.
    private List<Tags> modList(List<String> myList){
        // Get the elements from the collection.
        int count = myList.size();
        List<Tags> allTags = new ArrayList<>();
        Tags myTag ;
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();

        for ( int index=0; index < count; index++) {
            if (index % 2 == 0)
                keys.add(myList.get(index));
            else
                values.add(myList.get(index));
        }

        // Create a list where each element is a Tags object.
        for (int r=0; r<keys.size(); r++){
            myTag = new Tags();
            myTag.setName(keys.get(r));
            myTag.setDesc(values.get(r));
            allTags.add(myTag);
        }
        return allTags;
    }


    // Reads a video from a bucket and returns a ResponseEntity.
    public ResponseEntity<byte[]> getObjectBytes (String bucketName, String keyName) {
        S3Client s3 = getClient();
        try {
            // create a GetObjectRequest instance.
            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .key(keyName)
                    .bucket(bucketName)
                    .build();

            // get the byte[] from this AWS S3 object and return a ResponseEntity.
            ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(objectRequest);
            return ResponseEntity.status(HttpStatus.OK)
                    .header(CONTENT_TYPE, VIDEO_CONTENT + "mp4")
                    .header(CONTENT_LENGTH, String.valueOf(objectBytes.asByteArray().length))
                    .body(objectBytes.asByteArray());

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return null;
    }


    // Convert a LIST to XML data.
    private Document toXml(List<Tags> itemList) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Start building the XML
            Element root = doc.createElement( "Tags" );
            doc.appendChild( root );

            // Iterate through the list.
            for (Tags myItem: itemList) {

                Element item = doc.createElement( "Tag" );
                root.appendChild( item );

                // Set Name
                Element id = doc.createElement( "Name" );
                id.appendChild( doc.createTextNode(myItem.getName() ) );
                item.appendChild( id );

                // Set Description
                Element name = doc.createElement( "Description" );
                name.appendChild( doc.createTextNode(myItem.getDesc() ) );
                item.appendChild( name );
            }

            return doc;
        } catch(ParserConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String convertToString(Document xml) {
        try {
            TransformerFactory transformerFactory = getSecureTransformerFactory();
            Transformer transformer = transformerFactory.newTransformer();
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(xml);
            transformer.transform(source, result);
            return result.getWriter().toString();

        } catch(TransformerException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static TransformerFactory getSecureTransformerFactory() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
        return transformerFactory;
    }
}
