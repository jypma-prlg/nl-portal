/*
 * Copyright 2026 PinkRoccade Local Government B.V.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// VitePress's dead-link check only verifies that the target *page* exists. This test also checks
// that "page.md#anchor" links point at a heading (or explicit id) that exists on that page.

import { readdirSync, readFileSync, statSync } from "node:fs";
import { dirname, join, relative, resolve } from "node:path";
import { describe, expect, it } from "vitest";
import { SITE_ROOT } from "./sidebar";

const SKIP_DIRS = new Set(["node_modules", ".vitepress", "img"]);

function markdownFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const abs = join(dir, entry);
    if (statSync(abs).isDirectory()) return SKIP_DIRS.has(entry) ? [] : markdownFiles(abs);
    return entry.endsWith(".md") ? [abs] : [];
  });
}

/** Approximation of VitePress's heading slugs. */
function slugify(heading: string): string {
  return heading
    .replace(/<[^>]+>/g, "")
    .replace(/[*_`]/g, "")
    .replace(/\[([^\]]*)\]\([^)]*\)/g, "$1")
    .trim()
    .toLowerCase()
    .replace(/[^\p{L}\p{N}_\s-]/gu, "")
    .replace(/\s/g, "-");
}

function anchorsOf(source: string): Set<string> {
  const anchors = new Set<string>();
  let inFence = false;
  for (const line of source.split("\n")) {
    if (/^\s*(```|~~~)/.test(line)) inFence = !inFence;
    if (inFence) continue;
    const heading = line.match(/^#{1,6}\s+(.*?)\s*(?:\{#([^}]+)\})?\s*$/);
    if (heading) {
      anchors.add(slugify(heading[1]));
      if (heading[2]) anchors.add(heading[2]);
    }
    for (const id of line.matchAll(/\sid="([^"]+)"/g)) anchors.add(id[1]);
  }
  return anchors;
}

describe("anchor links between pages", () => {
  it("every page.md#anchor link resolves to an existing heading or id", () => {
    const files = markdownFiles(SITE_ROOT);
    const anchorCache = new Map<string, Set<string>>();
    const anchors = (file: string) => {
      if (!anchorCache.has(file)) anchorCache.set(file, anchorsOf(readFileSync(file, "utf8")));
      return anchorCache.get(file)!;
    };

    const broken: string[] = [];
    for (const file of files) {
      let inFence = false;
      for (const line of readFileSync(file, "utf8").split("\n")) {
        if (/^\s*(```|~~~)/.test(line)) inFence = !inFence;
        if (inFence) continue;
        for (const m of line.matchAll(/\]\(([^)\s]*\.md)?#([^)\s]+)\)/g)) {
          const target = m[1] ? resolve(dirname(file), decodeURI(m[1])) : file;
          const anchor = decodeURIComponent(m[2]);
          let ok = false;
          try {
            ok = anchors(target).has(anchor);
          } catch {
            ok = true; // missing page is VitePress's dead-link check's job
          }
          if (!ok) broken.push(`${relative(SITE_ROOT, file)}: ${m[1] ?? ""}#${anchor}`);
        }
      }
    }
    expect(broken).toEqual([]);
  });
});
