

FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./

RUN --mount=type=cache,target=/root/.npm \

    npm ci

COPY . .

RUN npm run build -- --configuration production

FROM nginx:1.27-alpine AS serve

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist/agrezen-angular/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
