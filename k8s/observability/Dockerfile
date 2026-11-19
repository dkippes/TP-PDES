FROM debian:trixie-slim AS package

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && mkdir -p /etc/apt/keyrings /tmp/alloy \
    && curl --fail --silent --show-error --location \
        https://apt.grafana.com/gpg.key --output /etc/apt/keyrings/grafana.asc \
    && printf '%s\n' \
        'Types: deb' \
        'URIs: https://apt.grafana.com' \
        'Suites: stable' \
        'Components: main' \
        'Signed-By: /etc/apt/keyrings/grafana.asc B53AE77BADB630A683046005963FA27710458545' \
        > /etc/apt/sources.list.d/grafana.sources \
    && apt-get update \
    && cd /tmp/alloy \
    && apt-get download alloy=1.10.0-1 \
    && dpkg-deb --extract ./*.deb /tmp/package

FROM debian:trixie-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates libsystemd0 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=package /tmp/package/usr/bin/alloy /usr/local/bin/alloy
RUN alloy --version

ENTRYPOINT ["/usr/local/bin/alloy"]
