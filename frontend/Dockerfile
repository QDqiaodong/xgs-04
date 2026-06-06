ARG DOCKER_REGISTRY=docker.m.daocloud.io

FROM ${DOCKER_REGISTRY}/library/node:20-alpine AS builder

ARG BACKEND_PORT=9027

WORKDIR /app

COPY .npmrc ./
COPY package.json package-lock.json* ./

RUN npm install

COPY . .

RUN sed -i "s|\${BACKEND_PORT}|${BACKEND_PORT}|g" nginx.conf.template && \
    cp nginx.conf.template nginx.conf

RUN npm run build

FROM ${DOCKER_REGISTRY}/library/nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html
COPY --from=builder /app/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
