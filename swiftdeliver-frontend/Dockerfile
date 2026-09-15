FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG NG_APP_API_URL=https://your-render-backend-url.onrender.com/api
RUN sed -i "s|https://your-render-backend-url.onrender.com/api|${NG_APP_API_URL}|g" src/environments/environment.prod.ts \
  && npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/del/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]