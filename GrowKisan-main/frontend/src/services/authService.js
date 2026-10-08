import api from './api'

// Request an OTP to be sent to the given mobile number (backend calls Twilio).
export async function sendOtp(mobile) {
  const res = await api.post('/auth/send-otp', { mobile })
  return res.data
}

// Verify the OTP. On success the backend returns { token, user }.
export async function verifyOtp(mobile, code) {
  const res = await api.post('/auth/verify-otp', { mobile, code })
  return res.data
}
