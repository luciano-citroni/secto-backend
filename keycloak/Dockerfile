FROM quay.io/keycloak/keycloak:23.0 AS builder

ENV KC_DB=postgres
ENV KC_PROXY=edge
ENV KC_HEALTH_ENABLED=true
ENV KC_HTTP_RELATIVE_PATH=/
ENV KC_FEATURES=declarative-user-profile

COPY themes/sectotech /opt/keycloak/themes/sectotech

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:23.0

COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized", "--proxy=edge", "--hostname-strict=false"]
