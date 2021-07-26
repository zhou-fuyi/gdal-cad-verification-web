package org.fuyi.gdal.cad.core;

import java.io.*;

public class BashExecutor {

    public final static String DEFAULT_WORKSPACE_PREFIX = "/home/fuyi/workspace/cache/";

    static {
        File file = new File(DEFAULT_WORKSPACE_PREFIX);
        if (!file.exists()){
            boolean flag = file.mkdirs();
            System.out.println("DEFAULT_WORKSPACE_PREFIX 目录已完成创建，状态：" + flag);
        }
    }

    public void execute(String bash){
        try {
            Process process = Runtime.getRuntime().exec(bash);
            int status = process.waitFor();
            if (status != 0){
                throw new RuntimeException("Failed to call shell's command and the return status's is: " + status);
            }
            System.out.println("The bash is executed");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String executeWithResult(String bash){
        BufferedReader bufferedReader = null;
        StringBuilder result = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(bash);
            InputStream inputStream = process.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "GBK");
            bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
                result.append(line);
            }
            int status = process.waitFor();
            if (status != 0){
                throw new RuntimeException("Failed to call shell's command and the return status's is: " + status);
            }
            System.out.println("The bash is executed");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }finally {
            if (bufferedReader != null){
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result.toString();
    }

    public void execute(String bash, String workspace){

    }

    public static boolean MakeDir(String destDir){
        File file = new File(destDir);
        if (!destDir.endsWith(File.separator)) {
            destDir += File.separator;
        }
        if (file.exists()){
            System.out.println("目标路径： [" + destDir + "] 已存在");
            return true;
        }
        return file.mkdirs();
    }

}
