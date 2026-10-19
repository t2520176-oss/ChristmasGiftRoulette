// A Worker entry point exports only handlers. Testable implementation helpers
// live in app.ts so Wrangler never treats constants as service entry points.
export { default } from './app';
