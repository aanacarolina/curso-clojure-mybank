(ns mybank-web-api.core
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test-http]
            [clojure.pprint :as pp])
  (:gen-class))
;; Exercicios Módulo 03 - Aula 01

;
;1 - Tratamento de conta inválida/inexistente no deposito. Retornar o status http de erro e mensagem no body.
;
;2 - Implementar funcionalidade saque
;
;3 - Criar reset do servidor (tenta stop e tenta start) e demonstrar no mesmo repl antes e depois do tratamento de erro no ex. 1
;verificar onde ha conlfito e que precisa resetar - jogar a funcao reset

(defonce contas (atom {:1 {:saldo 100}
                       :2 {:saldo 200}
                       :3 {:saldo 300}}))

;(defn existent-account?
;"verifica se existe conta com o id passado
;  []
;  )

;criar saldo suficiente
;;

(defn get-saldo [request]
  (let [id-conta (-> request :path-params :id keyword)]
    {:status 200 :body {:saldo (id-conta @contas "conta inválida!")}}))
;;CRIAR CASO NAO EXISTA


(defn saque
  [request]
  (let [id-conta (-> request :path-params :id keyword)
        valor-deposito (-> request :body slurp parse-double)
        SIDE-EFFECT! (try
                       (swap! contas (fn [m] (update-in m [id-conta :saldo] #(- % valor-deposito))))
                       {:status  200
                        :headers {"Content-Type" "text/plain"}
                        :body    {:id-conta   id-conta
                                  :novo-saldo (id-conta @contas)}}
                       (catch Exception e
                         {:status  418
                          :headers {"Content-Type" "text/plain"}
                          :body    "Conta inválida"}))]
    SIDE-EFFECT!))


(defn make-deposit [request]
  (let [id-conta (-> request :path-params :id keyword)
        valor-deposito (-> request :body slurp parse-double)
        SIDE-EFFECT! (try
                       (swap! contas (fn [m] (update-in m [id-conta :saldo] #(+ % valor-deposito))))
                       {:status  200
                        :headers {"Content-Type" "text/plain"}
                        :body    {:id-conta   id-conta
                                  :novo-saldo (id-conta @contas)}}
                       (catch Exception e
                         {:status  418
                          :headers {"Content-Type" "text/plain"}
                          :body    "Conta inválida"}))]
    SIDE-EFFECT!))

(def routes
  (route/expand-routes
    #{["/saldo/:id" :get get-saldo :route-name :saldo]
      ["/saque/:id" :post saque :route-name :saque]
      ["/deposito/:id" :post make-deposit :route-name :deposito]}))

(defn create-server []
  (http/create-server
    {::http/routes routes
     ::http/type   :jetty
     ::http/port   8890
     ::http/join?  false}))

(defonce server (atom nil))

(defn start []
  (reset! server (http/start (create-server))))

(defn stop []
  (http/stop @server))

(defn reset-server
  []
  (try
    (stop)
    (catch Exception e
      e))
  ((try
     (start)
     (catch Exception e
       e)))
  )

(defn test-request [server verb url]
  (test-http/response-for (::http/service-fn @server) verb url))
(defn test-post [server verb url body]
  (test-http/response-for (::http/service-fn @server) verb url :body body))
(defn test-saque [server verb url body]
  (test-http/response-for (::http/service-fn @server) verb url :body body))

(comment
  (start)
  (stop)
  (reset-server)

  (test-request server :get "/saldo/1")
  (test-request server :get "/saldo/2")
  (test-request server :get "/saldo/3")
  (test-request server :get "/saldo/4")

  (test-post server :post "/deposito/2" "863.99")
  (test-post server :post "/deposito/4" "10.99")

  (test-saque server :post "/saque/1" "10.99")

  )
