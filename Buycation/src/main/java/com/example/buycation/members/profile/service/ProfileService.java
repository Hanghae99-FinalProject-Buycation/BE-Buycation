package com.example.buycation.members.profile.service;

import com.example.buycation.common.exception.CustomException;
import com.example.buycation.members.member.entity.Member;
import com.example.buycation.members.member.repository.MemberRepository;
import com.example.buycation.members.profile.dto.MemberReviewResponseDto;
import com.example.buycation.members.profile.dto.ReviewRequestDto;
import com.example.buycation.members.profile.entity.Review;
import com.example.buycation.members.profile.mapper.ReviewMapper;
import com.example.buycation.members.profile.repository.ReviewRepository;
import com.example.buycation.participant.entity.Participant;
import com.example.buycation.participant.repository.ParticipantRepository;
import com.example.buycation.posting.dto.MainPostingResponseDto;
import com.example.buycation.posting.entity.Posting;
import com.example.buycation.posting.mapper.PostingMapper;
import com.example.buycation.posting.repository.PostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.example.buycation.common.exception.ErrorCode.AUTHORIZATION_LOOKUP_FAIL;
import static com.example.buycation.common.exception.ErrorCode.DUPLICATE_REVIEW;
import static com.example.buycation.common.exception.ErrorCode.MEMBER_NOT_FOUND;
import static com.example.buycation.common.exception.ErrorCode.POSTING_NOT_FOUND;
import static com.example.buycation.common.exception.ErrorCode.POSTING_PARTICIPANT_REVIEW;
import static com.example.buycation.common.exception.ErrorCode.POSTING_SUCCESS_ERROR;
import static com.example.buycation.common.exception.ErrorCode.SELF_REVIEW_ERROR;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final PostingRepository postingRepository;
    private final ParticipantRepository participantRepository;
    private final PostingMapper postingMapper;

    @Transactional
    public void createReview(ReviewRequestDto reviewRequestDto, Member reviewer, Long postingId, Long memberId) {
        Posting posting = postingRepository.findById(postingId).orElseThrow(() -> new CustomException(POSTING_NOT_FOUND));
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new CustomException(MEMBER_NOT_FOUND));

        //게시글 완료 전 리뷰 금지
        if (!posting.isDoneStatus()) {
            throw new CustomException(POSTING_SUCCESS_ERROR);
        }
        //참가자외에 리뷰 금지
        if (participantRepository.findByPostingAndMember(posting, reviewer) == null) {
            throw new CustomException(POSTING_PARTICIPANT_REVIEW);
        }
        //셀프리뷰금지
        if (memberId.equals(reviewer.getId())) {
            throw new CustomException(SELF_REVIEW_ERROR);
        }
        //중복리뷰 금지
        if (reviewRepository.findByPostingIdAndReviewerIdAndMember(postingId, reviewer.getId(), member) != null) {
            throw new CustomException(DUPLICATE_REVIEW);
        }

        Review review = reviewMapper.toReview(reviewRequestDto, reviewer.getId(), postingId, member);
        reviewRepository.save(review);
        member.addScore(review.getReviewScore(), 1);
    }

    @Transactional(readOnly = true)
    public List<MemberReviewResponseDto> getMemberReviewList(Long postingId, Member member) {
        Posting posting = postingRepository.findById(postingId).orElseThrow(() -> new CustomException(POSTING_NOT_FOUND));

        //참가자외에 리뷰 조회 금지
        if (participantRepository.findByPostingAndMember(posting, member) == null) {
            throw new CustomException(AUTHORIZATION_LOOKUP_FAIL);
        }

        List<Participant> participants = participantRepository.findAllByPosting(posting);
        List<MemberReviewResponseDto> memberReviewResponseDtoList = new ArrayList<>();
        for (Participant p : participants) {
            if (!p.getMember().getId().equals(member.getId())){
                memberReviewResponseDtoList.add(reviewMapper.toResponse(p));
            }
        }
        return memberReviewResponseDtoList;
    }

    @Transactional(readOnly = true)
    public List<MainPostingResponseDto> getMyPostingList(Member member, Long memberId) {
        if (!member.getId().equals(memberId)) {
            throw new CustomException(AUTHORIZATION_LOOKUP_FAIL);
        }
        List<Posting> postings = postingRepository.findAllByMember(member);
        List<MainPostingResponseDto> postingList = new ArrayList<>();
        for (Posting p : postings) {
            postingList.add(postingMapper.toResponse(p));
        }
        return postingList;
    }

    @Transactional(readOnly = true)
    public List<MainPostingResponseDto> getParticipationPostingList(Member member, Long memberId) {
        if (!member.getId().equals(memberId)) {
            throw new CustomException(AUTHORIZATION_LOOKUP_FAIL);
        }

        List<Participant> participant = participantRepository.findAllByMember(member);
        List<MainPostingResponseDto> postingList = new ArrayList<>();
        for (Participant p : participant) {
            postingList.add(postingMapper.toResponse(p.getPosting()));
        }
        return postingList;
    }
}