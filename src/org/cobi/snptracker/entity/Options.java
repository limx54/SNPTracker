/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cobi.snptracker.entity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.cobi.snptracker.Constants;

/**
 *
 * @author mxli
 */
public class Options implements Constants {

    String[] options;
    public int optionNum = 0;
    public String inputFileName = null;
    public String resultFileName = null;
    public String errorFileName = null;
    public String refVersion = null;
    public String oldRefVer = null;
    public String rsMergeFile = null;
    public String newRsMergeFile = null;
    public String snpChrPosFile = null;
    public String snpHistoryFile = null;
    public int snpIndex = 0;
    public int chr = 0;
    public int pos = 1;
    public boolean byid = false;
    public boolean bypos = false;
    public boolean bim = false;
    public boolean map = false;
    public boolean noWeb = false;
    public boolean basic = false;
    public boolean plink = false;
    public boolean coor = false;
    public boolean setmerge = false;
    public boolean setcoor = false;
    public boolean sethist = false;
    public boolean convert = false;

    private int find(String opp) {
        for (int i = 0; i < optionNum; i++) {
            if (options[i].equals(opp)) {
                return i;
            }
        }
        return -1;
    }

    private int findPart(String opp) {
        for (int i = 0; i < optionNum; i++) {
            if (options[i].contains(opp)) {
                return i;
            }
        }
        return -1;
    }
    /*
     private boolean allEvenFind(String opp) {
     //if all even parameters are "--",allEven=true;
     boolean allEven = false;
     for (int i = 0; i < optionNum; i = i + 2) {
     if (options[i].contains(opp)) {
     allEven = true;
     } else {
     allEven = false;
     break;
     }
     }
     return allEven;
     }
    
     private boolean allOddFind(String opp) {
     //if all even parameters are "--",allOdd=true;
     boolean allOdd = false;
     for (int i = 1; i < optionNum; i = i + 2) {
     if (options[i].contains(opp)) {
     allOdd = true;
     } else {
     allOdd = false;
     break;
     }
     }
     return allOdd;
     }
    
     private boolean oneEvenFind(String opp) {
     //if there is one even parameters equal "--",even=true;
     boolean even = false;
     for (int i = 0; i < optionNum; i = i + 2) {
     if (options[i].contains(opp)) {
     even = true;
     break;
     } else {
     even = false;
     }
     }
     return even;
     }
    
     private boolean oneOddFind(String opp) {
     //if there is one odd parameters equal "--",Odd=true;
     boolean Odd = false;
     for (int i = 1; i < optionNum; i = i + 2) {
     if (options[i].contains(opp)) {
     Odd = true;
     break;
     } else {
     Odd = false;
     }
     }
     return Odd;
     }*/

    public boolean noWebCheck() {
        int id = find("--no-web");
        if (id >= 0) {
            return true;
        }
        return false;
    }

    public boolean parseOptions() throws Exception {
        boolean flag = false;
        StringBuilder param = new StringBuilder();

        if (optionNum > 1) {
            int id = -1;
            id = findPart("--");
            if (id >= 0) {
                // Usage: java -Xmx1g -jar snptracker.jar --in input.txt --rsid 1 --out output.txt
                flag = true;
            } else {
                // Usage: java -Xmx1g -jar snptracker.jar input.txt 1 output.txt
                flag = false;
            }
        }
        /*
         * Commands
         */
        /*basic: rsid or coor; bim-file; map-file*/
        if ((find("--chr") >= 0) || (find("--pos") >= 0)) {
            coor = true;
        } else if ((find("--map-file") >= 0)) {
            map = true;
        } else if ((find("--bim-file") >= 0)) {
            bim = true;
        } else {
            basic = true;
        }

        if (flag) {
            int id = -1;

            /* begin----for plink bim file*/
            if (bim) {
                id = find("--bim-file");
                if (id >= 0 && optionNum > 0) {
                    inputFileName = options[id + 1];
                    param.append("--bim-file");
                    param.append(' ');
                    param.append(inputFileName);
                    param.append('\n');
                }

                id = find("--out");
                if (id >= 0) {
                    resultFileName = options[id + 1];
                    if (inputFileName.endsWith("gz")) {
                        resultFileName = resultFileName + ".result.bim.gz";
                    } else {
                        resultFileName = resultFileName + ".result.bim";
                    }
                } else {
                    resultFileName = "plink.result.bim";
                }
                param.append("--out");
                param.append(' ');

                param.append(resultFileName);
                param.append("\n");

                id = find("--by-id");
                if (id >= 0) {
                    byid = true;
                    snpIndex = 1;
                    param.append("--by-id");
                    param.append("\n");
                } else {
                    id = find("--by-pos");
                    if (id >= 0) {
                        bypos = true;
                        chr = 0;
                        pos = 3;
                        param.append("--by-pos");
                        param.append("\n");
                    } else {
                        byid = true;
                        snpIndex = 1;
                        param.append("--by-id (Default)");
                        param.append("\n");
                    }
                }

                /*end----for plink bim file*/
            } else if (map) {
                /* for plink map file */
                id = find("--map-file");
                if (id >= 0 && optionNum > 0) {
                    inputFileName = options[id + 1];
                    param.append("--map-file");
                    param.append(' ');
                    param.append(inputFileName);
                    param.append('\n');
                }

                id = find("--out");
                if (id >= 0) {
                    resultFileName = options[id + 1];
                    if (inputFileName.endsWith("gz")) {
                        resultFileName = resultFileName + ".result.map.gz";
                    } else {
                        resultFileName = resultFileName + ".result.map";
                    }
                } else {
                    resultFileName = "plink.result.map";
                }
                param.append("--out");
                param.append(' ');
                param.append(resultFileName);
                param.append("\n");

                id = find("--by-id");
                if (id >= 0) {
                    byid = true;
                    snpIndex = 1;
                    param.append("--by-id");
                    param.append("\n");
                } else {
                    id = find("--by-pos");
                    if (id >= 0) {
                        bypos = true;
                        chr = 1;
                        pos = 3;
                        param.append("--by-pos");
                        param.append("\n");
                    }

                }

                /*end----for plink map file*/
            } else if (coor) {
                coor = true;
                /*begin---for tracking id by coordinate*/
                id = find("--in");
                inputFileName = options[id + 1];
                param.append("--in");
                param.append(' ');
                param.append(inputFileName);
                param.append('\n');

                id = find("--out");
                if (id >= 0 && optionNum > 0) {
                    resultFileName = options[id + 1];
                    param.append("--out");
                    param.append(' ');
                    param.append(resultFileName);
                    param.append('\n');
                    errorFileName = resultFileName + ".error.txt";
                    if (inputFileName.endsWith("gz")) {
                        resultFileName = resultFileName + ".result.txt.gz";
                    } else {
                        resultFileName = resultFileName + ".result.txt";
                    }
                } else {
                    String infor = "No --out option to save result data!";
                    //throw new Exception(infor);
                    //return false;
                    if (inputFileName.endsWith("gz")) {
                        resultFileName = "snptracker.result.txt.gz";
                    } else {
                        resultFileName = "snptracker.result.txt";
                    }
                    errorFileName = "snptracker.error.txt";
                    param.append("--out");
                    param.append(' ');
                    param.append(resultFileName);
                    param.append("(Default)" + '\n');
                }

                id = find("--chr");
                if (id >= 0 && optionNum > 0) {
                    chr = Integer.parseInt(options[id + 1]);
                    param.append("--chr");
                    param.append(' ');
                    param.append(chr);
                    param.append('\n');
                    chr = chr - 1;
                } else {
                    param.append("--chr");
                    param.append(' ');
                    param.append(chr + 1);
                    param.append("(Default)" + '\n');
                }

                id = find("--pos");
                if (id >= 0 && optionNum > 0) {
                    pos = Integer.parseInt(options[id + 1]);
                    param.append("--pos");
                    param.append(' ');
                    param.append(pos);
                    param.append('\n');
                    pos = pos - 1;
                } else {
                    param.append("--pos");
                    param.append(' ');
                    param.append(pos + 1);
                    param.append("(Default)" + '\n');
                }

                /*end---for tracking id by coordinate*/
            } else if (basic) {
                id = find("--in");
                inputFileName = options[id + 1];
                if (inputFileName.equals("")) {
                    String infor = "Please set input file by --in or --map-file or --bim-file option!\nNote:\n1.--in input file with rsid or coordinate data\n2.--map-file input file in plink map format\n3.--bim-file input file in plink bim format\n";
                    throw new Exception(infor);
                    //return false;
                }
                param.append("--in");
                param.append(' ');
                param.append(inputFileName);
                param.append('\n');

                id = find("--out");
                if (id >= 0 && optionNum > 0) {
                    resultFileName = options[id + 1];
                    param.append("--out");
                    param.append(' ');
                    param.append(resultFileName);
                    param.append('\n');
                    errorFileName = resultFileName + ".error.txt";
                    if (inputFileName.endsWith("gz")) {
                        resultFileName = resultFileName + ".result.txt.gz";
                    } else {
                        resultFileName = resultFileName + ".result.txt";
                    }
                } else {
                    String infor = "No --out option to save result data!";
                    //throw new Exception(infor);
                    //return false;
                    if (inputFileName.endsWith("gz")) {
                        resultFileName = "snptracker.result.txt.gz";
                    } else {
                        resultFileName = "snptracker.result.txt";
                    }
                    errorFileName = "snptracker.error.txt";
                    param.append("--out");
                    param.append(' ');
                    param.append(resultFileName);
                    param.append("(Default)" + '\n');
                }

                id = find("--rsid");
                if (id >= 0 && optionNum > 0) {
                    snpIndex = Integer.parseInt(options[id + 1]);

                    param.append("--rsid");
                    param.append(' ');
                    param.append(snpIndex);
                    param.append('\n');
                    snpIndex = snpIndex - 1;
                } else {
                    snpIndex = 0;
                    param.append("--rsid");
                    param.append(' ');
                    param.append(1);
                    param.append("(Default)" + '\n');
                    //return false;
                }
            }
            id = find("--ref");
            if (id >= 0 && optionNum > 0) {
                refVersion = options[id + 1];
                param.append("--ref");
                param.append(' ');
                param.append(refVersion);
                if (refVersion.equals("hg17") || refVersion.equals("hg18")) {
                    param.append(" (Defaultly converting into hg19)");
                    oldRefVer = refVersion;
                    refVersion = "hg19";
                    convert = true;
                }
                param.append('\n');
            } else {
                refVersion = "hg19";
                param.append("--ref");
                param.append(' ');
                param.append(refVersion);
                param.append("(Default)" + '\n');
            }

            id = find("--merge-file");
            if (id >= 0 && optionNum > 0) {
                rsMergeFile = options[id + 1];
                param.append("--merge-file");
                param.append(' ');
                param.append(rsMergeFile);
                param.append('\n');

                setmerge = true;
            } else {
                if ("hg19".equals(refVersion)) {
                    rsMergeFile = "b142_GRCh19.RsMergeArch.bcp.gz";
                } else if ("hg38".equals(refVersion)) {
                    rsMergeFile = "b142_GRCh38.RsMergeArch.bcp.gz";
                }

                param.append("--merge-file");
                param.append(' ');
                param.append(rsMergeFile);
                param.append("(Default)" + '\n');
            }
            newRsMergeFile = rsMergeFile.replace("gz", "snptracker.gz");

            id = find("--coor-file");
            if (id >= 0 && optionNum > 0) {
                snpChrPosFile = options[id + 1];
                param.append("--coor-file");
                param.append(' ');
                param.append(snpChrPosFile);
                param.append('\n');
                setcoor = true;
            } else {
                if ("hg19".equals(refVersion)) {
                    snpChrPosFile = "b142_SNPChrPosOnRef_GRCh19p105.bcp.gz";
                } else if ("hg38".equals(refVersion)) {
                    snpChrPosFile = "b142_SNPChrPosOnRef_GRCh38p106.bcp.gz";
                }
                param.append("--coor-file");
                param.append(' ');
                param.append(snpChrPosFile);
                param.append("(Default)" + '\n');
            }

            id = find("--hist-file");
            if (id >= 0 && optionNum > 0) {
                snpHistoryFile = options[id + 1];
                param.append("--hist-file");
                param.append(' ');
                param.append(snpHistoryFile);
                param.append('\n');

                sethist = true;
            } else {
                if ("hg19".equals(refVersion)) {
                    snpHistoryFile = "b142_GRCh19.SNPHistory.bcp.gz";
                } else if ("hg38".equals(refVersion)) {
                    snpHistoryFile = "b142_GRCh38.SNPHistory.bcp.gz";
                }

                param.append("--hist-file");
                param.append(' ');
                param.append(snpHistoryFile);
                param.append("(Default)" + '\n');
            }
            System.out.println("Effective settings :");
            System.out.println(param);
            return true;

        } else {
            basic = true;
            if (optionNum == 1) {
                inputFileName = options[0];
                String infor = "No --rsid option to identify id column in data! Default --rsid 1\nNo --out option to save result data! Default --out snptracker.result.txt";
                snpIndex = 0;
                refVersion = "hg19";
                if (inputFileName.endsWith("gz")) {
                    resultFileName = "snptracker.result.txt.gz";
                } else {
                    resultFileName = "snptracker.result.txt";
                }
                errorFileName = "snptracker.error.txt";
                param.append("input: ");
                param.append(inputFileName);
                param.append('\n');
                param.append("Default rsid column: " + 1);
                param.append('\n');
                param.append("Default reference: " + refVersion);
                param.append('\n');
                param.append("Default output: " + resultFileName);
                param.append('\n');
            } else if (optionNum == 2) {
                inputFileName = options[0];
                if (options[1].matches("[0-9]+")) {
                    snpIndex = Integer.parseInt(options[1]) - 1;
                    refVersion = "hg19";
                    if (inputFileName.endsWith("gz")) {
                        resultFileName = "snptracker.result.txt.gz";
                    } else {
                        resultFileName = "snptracker.result.txt";
                    }
                    errorFileName = "snptracker.error.txt";
                    param.append("input: ");
                    param.append(inputFileName);
                    param.append('\n');
                    param.append("rsid column: ");
                    param.append(snpIndex + 1);
                    param.append('\n');
                    param.append("Default reference: " + refVersion);
                    param.append('\n');
                    param.append("Default output: " + resultFileName);
                    param.append('\n');
                } else if (options[1].equals("hg19") || options[1].equals("hg38") || options[1].equals("hg17") || options[1].equals("hg18")) {
                    snpIndex = 0;
                    refVersion = options[1];
                    if (inputFileName.endsWith("gz")) {
                        resultFileName = "snptracker.result.txt.gz";
                    } else {
                        resultFileName = "snptracker.result.txt";
                    }
                    errorFileName = "snptracker.error.txt";
                    param.append("input: ");
                    param.append(inputFileName);
                    param.append('\n');
                    param.append("Default rsid column: " + 1);
                    param.append('\n');
                    param.append("reference: ");
                    param.append(refVersion);
                    if (refVersion.equals("hg17") || refVersion.equals("hg18")) {
                        param.append(" (Defaultly converting into hg19)");
                        oldRefVer = refVersion;
                        refVersion = "hg19";
                        convert = true;
                    }
                    param.append('\n');
                    param.append("Default output: " + resultFileName);
                    param.append('\n');
                } else {
                    String infor = "No --rsid option to identify id column in data! Default --rsid 1\nNo --ref option to identify reference version! Default --ref hg19";
                    snpIndex = 0;
                    refVersion = "hg19";
                    if (inputFileName.endsWith("gz")) {
                        resultFileName = options[1] + ".result.txt.gz";
                    } else {
                        resultFileName = options[1] + ".result.txt";
                    }
                    errorFileName = options[1] + ".error.txt";
                    param.append("input: ");
                    param.append(inputFileName);
                    param.append('\n');
                    param.append("Default rsid column: " + 1);
                    param.append('\n');
                    param.append("Default reference: " + refVersion);
                    param.append('\n');
                    param.append("output: ");
                    param.append(resultFileName);
                    param.append('\n');
                }
            } else if (optionNum == 3) {
                inputFileName = options[0];
                if (options[1].matches("[0-9]+") && (options[2].equals("hg19") || options[2].equals("hg38") || options[2].equals("hg17") || options[2].equals("hg18"))) {
                    snpIndex = Integer.parseInt(options[1]) - 1;
                    refVersion = options[2];
                    if (inputFileName.endsWith("gz")) {
                        resultFileName = "snptracker.result.txt.gz";
                    } else {
                        resultFileName = "snptracker.result.txt";
                    }
                    errorFileName = "snptracker.error.txt";
                } else if ((options[1].equals("hg19") || options[1].equals("hg38") || options[1].equals("hg17") || options[1].equals("hg18"))) {
                    snpIndex = 0;
                    refVersion = options[1];
                    if (inputFileName.endsWith("gz")) {
                        resultFileName = options[2] + ".result.txt.gz";
                    } else {
                        resultFileName = options[2] + ".result.txt";
                    }
                    errorFileName = options[2] + ".error.txt";
                } else if (options[1].matches("[0-9]+")) {
                    snpIndex = Integer.parseInt(options[1]) - 1;
                    refVersion = "hg19";
                    if (inputFileName.endsWith("gz")) {
                        resultFileName = options[2] + ".result.txt.gz";
                    } else {
                        resultFileName = options[2] + ".result.txt";
                    }
                    errorFileName = options[2] + ".error.txt";
                }
                param.append("input: ");
                param.append(inputFileName);
                param.append('\n');
                param.append("rsid column: ");
                param.append(snpIndex + 1);
                param.append('\n');
                param.append("reference: ");
                param.append(refVersion);
                if (refVersion.equals("hg17") || refVersion.equals("hg18")) {
                    param.append(" (Defaultly converting into hg19)");
                    oldRefVer = refVersion;
                    refVersion = "hg19";
                    convert = true;
                }
                param.append('\n');
                param.append("output: ");
                param.append(resultFileName);
                param.append('\n');
            } else if (optionNum == 4) {
                inputFileName = options[0];
                snpIndex = Integer.parseInt(options[1]) - 1;
                refVersion = options[2];
                if (inputFileName.endsWith("gz")) {
                    resultFileName = options[3] + ".result.txt.gz";
                } else {
                    resultFileName = options[3] + ".result.txt";
                }
                errorFileName = options[3] + ".error.txt";
                param.append("input: ");
                param.append(inputFileName);
                param.append('\n');
                param.append("rsid column: ");
                param.append(snpIndex + 1);
                param.append('\n');
                param.append("reference: ");
                param.append(refVersion);
                if (refVersion.equals("hg17") || refVersion.equals("hg18")) {
                    param.append(" (Defaultly converting into hg19)");
                    oldRefVer = refVersion;
                    refVersion = "hg19";
                    convert = true;
                }
                param.append('\n');
                param.append("output: ");
                param.append(resultFileName);
                param.append('\n');
            }
        }

        if ("hg19".equals(refVersion)) {
            rsMergeFile = "b142_GRCh19.RsMergeArch.bcp.gz";
            snpChrPosFile = "b142_SNPChrPosOnRef_GRCh19p105.bcp.gz";
            snpHistoryFile = "b142_GRCh19.SNPHistory.bcp.gz";
            newRsMergeFile = rsMergeFile.replace("gz", "snptracker.gz");
        } else if ("hg38".equals(refVersion)) {
            rsMergeFile = "b142_GRCh38.RsMergeArch.bcp.gz";
            snpChrPosFile = "b142_SNPChrPosOnRef_GRCh38p106.bcp.gz";
            snpHistoryFile = "b142_GRCh38.SNPHistory.bcp.gz";
            newRsMergeFile = rsMergeFile.replace("gz", "snptracker.gz");
        }
        System.out.println("Effective settings :");
        System.out.println(param);

        return true;
    }

    public void readOptions(String fileName) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line = "";
        int lineNumber = 0;
        StringBuilder tmpStr = new StringBuilder();
        List<String> optionList = new ArrayList<String>();
        //assume every parameter has a line
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            if (line.startsWith("//")) {
                continue;
            }
            int index = line.indexOf("//");
            if (index >= 0) {
                line = line.substring(0, index);
            }
            //  System.out.println(line);
            StringTokenizer tokenizer = new StringTokenizer(line);
            //sometimes tokenizer.nextToken() can not release memory

            while (tokenizer.hasMoreTokens()) {
                //parameter Name value
                optionList.add(tmpStr.append(tokenizer.nextToken().trim()).toString());
                tmpStr.delete(0, tmpStr.length());
            }
            lineNumber++;
        }
        br.close();
        optionNum = optionList.size();
        options = new String[optionNum];
        for (int i = 0; i < optionNum; i++) {
            options[i] = optionList.get(i);
        }
    }

    public void readOptions(String[] args) throws Exception {
        optionNum = args.length;
        int tag = 0;
        for (int i = 0; i < optionNum; i++) {
            if (args[i].equals("--no-web")) {
                noWeb = true;
                tag = i;
                break;
            }
        }

        if (noWeb) {
            optionNum--;
            int j = 0;
            options = new String[optionNum];
            for (int k = 0; k < args.length; k++) {
                if (k == tag) {
                    continue;
                } else {
                    Array.set(options, j, args[k]);
                    j++;
                }
            }
        } else {
            options = new String[optionNum];
            System.arraycopy(args, 0, options, 0, optionNum);
        }

    }
}
