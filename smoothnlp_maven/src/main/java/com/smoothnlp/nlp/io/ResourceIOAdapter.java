package com.smoothnlp.nlp.io;

import com.smoothnlp.nlp.SmoothNLP;
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
        InputStream is = ResourceIOAdapter.class.getClassLoader().getResourceAsStream(path);
        if (is == null)
        {
            SmoothNLP.LOGGER.warning("资源文件" + path + "不存在于jar中");
            return new FileIOAdapter().open(path);
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
