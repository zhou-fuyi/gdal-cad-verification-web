package org.fuyi.gdal.cad.core;

import org.gdal.gdal.ProgressCallback;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import org.gdal.osr.SpatialReference;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

@Component("shardingTransfer")
public class ConcurrentShardingDataSourceTransfer implements DatasourceTransfer {

    private static final int DEFAULT_TILE_SIZE = 200000;
    private static final int DEFAULT_THRESHOLD = 900000;
    private static final int DEFAULT_TILE_SIZE_THRESHOLD = 300000;

    static boolean bSkipFailures = true;
    static int nGroupTransactions = 2000;
    //    static int nGroupTransactions = 1;
    static boolean bPreserveFID = false;
    static final int OGRNullFID = -1;
    static int nFIDToFetch = OGRNullFID;

    public static class GeomOperation {
        private GeomOperation() {
        }

        public static GeomOperation NONE = new GeomOperation();
        public static GeomOperation SEGMENTIZE = new GeomOperation();
        public static GeomOperation SIMPLIFY_PRESERVE_TOPOLOGY = new GeomOperation();
    }

    static int wkbFlatten(int eType) {
        return eType & (~ogrConstants.wkb25DBit);
    }

    @Override
    public boolean translateLayer(String srcSourPath, String destSourceDbPath, Map<Integer, String> filters, Vector lcoOptions, String nlnNameForNewLayer, boolean bTransform, SpatialReference targetSRS, SpatialReference sourceSRS, Vector papszSelFields, boolean bAppend, int nltForGeomType, boolean bOverwrite, SimpleDatasourceTransfer.GeomOperation eGeomOp, double dfGeomOpParam, Vector papszFieldTypesToString, long nCountLayerFeatures, Geometry poClipSrc, Geometry poClipDst, boolean bExplodeCollections, String pszZField, String pszWHERE, ProgressCallback pfnProgress) {
        int tileSize = DEFAULT_TILE_SIZE;
        DataSource srcSource = ogr.Open(srcSourPath, false);
        if (srcSource == null) {
            throw new RuntimeException("[" + srcSourPath + "] 文件无法解析");
        }
        long featureCount = 0;
        try {
            Layer tempLayer = srcSource.ExecuteSQL(filters.get(nltForGeomType));
            featureCount = tempLayer.GetFeatureCount();
            tempLayer.delete();
            srcSource.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (featureCount > DEFAULT_THRESHOLD) {
            int availProcessors = Runtime.getRuntime().availableProcessors();
            tileSize = Long.valueOf(featureCount).intValue() / availProcessors;
            if (tileSize > DEFAULT_TILE_SIZE_THRESHOLD) {
                tileSize = DEFAULT_TILE_SIZE_THRESHOLD;
            }
        }
        System.out.println("Table is : " + nlnNameForNewLayer + " And the FeatureCount is : " + featureCount);
        System.out.println("Tile Size is : " + tileSize);
        // 分片逻辑
        int tileCount = Long.valueOf(featureCount).intValue() / tileSize;
        // 余数
        int tileRemainder = Long.valueOf(featureCount).intValue() % tileSize;

        int targetTileCount = tileRemainder == 0 ? tileCount : tileCount + 1;

        CountDownLatch countDownLatch = new CountDownLatch(targetTileCount);

        boolean[] results = new boolean[targetTileCount];
        int index = 0;
        while (index < targetTileCount) {
            int finalIndex = index;
            int finalTileSize = tileSize;
            ExecutorTemplate.executor.execute(() -> {
                try {
                    results[finalIndex] = concurrentShardingTranslateLayer(srcSourPath,
                            destSourceDbPath, filters, lcoOptions, nlnNameForNewLayer,
                            bTransform, targetSRS, sourceSRS, papszSelFields, bAppend,
                            nltForGeomType, bOverwrite, eGeomOp, dfGeomOpParam,
                            papszFieldTypesToString, nCountLayerFeatures, poClipSrc,
                            poClipDst, bExplodeCollections, pszZField, pszWHERE, pfnProgress,
                            targetTileCount, finalIndex, tileRemainder, finalTileSize);
                    System.out.println("图层转换作业: 线程" + Thread.currentThread().getName() + "已完成作业");
                } catch (Exception e) {
                    results[finalIndex] = false;
                    e.printStackTrace();
                }
                countDownLatch.countDown();
            });
            index++;
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        boolean finalResult = true;
        for (boolean result : results) {
            if (!result)
                finalResult = false;
            break;
        }
        return finalResult;
    }

    //实际执行转换操作
    private boolean concurrentShardingTranslateLayer(String srcSourPath, String destSourceDbPath, Map<Integer, String> filters, Vector lcoOptions, String nlnNameForNewLayer, boolean bTransform, SpatialReference targetSRS, SpatialReference sourceSRS, Vector papszSelFields, boolean bAppend, int nltForGeomType, boolean bOverwrite, SimpleDatasourceTransfer.GeomOperation eGeomOp, double dfGeomOpParam, Vector papszFieldTypesToString, long nCountLayerFeatures, Geometry poClipSrc, Geometry poClipDst, boolean bExplodeCollections, String pszZField, String pszWHERE, ProgressCallback pfnProgress, int tileCount, int currentThreadIndex, int tileRemainder, int tileSize) {

        DataSource srcSource = ogr.Open(srcSourPath, false);
        if (srcSource == null) {
            throw new RuntimeException("[" + srcSourPath + "] 文件无法解析");
        }
        DataSource destSource = ogr.Open(destSourceDbPath, true);
        if (destSource == null) {
            throw new RuntimeException("[" + destSourceDbPath + "] 无法获取链接");
        }
        Layer srcLayer = srcSource.ExecuteSQL(filters.get(nltForGeomType));

        int finalFeatureStartIndex = currentThreadIndex * tileSize;
        int finalFeatureEndIndex = (currentThreadIndex + 1) * tileSize;
        if (currentThreadIndex == (tileCount - 1) && tileRemainder > 0) {
            finalFeatureEndIndex = finalFeatureStartIndex + tileRemainder;
        }

        boolean bForceToPoint = false;
        boolean bForceToLineString = false;
        boolean bForceToPolygon = false;

        if (wkbFlatten(nltForGeomType) == ogr.wkbPoint) {
            bForceToPoint = true;
        } else if (wkbFlatten(nltForGeomType) == ogr.wkbLineString) {
            bForceToLineString = true;
        } else if (wkbFlatten(nltForGeomType) == ogr.wkbPolygon)
            bForceToPolygon = true;

        Layer destLayer;
        FeatureDefn srcFeatureDefine;

        srcFeatureDefine = srcLayer.GetLayerDefn();
        gdal.PushErrorHandler("CPLQuietErrorHandler");

        destLayer = destSource.GetLayerByName(nlnNameForNewLayer);
        gdal.PopErrorHandler();
        gdal.ErrorReset();

        int iLayer = -1;
        if (destLayer != null) {
            int nLayerCount = destSource.GetLayerCount();
            for (iLayer = 0; iLayer < nLayerCount; iLayer++) {
                Layer poLayer = destSource.GetLayer(iLayer);

                if (poLayer != null
                        && poLayer.GetName().equals(destLayer.GetName())) {
                    break;
                }
            }

            if (iLayer == nLayerCount)
                /* shouldn't happen with an ideal driver */
                destLayer = null;
        }

        /* -------------------------------------------------------------------- */
        /*      If the layer does not exist, then create it.                    */
        /* -------------------------------------------------------------------- */

        if (destLayer == null) {
            if (destSource.TestCapability(ogr.ODsCCreateLayer) == false) {
                System.err.println(
                        "Layer " + nlnNameForNewLayer + "not found, and CreateLayer not supported by driver.");
                return false;
            }
            gdal.ErrorReset();
            destLayer = destSource.CreateLayer(nlnNameForNewLayer, targetSRS,
                    nltForGeomType, lcoOptions);
            if (destLayer == null)
                return false;
            bAppend = false;
        }


        /* Initialize the index-to-index map to -1's --> 将索引到索引映射初始化为 -1*/
        // 获取源图层中记录数
        int srcFieldCount = srcFeatureDefine.GetFieldCount();
        int iField;
        int[] panMap = new int[srcFieldCount];
        for (iField = 0; iField < srcFieldCount; iField++)
            panMap[iField] = -1;

        FeatureDefn destFeatureDefine = destLayer.GetLayerDefn();

        /* For an existing layer, build the map by fetching the index in the destination */
        /* layer for each source field */

        int destFieldCount = 0;
        if (destFeatureDefine != null)
            destFieldCount = destFeatureDefine.GetFieldCount();
        for (iField = 0; iField < srcFieldCount; iField++) {
            FieldDefn srcFieldDefine = srcFeatureDefine.GetFieldDefn(iField);
            FieldDefn destFieldDefine = new FieldDefn(srcFieldDefine.GetNameRef(), srcFieldDefine.GetFieldType());
            destFieldDefine.SetWidth(srcFieldDefine.GetWidth());
            destFieldDefine.SetPrecision(srcFieldDefine.GetPrecision());

            /* The field may have been already created at layer creation */
            int iDstField = -1;
            if (destFeatureDefine != null)
                iDstField = destFeatureDefine.GetFieldIndex(destFieldDefine.GetNameRef());
            if (iDstField >= 0) {
                panMap[iField] = iDstField;
            } else if (destLayer.CreateField(destFieldDefine) == 0) {
                /* now that we've created a field, GetLayerDefn() won't return NULL */
                if (destFeatureDefine == null)
                    destFeatureDefine = destLayer.GetLayerDefn();

                /* Sanity check : if it fails, the driver is buggy */
                if (destFeatureDefine != null &&
                        destFeatureDefine.GetFieldCount() != destFieldCount + 1) {
                    System.err.println(
                            "The output driver has claimed to have added the " + destFieldDefine.GetNameRef() + " field, but it did not!");
                } else {
                    panMap[iField] = destFieldCount;
                    destFieldCount++;
                }
            }
        }

        /* -------------------------------------------------------------------- */
        /*      Transfer features.                                              */
        /* -------------------------------------------------------------------- */
        Feature srcFeature;
        int nFeaturesInTransaction = 0;
        long nCount = 0;
        /* Reset feature reading to start on the first feature. */
        srcLayer.ResetReading();

        /* For datasources which support transactions, StartTransaction creates a transaction. */
        if (nGroupTransactions > 0) {
            destLayer.StartTransaction();
            destLayer.CommitTransaction();
            destLayer.StartTransaction();
        }

        System.out.println("Table is :" + nlnNameForNewLayer + " And the StartIndex is : " + finalFeatureStartIndex);
        srcLayer.SetNextByIndex(finalFeatureStartIndex);
        while (finalFeatureStartIndex++ < finalFeatureEndIndex) {
            Feature destFeature = null;
            if (nFIDToFetch != OGRNullFID) {
                // Only fetch feature on first pass.
                if (nFeaturesInTransaction == 0)
                    srcFeature = srcLayer.GetFeature(nFIDToFetch);
                else
                    srcFeature = null;
            } else
                srcFeature = srcLayer.GetNextFeature();
            if (srcFeature == null) {
                break;
            }
            int nIters = 1;
            for (int iPart = 0; iPart < nIters; iPart++) {

                if (++nFeaturesInTransaction == nGroupTransactions) {
                    destLayer.CommitTransaction();
                    destLayer.StartTransaction();
                    nFeaturesInTransaction = 0;
                }
                gdal.ErrorReset();
                destFeature = new Feature(destLayer.GetLayerDefn());

                /* SetFromWithMap(Feature srcFeature, int forgiving, int[] map): Set one feature from another. */
                if (destFeature.SetFromWithMap(srcFeature, 1, panMap) != 0) {
                    if (nGroupTransactions > 0)
                        destLayer.CommitTransaction();

                    System.err.println(
                            "Unable to translate feature " + srcFeature.GetFID() + " from layer " +
                                    srcFeatureDefine.GetName());

                    srcFeature.delete();
                    srcFeature = null;
                    destFeature.delete();
                    destFeature = null;
                    return false;
                }

                if (bPreserveFID)
                    destFeature.SetFID(srcFeature.GetFID());

                Geometry destGeometry = destFeature.GetGeometryRef();
                if (destGeometry != null) {
                    // dfGeomOpParam 默认 0
                    if (eGeomOp == SimpleDatasourceTransfer.GeomOperation.SEGMENTIZE) {
                    /*if (destFeature.GetGeometryRef() != null && dfGeomOpParam > 0)
                        poDstFeature.GetGeometryRef().segmentize(dfGeomOpParam);*/
                    } else if (eGeomOp == SimpleDatasourceTransfer.GeomOperation.SIMPLIFY_PRESERVE_TOPOLOGY && dfGeomOpParam > 0) {
                        Geometry poNewGeom = destGeometry.SimplifyPreserveTopology(dfGeomOpParam);
                        if (poNewGeom != null) {
                            destFeature.SetGeometryDirectly(poNewGeom);
                            destGeometry = poNewGeom;
                        }
                    }
                    int destGeometryType = destGeometry.GetGeometryType();
                    if (destLayer.GetGeomType() != destGeometryType) {
                        if (bForceToPoint && (destGeometryType == ogr.wkbPoint25D)) {
                            destFeature.SetGeometryDirectly(ogr.ForceTo(destGeometry, ogr.wkbPoint));
                        } else if (bForceToLineString && (destGeometryType == ogr.wkbLineString25D || destGeometryType == ogr.wkbMultiLineString25D)) {
                            destFeature.SetGeometryDirectly(ogr.ForceTo(destGeometry, ogr.wkbLineString));
                        } else if (bForceToPolygon && (destGeometryType == ogr.wkbLineString25D ||
                                destGeometryType == ogr.wkbMultiLineString25D || destGeometryType == ogr.wkbPolygon25D)) {
                            destFeature.SetGeometryDirectly(ogr.ForceToPolygon(destGeometry));
                        }
                    }
                }
                gdal.ErrorReset();
                int createFeatureResult = -1;
                try {
                    createFeatureResult = destLayer.CreateFeature(destFeature);
                } catch (Exception e) {
//                    System.err.println("DestLayer.CreateFeature失败， layer`s GeometryType is [" + destLayer.GetGeomType()
//                            + "] But the feature`s GeometryType is [" + destFeature.GetGeometryRef().GetGeometryType() + "]");
//                    e.printStackTrace();
                }
                if (createFeatureResult != 0 && !bSkipFailures) {
                    if (nGroupTransactions > 0)
                        destLayer.RollbackTransaction();

                    destFeature.delete();
                    destFeature = null;
                    return false;
                }

                destFeature.delete();
                destFeature = null;
            }
            srcFeature.delete();
            srcFeature = null;
        }
        if (nGroupTransactions > 0) {
            System.out.println("Last Commit: " + nlnNameForNewLayer);
            destLayer.CommitTransaction();
        }
        srcLayer.delete();
        destLayer.delete();
        srcSource.delete();
        destSource.delete();
        return true;
    }
}
