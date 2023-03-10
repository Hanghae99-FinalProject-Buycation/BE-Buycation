package com.example.buycation.posting.dto;

import com.example.buycation.comment.dto.CommentResponseDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PostingResponseDto {
    private Long memberId;
    private String nickname;
    private String profileImage;
    private Long postingId;
    private String title;
    private String address;
    private String addressDetail;
    private String content;
    private String image;
    private String dueDate;
    private long budget;
    private long perBudget;
    private int totalMembers;
    private int currentMembers;
    private String createdAt;
    private String category;
    private List<CommentResponseDto> commentList;
    private boolean doneStatus;
    private boolean myPosting;
    private boolean participant;
    private double coordsX;
    private double coordsY;
}
