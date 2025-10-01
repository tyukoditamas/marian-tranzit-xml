package org.app.dto;

import java.util.ArrayList;
import java.util.List;

public class TranzitDto {
    private String dataMrn = "";
    private String customOfficeDeparture = "";
    private String containerNumber = "";
    private List<HouseConsigment> houseConsigmentList = new ArrayList<>();
    private String departureTransportMeanFirst = "";
    private String departureTransportMeanSecond = "";

    public String getDataMrn() { return dataMrn; }
    public void setDataMrn(String v) { this.dataMrn = v == null ? "" : v; }

    public String getCustomOfficeDeparture() { return customOfficeDeparture; }
    public void setCustomOfficeDeparture(String v) { this.customOfficeDeparture = v == null ? "" : v; }

    public String getContainerNumber() { return containerNumber; }
    public void setContainerNumber(String v) { this.containerNumber = v == null ? "" : v; }

    public List<HouseConsigment> getHouseConsigmentList() { return houseConsigmentList; }
    public void setHouseConsigmentList(List<HouseConsigment> list) {
        this.houseConsigmentList = (list == null ? new ArrayList<>() : list);
    }

    public String getDepartureTransportMeanFirst() { return departureTransportMeanFirst; }
    public void setDepartureTransportMeanFirst(String v) { this.departureTransportMeanFirst = v == null ? "" : v; }

    public String getDepartureTransportMeanSecond() { return departureTransportMeanSecond; }
    public void setDepartureTransportMeanSecond(String v) { this.departureTransportMeanSecond = v == null ? "" : v; }

    @Override public String toString() {
        return "TranzitDto{dataMrn='" + dataMrn + "', customOfficeDeparture='" + customOfficeDeparture +
                "', containerNumber='" + containerNumber + "', houseConsigmentList(size)=" +
                (houseConsigmentList == null ? 0 : houseConsigmentList.size()) +
                ", departureTransportMeanFirst='" + departureTransportMeanFirst +
                "', departureTransportMeanSecond='" + departureTransportMeanSecond + "'}";
    }
}
