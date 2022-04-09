(ns tools.datetime.formatter
  (:import
    (java.time.format
      DateTimeFormatter)
    (java.util
      Locale)))


(def ^DateTimeFormatter iso-offset-datetime
  DateTimeFormatter/ISO_OFFSET_DATE_TIME)


(def formatters
  {"iso-offset-datetime" iso-offset-datetime})


(defn of-pattern
  ([^String pattern]
   (DateTimeFormatter/ofPattern pattern))
  ([^String pattern ^Locale locale]
   (DateTimeFormatter/ofPattern pattern locale)))
