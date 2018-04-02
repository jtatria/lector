/* 
 * Copyright (C) 2017 José Tomás Atria <jtatria at gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.columbia.incite.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility static methods for common file system operations.
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public abstract class FileUtils {

    public static final char EXT = '.';
    public static final String EXT_STR = new String( new char[]{ EXT } );
    public static final char DIR = File.separatorChar;

    /**
     * Get a list of {@link File files} pointing to files in the directory pointed at by the given
     * {@link File}.
     *
     * @param inputDir A file pointing to a directory.
     *
     * @return A (possibly empty) list containing files from the directory pointed at by the given
     *         file.
     */
    public static List<File> listFiles( File inputDir ) {
        return FileUtils.listFiles( inputDir, false );
    }

    /**
     * Get a list of {@link File files} pointing to files in the directory pointed at by the given
     * {@link File}.
     * This method will look for files recursively as requested.
     * This method does not fail and will return an empty list if the directory does not exist, can
     * not be read, or contains no files.
     *
     * @param inputDir  A file pointing to a directory.
     * @param recursive If {@code true}, include files from subdirectories.
     *
     * @return A (possibly empty) list containing files from the directory pointed at by the given
     *         file.
     */
    public static List<File> listFiles( File inputDir, boolean recursive ) {
        return listFiles( inputDir, recursive, null );
    }

    /**
     * Get a list of {@link File files} pointing to files in the directory pointed at by the given
     * {@link File}.
     * This method will look for files with names ending in the given extension, and recursively as
     * requested.
     * This method does not fail and will return an empty list if the directory does not exist, can
     * not be read, or contains no files.
     *
     * @param inputDir  A file pointing to a directory.
     * @param recursive If {@code true}, include files from subdirectories.
     * @param ext       If not {@code null}, only include files with names ending in the given
     *                  extension.
     *
     * @return A (possibly empty) list containing files from the directory pointed at by the given
     *         file.
     */
    public static List<File> listFiles( File inputDir, boolean recursive, String ext ) {
        List<File> files = new ArrayList<>();
        File[] entries = inputDir.listFiles();

        Arrays.sort( entries );
        ext = ext != null ? ext : "";
        for ( File entry : entries ) {
            if ( entry.isFile() && entry.getName().toLowerCase().endsWith( ext ) ) {
                files.add( entry );
            } else if ( entry.isDirectory() && recursive ) {
                files.addAll(listFiles( entry, recursive, ext ) );
            }
        }
        return files;
    }

    /**
     * Get a list of {@link Path paths} pointing to all files in the given directory.
     *
     * @param inputDir A {@link Path} to a directory.
     *
     * @return A list containing files from the directory pointed at by the given path.
     *
     * @throws IOException If the given directory can not be read.
     */
    public static List<Path> listPaths( Path inputDir ) throws IOException {
        return listPaths( inputDir, null, false );
    }

    /**
     * Get a list of {@link Path paths} pointing to files in the given directory.
     * This method will look for files in the given directory corresponding to the given glob, if
     * not null.
     *
     * @param inputDir A {@link Path} to a directory.
     * @param glob     A file system glob.
     *
     * @return A list containing files from the directory pointed at by the given path.
     *
     * @throws IOException If the given directory can not be read.
     */
    public static List<Path> listPaths( Path inputDir, String glob ) throws IOException {
        return listPaths( inputDir, glob, false );
    }

    /**
     * Get a list of {@link Path paths} pointing to files in the given directory.
     * This method will look for files in the given directory corresponding to the given glob, if
     * not null, and recursively as requested.
     *
     * @param inputDir  A {@link Path} to a directory.
     * @param glob      A file system glob.
     * @param recursive If {@code true}, add files from subdirectories to the returned list.
     *
     * @return A list containing files from the directory pointed at by the given path, and
     *         optionally all subdirectories.
     *
     * @throws IOException If the given directory (or any of its subdirectories) can not be read.
     */
    public static List<Path> listPaths( Path inputDir, String glob, boolean recursive ) throws IOException {
        List<Path> paths = new ArrayList<>();

        glob = glob == null ? "*" : glob;

        DirectoryStream<Path> inDir = Files.newDirectoryStream( inputDir, glob );
        for ( Path entry : inDir ) {
            if ( !Files.isDirectory( entry ) ) {
                paths.add( entry );
            } else if ( recursive ) {
                paths.addAll( listPaths( entry, glob, recursive ) );
            }
        }

        Collections.sort( paths );
        return paths;
    }
    
    /**
     * Obtain a {@link Writer} to a file with the given name in a directory with the given name.
     * This method will attempt to create missing directories and avoid overwriting existing files
     * as requested.
     *
     * @param d    A directory name.
     * @param f    A file name.
     *
     * @return A new buffered text writer.
     *
     * @throws IOException If directories or files can not be created or written to.
     */
    public static Writer getWriter( String d, String f ) throws IOException {
        return getWriter( d, f, false, false, StandardCharsets.UTF_8 );
    }
    
    public static Writer getWriter( Path p ) throws IOException {
        return getWriter( p, true, true, StandardCharsets.UTF_8 );
    }
    
    public static Writer getWriter( Path p, boolean mkd, boolean ow ) throws IOException {
        return getWriter( p, mkd, ow, StandardCharsets.UTF_8 );
    }
    
    public static Writer getWriter( Path p, boolean mkd, boolean ow, Charset cs ) throws IOException {
        Path path = getFilePath( p.getParent(), p, mkd, ow );
        return Files.newBufferedWriter( path, cs,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    /**
     * Obtain a {@link Writer} to a file with the given name in a directory with the given name.
     * This method will attempt to create missing directories and avoid overwriting existing files
     * as requested.
     *
     * @param d    A directory name.
     * @param f    A file name.
     * @param mkd  If {@code true}, attempt to create missing directories.
     * @param ow   If {@code false}, add a suffix to the given file name to prevent overwriting.
     *
     * @return A new buffered text writer.
     *
     * @throws IOException If directories or files can not be created or written to.
     */
    public static Writer getWriter( String d, String f, boolean mkd, boolean ow ) throws IOException {
        return getWriter( d, f, mkd, ow, StandardCharsets.UTF_8 );
    }

    /**
     * Obtain a {@link Writer} to a file with the given name in a directory with the given name.
     * This method will attempt to create missing directories and avoid overwriting existing files
     * as requested.
     *
     * @param d    A directory name.
     * @param f    A file name.
     * @param cs   A {@link Charset} for string encoding.
     *
     * @return A new buffered text writer.
     *
     * @throws IOException If directories or files can not be created or written to.
     */
    public static Writer getWriter( String d, String f, Charset cs ) throws IOException {
        return getWriter( d, f, false, false, cs );
    }

    /**
     * Obtain a {@link Writer} to a file with the given name in a directory with the given name.
     * This method will attempt to create missing directories and avoid overwriting existing files
     * as requested.
     *
     * @param d    A directory name.
     * @param f    A file name.
     * @param opts An array with {@link OpenOption} objects.
     *
     * @return A new buffered text writer.
     *
     * @throws IOException If directories or files can not be created or written to.
     */
    public static Writer getWriter( String d, String f, OpenOption... opts ) throws IOException {
        return getWriter( d, f, false, false, StandardCharsets.UTF_8, opts );
    }


    /**
     * Obtain a {@link Writer} to a file with the given name in a directory with the given name.
     * This method will attempt to create missing directories and avoid overwriting existing files
     * as requested.
     *
     * @param d    A directory name.
     * @param f    A file name.
     * @param cs   A {@link Charset} for string encoding.
     * @param opts An array with {@link OpenOption} objects.
     *
     * @return A new buffered text writer.
     *
     * @throws IOException If directories or files can not be created or written to.
     */
    public static Writer getWriter( String d, String f, Charset cs, OpenOption... opts ) throws IOException {
        return getWriter( d, f, false, false, cs, opts );
    }


    /**
     * Obtain a {@link Writer} to a file with the given name in a directory with the given name.
     * This method will attempt to create missing directories and avoid overwriting existing files
     * as requested.
     *
     * @param d    A directory name.
     * @param f    A file name.
     * @param mk   If {@code true}, attempt to create missing directories.
     * @param ow   If {@code false}, add a suffix to the given file name to prevent overwriting.
     * @param cs   A {@link Charset} for string encoding.
     * @param opts An array with {@link OpenOption} objects.
     *
     * @return A new buffered text writer.
     *
     * @throws IOException If directories or files can not be created or written to.
     */
    public static Writer getWriter( String d, String f, boolean mk, boolean ow, Charset cs, OpenOption... opts ) throws IOException {
        Path fp = getFilePath( d, f, mk, ow );
        return Files.newBufferedWriter( fp, cs, opts );
    }

    /**
     * Obtain an {@link OutputStream} to a file with the given name in a directory with the given
     * name.
     * This method will fail if a directory with the given name doesn't exist, or if it already
     * contains a file with the given name.
     *
     * @param dir  A directory name.
     * @param name A file name.
     *
     * @return An OutputStream.
     *
     * @throws IOException If directories or files can not be created or written to.
     */
    public static OutputStream getOutputStream( String dir, String name ) throws IOException {
        return getOutputStream( dir, name, false, false );
    }

    /**
     * Obtain an {@link OutputStream} to a file with the given name in a directory with the given
     * name.
     * This method will attempt to create missing directories as requested
     *
     * @param dir    A directory name.
     * @param name   A file name.
     * @param mkdirs If {@code true}, attempt to create missing directories.
     *
     * @return An OutputStream.
     *
     * @throws IOException If directories or files can not be created or written to.
     */
    public static OutputStream getOutputStream( String dir, String name, boolean mkdirs ) throws IOException {
        return getOutputStream( dir, name, mkdirs, false );
    }

    /**
     * Obtain an {@link OutputStream} to a file with the given name in a directory with the given
     * name.
     * This method will attempt to create missing directories and avoid overwriting existing files
     * as requested.
     *
     * @param dir       A directory name.
     * @param name      A file name.
     * @param mkdirs    If {@code true}, attempt to create missing directories.
     * @param overwrite If {@code false}, add a suffix to the given file name if a file with that
     *                  name already exists.
     *
     * @return An OutputStream.
     *
     * @throws IOException If directories or files can not be created or written to.
     */
    public static OutputStream getOutputStream( String dir, String name, boolean mkdirs, boolean overwrite )
        throws IOException {
        Path filePath = getFilePath( dir, name, mkdirs, overwrite );
        return Files.newOutputStream( filePath );
    }

    public static Path getFilePath( String dir, String name, boolean mkdirs, boolean overwrite )
        throws IOException {
        return getFilePath( Paths.get( dir ), Paths.get( name ), mkdirs, overwrite );
    }

    public static Path getFilePath( Path dir, String name, String ext, boolean mkdirs, boolean overwrite ) throws IOException {
        return getFilePath( dir, Paths.get( name + EXT + ext ), mkdirs, overwrite );
    }

    public static Path getFilePath( Path dir, Path name, boolean mkdirs, boolean overwrite )
        throws IOException {
        Path file = dir.resolve( name );
        Path realDir = file.getParent();

        if ( !Files.isDirectory( realDir ) ) {
            if ( !Files.exists( realDir ) ) {
                if ( mkdirs ) {
                    Files.createDirectories( realDir );
                } else {
                    throw new FileNotFoundException( String.format(
                        "Directory %s not found", dir.toString()
                    ) );
                }
            } else {
                throw new NotDirectoryException( String.format(
                    "%s is not a directory.", dir.toString()
                ) );
            }
        }

        if ( Files.exists( file ) && !overwrite ) {
            String[] fName = splitFilename( file );
            Path nuFile;
            int i = 0;
            do {
                String nuName = String.format( "%s-%d.%s", fName[ 0 ], i++, fName[ 1 ] );
                nuFile = realDir.resolve( Paths.get( nuName ) );
            } while ( Files.exists( nuFile ) );
            file = nuFile;
        }

        return file;
    }

    public static FileChannel openChannel( String dir, String name, boolean mkd, boolean ow ) throws IOException {
        return openChannel( dir, name, mkd, ow, true, true );
    }
    
    public static FileChannel openChannel( String dir, String name, boolean mkd, boolean ow, boolean w, boolean r ) throws IOException {
        Path path = getFilePath( dir, name, mkd, ow );
        return openChannel( path, mkd, ow, w, r );
    }
    
    public static FileChannel openChannel( Path path, boolean mkdir, boolean ow, boolean w, boolean r ) throws IOException {
        List<OpenOption> opts = new ArrayList<>();
        opts.add( StandardOpenOption.CREATE );
        if( r ) opts.add( StandardOpenOption.READ );
        if( w ) opts.add( StandardOpenOption.WRITE );
        if( ow ) opts.add( StandardOpenOption.TRUNCATE_EXISTING );
        return FileChannel.open( path, opts.toArray( new OpenOption[opts.size()] ) );
    }
    
    public static FileChannel openChannel( String dir, String name, boolean mkd, OpenOption... opts ) throws IOException {
        boolean ow = false;
        for( OpenOption opt : opts ) {
            if( opt.equals(  StandardOpenOption.TRUNCATE_EXISTING ) ) ow = true;
        }
        Path path = FileUtils.getFilePath( dir, name, mkd, ow );
        return FileChannel.open( path, opts );
    }
    
    public static String[] splitFileName( String file ) {
        int ext = file.lastIndexOf( EXT );
        return new String[]{ file.substring( 0, ext ), file.substring( ext + 1 ) };
    }

    public static String[] splitFilename( Path path ) {
        return splitFileName( path.toString() );
    }
    
    public static void clearDirectory( Path inputDir ) throws IOException {
        clearDirectory( inputDir, null, false );
    }
    
    public static void clearDirectory( Path inputDir, String glob ) throws IOException {
        clearDirectory( inputDir, glob, false );
    }
    
    public static void clearDirectory( Path inputDir, String glob, boolean recursive ) throws IOException {
        for( Path p : listPaths( inputDir, glob, recursive ) ) {
            Files.delete( p );
        }
    }

}
