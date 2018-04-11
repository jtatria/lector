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
package edu.columbia.incite.run;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.columbia.incite.run.Status.STATUS_DONE;
import static edu.columbia.incite.run.Status.STATUS_TIME;
import static edu.columbia.incite.run.Status.STATUS_TOTAL;

/**
 * A progress output object that reports progress status from status arrays.
 */
public interface Progress {
    /**
     * Report the given status array.
     * @param name  Parent progress' name.
     * @param status A status array. See {@link Status#status()}.
     * @param debug A flag indicating whether to include extra information.
     */
    public void report( String name, long[] status, boolean debug );

    /**
     * Take any actions that may be necessary for this output after all tasks are done.
     * @param name Parent progress' name.
     */
    public void finish( String name );
    
    /**
     * A progress output object that reports status by printing pretty progress bars to a 
     * {@link PrintStream}.
     */
    public static class Stream implements Progress {
        
        /** Default line width in characters, equal to 100. **/
        public static final Integer DEFAULT_WIDTH = 100;
        
        private static final String  TAIL          = "] 00.00% *";
        private static final String  FORMAT        = "%s] %4.2f%% %c";
        private static final char    DONE_CHAR     = '#';
        private static final char    LEFT_CHAR     = ' ';
        private static final char[]  SPINNER       = new char[]{ '|', '/', '-', '\\' };

        private final int width;
        private final int bLen;
        private final String format;
        private final PrintStream term;

        /**
         * Create new progress report on the given output stream with the given name/header.
         * @param prtstr A print stream.
         * @param name   A name/header for this progress.
         */
        public Stream( PrintStream prtstr, String name ) {
            this( prtstr, DEFAULT_WIDTH, name );
        }
        
        /**
         * Create a new progress report on the given output stream wit the given character width 
         * and the given name/header.
         * @param prtstr A print stream.
         * @param width  Length of print stream lines.
         * @param name   A name/header for this progress.
         */
        public Stream( PrintStream prtstr, int width, String name ) {
            this.term = prtstr == null ? System.out : prtstr;
            this.width = width;
            this.bLen =  this.width - name.length() - TAIL.length();
            this.format = name + "... [" + FORMAT;
        }

        @Override
        public void report( String name, long[] status, boolean debug ) {
            float prog = (float) status[STATUS_DONE] / (float) status[STATUS_TOTAL];
            int len = bLen - ( debug ? 50 : 0 );
            int b = (int) ( len * prog );
            
            String body = repeat( DONE_CHAR, b ) +
                repeat( LEFT_CHAR, len - b );
            
            String msg = String.format( format,
                body, prog * 100, SPINNER[ (int) status[STATUS_DONE] % SPINNER.length ] 
            );
            
            if( debug ) {
                term.print( String.format(
                    "%d\t%d\t", status[STATUS_TIME], System.currentTimeMillis() ) 
                );
            }
            
            term.print( msg );
            term.flush();
            if( debug ) {
                term.print( String.format( 
                    " i: %d, nanotime: %d",
                    status[STATUS_DONE], System.nanoTime() - status[STATUS_TIME] ) 
                );
            } else {
                // this deletes the line and resets the cursor to the beggining
                // TODO figure out a better way to determine whether the line should be deleted 
                // with \r or \b+.
                //term.print( StringUtils.repeat( "\b", msg.length() ) ); 
                term.print( "\r" ); 
                term.flush();
            }
        }
        
        @Override
        public void finish( String name ) {
            term.println();
            term.flush();
        }

        // Copied verbatim from org.apache.commons.lang3.StringUtils.repeat( char, int )
        // (C) Apache Software Foundation, licensed under Apache License v2.0
        private static String repeat( final char ch, final int repeat ) {
            final char[] buf = new char[repeat];
            for (int i = repeat - 1; i >= 0; i--) {
                buf[i] = ch;
            }
            return new String(buf);
        }
    }

    /** 
     * A progress output that prints status messages to a {@link Logger} object.
     */
    public static class Log implements Progress {
        /** Message format string **/
        public static final String FORMAT = "%s: %d tasks left; %4.2f%% complete.";
        
        private static final long MAX_MSG = 20;

        private final Logger log;
        private final Level lvl;
        private long last = 0;

        /**
         * Create a new progress object on the given logger with the given log message level.
         * @param log A logger object.
         * @param lvl A log message level.
         */
        public Log( Logger log, Level lvl ) {
            this.log = log;
            this.lvl = lvl;
        }

        @Override
        public void report( String name, long[] status, boolean debug ) {
            long total = status[STATUS_TOTAL];
            long done  = status[STATUS_DONE];
            long cSize = total > MAX_MSG ? total / MAX_MSG : 1;
            long chunk = done / cSize;
            if( chunk == last ) return; // already reported this chunk.
            long left = total - done;
            
            log.log(lvl, String.format( FORMAT, name, left, ( (double) done / total ) * 100 ) );
            last = chunk;
        }

        @Override
        public void finish( String name ) {            
            log.log( lvl, String.format( "%s: work complete.", name ) );
        }
    }
}


