package com.studyolle.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

//TODO 2021.01.18 35.관심 주체 도메인
//     1.

@Entity
@Getter @Setter
@NoArgsConstructor
public class AccountTag {

    @Id @GeneratedValue
    @Column(name = "account_tag_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    private Tag tag;

    public void setAccount(Account account) {
        this.account = account;
        account.addAccountTag(this);
    }

    public static AccountTag createAccountTag(Account account, Tag tag) {
        AccountTag accountTag = new AccountTag();
        accountTag.setAccount(account);
//        accountTag.getAccount().addAccountTag(accountTag);
        accountTag.setTag(tag);
        return accountTag;
    }
}
