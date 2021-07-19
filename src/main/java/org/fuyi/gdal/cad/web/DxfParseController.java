package org.fuyi.gdal.cad.web;

import org.fuyi.gdal.cad.entity.DxfGeometry;
import org.fuyi.gdal.cad.service.DxfParseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Collection;

@RestController
@RequestMapping("/dxf/parse")
public class DxfParseController {

    @Autowired
    private DxfParseService dxfParseService;
    private final String pathPrefix = "/home/fuyi/work/dxf/";

    @GetMapping("/{path}")
    public Collection<DxfGeometry> parseDxfFile(@PathVariable String path) throws IllegalAccessException, InterruptedException {
        path = pathPrefix + path;
        return dxfParseService.parse(new File(path));
    }
}
