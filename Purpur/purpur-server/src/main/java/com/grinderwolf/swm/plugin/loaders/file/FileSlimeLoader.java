package com.grinderwolf.swm.plugin.loaders.file;

import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.log.Logging;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.NotDirectoryException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FileSlimeLoader implementation for loading SLF world files from disk.
 */
public class FileSlimeLoader implements SlimeLoader {

    private static final String SLIME_EXTENSION = ".slime";

    private final File worldDirectory;
    private final Map<String, RandomAccessFile> openFiles = new HashMap<>();

    /**
     * Creates a new FileSlimeLoader.
     *
     * @param worldDirectory the directory containing world files
     */
    public FileSlimeLoader(File worldDirectory) {
        this.worldDirectory = worldDirectory;

        if (worldDirectory.exists() && !worldDirectory.isDirectory()) {
            Logging.warning("Found world file named '" + worldDirectory.getName()
                    + "' instead of directory. Deleting.");
            worldDirectory.delete();
        }

        if (!worldDirectory.exists()) {
            worldDirectory.mkdirs();
        }
    }

    @Override
    public byte[] loadWorld(String worldName, boolean readOnly)
            throws UnknownWorldException, IOException, WorldInUseException {
        
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        File file = new File(worldDirectory, worldName + SLIME_EXTENSION);
        RandomAccessFile raf = openFiles.computeIfAbsent(worldName, name -> {
            try {
                return new RandomAccessFile(file, readOnly ? "r" : "rw");
            } catch (FileNotFoundException e) {
                return null;
            }
        });

        if (raf == null) {
            throw new UnknownWorldException(worldName);
        }

        if (!readOnly) {
            tryLock(worldName);
        }

        byte[] data = new byte[(int) raf.length()];
        raf.seek(0);
        raf.readFully(data);

        return data;
    }

    @Override
    public boolean worldExists(String worldName) {
        File file = new File(worldDirectory, worldName + SLIME_EXTENSION);
        return file.exists() && file.isFile();
    }

    @Override
    public List<String> listAll() throws IOException {
        File[] files = worldDirectory.listFiles();
        if (files == null) {
            throw new NotDirectoryException(worldDirectory.getAbsolutePath());
        }

        return Arrays.stream(files)
                .filter(f -> f.getName().endsWith(SLIME_EXTENSION))
                .map(f -> f.getName().substring(0, f.getName().length() - SLIME_EXTENSION.length()))
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public void saveWorld(String worldName, byte[] data, boolean lock)
            throws IOException {
        
        File file = new File(worldDirectory, worldName + SLIME_EXTENSION);
        
        if (lock) {
            tryLock(worldName);
        }

        RandomAccessFile raf = openFiles.get(worldName);
        if (raf == null) {
            raf = new RandomAccessFile(file, "rw");
        }

        raf.seek(0);
        raf.setLength(0);
        raf.write(data);
        raf.close();
        openFiles.remove(worldName);
    }

    @Override
    public void unlockWorld(String worldName) throws UnknownWorldException, IOException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        RandomAccessFile raf = openFiles.remove(worldName);
        if (raf != null) {
            try {
                raf.close();
            } catch (IOException e) {
                Logging.warning("Failed to close file for world: " + worldName, e);
            }
        }
    }

    @Override
    public boolean isWorldLocked(String worldName) throws UnknownWorldException, IOException {
        File file = new File(worldDirectory, worldName + SLIME_EXTENSION);
        if (!file.exists()) {
            throw new UnknownWorldException(worldName);
        }

        try (FileChannel channel = new FileInputStream(file).getChannel()) {
            try (FileLock lock = channel.tryLock()) {
                return lock != null;
            } catch (OverlappingFileLockException e) {
                return true;
            }
        }
    }

    @Override
    public void deleteWorld(String worldName) throws UnknownWorldException, IOException {
        File file = new File(worldDirectory, worldName + SLIME_EXTENSION);
        if (!file.exists()) {
            throw new UnknownWorldException(worldName);
        }

        if (openFiles.containsKey(worldName)) {
            unlockWorld(worldName);
        }

        file.delete();
    }

    /**
     * Tries to acquire a lock on the world file.
     *
     * @param worldName the world name
     * @throws WorldInUseException if the file is already locked
     */
    private void tryLock(String worldName) throws WorldInUseException {
        File file = new File(worldDirectory, worldName + SLIME_EXTENSION);
        
        try (FileChannel channel = new RandomAccessFile(file, "rw").getChannel()) {
            if (channel.tryLock() == null) {
                throw new WorldInUseException(worldName);
            }
        } catch (OverlappingFileLockException e) {
            throw new WorldInUseException(worldName);
        } catch (IOException e) {
            Logging.warning("Failed to lock file for world: " + worldName, e);
            throw e;
        }
    }
}
