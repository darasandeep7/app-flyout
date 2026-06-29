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
  intelligence?: JobIntelligence;
  applicationStatus: string;
  resumeReady: boolean;
  coverLetterReady: boolean;
};

type JobIntelligence = {
  visa: { visaEligible: boolean; visaScore: number; visaConfidence: number; recommendation: string; reason: string; supportingEvidence: string[]; needsManualReview: boolean };
  ranking: { overallMatch: number; technicalMatch: number; javaMatch: number; springMatch: number; snowflakeMatch: number; backendMatch: number; microservicesMatch: number; cloudMatch: number; leadershipMatch: number; salaryMatch: number; locationMatch: number; remoteMatch: number; visaMatch: number; careerGrowthScore: number; interviewProbability: number; confidence: number; explanations: Record<string, string> };
  recommendation: { category: string; confidence: number; explanation: string; userOverrideAllowed: boolean };
  duplicate: { likelyDuplicate: boolean; duplicateConfidence: number; matchingSignals: string[] };
};

type DailyBriefing = {
  greeting: string;
  jobsFoundToday: number;
  excellentMatches: number;
  visaFriendlyJobs: number;
  applicationsReady: number;
  companiesRequiringReview: number;
  topRecommendedJobs: string[];
  topCompaniesHiring: string[];
  resumeHealth: { atsScore: number; resumeHealthScore: number; missingKeywords: string[]; strengths: string[]; improvementSuggestions: string[] };
  applicationFunnel: { jobsApplied: number; jobsSkipped: number; interviews: number; offers: number; rejections: number; ghostedApplications: number };
};

type ResumeHealth = DailyBriefing["resumeHealth"];

type RecommendationView = {
  jobId: string;
  company: string;
  title: string;
  location: string;
  recommendation?: { category: string; confidence: number; explanation: string };
  ranking?: { overallMatch: number; technicalMatch: number; visaMatch: number; interviewProbability: number; confidence: number };
};

type ApplicationPackage = {
  id: string;
  jobId: string;
  company: string;
  title: string;
  status: string;
  recommendationConfidence: number;
  recommendation: string;
  resumePath: string;
  coverLetterPath: string;
  answersPath: string;
  reportPath: string;
};

type CareerPreferences = {
  preferredTitles: string[];
  preferredSkills: string[];
  preferredLocations: string[];
  remotePreference: string;
  hybridPreference: string;
  minimumSalary: number;
  visaRequired: boolean;
  minimumMatchScore: number;
  blacklistCompanies: string[];
  whitelistCompanies: string[];
  dailyScanTime: string;
  maximumApplicationsPerDay: number;
};

type MasterResume = {
  content: string;
  preferredSkills: string[];
  preferredKeywords: string[];
  versions: string[];
  updatedAt: string;
};

type JobDiscoveryResult = {
  scannedAt: string;
  companiesScanned: number;
  jobsFound: number;
  jobsSaved: number;
  expiredRemoved: number;
  messages: string[];
};

type ApplicationExecutionResult = {
  applicationId: string;
  status: string;
  pauseReason: string;
  actions: string[];
  screenshots: string[];
  fallback: boolean;
  error?: string;
};

type ApplicationHistoryRecord = {
  applicationId: string;
  jobId: string;
  company: string;
  title: string;
  status: string;
  note: string;
  resumeVersion: string;
  recordedAt: string;
};

type AnswerTrainingRule = {
  id: string;
  questionPattern: string;
  preferredFormat: string;
  exampleAnswer: string;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
};

type LocalModel = {
  name: string;
  family: string;
  size: number;
  modifiedAt: string;
};

type AiModelPreferences = {
  resumeGeneration: string;
  coverLetters: string;
  applicationAnswers: string;
  jobAnalysis: string;
  visaReasoning: string;
  codeGeneration: string;
  fastFallback: string;
};

type CareerLearningInsight = {
  company: string;
  applied: number;
  interviews: number;
  offers: number;
  rejections: number;
  blocked: number;
  score: number;
  recommendation: string;
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
  const [briefing, setBriefing] = useState<DailyBriefing | null>(null);
  const [resumeHealth, setResumeHealth] = useState<ResumeHealth | null>(null);
  const [recommendations, setRecommendations] = useState<RecommendationView[]>([]);
  const [applications, setApplications] = useState<ApplicationPackage[]>([]);
  const [applicationHistory, setApplicationHistory] = useState<ApplicationHistoryRecord[]>([]);
  const [answerTrainingRules, setAnswerTrainingRules] = useState<AnswerTrainingRule[]>([]);
  const [learningInsights, setLearningInsights] = useState<CareerLearningInsight[]>([]);
  const [preferences, setPreferences] = useState<CareerPreferences | null>(null);
  const [preferredTitles, setPreferredTitles] = useState("");
  const [preferredSkills, setPreferredSkills] = useState("");
  const [preferredLocations, setPreferredLocations] = useState("");
  const [blacklistCompanies, setBlacklistCompanies] = useState("");
  const [whitelistCompanies, setWhitelistCompanies] = useState("");
  const [minimumSalary, setMinimumSalary] = useState(0);
  const [minimumMatchScore, setMinimumMatchScore] = useState(75);
  const [maximumApplicationsPerDay, setMaximumApplicationsPerDay] = useState(5);
  const [visaRequired, setVisaRequired] = useState(true);
  const [dailyScanTime, setDailyScanTime] = useState("08:00");
  const [masterResume, setMasterResume] = useState<MasterResume | null>(null);
  const [masterResumeContent, setMasterResumeContent] = useState("");
  const [resumeKeywords, setResumeKeywords] = useState("");
  const [companyImportText, setCompanyImportText] = useState("");
  const [scanResult, setScanResult] = useState<JobDiscoveryResult | null>(null);
  const [executionResult, setExecutionResult] = useState<ApplicationExecutionResult | null>(null);
  const [trainingPattern, setTrainingPattern] = useState("");
  const [trainingFormat, setTrainingFormat] = useState("");
  const [trainingExample, setTrainingExample] = useState("");
  const [status, setStatus] = useState("Loading Career Copilot...");

  async function load() {
    const [dashboardResponse, briefingResponse, recommendationResponse, applicationResponse, historyResponse, answerTrainingResponse, learningResponse, preferenceResponse, masterResumeResponse, resumeHealthResponse] = await Promise.all([
      fetch("/api/plugins/career/dashboard"),
      fetch("/api/plugins/career/intelligence/daily-briefing"),
      fetch("/api/plugins/career/intelligence/recommendations"),
      fetch("/api/plugins/career/applications"),
      fetch("/api/plugins/career/applications/history"),
      fetch("/api/plugins/career/answer-training"),
      fetch("/api/plugins/career/learning/insights"),
      fetch("/api/plugins/career/preferences"),
      fetch("/api/plugins/career/resume/master"),
      fetch("/api/plugins/career/resume/health")
    ]);
    setDashboard(await dashboardResponse.json());
    setBriefing(await briefingResponse.json());
    setRecommendations(await recommendationResponse.json());
    setApplications(await applicationResponse.json());
    setApplicationHistory(await historyResponse.json());
    setAnswerTrainingRules(await answerTrainingResponse.json());
    setLearningInsights(await learningResponse.json());
    const loadedPreferences = await preferenceResponse.json();
    setPreferences(loadedPreferences);
    syncPreferenceForm(loadedPreferences);
    const loadedResume = await masterResumeResponse.json();
    setMasterResume(loadedResume);
    setMasterResumeContent(loadedResume.content);
    setResumeKeywords(loadedResume.preferredKeywords.join(", "));
    setResumeHealth(await resumeHealthResponse.json());
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

  async function runDailyPreparation() {
    setStatus("Preparing application review queue...");
    await fetch("/api/plugins/career/daily/run", { method: "POST" });
    await load();
  }

  async function importCompanies() {
    setStatus("Importing career pages...");
    const response = await fetch("/api/plugins/career/companies/import", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ text: companyImportText })
    });
    setScanResult(await response.json());
    setCompanyImportText("");
    await load();
  }

  async function scanJobs() {
    setStatus("Scanning company career pages...");
    const response = await fetch("/api/plugins/career/jobs/scan", { method: "POST" });
    setScanResult(await response.json());
    await load();
  }

  async function savePreferences() {
    setStatus("Saving career preferences...");
    const nextPreferences: CareerPreferences = {
      preferredTitles: splitList(preferredTitles),
      preferredSkills: splitList(preferredSkills),
      preferredLocations: splitList(preferredLocations),
      remotePreference: preferences?.remotePreference ?? "Remote preferred",
      hybridPreference: preferences?.hybridPreference ?? "Hybrid acceptable",
      minimumSalary,
      visaRequired,
      minimumMatchScore,
      blacklistCompanies: splitList(blacklistCompanies),
      whitelistCompanies: splitList(whitelistCompanies),
      dailyScanTime,
      maximumApplicationsPerDay
    };
    const response = await fetch("/api/plugins/career/preferences", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(nextPreferences)
    });
    const saved = await response.json();
    setPreferences(saved);
    syncPreferenceForm(saved);
    setStatus("Career preferences saved");
  }

  async function saveMasterResume() {
    setStatus("Saving master resume...");
    const response = await fetch("/api/plugins/career/resume/master", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        content: masterResumeContent,
        preferredSkills: preferences?.preferredSkills ?? [],
        preferredKeywords: splitList(resumeKeywords),
        versions: masterResume?.versions ?? [],
        updatedAt: masterResume?.updatedAt ?? new Date().toISOString()
      })
    });
    const saved = await response.json();
    setMasterResume(saved);
    setMasterResumeContent(saved.content);
    setResumeKeywords(saved.preferredKeywords.join(", "));
    await load();
  }

  async function saveAnswerTrainingRule() {
    setStatus("Saving answer training rule...");
    await fetch("/api/plugins/career/answer-training", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        id: "",
        questionPattern: trainingPattern,
        preferredFormat: trainingFormat,
        exampleAnswer: trainingExample,
        enabled: true
      })
    });
    setTrainingPattern("");
    setTrainingFormat("");
    setTrainingExample("");
    await load();
    setStatus("Answer training rule saved");
  }

  async function approveApplication(applicationId: string) {
    setStatus("Approving application for Browser Agent...");
    await fetch(`/api/plugins/career/applications/${applicationId}/approve`, { method: "POST" });
    await load();
  }

  async function executeApplication(applicationId: string) {
    setStatus("Launching Browser Agent...");
    const response = await fetch(`/api/plugins/career/applications/${applicationId}/execute`, { method: "POST" });
    setExecutionResult(await response.json());
    await load();
  }

  async function markApplication(applicationId: string, nextStatus: string, note: string) {
    setStatus(`Marking application ${nextStatus.toLowerCase()}...`);
    await fetch(`/api/plugins/career/applications/${applicationId}/mark`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: nextStatus, note })
    });
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

  function syncPreferenceForm(nextPreferences: CareerPreferences) {
    setPreferredTitles(nextPreferences.preferredTitles.join(", "));
    setPreferredSkills(nextPreferences.preferredSkills.join(", "));
    setPreferredLocations(nextPreferences.preferredLocations.join(", "));
    setBlacklistCompanies(nextPreferences.blacklistCompanies.join(", "));
    setWhitelistCompanies(nextPreferences.whitelistCompanies.join(", "));
    setMinimumSalary(nextPreferences.minimumSalary);
    setMinimumMatchScore(nextPreferences.minimumMatchScore);
    setMaximumApplicationsPerDay(nextPreferences.maximumApplicationsPerDay);
    setVisaRequired(nextPreferences.visaRequired);
    setDailyScanTime(nextPreferences.dailyScanTime);
  }

  function splitList(value: string) {
    return value.split(",").map((item) => item.trim()).filter(Boolean);
  }

  return (
    <div className="space-y-6">
      <header className="flex items-start justify-between gap-6">
        <div>
          <h2 className="text-3xl font-semibold">{briefing?.greeting ?? "Career Intelligence"}</h2>
          <p className="mt-2 max-w-3xl text-zinc-400">Evaluate companies, jobs, visa risk, resume health, recommendations, and application history before taking action.</p>
        </div>
        <div className="flex items-center gap-3">
          <button className="button" onClick={scanJobs} title="Scan company career pages"><BriefcaseBusiness size={18} />Scan Jobs</button>
          <button className="button" onClick={runDailyPreparation} title="Prepare application queue"><ClipboardCheck size={18} />Prepare Queue</button>
          <div className="rounded border border-zinc-800 bg-zinc-900 px-4 py-3 text-sm text-zinc-300">{status}</div>
        </div>
      </header>

      <div className="grid grid-cols-5 gap-4">
        <Metric icon={<Building2 size={18} />} label="Companies" value={dashboard?.trackedCompanies ?? 0} />
        <Metric icon={<BriefcaseBusiness size={18} />} label="Jobs" value={dashboard?.newJobs ?? 0} />
        <Metric icon={<ClipboardCheck size={18} />} label="Excellent" value={dashboard?.excellentMatches ?? 0} />
        <Metric icon={<FileText size={18} />} label="Waiting" value={applications.filter((item) => item.status === "WAITING_FOR_REVIEW").length} />
        <Metric icon={<ShieldCheck size={18} />} label="Visa Risk" value={dashboard?.blockedByVisa ?? 0} />
      </div>

      <section className="panel space-y-4">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h3 className="text-lg font-semibold">Career Preferences</h3>
            <p className="mt-1 text-sm text-zinc-500">These local rules control scoring, visa filtering, and daily queue preparation.</p>
          </div>
          <button className="button" onClick={savePreferences} title="Save career preferences"><Settings size={18} />Save</button>
        </div>
        <div className="grid grid-cols-3 gap-3">
          <label className="field-label">Target Titles<input className="input mt-1 w-full" value={preferredTitles} onChange={(event) => setPreferredTitles(event.target.value)} /></label>
          <label className="field-label">Target Skills<input className="input mt-1 w-full" value={preferredSkills} onChange={(event) => setPreferredSkills(event.target.value)} /></label>
          <label className="field-label">Locations<input className="input mt-1 w-full" value={preferredLocations} onChange={(event) => setPreferredLocations(event.target.value)} /></label>
          <label className="field-label">Minimum Salary<input className="input mt-1 w-full" type="number" value={minimumSalary} onChange={(event) => setMinimumSalary(Number(event.target.value))} /></label>
          <label className="field-label">Minimum Match<input className="input mt-1 w-full" type="number" min={0} max={100} value={minimumMatchScore} onChange={(event) => setMinimumMatchScore(Number(event.target.value))} /></label>
          <label className="field-label">Daily Application Cap<input className="input mt-1 w-full" type="number" min={1} value={maximumApplicationsPerDay} onChange={(event) => setMaximumApplicationsPerDay(Number(event.target.value))} /></label>
          <label className="field-label">Daily Scan Time<input className="input mt-1 w-full" value={dailyScanTime} onChange={(event) => setDailyScanTime(event.target.value)} /></label>
          <label className="field-label">Blacklist<input className="input mt-1 w-full" value={blacklistCompanies} onChange={(event) => setBlacklistCompanies(event.target.value)} /></label>
          <label className="field-label">Whitelist<input className="input mt-1 w-full" value={whitelistCompanies} onChange={(event) => setWhitelistCompanies(event.target.value)} /></label>
        </div>
        <label className="flex items-center gap-3 text-sm text-zinc-300">
          <input className="h-4 w-4 accent-cyan-400" type="checkbox" checked={visaRequired} onChange={(event) => setVisaRequired(event.target.checked)} />
          Require visa-friendly jobs before preparing application packages
        </label>
      </section>

      <div className="grid grid-cols-3 gap-5">
        <section className="panel">
          <h3 className="mb-3 text-lg font-semibold">Resume Health</h3>
          <div className="grid grid-cols-2 gap-3">
            <Metric label="ATS" value={`${resumeHealth?.atsScore ?? 0}%`} />
            <Metric label="Health" value={`${resumeHealth?.resumeHealthScore ?? 0}%`} />
          </div>
          <p className="mt-3 text-sm text-zinc-400">Missing: {(resumeHealth?.missingKeywords ?? []).join(", ") || "None yet"}</p>
        </section>
        <section className="panel space-y-3">
          <div className="flex items-center justify-between gap-3">
            <h3 className="text-lg font-semibold">Master Resume</h3>
            <button className="button" onClick={saveMasterResume} title="Save master resume"><FileText size={18} />Save</button>
          </div>
          <textarea className="input min-h-36 w-full py-3" value={masterResumeContent} onChange={(event) => setMasterResumeContent(event.target.value)} />
          <input className="input w-full" value={resumeKeywords} onChange={(event) => setResumeKeywords(event.target.value)} placeholder="Preferred resume keywords" />
          <p className="text-xs text-zinc-500">Versions saved: {masterResume?.versions.length ?? 0}. Atlas can tailor only from this resume.</p>
        </section>
        <section className="panel">
          <h3 className="mb-3 text-lg font-semibold">Application Funnel</h3>
          <div className="grid grid-cols-3 gap-3">
            <Metric label="Applied" value={briefing?.applicationFunnel.jobsApplied ?? 0} />
            <Metric label="Interviews" value={briefing?.applicationFunnel.interviews ?? 0} />
            <Metric label="Offers" value={briefing?.applicationFunnel.offers ?? 0} />
          </div>
        </section>
        <section className="panel">
          <h3 className="mb-3 text-lg font-semibold">Visa Insights</h3>
          <div className="grid grid-cols-2 gap-3">
            <Metric label="Friendly" value={briefing?.visaFriendlyJobs ?? 0} />
            <Metric label="Review" value={briefing?.companiesRequiringReview ?? 0} />
          </div>
          <p className="mt-3 text-sm text-zinc-400">Every recommendation includes confidence and an override path.</p>
        </section>
      </div>

      <section className="panel space-y-4">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h3 className="text-lg font-semibold">Answer Training</h3>
            <p className="mt-1 text-sm text-zinc-500">Save reusable guidance for application questions. Atlas injects matching rules into future answer generation.</p>
          </div>
          <button className="button" onClick={saveAnswerTrainingRule} disabled={!trainingPattern.trim() || !trainingFormat.trim()} title="Save answer training"><BrainCircuit size={18} />Save Rule</button>
        </div>
        <div className="grid grid-cols-[0.8fr_1fr_1fr] gap-3">
          <label className="field-label">Question Pattern<input className="input mt-1 w-full" value={trainingPattern} onChange={(event) => setTrainingPattern(event.target.value)} placeholder="work authorization, salary, tell me about yourself" /></label>
          <label className="field-label">Preferred Format<textarea className="input mt-1 min-h-24 w-full py-3" value={trainingFormat} onChange={(event) => setTrainingFormat(event.target.value)} placeholder="Use 3 short paragraphs. Start direct. Mention Java/backend experience. Do not over-explain." /></label>
          <label className="field-label">Example Answer<textarea className="input mt-1 min-h-24 w-full py-3" value={trainingExample} onChange={(event) => setTrainingExample(event.target.value)} placeholder="Optional example answer Atlas can imitate." /></label>
        </div>
        <div className="grid grid-cols-2 gap-3">
          {answerTrainingRules.slice(0, 6).map((rule) => (
            <div className="rounded border border-zinc-800 bg-zinc-950 p-3" key={rule.id}>
              <div className="flex items-center justify-between gap-3">
                <span className="font-medium">{rule.questionPattern}</span>
                <span className="text-xs text-cyan-300">{rule.enabled ? "Enabled" : "Paused"}</span>
              </div>
              <p className="mt-2 text-sm text-zinc-400">{rule.preferredFormat}</p>
              {rule.exampleAnswer && <p className="mt-2 text-xs text-zinc-500">{rule.exampleAnswer}</p>}
            </div>
          ))}
          {answerTrainingRules.length === 0 && <p className="text-sm text-zinc-500">No answer rules saved yet.</p>}
        </div>
      </section>

      <div className="grid grid-cols-[1fr_1.2fr] gap-5">
        <section className="panel space-y-4">
          <h3 className="text-lg font-semibold">Company Knowledge Base</h3>
          <div className="space-y-3">
            <input className="input w-full" value={companyName} onChange={(event) => setCompanyName(event.target.value)} placeholder="Company name" />
            <input className="input w-full" value={careerUrl} onChange={(event) => setCareerUrl(event.target.value)} placeholder="Career URL" />
            <button className="button" onClick={addCompany} disabled={!companyName.trim()} title="Add company"><Building2 size={18} />Add Company</button>
          </div>
          <div className="space-y-3">
            <textarea className="input min-h-28 w-full py-3" value={companyImportText} onChange={(event) => setCompanyImportText(event.target.value)} placeholder={"Import career pages, one per line:\nCompany, https://company.com/careers\nhttps://jobs.lever.co/company"} />
            <button className="button" onClick={importCompanies} disabled={!companyImportText.trim()} title="Import career pages"><Building2 size={18} />Import Pages</button>
          </div>
          {scanResult && (
            <div className="rounded border border-zinc-800 bg-zinc-950 p-3 text-sm text-zinc-400">
              <div className="text-zinc-200">Scanned {scanResult.companiesScanned} companies, found {scanResult.jobsFound} jobs, saved {scanResult.jobsSaved}, removed {scanResult.expiredRemoved} expired.</div>
              <div className="mt-2 space-y-1 text-xs">{scanResult.messages.map((message) => <div key={message}>{message}</div>)}</div>
            </div>
          )}
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
          <h3 className="mb-3 text-lg font-semibold">Recommendation Queue</h3>
          <div className="space-y-3">
            {recommendations.map((item) => (
              <div className="w-full rounded border border-zinc-800 bg-zinc-950 p-3 text-left" key={item.jobId}>
                <div className="flex items-center justify-between gap-3">
                  <span className="font-medium">{item.title}</span>
                  <span className="text-sm text-cyan-300">{item.recommendation?.category ?? "UNSCORED"}</span>
                </div>
                <p className="mt-1 text-sm text-zinc-500">{item.company} - {item.location} - Overall {item.ranking?.overallMatch ?? 0}% - Visa {item.ranking?.visaMatch ?? 0}%</p>
                <p className="mt-2 text-xs text-zinc-400">{item.recommendation?.explanation ?? "Analyze jobs to build the queue."}</p>
              </div>
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
              <pre className="max-h-56 overflow-auto rounded bg-zinc-950 p-3 text-xs text-zinc-400">{JSON.stringify(selectedJob.intelligence?.ranking.explanations ?? selectedJob.match.explanations, null, 2)}</pre>
            </div>
          ) : (
            <p className="text-sm text-zinc-500">Analyze or select a job to inspect scoring.</p>
          )}
        </section>
      </div>

      <section className="panel">
        <h3 className="mb-3 text-lg font-semibold">Application Review Queue</h3>
        <div className="space-y-3">
          {applications.map((application) => (
            <div className="rounded border border-zinc-800 bg-zinc-950 p-3" key={application.id}>
              <div className="flex items-center justify-between gap-3">
                <div>
                  <div className="font-medium">{application.title}</div>
                  <div className="text-sm text-zinc-500">{application.company} - {application.status} - {application.recommendation}</div>
                </div>
                <div className="flex gap-2">
                  <button className="button" onClick={() => approveApplication(application.id)} title="Approve application"><Play size={18} />Approve</button>
                  <button className="button" onClick={() => executeApplication(application.id)} title="Run browser agent"><BriefcaseBusiness size={18} />Execute</button>
                  <button className="button" onClick={() => markApplication(application.id, "APPLIED", "Confirmed by Sandeep after browser review.")} title="Mark applied"><ClipboardCheck size={18} />Applied</button>
                  <button className="button" onClick={() => markApplication(application.id, "BLOCKED", "Blocked during application workflow.")} title="Mark blocked"><ShieldCheck size={18} />Block</button>
                </div>
              </div>
              <div className="mt-2 grid grid-cols-4 gap-2 text-xs text-zinc-500">
                <span>{application.resumePath}</span>
                <span>{application.coverLetterPath}</span>
                <span>{application.answersPath}</span>
                <span>{application.reportPath}</span>
              </div>
            </div>
          ))}
          {applications.length === 0 && <p className="text-sm text-zinc-500">No prepared applications yet. Analyze a good job, then prepare the queue.</p>}
          {executionResult && (
            <div className="rounded border border-zinc-800 bg-zinc-950 p-3 text-sm text-zinc-400">
              <div className="text-zinc-200">{executionResult.status}: {executionResult.pauseReason}</div>
              <div className="mt-2 space-y-1 text-xs">{executionResult.actions.map((action) => <div key={action}>{action}</div>)}</div>
              {executionResult.error && <div className="mt-2 text-xs text-red-300">{executionResult.error}</div>}
            </div>
          )}
        </div>
      </section>

      <section className="panel">
        <h3 className="mb-3 text-lg font-semibold">Recent Applications</h3>
        <div className="space-y-3">
          {applicationHistory.slice(0, 8).map((item) => (
            <div className="rounded border border-zinc-800 bg-zinc-950 p-3" key={`${item.applicationId}-${item.recordedAt}`}>
              <div className="flex items-center justify-between gap-3">
                <div>
                  <div className="font-medium">{item.title}</div>
                  <div className="text-sm text-zinc-500">{item.company} - {item.status} - {item.resumeVersion}</div>
                </div>
                <div className="text-xs text-zinc-500">{new Date(item.recordedAt).toLocaleString()}</div>
              </div>
              {item.note && <p className="mt-2 text-sm text-zinc-400">{item.note}</p>}
            </div>
          ))}
          {applicationHistory.length === 0 && <p className="text-sm text-zinc-500">No application history yet.</p>}
        </div>
      </section>

      <section className="panel">
        <h3 className="mb-3 text-lg font-semibold">Learning Insights</h3>
        <div className="space-y-3">
          {learningInsights.slice(0, 8).map((item) => (
            <div className="rounded border border-zinc-800 bg-zinc-950 p-3" key={item.company}>
              <div className="flex items-center justify-between gap-3">
                <div>
                  <div className="font-medium">{item.company}</div>
                  <div className="text-sm text-zinc-500">{item.recommendation} - score {item.score}</div>
                </div>
                <div className="text-xs text-zinc-500">Applied {item.applied} - Interviews {item.interviews} - Offers {item.offers} - Blocked {item.blocked}</div>
              </div>
            </div>
          ))}
          {learningInsights.length === 0 && <p className="text-sm text-zinc-500">Atlas will learn once application outcomes are recorded.</p>}
        </div>
      </section>

      <section className="panel">
        <h3 className="mb-3 text-lg font-semibold">Job Ranking Table</h3>
        <div className="overflow-auto">
          <table className="w-full text-left text-sm">
            <thead className="text-zinc-500">
              <tr>
                <th className="border-b border-zinc-800 py-2">Role</th>
                <th className="border-b border-zinc-800 py-2">Company</th>
                <th className="border-b border-zinc-800 py-2">Overall</th>
                <th className="border-b border-zinc-800 py-2">Technical</th>
                <th className="border-b border-zinc-800 py-2">Visa</th>
                <th className="border-b border-zinc-800 py-2">Recommendation</th>
              </tr>
            </thead>
            <tbody>
              {(dashboard?.topMatches ?? []).map((job) => (
                <tr className="text-zinc-300" key={job.id}>
                  <td className="border-b border-zinc-900 py-2">{job.title}</td>
                  <td className="border-b border-zinc-900 py-2">{job.company}</td>
                  <td className="border-b border-zinc-900 py-2">{job.intelligence?.ranking.overallMatch ?? job.match.overallMatch}%</td>
                  <td className="border-b border-zinc-900 py-2">{job.intelligence?.ranking.technicalMatch ?? job.match.resumeMatch}%</td>
                  <td className="border-b border-zinc-900 py-2">{job.intelligence?.ranking.visaMatch ?? job.match.visaMatch}%</td>
                  <td className="border-b border-zinc-900 py-2">{job.intelligence?.recommendation.category ?? job.applicationStatus}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
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
  const [models, setModels] = useState<LocalModel[]>([]);
  const [preferences, setPreferences] = useState<AiModelPreferences>({
    resumeGeneration: "qwen3:4b",
    coverLetters: "qwen3:4b",
    applicationAnswers: "qwen3:4b",
    jobAnalysis: "qwen3:4b",
    visaReasoning: "qwen3:4b",
    codeGeneration: "qwen2.5-coder:3b",
    fastFallback: "gemma3:4b"
  });
  const [testModel, setTestModel] = useState("qwen3:4b");
  const [testPrompt, setTestPrompt] = useState("Analyze this in one sentence: Java Spring backend role with Kafka and Snowflake.");
  const [testResult, setTestResult] = useState("");
  const [status, setStatus] = useState("Loading model settings...");

  async function loadAiModels() {
    const response = await fetch("/api/settings/ai-models");
    const data = await response.json();
    setModels(data.models ?? []);
    setPreferences(data.preferences);
    setTestModel(data.preferences?.fastFallback ?? "gemma3:4b");
    setStatus(`Detected ${(data.models ?? []).length} Ollama models`);
  }

  useEffect(() => {
    loadAiModels().catch((error) => setStatus(`Could not load Ollama models: ${error}`));
  }, []);

  async function saveAiModels() {
    setStatus("Saving AI model preferences...");
    const response = await fetch("/api/settings/ai-models", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(preferences)
    });
    const data = await response.json();
    setPreferences(data.preferences);
    setModels(data.models ?? []);
    setStatus("AI model preferences saved");
  }

  async function testAiModel() {
    setStatus("Testing model...");
    const response = await fetch("/api/settings/ai-models/test", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ model: testModel, prompt: testPrompt })
    });
    const data = await response.json();
    const elapsed = typeof data.elapsed === "string" ? data.elapsed : `${Math.round((data.elapsed?.nano ?? 0) / 1000000)} ms`;
    setTestResult(`${data.model} responded in ${elapsed}\n\n${data.text}${data.error ? `\n\nError: ${data.error}` : ""}`);
    setStatus(data.fallback ? "Model test used fallback/error path" : "Model test complete");
  }

  function updateModel(key: keyof AiModelPreferences, value: string) {
    setPreferences({ ...preferences, [key]: value });
  }

  const modelOptions = Array.from(new Set([...models.map((model) => model.name), "qwen3:4b", "qwen2.5-coder:3b", "gemma3:4b"]));

  return (
    <div className="space-y-6">
      <header className="flex items-start justify-between gap-6">
        <div>
          <h2 className="flex items-center gap-3 text-3xl font-semibold"><Settings />AI Model Settings</h2>
          <p className="mt-2 text-zinc-400">Task-specific Ollama routing optimized for an 8 GB local machine.</p>
        </div>
        <div className="flex gap-3">
          <button className="button" onClick={loadAiModels} title="Detect installed Ollama models"><BrainCircuit size={18} />Detect</button>
          <button className="button" onClick={saveAiModels} title="Save model preferences"><Settings size={18} />Save</button>
        </div>
      </header>

      <div className="rounded border border-zinc-800 bg-zinc-900 px-4 py-3 text-sm text-zinc-300">{status}</div>

      <section className="panel space-y-4">
        <h3 className="text-lg font-semibold">Task Models</h3>
        <div className="grid grid-cols-3 gap-3">
          <ModelSelect label="Resume generation" value={preferences.resumeGeneration} options={modelOptions} onChange={(value) => updateModel("resumeGeneration", value)} />
          <ModelSelect label="Cover letters" value={preferences.coverLetters} options={modelOptions} onChange={(value) => updateModel("coverLetters", value)} />
          <ModelSelect label="Application answers" value={preferences.applicationAnswers} options={modelOptions} onChange={(value) => updateModel("applicationAnswers", value)} />
          <ModelSelect label="Job analysis" value={preferences.jobAnalysis} options={modelOptions} onChange={(value) => updateModel("jobAnalysis", value)} />
          <ModelSelect label="Visa reasoning" value={preferences.visaReasoning} options={modelOptions} onChange={(value) => updateModel("visaReasoning", value)} />
          <ModelSelect label="Code generation" value={preferences.codeGeneration} options={modelOptions} onChange={(value) => updateModel("codeGeneration", value)} />
          <ModelSelect label="Fast fallback" value={preferences.fastFallback} options={modelOptions} onChange={(value) => updateModel("fastFallback", value)} />
        </div>
      </section>

      <div className="grid grid-cols-[1fr_1fr] gap-5">
        <section className="panel space-y-3">
          <h3 className="text-lg font-semibold">Installed Models</h3>
          <div className="max-h-80 space-y-2 overflow-auto">
            {models.length === 0 && <p className="text-sm text-zinc-500">No models detected. Start Ollama or run `ollama list` locally.</p>}
            {models.map((model) => (
              <div className="rounded border border-zinc-800 bg-zinc-950 p-3" key={model.name}>
                <div className="font-medium">{model.name}</div>
                <div className="mt-1 text-xs text-zinc-500">{model.family} · {(model.size / 1024 / 1024 / 1024).toFixed(1)} GB</div>
              </div>
            ))}
          </div>
        </section>

        <section className="panel space-y-3">
          <h3 className="text-lg font-semibold">Test Model</h3>
          <ModelSelect label="Model" value={testModel} options={modelOptions} onChange={setTestModel} />
          <textarea className="input min-h-32 w-full py-3" value={testPrompt} onChange={(event) => setTestPrompt(event.target.value)} />
          <button className="button" onClick={testAiModel} title="Test selected model"><Play size={18} />Test</button>
          <pre className="max-h-72 overflow-auto rounded border border-zinc-800 bg-zinc-950 p-3 text-sm text-zinc-300">{testResult}</pre>
        </section>
      </div>
    </div>
  );
}

function ModelSelect({ label, value, options, onChange }: { label: string; value: string; options: string[]; onChange: (value: string) => void }) {
  return (
    <label className="field-label">
      {label}
      <select className="input mt-1 w-full" value={value} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => <option key={option} value={option}>{option}</option>)}
      </select>
    </label>
  );
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
