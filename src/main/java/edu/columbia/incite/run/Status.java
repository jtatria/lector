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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: remove deps to progress. I.e. invert constructor logic.
import edu.columbia.incite.run.Progress.Log;
import edu.columbia.incite.run.Progress.Stream;

/**
 * An object that maintains progress status on some pending operation and is capable of producing 
 * progress bars to report on said status.
 * 
 * This class only implements progress counters and status keeping, output and printing is handled 
 * through {@link Progress} implementations.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class Status {
    
    public static final String DEFAULT_NAME = "Working";
    public static final long RATELIMIT = 10_000;
    public static final int STATUS_DONE  = 0;
    public static final int STATUS_TOTAL = 1;
    public static final int STATUS_TIME  = 2;
    
    private final String name;
    
    /* total number of tasks */
    private final AtomicLong    taskTotal = new AtomicLong();
    /* number of completed tasks */
    private final AtomicLong    taskDone  = new AtomicLong();
    /* time between last task and previous */
    private final AtomicLong    taskTime  = new AtomicLong();
    /* time since last report */
    private final AtomicLong    lastTime  = new AtomicLong();
    /* all tasks completed */
    private final AtomicBoolean allDone   = new AtomicBoolean( false );
    /* final report after completed */
    private final AtomicBoolean outDone   = new AtomicBoolean();
    
    /* progress outputs */
    private final List<Progress> outputs = new ArrayList<>();

    /**
     * Make a new progress object with default settings and outputs.
     * 
     * @return An empty progress.
     */
    public static Status make() {
        return make( DEFAULT_NAME, null, System.err );
    }

    /**
     * Make a new progress object with the given name and default settings and outputs.
     * 
     * @param name A name for this progress object, e.g. "Frobbing", "Munging", etc.
     * @return An empty progress.
     */
    public static Status make( String name ) {
        return make( name, null, System.err );
    }
    
    /**
     * Make a new progress object with the given name and number of tasks and default settings and 
     * outputs.
     * 
     * @param name A name for this progress object, e.g. "Frobbing", "Munging", etc.
     * @param tasks Number of tasks.
     * @return An progress with the given number of total tasks.
     */
    public static Status make( String name, long tasks ) {
        Status p = make( name );
        p.add( tasks );
        return p;
    }

    /**
     * Make a new empty progress object with the given name, default settings and 
     * open a {@link Progress} instance over the given {@link Logger}.
     * 
     * @param name A name for this progress object, e.g. "Frobbing", "Munging", etc.
     * @param log A Logger object for output.
     * @return An empty progress.
     */
    public static Status make( String name, Logger log ) {
        return make( name, log, System.err );
    }
    
        /**
     * Make a new empty progress object with the given name, default settings and 
     * open a {@link Progress} instance over the given {@link PrintStream}.
     * 
     * @param name A name for this progress object, e.g. "Frobbing", "Munging", etc.
     * @param out A PrintStream for output, e.g. System.out, System.err, etc.
     * @return An empty progress.
     */
    public static Status make( String name, PrintStream out ) {
        return make( name, null, out );
    }

    /**
     * Make a new empty progress object with the given name, default settings,
     * open {@link Progress} instances over the given {@link Logger} and {@link PrintStream}.
     * 
     * @param name A name for this progress object, e.g. "Frobbing", "Munging", etc.
     * @param log A logger object for output.
     * @param out A PrintStream for output, e.g. System.out, System.err, etc.
     * @return An empty progress.
     */
    public static Status make( String name, Logger log, PrintStream out ) {
        return make( name, log, Level.FINE, out );
    }
    
    /**
     * Make a new progress object with the given name and number of tasks, open a {@link Progress} 
     * instance over the given {@link Logger} with the given {@link Level} for log messages
     * and an instance over the given {@link PrintStream}.
     * 
     * @param name A name for this progress object, e.g. "Frobbing", "Munging", etc.
     * @param log A logger object for output.
     * @param lvl A Level for status messages.
     * @param ps A PrintStream for output, e.g. System.out, System.err, etc.
     * @return An empty progress.
     */
    public static Status make( String name, Logger log, Level lvl, PrintStream ps ) {
        List<Progress> list = new ArrayList<>();
        if( ps != null ) list.add(new Stream( ps,  name ) );
        if( log != null && lvl != null ) list.add(new Log( log, lvl ) );
        Progress[] outs = list.toArray(new Progress[list.size()] );
        return new Status( name, outs );
    }
    
    public Status( String name, Progress... outs ) {
        this.name = name != null ? name : DEFAULT_NAME;
        for( Progress out : outs ) {
            if( out != null ) this.outputs.add( out );
        }
    }

    /**
     * Increment total work by one.
     */
    public void add() {
        this.add( 1 );
    }

    /**
     * Increment total work by the given number of tasks.
     * @param tasks A number representing additional work.
     */
    public void add( long tasks ) {
        taskTotal.addAndGet( tasks );
    }

    /**
     * Complete work equal to 1.
     */
    public void update() {
        update( 1 );
    }

    /**
     * Complete work equal to the given clear amount.
     * @param clear A number representing completed work.
     */
    public void update( long clear ) {
        this.taskTime.set( System.nanoTime() - this.taskTime.get() );
        this.taskDone.addAndGet( clear );
    }

    /**
     * Obtain a quantity representing the amount of work left to be done.
     * @return  A number representing work to be completed.
     */
    public long remaining() {
        return taskTotal.get() - taskDone.get();
    }
    
    /**
     * Print a message with the current status of this progress.
     */
    public void report() {
        report( taskDone.get(), false );
    }

    /**
     * Print a message with the current status of this progress.
     * @param debug if {@code true}, add debug information.
     */
    public void report( boolean debug ) {
        this.report( taskDone.get(), debug );
    }

    /**
     * Obtain a status array. Values for completed tasks, total tasks and time since las update can 
     * be obtained by indexing into this array using the {@link Status#STATUS_DONE}, 
     * {@link Status#STATUS_TOTAL} and {@link Status#STATUS_TIME} constants.
     * 
     * This is the object that is used by outputs for reporting status.
     * See {@link Progress#report(java.lang.String, long[], boolean) }.
     * 
     * @return A {@code long[]} status array.
     */
    private final ThreadLocal<long[]> status = ThreadLocal.withInitial( () -> new long[3] );
    public long[] status() {
        long[] st = status.get();
        st[STATUS_DONE]  = taskDone.get();
        st[STATUS_TOTAL] = taskTotal.get();
        st[STATUS_TIME]  = taskTime.get();
        return st;
//        return new long[]{ taskDone.get(), taskTotal.get(), taskTime.get() };
    }

    private void report( long i, boolean debug ) {
        if( allDone.get() && outDone.getAndSet( true ) ) return;
        long now = System.nanoTime();
        if( !allDone.get() && ( now - lastTime.getAndSet( now ) ) <= RATELIMIT ) return;
        allDone.set( i >= this.taskTotal.get() );
        for( Progress report : outputs ) {
            report.report( name, status(), debug );
            if( allDone.get() ) { 
                report.finish( name );
            }
        }
    }
}
