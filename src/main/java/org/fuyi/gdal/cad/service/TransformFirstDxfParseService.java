package org.fuyi.gdal.cad.service;

import org.fuyi.gdal.cad.core.DatasourceTransfer;
import org.fuyi.gdal.cad.core.ExecutorTemplate;
import org.fuyi.gdal.cad.core.SimpleDatasourceTransfer;
import org.fuyi.gdal.cad.entity.DxfGeometry;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

@Service
public class TransformFirstDxfParseService implements DxfParseService {

    @Autowired
    @Qualifier("shardingTransfer")
//    @Qualifier("simpleTransfer")
    private DatasourceTransfer datasourceTransfer;

    @Override
    public Collection<DxfGeometry> parse(File dxfFile) throws IllegalAccessException, InterruptedException {
        // 注册驱动
        ogr.RegisterAll();
        // 支持中文路径
        gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
        // 属性表字段支持中文
//        gdal.SetConfigOption("SHAPE_ENCODING", "UTF-8");
        gdal.SetConfigOption("SHAPE_ENCODING", "GBK");
        gdal.SetConfigOption("DXF_FEATURE_LIMIT_PER_BLOCK", "-1");
        gdal.SetConfigOption("DXF_ENCODING", "ASCII");

        long startTime = System.currentTimeMillis();
        DataSource srcSource = ogr.Open(dxfFile.getAbsolutePath(), false);
        if (srcSource == null) {
            throw new RuntimeException("[" + dxfFile.getName() + "] 文件无法解析");
        }
        String dbPath = "Pg:dbname=gdal-cad-fetch host=127.0.0.1 user=postgres password=root port=5432";
        DataSource destSource = ogr.Open(dbPath, true);
        if (destSource == null) {
            throw new RuntimeException("[" + dbPath + "] 无法获取链接");
        }
        destSource.delete();
        Layer sourceLayer = srcSource.GetLayer(0);
        if (sourceLayer == null) {
            throw new RuntimeException("FAILURE: Couldn't fetch layer " + sourceLayer + "!");
        }
        Vector lcoOptions = new Vector();
        lcoOptions.add("GEOMETRY_NAME=geom");
        lcoOptions.add("FID=gid");
        lcoOptions.add("PRECISION=NO");
        lcoOptions.add("OVERWRITE=YES");
//        lcoOptions.add("SPATIAL_INDEX=YES");
        String fileName = Base64.getEncoder().encodeToString(dxfFile.getName().getBytes(StandardCharsets.UTF_8)) + "_";
//        String[] nlnLayerNames = new String[]{fileName + "point", fileName + "line", fileName + "polygon", fileName + "multi_line"};
//        int[] nltGeomTypes = new int[]{ogr.wkbPoint25D, ogr.wkbLineString25D, ogr.wkbPolygon25D, ogr.wkbMultiLineString25D};
//        int[] nltGeomTypes = new int[]{ogr.wkbPoint, ogr.wkbLineString, ogr.wkbPolygon, ogr.wkbMultiLineString};
        String[] nlnLayerNames = new String[]{fileName + "point_z", fileName + "line", fileName + "line_z", fileName + "multi_line_z"};
        int[] nltGeomTypes = new int[]{ogr.wkbPoint25D, ogr.wkbLineString, ogr.wkbLineString25D, ogr.wkbMultiLineString25D};
        boolean bSkipFailures = true;
        long zeroLayerFeatureCount = sourceLayer.GetFeatureCount();
        CountDownLatch countDownLatch = new CountDownLatch(nltGeomTypes.length);
        for (int index = 0; index < nltGeomTypes.length; index++) {
            try{
                int finalIndex = index;
                ExecutorTemplate.executor.execute(() -> {
                    if (!datasourceTransfer.translateLayer(dxfFile.getAbsolutePath(),
                            dbPath,
                            lcoOptions,
                            nlnLayerNames[finalIndex],
                            false,
                            null,
                            null,
                            null,
                            false,
                            nltGeomTypes[finalIndex],
                            true,
                            SimpleDatasourceTransfer.GeomOperation.NONE,
                            0l,
                            null,
                            zeroLayerFeatureCount,
                            null,
                            null,
                            false,
                            null,
                            null,
                            null)) {
                        System.err.println("图层[" + nlnLayerNames[finalIndex] + "]提取失败");
                    }
                    countDownLatch.countDown();
                    System.out.println("外层图层分层作业: 线程" + Thread.currentThread().getName() + "已完成作业");
                });
//                if (!datasourceTransfer.translateLayer(dxfFile.getAbsolutePath(),
//                        dbPath,
//                        lcoOptions,
//                        nlnLayerNames[finalIndex],
//                        false,
//                        null,
//                        null,
//                        null,
//                        false,
//                        nltGeomTypes[finalIndex],
//                        true,
//                        SimpleDatasourceTransfer.GeomOperation.NONE,
//                        0l,
//                        null,
//                        zeroLayerFeatureCount,
//                        null,
//                        null,
//                        false,
//                        null,
//                        null,
//                        null)) {
//                    System.err.println("图层[" + nlnLayerNames[finalIndex] + "]提取失败");
//                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        countDownLatch.await();
        long endTime = System.currentTimeMillis();
        System.out.println("执行结果，共计耗时约：" + ((endTime - startTime) / 1000) + "秒");
        srcSource.delete();
        destSource.delete();
        return Collections.EMPTY_LIST;
    }
}
