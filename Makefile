.PHONY: run stop run-containers stop-containers clean-mongo shell-mongo

run-containers:
	docker run -p 27017:27017 --name spike-mongo -d mongo:3.2
	docker run -p 5672:5672 -p 8080:15672 --name spike-rabbit -d rabbitmq:3.6.1-management

stop-containers:
	docker stop spike-mongo
	docker rm spike-mongo
	docker stop spike-rabbit
	docker rm spike-rabbit

clean-mongo:
	docker exec spike-mongo mongo things --eval 'db.things.drop()'

shell-mongo:
	docker exec -ti spike-mongo mongo things

query-for-nonbroken-mongo:
	docker exec spike-mongo mongo things --eval  'db.things.find({state: {$$ne: "fuckedup"}}).forEach(printjson)'
	docker exec spike-mongo mongo things --eval  'db.things.find({state: {$$ne: "fuckedup"}}).count()'

rabbitmqadmin:
	wget http://localhost:8080/cli/rabbitmqadmin
	chmod u+x $@

things-exchange: rabbitmqadmin
	./rabbitmqadmin -P 8080 declare exchange name=things type=topic

