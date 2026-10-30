# Egress

What can leave the phone. Finances, PAN, DOB, vault keys, and statement PDFs do not.

| Destination | When | What is sent | What is not sent |
|---|---|---|---|
| `https://portal.amfiindia.com/spages/NAVAll.txt` | User refreshes mutual-fund NAV | The NAV file request only | Holdings, PAN, folios |
| `https://nsearchives.nseindia.com/content/cm/BhavCopy_NSE_CM_…` | User refreshes NSE close | The bhavcopy request. Referer `https://www.nseindia.com/all-reports` | Holdings, PAN |
| `https://www.bseindia.com/download/BhavCopy/Equity/BhavCopy_BSE_CM_…` | User refreshes BSE close | The bhavcopy request. Referer `https://www.bseindia.com/` | Holdings, PAN |
| `https://gmail.googleapis.com/gmail/v1/users/me/profile` | Gmail is connected | OAuth profile of the chosen mailbox | Ledger, PAN, PDF bytes |
| `https://www.googleapis.com/auth/gmail.readonly` | Gmail consent | Read-only mail scope. Real consent is BLOCKED-OWNER | Books are not uploaded |
| Google Play Billing | Subscribe or restore `gud_annual` | Play purchase token, handled by Play | Card data, holdings |
| `https://play.google.com/store/apps/details?id=in.growurdhan.app` | Listing URL only. The in-app prompt does not open it | Nothing automatic | Rating is not collected |

Diagnostics are off until the user opts in, stay on the screen, and are not uploaded. Backup folders are the user's own files. No cloud book of finances.
