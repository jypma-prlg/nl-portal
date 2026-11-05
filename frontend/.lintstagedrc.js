module.exports = {
  "*.{js,jsx,ts,tsx}": [
    // Brings the PinkRoccade copyright year of committed files up to date (ADR 0015).
    "eslint --config eslint.copyright-year.config.mjs --fix --no-warn-ignored",
    "eslint",
    "prettier --check",
  ],
  "**/*.{ts,tsx}": [
    () => "tsc --noEmit --project ./packages/user-interface/tsconfig.json",
  ],
  "*.{css,scss}": ["stylelint --allow-empty-input", "prettier --check"],
};
