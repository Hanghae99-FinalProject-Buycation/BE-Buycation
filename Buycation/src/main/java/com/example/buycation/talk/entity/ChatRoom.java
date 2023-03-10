package com.example.buycation.talk.entity;

import com.example.buycation.posting.entity.Posting;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class ChatRoom {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Posting posting;

    @OneToMany(mappedBy = "chatRoom", fetch = FetchType.LAZY)
    private List<Talk> talks = new ArrayList<>();

    public ChatRoom(Posting posting){
        this.posting = posting;
    }

}
