# Try this build on your phone

This is a test copy, not a Play Store app. It uses a debug key. Your real statements stay on the phone.

1. Copy `release-artifacts/gud-arm64-debug.apk` onto the phone (USB, or the download from this run).
2. Open the file. If Android asks, allow install from that source (Files or Chrome).
3. Tap Install. If an older GrowUrDhan test build is in the way, uninstall that test copy first. Do not uninstall a vault you have not backed up.
4. Or, from a computer with the phone connected: `adb install -r gud-arm64-debug.apk`.

Then about 15 minutes:

- Create the app password. The first screen should also ask for your name, date of birth, two security questions, PAN, and consent before the books open. PAN is required because the account record on the phone needs it.
- The app asks for a new password every 180 days. If you forget it, the offline recovery code plus the phone screen lock can set a new one. That does not wipe the vault.
- Lock the phone, open the app, unlock again.
- Add one family member. On Insights and on Record a transaction, choose one person and confirm the other person's holding name is hidden.
- Add one holding in each of the eight kinds (shares, mutual fund, gold, fixed income, property, specialised, bank cash, loan).
- Record a buy and a sell on one holding. For a loan, the opening balance is the outstanding amount, not a buy.
- Look at Home. Amounts should stay on one line. They are saved book values, not today’s market price.
- Import your own CAS PDF if you want. It is not uploaded by this checklist. Do not send the PDF to anyone.
- Make an encrypted backup and restore it only on a spare copy or after you understand that restore replaces the books.
- Turn on dark mode. Enlarge system font. Turn on TalkBack and move through Home and Settings.
- Note anything wrong, with a photo of the screen.

The computer test did not open a real PDF and did not run this APK on a phone. Those checks are yours.
