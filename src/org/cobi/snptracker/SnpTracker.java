/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cobi.snptracker;

import cern.colt.map.OpenIntIntHashMap;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;
import net.sf.picard.liftover.LiftOver;
import net.sf.picard.util.Interval;
import net.sf.samtools.util.CompressedFileReader;
import net.sf.samtools.util.LineReader;
import org.cobi.snptracker.entity.Options;
import org.cobi.util.net.NetUtils;
import org.cobi.util.text.Util;
import org.cobi.util.rsmergefile.CreateOwnMergeFile;

/**
 *
 * @author mxli
 */
public class SnpTracker implements Constants {
    // ********************* Parameters **********************//

    private Map<String, Byte> inputRsIdHash;
    private Map<String, String> inputCoorHash;
    private Map<String, Byte> rsIdHash;
    private Map<String, Byte> snpHistoryHash;
    private int deleted = 0;
    private int invalid = 0;
    private int duplicated = 0;
    private int noPos = 0;
    private int unChr = 0;
    private int altOnly = 0;
    private int mt = 0;
    private int multi = 0;
    private int notOn = 0;
    private int par = 0;
    private boolean head = false;

    public SnpTracker() {
        inputRsIdHash = new HashMap<String, Byte>();
        inputCoorHash = new HashMap<String, String>();
        rsIdHash = new HashMap<String, Byte>();
        snpHistoryHash = new HashMap<String, Byte>();
    }

    public int getErr() {
        return altOnly + unChr + multi + notOn + deleted + invalid + noPos + duplicated;
    }

    public int getDeleted() {
        return deleted;
    }

    public int getInvalid() {
        return invalid;
    }

    public int getDuplicated() {
        return duplicated;
    }

    public int getNoPos() {
        return noPos;
    }

    public int getUnChr() {
        return unChr;
    }

    public int getAltOnly() {
        return altOnly;
    }

    public int getMt() {
        return mt;
    }

    public int getMulti() {
        return multi;
    }

    public int getNotOn() {
        return notOn;
    }

    public int getPar() {
        return par;
    }

    public void conversionId(String inFilePath, String resultFileName, String errorFilePath, int snpIndex,
            Map<String, String> cphash, Map<String, String> rsmhash, String ref) throws IOException, Exception {
        BufferedWriter bw;
        if (resultFileName.endsWith("gz") || inFilePath.endsWith("gz")) {
            bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(resultFileName))));
        } else {
            bw = new BufferedWriter(new FileWriter(new File(resultFileName)));
        }
        BufferedWriter bwe = new BufferedWriter(new FileWriter(new File(errorFilePath)));

        boolean space = false;
        LineReader bf = new CompressedFileReader(new File(inFilePath));
        if (head) {
            String header = bf.readLine().trim();
            if (header.indexOf(" ") != -1) {
                bw.write("CHR SNP BP " + header + "\n");
                space = true;
            } else if (header.indexOf("\t") != -1) {
                bw.write("CHR\tSNP\tBP\t" + header + "\n");
            }
        }
        String tmLine;
        //int n = 0;
        StringBuilder sb = new StringBuilder();
        String currentId;

        //note
        bwe.write("#Deleted\tvariants that is deleted in dbsnp database\n");
        bwe.write("#Invalid\tvariants that is reported as an invalid snp_id in dbsnp database\n");
        bwe.write("#DuplicatedRs\tvariants that merged in to a new Id which is already exists in the input file\n");
        bwe.write("#chr_AltOnly\tvariants that map to non-reference (alternative) assemblely(e.g., a human refSNP maps to HuRef or TCAGChr7, but not to GRC)\n");
        bwe.write("#chr_Multi\tvariants that map to multiple contigs\n");
        bwe.write("#chr_NotOn\tvariants that did not map to any current chromosome\n");
        bwe.write("#chr_Un\tmapped variants that are on unplaced chromosomes\n");
        bwe.write("#NoCoord.\tvariants that is without position information in the SNPChrPosOnRef.bcp file\n");
        bwe.write("-------------------------------------------------------------------------------------------------\n\n");
        while ((tmLine = bf.readLine()) != null) {
            tmLine = tmLine.trim();
            StringTokenizer st = new StringTokenizer(tmLine);
            //n++;
            for (int i = 0; i < snpIndex; i++) {
                st.nextToken();
            }
            sb.append(st.nextToken());
            String id = sb.toString().trim();
            //omit the "rs" label of rs ID
            if (id.startsWith("rs")) {
                id = id.substring(2);
            }
            sb.delete(0, sb.length());

            if (rsmhash.containsKey(id)) {
                currentId = rsmhash.get(id);
                if (currentId.equals("D")) {
                    this.deleted++;
                    bwe.write("Deleted\trs" + id + "\n");
                    continue;
                }
            } else {
                currentId = id;
            }

            // judge snp have deleted or not
            if (!cphash.containsKey(currentId)) {
                if (snpHistoryHash.containsKey(currentId)) {
                    this.deleted++;
                    if (currentId.equals(id)) {
                        bwe.write("Deleted\trs" + currentId + "\n");
                    } else {
                        bwe.write("Deleted\trs" + currentId + "\trs" + id + "(input id) is merged into rs" + currentId + "\n");
                    }
                    continue;
                } else {
                    this.invalid++;
                    if (currentId.equals(id)) {
                        bwe.write("Invalid\trs" + id + "\n");
                    } else {
                        bwe.write("Invalid\trs" + currentId + "\trs" + id + "(input id) is merged into rs" + currentId + "\n");
                    }
                    continue;
                }
            }

            if (cphash.get(currentId).contains("-9")) {
                this.noPos++;
                if (currentId.equals(id)) {
                    bwe.write("NoCoord.\trs" + currentId + "\n");
                } else {
                    bwe.write("NoCoord\trs" + currentId + "\trs" + id + "(input id) is merged into rs" + currentId + "\n");
                }
                continue;
            }

            // judge duplicated snp and unChromosome snp
            if (cphash.get(currentId).equals("Un\t1")) {
                this.unChr++;
                if (currentId.equals(id)) {
                    bwe.write("chr_Un\trs" + id + "\n");
                } else {
                    bwe.write("chr_Un\trs" + currentId + "\trs" + id + "(input id) is merged into rs" + currentId + "\n");
                }
                continue;
            }
            if (cphash.get(currentId).equals("AltOnly\t1")) {
                this.altOnly++;
                if (currentId.equals(id)) {
                    bwe.write("chr_AltOnly\trs" + id + "\n");
                } else {
                    bwe.write("chr_AltOnly\trs" + currentId + "\trs" + id + "(input id) is merged into rs" + currentId + "\n");
                }
                continue;
            }
            /*
             if (cphash.get(currentId).equals("MT\t1")) {
             this.mt++;
             bwe.write("#MT\t" + tmLine + "\n");
             continue;
             }
             */
            if (cphash.get(currentId).equals("Multi\t1")) {
                this.multi++;
                if (currentId.equals(id)) {
                    bwe.write("chr_Multi\trs" + id + "\n");
                } else {
                    bwe.write("chr_Multi\trs" + currentId + "\trs" + id + "(input id) is merged into rs" + currentId + "\n)");
                }
                continue;
            }
            if (cphash.get(currentId).equals("NotOn\t1")) {
                this.notOn++;
                if (currentId.equals(id)) {
                    bwe.write("chr_NotOn\trs" + id + "\n");
                } else {
                    bwe.write("chr_NotOn\trs" + currentId + "\trs" + id + "(input id) is merged into rs" + currentId + "\n");
                }
                continue;
            }
            /*
             if (cphash.get(currentId).equals("PAR\t1")) {
             this.par++;
             bwe.write("#PAR\t" + tmLine + "\n");
             continue;
             }
             */
            //DuplicatedRs means if oldId and currentId are both in input file, deleting oldId and keeping currentId in result.
            if ((inputRsIdHash.get(currentId)) > 1 && (!currentId.equals(id))) {
                this.duplicated++;
                bwe.write("DuplicatedRs\trs" + id + "\trs" + id + " is merged into rs" + currentId + "\trs" + currentId + " exists in the input file\n");
                continue;
            }
            String[] cells = Util.tokenize(cphash.get(currentId), '\t');
            if (space) {
                bw.write(cells[0] + " rs" + currentId + " " + cells[1] + " " + tmLine + "\n");
            } else {
                bw.write(cells[0] + "\trs" + currentId + "\t" + cells[1] + "\t" + tmLine + "\n");
            }
        }
        bw.flush();
        bw.close();
        bwe.flush();
        bwe.close();
    }

    public void conversionIdByPlinkMap(String inFilePath, String resultFileName, int snpIndex,
            Map<String, String> cphash, Map<String, String> rsmhash, String ref, boolean bypos, boolean convert, String oldRef) throws IOException, Exception {
        BufferedWriter bw;
        if (resultFileName.endsWith("gz")|| inFilePath.endsWith("gz")) {
            bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(resultFileName))));
        } else {
            bw = new BufferedWriter(new FileWriter(new File(resultFileName)));
        }

        boolean space = false;
        LineReader bf = new CompressedFileReader(new File(inFilePath));
        String header = bf.readLine().trim();
        if (header.indexOf(" ") != -1) {
            bw.write("CHR SNP OR BP\n");
            space = true;
        } else if (header.indexOf("\t") != -1) {
            bw.write("CHR\tSNP\tOR\tBP\n");
        }

        String tmLine;
        //int n = 0;
        StringBuilder sb = new StringBuilder();
        String currentId;
        while ((tmLine = bf.readLine()) != null) {
            tmLine = tmLine.trim();
            StringTokenizer st = new StringTokenizer(tmLine);

            sb.append(st.nextToken());
            String chr = sb.toString();
            sb.delete(0, sb.length());

            sb.append(st.nextToken());
            String id = sb.toString().trim();
            //omit the "rs" label of rs ID
            if (id.startsWith("rs")) {
                id = id.substring(2);
            }
            sb.delete(0, sb.length());

            sb.append(st.nextToken());
            String or = sb.toString();
            sb.delete(0, sb.length());

            sb.append(st.nextToken());
            String pos = sb.toString();
            sb.delete(0, sb.length());

            if (bypos) {
                //liftover coordinates
                if (convert) {
                    File CHAIN_FILE = null;
                    if (oldRef.equals("hg17")) {
                        CHAIN_FILE = new File(GlobalManager.RESOURCE_PATH + File.separator + "hg17ToHg19.over.chain.gz");
                    } else if (oldRef.equals("hg18")) {
                        CHAIN_FILE = new File(GlobalManager.RESOURCE_PATH + File.separator + "hg18ToHg19.over.chain.gz");
                    }

                    LiftOver liftOver = new LiftOver(CHAIN_FILE);
                    int oldpos = Integer.parseInt(pos);
                    Interval interval = new Interval("chr" + chr, oldpos, oldpos);
                    Interval int2 = liftOver.liftOver(interval);
                    if (int2 != null) {
                        oldpos = int2.getStart();
                        pos = Integer.toString(oldpos);
                    }
                }

                if (inputCoorHash.containsKey(chr + ":" + pos)) {
                    if (inputCoorHash.get(chr + ":" + pos).equals("E")) {
                        if (space) {
                            bw.write("0" + " rs" + id + " " + or + " -1\n");
                        } else {
                            bw.write("0" + "\trs" + id + "\t" + or + "\t-1\n");
                        }
                        continue;
                    }
                    id = inputCoorHash.get(chr + ":" + pos);
                } else {
                    if (space) {
                        bw.write(chr + " rs" + id + " " + or + " -1\n");
                    } else {
                        bw.write(chr + "\trs" + id + "\t" + or + "\t-1\n");
                    }
                    continue;
                }
            }

            if (rsmhash.containsKey(id)) {
                currentId = rsmhash.get(id);
                if (currentId.equals("D")) {
                    this.deleted++;
                    if (space) {
                        bw.write(chr + " rs" + id + " " + or + " -1\n");
                    } else {
                        bw.write(chr + "\trs" + id + "\t" + or + "\t-1\n");
                    }
                    continue;
                }
            } else {
                currentId = id;
            }

            // judge snp have deleted or not
            if (!cphash.containsKey(currentId)) {
                //FailToMap
                if (snpHistoryHash.containsKey(currentId)) {
                    this.deleted++;
                    if (space) {
                        bw.write(chr + " rs" + currentId + " " + or + " -1\n");
                    } else {
                        bw.write(chr + "\trs" + currentId + "\t" + or + "\t-1\n");
                    }
                    continue;
                } else {
                    this.invalid++;
                    if (space) {
                        bw.write(chr + " rs" + currentId + " " + or + " -1\n");
                    } else {
                        bw.write(chr + "\trs" + currentId + "\t" + or + "\t-1\n");
                    }
                    continue;
                }

            }

            //DuplicatedRs means if oldId and currentId are both in input file, deleting oldId and keeping currentId in result.
            if ((inputRsIdHash.get(currentId)) > 1 && (!currentId.equals(id))) {
                this.duplicated++;
                if (space) {
                    bw.write(chr + " rs" + currentId + " " + or + " -2\n");
                } else {
                    bw.write(chr + "\trs" + currentId + "\t" + or + "\t-2\n");
                }
                continue;
            }
            //NoPosInChrPosRef
            if (cphash.get(currentId).contains("-9")) {
                this.noPos++;
                if (space) {
                    bw.write(chr + " rs" + currentId + " " + or + " -3\n");
                } else {
                    bw.write(chr + "\trs" + currentId + "\t" + or + "\t-3\n");
                }
                continue;
            }

            if (cphash.get(currentId).equals("AltOnly\t1")) {
                this.altOnly++;
                if (space) {
                    bw.write(chr + " rs" + currentId + " " + or + " -4\n");
                } else {
                    bw.write(chr + "\trs" + currentId + "\t" + or + "\t-4\n");
                }
                continue;
            }
            // judge duplicated snp and unChromosome snp
            if (cphash.get(currentId).equals("Un\t1")) {
                this.unChr++;
                if (space) {
                    bw.write(chr + " rs" + currentId + " " + or + " -5\n");
                } else {
                    bw.write(chr + "\trs" + currentId + "\t" + or + "\t-5\n");
                }
                continue;
            }
            /*
             if (cphash.get(currentId).equals("MT\t1")) {
             this.mt++;
             if (space) {
             bw.write(chr + " rs" + currentId + " " + or + " -6\n");
             } else {
             bw.write(chr + "\trs" + currentId + "\t" + or + "\t-6\n");
             }
             continue;
             }*/
            if (cphash.get(currentId).equals("Multi\t1")) {
                this.multi++;
                if (space) {
                    bw.write(chr + " rs" + currentId + " " + or + " -7\n");
                } else {
                    bw.write(chr + "\trs" + currentId + "\t" + or + "\t-7\n");
                }
                continue;
            }
            if (cphash.get(currentId).equals("NotOn\t1")) {
                this.notOn++;
                if (space) {
                    bw.write(chr + " rs" + currentId + " " + or + " -8\n");
                } else {
                    bw.write(chr + "\trs" + currentId + "\t" + or + "\t-8\n");
                }
                continue;
            }
            /*
             if (cphash.get(currentId).equals("PAR\t1")) {
             this.par++;
             if (space) {
             bw.write(chr + " rs" + currentId + " " + or + " -9\n");
             } else {
             bw.write(chr + "\trs" + currentId + "\t" + or + "\t-9\n");
             }
             continue;
             }*/

            String[] cells = Util.tokenize(cphash.get(currentId), '\t');
            if (space) {
                if (cells[0].equals("X")) {
                    bw.write("23 rs" + currentId + " " + or + " " + cells[1] + "\n");
                } else if (cells[0].equals("Y")) {
                    bw.write("24 rs" + currentId + " " + or + " " + cells[1] + "\n");
                } else if (cells[0].equals("PAR")) {
                    bw.write("25 rs" + currentId + " " + or + " " + cells[1] + "\n");
                } else if (cells[0].equals("MT")) {
                    bw.write("26 rs" + currentId + " " + or + " " + cells[1] + "\n");
                } else {
                    bw.write(cells[0] + " rs" + currentId + " " + or + " " + cells[1] + "\n");
                }
            } else {
                if (cells[0].equals("X")) {
                    bw.write("23\trs" + currentId + "\t" + or + "\t" + cells[1] + "\n");
                } else if (cells[0].equals("Y")) {
                    bw.write("24\trs" + currentId + "\t" + or + "\t" + cells[1] + "\n");
                } else if (cells[0].equals("PAR")) {
                    bw.write("25\trs" + currentId + "\t" + or + "\t" + cells[1] + "\n");
                } else if (cells[0].equals("MT")) {
                    bw.write("26\trs" + currentId + "\t" + or + "\t" + cells[1] + "\n");
                } else {
                    bw.write(cells[0] + "\trs" + currentId + "\t" + or + "\t" + cells[1] + "\n");
                }

            }
        }
        bw.flush();
        bw.close();

    }

    public void conversionIdByPlinkBim(String inFilePath, String resultFileName, int snpIndex,
            Map<String, String> cphash, Map<String, String> rsmhash, String ref, boolean bypos, boolean convert, String oldRef) throws IOException, Exception {
        BufferedWriter bw;
        if (resultFileName.endsWith("gz")|| inFilePath.endsWith("gz")) {
            bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(resultFileName))));
        } else {
            bw = new BufferedWriter(new FileWriter(new File(resultFileName)));
        }

        boolean space = false;
        LineReader bf = new CompressedFileReader(new File(inFilePath));

        /*
         //bim file has no rsID.
         String header = bf.readLine().trim();
         if (header.indexOf(" ") != -1) {
         bw.write("CHR SNP OR BP Ref Alt\n");
         space = true;
         } else if (header.indexOf("\t") != -1) {
         bw.write("CHR\tSNP\tOR\tBP\tRef\tAlt\n");
         }
         * 
         */
        String tmLine;
        //int n = 0;
        StringBuilder sb = new StringBuilder();
        String currentId;
        while ((tmLine = bf.readLine()) != null) {
            tmLine = tmLine.trim();
            StringTokenizer st = new StringTokenizer(tmLine);

            sb.append(st.nextToken());
            String chr = sb.toString();
            sb.delete(0, sb.length());

            sb.append(st.nextToken());
            String id = sb.toString().trim();
            //omit the "rs" label of rs ID
            if (id.startsWith("rs")) {
                id = id.substring(2);
            }
            sb.delete(0, sb.length());

            sb.append(st.nextToken());
            String or = sb.toString();
            sb.delete(0, sb.length());

            sb.append(st.nextToken());
            String pos = sb.toString();
            sb.delete(0, sb.length());

            sb.append(st.nextToken());
            String refallele = sb.toString();
            sb.delete(0, sb.length());

            sb.append(st.nextToken());
            String altallele = sb.toString();
            sb.delete(0, sb.length());

            if (bypos) {
                //liftover coordinates
                if (convert) {
                    File CHAIN_FILE = null;
                    if (oldRef.equals("hg17")) {
                        CHAIN_FILE = new File(GlobalManager.RESOURCE_PATH + File.separator + "hg17ToHg19.over.chain.gz");
                    } else if (oldRef.equals("hg18")) {
                        CHAIN_FILE = new File(GlobalManager.RESOURCE_PATH + File.separator + "hg18ToHg19.over.chain.gz");
                    }

                    LiftOver liftOver = new LiftOver(CHAIN_FILE);
                    int oldpos = Integer.parseInt(pos);
                    Interval interval = new Interval("chr" + chr, oldpos, oldpos);
                    Interval int2 = liftOver.liftOver(interval);
                    if (int2 != null) {
                        oldpos = int2.getStart();
                        pos = Integer.toString(oldpos);
                    }
                }
                if (inputCoorHash.containsKey(chr + ":" + pos)) {
                    if (inputCoorHash.get(chr + ":" + pos).equals("E")) {
                        if (space) {
                            bw.write("0" + " rs" + id + " " + or + " 0 " + refallele + " " + altallele + "\n");
                        } else {
                            bw.write("0" + "\trs" + id + "\t" + or + "\t0\t" + refallele + "\t" + altallele + "\n");
                        }
                        continue;
                    }
                    id = inputCoorHash.get(chr + ":" + pos);
                } else {
                    if (space) {
                        bw.write("0" + " rs" + id + " " + or + " 0 " + refallele + " " + altallele + "\n");
                    } else {
                        bw.write("0" + "\trs" + id + "\t" + or + "\t0\t" + refallele + "\t" + altallele + "\n");
                    }
                    continue;
                }
            }

            if (rsmhash.containsKey(id)) {
                currentId = rsmhash.get(id);
                if (currentId.equals("D")) {
                    this.deleted++;
                    if (space) {
                        bw.write("0" + " rs" + id + " " + or + " 0 " + refallele + " " + altallele + "\n");
                    } else {
                        bw.write("0" + "\trs" + id + "\t" + or + "\t0\t" + refallele + "\t" + altallele + "\n");
                    }
                    continue;
                }
            } else {
                currentId = id;
            }

            // judge snp have deleted or not
            if (!cphash.containsKey(currentId)) {
                //FailToMap
                if (snpHistoryHash.containsKey(currentId)) {
                    this.deleted++;
                    if (space) {
                        bw.write("0" + " rs" + id + " " + or + " 0 " + refallele + " " + altallele + "\n");
                    } else {
                        bw.write("0" + "\trs" + id + "\t" + or + "\t0\t" + refallele + "\t" + altallele + "\n");
                    }
                    continue;
                } else {
                    this.invalid++;
                    if (space) {
                        bw.write("0" + " rs" + id + " " + or + " 0 " + refallele + " " + altallele + "\n");
                    } else {
                        bw.write("0" + "\trs" + id + "\t" + or + "\t0\t" + refallele + "\t" + altallele + "\n");
                    }
                    continue;
                }
            }

            //DuplicatedRs means if oldId and currentId are both in input file, deleting oldId and keeping currentId in result.
            if ((inputRsIdHash.get(currentId)) > 1 && (!currentId.equals(id))) {
                this.duplicated++;
                if (space) {
                    bw.write("0" + " rs" + id + " " + or + " 0 " + refallele + " " + altallele + "\n");
                } else {
                    bw.write("0" + "\trs" + id + "\t" + or + "\t0\t" + refallele + "\t" + altallele + "\n");
                }
                continue;
            }
            //NoPosInChrPosRef
            if (cphash.get(currentId).contains("-9")) {
                this.noPos++;
                if (space) {
                    bw.write("0" + " rs" + id + " " + or + " 0 " + refallele + " " + altallele + "\n");
                } else {
                    bw.write("0" + "\trs" + id + "\t" + or + "\t0\t" + refallele + "\t" + altallele + "\n");
                }
                continue;
            }

            if (cphash.get(currentId).equals("AltOnly\t1")) {
                this.altOnly++;
                if (space) {
                    bw.write("0" + " rs" + id + " " + or + " 0 " + refallele + " " + altallele + "\n");
                } else {
                    bw.write("0" + "\trs" + id + "\t" + or + "\t0\t" + refallele + "\t" + altallele + "\n");
                }
                continue;
            }
            // judge duplicated snp and unChromosome snp
            if (cphash.get(currentId).equals("Un\t1")) {
                this.unChr++;
                if (space) {
                    bw.write("0" + " rs" + id + " " + or + " 0 " + refallele + " " + altallele + "\n");
                } else {
                    bw.write("0" + "\trs" + id + "\t" + or + "\t0\t" + refallele + "\t" + altallele + "\n");
                }
                continue;
            }
            /*
             if (cphash.get(currentId).equals("MT\t1")) {
             this.mt++;
             if (space) {
             bw.write("0" + " rs" + id + " " + or + " 0 " + refallele + " " + altallele + "\n");
             } else {
             bw.write("0" + "\trs" + id + "\t" + or + "\t0\t" + refallele + "\t" + altallele + "\n");
             }
             continue;
             }
             */
            if (cphash.get(currentId).equals("Multi\t1")) {
                this.multi++;
                if (space) {
                    bw.write("0" + " rs" + id + " " + or + " 0 " + refallele + " " + altallele + "\n");
                } else {
                    bw.write("0" + "\trs" + id + "\t" + or + "\t0\t" + refallele + "\t" + altallele + "\n");
                }
                continue;
            }
            if (cphash.get(currentId).equals("NotOn\t1")) {
                this.notOn++;
                if (space) {
                    bw.write("0" + " rs" + id + " " + or + " 0 " + refallele + " " + altallele + "\n");
                } else {
                    bw.write("0" + "\trs" + id + "\t" + or + "\t0\t" + refallele + "\t" + altallele + "\n");
                }
                continue;
            }
            /*
             if (cphash.get(currentId).equals("PAR\t1")) {
             this.par++;
             if (space) {
             bw.write("0" + " rs" + id + " " + or + " 0 " + refallele + " " + altallele + "\n");
             } else {
             bw.write("0" + "\trs" + id + "\t" + or + "\t0\t" + refallele + "\t" + altallele + "\n");
             }
             continue;
             }*/

            String[] cells = Util.tokenize(cphash.get(currentId), '\t');
            if (space) {
                if (cells[0].equals("X")) {
                    bw.write("23 rs" + currentId + " " + or + " " + cells[1] + " " + refallele + " " + altallele + "\n");
                } else if (cells[0].equals("Y")) {
                    bw.write("24 rs" + currentId + " " + or + " " + cells[1] + " " + refallele + " " + altallele + "\n");
                } else if (cells[0].equals("PAR")) {
                    bw.write("25 rs" + currentId + " " + or + " " + cells[1] + " " + refallele + " " + altallele + "\n");
                } else if (cells[0].equals("MT")) {
                    bw.write("26 rs" + currentId + " " + or + " " + cells[1] + " " + refallele + " " + altallele + "\n");
                } else {
                    bw.write(cells[0] + " rs" + currentId + " " + or + " " + cells[1] + " " + refallele + " " + altallele + "\n");
                }
            } else {
                if (cells[0].equals("X")) {
                    bw.write("23\trs" + currentId + "\t" + or + "\t" + cells[1] + "\t" + refallele + "\t" + altallele + "\n");
                } else if (cells[0].equals("Y")) {
                    bw.write("24\trs" + currentId + "\t" + or + "\t" + cells[1] + "\t" + refallele + "\t" + altallele + "\n");
                } else if (cells[0].equals("PAR")) {
                    bw.write("25\trs" + currentId + "\t" + or + "\t" + cells[1] + "\t" + refallele + "\t" + altallele + "\n");
                } else if (cells[0].equals("MT")) {
                    bw.write("26\trs" + currentId + "\t" + or + "\t" + cells[1] + "\t" + refallele + "\t" + altallele + "\n");
                } else {
                    bw.write(cells[0] + "\trs" + currentId + "\t" + or + "\t" + cells[1] + "\t" + refallele + "\t" + altallele + "\n");
                }
            }
        }
        bw.flush();
        bw.close();

    }

    public void conversionIdByCoor(String inFilePath, String resultFileName, String errorFilePath, Map<String, String> cphash,
            Map<String, String> rsmhash, int chrIndex, int posIndex, String oldRef, boolean convert) throws IOException, Exception {
        BufferedWriter bw;
        if (resultFileName.endsWith("gz")|| inFilePath.endsWith("gz")) {
            bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(resultFileName))));
        } else {
            bw = new BufferedWriter(new FileWriter(new File(resultFileName)));
        }
        BufferedWriter bwe = new BufferedWriter(new FileWriter(new File(errorFilePath)));

        boolean space = false;
        LineReader bf = new CompressedFileReader(new File(inFilePath));
        if (head) {
            String header = bf.readLine().trim();
            if (header.indexOf(" ") != -1) {
                bw.write("CHR SNP BP " + header + "\n");
                space = true;
            } else if (header.indexOf("\t") != -1) {
                bw.write("CHR\tSNP\tBP\t" + header + "\n");
            }
        }
        //note
        bwe.write("#Deleted\tvariants that is deleted in dbsnp database\n");
        bwe.write("#Invalid\tvariants that is reported as an invalid snp_id in dbsnp database\n");
        bwe.write("#DuplicatedRs\tvariants that merged in to a new Id which is already exists in the input file\n");
        bwe.write("#chr_AltOnly\tvariants that map to non-reference (alternative) assemblely(e.g., a human refSNP maps to HuRef or TCAGChr7, but not to GRC)\n");
        bwe.write("#chr_Multi\tvariants that map to multiple chromosomes\n");
        bwe.write("#chr_NotOn\tvariants that did not map to any current chromosome\n");
        bwe.write("#chr_Un\tmapped variants that are on unplaced chromosomes\n");
        bwe.write("#NoCoord.\tvariants that is without position information in the SNPChrPosOnRef.bcp file\n");
        bwe.write("-------------------------------------------------------------------------------------------------\n\n");

        String tmLine;
        String currentId;
        //int n = 0;
        StringBuilder sb = new StringBuilder();
        while ((tmLine = bf.readLine()) != null) {
            String id = null;
            StringTokenizer st = new StringTokenizer(tmLine.trim());
            for (int i = 0; i < chrIndex; i++) {
                st.nextToken();
            }
            sb.append(st.nextToken());
            String chr = sb.toString().trim();
            sb.delete(0, sb.length());

            StringTokenizer st2 = new StringTokenizer(tmLine.trim());
            for (int i = 0; i < posIndex; i++) {
                st2.nextToken();
            }
            sb.append(st2.nextToken());
            String pos = sb.toString().trim();
            sb.delete(0, sb.length());

            //liftover coordinates
            if (convert) {
                File CHAIN_FILE = null;
                if (oldRef.equals("hg17")) {
                    CHAIN_FILE = new File(GlobalManager.RESOURCE_PATH + File.separator + "hg17ToHg19.over.chain.gz");
                } else if (oldRef.equals("hg18")) {
                    CHAIN_FILE = new File(GlobalManager.RESOURCE_PATH + File.separator + "hg18ToHg19.over.chain.gz");
                }

                LiftOver liftOver = new LiftOver(CHAIN_FILE);
                int oldpos = Integer.parseInt(pos);
                Interval interval = new Interval("chr" + chr, oldpos, oldpos);
                Interval int2 = liftOver.liftOver(interval);
                if (int2 != null) {
                    oldpos = int2.getStart();
                    pos = Integer.toString(oldpos);
                }
            }

            if (chr.startsWith("c") || chr.startsWith("C")) {
                chr = chr.substring(3);
            }
            if (inputCoorHash.containsKey(chr + ":" + pos)) {
                if (inputCoorHash.get(chr + ":" + pos).equals("E")) {
                    bwe.write("Deleted\t" + chr + "\t" + pos + "\trs.\n");
                    continue;
                }
                id = inputCoorHash.get(chr + ":" + pos);
            } else {
                bwe.write("Deleted\t" + chr + "\t" + pos + "\trs.\n");
                //bwe.write("Deleted\t" + tmLine + "\n");
                continue;
            }

            if (rsmhash.containsKey(id)) {
                currentId = rsmhash.get(id);
                if (currentId.equals("D")) {
                    this.deleted++;
                    bwe.write("Deleted\t" + chr + "\t" + pos + "\trs" + id + "\n");
                    continue;
                }
            } else {
                currentId = id;
            }

            // judge snp have deleted or not
            if (!cphash.containsKey(currentId)) {
                if (snpHistoryHash.containsKey(currentId)) {
                    this.deleted++;
                    if (currentId.equals(id)) {
                        bwe.write("Deleted\t" + chr + "\t" + pos + "\trs" + currentId + "\n");
                    } else {
                        bwe.write("Deleted\t" + chr + "\t" + pos + "\trs" + currentId + "\trs" + id + "(input id) is merged into rs" + currentId + "\n");
                    }
                    continue;
                } else {
                    this.invalid++;
                    if (currentId.equals(id)) {
                        bwe.write("Invalid\trs" + id + "\n");
                    } else {
                        bwe.write("Invalid\t" + chr + "\t" + pos + "\trs" + currentId + "\trs" + id + "(input id) is merged into rs" + currentId + "\n");
                    }
                    continue;
                }
            }

            String[] cells = Util.tokenize(cphash.get(currentId), '\t');
            if (space) {
                bw.write(cells[0] + " rs" + currentId + " " + cells[1] + " " + tmLine + "\n");
            } else {
                bw.write(cells[0] + "\trs" + currentId + "\t" + cells[1] + "\t" + tmLine + "\n");
            }
        }
        bw.flush();
        bw.close();
        bwe.flush();
        bwe.close();

    }

    public Map<String, String> readChrPosBackground(String cF) throws IOException, Exception {
        String tmLine;
        Map<String, String> chrPosHash = new HashMap<String, String>();
        int pos = 0;
        LineReader bf = new CompressedFileReader(new File(cF));
        StringBuilder sb = new StringBuilder();
        while ((tmLine = bf.readLine()) != null) {
            /*
             if (chrPosHash.size() == inputRsIdHash.size()) {
             break;
             }
             */
            String[] array = Util.tokenize(tmLine, '\t', 2);

            /*
             sb.append(array[0]);
             String id = sb.toString();
             sb.delete(0, sb.length());
            
             sb.append(array[1]);
             String chr = sb.toString();
             sb.delete(0, sb.length());
             * 
             */
            String id = array[0];
            String chr = array[1];

            if (inputRsIdHash.containsKey(id)) {
                if (chr.equals("Un") || chr.equals("AltOnly") || chr.equals("Multi") || chr.equals("NotOn")) {
                    pos = 0;
                } else {
                    if (array[2].isEmpty()) {
                        pos = -9;
                        chrPosHash.put(id, chr + "\t" + pos);
                        continue;
                    }
                    pos = Util.parseInt(array[2]);
                    //   pos = Integer.parseInt(array[2]);
                }

                // pos in dbsnp SNPChrPosOnRef is 0-based, So we changed it into 1-based and save in chrPosHash
                pos++;
                chrPosHash.put(id, chr + "\t" + pos);
            } else {
                continue;
            }
        }
        bf.close();
        return chrPosHash;
    }

    public void catchRsIdFromCoor(String cF) throws IOException, Exception {
        String tmLine;
        //Map<String, String> chrPosHash = new HashMap<String, String>();
        LineReader bf = new CompressedFileReader(new File(cF));
        StringBuilder sb = new StringBuilder();
        while ((tmLine = bf.readLine()) != null) {
            /*
             if (inputRsIdHash.size() == inputCoorHash.size()) {
             break;
             }
             */
            String[] array = Util.tokenize(tmLine, '\t');

            sb.append(array[0]);
            String id = sb.toString();
            sb.delete(0, sb.length());

            sb.append(array[1]);
            String chr = sb.toString();
            sb.delete(0, sb.length());

            sb.append(array[2]);
            String pos0base = sb.toString();
            sb.delete(0, sb.length());
            // pos in dbsnp SNPChrPosOnRef was 0-based, input file was 1-based,So we map pos0base to 1-based
            if (pos0base.equals("")) {
                continue;
            }
            int pos1base = Util.parseInt(pos0base);
            pos1base++;
            if (inputCoorHash.containsKey(chr + ":" + pos1base)) {
                // There are two or more id in same location;
                if (inputCoorHash.get(chr + ":" + pos1base).equals("E")) {
                    inputCoorHash.put(chr + ":" + pos1base, id);
                    inputRsIdHash.put(id, (byte) 1);
                } else {
                    continue;
                }

            } else if (inputCoorHash.containsKey(chr + ":" + pos0base)) { // There are some 0-based position in input file.
                if (inputCoorHash.get(chr + ":" + pos0base).equals("E")) {
                    //There are some snps 1 base next to the other one
                    inputCoorHash.put(chr + ":" + pos0base, id);
                    inputRsIdHash.put(id, (byte) 1);
                } else {
                    continue;
                }
            } else {
                continue;
            }
        }
        bf.close();
        //return chrPosHash;
    }

    public Map<String, String> readMergeBackground(String mF) throws IOException, Exception {
        String tmLine;
        int n = 0;
        Map<String, String> mergeArchHash = new HashMap<String, String>();

        StringBuilder sb = new StringBuilder(); // improve effection to create a string. StringBuilder is more safety than StringBuffer
        LineReader bf = new CompressedFileReader(new File(mF));
        bf.readLine();
        while ((tmLine = bf.readLine()) != null) {
            /*
             if (n == inputRsIdHash.size()) {
             break;
             }
             */

            String[] array = Util.tokenize(tmLine, '\t');
            //to save RAM
            /*
             sb.append(array[0]);
             String highId = sb.toString();
             sb.delete(0, sb.length());
             sb.append(array[1]);
             String lowId = sb.toString();
             sb.delete(0, sb.length());
             sb.append(array[6]);
             String rsCurrent = sb.toString();
             sb.delete(0, sb.length());
             * 
             */

            String highId = array[0];
            String rsCurrent = array[1];
            if (inputRsIdHash.containsKey(highId)) {
                n++;
                if (snpHistoryHash.containsKey(rsCurrent)) {
                    // already deleted
                    mergeArchHash.put(highId, "D");
                    continue;
                }

                mergeArchHash.put(highId, rsCurrent);
                if (inputRsIdHash.containsKey(rsCurrent)) {
                    inputRsIdHash.put(rsCurrent, (byte) 2);
                } else {
                    inputRsIdHash.put(rsCurrent, (byte) 1);
                }
            } else {
                continue;
            }
        }
        bf.close();
        return mergeArchHash;
    }

    private void readSNPHistory(String hF) throws IOException, Exception {
        String tmLine;
        int n = 0;
        StringBuilder sb = new StringBuilder(); // improve effection to create a string. StringBuilder is more safety than StringBuffer
        LineReader bf = new CompressedFileReader(new File(hF));
        while ((tmLine = bf.readLine()) != null) {
            String[] array = Util.tokenize(tmLine, '\t', 4);
            //to save RAM
            // id
            /*
             sb.append(array[0]);
             String rsId = sb.toString();
             sb.delete(0, sb.length());
             // comment--To catch Re-activated record
             sb.append(array[4]);
             String comment = sb.toString();
             sb.delete(0, sb.length());
             if (comment.indexOf("Re-activated") >= 0) {
             continue;
             } else {
             snpHistoryHash.put(rsId, (byte) 1);
             }
             */

            String rsId = array[0];

            // comment--To catch Re-activated record
            String comment = array[4];
            if (comment.indexOf("Re-activated") >= 0) {
                continue;
            } else {
                snpHistoryHash.put(rsId, (byte) 1);
            }
        }
    }

    /**
     * getInputRsId
     *
     * @return Map <String, Integer> list
     * @throws IOException
     */
    private int readInputRsId(String inFilePath, int snpIndex) throws IOException, Exception {

        String tmLine;
        LineReader bf = new CompressedFileReader(new File(inFilePath));
        int n = 0;
        StringBuilder sb = new StringBuilder();
        while ((tmLine = bf.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(tmLine.trim());

            for (int i = 0; i < snpIndex; i++) {
                st.nextToken();
            }
            sb.append(st.nextToken());
            String id = sb.toString().trim();

            //header
            if ((n == 0) && (!id.substring(2).matches("\\d+"))) {
                head = true;
                n++;
                sb.delete(0, sb.length());
                continue;
            } else if (n == 0) {
                head = false;
            }
            n++;
            //omit the "rs" label of rs ID
            if (id.startsWith("rs")) {
                inputRsIdHash.put(id.substring(2), (byte) 1);
            } else {
                inputRsIdHash.put(id, (byte) 1);
            }
            sb.delete(0, sb.length());
        }
        bf.close();
        if (head) {
            n--;
        }
        System.out.println(n + " SNPs are read from " + inFilePath);
        return n;
        //System.out.println(inputRsIdHash.keySet());
    }

    private int readInputCoor(String inFilePath, int chrIndex, int posIndex, String ref, String oldRef, boolean convert) throws IOException, Exception {
        String tmLine;
        int n = 0;
        LineReader bf = new CompressedFileReader(new File(inFilePath));
        StringBuilder sb = new StringBuilder();
        while ((tmLine = bf.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(tmLine.trim());

            for (int i = 0; i < chrIndex; i++) {
                st.nextToken();
            }
            sb.append(st.nextToken());
            String chr = sb.toString().trim();
            sb.delete(0, sb.length());

            StringTokenizer st2 = new StringTokenizer(tmLine.trim());
            for (int i = 0; i < posIndex; i++) {
                st2.nextToken();
            }
            sb.append(st2.nextToken());
            String pos = sb.toString().trim();// position in input file is 1-based
            sb.delete(0, sb.length());

            //header
            if ((n == 0) && !pos.matches("\\d+")) {
                head = true;
                n++;
                sb.delete(0, sb.length());
                continue;
            } else if (n == 0) {
                head = false;
            }

            //liftover coordinates
            if (convert) {
                File CHAIN_FILE = null;
                if (oldRef.equals("hg17")) {
                    CHAIN_FILE = new File(GlobalManager.RESOURCE_PATH + File.separator + "hg17ToHg19.over.chain.gz");
                } else if (oldRef.equals("hg18")) {
                    CHAIN_FILE = new File(GlobalManager.RESOURCE_PATH + File.separator + "hg18ToHg19.over.chain.gz");
                }

                LiftOver liftOver = new LiftOver(CHAIN_FILE);
                int oldpos = Integer.parseInt(pos);
                Interval interval = new Interval("chr" + chr, oldpos, oldpos);
                Interval int2 = liftOver.liftOver(interval);
                if (int2 != null) {
                    oldpos = int2.getStart();
                    pos = Integer.toString(oldpos);
                }
            }

            n++;
            //omit the "chr" label of chromosome
            if (chr.startsWith("c") || chr.startsWith("C")) {
                inputCoorHash.put(chr.substring(3) + ":" + pos, "E");
            } else {
                inputCoorHash.put(chr + ":" + pos, "E");
            }

        }
        bf.close();
        if (head) {
            n--;
        }
        System.out.println(n + " " + ref + " genomic coordinates (1-based) are read from " + inFilePath);
        return n;
        //System.out.println(inputRsIdHash.keySet());    
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) {
        String PVERSION = "1.0";        // 3 chars
        String PREL = " ";               // space or p (full, or prelease)
        String PDATE = "11/Dec/2014"; // 11 chars
        // TODO code application logic here

        long startTime = System.currentTimeMillis();
        String headInfor = "@----------------------------------------------------------@\n" + "|     snpTracker!     |     v" + PVERSION + PREL + "     |   " + PDATE + "    |\n" + "|----------------------------------------------------------|\n" + "|  (C) 2013 Jiaen Deng,  dengje@yahoo.com                  |\n" + "|  (C) 2013 Miaoxin Li,  limx54@yahoo.com                  |\n" + "|----------------------------------------------------------|\n" + "|  For documentation, citation & bug-report instructions:  |\n"
                + "|      http://statgenpro.psychiatry.hku.hk/snpTracker      |\n"
                + "@----------------------------------------------------------@";
        System.out.println(headInfor);
        Options option = new Options();
        SnpTracker snpTracker = new SnpTracker();

        try {
            if (args.length == 0) {
                return;
            } else {
                option.readOptions(args);
                option.parseOptions();

                if (option.inputFileName == null) {
                    System.err.println("*Note: No input file");
                    System.err.println("Usage: java -Xmx1g -jar snptracker.jar input [column] output\n Or:    java -Xmx1g -jar snptracker.jar [options] ...");
                    return;
                }
            }
            boolean noWeb = false;
            if (option.noWeb) {
                noWeb = true;
            } else {
                String info = "To disable web checking, use --no-web";
                System.out.println(info);
            }
            if (!noWeb) {
                if (!NetUtils.isConnected()) {
                    String msg = "Sorry, I cannot connect to website to check the latest version!\n Please check your local network configurations!";
                    System.err.println(msg);
                } else {
                    NetUtils.downloadResource();
                }

            }
            int totalSNP = 0;
            if (option.basic) {
                totalSNP = snpTracker.readInputRsId(option.inputFileName, option.snpIndex);
            } else if (option.bim || option.map) {
                if (option.byid) {
                    totalSNP = snpTracker.readInputRsId(option.inputFileName, option.snpIndex);
                } else if (option.bypos) {
                    totalSNP = snpTracker.readInputCoor(option.inputFileName, option.chr, option.pos, option.refVersion, option.oldRefVer, option.convert);
                }
            } else if (option.coor) {
                totalSNP = snpTracker.readInputCoor(option.inputFileName, option.chr, option.pos, option.refVersion, option.oldRefVer, option.convert);
            }
            Map<String, String> rsMerge = null;
            Map<String, String> chrPos = null;

            //begin--- read Background data   
            String RMF = null;
            String CPF = null;
            String SHF = null;

            if (option.sethist) {
                SHF = option.snpHistoryFile;
            } else {
                SHF = GlobalManager.RESOURCE_PATH + File.separator + option.snpHistoryFile;
            }

            if (option.setmerge) {
                CreateOwnMergeFile cMF = new CreateOwnMergeFile();
                OpenIntIntHashMap rM = cMF.readMergeBackground(option.rsMergeFile);
                cMF.createOwnMergeFile(rM, option.rsMergeFile);
                RMF = option.newRsMergeFile;

            } else {
                CreateOwnMergeFile cMF = new CreateOwnMergeFile();
                OpenIntIntHashMap rM = cMF.readMergeBackground(GlobalManager.RESOURCE_PATH + File.separator + option.rsMergeFile);
                cMF.createOwnMergeFile(rM, GlobalManager.RESOURCE_PATH + File.separator + option.rsMergeFile);
                RMF = GlobalManager.RESOURCE_PATH + File.separator + option.rsMergeFile;
            }

            if (option.setcoor) {
                CPF = option.snpChrPosFile;
            } else {
                CPF = GlobalManager.RESOURCE_PATH + File.separator + option.snpChrPosFile;
            }

            if (option.basic) {
                System.out.println("Reading SNP history....");
                snpTracker.readSNPHistory(SHF);
                System.out.println("Reading merging history of SNPs....");
                rsMerge = snpTracker.readMergeBackground(RMF);
                System.out.println("Reading genomic coordinates of SNPs....");
                chrPos = snpTracker.readChrPosBackground(CPF);
            } else if (option.bim || option.map) {
                if (option.byid) {
                    System.out.println("Reading SNP history....");
                    snpTracker.readSNPHistory(SHF);
                    System.out.println("Reading merging history of SNPs....");
                    rsMerge = snpTracker.readMergeBackground(RMF);
                    System.out.println("Reading genomic coordinates of SNPs....");
                    chrPos = snpTracker.readChrPosBackground(CPF);
                } else if (option.bypos) {
                    System.out.println("Catching RsId from " + option.refVersion + " genomic coordinates of SNPs....");
                    snpTracker.catchRsIdFromCoor(CPF);
                    System.out.println("Reading SNP history....");
                    snpTracker.readSNPHistory(SHF);
                    System.out.println("Reading merging history of SNPs....");
                    rsMerge = snpTracker.readMergeBackground(RMF);
                    System.out.println("Reading genomic coordinates of SNPs....");
                    chrPos = snpTracker.readChrPosBackground(CPF);
                }
            } else if (option.coor) {
                System.out.println("Catching RsId from " + option.refVersion + " genomic coordinates of SNPs....");
                snpTracker.catchRsIdFromCoor(CPF);
                System.out.println("Reading SNP history....");
                snpTracker.readSNPHistory(SHF);
                System.out.println("Reading merging history of SNPs....");
                rsMerge = snpTracker.readMergeBackground(RMF);
                System.out.println("Reading genomic coordinates of SNPs....");
                chrPos = snpTracker.readChrPosBackground(CPF);
            }

            //begin--- rsId conversion
            System.out.println("Trackering SNPs coordinates....");
            if (option.bim || option.map) {
                if (option.bim) {
                    snpTracker.conversionIdByPlinkBim(option.inputFileName, option.resultFileName, option.snpIndex, chrPos, rsMerge, option.refVersion, option.bypos, option.convert, option.oldRefVer);
                } else if (option.map) {
                    snpTracker.conversionIdByPlinkMap(option.inputFileName, option.resultFileName, option.snpIndex, chrPos, rsMerge, option.refVersion, option.bypos, option.convert, option.oldRefVer);
                }
                int err = snpTracker.getErr();
                int delete = snpTracker.getDeleted();
                int invalid = snpTracker.getInvalid();
                int duplicated = snpTracker.getDuplicated();
                int noPos = snpTracker.getNoPos();
                int alt = snpTracker.getAltOnly();
                int unchr = snpTracker.getUnChr();
                int mt = snpTracker.getMt();
                int multi = snpTracker.getMulti();
                int notOn = snpTracker.getNotOn();
                int par = snpTracker.getPar();
                StringBuilder sb = new StringBuilder();
                sb.append((totalSNP - err - duplicated) + " SNPs successfully tracked and mapped are saved in " + option.resultFileName + " \n");
                sb.append(delete + " SNPs were deleted in dbSNP (Coordinate: -1) \n");
                sb.append(invalid + " SNPs were invalid id in dbSNP (Coordinate: -1.1) \n");
                sb.append(duplicated + " SNPs were duplicated in input file (Coordinate: -2) \n");
                sb.append(noPos + " SNPs had no position in dbSNP (Coordinate: -3)\n");
                sb.append(alt + " SNPs are AltOnly in dbSNP (Coordinate: -4)\n");
                sb.append(unchr + " SNPs are UnChr in dbSNP (Coordinate: -5)\n");
                //sb.append(mt + " SNPs are MT in dbSNP (Coordinate: -6)\n");
                sb.append(multi + " SNPs are MultiChromosome in dbSNP (Coordinate: -7)\n");
                sb.append(notOn + " SNPs are NotOn in dbSNP (Coordinate: -8)\n");
                //sb.append(par + " SNPs are PAR in dbSNP (Coordinate: -9)\n");

                System.out.println(sb.toString());

            } else if (option.basic) {
                snpTracker.conversionId(option.inputFileName, option.resultFileName, option.errorFileName, option.snpIndex, chrPos, rsMerge, option.refVersion);
                // print error information
                int deleted = snpTracker.getErr();
                StringBuilder sb = new StringBuilder();
                sb.append((totalSNP - deleted) + " SNPs successfully tracked and mapped are saved in " + option.resultFileName + " \n");
                sb.append(deleted + " SNPs were failed to map in dbSNP, which are saved in " + option.errorFileName + " \n");
                //sb.append(duplicated + " SNPs duplicated; " + unchr + " SNPs in Un chromosome\n");

                System.out.println(sb.toString());
            } else if (option.coor) {
                snpTracker.conversionIdByCoor(option.inputFileName, option.resultFileName, option.errorFileName, chrPos, rsMerge, option.chr, option.pos, option.oldRefVer, option.convert);
                int deleted = snpTracker.getErr();
                StringBuilder sb = new StringBuilder();
                sb.append((totalSNP - deleted) + " SNPs successfully tracked and mapped are saved in " + option.resultFileName + " \n");
                sb.append(deleted + " SNPs were failed to map in dbSNP, which are saved in " + option.errorFileName + " \n");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("The time used is " + (System.currentTimeMillis() - startTime) / 1000 + " seconds.");
    }
}
