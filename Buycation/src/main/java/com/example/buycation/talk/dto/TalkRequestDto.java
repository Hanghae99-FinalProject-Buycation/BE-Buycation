package com.example.buycation.talk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TalkRequestDto {

    private Long roomId;
    private Long memberId;
    private String sender;
    private String message;

}