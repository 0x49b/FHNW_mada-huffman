package ch.fhnw.mada.huffman;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Huffman Compression and Decpompression, Assignment 2, mada FS21, A.Vogt
 */
public class Huffman {

    private static final String     FILENAME_DAT_FILE        = "output.dat";
    private static final String     FILENAME_DECRYPTION_FILE = "dec_tab.txt";
    private static final String     FILENAME_DECOMPRESS      = "decompress.txt";
    private static final List<Leaf> codeLeaves               = new ArrayList<>();
    private static       List<Leaf> decodedLeaf              = new ArrayList<>();
    private static final String     CLI_ENC                  = "enc";
    private static final String     CLI_DEC                  = "dec";

    /**
     * In main ist just some logic to make cli work, nothing special
     *
     * @param args
     */
    public static void main(String[] args) {

        if (args.length > 0) {

            String type = args[0];

            if (type.equals(CLI_DEC) || type.equals(CLI_ENC)) {


                // Decryption
                if (type.equals(CLI_DEC)) {
                    if (args.length == 3) {
                        String cryptFile = args[1];
                        String datFile   = args[2];

                        if (checkExtension("txt", cryptFile) || checkExtension("dat", datFile)) {
                            decode(args[1], args[2]);
                        } else {
                            System.err.println("Please check order & type of supplied files");
                            printHelp();
                        }
                    } else {
                        printHelp();
                    }
                }

                // Encryption
                if (type.equals(CLI_ENC)) {

                    if (args.length == 2) {

                        String clearFile = args[1];
                        if (checkExtension("txt", clearFile)) {
                            encode(clearFile);
                        } else {
                            System.err.println("Please check type of supplied file");
                            printHelp();
                        }

                    } else {
                        printHelp();
                    }
                }
            } else {
                printHelp();
            }
        } else {
            printHelp();
        }
    }


    /**
     * Method to encode a clear file to Huffman Codes and save the code table as .txt and the data to the a .dat file
     *
     * @param clearFile
     */
    public static void encode(String clearFile) {
        // ENCODE
        System.out.println("**********************************************************************");
        System.out.println("Starting ENCRYPTION");
        System.out.println("**********************************************************************\n");


        try {
            String pathForOutFile = getFilePath(clearFile);
            String fileContent    = readTextFromFile(clearFile);

            List<Leaf> characterList = createCharacterList(fileContent);

            createHuffmanCodeTable(characterList);
            String encryptionKey = createEncryptionKey();
            writeDecriptionKeyToFile(encryptionKey, pathForOutFile);

            try {
                String huffmanCodeString          = convertTextToHuffmanCode(fileContent);
                String postfixedHuffmanCodeString = addPostfix(huffmanCodeString);
                byte[] byteArray                  = createByteArray(postfixedHuffmanCodeString);
                writeByteArrayToFile(byteArray, pathForOutFile + FILENAME_DAT_FILE);

                System.out.println("Finished Encryption of file clearFile. Write output to " + pathForOutFile + FILENAME_DAT_FILE);

            } catch (Exception e) {
                e.printStackTrace();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to decode an encoded .dat file with a .txt key file
     *
     * @param cryptFile
     * @param datFile
     */
    public static void decode(String cryptFile, String datFile) {
        //DECODE
        System.out.println("**********************************************************************");
        System.out.println("Starting DECRYPTION");
        System.out.println("**********************************************************************\n");
        try {
            String pathForOutFile    = getFilePath(cryptFile);
            String readDecryptionKey = readTextFromFile(cryptFile);
            System.out.println("Decryption Key from File is: " + readDecryptionKey);
            decodedLeaf = createCodeLeavesForDecryptionKey(readDecryptionKey);

            byte[] datFileContent          = readByteArrayFromFile(datFile);
            String bitstring               = getBitString(datFileContent);
            String bitstringWithoutPostfix = cutOffPostfix(bitstring);
            try {
                String cleartext = decodeBitstringToText(bitstringWithoutPostfix);

                if (cleartext.length() > 500) {
                    System.out.println("Cleartext is: \n" + cleartext.substring(0, 100) + "... [truncated]");
                } else {
                    System.out.println("Cleartext is: \n" + cleartext);
                }

                System.out.println(" \nwrite to file " + pathForOutFile + FILENAME_DECOMPRESS);


                writeByteArrayToFile(cleartext.getBytes(), pathForOutFile + FILENAME_DECOMPRESS);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Decode the string from the .dat file {0,1} to a human readable text
     *
     * @param bitstring
     * @return
     * @throws Exception
     */
    public static String decodeBitstringToText(String bitstring) throws Exception {
        StringBuilder sb  = new StringBuilder();
        int           pos = 0;

        StringBuilder filter = new StringBuilder();
        for (char c : bitstring.toCharArray()) {
            filter.append(c);
            for (Leaf l : decodedLeaf) {
                if (l.getCode().equals(filter.toString())) {
                    sb.append(l.getData());
                    filter.delete(0, filter.length());
                }
            }
        }
        return sb.toString();
    }

    /**
     * Remove the postfix from the bitstring. postfix is 1 and some 0 until string.length % 8 = 0
     *
     * @param bitstring
     * @return
     */
    public static String cutOffPostfix(String bitstring) {
        return bitstring.substring(0, bitstring.lastIndexOf("1"));
    }


    /**
     * Covert the byte array from the .dat file to a bitstring of {0,1}
     * neede help from https://stackoverflow.com/questions/12310017/how-to-convert-a-byte-to-its-binary-string-representation (answer from Raekye)
     *
     * @param b
     * @return
     */
    public static String getBitString(byte[] b) {
        StringBuilder bitStringBuilder = new StringBuilder();
        for (byte s : b) {
            bitStringBuilder.append(Integer.toBinaryString((s & 0xFF) + 0x100).substring(1));
        }
        return bitStringBuilder.toString();
    }


    /**
     * Read the key file and create data structure of Leaf Nodes eg. Huffman table
     *
     * @param decryptionKey
     * @return
     */
    public static List<Leaf> createCodeLeavesForDecryptionKey(String decryptionKey) {
        List<Leaf> codeLeaves = new ArrayList<>();
        String[]   decArr     = decryptionKey.split("-");

        for (String s : decArr) {
            String[] leafData = s.split(":");
            int      charcode = Integer.parseInt(leafData[0]);
            codeLeaves.add(new Leaf((char) charcode, leafData[1]));
        }
        return codeLeaves;
    }


    /**
     * Create the actual Huffman Table from the leaves
     *
     * @param characterList
     */
    public static void createHuffmanCodeTable(List<Leaf> characterList) {
        List<Leaf> tree = characterList; //needed because of concurrent exception

        while (tree.size() > 1) {
            List<Leaf> subtree = characterList.stream().sorted(Comparator.comparingInt(Leaf::getFreq)).collect(Collectors.toList());
            Leaf       left    = subtree.get(0);
            Leaf       right   = subtree.get(1);
            tree.add(new Leaf(left.getFreq() + right.getFreq(), left, right));
            tree.remove(left);
            tree.remove(right);
        }
        createHuffmanCodeForLeaf(tree.get(0), "");
    }

    /**
     * Create the Code representation for a leaf in the Huffman Tree
     *
     * @param leaf
     * @param codeString
     */
    public static void createHuffmanCodeForLeaf(Leaf leaf, String codeString) {
        if (leaf.isLast()) {
            codeLeaves.add(new Leaf(leaf.getData(), codeString));
            return;
        }
        createHuffmanCodeForLeaf(leaf.getLeft(), codeString + "0");
        createHuffmanCodeForLeaf(leaf.getRight(), codeString + "1");
    }


    /**
     * Create the LIst of Characters an their frequencies to later create the Huffman tree
     *
     * @param content
     * @return
     */
    public static List<Leaf> createCharacterList(String content) {
        Map<Character, Integer> cMap  = new HashMap<>();
        List<Leaf>              cList = new ArrayList<>();

        for (char c : content.toCharArray()) {
            if (cMap.containsKey(c)) {
                int cm = cMap.get(c);
                cm++;
                cMap.put(c, cm);
            } else {
                cMap.put(c, 1);
            }
        }

        for (Character c : cMap.keySet()) {
            cList.add(new Leaf(c, cMap.get(c)));
        }
        return cList.stream().sorted(Comparator.comparingInt(Leaf::getFreq).reversed()).collect(Collectors.toList());
    }

    /**
     * Read the content from a .txt file
     *
     * @param filePath
     * @return
     * @throws FileNotFoundException
     */
    public static String readTextFromFile(String filePath) throws FileNotFoundException {

        File          file         = new File(filePath);
        Scanner       scanner      = new Scanner(file);
        StringBuilder returnString = new StringBuilder();

        while (scanner.hasNextLine()) { returnString.append(scanner.nextLine()); }
        return returnString.toString();
    }

    /**
     * Write the decryption key to a file
     *
     * @param decriptionKey
     * @param filepath
     */
    public static void writeDecriptionKeyToFile(String decriptionKey, String filepath) {
        try {
            writeByteArrayToFile(decriptionKey.getBytes(StandardCharsets.UTF_8), filepath + FILENAME_DECRYPTION_FILE);
            System.out.println("Write decryption key to file " + filepath + FILENAME_DECRYPTION_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write a byte array of any data to a file
     *
     * @param out
     * @param filename
     * @throws IOException
     */
    public static void writeByteArrayToFile(byte[] out, String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        fos.write(out);
        fos.close();
    }

    /**
     * read a byte array from any file to a byte[]
     *
     * @param filename
     * @return
     * @throws IOException
     */
    public static byte[] readByteArrayFromFile(String filename) throws IOException {
        File            file  = new File(filename);
        byte[]          bFile = new byte[(int) file.length()];
        FileInputStream fis   = new FileInputStream(file);
        fis.read(bFile);
        fis.close();
        return bFile;
    }

    /**
     * Do the actual compression from ASCII Chars to the Code for a leaf
     *
     * @param content
     * @return
     * @throws Exception
     */
    public static String convertTextToHuffmanCode(String content) throws Exception {

        if (codeLeaves.isEmpty()) {
            throw new Exception("CodeLeaves are Empty. Please initialize it first");
        }
        StringBuilder huffmanCodeSB = new StringBuilder();
        for (Character c : content.toCharArray()) {
            huffmanCodeSB.append(getCodeForCharacter(c));
        }
        return huffmanCodeSB.toString();
    }

    /**
     * Extract the code for a leaf for a Character c
     *
     * @param c
     * @return
     */
    public static String getCodeForCharacter(Character c) {
        return codeLeaves.stream().filter(l -> l.getData() == c).findFirst().orElseThrow(IllegalArgumentException::new).getCode();
    }

    /**
     * add the postfix to the generated code string. The Postfix is a 1 and several 0 until content.length % 8 = 0
     *
     * @param content
     * @return
     */
    public static String addPostfix(String content) {

        StringBuilder sb = new StringBuilder();
        sb.append(content);
        sb.append(1);

        while (sb.toString().length() % 8 != 0) {
            sb.append(0);
        }
        return sb.toString();

    }

    /**
     * Create the Byte array for all bytes of length 8, to be later written to the .dat file
     * https://www.javatpoint.com/java-integer-parseint-method
     *
     * @param content
     * @return
     */
    public static byte[] createByteArray(String content) {
        int    numBytes = content.length() / 8;
        byte[] retArr   = new byte[numBytes];
        for (int i = 0; i < content.length() / 8; i++) {
            retArr[i] = (byte) Integer.parseInt(content.substring(i * 8, (i + 1) * 8), 2);
        }
        return retArr;
    }

    /**
     * Create the Key for the Huffman Table, later ist that saved to a .txt file
     *
     * @return
     */
    public static String createEncryptionKey() {
        StringBuilder decSB = new StringBuilder();
        codeLeaves.forEach(leaf -> {
            // Key Format <Char Code as Int 10>: <HuffmanCode as ByteString 1010111>-
            decSB.append((int) leaf.getData());
            decSB.append(":");
            decSB.append(leaf.getCode());

            if (leaf != codeLeaves.get(codeLeaves.size() - 1)) {
                decSB.append("-");
            }
        });
        return decSB.toString();
    }

    /**
     * Helper Methods
     */

    /**
     * Get the path for a file, needed to save to output where the input came from
     *
     * @param filepath
     * @return directory of origin
     */
    public static String getFilePath(String filepath) {
        return filepath.substring(0, filepath.lastIndexOf(File.separator) + 1);
    }

    /**
     * Check if the extensions are correct and as needed
     *
     * @param extension
     * @param filepath
     * @return
     */
    public static boolean checkExtension(String extension, String filepath) {

        String ext = filepath.substring(filepath.lastIndexOf(".") + 1);
        if (ext.equals(extension)) {
            return true;
        }
        return false;
    }

    /**
     * Print the cli help
     */
    public static void printHelp() {
        System.out.println("**********************************************************************");
        System.out.println("Huffman Compression & Decompression");
        System.out.println("**********************************************************************\n");
        System.out.println("Usage:");
        System.out.println("    Encryption");
        System.out.println("      enc <filepath to cleartextfile>");
        System.out.println();
        System.out.println("    Decryption");
        System.out.println("      dec <filepath to dat file> <filepath to keyfile>");
        System.out.println("\n**********************************************************************\n");
    }
}

