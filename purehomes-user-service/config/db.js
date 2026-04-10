const mongoose = require('mongoose');
require('dotenv').config();

/**
 * INNOVATION ALGORITHM: Adaptive Jittered Backoff + Circuit Breaker (AJB-CB)
 *
 * This algorithm dynamically adjusts retry delays based on:
 * 1. Consecutive failures (exponential backoff)
 * 2. Random jitter to avoid thundering herd
 * 3. A circuit breaker that opens after 5 failures, then half-opens after a cooldown
 *
 * Unlike standard MongoDB drivers, this predicts network congestion patterns
 * using a weighted moving average of past latencies.
 */
class AdaptiveCircuitBreaker {
    constructor(options = {}) {
        this.failureCount = 0;
        this.lastFailureTime = null;
        this.state = 'CLOSED'; // CLOSED, OPEN, HALF_OPEN
        this.failureThreshold = options.failureThreshold || 5;
        this.cooldownPeriod = options.cooldownPeriod || 30000; // 30 seconds
        this.latencyHistory = [];
        this.maxHistorySize = 10;
    }

    recordSuccess() {
        this.failureCount = 0;
        if (this.state === 'HALF_OPEN') {
            this.state = 'CLOSED';
        }
    }

    recordFailure(error) {
        this.failureCount++;
        this.lastFailureTime = Date.now();

        if (this.state === 'HALF_OPEN') {
            this.state = 'OPEN';
        } else if (this.failureCount >= this.failureThreshold && this.state === 'CLOSED') {
            this.state = 'OPEN';
        }
    }

    recordLatency(latencyMs) {
        this.latencyHistory.push(latencyMs);
        if (this.latencyHistory.length > this.maxHistorySize) {
            this.latencyHistory.shift();
        }
    }

    getPredictedLatency() {
        if (this.latencyHistory.length === 0) return 100;
        const weights = this.latencyHistory.map((_, i) => (i + 1) / this.latencyHistory.length);
        const weightedSum = this.latencyHistory.reduce((sum, lat, i) => sum + lat * weights[i], 0);
        const weightTotal = weights.reduce((a, b) => a + b, 0);
        return weightedSum / weightTotal;
    }

    canAttempt() {
        if (this.state === 'CLOSED') return true;
        if (this.state === 'OPEN') {
            const timeSinceLastFailure = Date.now() - this.lastFailureTime;
            if (timeSinceLastFailure >= this.cooldownPeriod) {
                this.state = 'HALF_OPEN';
                return true;
            }
            return false;
        }
        return this.state === 'HALF_OPEN';
    }

    getBackoffDelay(attempt) {
        if (this.state === 'OPEN') return this.cooldownPeriod;

        const baseDelay = Math.min(100 * Math.pow(2, attempt), 10000);
        const predictedLatency = this.getPredictedLatency();
        const adaptiveFactor = Math.min(2, predictedLatency / 100);
        const jitter = Math.random() * 0.3 * baseDelay;

        return Math.floor(baseDelay * adaptiveFactor + jitter);
    }
}

// Enhanced connection options with production-grade settings
// REMOVED deprecated options: useNewUrlParser and useUnifiedTopology
const getConnectionOptions = () => ({
    serverSelectionTimeoutMS: 5000,
    socketTimeoutMS: 45000,
    family: 4,
    maxPoolSize: parseInt(process.env.MONGO_POOL_SIZE) || 10,
    minPoolSize: parseInt(process.env.MONGO_MIN_POOL_SIZE) || 2,
    maxIdleTimeMS: 30000,
    heartbeatFrequencyMS: 10000,
    retryWrites: true,
    retryReads: true,
});

class MongoDBConnectionManager {
    constructor() {
        this.isConnected = false;
        this.circuitBreaker = new AdaptiveCircuitBreaker();
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 10;
        this.connectionPromise = null;
        this.healthCheckInterval = null;
    }

    async connect() {
        if (this.isConnected && mongoose.connection.readyState === 1) {
            return mongoose.connection;
        }

        if (this.connectionPromise) {
            return this.connectionPromise;
        }

        this.connectionPromise = this._performConnection();
        return this.connectionPromise;
    }

    async _performConnection() {
        const startTime = Date.now();
        let connectionError = null;

        if (!this.circuitBreaker.canAttempt()) {
            const cooldownRemaining = Math.ceil(
                (this.circuitBreaker.cooldownPeriod -
                    (Date.now() - this.circuitBreaker.lastFailureTime)) / 1000
            );
            throw new Error(`Circuit breaker OPEN. Try again in ${cooldownRemaining} seconds`);
        }

        try {
            const mongoUri = process.env.MONGO_URI;
            if (!mongoUri) {
                throw new Error('MONGO_URI not defined in environment variables');
            }

            if (!mongoUri.startsWith('mongodb://') && !mongoUri.startsWith('mongodb+srv://')) {
                throw new Error('Invalid MONGO_URI format');
            }

            const connectionPromise = mongoose.connect(mongoUri, getConnectionOptions());
            const timeoutPromise = new Promise((_, reject) => {
                setTimeout(() => reject(new Error('Connection timeout after 10s')), 10000);
            });

            await Promise.race([connectionPromise, timeoutPromise]);

            const latency = Date.now() - startTime;
            this.circuitBreaker.recordLatency(latency);
            this.circuitBreaker.recordSuccess();

            this.isConnected = true;
            this.reconnectAttempts = 0;

            this._startHealthCheck();
            return mongoose.connection;

        } catch (err) {
            connectionError = err;
            this.circuitBreaker.recordFailure(err);
            this.isConnected = false;

            this.reconnectAttempts++;

            if (this.reconnectAttempts <= this.maxReconnectAttempts) {
                const backoffDelay = this.circuitBreaker.getBackoffDelay(this.reconnectAttempts);
                await this._sleep(backoffDelay);
                this.connectionPromise = null;
                return this.connect();
            }

            throw new Error(`Failed to connect after ${this.maxReconnectAttempts} attempts`);
        } finally {
            if (connectionError) {
                this.connectionPromise = null;
            }
        }
    }

    _startHealthCheck() {
        if (this.healthCheckInterval) clearInterval(this.healthCheckInterval);

        this.healthCheckInterval = setInterval(async () => {
            if (mongoose.connection.readyState !== 1) {
                this.isConnected = false;
                this.connectionPromise = null;
                this.connect().catch(() => {});
            }
        }, 30000);
    }

    _sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    async disconnect() {
        if (this.healthCheckInterval) {
            clearInterval(this.healthCheckInterval);
            this.healthCheckInterval = null;
        }

        if (mongoose.connection.readyState !== 0) {
            await mongoose.disconnect();
            this.isConnected = false;
            this.connectionPromise = null;
        }
    }

    getConnectionStatus() {
        const states = ['disconnected', 'connected', 'connecting', 'disconnecting'];
        return {
            readyState: states[mongoose.connection.readyState] || 'unknown',
            isConnected: this.isConnected,
            circuitBreakerState: this.circuitBreaker.state,
            failureCount: this.circuitBreaker.failureCount,
            predictedLatency: Math.round(this.circuitBreaker.getPredictedLatency()),
            poolSize: mongoose.connection?.client?.options?.maxPoolSize || 'N/A'
        };
    }
}

const connectionManager = new MongoDBConnectionManager();

const connectDB = async () => {
    try {
        const connection = await connectionManager.connect();

        mongoose.connection.on('error', (err) => {
            connectionManager.isConnected = false;
        });

        mongoose.connection.on('disconnected', () => {
            connectionManager.isConnected = false;
            connectionManager.connect().catch(() => {});
        });

        mongoose.connection.on('reconnected', () => {
            connectionManager.isConnected = true;
        });

        return connection;

    } catch (err) {
        throw err;
    }
};

module.exports = connectDB;
module.exports.getConnectionStatus = () => connectionManager.getConnectionStatus();
module.exports.gracefulDisconnect = () => connectionManager.disconnect();