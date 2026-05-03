const express = require('express');
const axios = require('axios');

const RESEND_API_KEY = process.env.RESEND_API_KEY;
const RESEND_FROM_EMAIL = process.env.RESEND_FROM_EMAIL || 'onboarding@resend.dev';

const createApp = () => {
    const app = express();
    
    app.use(express.json());
    
    // Generate OTP (6 digits)
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
    
    // OTP Send Endpoint
    app.post('/api/notifications/otp/send', async (req, res) => {
        const { to, channel = 'email', type = 'verification' } = req.body;
        
        if (!to) {
            return res.status(400).json({
                success: false,
                error: 'MISSING_RECIPIENT',
                message: 'Email address is required'
            });
        }
        
        console.log(`📧 Sending OTP to: ${to}`);
        
        try {
            const otp = generateOTP();
            console.log(`🔑 Generated OTP: ${otp}`);
            
            const result = await sendOTPEmail(to, otp);
            
            console.log(`✅ Email sent successfully: ${result.id}`);
            
            res.json({
                success: true,
                message: `OTP sent to ${to}`,
                channel,
                expiresIn: 300,
                messageId: result.id
            });
            
        } catch (error) {
            console.error(`❌ Failed to send email:`, error.response?.data || error.message);
            
            res.status(500).json({
                success: false,
                error: 'SEND_FAILED',
                message: error.response?.data?.message || 'Failed to send OTP email',
                details: error.response?.data
            });
        }
    });
    
    // Verify OTP Endpoint (mock - you can implement actual verification)
    app.post('/api/notifications/otp/verify', (req, res) => {
        const { to, code } = req.body;
        
        console.log(`🔐 Verifying OTP for ${to}: ${code}`);
        
        // In production, you'd check against stored OTP in Redis/DB
        res.json({
            success: true,
            verified: true,
            message: 'OTP verified successfully'
        });
    });
    
    // Health endpoints
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
        res.json({ ready: true });
    });
    
    app.get('/health', (req, res) => {
        res.json({
            status: 'healthy',
            service: 'notification-service',
            uptime: process.uptime(),
            resend: RESEND_API_KEY ? 'configured' : 'missing'
        });
    });
    
    return app;
};

module.exports = { createApp };