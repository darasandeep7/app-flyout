# Development Progress

## Implemented

- Multi-module Gradle project layout.
- Added `atlas-common`, `atlas-prompts`, and `atlas-settings` shared modules.
- Spring Boot backend entrypoint.
- Plugin registry and `AtlasPlugin` extension contract.
- Career Copilot registered as the primary plugin.
- Property Plugin registered as the first plugin.
- Career company knowledge base under `workspace/career/companies`.
- Career job analysis under `workspace/career/jobs`.
- Career Intelligence Engine modules: `career-intelligence`, `company-intelligence`, `resume-intelligence`, `visa-intelligence`, `job-ranking`, `recommendation-engine`, `daily-briefing`, and `career-learning`.
- Visa Intelligence scoring for sponsorship restriction language.
- Match Engine for Java, Spring, Snowflake, backend, leadership, salary, location, visa, and interview probability scores.
- Job Intelligence ranking, duplicate confidence, recommendation categories, daily briefing, resume health, and learning statistics endpoints.
- Versioned SQLite migration script for Career Intelligence tables.
- Daily preparation workflow creates local application packages with resume draft, cover letter draft, answer drafts, and review report.
- Application Review Queue with approval status for future Browser Agent handoff.
- Local Career Preferences are stored under `workspace/career/preferences` and drive scoring, visa filtering, queue thresholds, company lists, and daily application caps.
- Master Resume storage is available under `workspace/career/resumes` with version snapshots and resume health scoring.
- Ollama model catalog and generation adapter with graceful fallback.
- Browser automation interface with Python Playwright worker.
- Local project storage under `workspace/` with SQLite project index.
- Project memory write path.
- REST endpoints for plugins, projects, settings, prompts, playground, and property analysis.
- Built-in agent registry with Manager, Browser, Vision, Marketing, Writer, Video Planner, and Memory agents.
- Property Plugin writes JSON, Markdown, a simple PDF brochure, and a zipped project package.
- React/Vite/Tailwind UI scaffold.
- Career-first React UI connected to Career Copilot backend endpoints.
- Career Intelligence Dashboard with recommendation queue, company insights, visa insights, resume health, learning funnel, and job ranking table.
- Dashboard can trigger daily queue preparation and approve application packages for the future Browser Agent.
- Dashboard can edit Career Preferences used by the local job copilot.
- Real job discovery can import career pages, scan tracked companies, classify common ATS platforms, dedupe discovered jobs, remove expired scanner jobs, and store analyzed postings locally.
- Startup script and install guide.
- Gradle wrapper files so the project does not require a global Gradle install.
- Gradle Java toolchain resolver so Java 21 can be provisioned by Gradle when missing locally.

## Extension Points

- Add plugins under `plugins/<plugin-name>` and implement `AtlasPlugin`.
- Add agent implementations behind core orchestration services.
- Replace direct Ollama REST adapter with Spring AI once the target Spring AI version is pinned.
- Expand browser worker to download listing images and structured assets.
- Generate PDF from brochure markdown with a local renderer.

## Known Local Environment Gaps

- This shell currently launches with Java 17; Gradle is configured to resolve a Java 21 toolchain.
- `node` is not currently available in `PATH`.
