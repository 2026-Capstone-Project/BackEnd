package com.project.backend.domain.suggestion.converter;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.suggestion.dto.request.SuggestionReqDTO;
import com.project.backend.domain.suggestion.dto.response.SuggestionResDTO;
import com.project.backend.domain.suggestion.entity.Suggestion;
import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.suggestion.enums.StableType;
import com.project.backend.domain.suggestion.enums.Status;
import com.project.backend.domain.suggestion.vo.SuggestionCandidate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SuggestionConverter {

    public static SuggestionReqDTO.LlmSuggestionReq toLlmSuggestionReq(
            long suggestionReqCnt,
            List<SuggestionReqDTO.LlmSuggestionDetail> suggestionReqList
    ) {
        return SuggestionReqDTO.LlmSuggestionReq.builder()
                .suggestionReqCnt(suggestionReqCnt)
                .suggestionDetailList(suggestionReqList)
                .build();
    }

    // TODO : 오버로딩 처리
    public static Suggestion toSuggestion(
            SuggestionCandidate baseCandidate,
            SuggestionResDTO.LlmSuggestion llmSuggestion,
            Member member,
            Event previousEvent
    ) {
        return Suggestion.builder()
                .primaryPattern(baseCandidate.primary())
                .secondaryPattern(baseCandidate.secondary())
                .primaryContent(llmSuggestion.primaryContent())
                .secondaryContent(llmSuggestion.secondaryContent())
                .category(Category.EVENT)
                .status(Status.PRIMARY)
                .member(member)
                .previousEvent(previousEvent)
                .build();
    }

    // TODO : 오버로딩 처리
    public static Suggestion toSuggestion(
            RecurrenceGroup baseRG,
            SuggestionResDTO.LlmRecurrenceGroupSuggestion llmRecurrenceGroupSuggestion,
            Member member
    ) {
        return Suggestion.builder()
                .primaryContent(llmRecurrenceGroupSuggestion.content())
                .recurrenceGroup(baseRG)
                .member(member)
                .category(Category.EVENT)
                .status(Status.PRIMARY)
                .build();
    }

    public static SuggestionReqDTO.LlmRecurrenceGroupSuggestionDetail toLlmRecurrenceGroupSuggestionDetail(RecurrenceGroup rg, LocalDateTime last) {
        return SuggestionReqDTO.LlmRecurrenceGroupSuggestionDetail.builder()
                .recurrenceGroupId(rg.getId())
                // TODO : 중간에 익셉션이 이름을 수정했다면? 그것도 패턴이 유지되었다고 보아야하는가?
                .title(rg.getEvent().getTitle())
                .lastStartTime(last)
                .endDate(rg.getEndDate() != null ? rg.getEndDate() : null)
                .occurrenceCount(rg.getOccurrenceCount() != null ? rg.getOccurrenceCount() : null)
                .build();
    }

    public static SuggestionReqDTO.LlmRecurrenceGroupSuggestionReq toLlmRecurrenceGroupSuggestionReq(
            long suggestionReqCnt,
            List<SuggestionReqDTO.LlmRecurrenceGroupSuggestionDetail> llmRecurrenceGroupSuggestionList
    ) {
        return SuggestionReqDTO.LlmRecurrenceGroupSuggestionReq.builder()
                .suggestionReqCnt(suggestionReqCnt)
                .recurrenceGroupSuggestionDetails(llmRecurrenceGroupSuggestionList)
                .build();
    }


}
