/**
 * ============================================================
 * ⚡ SERVER.JS — HTTP SERVER & CLUSTER MANAGEMENT v1.0
 * ============================================================
 *
 * PURPOSE:
 * - Start HTTP server
 * - Cluster mode for multi-core utilization
 * - Graceful shutdown handling
 * - Process monitoring and auto-restart
 *
 * SCALE TARGET:
 * - 50M+ concurrent requests
 * - Zero-downtime deployments
 * - Automatic worker recovery
 *
 * ============================================================
 */

const cluster = require('cluster');
const os = require('os');
const mongoose = require('mongoose');
const dotenv = require('dotenv');
const path = require('path');

// Load environment variables from correct path
dotenv.config({ path: path.join(__dirname, '.env') });

// ============================================================
// CONFIG
// ============================================================

const PORT = process.env.PORT || 5004;
const ENVIRONMENT = process.env.NODE_ENV || 'development';
const CLUSTER_MODE = process.env.CLUSTER_MODE !== 'false';
const WORKER_COUNT = parseInt(process.env.WORKER_COUNT) || Math.max(1, os.cpus().length);
const GRACEFUL_SHUTDOWN_TIMEOUT = parseInt(process.env.GRACEFUL_SHUTDOWN_TIMEOUT) || 30000;

// ============================================================
// 🚀 CLUSTER MODE (Multi-core utilization)
// ============================================================

if (CLUSTER_MODE && cluster.isMaster && ENVIRONMENT === 'production') {
    console.log(`[MASTER] 🚀 Starting master process (PID: ${process.pid})`);
    console.log(`[MASTER] 📊 Creating ${WORKER_COUNT} workers on ${os.cpus().length} cores`);
    console.log(`[MASTER] 🌍 Environment: ${ENVIRONMENT}`);

    // Fork workers
    for (let i = 0; i < WORKER_COUNT; i++) {
        const worker = cluster.fork();
        console.log(`[MASTER] ✅ Worker ${worker.id} started (PID: ${worker.process.pid})`);
    }

    // Track worker exits
    cluster.on('exit', (worker, code, signal) => {
        console.error(`[MASTER] ❌ Worker ${worker.id} died (PID: ${worker.process.pid}, Code: ${code}, Signal: ${signal})`);

        // Restart worker
        console.log(`[MASTER] 🔄 Restarting worker ${worker.id}...`);
        const newWorker = cluster.fork();
        console.log(`[MASTER] ✅ Worker ${newWorker.id} restarted (PID: ${newWorker.process.pid})`);
    });

    // Handle master process signals
    process.on('SIGTERM', () => {
        console.log('[MASTER] 🔒 Received SIGTERM, shutting down workers gracefully...');

        const workers = Object.values(cluster.workers);
        let completed = 0;

        if (workers.length === 0) {
            process.exit(0);
        }

        workers.forEach(worker => {
            worker.send('shutdown');
            worker.on('exit', () => {
                completed++;
                if (completed === workers.length) {
                    console.log('[MASTER] ✅ All workers shut down');
                    process.exit(0);
                }
            });
        });

        // Force exit after timeout
        setTimeout(() => {
            console.error('[MASTER] ⏰ Timeout reached, forcing exit');
            process.exit(1);
        }, GRACEFUL_SHUTDOWN_TIMEOUT);
    });

    process.on('SIGINT', () => {
        process.emit('SIGTERM');
    });

} else {
    // ============================================================
    // 🚀 WORKER PROCESS (Single or clustered)
    // ============================================================

    // Use absolute path for app import to handle spaces in folder name
    const appPath = path.join(__dirname, 'app.js');
    const app = require(appPath);
    let server = null;
    let isShuttingDown = false;

    // ============================================================
    // 📦 IMPORT DB HELPER FOR ENHANCED CONNECTION MANAGEMENT
    // ============================================================
    const { ensureDBConnection, getDBMetrics, healthCheck: dbHealthCheck } = require('./utils/dbHelper');

    /**
     * Graceful shutdown handler
     */
    const gracefulShutdown = async (signal) => {
        if (isShuttingDown) {
            console.log(`[WORKER] ⏳ Already shutting down, ignoring ${signal}`);
            return;
        }

        isShuttingDown = true;
        console.log(`[WORKER] 🔒 Received ${signal}, starting graceful shutdown...`);

        // Set timeout to force exit
        const forceExitTimeout = setTimeout(() => {
            console.error('[WORKER] ⏰ Graceful shutdown timeout, forcing exit');
            process.exit(1);
        }, GRACEFUL_SHUTDOWN_TIMEOUT);

        try {
            // Stop accepting new connections
            if (server) {
                console.log('[WORKER] 🛑 Stopping HTTP server...');
                await new Promise((resolve) => {
                    server.close(resolve);
                });
                console.log('[WORKER] ✅ HTTP server closed');
            }

            // Close database connection (using dbHelper disconnect if available)
            if (mongoose.connection && mongoose.connection.readyState === 1) {
                console.log('[WORKER] 🛑 Closing MongoDB connection...');
                await mongoose.disconnect();
                console.log('[WORKER] ✅ MongoDB disconnected');
            }

            // Try to close tracing if available
            try {
                const tracingPath = path.join(__dirname, 'monitoring', 'tracing.js');
                const { shutdown: shutdownTracing } = require(tracingPath);
                if (shutdownTracing) await shutdownTracing();
            } catch (e) {
                // Tracing not available, ignore
            }

            console.log('[WORKER] ✅ Graceful shutdown complete');

            clearTimeout(forceExitTimeout);
            process.exit(0);
        } catch (error) {
            console.error('[WORKER] ❌ Error during graceful shutdown:', error);
            process.exit(1);
        }
    };

    /**
     * Start server with connection retries (Enhanced with dbHelper)
     */
    const startServer = async (retries = 5, delay = 2000) => {
        let attempt = 0;

        while (attempt < retries) {
            try {
                // Connect to database using dbHelper (enhanced connection management)
                await ensureDBConnection();
                console.log('[WORKER] ✅ MongoDB connected (via dbHelper)');

                // Start HTTP server
                server = app.listen(PORT, () => {
                    console.log(`[WORKER] 🚀 Payment Service listening on port ${PORT}`);
                    console.log(`[WORKER] 🆔 Worker PID: ${process.pid}`);
                    console.log(`[WORKER] 🌍 Environment: ${ENVIRONMENT}`);
                    console.log(`[WORKER] 🔧 Cluster mode: ${CLUSTER_MODE && ENVIRONMENT === 'production' ? 'Enabled' : 'Disabled'}`);

                    // Log database health status on startup
                    const dbHealth = dbHealthCheck();
                    console.log(`[WORKER] 🗄️ Database health: ${dbHealth.status}, Circuit: ${dbHealth.circuitState}, Score: ${dbHealth.healthScore}`);
                });

                // Handle server errors
                server.on('error', (error) => {
                    if (error.code === 'EADDRINUSE') {
                        console.error(`[WORKER] ❌ Port ${PORT} is already in use`);
                        process.exit(1);
                    } else {
                        console.error('[WORKER] ❌ Server error:', error);
                    }
                });

                break;
            } catch (error) {
                attempt++;
                console.error(`[WORKER] ❌ Connection attempt ${attempt}/${retries} failed:`, error.message);

                if (attempt === retries) {
                    console.error('[WORKER] 💀 All connection attempts failed, exiting...');
                    process.exit(1);
                }

                console.log(`[WORKER] ⏳ Retrying in ${delay}ms...`);
                await new Promise(resolve => setTimeout(resolve, delay));
            }
        }
    };

    // ============================================================
    // 🧠 PROCESS EVENT HANDLERS
    // ============================================================

    // Handle shutdown signals
    process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
    process.on('SIGINT', () => gracefulShutdown('SIGINT'));

    // Handle uncaught exceptions
    process.on('uncaughtException', (error) => {
        console.error('[WORKER] 💥 Uncaught Exception:', error);
        gracefulShutdown('uncaughtException');
    });

    // Handle unhandled promise rejections
    process.on('unhandledRejection', (reason) => {
        console.error('[WORKER] 💥 Unhandled Rejection:', reason);
        // DON'T shutdown in development - just log
        if (ENVIRONMENT !== 'production') {
            console.log('[WORKER] Continuing despite unhandled rejection (development mode)');
        } else {
            gracefulShutdown('unhandledRejection');
        }
    });

    // Handle worker messages (from master)
    process.on('message', (msg) => {
        if (msg === 'shutdown') {
            gracefulShutdown('master-shutdown');
        }
    });

    // Start the server
    startServer();
}

// ============================================================
// EXPORTS (for testing)
// ============================================================

const appPathForExport = path.join(__dirname, 'app.js');
module.exports = { app: require(appPathForExport) };