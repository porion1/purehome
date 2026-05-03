// ============================================
// 🚀 SERVER - Fixed FAANG Level Entry Point
// ============================================

const mongoose = require('mongoose');
const { createApp } = require('./app');
const logger = require('./utils/logger');

const PORT = process.env.PORT || 5006;

const startServer = async () => {
    try {
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGO_URI, {
            maxPoolSize: 50,
            minPoolSize: 10,
            serverSelectionTimeoutMS: 5000
        });
        logger.info('SERVER', 'Connected to MongoDB');
        
        // Create and start app
        const app = createApp();
        const server = app.listen(PORT, () => {
            logger.info('SERVER', `Notification service running on port ${PORT}`);
            console.log(`✅ Notification service running on port ${PORT}`);
        });
        
        // Graceful shutdown
        process.on('SIGTERM', () => {
            logger.info('SERVER', 'Received SIGTERM, closing...');
            server.close(async () => {
                await mongoose.disconnect();
                process.exit(0);
            });
        });
        
        process.on('uncaughtException', (err) => {
            logger.error('SERVER', 'Uncaught exception', err);
            server.close(() => process.exit(1));
        });
        
    } catch (error) {
        logger.error('SERVER', 'Failed to start server', error);
        console.error('Failed to start:', error.message);
        process.exit(1);
    }
};

startServer();

module.exports = { startServer };