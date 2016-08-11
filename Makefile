.PHONY: run-containers kill-containers

run-containers:
	docker run -p 27017:27017 --name spike-mongo -d mongo:3.2
	docker run -p 5672:5672 -p 8080:15672 --name spike-rabbit -d rabbitmq:3.6.1-management

kill-containers:
	docker stop spike-mongo
	docker rm spike-mongo
	docker stop spike-rabbit
	docker rm spike-rabbit

