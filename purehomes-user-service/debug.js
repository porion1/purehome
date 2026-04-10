const mongoose = require('mongoose');
require('dotenv').config();

async function debug() {
    console.log('=========================================');
    console.log('DEBUGGING USER MODEL');
    console.log('=========================================\n');

    console.log('1. Starting debug...');
    console.log('2. MONGO_URI:', process.env.MONGO_URI);

    try {
        console.log('\n--- TEST 1: Simple Model ---');
        console.log('3. Attempting to connect to MongoDB...');
        await mongoose.connect(process.env.MONGO_URI);
        console.log('4. ✅ MongoDB connected!');

        // Clear any existing models
        mongoose.models = {};
        mongoose.modelSchemas = {};

        // Test simple model
        const SimpleUser = require('./models/userModel-simple');
        console.log('5. ✅ Simple User model loaded');

        console.log('6. Attempting to create guest user with simple model...');
        const crypto = require('crypto');
        const guestId = crypto.randomBytes(8).toString('hex');

        const simpleUser = await SimpleUser.create({
            name: `Guest_${guestId.slice(0, 6)}`,
            email: `guest_${guestId}@temp.local`,
            isGuest: true
        });
        console.log('7. ✅ Simple model created user:', simpleUser._id);

        // Close connection and clear models
        await mongoose.connection.close();
        console.log('8. ✅ Disconnected from MongoDB\n');

        // Wait a bit before reconnecting
        await new Promise(resolve => setTimeout(resolve, 100));

        console.log('--- TEST 2: Fixed User Model ---');
        console.log('9. Reconnecting to MongoDB...');
        await mongoose.connect(process.env.MONGO_URI);
        console.log('10. ✅ MongoDB reconnected!');

        // Clear models again before loading fixed model
        delete require.cache[require.resolve('./models/userModel')];
        mongoose.models = {};
        mongoose.modelSchemas = {};

        const FixedUser = require('./models/userModel');
        console.log('11. ✅ Fixed User model loaded');

        console.log('12. Attempting to create guest user with fixed model...');
        const guestId2 = crypto.randomBytes(8).toString('hex');

        const fixedUser = await FixedUser.create({
            name: `Guest_${guestId2.slice(0, 6)}`,
            email: `guest_${guestId2}@temp.local`,
            isGuest: true,
            emailVerified: false,
            lastLoginAt: new Date()
        });
        console.log('13. ✅ Fixed model created user:', fixedUser._id);

        console.log('14. Disconnecting...');
        await mongoose.disconnect();
        console.log('15. ✅ All tests passed!');

    } catch (err) {
        console.error('❌ Error:', err.message);
        console.error('Full error:', err);
    } finally {
        if (mongoose.connection.readyState === 1) {
            await mongoose.disconnect();
        }
    }
}

debug();