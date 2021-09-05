(ns core
  (:require [reitit.ring :as ring]
            [reitit.coercion.spec]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.ring.coercion :as coercion]
            [reitit.dev.pretty :as pretty]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.parameters :as parameters]
            ;; Uncomment to use
            ; [reitit.ring.middleware.dev :as dev]
            ; [reitit.ring.spec :as spec]
            ; [spec-tools.spell :as spell]
            [ring.adapter.jetty :as jetty]
            [muuntaja.core :as m]))

(def candidates (slurp "candidates.txt"))

(def blockchain (atom {}))

(def app
  (ring/ring-handler
    (ring/router
      [["/swagger.json"
        {:get {:no-doc true
               :swagger {:info {:title "my-api"
                                :description "with reitit-ring"}}
               :handler (swagger/create-swagger-handler)}}]

       ["/vote"
        {:post {:summary "Submit your vote"
                :parameters {:body {:sha string? :to string? :coin string? :sig string?}}
                :response {201 nil}
                :handler (fn [{{{:keys [sha to coin sig]} :body} :parameters}]
                           {:status 201})}}]
       ["/gift"
        {:post {:summary "Authenticate and recieve vote"
                :parameters {:body {:to string? :image string?}}
                :reponse {200 {:body {:coin string?}}}
                :handler (fn [{{{:keys [to image]} :body} :parameters}]
                           {:status 200
                            :body {:coin 1}})}}]
       ["/result"
        {:get {:summary "Get the voting results"
               :response {200 {:body [{:to string? :count int?}]}}
               :handler (fn [_] (let [ret {}] (reduce #(assoc %1 (:to %2) (inc (%1 (:to %2) 0))) {} @blockchain)))}}]


       ["/math"
        {:swagger {:tags ["math"]}}

        ["/plus"
         {:get {:summary "plus with spec query parameters"
                :parameters {:query {:x int?, :y int?}}
                :responses {200 {:body {:total int?}}}
                :handler (fn [{{{:keys [x y]} :query} :parameters}]
                           {:status 200
                            :body {:total (+ x y)}})}
          :post {:summary "plus with spec body parameters"
                 :parameters {:body {:x int?, :y int?}}
                 :responses {200 {:body {:total int?}}}
                 :handler (fn [{{{:keys [x y]} :body} :parameters}]
                            {:status 200
                             :body {:total (+ x y)}})}}]]]

      {;;:reitit.middleware/transform dev/print-request-diffs ;; pretty diffs
       ;;:validate spec/validate ;; enable spec validation for route data
       ;;:reitit.spec/wrap spell/closed ;; strict top-level validation
       :exception pretty/exception
       :data {:coercion reitit.coercion.spec/coercion
              :muuntaja m/instance
              :middleware [;; swagger feature
                           swagger/swagger-feature
                           ;; query-params & form-params
                           parameters/parameters-middleware
                           ;; content-negotiation
                           muuntaja/format-negotiate-middleware
                           ;; encoding response body
                           muuntaja/format-response-middleware
                           ;; exception handling
                           exception/exception-middleware
                           ;; decoding request body
                           muuntaja/format-request-middleware
                           ;; coercing response bodys
                           coercion/coerce-response-middleware
                           ;; coercing request parameters
                           coercion/coerce-request-middleware
                           ;; multipart
                           multipart/multipart-middleware]}})
    (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path "/"
         :config {:validatorUrl nil
                  :operationsSorter "alpha"}})
      (ring/create-default-handler))))

(defn start [_]
  (jetty/run-jetty #'app {:port 3000, :join? false})
  (println "server running in port 3000"))
