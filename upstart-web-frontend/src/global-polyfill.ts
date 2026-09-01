// sockjs-client (lib/utils/browser-crypto.js) references the Node.js `global`
// variable at module load time. Provide a browser-compatible equivalent before
// the application bundle is evaluated.
if (typeof (window as any).global === 'undefined') {
  (window as any).global = window;
}