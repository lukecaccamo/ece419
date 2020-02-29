package app_kvServer;

import org.apache.log4j.Logger;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// serialize each KV entry
// use randomaccessfile.seek() to find starting point
// starting point stored in another map Map<String, long> key, startingPoint
// in the actual disk storage
// 1 byte for valid
// 4 bytes for key length
// 4 bytes for value length
//
public class KVDatabase {

    private static Logger logger = Logger.getRootLogger();

    private String indexFile = "index.txt";
    private String databaseFile = "databaseFile.db";
    private int port;
    //byte for valid, 4 bytes per lengths (ints)
    private Integer entryLength = 1 + 4 + 4 + 20 + 128000;
    private HashMap<String, Integer> index;

    public KVDatabase(int port) {
        // Map<String, Long> index = new HashMap<String, long>();
        this.port = port;
        initDB();
        index = loadIndex();
    }

    public void initDB(){
        File database = new File(this.port + databaseFile);
        if (!database.exists()){
            try {
                database.createNewFile();
            } catch (IOException e) {
                logger.error("Error! Couldn't create new db");
            }
            logger.info("Created new db file");
        }
    }

    public HashMap<String, Integer> loadIndex(){
        File tmp = new File(indexFile);
        if (!tmp.exists()){
            // don't have to make new index file here because it is created in saveIndex
            return new HashMap<String, Integer>();
        }

        HashMap<String, Integer> map = null;
        try {
            FileInputStream fileIn = new FileInputStream(indexFile);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);
            map = (HashMap<String, Integer>) objectIn.readObject();
            fileIn.close();
            objectIn.close();
            //System.out.println("Index loaded");
            //System.out.println(map);
            return map;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public boolean inStorage(String key){
        return index.containsKey(key);
    }


    public String get(String key) throws IOException {
        Integer start = index.get(key);
        //System.out.println(start);
        if (start != null){
            String value = getValue(start);
            return value;
        }
        return null;
    }

    public String getValue(Integer start) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(databaseFile, "r");
        raf.seek(start);
        boolean valid = raf.readBoolean();
        if (!valid) {return null;}

        raf.seek(start + 1 + 4);
        int valueSize = raf.readInt();

        raf.seek(start + 1 + 8 + 20);
        byte[] value = new byte[valueSize];
        raf.readFully(value);
        raf.close();
        return new String(value);
    }

    private void writeKV(Integer start, String key, String value) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(databaseFile, "rw");
        long indexLoc = 0;
        if (start == -1) {
            // new entry
            indexLoc = raf.length();
        } else {
            indexLoc = start.longValue();
        }

        raf.seek(indexLoc);

        if (value == "null"){
            raf.writeBoolean(false);
            raf.close();

            index.remove(key);
            return;
        }

        raf.writeBoolean(true);
        raf.writeInt(key.length());
        raf.writeInt(value.length());
        raf.writeBytes(key);
        raf.seek(indexLoc + entryLength - 128000);
        raf.writeBytes(value);


        //System.out.println(raf.length());
        raf.close();

        index.put(key, (int) indexLoc);
    }

    public void put(String key, String value){

        Integer start = index.get(key);
        if (start != null){
            // update existing
            try {
                writeKV(start, key, value);
            } catch (IOException e) {
                logger.error("Error! Write to disk");
            }
        } else {
            //new entry
            try {
                //can also start from beginning of file and loop through until find invalid block
                writeKV(-1, key, value);
            } catch (IOException e) {
                logger.error("Error! Write to disk");
            }
        }

        saveIndex();
    }


    public boolean saveIndex() {
        try {

            //System.out.println(index);
            FileOutputStream fileOut = new FileOutputStream(indexFile,false);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(index);
            objectOut.close();
            fileOut.close();
            logger.info("Index saved");
            //System.out.println("Index saved");
        } catch (IOException i) {
            logger.error("Error! Saving index");

        }
        return false;
    }

    public void clear() {
        if (index!=null){
            index.clear();
        }

        File file = new File(databaseFile);
        file.delete();

        file = new File(indexFile);
        file.delete();

        initDB();
        index = loadIndex();

    }

}
