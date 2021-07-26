package org.fuyi.gdal.cad.web;

import org.fuyi.gdal.cad.core.BashExecutor;
import org.fuyi.gdal.cad.entity.DxfGeometry;
import org.fuyi.gdal.cad.service.DxfParseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/dxf/parse")
public class DxfParseController {

    public static Map<String, String> DWG_VERSION = new HashMap<>();

    static {
        DWG_VERSION.put("AC1009", "r12");
        DWG_VERSION.put("AC1014", "r14");
        DWG_VERSION.put("AC1015", "r2000");
        DWG_VERSION.put("AC1018", "r2004");
        DWG_VERSION.put("AC1021", "r2007");
        DWG_VERSION.put("AC1024", "r2010");
        DWG_VERSION.put("AC1027", "r2013");
    }

    @Autowired
    private DxfParseService dxfParseService;


    private BashExecutor bashExecutor = new BashExecutor();

    @GetMapping("/{path}")
    public Collection<DxfGeometry> parseDxfFile(@PathVariable String path) throws IllegalAccessException, InterruptedException {
        long startTime = System.currentTimeMillis();
        String pathPrefix = "/home/fuyi/work/dxf/";
        path = pathPrefix + path;
        Collection result = dxfParseService.parse(dwg2dxf(new File(path)));
        long endTime = System.currentTimeMillis();
        System.out.println("Total --> 执行结果，共计耗时约：" + ((endTime - startTime) / 1000) + "秒");
        return result;
    }

    @GetMapping("/dwg/{path}")
    public Collection<DxfGeometry> parseDwgFile(@PathVariable String path) throws IllegalAccessException, InterruptedException {
        String pathPrefix = "/home/fuyi/work/origin/";
        long startTime = System.currentTimeMillis();
        path = pathPrefix + path;
        Collection result = dxfParseService.parse(dwg2dxf(new File(path)));
        long endTime = System.currentTimeMillis();
        System.out.println("Total --> 执行结果，共计耗时约：" + ((endTime - startTime) / 1000) + "秒");
        return result;
    }

    private File dwg2dxf(File targetFile) {
        if (targetFile.exists() && targetFile.getAbsolutePath().endsWith("dwg")) {
            String dxfFileParentPath = BashExecutor.DEFAULT_WORKSPACE_PREFIX + "current_user_code/dxf/";
//            String reWriteDwgFileParentPath = BashExecutor.DEFAULT_WORKSPACE_PREFIX + "current_user_code/dwg/";

            if (BashExecutor.MakeDir(dxfFileParentPath)) {
//                String dwgFilePath = reWriteDwgFileParentPath + targetFile.getName().substring(0, targetFile.getName().length() - 4) + "-r2000.dwg";

//                String dwgRewriteBash = "dwgrewrite --as r2000 -o " + dwgFilePath + " " + targetFile.getAbsolutePath();
//                System.out.println(dwgRewriteBash);
//                bashExecutor.execute(dwgRewriteBash);

                String fetChVersionBash = "head -c 6 " + targetFile.getAbsolutePath();
                System.out.println(fetChVersionBash);
                String dwgVersion = bashExecutor.executeWithResult(fetChVersionBash);
                System.out.println("dwg version is " + dwgVersion);

                String dxfFilePath = dxfFileParentPath + targetFile.getName().substring(0, targetFile.getName().length() - 4) + "-r2004.dxf";
                String bash = "dwg2dxf --as " + getDwgVersionForLibreDWG(dwgVersion) + " -y -o " + dxfFilePath + " " + targetFile.getAbsolutePath();
                System.out.println(bash);
                bashExecutor.execute(bash);
                File dxfFile = new File(dxfFilePath);
                return dxfFile;
            }
        }
        return targetFile;
    }

    private String getDwgVersionForLibreDWG(String originVersion){
        String version = DWG_VERSION.get(originVersion);
        if (StringUtils.hasText(version)){
            return version;
        }else {
            throw new RuntimeException("系统在不支持DWG版本: " + originVersion);
        }
    }
}
