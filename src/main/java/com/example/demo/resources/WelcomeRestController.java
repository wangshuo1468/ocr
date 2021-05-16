package com.example.demo.resources;

import com.example.demo.util.Ocr;
import io.swagger.annotations.ApiParam;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;

@Api(value = "Swagger2WelcomeRestController")
@RestController
public class WelcomeRestController {

    @ApiOperation(value = "上传图片", response = String.class, tags = "上传图片")
    @PostMapping("/ocr/upload")
    public String ocr(@RequestParam("file") MultipartFile file, HttpServletResponse response) throws Exception {
        if (file.isEmpty()) {
            return "上传失败，请选择文件";
        }
        FileWriter fileWriter = new FileWriter("test.txt");
        BufferedWriter out = new BufferedWriter(fileWriter);
        out.write(Ocr.doOcr(ImageIO.read(file.getInputStream())));
        out.close();
        File file1 = new File("test.txt");
        logDownload( response, file1);
        return "success";
    }
    public void logDownload( HttpServletResponse response, File file) throws Exception {
        String name = "OcrTxt.txt";
        name = URLEncoder.encode(name, "UTF-8");
        response.setContentType("application/force-download");
        response.addHeader("Content-Disposition", "attachment;fileName=" + name);
        InputStream in = new FileInputStream(file);
        OutputStream out = response.getOutputStream();
        byte[] b = new byte[1024];
        int length = 0;
        while ((length = in.read(b)) != -1) {
            out.write(b, 0, length);
        }
        in.close();
        out.close();

    }

}
