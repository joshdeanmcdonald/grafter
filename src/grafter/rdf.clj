(ns grafter.rdf
  (:require [clojure.java.io :as io])
  (:require [grafter.rdf.protocols :as pr])
  (:require [grafter.rdf.sesame :as ses])
  (:import [grafter.rdf.protocols Triple]
           [grafter.rdf.sesame ISesameRDFConverter])
  (:import [org.openrdf.model Statement Value Resource Literal URI BNode ValueFactory]
           [org.openrdf.model.impl ValueFactoryImpl LiteralImpl]
           [org.openrdf.repository Repository]
           [org.openrdf.repository.sail SailRepository]
           [org.openrdf.sail.memory MemoryStore]
           [org.openrdf.sail.nativerdf NativeStore]
           [org.openrdf.query TupleQuery TupleQueryResult BindingSet QueryLanguage]
           [org.openrdf.rio RDFFormat]))

(def format-rdf-xml RDFFormat/RDFXML)
(def format-rdf-n3 RDFFormat/N3)
(def format-rdf-ntriples RDFFormat/NTRIPLES)
(def format-rdf-nquads RDFFormat/NQUADS)
(def format-rdf-turtle RDFFormat/TURTLE)
(def format-rdf-jsonld RDFFormat/JSONLD)
(def format-rdf-trix RDFFormat/TRIX)
(def format-rdf-trig RDFFormat/TRIG)

(defn s
  "Cast a string to an RDF literal"
  ([str]
     {:pre [(string? str)]}
     (reify Object
       (toString [_] str)
       ses/ISesameRDFConverter
       (->sesame-rdf-type [this]
         (LiteralImpl. str))))
  ([str lang-or-uri]
     {:pre [(string? str) (or (string? lang-or-uri) (instance? URI lang-or-uri))]}
     (reify Object
       (toString [_] str)
       ses/ISesameRDFConverter
       (->sesame-rdf-type [this]
         (LiteralImpl. str lang-or-uri)))))

(defn expand-subject
  "Takes a turtle like data structure and converts it to triples e.g.

   [:rick [:a :Person]
          [:age 34]]"
  [[subject & po-pairs]]

  (map (fn [[predicate object]]
         (Triple. subject predicate object)) po-pairs))
