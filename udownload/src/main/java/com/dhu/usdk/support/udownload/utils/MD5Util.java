package com.dhu.usdk.support.udownload.utils;


import java.io.File;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;

public class MD5Util {

    /**
     * 获取单个文件的MD5值！
     *
     * @param file
     * @return 解决首位0被省略问题
     * 解决超大文件问题
     */

    public static String getFileMD5(File file) {

        StringBuffer stringbuffer;
        try {
            char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
            FileInputStream in = new FileInputStream(file);
            FileChannel ch = in.getChannel();

            long fileSize = ch.size();
            int bufferCount = (int) Math.ceil((double) fileSize / (double) Integer.MAX_VALUE);
            MappedByteBuffer[] mappedByteBuffers = new MappedByteBuffer[bufferCount];

            long preLength = 0;
            long regionSize = Integer.MAX_VALUE;
            for (int i = 0; i < bufferCount; i++) {
                if (fileSize - preLength < Integer.MAX_VALUE) {
                    regionSize = fileSize - preLength;
                }
                mappedByteBuffers[i] = ch.map(FileChannel.MapMode.READ_ONLY, preLength, regionSize);
                preLength += regionSize;
            }

            MessageDigest messagedigest = MessageDigest.getInstance("MD5");

            for (int i = 0; i < bufferCount; i++) {
                messagedigest.update(mappedByteBuffers[i]);
            }
            byte[] bytes = messagedigest.digest();
            int n = bytes.length;
            stringbuffer = new StringBuffer(2 * n);
            for (int l = 0; l < n; l++) {
                byte bt = bytes[l];
                char c0 = hexDigits[(bt & 0xf0) >> 4];
                char c1 = hexDigits[bt & 0xf];
                stringbuffer.append(c0);
                stringbuffer.append(c1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return stringbuffer.toString();

    }

}
