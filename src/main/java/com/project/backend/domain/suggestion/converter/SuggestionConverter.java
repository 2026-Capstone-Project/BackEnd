package com.project.backend.domain.suggestion.converter;

import com.project.backend.domain.event.entity.Event;
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

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SuggestionConverter {

    public static SuggestionReqDTO.LlmSuggestionDetail toLlmSuggestionDetail(
            SuggestionCandidate detail,
            Integer primaryDiff,
            Integer secondaryDiff,
            StableType stableType
    ) {
        return SuggestionReqDTO.LlmSuggestionDetail.builder()
                .eventId(detail.id())
                .title(detail.title())
                .start(detail.start())
                .primaryDiff(primaryDiff)
                .secondaryDiff(secondaryDiff)
                .stableType(stableType)
                .category(Category.EVENT)
                .build();
    }

    public static SuggestionReqDTO.LlmSuggestionReq toLlmSuggestionReq(
            long suggestionReqCnt,
            List<SuggestionReqDTO.LlmSuggestionDetail> suggestionReqList
    ) {
        return SuggestionReqDTO.LlmSuggestionReq.builder()
                .suggestionReqCnt(suggestionReqCnt)
                .suggestionDetailList(suggestionReqList)
                .build();
    }

    public static Suggestion toSuggestion(
            SuggestionCandidate baseCandidate,
            SuggestionResDTO.LlmSuggestion llmSuggestion,
            Member member,
            Event previousEvent
    ) {
        return Suggestion.builder()
                .title(baseCandidate.title())
                .primaryDiff(baseCandidate.primaryDiff())
                .secondaryDiff(baseCandidate.secondaryDiff())
                .primaryContent(llmSuggestion.primaryContent())
                .secondaryContent(llmSuggestion.secondaryContent())
                .category(Category.EVENT)
                .status(Status.PRIMARY)
                .member(member)
                .previousEvent(previousEvent)
                .build();
    }

}
