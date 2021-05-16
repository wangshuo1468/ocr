package com.example.demo.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Ocr {

    public static String doOcr(BufferedImage image) {
        byte[] bytes1 = CommUtils.imageToBytes(image);
        return  OcrUtils.ocrImg(bytes1);
    }

    public static void main(String[] args) throws IOException {
        BufferedImage image = ImageIO.read(new FileInputStream("C:/Users/WangShuo/Pictures/1.jpg"));
        doOcr(image);
    }
}

