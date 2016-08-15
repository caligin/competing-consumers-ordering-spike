(ns competing-consumers-ordering-spike.producer
  (:gen-class)
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as le]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [clojure.pprint :as pp]))

(defn gen-events [id]
  (map (partial list id) '("create" "update" "update" "update" "update" "close")))

(defn make-body [[evid evtype]]
  (str evid ":" evtype))

(defn -main
  "Producer"
  [& args]
  (let [conn  (rmq/connect)
        ch    (lch/open conn)]
    (println (format "Producer Connected. Channel id: %d" (.getChannelNumber ch)))
    (le/declare ch "things" "topic" {:durable true :auto-delete false})
    (doseq [ev (mapcat gen-events (range 9999))]
      (let [[evid _] ev]
        (lb/publish ch "things" "events.for.things" (make-body ev) {:type "greetings.hi" :content-type "text/plain" :headers {"hashid" (str evid)}})))

    (rmq/close ch)
    (rmq/close conn)))
