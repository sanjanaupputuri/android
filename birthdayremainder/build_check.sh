#!/bin/bash

echo "Birthday Reminder - Build Check"
echo "==============================="

# Check if google-services.json exists
if [ ! -f "google-services.json" ]; then
    echo "❌ google-services.json not found"
    echo "   Copy your google-services.json file to this directory"
    echo "   Or rename google-services.json.template and fill in your values"
else
    echo "✅ google-services.json found"
fi

# Check if Web Client ID is configured
if grep -q "YOUR_WEB_CLIENT_ID_HERE" src/main/java/com/example/birthdayremainder/MainActivity.kt; then
    echo "❌ Web Client ID not configured"
    echo "   Update the webClientId in CalendarService class"
elif grep -q "demo-client-id" src/main/java/com/example/birthdayremainder/MainActivity.kt; then
    echo "⚠️  Using demo Web Client ID (app will build but Google Calendar features limited)"
    echo "   For full functionality, configure your actual Web Client ID"
else
    echo "✅ Web Client ID appears to be configured"
fi

# Check package name consistency
echo ""
echo "Package name check:"
grep -n "package\|applicationId\|namespace" build.gradle.kts src/main/AndroidManifest.xml src/main/java/com/example/birthdayremainder/MainActivity.kt | head -5

echo ""
echo "Build recommendations:"
echo "1. Use Android Studio to build (recommended)"
echo "2. Test on a real device (not emulator) for Google Sign-In"
echo "3. Check SETUP_INSTRUCTIONS.md for Google Calendar setup"
echo "4. The app works without Google Calendar integration"

echo ""
echo "To build: ./gradlew assembleDebug"
