package org.fuyi.gdal.cad.service;

import org.fuyi.gdal.cad.entity.DxfGeometry;
import org.gdal.gdal.gdal;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Feature;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;

@Service
public class SimpleDxfParseService implements DxfParseService{

    private final int DEFAULT_CORE_SIZE = 10;
    private final int DEFAULT_MAX_SIZE = 100;

//    private Executor executor = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);
    private Executor executor = new ThreadPoolExecutor(DEFAULT_CORE_SIZE, DEFAULT_MAX_SIZE,
        6000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    @Override
    public Collection<DxfGeometry> parse(File dxfFile) throws IllegalAccessException, InterruptedException {
        // 注册驱动
        ogr.RegisterAll();
        // 支持中文路径
        gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8","YES");
        // 属性表字段支持中文
        gdal.SetConfigOption("SHAPE_ENCODING","CP936");
        gdal.SetConfigOption("DXF_FEATURE_LIMIT_PER_BLOCK", "-1");

        DataSource dataSource = ogr.Open(dxfFile.getAbsolutePath(), 0);
        if (dataSource == null) {
            throw new RuntimeException("[" + dxfFile.getName() + "] 文件无法解析");
        }
        Layer layer = dataSource.GetLayerByIndex(0);
        System.out.println("LayerName: " + layer.GetName());
        // Feature Count
        long featureCount = layer.GetFeatureCount();
        int index = 0;
        Collection<DxfGeometry> dxfGeometries = new ArrayList<>(Long.valueOf(featureCount).intValue());

        CountDownLatch countDownLatch = new CountDownLatch(DEFAULT_CORE_SIZE);

        int tileSize = Long.valueOf(featureCount).intValue() / DEFAULT_CORE_SIZE;
        // 余数
        int remainder = Long.valueOf(featureCount).intValue() % DEFAULT_CORE_SIZE;

        long startTime = System.currentTimeMillis();
        while (index < DEFAULT_CORE_SIZE){
            int featureEndIndex = (index + 1) * tileSize;
            if (index == (DEFAULT_CORE_SIZE -1)){
                featureEndIndex += remainder;
            }
            int finalIndex = index;
            int finalFeatureEndIndex = featureEndIndex;
            executor.execute(() -> {
                long finalStartTime = System.currentTimeMillis();
                int featureStartIndex = finalIndex * tileSize;
                System.out.println("当前线程：" + Thread.currentThread().getName() + "\t读取区间：[" + featureStartIndex + ", " + finalFeatureEndIndex + "]");
                DataSource finalDatasource = ogr.Open(dxfFile.getAbsolutePath(), 0);
                Layer finalLayer = finalDatasource.GetLayerByIndex(0);
                while (featureStartIndex < finalFeatureEndIndex){
                    Feature feature = finalLayer.GetFeature(featureStartIndex);
                    try {
                        dxfGeometries.add(featureParse(feature));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    featureStartIndex ++;
                }
                countDownLatch.countDown();
                long finalEndTime = System.currentTimeMillis();
                System.out.println("当前线程：" + Thread.currentThread().getName() + "执行结束，累计耗时约：" + ((finalEndTime - finalStartTime) / 60000) + "秒");
            });
            index ++;
        }
        countDownLatch.await();
        dataSource.delete();
        long endTime = System.currentTimeMillis();
        System.out.println("执行结果，共计耗时约：" + ((endTime - startTime) / 60000) + "秒");
        return dxfGeometries;
    }

    private DxfGeometry featureParse(Feature feature) throws IllegalAccessException {
        DxfGeometry dxfGeometry = new DxfGeometry();
        if (null ==  feature){
            return dxfGeometry;
        }
        dxfGeometry.setGeometry(feature.GetGeometryRef().ExportToIsoWkb());
        // Feature中存储的Field个数
        int fieldCount = feature.GetFieldCount();
        // 用于遍历Feature中存储的Field的索引
        int fieldCountIndex = 0;
        // 用于存储Feature中存储的Field的Name与Value的索引
        int fieldsIndex = 0;
        String[] fields = new String[2 * fieldCount];
        while (fieldCountIndex < fieldCount){
            fields[fieldsIndex++] = feature.GetFieldDefnRef(fieldCountIndex).GetName();
            fields[fieldsIndex++] = feature.GetFieldAsString(fieldCountIndex);
            fieldCountIndex ++;
        }
        dxfGeometry.setFields(fields);
        System.out.println("当前线程：" + Thread.currentThread().getName() + "执行作业，dxfGeometry：" + dxfGeometry);
        return dxfGeometry;
    }
}
