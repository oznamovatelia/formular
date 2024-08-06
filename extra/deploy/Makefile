.RECIPEPREFIX := $(.RECIPEPREFIX) # recipeprefix is a <space> (allow spaces in Makefile)
.PHONY: build run shutdown rebuild logs restart
GITHUB_USERNAME ?= $(shell echo $$GITHUB_USERNAME)
GITHUB_ACCESSTOKEN ?= $(shell echo $$GITHUB_ACCESSTOKEN)

setup:
    bash setup.sh

build:
    docker build --build-arg GITHUB_USERNAME=$(GITHUB_USERNAME) --build-arg GITHUB_ACCESSTOKEN=$(GITHUB_ACCESSTOKEN) -t uoo-formular:1.0.4 .

rebuild:
    docker build --no-cache --build-arg GITHUB_USERNAME=$(GITHUB_USERNAME) --build-arg GITHUB_ACCESSTOKEN=$(GITHUB_ACCESSTOKEN) -t uoo-formular:1.0.4 .

run:
    docker-compose up -d

logs:
    docker-compose logs -f

shutdown:
    docker-compose down

restart:
    docker-compose down --remove-orphans --force && docker-compose up -d
    
