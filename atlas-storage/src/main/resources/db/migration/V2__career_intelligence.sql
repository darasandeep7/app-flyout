create table if not exists career_schema_version (
  version integer primary key,
  description text not null,
  applied_at text not null
);

create table if not exists career_companies (
  id text primary key,
  company_name text not null,
  industry text,
  website text,
  careers_url text,
  known_ats_platform text,
  locations_json text,
  remote_policy text,
  visa_sponsorship_confidence integer,
  historical_sponsorship text,
  historical_applications integer,
  historical_interviews integer,
  historical_rejections integer,
  historical_offers integer,
  average_match_score integer,
  recruiter_notes text,
  blocked integer,
  priority integer,
  last_scan text,
  last_updated text,
  confidence_score integer,
  technology_stack_json text
);

create table if not exists career_jobs (
  id text primary key,
  company_id text,
  company text not null,
  title text not null,
  location text,
  url text,
  description text,
  ranking_json text,
  visa_json text,
  recommendation_json text,
  duplicate_confidence integer,
  application_status text,
  discovered_at text,
  updated_at text
);

insert or ignore into career_schema_version(version, description, applied_at)
values (2, 'career intelligence schema', datetime('now'));
