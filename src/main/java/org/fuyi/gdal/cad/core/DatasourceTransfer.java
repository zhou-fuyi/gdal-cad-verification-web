package org.fuyi.gdal.cad.core;

import org.gdal.ogr.Geometry;
import org.gdal.osr.SpatialReference;

import java.util.Vector;

public interface DatasourceTransfer {

    boolean translateLayer(String srcSourPath,
                           String destSourceDbPath,
                           Vector lcoOptions,
                           String nlnNameForNewLayer,
                           boolean bTransform,
                           SpatialReference targetSRS,
                           SpatialReference sourceSRS,
                           Vector papszSelFields,
                           boolean bAppend,
                           int nltForGeomType,
                           boolean bOverwrite,
                           SimpleDatasourceTransfer.GeomOperation eGeomOp,
                           double dfGeomOpParam,
                           Vector papszFieldTypesToString,
                           long nCountLayerFeatures,
                           Geometry poClipSrc,
                           Geometry poClipDst,
                           boolean bExplodeCollections,
                           String pszZField,
                           String pszWHERE,
                           org.gdal.gdal.ProgressCallback pfnProgress);
}
