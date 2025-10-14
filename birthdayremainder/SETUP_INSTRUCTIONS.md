# Birthday Reminder Setup Instructions

## Google Calendar Integration Setup

### 1. Create Google Cloud Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable the Google Calendar API:
   - Go to "APIs & Services" > "Library"
   - Search for "Google Calendar API"
   - Click "Enable"

### 2. Configure OAuth Consent Screen
1. Go to "APIs & Services" > "OAuth consent screen"
2. Choose "External" user type
3. Fill in required fields:
   - App name: "Birthday Reminder"
   - User support email: your email
   - Developer contact: your email
4. Add scopes:
   - `https://www.googleapis.com/auth/calendar.readonly`
5. Add test users (your Gmail account)

### 3. Create OAuth Credentials
1. Go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "OAuth client ID"
3. Choose "Web application"
4. Add authorized redirect URIs (if needed)
5. Copy the Web Client ID

### 4. Create Android OAuth Client
1. In the same "Credentials" page
2. Click "Create Credentials" > "OAuth client ID"
3. Choose "Android"
4. Get your SHA-1 fingerprint:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Enter package name: `com.example.birthdayremainder`
6. Enter SHA-1 fingerprint

### 5. Download google-services.json
1. Go to "Project Settings" (gear icon)
2. Select your Android app
3. Download `google-services.json`
4. Place it in the `app/` directory (same level as `build.gradle.kts`)

### 6. Update Code Configuration
1. Open `MainActivity.kt`
2. Find the `CalendarService` class
3. Replace `YOUR_WEB_CLIENT_ID_HERE` with your actual Web Client ID from step 3

### 7. Build and Test
1. Clean and rebuild the project
2. Run on a device (not emulator for Google Sign-In)
3. Test the "Connect" button

## Troubleshooting

### Common Issues:
1. **"Developer error"**: Check OAuth client ID configuration
2. **"Sign-in cancelled"**: User cancelled the flow
3. **"Network error"**: Check internet connection
4. **No birthdays found**: 
   - Make sure you have birthday events in Google Calendar
   - Check if contacts birthdays are synced to calendar

### Debug Steps:
1. Check Android Studio logs for detailed error messages
2. Verify package name matches in all configurations
3. Ensure SHA-1 fingerprint is correct
4. Test with a real device (not emulator)

## Features

### Current Features:
- ✅ Local birthday storage
- ✅ Read birthdays from Google Calendar
- ✅ Month-wise filtering
- ✅ Beautiful Material Design UI

### Planned Features:
- 🔄 Write birthdays to Google Calendar (requires additional permissions)
- 🔄 Birthday notifications
- 🔄 Backup/restore functionality

## Permissions Required:
- `INTERNET`: For Google API calls
- `READ_CALENDAR`: To read calendar events
- `GET_ACCOUNTS`: For Google account access

## Notes:
- The app currently works in read-only mode for Google Calendar
- All manually added birthdays are stored locally
- Google Calendar integration requires proper OAuth setup
