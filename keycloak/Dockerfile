FROM quay.io/keycloak/keycloak:23.0 AS builder

ARG FRONTEND_URL=https://frontend-secto-front-updated.vlxbym.easypanel.host

ENV KC_DB=postgres
ENV KC_PROXY=edge
ENV KC_HEALTH_ENABLED=true
ENV KC_HTTP_RELATIVE_PATH=/
ENV KC_FEATURES=declarative-user-profile

COPY themes/sectotech /opt/keycloak/themes/sectotech

RUN sed -i "s|__FRONTEND_URL__|${FRONTEND_URL}|g" /opt/keycloak/themes/sectotech/login/theme.properties

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:23.0

COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized", "--proxy=edge", "--hostname-strict=false"]
