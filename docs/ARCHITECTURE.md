# Atlas AI Architecture

```mermaid
flowchart LR
  UI["atlas-ui"] --> Core["atlas-core"]
  Core --> AI["atlas-ai"]
  Core --> Browser["atlas-browser"]
  Core --> Memory["atlas-memory"]
  Core --> Storage["atlas-storage"]
  Core --> Prompts["atlas-prompts"]
  Core --> Settings["atlas-settings"]
  Core --> CareerIntel["career-intelligence"]
  CareerIntel --> CompanyIntel["company-intelligence"]
  CareerIntel --> VisaIntel["visa-intelligence"]
  CareerIntel --> Ranking["job-ranking"]
  CareerIntel --> Recs["recommendation-engine"]
  CareerIntel --> ResumeIntel["resume-intelligence"]
  CareerIntel --> Briefing["daily-briefing"]
  CareerIntel --> Learning["career-learning"]
  Core --> Plugins["Plugin API"]
  Career["career-plugin"] --> Plugins
  Career --> CareerIntel
  Property["property-plugin"] --> Plugins
```

Atlas is local-first and plugin-oriented. Career Copilot is now the primary product plugin; Property Copilot remains available as a secondary plugin.

Career Intelligence is intentionally separate from browser automation. It scores, explains, ranks, and recommends before any future ATS workflow is allowed to act.
