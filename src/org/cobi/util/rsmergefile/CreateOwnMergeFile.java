package org.cobi.util.rsmergefile;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;
import net.sf.samtools.util.CompressedFileReader;
import net.sf.samtools.util.LineReader;
import org.cobi.util.text.Util;
import cern.colt.map.OpenIntIntHashMap;

/**
 *
 * @author jdeng
 */
public class CreateOwnMergeFile {

    public OpenIntIntHashMap readMergeBackground(String mF) throws IOException, Exception {
        String tmLine;
        OpenIntIntHashMap rawMergeHash = new OpenIntIntHashMap();
        StringBuilder sb = new StringBuilder(); // improve effection to create a string. StringBuilder is more safety than StringBuffer
        LineReader bf = new CompressedFileReader(new File(mF));
        while ((tmLine = bf.readLine()) != null) {
            String[] array = Util.tokenize(tmLine, '\t', 6);
            String highId = array[0];
            String lowId = array[1];
            String rsCurrent = array[6];
            if (rsCurrent.equals("")) {
                rsCurrent = lowId;
            }
            rawMergeHash.put(Integer.parseInt(highId), Integer.parseInt(rsCurrent));
        }
        return rawMergeHash;
        
    }
    
    public void createOwnMergeFile(OpenIntIntHashMap rM, String mF) throws Exception{
        String tmLine;
        StringBuilder sb = new StringBuilder(); // improve effection to create a string. StringBuilder is more safety than StringBuffer
        LineReader bf = new CompressedFileReader(new File(mF));
        
        BufferedWriter bw;
        String newMergeFileName = mF.replace("gz", "snptracker.gz");
        bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(newMergeFileName))));
        bw.write("oldId\tCurrentId\n");;
        while ((tmLine = bf.readLine()) != null) {
            
            String[] array = Util.tokenize(tmLine, '\t', 1);
            String highId = array[0];
            int rs = Integer.parseInt(highId);
            boolean tag=true;
            while(tag){
                if(rM.containsKey(rs)){
                    rs = rM.get(rs);
                }else if(!rM.containsKey(rs)){
                    bw.write(highId+"\t"+rs+"\n");
                    break;
                }
            }
        }
        bw.flush();
        bw.close();
    }
    
    public static void main(String[] args) throws Exception {
        CreateOwnMergeFile c = new CreateOwnMergeFile();
        OpenIntIntHashMap rM = c.readMergeBackground("H:\\JavaNetBean\\snptracker\\resources\\b142_GRCh38.RsMergeArch.bcp.gz");
        c.createOwnMergeFile(rM,"H:\\JavaNetBean\\snptracker\\resources\\b142_GRCh38.RsMergeArch.bcp.gz");
    }
}
