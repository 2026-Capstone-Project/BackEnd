package com.project.backend.domain.event.service.command;

import com.project.backend.domain.event.converter.EventConverter;
import com.project.backend.domain.event.converter.RecurrenceGroupConverter;
import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.event.repository.RecurrenceGroupRepository;
import com.project.backend.domain.event.validator.EventValidator;
import com.project.backend.domain.event.validator.RecurrenceGroupValidator;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.exception.MemberErrorCode;
import com.project.backend.domain.member.exception.MemberException;
import com.project.backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventCommandServiceImpl implements EventCommandService {

    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final RecurrenceGroupRepository recurrenceGroupRepository;
    private final EventValidator eventValidator;
    private final RecurrenceGroupValidator rgValidator;

    @Override
    public EventResDTO.CreateRes createEvent(EventReqDTO.CreateReq req, Long memberId) {
        eventValidator.validateCreate(req);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        RecurrenceGroup recurrenceGroup = null;
        if (req.recurrenceGroup() != null) {

            rgValidator.validateCreate(req.recurrenceGroup(), req.startTime());

            recurrenceGroup =
                    RecurrenceGroupConverter.toRecurrenceGroup(req.recurrenceGroup(), req.startTime(), member);
            recurrenceGroupRepository.save(recurrenceGroup);
        }

        // TODO : 임시 조치이므로 리펙토링
        Event event = EventConverter.toEvent(req, member, recurrenceGroup);
        if (recurrenceGroup != null) {
            recurrenceGroup.setEvent(event);
            recurrenceGroupRepository.save(recurrenceGroup);
        }
        eventRepository.save(event);

        return EventConverter.toCreateRes(event);
    }
}
