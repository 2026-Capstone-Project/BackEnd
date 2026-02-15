package com.project.backend.domain.suggestion.converter;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.suggestion.dto.request.SuggestionReqDTO;
import com.project.backend.domain.suggestion.dto.response.SuggestionResDTO;
import com.project.backend.domain.suggestion.entity.Suggestion;
import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.suggestion.enums.Status;
import com.project.backend.domain.suggestion.enums.SuggestionType;
import com.project.backend.domain.suggestion.util.SuggestionTargetKeyUtil;
import com.project.backend.domain.suggestion.util.TargetKeyHashUtil;
import com.project.backend.domain.suggestion.vo.RecurrenceSuggestionCandidate;
import com.project.backend.domain.suggestion.vo.SuggestionCandidate;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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
            Event previousEvent,
            Todo previousTodo
    ) {
        String targetKey = null;
        if (previousEvent != null) {
            targetKey = SuggestionTargetKeyUtil.eventKey(previousEvent.getTitle(), previousEvent.getLocation());
        }
        else if (previousTodo != null) {
            targetKey = SuggestionTargetKeyUtil.todoKey(previousTodo.getTitle(), previousTodo.getMemo());
        }

        return Suggestion.builder()
                .primaryAnchorDate(baseCandidate.primaryAnchorDate())
                .secondaryAnchorDate(baseCandidate.secondaryAnchorDate())
                .primaryContent(llmSuggestion.primaryContent())
                .secondaryContent(llmSuggestion.secondaryContent())
                .category(baseCandidate.category())
                .status(Status.PRIMARY)
                .suggestionType(
                        previousEvent != null
                                ? SuggestionType.CREATE_EVENT
                                : SuggestionType.CREATE_TODO)
                .targetKey(targetKey)
                .targetKeyHash(TargetKeyHashUtil.sha256(targetKey))
                .member(member)
                .previousEvent(previousEvent)
                .previousTodo(previousTodo)
                .build();
    }

    // TODO : 오버로딩 처리
    public static Suggestion toSuggestion(
            RecurrenceSuggestionCandidate baseCandidate,
            SuggestionResDTO.LlmRecurrenceGroupSuggestion llmRecurrenceGroupSuggestion,
            Member member,
            RecurrenceGroup rg,
            TodoRecurrenceGroup trg
    ) {
        String targetKey = null;
        if (rg != null) {
            targetKey = SuggestionTargetKeyUtil.rgKey(rg.getId());
        }
        else if (trg != null) {
            targetKey = SuggestionTargetKeyUtil.trgKey(trg.getId());
        }
        return Suggestion.builder()
                .primaryContent(llmRecurrenceGroupSuggestion.content())
                .targetKey(targetKey)
                .targetKeyHash(TargetKeyHashUtil.sha256(targetKey))
                .recurrenceGroup(rg)
                .todoRecurrenceGroup(trg)
                .member(member)
                .category(Category.EVENT)
                .status(Status.PRIMARY)
                .suggestionType(
                        rg != null
                                ? SuggestionType.EXTEND_EVENT_RECURRENCE_GROUP
                                : SuggestionType.EXTEND_TODO_RECURRENCE_GROUP)
                .build();
    }

    public static SuggestionReqDTO.LlmRecurrenceGroupSuggestionDetail toLlmRecurrenceGroupSuggestionDetail(RecurrenceSuggestionCandidate candidate, LocalDateTime last) {
        return SuggestionReqDTO.LlmRecurrenceGroupSuggestionDetail.builder()
                .id(candidate.id())
                // TODO : 중간에 익셉션이 이름을 수정했다면? 그것도 패턴이 유지되었다고 보아야하는가?
                .title(candidate.title())
                .lastStartTime(last)
                .endDate(candidate.endDate() != null ? candidate.endDate() : null)
                .occurrenceCount(candidate.occurrenceCount() != null ? candidate.occurrenceCount() : null)
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

    public static SuggestionResDTO.SuggestionDetailRes toSuggestionDetailRes(Suggestion suggestion) {
        return switch(suggestion.getStatus()) {
            case PRIMARY -> toPrimary(suggestion);
            case SECONDARY -> toSecondary(suggestion);
            default -> null;
        };
    }

    public static SuggestionResDTO.SuggestionListRes toSuggestionListRes(List<SuggestionResDTO.SuggestionDetailRes> suggestionListRes) {
        return SuggestionResDTO.SuggestionListRes.builder()
                .details(suggestionListRes)
                .build();
    }

    private static SuggestionResDTO.SuggestionDetailRes toPrimary(Suggestion suggestion) {
        return SuggestionResDTO.SuggestionDetailRes.builder()
                .id(suggestion.getId())
                .content(suggestion.getPrimaryContent())
                .build();
    }

    private static SuggestionResDTO.SuggestionDetailRes toSecondary(Suggestion suggestion) {
        return SuggestionResDTO.SuggestionDetailRes.builder()
                .id(suggestion.getId())
                .content(suggestion.getSecondaryContent())
                .build();
    }



}
