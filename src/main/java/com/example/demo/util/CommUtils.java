package com.example.demo.util;

import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.StaticLog;
import com.example.demo.pojo.TextBlock;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommUtils {



    private static final float IMAGE_QUALITY = 0.5f;
    private static final int SAME_LINE_LIMIT = 8;
    private static final int CHAR_WIDTH = 12;
    private static Pattern NORMAL_CHAR = Pattern.compile("[\\u4e00-\\u9fa5\\w、-，/|_]");




    public static byte[] imageToBytes(BufferedImage img) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream outputStream = new MemoryCacheImageOutputStream(byteArrayOutputStream);
        try {
            Iterator iter = ImageIO.getImageWritersByFormatName("jpeg");
            ImageWriter writer = (ImageWriter) iter.next();
            ImageWriteParam iwp = writer.getDefaultWriteParam();
            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

            iwp.setCompressionQuality(IMAGE_QUALITY);
            writer.setOutput(outputStream);
            IIOImage image = new IIOImage(img, null, null);
            writer.write(null, image, iwp);
            writer.dispose();
            byte[] result = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            outputStream.close();
            return result;
        } catch (IOException e) {
            e.getStackTrace();
            StaticLog.error(e);
            return new byte[0];
        }
    }

    static byte[] mergeByte(byte[]... bytes) {
        int length = 0;
        for (byte[] b : bytes) {
            length += b.length;
        }
        byte[] resultBytes = new byte[length];
        int offset = 0;
        for (byte[] arr : bytes) {
            System.arraycopy(arr, 0, resultBytes, offset, arr.length);
            offset += arr.length;
        }
        return resultBytes;
    }

    static String postMultiData(String url, byte[] data, String boundary) {
        return postMultiData(url, data, boundary, "", "");
    }

    private static String postMultiData(String url, byte[] data, String boundary, String cookie, String referer) {
        try {
            HttpRequest request = HttpUtil.createPost(url).timeout(15000);
            request.contentType("multipart/form-data; boundary=" + boundary);
            request.body(data);
            if (StrUtil.isNotBlank(referer)) {
                request.header("Referer", referer);
            }
            if (StrUtil.isNotBlank(cookie)) {
                request.cookie(cookie);
            }
            HttpResponse response = request.execute();
            return WebUtils.getSafeHtml(response);
        } catch (Exception ex) {
            StaticLog.error(ex);
            return null;
        }
    }

    static Point frameToPoint(String text) {
        String[] arr = text.split(",");
        return new Point(Integer.valueOf(arr[0].trim()), Integer.valueOf(arr[1].trim()));
    }

    static String combineTextBlocks(List<TextBlock> textBlocks, boolean isEng) {
        textBlocks.sort(Comparator.comparingInt(o -> o.getTopLeft().y));
        List<List<TextBlock>> lineBlocks = new ArrayList<>();
        int lastY = -1;
        List<TextBlock> lineBlock = new ArrayList<>();
        boolean sameLine = true;
        int minX = Integer.MAX_VALUE;
        TextBlock minBlock = null;
        TextBlock maxBlock = null;
        int maxX = -1;
        double maxAngle = -100;
        for (TextBlock textBlock : textBlocks) {
            //System.out.println(textBlock.getAngle()+ "\t" + textBlock.getFontSize());
            if (textBlock.getTopLeft().x < minX) {
                minX = textBlock.getTopLeft().x;
                minBlock = textBlock;
            }
            if (textBlock.getTopRight().x > maxX) {
                maxX = textBlock.getTopRight().x;
                maxBlock = textBlock;
            }
            if (Math.abs(textBlock.getAngle()) > maxAngle){
                maxAngle = Math.abs(textBlock.getAngle());
            }
            if (lastY == -1) {
                lastY = textBlock.getTopLeft().y;
            } else {
                sameLine = textBlock.getTopLeft().y - lastY <= SAME_LINE_LIMIT;
            }
            if (!sameLine) {
                lineBlock.sort(Comparator.comparingInt(o -> o.getTopLeft().x));
                lineBlocks.add(lineBlock);
                lineBlock = new ArrayList<>();
                sameLine = true;
                lastY = textBlock.getTopLeft().y;
            }
            lineBlock.add(textBlock);
        }

        if (maxAngle >= 0.05){
            //todo 文本倾斜校正
        }

        if (lineBlock.size() > 0) {
            lineBlock.sort(Comparator.comparingInt(o -> o.getTopLeft().x));
            lineBlocks.add(lineBlock);
        }
        StringBuilder sb = new StringBuilder();
        TextBlock lastBlock = null;
        for (List<TextBlock> line : lineBlocks) {
            TextBlock firstBlock = line.get(0);
            if (lastBlock != null) {
                String blockTxt = lastBlock.getText().trim();
                String endTxt = blockTxt.substring(blockTxt.length() - 1);
                if (maxX - lastBlock.getTopRight().x >= CHAR_WIDTH * 2 ||
                        !NORMAL_CHAR.matcher(endTxt).find() ||
                        (NORMAL_CHAR.matcher(endTxt).find() &&
                                (firstBlock.getTopLeft().x - minX) > CHAR_WIDTH * 2)){
                    sb.append("\n");
                    for (int i = 0, ln = (firstBlock.getTopLeft().x - minX) / CHAR_WIDTH; i < ln; i++) {
                        if (i % 2 == 0){
                            sb.append("    ");
                        }
                    }
                }
                else{
                    if (CharUtil.isLetterOrNumber(endTxt.charAt(0)) && CharUtil.isLetterOrNumber(firstBlock.getText().charAt(0))){
                        sb.append(" ");
                    }
                }
            }
            else{
                for (int i = 0, ln = (firstBlock.getTopLeft().x - minX) / CHAR_WIDTH; i < ln; i++) {
                    if (i % 2 == 0){
                        sb.append("    ");
                    }
                }
            }

            for (int i = 0; i < line.size(); i++) {
                TextBlock text = line.get(i);
                String ocrText = text.getText();
                if (i > 0) {
                    for (int a = 0, ln = (text.getTopLeft().x - line.get(i - 1).getTopRight().x) / (CHAR_WIDTH * 2);
                         a < ln; a++) {
                        sb.append("  ");
                    }
                }
                sb.append(ocrText);
            }
            lastBlock = line.get(line.size() - 1);
        }
        return sb.toString();
    }
}
