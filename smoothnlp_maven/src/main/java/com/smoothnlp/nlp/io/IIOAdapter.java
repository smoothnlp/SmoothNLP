package com.smoothnlp.nlp.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * IO适配器接口<br>
 * 实现该接口以移植HanLP到不同的平台
 *
 * @author hankcs
 */
public interface IIOAdapter
{
    /**
     * 打开一个文件以供读取
     * @param path 文件路径
     * @return 一个输入流
     * @throws IOException 任何可能的IO异常
     */
    InputStream open(String path) throws IOException;

    /**
     * 创建一个新文件以供输出
     * @param path 文件路径
     * @return 一个输出流
     * @throws IOException 任何可能的IO异常
     */
    OutputStream create(String path) throws IOException;
}
