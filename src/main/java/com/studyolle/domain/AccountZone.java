package com.studyolle.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountZone {

    @Id @GeneratedValue
    @Column(name = "account_zone_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    public static AccountZone createAccountZone(Account account, Zone zone) {
        AccountZone accountZone = new AccountZone();
        accountZone.setAccount(account);
        accountZone.setZone(zone);
        return accountZone;
    }
}
