import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    name: "eslint-rules",
    root: import.meta.dirname,
    include: ["*.test.ts"],
  },
});
