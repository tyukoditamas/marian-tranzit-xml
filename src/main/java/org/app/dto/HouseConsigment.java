package org.app.dto;

public class HouseConsigment {
    private String consigneeNumber = "";
    private String consigneeName = "";
    private String packs = "";
    private String weight = "";
    private String goodsDescription = "";
    private String consignorName = "";
    private String incadrareTarifara = "";

    public String getConsigneeNumber() { return consigneeNumber; }
    public void setConsigneeNumber(String v) { this.consigneeNumber = v == null ? "" : v; }

    public String getConsigneeName() { return consigneeName; }
    public void setConsigneName(String v) { this.consigneeName = v == null ? "" : v; }

    public String getPacks() { return packs; }
    public void setPacks(String v) { this.packs = v == null ? "" : v; }

    public String getWeight() { return weight; }
    public void setWeight(String v) { this.weight = v == null ? "" : v; }

    public String getGoodsDescription() { return goodsDescription; }
    public void setGoodsDescription(String v) { this.goodsDescription = v == null ? "" : v; }

    public String getConsignorName() { return consignorName; }
    public void setConsignorName(String v) { this.consignorName = v == null ? "" : v; }

    public String getIncadrareTarifara() { return incadrareTarifara; }
    public void setIncadrareTarifara(String v) { this.incadrareTarifara = v == null ? "" : v; }
}
