import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    id: "/chat",
    name: "OurChat",
    short_name: "OurChat",
    description: "A private conversation for the two of you.",
    lang: "en",
    start_url: "/chat",
    scope: "/",
    display: "standalone",
    background_color: "#0b3b8f",
    theme_color: "#0b3b8f",
    icons: [
      { src: "/icons/icon-192.png", sizes: "192x192", type: "image/png", purpose: "any" },
      { src: "/icons/icon-512.png", sizes: "512x512", type: "image/png", purpose: "any" },
      {
        src: "/icons/icon-maskable-512.png",
        sizes: "512x512",
        type: "image/png",
        purpose: "maskable",
      },
    ],
  };
}
