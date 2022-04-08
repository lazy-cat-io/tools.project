(ns tools.path
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [tools.system :as system])
  (:import
    (java.io
      File)))


(def file-separator
  File/separator)


(defn user-dir
  []
  (system/get-property "user.dir"))


(defn user-home
  []
  (system/get-property "user.home"))


(defn symbol->path
  [sym]
  (some-> sym
          (str/replace "." file-separator)))


(defn ensure-dir
  ^File [dir]
  (let [dir' (io/file dir)]
    (if (.exists dir')
      dir'
      (if (.mkdirs dir')
        dir'
        (throw (ex-info (str "Can't create directory " dir) {:dir dir}))))))


(defn ensure-file
  ([file]
   (ensure-file file ""))
  ([file contents & opts]
   (let [file (io/file file)
         parent (.getParent file)]
     (if (.exists (io/file parent))
       (apply spit file contents opts)
       (do
         (ensure-dir parent)
         (apply spit file contents opts))))))
