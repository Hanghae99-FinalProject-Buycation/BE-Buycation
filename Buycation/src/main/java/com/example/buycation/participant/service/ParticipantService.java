package com.example.buycation.participant.service;

import com.example.buycation.alarm.dto.AlarmResponseDto;
import com.example.buycation.alarm.dto.RealtimeAlarmDto;
import com.example.buycation.alarm.entity.AlarmType;
import com.example.buycation.alarm.service.AlarmService;
import com.example.buycation.common.exception.CustomException;
import com.example.buycation.common.exception.ErrorCode;
import com.example.buycation.members.member.entity.Member;
import com.example.buycation.participant.dto.ApplicationResponseDto;
import com.example.buycation.participant.entity.Application;
import com.example.buycation.participant.entity.Participant;
import com.example.buycation.participant.mapper.ApplicationMapper;
import com.example.buycation.participant.repository.ApplicationRepository;
import com.example.buycation.participant.repository.ParticipantRepository;
import com.example.buycation.posting.entity.Posting;
import com.example.buycation.posting.repository.PostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.example.buycation.common.exception.ErrorCode.APPLICANT_NOT_FOUND;
import static com.example.buycation.common.exception.ErrorCode.AUTHORIZATION_APPLICANT_LOOKUP_FAIL;
import static com.example.buycation.common.exception.ErrorCode.AUTHORIZATION_DECISION_FAIL;
import static com.example.buycation.common.exception.ErrorCode.DUPLICATE_APPLICATION;
import static com.example.buycation.common.exception.ErrorCode.DUPLICATE_PARTICIPATION;
import static com.example.buycation.common.exception.ErrorCode.FINISH_PARTICIPATION;
import static com.example.buycation.common.exception.ErrorCode.PARTICIPANT_NOT_FOUND;
import static com.example.buycation.common.exception.ErrorCode.POSTING_NOT_FOUND;
import static com.example.buycation.common.exception.ErrorCode.POSTING_RECRUITMENT_SUCCESS_ERROR;
import static com.example.buycation.common.exception.ErrorCode.WRITER_PARTICIPATION_CANAEL;

@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ApplicationRepository applicationRepository;
    private final PostingRepository postingRepository;
    private final ApplicationMapper applicationMapper;
    private final ParticipantRepository participantRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void createApplicant(Member member, Long postingId) {
        Posting posting = postingRepository.findById(postingId).orElseThrow(() -> new CustomException(POSTING_NOT_FOUND));

        //????????? ????????? ?????? ??????
        if(posting.isDoneStatus()){
            throw new CustomException(POSTING_RECRUITMENT_SUCCESS_ERROR);
        }

        //????????? ???????????? ??????
        if (posting.getMember().getId().equals(member.getId())){
            throw new CustomException(DUPLICATE_PARTICIPATION);
        }
        //????????? ?????? ?????? ??????
        if (applicationRepository.findByPostingAndMember(posting, member) != null){
            throw new CustomException(DUPLICATE_APPLICATION);
        }
        //????????? ?????????????????? ??? ?????? ??????
        if (participantRepository.findByPostingAndMember(posting, member) != null){
            throw new CustomException(DUPLICATE_PARTICIPATION);
        }
        Application application = applicationMapper.toApplication(member,posting);
        applicationRepository.save(application);
        posting.add(application);

        try {
            if (!posting.getMember().getId().equals(member.getId())) {
                //alarmService.createAlarm(posting.getMember(),  AlarmType.APPLICATION, postingId, posting.getTitle());
                applicationEventPublisher.publishEvent(RealtimeAlarmDto.builder()
                        .postingId(posting.getId())
                        .alarmType(AlarmType.APPLICATION)
                        .member(posting.getMember())
                        .message(AlarmType.APPLICATION.getMessage())
                        .title(posting.getTitle()).build());
            }
        } catch(Exception e){
            System.out.println(ErrorCode.ALARM_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getApplicant(Member member, Long postingId) {
        Posting posting = postingRepository.findById(postingId).orElseThrow(() -> new CustomException(POSTING_NOT_FOUND));

        //???????????? ?????? ??????
        if (!posting.getMember().getId().equals(member.getId())){
            throw new CustomException(AUTHORIZATION_APPLICANT_LOOKUP_FAIL);
        }

        List<Application> applications = applicationRepository.findAllByPosting(posting);
        List<ApplicationResponseDto> applicationList = new ArrayList<>();
        for (Application a : applications) {
            applicationList.add(applicationMapper.toResponse(a));
        }
        return applicationList;
    }

    @Transactional
    public void applicantAccept(Member member, Long postingId, Long applicationId) {
        Posting posting = postingRepository.findById(postingId).orElseThrow(() -> new CustomException(POSTING_NOT_FOUND));

        //????????? ????????? ?????? ??????
        if(posting.isDoneStatus()){
            throw new CustomException(POSTING_RECRUITMENT_SUCCESS_ERROR);
        }

        //???????????? ?????? ??????
        if (!posting.getMember().getId().equals(member.getId())){
            throw new CustomException(AUTHORIZATION_DECISION_FAIL);
        }

        //???????????? ?????? ???????????? ???????????? ????????? ??? ?????? ??????
        Application application = applicationRepository.findById(applicationId).orElseThrow(()->new CustomException(APPLICANT_NOT_FOUND));
        if (!application.getPosting().equals(posting)){
            throw new CustomException(APPLICANT_NOT_FOUND);
        }

        //?????? ????????? ????????? ???????????? ??????
        if (participantRepository.findByPostingAndMember(application.getPosting(), application.getMember()) != null){
            throw new CustomException(DUPLICATE_PARTICIPATION);
        }

        //?????? ???????????? ??????????????? ??????
        if (posting.getCurrentMembers()==posting.getTotalMembers()){
            throw new CustomException(FINISH_PARTICIPATION);
        }

        Participant participant = applicationMapper.toParticipant(application);
        participantRepository.save(participant);
        posting.add(participant);

        posting.addMembers(1);

        applicationRepository.deleteById(applicationId);
        try {
            applicationEventPublisher.publishEvent(RealtimeAlarmDto.builder()
                    .postingId(posting.getId())
                    .alarmType(AlarmType.ACCEPT)
                    .member(application.getMember())
                    .message(AlarmType.ACCEPT.getMessage())
                    .title(posting.getTitle()).build());
        } catch(Exception e){
            System.out.println(ErrorCode.ALARM_NOT_FOUND);
        }

    }

    @Transactional
    public void applicantRefuse(Member member, Long postingId, Long applicationId) {
        Posting posting = postingRepository.findById(postingId).orElseThrow(() -> new CustomException(POSTING_NOT_FOUND));

        //???????????? ?????? ??????
        if (!posting.getMember().getId().equals(member.getId())){
            throw new CustomException(AUTHORIZATION_DECISION_FAIL);
        }

        Application application = applicationRepository.findById(applicationId).orElseThrow(()->new CustomException(APPLICANT_NOT_FOUND));

        //?????? ????????? ????????? ???????????? ??????
        if (participantRepository.findByPostingAndMember(application.getPosting(), application.getMember()) != null){
            throw new CustomException(DUPLICATE_PARTICIPATION);
        }

        applicationRepository.deleteById(applicationId);


        try {
            applicationEventPublisher.publishEvent(RealtimeAlarmDto.builder()
                    .postingId(posting.getId())
                    .alarmType(AlarmType.REJECT)
                    .member(application.getMember())
                    .message(AlarmType.REJECT.getMessage())
                    .title(posting.getTitle()).build());
        } catch(Exception e){
            System.out.println(ErrorCode.ALARM_NOT_FOUND);
        }
    }

    @Transactional
    public void cancelParticipation(Member member, Long postingId) {
        Posting posting = postingRepository.findById(postingId).orElseThrow(() -> new CustomException(POSTING_NOT_FOUND));

        //????????? ????????? ???????????? ??????
        if(posting.isDoneStatus()){
            throw new CustomException(POSTING_RECRUITMENT_SUCCESS_ERROR);
        }

        //????????? ????????? ???????????? ??????
        if (posting.getMember().getId().equals(member.getId())){
            throw new CustomException(WRITER_PARTICIPATION_CANAEL);
        }

        Participant participant = participantRepository.findByPostingAndMember(posting, member);

        //???????????? ????????? ????????? ??????
        if (participant == null){
            throw new CustomException(PARTICIPANT_NOT_FOUND);
        }

        participantRepository.deleteById(participant.getId());

        posting.addMembers(-1);
    }
}
