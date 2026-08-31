FROM node:20-alpine AS build
WORKDIR /app
ENV PNPM_HOME=/pnpm
ENV PATH=$PNPM_HOME:$PATH
RUN npm install -g pnpm
COPY package.json pnpm-lock.yaml* ./
RUN pnpm install --frozen-lockfile
COPY . .
RUN npx expo export -t standalone

FROM ghcr.io/expo/expo:latest
WORKDIR /app
COPY --from=build /app/dist/ ./
EXPOSE 8081
CMD ["npx", "expo", "start"]