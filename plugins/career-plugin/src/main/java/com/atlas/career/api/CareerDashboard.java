package com.atlas.career.api;

import com.atlas.career.domain.CompanyRecord;
import com.atlas.career.domain.JobRecord;
import java.util.List;

public record CareerDashboard(
        int trackedCompanies,
        int newJobs,
        int excellentMatches,
        int applicationsReady,
        int blockedByVisa,
        List<JobRecord> topMatches,
        List<CompanyRecord> topCompanies
) {
}
