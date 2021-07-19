package org.fuyi.gdal.cad.service;

import org.fuyi.gdal.cad.entity.DxfGeometry;

import java.io.File;
import java.util.Collection;

public interface DxfParseService {

    Collection<DxfGeometry> parse(File dxfFile) throws IllegalAccessException, InterruptedException;

}
