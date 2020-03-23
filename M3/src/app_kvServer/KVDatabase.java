package app_kvServer;

import org.apache.log4j.Logger;
import shared.hashring.Hash;
import shared.hashring.HashComparator;

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

public class KVDatabase {
    private static final String PROJECT_PATH = System.getProperty("user.home");
    private static final String INDEX_FILE_PATH = ".txt";
    private static final String DATABASE_FILE_PATH = ".db";
    private static Logger logger = Logger.getRootLogger();

    private final String indexFile;
    private final String databaseFile;
    private int port;
    // byte for valid, 4 bytes per lengths (ints)
    private Integer entryLength = 1 + 4 + 4 + 20 + 128000;
    private HashMap<String, Integer> index;
    private HashComparator hc;

    public static String parseIndexFilePath(String port) {
        return PROJECT_PATH + "/" + port + INDEX_FILE_PATH;
    }

    public static String parseDatabaseFilePath(String port) {
        return PROJECT_PATH + "/" + port + DATABASE_FILE_PATH;
    }

    public KVDatabase(int port) {
        this.port = port;
        this.indexFile = parseIndexFilePath(Integer.toString(this.port));
        this.databaseFile = parseDatabaseFilePath(Integer.toString(this.port));

        initDB();
        index = loadIndex();
        hc = new HashComparator();
    }

    public void initDB() {
        File database = new File(this.databaseFile);
        if (!database.exists()) {
            try {
                database.createNewFile();
            } catch (IOException e) {
                this.logger.error("Error! Couldn't create new db");
            }
            this.logger.info("Created new db file");
        }
    }

    public HashMap<String, Integer> loadIndex() {
        File tmp = new File(this.indexFile);
        if (!tmp.exists())
            // don't have to make new index file here because it is created in saveIndex
            return new HashMap<String, Integer>();

        HashMap<String, Integer> map = null;
        try {
            FileInputStream fileIn = new FileInputStream(this.indexFile);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);
            map = (HashMap<String, Integer>) objectIn.readObject();
            fileIn.close();
            objectIn.close();
            return map;
        } catch (Exception e) {
            this.logger.error(e);
            e.printStackTrace();
            return null;
        }
    }

    public boolean inStorage(String key) {
        return index.containsKey(key);
    }

    public String get(String key) throws IOException {
        Integer start = index.get(key);
        if (start != null) {
            String value = getValue(start);
            return value;
        }
        return null;
    }

    public String getValue(Integer start) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(this.databaseFile, "r");
        raf.seek(start);
        boolean valid = raf.readBoolean();
        if (!valid) {
            return null;
        }

        raf.seek(start + 1 + 4);
        int valueSize = raf.readInt();

        raf.seek(start + 1 + 8 + 20);
        byte[] value = new byte[valueSize];
        raf.readFully(value);
        raf.close();
        return new String(value);
    }

    private void writeKV(Integer start, String key, String value) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(this.databaseFile, "rw");
        long indexLoc = 0;
        if (start == -1) {
            // new entry
            indexLoc = raf.length();
        } else {
            indexLoc = start.longValue();
        }

        raf.seek(indexLoc);

        if (value == "null") {
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

        raf.close();

        index.put(key, (int) indexLoc);
    }

    public void put(String key, String value) {

        Integer start = index.get(key);
        if (start != null) {
            // update existing
            try {
                writeKV(start, key, value);
            } catch (IOException e) {
                this.logger.error("Error! Write to disk");
            }
        } else {
            // new entry
            try {
                // can also start from beginning of file and loop through until find invalid
                // block
                writeKV(-1, key, value);
            } catch (IOException e) {
                this.logger.error("Error! Write to disk");
            }
        }

        saveIndex();
    }

    public void delete(String key) {
        index.remove(key);
        saveIndex();
    }

    public boolean saveIndex() {
        try {
            FileOutputStream fileOut = new FileOutputStream(this.indexFile, false);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(index);
            objectOut.close();
            fileOut.close();
            this.logger.info("Index saved");
        } catch (IOException e) {
            this.logger.error("Error! Saving index");
        }
        return false;
    }

    public void clear() {
        if (this.index != null)
            this.index.clear();

        File file = new File(this.indexFile);
        file.delete();

        file = new File(this.databaseFile);
        file.delete();

        initDB();
        this.index = loadIndex();
    }

    public HashMap<String, String> moveData(String[] range) {
        String start = range[0];
        String end = range[1];
        String end_limit = "ffffffffffffffffffffffffffffffff";
        String start_limit = "0";

        HashMap<String, String> movingData = new HashMap<String, String>();

        if (hc.compare(start, end) > 0){
            for (Map.Entry<String, Integer> entry : index.entrySet()) {
                String key = entry.getKey();
                String keyHash = Hash.MD5(key);
                if ((hc.compare(keyHash, start) >= 0 && hc.compare(keyHash, end_limit) <= 0)
                    || (hc.compare(keyHash, start_limit) >= 0 && hc.compare(keyHash, end) <= 0)) {
                    Integer startIndex = entry.getValue();
                    // find in actual db file
                    String value = "";
                    try {
                        value = getValue(startIndex);
                    } catch (IOException e) {
                        this.logger.error(e);
                        e.printStackTrace();
                    }
                    movingData.put(key, value);
                }
            }

        } else {
            for (Map.Entry<String, Integer> entry : index.entrySet()) {
                String key = entry.getKey();
                String keyHash = Hash.MD5(key);
                if (hc.compare(keyHash, start) >= 0 && hc.compare(keyHash, end) <= 0) {
                    Integer startIndex = entry.getValue();
                    // find in actual db file
                    String value = "";
                    try {
                        value = getValue(startIndex);
                    } catch (IOException e) {
                        this.logger.error(e);
                        e.printStackTrace();
                    }
                    movingData.put(key, value);
                }
            }
        }
        System.out.println(movingData);
        this.logger.error("Move Data" + movingData.size());
        return movingData;
    }

    public void deleteMovedData(HashMap<String, String> movedData) {
        for (Map.Entry<String, String> entry : movedData.entrySet()) {
            // don't really need to delete from db, as long as index is deleted
            index.remove(entry.getKey());
        }
        saveIndex();
    }

    public void receiveData(HashMap<String, String> newData) {

        for (Map.Entry<String, String> entry : newData.entrySet()) {
            try {
                writeKV(-1, entry.getKey(), entry.getValue());
            } catch (IOException e) {
                this.logger.error(e);
                e.printStackTrace();
            }
        }
        saveIndex();
    }

}
