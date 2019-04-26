package com.smoothnlp.nlp.io;

import com.typesafe.config.ConfigException;

import java.io.*;

/**
 * 从jar包资源读取文件的适配器
 * @author hankcs
 */
public class ResourceIOAdapter implements IIOAdapter
{

    public InputStream open(String path) throws IOException
    {
//        InputStream is = this.getClass().getClassLoader().getResourceAsStream()
        InputStream is = ResourceIOAdapter.class.getClassLoader().getResourceAsStream(path);
        if (is == null)
        {
            throw new FileNotFoundException("资源文件" + path + "不存在于jar中");
        }

        return is;
    }


    public OutputStream create(String path) throws IOException
    {
        return new FileOutputStream(path);
    }

//    public static void main(String[] args) throws IOException {
//        ResourceIOAdapter r = new ResourceIOAdapter();
//        r.open("segment_crfpp.bin");
//    }

}
