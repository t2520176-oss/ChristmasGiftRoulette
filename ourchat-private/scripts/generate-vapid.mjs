import webpush from "web-push";

// This command is explicitly run by the administrator. Nothing is written to disk.
const { publicKey, privateKey } = webpush.generateVAPIDKeys();

process.stdout.write(
  [
    "# Copy these values to .env.local and your hosting environment variables.",
    "# Keep the private key secret. Do not commit this output or rotate casually.",
    `NEXT_PUBLIC_VAPID_PUBLIC_KEY=${publicKey}`,
    `VAPID_PRIVATE_KEY=${privateKey}`,
    "# Set VAPID_SUBJECT separately to mailto: plus your real administrator email.",
    "",
  ].join("\n"),
);
