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

}
