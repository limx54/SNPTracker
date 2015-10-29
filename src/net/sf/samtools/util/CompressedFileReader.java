/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.samtools.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import java.util.zip.ZipFile;
 

/**
 *
 * @author mxli
 */
public class CompressedFileReader implements LineReader {

    BufferedReader reader;
    private int lineNumber = 0;
    private String peekedLine;
    private ZipFile zip;

    public static void main(String[] args) throws Exception {
        String src = "c:/tmp/test.txt";//指定压缩源，可以是目录或文件
        String decompressDir = "c:/tmp/decompress";//解压路径
        //String archive = "c:/tmp/test.zip";//压缩包路径
        String archive = "c:/tmp/genotype_freqs_chr22_CHB_r24_nr.b36.txt.gz";
        String comment = "Java Zip 测试.";//压缩包注释
        File file = new File(archive);



        LineReader lr = new CompressedFileReader(file);
        String line = null;
        while ((line = lr.readLine()) != null) {
            System.out.println(line);
        }
        lr.close();

    }

    public CompressedFileReader(File file) throws Exception {
      /* if (file.getName().endsWith(".zip")) {
            zip = new ZipFile(file);
            Enumeration e = zip.getEntries(); // java包是zip.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
                //note only read the first file
                break;
            }
        } else if (file.getName().endsWith(".tar.gz")) {
            throw new Exception("Error! Cannot pass a tar.gz file!");
        } else */
            if (file.getName().endsWith(".gz")) {
            reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
        } else {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        }
    }

    /**
     * Read a line and remove the line terminator
     *
     * @return the line read, or null if EOF has been reached.
     */
    public String readLine() {
        ++lineNumber;
        String ret = null;
        try {
            if (peekedLine != null) {
                ret = peekedLine;
                peekedLine = null;
            } else {

                ret = reader.readLine();
            }
            return ret;
        } catch (IOException e) {
            // throw new RuntimeIOException(e);
            return ret;
        }
    }

    /**
     * @return 1-based number of line most recently read
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Non-destructive one-character look-ahead.
     *
     * @return If not eof, the next character that would be read.  If eof, -1.
     */
    public int peek() {
        if (peekedLine == null) {
            try {
                peekedLine = reader.readLine();
            } catch (IOException e) {
                //throw new RuntimeIOException(e);
            }
        }
        if (peekedLine == null) {
            return -1;
        }
        if (peekedLine.length() == 0) {
            return '\n';
        }
        return peekedLine.charAt(0);
    }

    public void close() {
        peekedLine = null;
        try {
            if (zip != null) {
                zip.close();
            }
            if (reader != null) {
                reader.close();
            } 

        } catch (IOException e) {
            // throw new RuntimeIOException(e);
        }
    }
}
