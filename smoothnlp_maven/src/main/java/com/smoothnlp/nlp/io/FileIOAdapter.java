package com.smoothnlp.nlp.io;

import java.io.*;

/**
 * 基于普通文件系统的IO适配器
 *
 * @author hankcs
 */
public class FileIOAdapter implements IIOAdapter
{
    public InputStream open(String path) throws FileNotFoundException
    {
        return new FileInputStream(path);
    }

    public OutputStream create(String path) throws FileNotFoundException
    {
        return new FileOutputStream(path);
    }
}
