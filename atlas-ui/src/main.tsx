import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Bot,
  Boxes,
  BrainCircuit,
  BriefcaseBusiness,
  Building2,
  ClipboardCheck,
  FileText,
  FolderKanban,
  Image,
  Play,
  Settings,
  ShieldCheck,
  Terminal
} from "lucide-react";
import "./styles.css";

type NavKey = "career" | "dashboard" | "property" | "playground" | "projects" | "settings" | "logs";

type CareerDashboard = {
  trackedCompanies: number;
  newJobs: number;
  excellentMatches: number;
  applicationsReady: number;
  blockedByVisa: number;
  topMatches: JobRecord[];
  topCompanies: CompanyRecord[];
};

type CompanyRecord = {
  id: string;
  name: string;
  careerUrl: string;
  visaConfidence: number;
  locations: string[];
  priority: number;
  blocked: boolean;
  notes: string;
};

type JobRecord = {
  id: string;
  company: string;
  title: string;
  location: string;
  url: string;
  visa: { score: number; confidence: number; recommendation: string; reason: string; detectedSignals: string[] };
  match: { overallMatch: number; resumeMatch: number; javaMatch: number; springMatch: number; backendMatch: number; visaMatch: number; interviewProbability: number; explanations: Record<string, string> };
  applicationStatus: string;
  resumeReady: boolean;
  coverLetterReady: boolean;
};

function App() {
  const [active, setActive] = useState<NavKey>("career");
  const nav = [
    ["career", BriefcaseBusiness, "Career"],
    ["dashboard", Bot, "Core"],
    ["property", Boxes, "Property"],
    ["playground", BrainCircuit, "Playground"],
    ["projects", FolderKanban, "Projects"],
    ["settings", Settings, "Settings"],
    ["logs", Terminal, "Logs"]
  ] as const;

  return (
    <main className="min-h-screen bg-zinc-950 text-zinc-100">
      <aside className="fixed inset-y-0 left-0 w-64 border-r border-zinc-800 bg-zinc-950/95 p-4">
        <div className="mb-8 flex items-center gap-3 px-2">
          <div className="grid h-10 w-10 place-items-center rounded bg-cyan-400 text-zinc-950">
            <Bot size={22} />
          </div>
          <div>
            <h1 className="text-lg font-semibold">Atlas AI</h1>
            <p className="text-xs text-zinc-400">Local career operating system</p>
          </div>
        </div>
        <nav className="space-y-1">
          {nav.map(([key, Icon, label]) => (
            <button key={key} onClick={() => setActive(key)} className={`nav-item ${active === key ? "nav-active" : ""}`} title={label}>
              <Icon size={18} />
              <span>{label}</span>
            </button>
          ))}
        </nav>
      </aside>
      <section className="ml-64 min-h-screen p-8">
        {active === "career" && <Career />}
        {active === "dashboard" && <Dashboard />}
        {active === "property" && <Property />}
        {active === "playground" && <Playground />}
        {active === "projects" && <Projects />}
        {active === "settings" && <SettingsPanel />}
        {active === "logs" && <Logs />}
      </section>
    </main>
  );
}

function Career() {
  const [dashboard, setDashboard] = useState<CareerDashboard | null>(null);
  const [companyName, setCompanyName] = useState("");
  const [careerUrl, setCareerUrl] = useState("");
  const [jobCompany, setJobCompany] = useState("");
  const [jobTitle, setJobTitle] = useState("");
  const [jobLocation, setJobLocation] = useState("");
  const [jobUrl, setJobUrl] = useState("");
  const [jobDescription, setJobDescription] = useState("");
  const [selectedJob, setSelectedJob] = useState<JobRecord | null>(null);
  const [status, setStatus] = useState("Loading Career Copilot...");

  async function load() {
    const response = await fetch("/api/plugins/career/dashboard");
    setDashboard(await response.json());
    setStatus("Career Copilot ready");
  }

  useEffect(() => {
    load().catch((error) => setStatus(`Backend unavailable: ${error}`));
  }, []);

  async function addCompany() {
    setStatus("Saving company...");
    await fetch("/api/plugins/career/companies", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: companyName, careerUrl, locations: ["Remote"], priority: 5, notes: "Added from Career Copilot UI" })
    });
    setCompanyName("");
    setCareerUrl("");
    await load();
  }

  async function analyzeJob() {
    setStatus("Analyzing job locally...");
    const response = await fetch("/api/plugins/career/jobs/analyze", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ company: jobCompany, title: jobTitle, location: jobLocation, url: jobUrl, description: jobDescription })
    });
    const job = await response.json();
    setSelectedJob(job);
    await load();
  }

  return (
    <div className="space-y-6">
      <header className="flex items-start justify-between gap-6">
        <div>
          <h2 className="text-3xl font-semibold">Career Copilot</h2>
          <p className="mt-2 max-w-3xl text-zinc-400">Track companies, analyze jobs, score visa fit, and prepare application assets locally.</p>
        </div>
        <div className="rounded border border-zinc-800 bg-zinc-900 px-4 py-3 text-sm text-zinc-300">{status}</div>
      </header>

      <div className="grid grid-cols-5 gap-4">
        <Metric icon={<Building2 size={18} />} label="Companies" value={dashboard?.trackedCompanies ?? 0} />
        <Metric icon={<BriefcaseBusiness size={18} />} label="Jobs" value={dashboard?.newJobs ?? 0} />
        <Metric icon={<ClipboardCheck size={18} />} label="Excellent" value={dashboard?.excellentMatches ?? 0} />
        <Metric icon={<FileText size={18} />} label="Ready" value={dashboard?.applicationsReady ?? 0} />
        <Metric icon={<ShieldCheck size={18} />} label="Visa Risk" value={dashboard?.blockedByVisa ?? 0} />
      </div>

      <div className="grid grid-cols-[1fr_1.2fr] gap-5">
        <section className="panel space-y-4">
          <h3 className="text-lg font-semibold">Company Knowledge Base</h3>
          <div className="space-y-3">
            <input className="input w-full" value={companyName} onChange={(event) => setCompanyName(event.target.value)} placeholder="Company name" />
            <input className="input w-full" value={careerUrl} onChange={(event) => setCareerUrl(event.target.value)} placeholder="Career URL" />
            <button className="button" onClick={addCompany} disabled={!companyName.trim()} title="Add company"><Building2 size={18} />Add Company</button>
          </div>
          <div className="space-y-2">
            {(dashboard?.topCompanies ?? []).map((company) => (
              <div className="rounded border border-zinc-800 bg-zinc-950 p-3" key={company.id}>
                <div className="flex items-center justify-between gap-3">
                  <span className="font-medium">{company.name}</span>
                  <span className="text-xs text-cyan-300">Visa {company.visaConfidence}</span>
                </div>
                <p className="mt-1 text-sm text-zinc-500">{company.careerUrl}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="panel space-y-4">
          <h3 className="text-lg font-semibold">Analyze Job</h3>
          <div className="grid grid-cols-2 gap-3">
            <input className="input" value={jobCompany} onChange={(event) => setJobCompany(event.target.value)} placeholder="Company" />
            <input className="input" value={jobTitle} onChange={(event) => setJobTitle(event.target.value)} placeholder="Role title" />
            <input className="input" value={jobLocation} onChange={(event) => setJobLocation(event.target.value)} placeholder="Location" />
            <input className="input" value={jobUrl} onChange={(event) => setJobUrl(event.target.value)} placeholder="Job URL" />
          </div>
          <textarea className="input min-h-40 w-full py-3" value={jobDescription} onChange={(event) => setJobDescription(event.target.value)} placeholder="Paste job description" />
          <button className="button" onClick={analyzeJob} disabled={!jobCompany.trim() || !jobTitle.trim()} title="Analyze job"><Play size={18} />Analyze Job</button>
        </section>
      </div>

      <div className="grid grid-cols-2 gap-5">
        <section className="panel">
          <h3 className="mb-3 text-lg font-semibold">Top Matches</h3>
          <div className="space-y-3">
            {(dashboard?.topMatches ?? []).map((job) => (
              <button className="w-full rounded border border-zinc-800 bg-zinc-950 p-3 text-left hover:border-cyan-500" key={job.id} onClick={() => setSelectedJob(job)}>
                <div className="flex items-center justify-between gap-3">
                  <span className="font-medium">{job.title}</span>
                  <span className="text-sm text-cyan-300">{job.match.overallMatch}%</span>
                </div>
                <p className="mt-1 text-sm text-zinc-500">{job.company} - {job.location} - {job.visa.recommendation}</p>
              </button>
            ))}
          </div>
        </section>

        <section className="panel">
          <h3 className="mb-3 text-lg font-semibold">Selected Job</h3>
          {selectedJob ? (
            <div className="space-y-3">
              <div>
                <h4 className="font-semibold">{selectedJob.title}</h4>
                <p className="text-sm text-zinc-400">{selectedJob.company} - {selectedJob.location}</p>
              </div>
              <div className="grid grid-cols-3 gap-3">
                <Metric label="Overall" value={`${selectedJob.match.overallMatch}%`} />
                <Metric label="Visa" value={`${selectedJob.visa.score}%`} />
                <Metric label="Interview" value={`${selectedJob.match.interviewProbability}%`} />
              </div>
              <p className="text-sm text-zinc-300">{selectedJob.visa.reason}</p>
              <pre className="max-h-56 overflow-auto rounded bg-zinc-950 p-3 text-xs text-zinc-400">{JSON.stringify(selectedJob.match.explanations, null, 2)}</pre>
            </div>
          ) : (
            <p className="text-sm text-zinc-500">Analyze or select a job to inspect scoring.</p>
          )}
        </section>
      </div>
    </div>
  );
}

function Dashboard() {
  return (
    <div className="space-y-6">
      <header>
        <h2 className="text-3xl font-semibold">Atlas Core</h2>
        <p className="mt-2 text-zinc-400">Local plugin platform, model access, browser automation, prompt management, storage, and memory.</p>
      </header>
      <div className="grid grid-cols-3 gap-4">
        {["Career Copilot", "Property Copilot", "Ollama Provider", "Browser Worker", "Prompt Library", "Developer Mode"].map((item) => (
          <div className="rounded border border-zinc-800 bg-zinc-900 p-5" key={item}>
            <h3 className="font-medium">{item}</h3>
            <p className="mt-2 text-sm text-zinc-400">Connected to local Atlas services.</p>
          </div>
        ))}
      </div>
    </div>
  );
}

function Property() {
  const [url, setUrl] = useState("");
  const [result, setResult] = useState<string>("");

  async function analyze() {
    setResult("Running local property workflow...");
    const response = await fetch("/api/plugins/property/analyze", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ url })
    });
    setResult(JSON.stringify(await response.json(), null, 2));
  }

  return (
    <div className="space-y-5">
      <h2 className="text-3xl font-semibold">Property Copilot</h2>
      <div className="flex gap-3">
        <input className="input flex-1" value={url} onChange={(event) => setUrl(event.target.value)} placeholder="Paste public listing URL" />
        <button className="button" onClick={analyze} title="Analyze listing"><Play size={18} />Analyze</button>
      </div>
      <pre className="panel min-h-96 overflow-auto text-sm">{result}</pre>
    </div>
  );
}

function Playground() {
  return <Workbench title="AI Playground" icon={<BrainCircuit />} fields={["Model", "Prompt", "Raw response", "Parsed response", "Execution time"]} />;
}

function Projects() {
  return <Workbench title="Projects" icon={<FolderKanban />} fields={["Workspace", "Reports", "Images", "Generated prompts", "Downloads"]} />;
}

function SettingsPanel() {
  return <Workbench title="Settings" icon={<Settings />} fields={["Ollama model", "Python path", "Workspace folder", "Visa required", "Daily scan time", "Target skills"]} />;
}

function Logs() {
  return <Workbench title="Browser Monitor & Logs" icon={<Image />} fields={["Requests", "Retries", "Errors", "Screenshots", "Structured data"]} />;
}

function Metric({ icon, label, value }: { icon?: React.ReactNode; label: string; value: number | string }) {
  return (
    <div className="rounded border border-zinc-800 bg-zinc-900 p-4">
      <div className="flex items-center gap-2 text-sm text-zinc-400">{icon}{label}</div>
      <div className="mt-2 text-2xl font-semibold">{value}</div>
    </div>
  );
}

function Workbench({ title, icon, fields }: { title: string; icon: React.ReactNode; fields: string[] }) {
  return (
    <div className="space-y-5">
      <h2 className="flex items-center gap-3 text-3xl font-semibold">{icon}{title}</h2>
      <div className="grid grid-cols-2 gap-4">
        {fields.map((field) => (
          <div className="rounded border border-zinc-800 bg-zinc-900 p-5" key={field}>
            <h3 className="font-medium">{field}</h3>
            <p className="mt-2 text-sm text-zinc-400">Connected through the local API surface.</p>
          </div>
        ))}
      </div>
    </div>
  );
}

createRoot(document.getElementById("root")!).render(<App />);
