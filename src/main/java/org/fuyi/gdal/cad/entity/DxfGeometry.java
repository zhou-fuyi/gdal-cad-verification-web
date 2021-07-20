package org.fuyi.gdal.cad.entity;

import java.util.Arrays;

public class DxfGeometry {

    private byte[] geometry;

    private String layer;

    private GeomType geomType;

    /**
     * 一维数组存储key value数据对
     * 从0开始，每两个数据为一对
     */
    private String[] fields = new String[0];

    public DxfGeometry() {
    }

    public DxfGeometry(byte[] geometry, String layer, GeomType geomType, String[] fields) {
        this.geometry = geometry;
        this.layer = layer;
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
                ", geomType=" + geomType +
                ", fields=" + Arrays.toString(fields) +
                '}';
    }
}
