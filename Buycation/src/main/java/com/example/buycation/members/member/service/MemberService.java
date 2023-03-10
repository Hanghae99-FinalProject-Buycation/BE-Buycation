package com.example.buycation.members.member.service;

import com.example.buycation.common.exception.CustomException;
import com.example.buycation.mail.entity.EmailCheck;
import com.example.buycation.mail.repository.EmailCheckRepository;
import com.example.buycation.members.member.dto.LoginRequestDto;
import com.example.buycation.members.member.dto.MemberResponseDto;
import com.example.buycation.members.member.dto.SignupRequestDto;
import com.example.buycation.members.member.dto.UpdateMemberRequestDto;
import com.example.buycation.members.member.entity.Member;
import com.example.buycation.members.member.mapper.MemberMapper;
import com.example.buycation.members.member.repository.MemberRepository;
import com.example.buycation.members.profile.dto.ReviewResponseDto;
import com.example.buycation.members.profile.entity.Review;
import com.example.buycation.members.profile.mapper.ReviewMapper;
import com.example.buycation.members.profile.repository.ReviewRepository;
import com.example.buycation.security.UserDetailsImpl;
import com.example.buycation.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.example.buycation.common.exception.ErrorCode.AUTHORIZATION_UPDATE_FAIL;
import static com.example.buycation.common.exception.ErrorCode.DUPLICATE_EMAIL;
import static com.example.buycation.common.exception.ErrorCode.DUPLICATE_NICKNAME;
import static com.example.buycation.common.exception.ErrorCode.EMAIL_CERTIFICATION_FAIL;
import static com.example.buycation.common.exception.ErrorCode.INCORRECT_PASSWORD;
import static com.example.buycation.common.exception.ErrorCode.INVALID_NICKNAME_PATTERN;
import static com.example.buycation.common.exception.ErrorCode.MEMBER_NOT_FOUND;


@Service
@Slf4j
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final MemberMapper memberMapper;
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final EmailCheckRepository emailCheckRepository;

    @Transactional
    public void signup(SignupRequestDto signupRequestDto) {
        Member member = memberMapper.toMember(signupRequestDto);

        // ????????? ID ?????? ??????
        Optional<Member> emailDuplicateCheck = memberRepository.findByEmail(member.getEmail());
        if (emailDuplicateCheck.isPresent()) {
            throw new CustomException(DUPLICATE_EMAIL);
        }

        // ????????? ?????? ??????
        Optional<Member> nicknameDuplicateCheck = memberRepository.findByNickname(member.getNickname());
        if (nicknameDuplicateCheck.isPresent()) {
            throw new CustomException(DUPLICATE_NICKNAME);
        }

        try {
            //????????? ?????? ??????
            EmailCheck emailCheck = emailCheckRepository.findById(member.getEmail()).get();
            //????????? ?????? ????????? false??? ??????
            if (!emailCheck.isStatus()) {
                throw new CustomException(EMAIL_CERTIFICATION_FAIL);
            }
            emailCheckRepository.delete(emailCheck);
        } catch (Exception e) {
            //????????? ?????? ????????? ????????? ??????
            throw new CustomException(EMAIL_CERTIFICATION_FAIL);
        }
        memberRepository.save(member);
    }

    @Transactional
    public void login(LoginRequestDto loginRequestDto, HttpServletResponse response, HttpServletRequest request) {
        String inputEmail = loginRequestDto.getEmail();
        String inputPassword = loginRequestDto.getPassword();

        //?????????????????? ip ????????????
        String ip = request.getHeader("X-Forwarded-For");
        String getIp = "?????? ????????? ?????? IP, X-Forwarded-For :" + ip;
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
            getIp = "?????? ????????? ?????? IP, Proxy-Client-IP :" + ip;
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
            getIp = "?????? ????????? ?????? IP, WL-Proxy-Client-IP :" + ip;
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
            getIp = "?????? ????????? ?????? IP, HTTP_CLIENT_IP :" + ip;
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            getIp = "?????? ????????? ?????? IP, HTTP_X_FORWARDED_FOR :" + ip;
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
            getIp = "?????? ????????? ?????? IP, X-Real-IP :" + ip;
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-RealIP");
            getIp = "?????? ????????? ?????? IP, X-RealIP :" + ip;
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("REMOTE_ADDR");
            getIp = "?????? ????????? ?????? IP, REMOTE_ADDR :" + ip;
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            getIp = "?????? ????????? ?????? IP :" + ip;
        }
        log.info(getIp);

        Member member = memberRepository.findByEmail(inputEmail).orElseThrow(
                () -> new CustomException(MEMBER_NOT_FOUND)
        );

        //???????????? ?????? ??????
        if (!passwordEncoder.matches(inputPassword, member.getPassword())) {
            throw new CustomException(INCORRECT_PASSWORD);
        }

        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, jwtUtil.createToken(member.getEmail()));
    }

    @Transactional(readOnly = true)
    public void checkNickname(String nickname, UserDetailsImpl userDetails) {
        //????????? ????????? ??????
        if (!Pattern.matches("^(?=.*[a-z0*9???-???])[a-z0-9???-???]{2,10}$", nickname)) {
            throw new CustomException(INVALID_NICKNAME_PATTERN);
        }

        //????????????
        if (userDetails != null) {
            //????????? ?????? ??????????????? ????????? ????????????
            if (!nickname.equals(userDetails.getMember().getNickname())) {
                if (memberRepository.findByNickname(nickname).isPresent()) {
                    throw new CustomException(DUPLICATE_NICKNAME);
                }
            }
        } else {
            if (memberRepository.findByNickname(nickname).isPresent()) {
                throw new CustomException(DUPLICATE_NICKNAME);
            }
        }
    }

    @Transactional(readOnly = true)
    public MemberResponseDto getMember(Long memberId, UserDetailsImpl userDetails) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new CustomException(MEMBER_NOT_FOUND));
        boolean myProfile = false;
        if (userDetails != null) {
            if (userDetails.getMember().getId().equals(member.getId())) {
                myProfile = true;
            }
        }
        List<Review> reviews = reviewRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId);
        List<ReviewResponseDto> reviewList = new ArrayList<>();
        for (Review r : reviews) {
            reviewList.add(reviewMapper.toResponse(r));
        }
        return memberMapper.toResponse(member, reviewList, myProfile);
    }

    @Transactional
    public void updateMember(Member member, UpdateMemberRequestDto updateMemberRequestDto, Long memberId) {
        //?????? ??????
        if (!member.getId().equals(memberId)) {
            throw new CustomException(AUTHORIZATION_UPDATE_FAIL);
        }
        //????????? ????????? ??????
        if (!Pattern.matches("^(?=.*[a-z0*9???-???])[a-z0-9???-???]{2,10}$", updateMemberRequestDto.getNickname())) {
            throw new CustomException(INVALID_NICKNAME_PATTERN);
        }
        //????????? ?????? ??????????????? ????????? ????????????
        if (!updateMemberRequestDto.getNickname().equals(member.getNickname())) {
            if (memberRepository.findByNickname(updateMemberRequestDto.getNickname()).isPresent()) {
                throw new CustomException(DUPLICATE_NICKNAME);
            }
        }
        Member updateMember = memberRepository.findById(memberId).orElseThrow(() -> new CustomException(MEMBER_NOT_FOUND));
        updateMember.update(
                updateMemberRequestDto.getNickname(),
                updateMemberRequestDto.getProfileImage(),
                updateMemberRequestDto.getAddress());
    }

    @Transactional(readOnly = true)
    public MemberResponseDto getMyProfile(Member member) {
        List<Review> reviews = reviewRepository.findAllByMemberIdOrderByCreatedAtDesc(member.getId());
        List<ReviewResponseDto> reviewList = new ArrayList<>();
        for (Review r : reviews) {
            reviewList.add(reviewMapper.toResponse(r));
        }
        return memberMapper.toResponse(member, reviewList, true);
    }
}
