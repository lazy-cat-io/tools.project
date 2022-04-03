(ns tools.process
  (:require
    [babashka.process :as process]
    [clojure.string :as str]))


(defn execute
  [command]
  (let [res (->> command
                 (process/tokenize)
                 (process/process)
                 :out
                 slurp
                 str/trim-newline)]
    (when (seq res)
      res)))
