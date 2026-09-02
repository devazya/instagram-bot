# Instagram Comment-to-DM Bot

Spring Boot backend that listens for Instagram comments containing a trigger
keyword (e.g. "LINK") and automatically sends the commenter a Private Reply DM
via the Meta Graph API.

## Stack

- Java 17
- Spring Boot 3.2 (Web, Validation)
- Jackson (JSON binding)
- SLF4J + Logback (logging)
- Maven

## File structure

```
instagram-bot/
├── pom.xml
├── .gitignore
├── .env.example
├── README.md
└── src/main/
    ├── java/com/instabot/
    │   ├── InstagramBotApplication.java   # main() entry point, RestTemplate bean
    │   ├── AppConfig.java                 # @EnableAsync
    │   ├── MetaConfig.java                # typed binding of meta.* properties
    │   ├── WebhookController.java         # GET verify + POST receive
    │   ├── CommentProcessorService.java   # keyword matching logic
    │   ├── MetaGraphApiService.java       # outbound Graph API call (Private Reply)
    │   ├── WebhookPayload.java            # top-level JSON DTO
    │   ├── CommentData.java               # entry.changes.value JSON DTO
    │   └── GlobalExceptionHandler.java    # catches stray controller errors
    └── resources/
        ├── application.properties         # config (verify token, page id, etc.)
        └── logback.xml                    # logging config
```

## 1. Configure secrets

Set these as real environment variables (recommended) — see `.env.example`:

| Variable | Where to get it |
|---|---|
| `META_VERIFY_TOKEN` | Any string you make up — you'll type this same value into the Meta dashboard |
| `META_PAGE_ACCESS_TOKEN` | Meta App Dashboard → Instagram → API setup → Generate token (Page must have you added as a Tester) |
| `META_PAGE_ID` | The Facebook Page ID linked to your Instagram professional account |

**Windows (PowerShell):**
```powershell
$env:META_VERIFY_TOKEN="mySecret123"
$env:META_PAGE_ACCESS_TOKEN="EAAG..."
$env:META_PAGE_ID="1234567890"
```

## 2. Run the server

```bash
mvn spring-boot:run
```
Server starts on `http://localhost:8080`.

## 3. Expose it with Ngrok

Meta requires an **HTTPS** URL — your local server isn't reachable from the
internet, so Ngrok creates a secure tunnel to it.

1. Install ngrok: https://ngrok.com/download
2. In a separate terminal, run:
   ```bash
   ngrok http 8080
   ```
3. Ngrok prints a forwarding URL like:
   ```
   Forwarding  https://abcd-1234.ngrok-free.app -> http://localhost:8080
   ```
4. Your webhook URL to paste into Meta is:
   ```
   https://abcd-1234.ngrok-free.app/webhook
   ```
   (Note: this URL changes every time you restart ngrok on the free tier.)

## 4. Register the webhook in Meta

1. Go to your Meta App Dashboard → your app → **Webhooks** (or Instagram →
   Webhooks, depending on product setup).
2. Callback URL: `https://<your-ngrok-domain>/webhook`
3. Verify Token: the same value you set as `META_VERIFY_TOKEN`.
4. Click **Verify and Save** — Meta calls the `GET /webhook` endpoint;
   check the server console log for `"Webhook verified successfully"`.
5. Subscribe to the **comments** field under the Instagram product.

## 5. Add yourself as a Tester

Since the app stays in Development Mode:
- App Dashboard → App roles → Roles → add your Instagram/Facebook account
  as a **Tester** (or Developer/Admin).
- Accept the invite from your Instagram/Facebook account settings.
- Only accounts with a role on the app can trigger/receive events while
  the app is unreviewed.

## 6. Test end-to-end

1. Comment "LINK" (or "SETUP") on one of your own Reels/posts, from a
   tester-role account.
2. Watch the console logs — you should see the comment payload logged,
   the keyword match, and the Graph API response.
3. Check the Instagram DM requests/inbox of the commenting account for
   the private reply.

## Notes on Development Mode limits

- Private Replies (`comment_id` → `/messages`) generally work for
  Tester-role accounts without App Review.
- Comments must be on **your own** connected Instagram professional
  account's media for the webhook to fire.
- If Graph API calls return 4xx errors, check the logged response body —
  it's usually a permissions/scope or expired-token issue.
