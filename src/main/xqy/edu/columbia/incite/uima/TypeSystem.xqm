module namespace ui="http://incite.columbia.edu/uima/TypeSystem";

declare default element namespace "http://uima.apache.org/resourceSpecifier";

declare variable $ui:AnnotationBase := "uima.cas.AnnotationBase";
declare variable $ui:Annotation := "uima.tcas.Annotation";

declare variable $ui:String := "uima.cas.String";
declare variable $ui:Integer := "uima.cas.Integer";
declare variable $ui:Double := "uima.cas.Double";
declare variable $ui:Boolean := "uima.cas.Boolean";

declare variable $ui:BooleanArray := "uima.cas.BooleanArray";
declare variable $ui:ByteArray := "uima.cas.ByteArray";
declare variable $ui:ShortArray := "uima.cas.ShortArray";
declare variable $ui:IntegerArray := "uima.cas.IntegerArray";
declare variable $ui:LongArray := "uima.cas.LongArray";
declare variable $ui:FloatArray := "uima.cas.FloatArray";
declare variable $ui:DoubleArray := "uima.cas.DoubleArray";
declare variable $ui:StringArray := "uima.cas.StringArray";
declare variable $ui:FSArray := "uima.cas.FSArray";

declare variable $ui:arrays := (
  "uima.cas.BooleanArray",
  "uima.cas.ByteArray",
  "uima.cas.ShortArray",
  "uima.cas.IntegerArray",
  "uima.cas.LongArray",
  "uima.cas.FloatArray",
  "uima.cas.DoubleArray",
  "uima.cas.StringArray",
  "uima.cas.FSArray"
);

declare function ui:getFilePath( $root as xs:string, $classPath as xs:string ) as xs:string {
  let $tokens := fn:tokenize( $classPath, "\." )
  return fn:string-join( ( $root, $tokens ), file:dir-separator() )
};

declare function ui:typeSystem(
  $name as xs:string, $desc as xs:string, $version as xs:string, $vendor as xs:string
  , $types as element()+
)
as element() {
  ui:typeSystem( $name, $desc, $version, $vendor, $types, () )
};

declare function ui:typeSystem(
  $name as xs:string, $desc as xs:string, $version as xs:string, $vendor as xs:string
  , $types as element()+, $imports as element()*
)
as element() {
  element typeSystemDescription {
    ui:nameAndDesc( $name, $desc ),
    element version { $version },
    element vendor { $vendor },
    $imports,
    element types { $types }
  }
};

declare function ui:nameAndDesc( $name as xs:string, $desc as xs:string )
as element()+ {
  ( element name { $name }, element description { $desc } )
};

declare function ui:import( $locations as xs:string+ )
as element() {
  for $loc in $locations return element import { attribute location { $loc } }
};

declare function ui:type( $name as xs:string, $desc as xs:string, $super as xs:string )
as element() {
  ui:type( $name, $desc, $super, () )
};

declare function ui:type(
  $name as xs:string, $desc as xs:string, $super as xs:string, $feats as element()*
)
as element() {
  element typeDescription {
    ui:nameAndDesc( $name, $desc ),
    element supertypeName { $super },
    if( $feats ) then(
      element features { $feats }
    ) else ()
  }
};

declare function ui:feature( $name as xs:string, $desc as xs:string, $range as xs:string )
as element() {
  element featureDescription {
    ui:nameAndDesc( $name, $desc ),
    element rangeTypeName{ $range }
  }
};

declare function ui:feature(
  $name as xs:string, $desc as xs:string, $range as xs:string, $elementType as xs:string
)
as element() {
  ui:feature( $name, $desc, $range, $elementType, false() )
};

declare function ui:feature(
  $name as xs:string, $desc as xs:string, $range as xs:string, $elementType as xs:string
  , $multRef as xs:boolean
)
as element() {
  element featureDescription {
    ui:nameAndDesc( $name, $desc ),
    element rangeTypeName { $range },
    if( ui:isArrayType( $range ) ) then(
      element elementType { $elementType },
      element multipleReferencesAllowed { $multRef }
    ) else ()
  }
};

declare function ui:isArrayType( $type as xs:string ) as xs:boolean {
  $type = $ui:arrays
};

declare function ui:fsName( $fs as element() ) as xs:string {
  $fs/name/text()
};


