(ns tools.datetime
  (:refer-clojure :exclude [format])
  (:require
    [tools.datetime.formatter :as fmt])
  (:import
    (java.time
      ZoneId
      ZonedDateTime)
    (java.time.format
      DateTimeFormatter)))


(defn zone-id
  ([]
   (ZoneId/of "UTC"))
  ([^String zone-id]
   (ZoneId/of zone-id)))


(defn zoned-date-time
  ([]
   (ZonedDateTime/now ^ZoneId (zone-id)))
  ([^ZoneId zone-id]
   (ZonedDateTime/now zone-id)))


(defprotocol Formattable
  (format [this] [this ^DateTimeFormatter fmt]))


(extend-protocol Formattable
  ZonedDateTime
  (format
    ([dt] (str (.format dt fmt/iso-offset-datetime)))
    ([dt ^DateTimeFormatter fmt] (str (.format dt fmt)))))
