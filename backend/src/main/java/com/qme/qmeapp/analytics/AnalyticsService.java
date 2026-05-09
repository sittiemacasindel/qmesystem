package com.qme.qmeapp.analytics;

import com.qme.qmeapp.analytics.dto.AnalyticsResponse;
import com.qme.qmeapp.organization.OrganizationRepository;
import com.qme.qmeapp.queue.QueueEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrganizationRepository organizationRepository;
    private final QueueEntryRepository queueEntryRepository;

    public AnalyticsResponse getOverallAnalytics() {
        long activeOrgs = organizationRepository.countByStatus("ACTIVE");
        
        long waitingCount = queueEntryRepository.countByStatus("WAITING");
        long servingCount = queueEntryRepository.countByStatus("SERVING");
        
        OffsetDateTime startOfDay = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long servedToday = queueEntryRepository.countByStatusAndServedAtAfter("SERVED", startOfDay);

        return AnalyticsResponse.builder()
                .activeOrganizations(activeOrgs)
                .totalWaitingCustomers(waitingCount + servingCount)
                .totalServedToday(servedToday)
                .build();
    }
}
