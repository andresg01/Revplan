# =========
# Variables
# =========
PROJECT           ?= llm-adapter
COMPOSE_FILE      ?= docker-compose.yml
APP_SERVICE       ?= llm-adapter-ms
STACK_NETWORK_KEY ?= revplan-network

# Detecta el comando compose (v2 o v1)
COMPOSE := $(shell (docker compose version >/dev/null 2>&1 && echo "docker compose") || \
                     (docker-compose version >/dev/null 2>&1 && echo "docker-compose"))

# Nombre de la red a usar (sin prefijo del proyecto)
COMPOSE_NET := $(STACK_NETWORK_KEY)

# =========
# Configuración global
# =========
export DOCKER_BUILDKIT=0

# =========
# Ayuda
# =========
.PHONY: help
help:
	@echo "Targets disponibles:"
	@echo "  make up           - Crea red (si falta), build y levanta el stack en segundo plano"
	@echo "  make build        - Build (compose) del stack"
	@echo "  make build-nc     - Build sin caché"
	@echo "  make down         - Para y elimina el stack (sin borrar volúmenes)"
	@echo "  make nuke         - Para el stack y borra la red"
	@echo "  make ps           - Estado de los servicios"
	@echo "  make logs         - Logs de todos los servicios (follow)"
	@echo "  make logs-app     - Logs del micro ($(APP_SERVICE))"
	@echo "  make restart      - Reinicia sólo el micro"
	@echo "  make sh-app       - Shell dentro del micro"
	@echo "  make check        - Muestra versiones de docker y compose detectadas"

# =========
# Checks & red
# =========
.PHONY: check
check:
	@echo "Docker:  $$(docker --version)"
	@echo "Compose: $$( $(COMPOSE) version )"
	@echo "Proyecto: $(PROJECT)"
	@echo "Compose file: $(COMPOSE_FILE)"
	@echo "Red esperada: $(COMPOSE_NET)"

.PHONY: network
network:
	@# Crea la red externa compartida si no existe
	@if ! docker network inspect $(COMPOSE_NET) >/dev/null 2>&1; then \
		echo ">> Creando red $(COMPOSE_NET)"; \
		docker network create $(COMPOSE_NET); \
	else \
		echo ">> Red $(COMPOSE_NET) ya existe"; \
	fi

# =========
# Ciclo de vida
# =========
.PHONY: up
up: remove-wrong-network network
	$(COMPOSE) -p $(PROJECT) -f $(COMPOSE_FILE) up -d --build
	@echo ">> Stack levantado. Swagger en http://localhost:8083/swagger-ui.html (cuando el micro arranque)"

.PHONY: build
build:
	$(COMPOSE) -p $(PROJECT) -f $(COMPOSE_FILE) build

.PHONY: build-nc
build-nc:
	$(COMPOSE) -p $(PROJECT) -f $(COMPOSE_FILE) build --no-cache

.PHONY: down
down:
	$(COMPOSE) -p $(PROJECT) -f $(COMPOSE_FILE) down

# Elimina la red con nombre "prefijado" si existe (evita el error de etiquetas de Compose)
.PHONY: remove-wrong-network
remove-wrong-network:
	-@docker network rm $(PROJECT)_$(STACK_NETWORK_KEY) 2>/dev/null || true

.PHONY: nuke
nuke:
	$(COMPOSE) -p $(PROJECT) -f $(COMPOSE_FILE) down -v
	-@docker network rm $(COMPOSE_NET) 2>/dev/null || true
	@echo ">> Limpieza completa (stack y red del proyecto)"

# =========
# Observabilidad
# =========
.PHONY: ps
ps:
	$(COMPOSE) -p $(PROJECT) -f $(COMPOSE_FILE) ps

.PHONY: logs
logs:
	$(COMPOSE) -p $(PROJECT) -f $(COMPOSE_FILE) logs -f

.PHONY: logs-app
logs-app:
	$(COMPOSE) -p $(PROJECT) -f $(COMPOSE_FILE) logs -f $(APP_SERVICE)

.PHONY: restart
restart:
	$(COMPOSE) -p $(PROJECT) -f $(COMPOSE_FILE) restart $(APP_SERVICE)

# =========
# Shell útil
# =========
.PHONY: sh-app
sh-app:
	docker exec -it $(APP_SERVICE) sh
