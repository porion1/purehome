FROM node:20-alpine

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init

# Install nodemon globally for hot reload
RUN npm install -g nodemon

# Create app directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies (including dev dependencies for nodemon)
RUN npm ci

# Copy source code
COPY . .

# Create a non-root user to run the app
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nodejs -u 1001

# Change ownership of app files
RUN chown -R nodejs:nodejs /app

# Switch to non-root user
USER nodejs

# Expose port
EXPOSE 5004

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Start the service with nodemon for hot reload
# --legacy-watch helps with file change detection in Docker
# --ignore node_modules prevents unnecessary restarts
CMD ["nodemon", "--legacy-watch", "--ignore", "node_modules", "server.js"]