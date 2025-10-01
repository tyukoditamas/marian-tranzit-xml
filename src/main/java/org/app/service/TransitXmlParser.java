package org.app.service;

import org.app.dto.HouseConsigment;
import org.app.dto.TranzitDto;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TransitXmlParser {

    public TranzitDto parse(File xmlFile) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false); // we use local-name() XPaths, so namespace-agnostic
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(xmlFile);
        doc.getDocumentElement().normalize();

        TranzitDto dto = new TranzitDto();
        dto.setDataMrn(eval(doc, "/*[local-name()='CC015C']/*[local-name()='preparationDateAndTime']/text()"));
        dto.setCustomOfficeDeparture(eval(doc, "/*[local-name()='CC015C']/*[local-name()='CustomsOfficeOfDeparture']/*[local-name()='referenceNumber']/text()"));
        dto.setContainerNumber(eval(doc, "/*[local-name()='CC015C']/*[local-name()='Consignment']/*[local-name()='TransportEquipment']/*[local-name()='containerIdentificationNumber']/text()"));
        dto.setDepartureTransportMeanFirst(eval(doc, "/*[local-name()='CC015C']/*[local-name()='Consignment']/*[local-name()='DepartureTransportMeans'][1]/*[local-name()='identificationNumber']/text()"));
        dto.setDepartureTransportMeanSecond(eval(doc, "/*[local-name()='CC015C']/*[local-name()='Consignment']/*[local-name()='DepartureTransportMeans'][2]/*[local-name()='identificationNumber']/text()"));

        NodeList hcNodes = (NodeList) xp().evaluate(
                "/*[local-name()='CC015C']/*[local-name()='Consignment']/*[local-name()='HouseConsignment']",
                doc, XPathConstants.NODESET);

        List<HouseConsigment> list = new ArrayList<>();
        for (int i = 0; i < hcNodes.getLength(); i++) {
            Node n = hcNodes.item(i);
            HouseConsigment hc = new HouseConsigment();

            String consigneeName = eval(n, "./*[local-name()='Consignee']/*[local-name()='name']/text()");
            if (!consigneeName.isEmpty())
                hc.setConsigneName(consigneeName);
            else
                hc.setConsigneeNumber(eval(n, "./*[local-name()='Consignee']/*[local-name()='identificationNumber']/text()"));

            hc.setPacks(eval(n, "./*[local-name()='ConsignmentItem']/*[local-name()='Packaging']/*[local-name()='numberOfPackages']/text()"));
            hc.setWeight(eval(n, "./*[local-name()='grossMass']/text()"));
            hc.setGoodsDescription(eval(n, "./*[local-name()='ConsignmentItem']/*[local-name()='Commodity']/*[local-name()='descriptionOfGoods']/text()"));
            hc.setConsignorName(eval(n, "./*[local-name()='Consignor']/*[local-name()='name']/text()"));
            hc.setIncadrareTarifara(eval(n, "./*[local-name()='ConsignmentItem']/*[local-name()='Commodity']/*[local-name()='CommodityCode']/*[local-name()='harmonizedSystemSubHeadingCode']/text()"));
            list.add(hc);
        }
        dto.setHouseConsigmentList(list);
        return dto;
    }

    private static XPath xp() { return XPathFactory.newInstance().newXPath(); }

    private static String eval(Object item, String expr) throws XPathExpressionException {
        String v = (String) xp().evaluate(expr, item, XPathConstants.STRING);
        return v == null ? "" : v.trim();
    }
}
