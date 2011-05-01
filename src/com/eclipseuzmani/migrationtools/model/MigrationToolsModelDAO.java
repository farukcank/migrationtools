package com.eclipseuzmani.migrationtools.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;


public class MigrationToolsModelDAO {
	private static class MigrationToolsHandler extends DefaultHandler {
		public final MigrationToolsModel migrationToolsModel;
		
		public MigrationToolsHandler(MigrationToolsModel migrationToolsModel) {
			if(migrationToolsModel==null)
				throw new IllegalArgumentException();
			this.migrationToolsModel = migrationToolsModel;
		}

		private StringBuilder sb = new StringBuilder();

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			sb.delete(0, sb.length());
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			sb.append(ch, start, length);
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			if (localName.equals("pre")) {
				migrationToolsModel.setPre(sb.toString());
			} else if (localName.equals("detail")) {
				migrationToolsModel.setDetail(sb.toString());
			} else if (localName.equals("post")) {
				migrationToolsModel.setPost(sb.toString());
			}
		}

		@Override
		public void warning(SAXParseException e) throws SAXException {
			throw e;
		}

		@Override
		public void error(SAXParseException e) throws SAXException {
			throw e;
		}

		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			throw e;
		}

	}

	private static final String SCHEMA_URI = "http://www.eclipseuzmani.com/migrationtools";
	
	public void read(InputStream in, MigrationToolsModel migrationToolsModel) throws IOException, SAXException{
		String language = XMLConstants.W3C_XML_SCHEMA_NS_URI;
		SchemaFactory factory = SchemaFactory.newInstance(language);
		Schema schema = factory.newSchema(MigrationToolsModelDAO.class
				.getResource("migrationtools.xsd"));
		XMLReader xr = XMLReaderFactory.createXMLReader();
		ValidatorHandler validatorHandler = schema.newValidatorHandler();
		MigrationToolsHandler migrationToolsHandler = new MigrationToolsHandler(migrationToolsModel);
		validatorHandler.setContentHandler(migrationToolsHandler);
		xr.setContentHandler(validatorHandler);
		xr.setErrorHandler(migrationToolsHandler);
		xr.parse(new InputSource(in));
	}

	public void write(OutputStream out, MigrationToolsModel migrationToolsModel)
			throws TransformerFactoryConfigurationError,
			TransformerConfigurationException, SAXException {
		StreamResult streamResult = new StreamResult(out);
		SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory
				.newInstance();
		// SAX2.0 ContentHandler.
		TransformerHandler hd = tf.newTransformerHandler();
		Transformer serializer = hd.getTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		hd.setResult(streamResult);
		hd.startDocument();
		AttributesImpl atts = new AttributesImpl();
		atts.clear();
		hd.startElement(SCHEMA_URI, "", "migrationToolsModel", atts);

		atts.clear();
		hd.startElement(SCHEMA_URI, "", "pre", atts);
		hd.characters(migrationToolsModel.getPre().toCharArray(), 0, migrationToolsModel.getPre()
				.length());
		hd.endElement(SCHEMA_URI, "", "pre");

		atts.clear();
		hd.startElement(SCHEMA_URI, "", "detail", atts);
		hd.characters(migrationToolsModel.getDetail().toCharArray(), 0, migrationToolsModel
				.getDetail().length());
		hd.endElement(SCHEMA_URI, "", "detail");

		atts.clear();
		hd.startElement(SCHEMA_URI, "", "post", atts);
		hd.characters(migrationToolsModel.getPost().toCharArray(), 0, migrationToolsModel.getPost()
				.length());
		hd.endElement(SCHEMA_URI, "", "post");

		hd.endElement(SCHEMA_URI, "", "migrationToolsModel");
		hd.endDocument();
	}
}
