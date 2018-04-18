# incite-lector
NLP/Indexing backend for Incite's text analysis tools

This package includes three main areas of functionality:
a set of tools for the construction and deployment of UIMA pipelines, including collection readers, analysis engines and external resources,
an API and associated tools for building Lucene indices from UIMA data (which replaces the ancient and obsolete Lucas component), and a set of classes for parameterized construction of cooccurrence matrices and other distributional datasets from Lucene indices.

All three components are more or less independent, but they have all been designed to work seamlessly with each other around a set of conventions for dealing with textual corpora.

This package encapsulates most of the data-acquisition code used for production of my doctoral dissertation, "Beyond Discourse: Computational Text Analysis and Material Historical Processes", written under the supervision of Prof. Peter S. Bearman at the Sociology department of Columbia University.
A second package, written in R, encapsulates the analysis parts and provides an interface to this library as an optional dependency to extract data directly from Lucene indices into an R session.

The Lector class at the root of the source tree provides an interface object to the rest of the package, but this is just a temporary solution. I have tried to document each component to the best of my abilities.


