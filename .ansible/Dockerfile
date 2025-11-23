FROM python:3.12-slim-bookworm

ARG LIQUIBASE_VERSION=4.33.0

RUN apt-get update && \
    apt-get install -y \
      curl \
      git \
      jq \
      openjdk-17-jre-headless \
      ssh \
      sshpass \
    && rm -rf /var/lib/apt/lists/* \
    && pip install --no-cache-dir ansible-core==2.19.4 \
    && curl -fSL "https://github.com/liquibase/liquibase/releases/download/v${LIQUIBASE_VERSION}/liquibase-${LIQUIBASE_VERSION}.tar.gz" \
      -o /tmp/liquibase.tar.gz && \
    mkdir -p /opt/liquibase && \
    tar -xzf /tmp/liquibase.tar.gz -C /opt/liquibase && \
    rm /tmp/liquibase.tar.gz && \
    ln -s /opt/liquibase/liquibase /usr/local/bin/liquibase

COPY .ansible/ansible.cfg /etc/ansible/ansible.cfg