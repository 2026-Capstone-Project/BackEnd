package com.project.backend.domain.event.converter;

import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.entity.EventTitleHistory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventHistoryConverter {

    public static EventTitleHistory toEventTitleHistory(Long memberId, String title) {
        return EventTitleHistory.builder()
                .title(title)
                .lastUsedAt(LocalDateTime.now())
                .memberId(memberId)
                .build();
    }

    public static EventResDTO.EventTitleHistoryRes toEventTitleHistoryRes(List<String> titleHistory) {
        return EventResDTO.EventTitleHistoryRes.builder()
                .titleHistory(titleHistory)
                .build();
    }
}
