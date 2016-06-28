import module namespace uima="http://incite.columbia.edu/uima/TypeSystem";

declare variable $name := "InciteTypes";
declare variable $desc := "Types for Incite's UIMA API";
declare variable $version := "1.0";
declare variable $vendor := "Incite - Columbia University";

declare variable $ns := "edu.columbia.incite.uima.api.types";
declare variable $root := "/home/gorgonzola/src/local";
declare variable $outFile :=  $root 
  || "/incite/incite-uima/src/main/resources/desc/type/" || $name || ".xml";

let $baseName := $ns || ".Span"

let $attr := uima:type(
  $ns || ".Attribute"
  , "A key-value pair for arbitrary annotation attributes"
  , $uima:AnnotationBase (: no indexing :)
  , (
    uima:feature( "k", "Key", $uima:String )
    , uima:feature( "v", "Value", $uima:String )
  )
)

let $tuple := uima:type(
  $ns || ".Tuple"
  , "Predicate tuples of the form (subject-predicate-object)"
  , $uima:AnnotationBase (: no indexing :)
  , (
    uima:feature( "subject", "Subject of this predicate", $baseName )
    , uima:feature( "predicate", "Predicate identifier", $uima:String )
    , uima:feature( "object", "Object of this predicate", $baseName )
  )
)

let $inciteBase := uima:type(
  $baseName
  , "Base type for span annotations"
  , $uima:Annotation
  , ( 
    uima:feature( "attributes", "Annotation attributes", $uima:FSArray, uima:fsName( $attr ), true() )
    , uima:feature( "tuples", "Tuples that refer to this entity", $uima:FSArray, uima:fsName( $tuple ), true() )
    , uima:feature( "id", "Annotation id, unique within CAS and type", $uima:String )
  )
)

let $mark := uima:type(
  $ns || ".Mark"
  , "Inline mark for arbitrary annotations"
  , $uima:Annotation
  , (
    uima:feature( "notes", "Notes at position", $uima:String )
  )
)

let $baseTypes := ( $inciteBase, $attr, $tuple, $mark )

(: Generic Annotation Types :)
let $entity := uima:type(
  $ns || ".Entity"
  , "Base type for arbitrary span annotations"
  , uima:fsName( $inciteBase )
)

let $segment := uima:type(
  $ns || ".Segment"
  , "Base type for segment annotations"
  , uima:fsName( $inciteBase )
  , uima:feature( "level", "Segmentation level", $uima:Integer )
)

let $paragraph := uima:type(
  $ns || ".Paragraph"
  , "Paragraph annotations"
  , uima:fsName( $segment )
)

let $genericTypes := ( $entity, $segment, $paragraph )

(: Document metadata :)
let $md := uima:type(
  $ns || ".Document"
  , "Document metadata annotation"
  , uima:fsName( $inciteBase )
  (: , $uima:Annotation :)
  , (
    uima:feature( "id", "Document identifier within a collection", $uima:String )
    , uima:feature( "collection", "Collection identifier", $uima:String )
    , uima:feature( "uri", "URI of this document's data source", $uima:String )
    , uima:feature( "xpath", "XPath expression for this document's data node sequence", $uima:String )
    , uima:feature( "index", "Index of this document's data in the node sequence", $uima:Integer )
    , uima:feature( "proc_isLast", "Boolean indicating if this is the last document in the sequence", $uima:Boolean )
    , uima:feature( "proc_skip", "Boolean indicating that this document should be skipped in processing", $uima:Boolean )
  )
)

let $ts := uima:typeSystem( $name, $desc, $version, $vendor, (
  $md, $baseTypes, $genericTypes
), () )

return ( $ts, file:write( $outFile, $ts ) )