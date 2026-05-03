# ============================================
# 🐳 DOCKERFILE - PureHomes Gateway X1
# ============================================

FROM node:20-alpine

# Install dumb-init and nodemon globally
RUN apk add --no-cache dumb-init && \
    npm install -g nodemon

# Create non-root user
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nodejs -u 1001

WORKDIR /app

# Copy package files first
COPY package*.json ./

# Install ALL dependencies (including dev)
RUN npm install

# Copy source code
COPY . .

# Switch to non-root user
USER nodejs

# Expose gateway port
EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD node -e "require('http').get('http://localhost:3000/health/live', (r) => {r.statusCode === 200 ? process.exit(0) : process.exit(1)})"

ENTRYPOINT ["dumb-init", "--"]

# Start gateway with nodemon
CMD ["nodemon", "src/server.js"]