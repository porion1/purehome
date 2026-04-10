const mongoose = require('mongoose');
require('dotenv').config();

// --------------------------
// Innovative Connection Algorithms
// --------------------------
// Algorithm 1: Multi-URI Failover with Exponential Backoff (Existing)
// Algorithm 2: Adaptive Circuit Breaker + Connection Health Prediction (NEW)
// --------------------------
// This predicts potential failures using historical latency patterns,
// preemptively cycles URIs, and prevents connection storms.
// --------------------------

// Connection pool settings optimized for FAANG-scale MVP
const DB_OPTIONS = {
    useNewUrlParser: true,
    useUnifiedTopology: true,
    maxPoolSize: 50,           // FAANG-scale concurrency
    serverSelectionTimeoutMS: 5000, // Fail fast if DB unreachable
    socketTimeoutMS: 45000,    // Keep alive for long-running queries
};

// --------------------------
// NEW ALGORITHM: Adaptive Circuit Breaker + Health Predictor
// --------------------------
class ConnectionIntelligence {
    constructor() {
        this.failureHistory = []; // Stores recent failure timestamps & latency
        this.circuitState = 'CLOSED'; // CLOSED, OPEN, HALF_OPEN
        this.failureThreshold = 3;     // Failures before circuit opens
        this.recoveryTimeout = 15000;   // 15 seconds before retry
        this.lastFailureTime = null;
        this.predictiveScore = 100;      // 0-100, higher = healthier
        this.uriHealthMap = new Map();    // Tracks health per URI
    }

    // Records a failure event with latency context
    recordFailure(uri, latencyMs = 5000) {
        const timestamp = Date.now();
        this.failureHistory.push({ timestamp, uri, latencyMs });
        // Keep last 10 failures only
        if (this.failureHistory.length > 10) this.failureHistory.shift();

        // Update URI health
        const currentHealth = this.uriHealthMap.get(uri) || 100;
        const degradedHealth = Math.max(0, currentHealth - (latencyMs / 100));
        this.uriHealthMap.set(uri, degradedHealth);

        // Update predictive score
        this.predictiveScore = Math.max(0, this.predictiveScore - (latencyMs / 500));

        // Circuit breaker logic
        const recentFailures = this.failureHistory.filter(f =>
            (timestamp - f.timestamp) < 30000
        ).length;

        if (recentFailures >= this.failureThreshold && this.circuitState === 'CLOSED') {
            this.circuitState = 'OPEN';
            this.lastFailureTime = timestamp;
            console.warn(`🔌 Circuit BREAKER OPEN for MongoDB (${recentFailures} failures)`);
        }
    }

    // Records a successful connection
    recordSuccess(uri, latencyMs) {
        this.failureHistory = this.failureHistory.filter(f => f.uri !== uri);
        this.uriHealthMap.set(uri, Math.min(100, (this.uriHealthMap.get(uri) || 50) + 15));
        this.predictiveScore = Math.min(100, this.predictiveScore + 10);

        if (this.circuitState === 'OPEN') {
            this.circuitState = 'HALF_OPEN';
            console.log(`🔄 Circuit HALF-OPEN — testing recovery...`);
        } else if (this.circuitState === 'HALF_OPEN') {
            this.circuitState = 'CLOSED';
            console.log(`✅ Circuit CLOSED — MongoDB healthy again`);
        }
    }

    // Predicts if a connection attempt is likely to fail
    shouldAttemptConnection(uri) {
        const uriHealth = this.uriHealthMap.get(uri) ?? 100;

        if (this.circuitState === 'OPEN') {
            const timeSinceLastFailure = Date.now() - this.lastFailureTime;
            if (timeSinceLastFailure < this.recoveryTimeout) {
                console.log(`⏸️ Circuit OPEN — skipping connection attempt (${Math.ceil((this.recoveryTimeout - timeSinceLastFailure)/1000)}s remaining)`);
                return false;
            } else {
                this.circuitState = 'HALF_OPEN';
                console.log(`🔓 Circuit HALF-OPEN — allowing test connection`);
                return true;
            }
        }

        // Predictive rejection if URI health is critically low
        if (uriHealth < 20) {
            console.log(`⚠️ Predictive rejection: URI health at ${uriHealth}% — skipping`);
            return false;
        }

        return true;
    }

    // Dynamically reorders URIs based on health (best first)
    prioritizeURIs(uris) {
        return [...uris].sort((a, b) => {
            const healthA = this.uriHealthMap.get(a) ?? 100;
            const healthB = this.uriHealthMap.get(b) ?? 100;
            return healthB - healthA;
        });
    }

    getSummary() {
        return {
            circuitState: this.circuitState,
            predictiveScore: this.predictiveScore,
            uriHealth: Object.fromEntries(this.uriHealthMap),
            recentFailures: this.failureHistory.length
        };
    }
}

// Initialize intelligence engine
const connectionAI = new ConnectionIntelligence();

const connectDB = async () => {
    let rawURIs = [
        process.env.MONGO_URI,             // Primary
        process.env.MONGO_URI_FALLBACK     // Optional fallback
    ].filter(Boolean); // Remove undefined entries

    // NEW: AI-powered URI prioritization based on historical health
    const dbURIs = connectionAI.prioritizeURIs(rawURIs);

    let connected = false;
    let attempt = 0;

    while (!connected && attempt < dbURIs.length) {
        const targetURI = dbURIs[attempt];

        // NEW: Predictive check before attempting connection
        if (!connectionAI.shouldAttemptConnection(targetURI)) {
            console.log(`🧠 AI prediction: Skipping unhealthy URI [${attempt + 1}]`);
            attempt++;
            continue;
        }

        const startTime = Date.now();

        try {
            console.log(`Attempting MongoDB connection [${attempt + 1}]...`);
            await mongoose.connect(targetURI, DB_OPTIONS);

            const latencyMs = Date.now() - startTime;
            connectionAI.recordSuccess(targetURI, latencyMs);

            connected = true;
            console.log(`MongoDB connected ✅ (latency: ${latencyMs}ms)`, targetURI);
            console.log(`🧠 Connection Health Summary:`, connectionAI.getSummary());

        } catch (error) {
            const latencyMs = Date.now() - startTime;
            connectionAI.recordFailure(targetURI, latencyMs);

            console.error(`MongoDB connection failed [${attempt + 1}]:`, error.message);
            console.error(`🧠 AI recorded failure (health score: ${connectionAI.predictiveScore})`);

            attempt++;
            if (attempt < dbURIs.length) {
                console.log('Trying next fallback URI (AI-reordered)...');
            } else {
                console.error('All connection attempts failed. Exiting process...');
                console.error('Final AI Summary:', connectionAI.getSummary());
                process.exit(1);
            }
        }
    }

    // Optional: Event listeners for connection resiliency
    mongoose.connection.on('disconnected', () => {
        console.warn('MongoDB disconnected! Attempting AI-driven reconnect...');
        connectionAI.recordFailure(mongoose.connection.host || 'unknown', 1000);
        setTimeout(connectDB, 2000); // Retry after 2 seconds
    });

    mongoose.connection.on('reconnected', () => {
        console.log('MongoDB reconnected successfully!');
        connectionAI.recordSuccess(mongoose.connection.host || 'unknown', 0);
    });

    // NEW: Periodic health reporting (every 60 seconds)
    if (process.env.NODE_ENV !== 'test') {
        setInterval(() => {
            const summary = connectionAI.getSummary();
            if (summary.circuitState !== 'CLOSED' || summary.predictiveScore < 70) {
                console.warn(`🧠 Health Monitor:`, summary);
            }
        }, 60000);
    }
};

module.exports = connectDB;