(ns clojars.routes.artifact
  (:require [compojure.core :as compojure :refer [GET POST]]
            [clojars.db :as db]
            [clojars.auth :as auth]
            [clojars.web.jar :as view]
            [clojars.web.common :as common]
            [ring.util.response :as response]
            [clojure.set :as set]))

(defn show [db reporter stats group-id artifact-id]
  (if-let [artifact (db/find-jar db group-id artifact-id)]
    (auth/try-account
     (view/show-jar db
                    reporter
                    stats
                    account
                    artifact
                    (db/recent-versions db group-id artifact-id 5)
                    (db/count-versions db group-id artifact-id)))))

(defn list-versions [db group-id artifact-id]
  (if-let [artifact (db/find-jar db group-id artifact-id)]
    (auth/try-account
     (view/show-versions account
                         artifact
                         (db/recent-versions db group-id artifact-id)))))

(defn show-version [db reporter stats group-id artifact-id version]
  (if-let [artifact (db/find-jar db group-id artifact-id version)]
    (auth/try-account
     (view/show-jar db
                    reporter
                    stats
                    account
                    artifact
                    (db/recent-versions db group-id artifact-id 5)
                    (db/count-versions db group-id artifact-id)))))

(defn response-based-on-format
  "render appropriate response based on the file type suffix provided:
  JSON or SVG"
  [db file-format artifact-id & [group-id]]
  (let [group-id (or group-id artifact-id)]
  (cond
    (= file-format "json") (-> (response/response (view/make-latest-version-json db group-id artifact-id))
                               (response/header "Cache-Control" "no-cache")
                               (response/content-type "application/json; charset=UTF-8")
                               (response/header "Access-Control-Allow-Origin" "*"))
    (= file-format "svg") (-> (response/response (view/make-latest-version-svg db group-id artifact-id))
                              (response/header "Cache-Control" "no-cache")
                              (response/content-type "image/svg+xml")
                              (response/header "Access-Control-Allow-Origin" "*")))))

(defn routes [db reporter stats]
  (compojure/routes
   (GET ["/:artifact-id", :artifact-id #"[^/]+"] [artifact-id]
        (show db reporter stats artifact-id artifact-id))
   (GET ["/:group-id/:artifact-id", :group-id #"[^/]+" :artifact-id #"[^/]+"]
        [group-id artifact-id]
        (show db reporter stats group-id artifact-id))

   (GET ["/:artifact-id/versions" :artifact-id #"[^/]+"] [artifact-id]
        (list-versions db artifact-id artifact-id))
   (GET ["/:group-id/:artifact-id/versions"
         :group-id #"[^/]+" :artifact-id #"[^/]+"]
        [group-id artifact-id]
        (list-versions db group-id artifact-id))

   (GET ["/:artifact-id/versions/:version"
         :artifact-id #"[^/]+" :version #"[^/]+"]
        [artifact-id version]
        (show-version db reporter stats artifact-id artifact-id version))
   (GET ["/:group-id/:artifact-id/versions/:version"
         :group-id #"[^/]+" :artifact-id #"[^/]+" :version #"[^/]+"]
        [group-id artifact-id version]
        (show-version db reporter stats group-id artifact-id version))

   (GET ["/:artifact-id/latest-version.:file-format"
         :artifact-id #"[^/]+"
         :file-format #"(svg|json)$"]
        [artifact-id file-format]
        (response-based-on-format db file-format artifact-id))

   (GET ["/:group-id/:artifact-id/latest-version.:file-format"
         :group-id #"[^/]+"
         :artifact-id #"[^/]+"
         :file-format #"(svg|json)$"]
        [group-id artifact-id file-format]
        (response-based-on-format db file-format artifact-id group-id))))
