package com.example.support.importer;

import com.example.support.dto.CreateTicketRequest;
import com.example.support.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class XmlImportParser implements ImportParser {

    private static final Logger log = LoggerFactory.getLogger(XmlImportParser.class);

    @Override
    public List<ParseResult> parse(InputStream inputStream) {
        List<ParseResult> results = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setCoalescing(true);
            Document document = factory.newDocumentBuilder().parse(inputStream);

            NodeList nodes = document.getElementsByTagName("ticket");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                try {
                    CreateTicketRequest request = mapElement(element);
                    results.add(new ParseResult(request, i, null));
                } catch (IllegalArgumentException ex) {
                    String message = "Element " + i + ": " + ex.getMessage();
                    log.warn(message, ex);
                    results.add(new ParseResult(null, i, message));
                }
            }
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            String message = "XML parse error: " + ex.getMessage();
            log.warn(message, ex);
            results.add(new ParseResult(null, 0, message));
        }
        return results;
    }

    private CreateTicketRequest mapElement(Element element) {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setCustomerId(getText(element, "customer_id"));
        request.setCustomerEmail(getText(element, "customer_email"));
        request.setCustomerName(getText(element, "customer_name"));
        request.setSubject(getText(element, "subject"));
        request.setDescription(getText(element, "description"));
        request.setCategory(parseEnum(getText(element, "category"), Category.class));
        request.setPriority(parseEnum(getText(element, "priority"), Priority.class));
        request.setStatus(parseStatus(getText(element, "status")));
        request.setAssignedTo(getText(element, "assigned_to"));
        request.setTags(parseTags(getText(element, "tags")));
        request.setSource(parseEnum(getText(element, "source"), Source.class));
        request.setBrowser(getText(element, "browser"));
        request.setDeviceType(parseEnum(getText(element, "device_type"), DeviceType.class));
        return request;
    }

    private String getText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

    private List<String> parseTags(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toList());
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumClass) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Invalid value '" + value + "' for enum " + enumClass.getSimpleName());
    }

    private Status parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if ("new".equalsIgnoreCase(value)) {
            return Status.new_;
        }
        return parseEnum(value, Status.class);
    }
}
