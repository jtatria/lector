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
package edu.columbia.incite;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.columbia.incite.corpus.Lexicon;
import edu.columbia.incite.run.ConfBase;
import edu.columbia.incite.uima.index.Tokenizer;
import edu.columbia.incite.uima.io.BinaryReader;
import edu.columbia.incite.uima.io.BinaryWriter;
import edu.columbia.incite.uima.tools.InciteBroker;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class Conf extends ConfBase {

    // Parameter declarations
    public static final String PARAM_HOME_DIR     = "home_dir";
    public static final String PARAM_CONF_DIR     = "conf_dir";
    public static final String PARAM_BASE_PROPS   = "base_props_file";
    public static final String PARAM_LOG_PROPS    = "log_props_file";

    public static final String PARAM_DATA_DIR     = "data_dir";
    public static final String PARAM_INDEX_DIR    = "index_dir";
    public static final String PARAM_INPUT_DIR    = "input_dir";
    public static final String PARAM_OUTPUT_DIR   = "output_dir";
    public static final String PARAM_TABLES_DIR   = "tables_dir";

    public static final String PARAM_COOC_FILE    = "cooc_file";
    public static final String PARAM_POSC_FILE    = "posc_file";
    public static final String PARAM_FREQ_FILE    = "freq_file";
    public static final String PARAM_LXCN_FILE    = "lxcn_file";
    public static final String PARAM_TERM_ID      = "term_id";

    public static final String PARAM_UIMA_READER  = "uima_reader";
    public static final String PARAM_UIMA_WRITER  = "uima_writer";
    public static final String PARAM_UIMA_AES     = "uima_aes";
    public static final String PARAM_UIMA_CONS    = "uima_consumer";
    public static final String PARAM_UIMA_ONERR   = "uima_onerr";
    
    public static final String PARAM_DOCID_FIELD  = "doc_id";
    public static final String PARAM_TXT_FIELD    = "field_txt";
    public static final String PARAM_SPLIT_FIELD  = "field_split";
    public static final String PARAM_FILTER_FIELD = "field_filter";
    public static final String PARAM_FILTER_TERM  = "filter_term";
    
    public static final String PARAM_COOCUR_W_PRE = "cooc_w_pre";
    public static final String PARAM_COOCUR_W_POS = "cooc_w_pos";
    public static final String PARAM_MIN_TERM_FRQ = "min_term_freq";
    
    public static final String PARAM_THREADS      = "threads";
    public static final String PARAM_QUIET        = "quiet";
    public static final String PARAM_DUMP_CONF    = "dump_conf";

    // Parameter documentation
    public static final String DESC_HOME_DIR     = "Root directory for all paths";
    public static final String DESC_CONF_DIR     = "Configuration files directory";
    public static final String DESC_BASE_PROPS   = "Base configuration file";
    public static final String DESC_LOG_PROPS    = "Logging configuration file";

    public static final String DESC_DATA_DIR     = "Base corpus data directory";
    public static final String DESC_INDEX_DIR    = "Lucene index directory";
    public static final String DESC_INPUT_DIR    = "UIMA input directory";
    public static final String DESC_OUTPUT_DIR   = "UIMA output directory";
    public static final String DESC_TABLES_DIR   = "Corpus metadata tables directory";

    public static final String DESC_COOC_FILE    = "Cooccurrence file name";
    public static final String DESC_POSC_FILE    = "POS counts file name";
    public static final String DESC_FREQ_FILE    = "Frequencies file name";
    public static final String DESC_LXCN_FILE    = "Lexicon file name";
    public static final String DESC_TERM_ID      = "Term id column header";
    
    public static final String DESC_UIMA_READER  = "UIMA collection reader";
    public static final String DESC_UIMA_WRITER  = "UIMA collection writer";
    public static final String DESC_UIMA_AES     = "UIMA analysis engienes";
    public static final String DESC_UIMA_CONS    = "UIMA consumer";
    public static final String DESC_UIMA_ONERR   = "UIMA action on error";

    public static final String DESC_DOCID_FIELD  = "Document id field";
    public static final String DESC_TXT_FIELD    = "Text field for corpus analysis";
    public static final String DESC_SPLIT_FIELD  = "Split field for term frequencies";
    public static final String DESC_FILTER_FIELD = "Default filtering field";
    public static final String DESC_FILTER_TERM  = "Default filtering field term";
    
    public static final String DESC_COOCUR_W_PRE = "Cooccurrence window trailing width";
    public static final String DESC_COOCUR_W_POS = "Cooccurrence window leading width";
    public static final String DESC_MIN_TERM_FRQ = "Minimum lexicon term frequency";
    
    public static final String DESC_THREADS      = "Number of threads to run workers on";
    public static final String DESC_QUIET        = "Silence worker progress reports";
    public static final String DESC_DUMP_CONF    = "Dump effective configuration to disk";

    // Default parameter values
    public static final String DFLT_HOME_DIR      = System.getProperty( "user.dir" );
    public static final String DFLT_CONF_DIR      = "conf";
    public static final String DFLT_BASE_PROPS    = "base.properties";
    public static final String DFLT_LOG_PROPS     = "logging.properties";

    public static final String DFLT_DATA_DIR      = "data";
    public static final String DFLT_INDEX_DIR     = "index";
    public static final String DFLT_INPUT_DIR     = "input";
    public static final String DFLT_OUTPUT_DIR    = "output";
    public static final String DFLT_TABLES_DIR    = "tables/csv";

    public static final String DFLT_COOC_FILE     = "cooc.bin";
    public static final String DFLT_POSC_FILE     = "posc.dsv";
    public static final String DFLT_FREQ_FILE     = "freq.dsv";
    public static final String DFLT_LXCN_FILE     = "lxcn.dsv";
    public static final String DFLT_TERM_ID       = Lexicon.TERM_ID;
    
    public static final Class  DFLT_UIMA_READER   = BinaryReader.class;
    public static final Class  DFLT_UIMA_WRITER   = BinaryWriter.class;
    public static final List<Class> DFLT_UIMA_AES = new ArrayList<>();
    public static final Class  DFLT_UIMA_CONS     = null;
    public static final String DFLT_UIMA_ONERR    = "continue";

    public static final String DFLT_DOCID_FIELD   = InciteBroker.DOCID_FIELD_NAME;
    public static final String DFLT_TXT_FIELD     = "text_field";
    public static final String DFLT_SPLIT_FIELD   = "split_field";
    public static final String DFLT_FILTER_FIELD  = "filter_field";
    public static final String DFLT_FILTER_TERM   = Tokenizer.NOTERM;

    public static final int    DFLT_COOCUR_W_PRE  = 10;
    public static final int    DFLT_COOCUR_W_POS  = 10;
    public static final int    DFLT_MIN_TERM_FRQ  = 5;
    
    public static final int    DFLT_THREADS       = Runtime.getRuntime().availableProcessors();
    public static final boolean DFLT_QUIET        = false;
    public static final boolean DFLT_DUMP_CONF    = false;
    
//    public static final String DFLT_DOCID_FIELD   = POBDocFields.OBO_SECTION_FIELD;
//    public static final String DFLT_TXT_FIELD     = POBTokenFields.FIELD_LEMMA_CONF;
//    public static final String DFLT_SPLIT_FIELD   = POBDocFields.OBO_YEAR_FIELD;
//    public static final String DFLT_FILTER_FIELD  = POBDocFields.OBO_TYPE_FIELD;

    public static final String DFLT_NS            = "edu.columbia.incite";

    public static final Options CLI_OPTS;
    static {
        Options opts = new Options();
        opts.addOption( "c", PARAM_CONF_DIR,   true, DESC_CONF_DIR );
        opts.addOption( "b", PARAM_BASE_PROPS, true, DESC_BASE_PROPS );
        CLI_OPTS = opts;
    }

    public static Conf make( String... args ) throws ParseException, IOException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse( CLI_OPTS, args );
        } catch( ParseException ex ) {
            usage();
        }

        // conf directory
        String confd = cmd.hasOption( PARAM_CONF_DIR ) ?
            cmd.getOptionValue( PARAM_CONF_DIR ) : DFLT_CONF_DIR;
        // base conf file
        String base_conf = cmd.hasOption( PARAM_BASE_PROPS ) ?
            cmd.getOptionValue( PARAM_BASE_PROPS ) : DFLT_BASE_PROPS;
        // home directory
        String home = cmd.getArgs().length >= 1 ? cmd.getArgs()[0] : DFLT_HOME_DIR;
        home = home.replaceFirst( "^~", System.getProperty( "user.home" ) );
        
        Properties props = new Properties();
        props.setProperty( DFLT_NS + "." + PARAM_HOME_DIR,  home );
        Path confDir = Paths.get( confd );
        try( FileInputStream fis = new FileInputStream( confDir.resolve( base_conf ).toFile() ) ) {
            props.load( fis );
        }
        
        return new Conf( DFLT_NS, props );
    }
    
    private static void usage() {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp( "obo", CLI_OPTS );
        System.exit( 0 );
    }

    public static void printParams() {
        String format = "%18s\t%s\n";
        System.out.printf( format, PARAM_HOME_DIR    , DESC_HOME_DIR     );
        System.out.printf( format, PARAM_CONF_DIR    , DESC_CONF_DIR     );
        System.out.printf( format, PARAM_BASE_PROPS  , DESC_BASE_PROPS   );
        System.out.printf( format, PARAM_LOG_PROPS   , DESC_LOG_PROPS    );

        System.out.printf( format, PARAM_DATA_DIR    , DESC_DATA_DIR     );
        System.out.printf( format, PARAM_INDEX_DIR   , DESC_INDEX_DIR    );
        System.out.printf( format, PARAM_INPUT_DIR   , DESC_INPUT_DIR    );
        System.out.printf( format, PARAM_OUTPUT_DIR  , DESC_OUTPUT_DIR   );
        System.out.printf( format, PARAM_TABLES_DIR  , DESC_TABLES_DIR   );

        System.out.printf( format, PARAM_COOC_FILE   , DESC_COOC_FILE    );
        System.out.printf( format, PARAM_POSC_FILE   , DESC_POSC_FILE    );
        System.out.printf( format, PARAM_FREQ_FILE   , DESC_FREQ_FILE    );
        System.out.printf( format, PARAM_LXCN_FILE   , DESC_LXCN_FILE    );
        System.out.printf( format, PARAM_TERM_ID     , DESC_TERM_ID      );

        System.out.printf( format, PARAM_UIMA_READER , DESC_UIMA_READER  );
        System.out.printf( format, PARAM_UIMA_WRITER , DESC_UIMA_WRITER  );
        System.out.printf( format, PARAM_UIMA_AES    , DESC_UIMA_AES     );
        System.out.printf( format, PARAM_UIMA_CONS   , DESC_UIMA_CONS    );
        System.out.printf( format, PARAM_UIMA_ONERR  , DESC_UIMA_ONERR   );

        System.out.printf( format, PARAM_DOCID_FIELD , DESC_DOCID_FIELD  );
        System.out.printf( format, PARAM_TXT_FIELD   , DESC_TXT_FIELD    );
        System.out.printf( format, PARAM_SPLIT_FIELD , DESC_SPLIT_FIELD  );
        System.out.printf( format, PARAM_FILTER_FIELD, DESC_FILTER_FIELD );
        System.out.printf( format, PARAM_FILTER_TERM , DESC_FILTER_TERM  );

        System.out.printf( format, PARAM_COOCUR_W_PRE, DESC_COOCUR_W_PRE );
        System.out.printf( format, PARAM_COOCUR_W_POS, DESC_COOCUR_W_POS );
        System.out.printf( format, PARAM_MIN_TERM_FRQ, DESC_MIN_TERM_FRQ );
        System.out.printf( format, PARAM_THREADS     , DESC_THREADS      );
        System.out.printf( format, PARAM_THREADS     , DESC_THREADS      );
        System.out.printf( format, PARAM_THREADS     , DESC_THREADS      );
    }

    public void printSettings() {
        String format = "%18s\t%s\n";
        System.out.printf( format, PARAM_HOME_DIR    , this.homeDir().toString()     );
        System.out.printf( format, PARAM_CONF_DIR    , this.confDir().toString()     );
        System.out.printf( format, PARAM_CONF_DIR    , this.baseProps().toString()   );
        System.out.printf( format, PARAM_CONF_DIR    , this.logProps().toString()    );
  
        System.out.printf( format, PARAM_DATA_DIR    , this.dataDir().toString()     );
        System.out.printf( format, PARAM_INDEX_DIR   , this.indexDir().toString()    );
        System.out.printf( format, PARAM_INPUT_DIR   , this.inputDir().toString()    );
        System.out.printf( format, PARAM_OUTPUT_DIR  , this.outputDir().toString()   );
        System.out.printf( format, PARAM_TABLES_DIR  , this.tablesDir().toString()   );
  
        System.out.printf( format, PARAM_COOC_FILE   , this.coocFile().toString()    );
        System.out.printf( format, PARAM_POSC_FILE   , this.poscFile().toString()    );
        System.out.printf( format, PARAM_FREQ_FILE   , this.freqFile().toString()    );
        System.out.printf( format, PARAM_LXCN_FILE   , this.lxcnFile().toString()    );
        System.out.printf( format, PARAM_TERM_ID     , this.termId()                 );
  
        System.out.printf( format, PARAM_UIMA_READER , this.uimaReader().getName()   );
        System.out.printf( format, PARAM_UIMA_WRITER , this.uimaWriter().getName()   );
        System.out.printf( format, PARAM_UIMA_AES    , this.uimaAes().toString()     );
        System.out.printf( format, PARAM_UIMA_CONS   , this.uimaConsumer().getName() );
        System.out.printf( format, PARAM_UIMA_ONERR  , this.uimaOnerr()              );
  
        System.out.printf( format, PARAM_DOCID_FIELD , this.fieldDocId()             );
        System.out.printf( format, PARAM_TXT_FIELD   , this.fieldTxt()               );
        System.out.printf( format, PARAM_SPLIT_FIELD , this.fieldSplit()             );
        System.out.printf( format, PARAM_FILTER_FIELD, this.fieldFilter()            );
        System.out.printf( format, PARAM_FILTER_TERM , this.filterTerm()             );
  
        System.out.printf( format, PARAM_COOCUR_W_PRE, this.wPre()                   );
        System.out.printf( format, PARAM_COOCUR_W_POS, this.wPos()                   );
        System.out.printf( format, PARAM_MIN_TERM_FRQ, this.minTermFreq()            );
  
        System.out.printf( format, PARAM_THREADS     , this.threads()                );
        System.out.printf( format, PARAM_QUIET       , this.quiet()                  );
        System.out.printf( format, PARAM_DUMP_CONF   , this.dumpConf()               );
    }

    public Conf() {
        super( new HashMap<>() );
    }

    protected Conf( Map<String,String> values ) {
        super( values );
    }

    protected Conf( String ns, Properties props ) {
        super( ns, props );
    }

    public Path homeDir() {
        return getPath( PARAM_HOME_DIR, null, Paths.get( DFLT_HOME_DIR ) );
    }

    public Path confDir() {
        return getPath( PARAM_CONF_DIR, homeDir(), Paths.get( DFLT_CONF_DIR ) );
    }

    public Path baseProps() {
        return getPath( PARAM_BASE_PROPS, confDir(), Paths.get( DFLT_BASE_PROPS ) );
    }

    public Path logProps() {
        return getPath( PARAM_LOG_PROPS,
            confDir(), Paths.get( DFLT_LOG_PROPS )
        );
    }

    public Path dataDir() {
        return getPath( PARAM_DATA_DIR,
            homeDir(), Paths.get( DFLT_DATA_DIR )
        );
    }

    public Path indexDir() {
        return getPath(
            PARAM_INDEX_DIR, dataDir(), dataDir().resolve( Paths.get( DFLT_INDEX_DIR ) )
        );
    }

    public Path inputDir() {
        return getPath(
            PARAM_INPUT_DIR, dataDir(), dataDir().resolve( Paths.get( DFLT_INPUT_DIR ) )
        );
    }

    public Path outputDir() {
        return getPath(
            PARAM_OUTPUT_DIR, dataDir(), dataDir().resolve( Paths.get( DFLT_OUTPUT_DIR ) )
        );
    }
    
    public Path tablesDir() {
        return getPath(
            PARAM_TABLES_DIR, dataDir(), dataDir().resolve( Paths.get( DFLT_TABLES_DIR ) )
        );
    }

    public Path coocFile() {
        return getPath( PARAM_COOC_FILE,
            dataDir(), Paths.get( DFLT_COOC_FILE )
        );
    }

    public Path poscFile() {
        return getPath( PARAM_POSC_FILE,
            dataDir(), Paths.get( DFLT_POSC_FILE )
        );
    }

    public Path freqFile() {
        return getPath( PARAM_FREQ_FILE,
            dataDir(), Paths.get( DFLT_FREQ_FILE )
        );
    }

    public Path lxcnFile() {
        return getPath( PARAM_LXCN_FILE,
            dataDir(), Paths.get( DFLT_LXCN_FILE )
        );
    }

    public String termId() {
        return getString( PARAM_TERM_ID, DFLT_TERM_ID );
    }

    public String fieldDocId() {
        return getString( PARAM_DOCID_FIELD, DFLT_DOCID_FIELD );
    }
    
    public String fieldTxt() {
        return getString( PARAM_TXT_FIELD, DFLT_TXT_FIELD );
    }

    public String fieldSplit() {
        return getString( PARAM_SPLIT_FIELD, DFLT_SPLIT_FIELD );
    }

    public String fieldFilter() {
        return getString( PARAM_FILTER_FIELD, DFLT_FILTER_FIELD );
    }

    public String filterTerm() {
        return getString( PARAM_FILTER_TERM, DFLT_FILTER_TERM );
    }

    public Class uimaReader() {
        return getClass( PARAM_UIMA_READER, DFLT_UIMA_READER );
    }
    
    public Class uimaWriter() {
        return getClass( PARAM_UIMA_WRITER, DFLT_UIMA_WRITER );
    }
    
    public List<Class> uimaAes() {
        String[] clzs = getStringArray( PARAM_UIMA_AES, new String[]{} );
        List<Class> out = DFLT_UIMA_AES; // TODO: List factory?
        for( String clz : clzs ) {
            try {
                out.add( Class.forName( clz ) );
            } catch ( ClassNotFoundException ex ) {
                throw new RuntimeException( ex ); // TODO: error
            }
        }
        return out;
    }
    
    public Class uimaConsumer() {
        return getClass( PARAM_UIMA_CONS, DFLT_UIMA_CONS );
    }
    
    public String uimaOnerr() {
        return getString( PARAM_UIMA_ONERR, DFLT_UIMA_ONERR );
    }
    
    public int wPre() {
        return getInteger( PARAM_COOCUR_W_PRE, DFLT_COOCUR_W_PRE );
    }

    public int wPos() {
        return getInteger( PARAM_COOCUR_W_POS, DFLT_COOCUR_W_POS );
    }

    public int minTermFreq() {
        return getInteger( PARAM_MIN_TERM_FRQ, DFLT_MIN_TERM_FRQ );
    }

    public int threads() {
        return getInteger( PARAM_THREADS, DFLT_THREADS );
    }
    
    public boolean quiet() {
        return getBoolean( PARAM_QUIET, DFLT_QUIET );
    }
    
    public boolean dumpConf() {
        return getBoolean( PARAM_DUMP_CONF, DFLT_DUMP_CONF );
    }
}
