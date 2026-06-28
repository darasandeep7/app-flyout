import React, { useState } from "react";
import { createRoot } from "react-dom/client";
import { Bot, Boxes, BrainCircuit, FolderKanban, Image, Play, Settings, Terminal } from "lucide-react";
import "./styles.css";

type NavKey = "dashboard" | "property" | "playground" | "projects" | "settings" | "logs";

function App() {
  const [active, setActive] = useState<NavKey>("dashboard");
  const nav = [
    ["dashboard", Bot, "Dashboard"],
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
            <p className="text-xs text-zinc-400">Local AI operating system</p>
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

function Dashboard() {
  return (
    <div className="space-y-6">
      <header>
        <h2 className="text-3xl font-semibold">Dashboard</h2>
        <p className="mt-2 text-zinc-400">Local modules, plugins, model access, browser automation, and memory in one workbench.</p>
      </header>
      <div className="grid grid-cols-3 gap-4">
        {["Property Plugin", "Ollama Provider", "Browser Worker", "Project Memory", "Prompt Library", "Developer Mode"].map((item) => (
          <div className="rounded border border-zinc-800 bg-zinc-900 p-5" key={item}>
            <h3 className="font-medium">{item}</h3>
            <p className="mt-2 text-sm text-zinc-400">Ready with graceful local fallbacks.</p>
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
      <h2 className="text-3xl font-semibold">Property Plugin</h2>
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
  return <Workbench title="Settings" icon={<Settings />} fields={["Ollama model", "Python path", "Playwright path", "Workspace folder", "Theme"]} />;
}

function Logs() {
  return <Workbench title="Browser Monitor & Logs" icon={<Image />} fields={["Requests", "Retries", "Errors", "Screenshots", "Structured data"]} />;
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
