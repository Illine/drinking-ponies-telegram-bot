FROM python:3.12-slim-bookworm

RUN pip install --no-cache-dir ansible-core==2.19.4 && \
    apt-get update && \
    apt-get install -y \
      curl \
      git \
      jq \
      ssh \
      sshpass \
    && rm -rf /var/lib/apt/lists/*

COPY .ansible/ansible.cfg /etc/ansible/ansible.cfg