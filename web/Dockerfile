FROM node:22-alpine AS deps
WORKDIR /app
# npm ci, not npm install: it installs exactly the lockfile and fails if the
# lockfile and package.json disagree, which is what you want in a build.
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

FROM node:22-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production

RUN addgroup -S app && adduser -S app -G app
USER app

# 'output: standalone' in next.config.mjs produces a self-contained server with
# only the modules actually reachable at runtime.
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public

EXPOSE 3000
ENV PORT=3000 HOSTNAME=0.0.0.0
CMD ["node", "server.js"]
