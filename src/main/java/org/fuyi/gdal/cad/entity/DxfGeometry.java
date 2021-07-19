package org.fuyi.gdal.cad.entity;

import java.util.Arrays;

public class DxfGeometry {

    private byte[] geometry;

    private String layer;

    private String paperSpace;

    private String subClasses;

    private String lineType;

    private String entityHandle;

    private String text;

    private GeomType geomType;

    /**
     * 一维数组存储key value数据对
     * 从0开始，每两个数据为一对
     */
    private String[] fields = new String[0];

    public DxfGeometry() {
    }

    public DxfGeometry(byte[] geometry, String layer, String paperSpace, String subClasses, String lineType, String entityHandle, String text, GeomType geomType, String[] fields) {
        this.geometry = geometry;
        this.layer = layer;
        this.paperSpace = paperSpace;
        this.subClasses = subClasses;
        this.lineType = lineType;
        this.entityHandle = entityHandle;
        this.text = text;
        this.geomType = geomType;
        this.fields = fields;
    }

    public byte[] getGeometry() {
        return geometry;
    }

    public void setGeometry(byte[] geometry) {
        this.geometry = geometry;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public String getPaperSpace() {
        return paperSpace;
    }

    public void setPaperSpace(String paperSpace) {
        this.paperSpace = paperSpace;
    }

    public String getSubClasses() {
        return subClasses;
    }

    public void setSubClasses(String subClasses) {
        this.subClasses = subClasses;
    }

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    public String getEntityHandle() {
        return entityHandle;
    }

    public void setEntityHandle(String entityHandle) {
        this.entityHandle = entityHandle;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public GeomType getGeomType() {
        return geomType;
    }

    public void setGeomType(GeomType geomType) {
        this.geomType = geomType;
    }

    public String[] getFields() {
        return fields;
    }

    public void setFields(String[] fields) {
        this.fields = fields;
    }

    @Override
    public String toString() {
        return "DxfGeometry{" +
//                "geometry=" + Arrays.toString(geometry) +
                "geometry=" + geometry +
                ", layer='" + layer + '\'' +
                ", paperSpace='" + paperSpace + '\'' +
                ", subClasses='" + subClasses + '\'' +
                ", lineType='" + lineType + '\'' +
                ", entityHandle='" + entityHandle + '\'' +
                ", text='" + text + '\'' +
                ", geomType=" + geomType +
                ", fields=" + Arrays.toString(fields) +
                '}';
    }
}
