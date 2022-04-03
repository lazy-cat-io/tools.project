(ns tools.system)


(defn get-env
  ([]
   (System/getenv))
  ([^String s]
   (System/getenv s))
  ([^String s not-found]
   (or (System/getenv s) not-found)))


(defn get-properties
  []
  (System/getProperties))


(defn get-property
  ([^String s]
   (System/getProperty s))
  ([^String s not-found]
   (System/getProperty s not-found)))
