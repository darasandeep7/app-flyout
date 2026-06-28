# Development Progress

## Implemented

- Multi-module Gradle project layout.
- Spring Boot backend entrypoint.
- Plugin registry and `AtlasPlugin` extension contract.
- Property Plugin registered as the first plugin.
- Ollama model catalog and generation adapter with graceful fallback.
- Browser automation interface with Python Playwright worker.
- Local project storage under `workspace/` with SQLite project index.
- Project memory write path.
- REST endpoints for plugins, projects, settings, prompts, playground, and property analysis.
- Built-in agent registry with Manager, Browser, Vision, Marketing, Writer, Video Planner, and Memory agents.
- Property Plugin writes JSON, Markdown, a simple PDF brochure, and a zipped project package.
- React/Vite/Tailwind UI scaffold.
- Startup script and install guide.

## Extension Points

- Add plugins under `plugins/<plugin-name>` and implement `AtlasPlugin`.
- Add agent implementations behind core orchestration services.
- Replace direct Ollama REST adapter with Spring AI once the target Spring AI version is pinned.
- Expand browser worker to download listing images and structured assets.
- Generate PDF from brochure markdown with a local renderer.

## Known Local Environment Gaps

- This shell currently has Java 17, while Atlas targets Java 21.
- `node` and `gradle` are not currently available in `PATH`.
