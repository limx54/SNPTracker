/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cobi.snptracker;

/**
 *
 * @author mxli
 */
public interface Constants {

    public static char MISSING_ALLELE_NAME = 'X';
    public static char MISSING_STRAND_NAME = '0';
    public static char DEFAULT_MISSING_GTY_NAME = '0';
    public static final String[] CHROM_NAMES = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14",
        "15", "16", "17", "18", "19", "20", "21", "22", "X", "Y", "XY", "MT"
    };
    static int FILE_SEGEMENT_NUM = 5;
    static int MAX_THREAD_NUM = 1;
    static String URL_FOLDER = "http://statgenpro.psychiatry.hku.hk/limx/snptracker/download/";
    static String[] RESOURCE_FILE_PATHES = {"b142_GRCh19.RsMergeArch.bcp.gz", "b142_GRCh38.RsMergeArch.bcp.gz","b142_SNPChrPosOnRef_GRCh19p105.bcp.gz","b142_SNPChrPosOnRef_GRCh38p106.bcp.gz","b142_GRCh19.SNPHistory.bcp.gz","b142_GRCh38.SNPHistory.bcp.gz","hg17ToHg19.over.chain.gz","hg18ToHg19.over.chain.gz"};
    static String[] LIB_FILE_PATHES = {"snptracker.jar"};
}
