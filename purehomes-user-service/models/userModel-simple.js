const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
    name: String,
    email: { type: String, unique: true },
    password: String,
    isGuest: { type: Boolean, default: false },
    emailVerified: { type: Boolean, default: false },
    lastLoginAt: Date,
    loginCount: { type: Number, default: 0 }
}, {
    timestamps: true
});

// NO middleware, NO virtuals, NO statics, NO indexes except email

const User = mongoose.model('purehomes_users', userSchema);

module.exports = User;