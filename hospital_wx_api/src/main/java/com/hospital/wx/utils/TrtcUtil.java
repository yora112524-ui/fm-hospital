package com.hospital.wx.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.Deflater;

@Component
public class TrtcUtil {

    // 从配置文件中获取腾讯TRTC的应用ID，存储为整数类型的appId
    @Value("${tencent.trtc.appId}")
    private int appId;

    // 从配置文件中获取签名的有效期（以秒为单位），存储为整数类型的expire
    @Value("${tencent.trtc.expire}")
    private int expire;

    // 从配置文件中获取腾讯TRTC的私钥，用于生成签名，存储为字符串类型的secretKey
    @Value("${tencent.trtc.secretKey}")
    private String secretKey;

    // 生成用户签名方法，接收用户ID作为参数，调用内部的genTLSSignature方法生成签名
    public String generateUserSignature(String userId) {
        // 调用GenTLSSignature方法，传递appId、userId、expire、空的userbuf和secretKey作为参数
        return genTLSSignature(appId, userId, expire, null, secretKey);
    }

    // 生成TLS签名方法，接收应用ID(sdkappid)、用户ID(userId)、有效期(expire)、用户自定义数据(userbuf)及私钥内容(priKeyContent)作为参数
    private String genTLSSignature(long sdkappid, String userId, long expire, byte[] userbuf, String priKeyContent) {
        // 如果私钥内容为空，返回空字符串
        if (StrUtil.isEmpty(priKeyContent)) {
            return "";
        }

        // 获取当前时间戳（秒级）
        long currTime = System.currentTimeMillis() / 1000;

        // 创建JSON对象，用于存储TLS签名信息
        JSONObject sigDoc = new JSONObject();
        sigDoc.put("TLS.ver", "2.0"); // 设置TLS签名版本
        sigDoc.put("TLS.identifier", userId); // 存储用户ID
        sigDoc.put("TLS.sdkappid", sdkappid); // 存储应用ID
        sigDoc.put("TLS.expire", expire); // 存储签名的有效期
        sigDoc.put("TLS.time", currTime); // 存储当前时间戳

        String base64UserBuf = null; // 用户自定义数据的Base64编码形式
        // 如果传入的用户自定义数据不为空，则进行Base64编码并存储到JSON对象中
        if (userbuf != null) {
            base64UserBuf = Base64.encode(userbuf); // 使用Hutool库进行Base64编码
            sigDoc.put("TLS.userbuf", base64UserBuf); // 将编码后的数据存入签名文档
        }

        // 生成HMAC-SHA256签名，传递应用ID、用户ID、当前时间戳、有效期、私钥内容和用户自定义数据
        String sig = hmacSha256(sdkappid, userId, currTime, expire, priKeyContent, base64UserBuf);
        // 如果签名为空，返回空字符串
        if (sig.isEmpty()) {
            return "";
        }

        // 将签名存储到JSON对象中
        sigDoc.put("TLS.sig", sig);

        // 使用Deflater进行签名文档的压缩处理
        try {
            Deflater compressor = new Deflater(); // 创建压缩对象
            byte[] input = sigDoc.toString().getBytes(StandardCharsets.UTF_8); // 将签名文档转换为字节数组
            compressor.setInput(input); // 设置压缩输入数据
            compressor.finish(); // 完成压缩

            byte[] compressedBytes = new byte[2048]; // 创建字节数组存储压缩数据
            int compressedBytesLength = compressor.deflate(compressedBytes); // 获取压缩后的数据长度
            compressor.end(); // 结束压缩

            // 使用Base64编码并进行URL安全处理，返回最终的签名字符串
            return new String(base64EncodeUrl(Arrays.copyOfRange(compressedBytes, 0, compressedBytesLength)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 捕获异常并输出异常信息
            e.printStackTrace();
            return "";
        }
    }

    // HMAC-SHA256签名生成方法，接收应用ID、用户ID、时间戳、有效期、私钥和用户自定义数据作为参数
    private static String hmacSha256(long sdkappid, String userId, long currTime, long expire, String priKeyContent, String base64Userbuf) {
        // 准备待签名的字符串内容，包含用户ID、应用ID、时间戳和有效期等信息
        String contentToBeSigned = "TLS.identifier:" + userId + "\n" +
                "TLS.sdkappid:" + sdkappid + "\n" +
                "TLS.time:" + currTime + "\n" +
                "TLS.expire:" + expire + "\n";

        // 如果用户自定义数据不为空，则将其加入待签名内容中
        if (base64Userbuf != null) {
            contentToBeSigned += "TLS.userbuf:" + base64Userbuf + "\n";
        }

        try {
            // 将私钥内容转换为字节数组
            byte[] byteKey = priKeyContent.getBytes(StandardCharsets.UTF_8);
            // 获取HMAC-SHA256实例
            Mac hmac = Mac.getInstance("HmacSHA256");
            // 使用私钥进行初始化
            SecretKeySpec keySpec = new SecretKeySpec(byteKey, "HmacSHA256");
            hmac.init(keySpec);
            // 对待签名的内容进行HMAC计算，并返回Base64编码的结果
            byte[] byteSig = hmac.doFinal(contentToBeSigned.getBytes(StandardCharsets.UTF_8));
            return Base64.encode(byteSig); // 使用Hutool库进行Base64编码
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // 捕获异常并输出异常信息
            e.printStackTrace();
            return "";
        }
    }

    // Base64编码并将结果转换为URL安全的格式
    private static byte[] base64EncodeUrl(byte[] input) {
        // 进行Base64编码，并转换为字节数组
        byte[] base64 = Base64.encode(input).getBytes(StandardCharsets.UTF_8);
        // 遍历字节数组，替换不安全的字符为URL安全的字符
        for (int i = 0; i < base64.length; i++) {
            switch (base64[i]) {
                case '+':
                    base64[i] = '*'; // 将'+'替换为'*'
                    break;
                case '/':
                    base64[i] = '-'; // 将'/'替换为'-'
                    break;
                case '=':
                    base64[i] = '_'; // 将'='替换为'_'
                    break;
                default:
                    break;
            }
        }
        return base64; // 返回URL安全的Base64编码结果
    }
}