const express = require('express');
const axios = require('axios');
const mongoose = require('mongoose');
const OTP = require('./models/otpModel');

const RESEND_API_KEY = process.env.RESEND_API_KEY;
const RESEND_FROM_EMAIL = process.env.RESEND_FROM_EMAIL || 'onboarding@resend.dev';
const MONGO_URI = process.env.MONGO_URI || 'mongodb://mongodb:27017/purehomes';

// Connect to MongoDB
mongoose.connect(MONGO_URI);

const createApp = () => {
    const app = express();
    app.use(express.json());

    // Generate 6-digit OTP
    const generateOTP = () => {
        return Math.floor(100000 + Math.random() * 900000).toString();
    };

    // Send OTP via email using Resend
    const sendOTPEmail = async (to, otp) => {
        const html = `
            <!DOCTYPE html>
            <html>
            <head>
                <title>PureHomes Verification Code</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 500px; margin: 0 auto; padding: 20px; }
                    .header { text-align: center; padding-bottom: 20px; border-bottom: 2px solid #4CAF50; }
                    .logo { font-size: 24px; font-weight: bold; color: #4CAF50; }
                    .code { 
                        background-color: #f4f4f4; 
                        padding: 20px; 
                        text-align: center; 
                        font-size: 36px; 
                        letter-spacing: 8px;
                        font-weight: bold;
                        border-radius: 8px;
                        margin: 20px 0;
                    }
                    .footer { text-align: center; font-size: 12px; color: #666; margin-top: 20px; padding-top: 20px; border-top: 1px solid #eee; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">PureHomes</div>
                    </div>
                    <h2>Verification Code</h2>
                    <p>Hello,</p>
                    <p>Your PureHomes verification code is:</p>
                    <div class="code">${otp}</div>
                    <p>This code will expire in <strong>5 minutes</strong>.</p>
                    <p>If you didn't request this, please ignore this email.</p>
                    <div class="footer">
                        <p>&copy; 2024 PureHomes. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
        `;

        const response = await axios.post('https://api.resend.com/emails', {
            from: RESEND_FROM_EMAIL,
            to: [to],
            subject: 'Your PureHomes Verification Code',
            html: html
        }, {
            headers: {
                'Authorization': `Bearer ${RESEND_API_KEY}`,
                'Content-Type': 'application/json'
            },
            timeout: 10000
        });

        return response.data;
    };

    // ============================================
    // 📧 SEND OTP (With Database Storage)
    // ============================================
    app.post('/api/notifications/otp/send', async (req, res) => {
        const { to, channel = 'email', type = 'verification' } = req.body;

        if (!to) {
            return res.status(400).json({
                success: false,
                error: 'MISSING_RECIPIENT',
                message: 'Email address is required'
            });
        }

        try {
            // Check for existing valid OTP
            const existingOTP = await OTP.findOne({
                identifier: to,
                type,
                verified: false,
                expiresAt: { $gt: new Date() }
            });

            if (existingOTP) {
                return res.status(429).json({
                    success: false,
                    error: 'OTP_ALREADY_SENT',
                    message: 'An active OTP already exists. Please wait.',
                    expiresIn: Math.ceil((existingOTP.expiresAt - Date.now()) / 1000)
                });
            }

            const otpCode = generateOTP();
            const expiresAt = new Date(Date.now() + 5 * 60 * 1000); // 5 minutes

            // Save OTP to database
            const otp = new OTP({
                code: otpCode,
                identifier: to,
                type,
                channel,
                expiresAt,
                maxAttempts: 5
            });
            await otp.save();

            console.log(`🔑 Saved OTP for ${to}: ${otpCode} (expires in 5 min)`);

            // Send email
            const result = await sendOTPEmail(to, otpCode);

            console.log(`✅ Email sent successfully: ${result.id}`);

            res.json({
                success: true,
                message: `OTP sent to ${to}`,
                channel,
                expiresIn: 300,
                messageId: result.id
            });

        } catch (error) {
            console.error(`❌ Failed to send email:`, error.message);
            res.status(500).json({
                success: false,
                error: 'SEND_FAILED',
                message: error.message
            });
        }
    });

    // ============================================
    // ✅ VERIFY OTP (With Database Check)
    // ============================================
    app.post('/api/notifications/otp/verify', async (req, res) => {
        const { to, code, type = 'verification' } = req.body;

        if (!to || !code) {
            return res.status(400).json({
                success: false,
                error: 'MISSING_FIELDS',
                message: 'Email and code are required'
            });
        }

        try {
            // Find valid OTP
            const otpRecord = await OTP.findOne({
                identifier: to,
                type,
                verified: false,
                expiresAt: { $gt: new Date() }
            });

            if (!otpRecord) {
                // Check if any OTP exists (maybe expired)
                const expiredOTP = await OTP.findOne({
                    identifier: to,
                    type,
                    expiresAt: { $lt: new Date() }
                });

                if (expiredOTP) {
                    return res.status(400).json({
                        success: false,
                        error: 'OTP_EXPIRED',
                        message: 'OTP has expired. Please request a new one.'
                    });
                }

                return res.status(400).json({
                    success: false,
                    error: 'INVALID_OTP',
                    message: 'Invalid OTP code. Please try again.'
                });
            }

            // Use the model's isValid method
            const result = otpRecord.isValid(code);
            await otpRecord.save();

            if (!result.valid) {
                const errorMsg = result.expired ? 'OTP_EXPIRED' : 'INVALID_OTP';
                const message = result.expired
                    ? 'OTP has expired. Please request a new one.'
                    : `Invalid OTP. ${result.attemptsRemaining} attempts remaining.`;

                return res.status(400).json({
                    success: false,
                    error: errorMsg,
                    message,
                    attemptsRemaining: result.attemptsRemaining
                });
            }

            console.log(`✅ OTP verified successfully for ${to}`);

            // Optionally delete the OTP after successful verification
            await OTP.deleteOne({ _id: otpRecord._id });

            res.json({
                success: true,
                verified: true,
                message: 'OTP verified successfully'
            });

        } catch (error) {
            console.error('Verification error:', error.message);
            res.status(500).json({
                success: false,
                error: 'VERIFY_FAILED',
                message: error.message
            });
        }
    });

    // ============================================
    // 🔄 RESEND OTP
    // ============================================
    app.post('/api/notifications/otp/resend', async (req, res) => {
        const { to, type = 'verification' } = req.body;

        if (!to) {
            return res.status(400).json({
                success: false,
                error: 'MISSING_RECIPIENT',
                message: 'Email address is required'
            });
        }

        try {
            // Deactivate old OTPs
            await OTP.updateMany(
                { identifier: to, type, verified: false },
                { verified: true, verifiedAt: new Date() }
            );

            const otpCode = generateOTP();
            const expiresAt = new Date(Date.now() + 5 * 60 * 1000);

            const otp = new OTP({
                code: otpCode,
                identifier: to,
                type,
                channel: 'email',
                expiresAt,
                maxAttempts: 5
            });
            await otp.save();

            await sendOTPEmail(to, otpCode);

            console.log(`✅ OTP resent to ${to}: ${otpCode}`);

            res.json({
                success: true,
                message: `OTP resent to ${to}`,
                expiresIn: 300
            });

        } catch (error) {
            res.status(500).json({
                success: false,
                error: 'RESEND_FAILED',
                message: error.message
            });
        }
    });

    // ============================================
    // 🔍 CHECK OTP STATUS
    // ============================================
    app.get('/api/notifications/otp/status', async (req, res) => {
        const { to, type = 'verification' } = req.query;

        if (!to) {
            return res.status(400).json({
                success: false,
                error: 'MISSING_EMAIL',
                message: 'Email address is required'
            });
        }

        try {
            const otpRecord = await OTP.findOne({
                identifier: to,
                type,
                verified: false,
                expiresAt: { $gt: new Date() }
            });

            if (!otpRecord) {
                return res.json({
                    success: true,
                    exists: false,
                    message: 'No active OTP found'
                });
            }

            res.json({
                success: true,
                exists: true,
                attemptsRemaining: otpRecord.maxAttempts - otpRecord.attempts,
                expiresIn: Math.max(0, Math.ceil((otpRecord.expiresAt - Date.now()) / 1000))
            });

        } catch (error) {
            res.status(500).json({
                success: false,
                error: 'STATUS_CHECK_FAILED',
                message: error.message
            });
        }
    });

    // ============================================
    // 🏥 Health endpoints
    // ============================================
    app.get('/health/fast', (req, res) => {
        res.json({
            status: 'ok',
            uptime: process.uptime(),
            service: 'notification-service',
            timestamp: new Date().toISOString()
        });
    });

    app.get('/health/live', (req, res) => {
        res.json({ status: 'alive', uptime: process.uptime() });
    });

    app.get('/health/ready', (req, res) => {
        const dbReady = mongoose.connection.readyState === 1;
        res.status(dbReady ? 200 : 503).json({ ready: dbReady });
    });

    app.get('/health', (req, res) => {
        res.json({
            status: 'healthy',
            service: 'notification-service',
            uptime: process.uptime(),
            database: mongoose.connection.readyState === 1 ? 'connected' : 'disconnected',
            resend: RESEND_API_KEY ? 'configured' : 'missing'
        });
    });

    return app;
};

module.exports = { createApp };