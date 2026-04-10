const mongoose = require('mongoose');
require('dotenv').config();

async function minimalTest() {
    console.log('1. Connecting to MongoDB...');
    await mongoose.connect(process.env.MONGO_URI);
    console.log('2. Connected!');

    // Define a simple schema without any middleware
    const simpleSchema = new mongoose.Schema({
        name: String,
        email: String,
        isGuest: Boolean
    });

    const SimpleUser = mongoose.model('simple_users', simpleSchema);

    console.log('3. Attempting to create user...');
    const user = await SimpleUser.create({
        name: 'Test Guest',
        email: `guest_${Date.now()}@test.com`,
        isGuest: true
    });
    console.log('4. ✅ User created:', user._id);

    console.log('5. Disconnecting...');
    await mongoose.disconnect();
    console.log('6. Done!');
}

minimalTest().catch(console.error);