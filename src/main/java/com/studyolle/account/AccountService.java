package com.studyolle.account;

import com.studyolle.account.dto.SignUpForm;
import com.studyolle.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//TODO 2021.01.10 - 이메일 전송에 필요한 토큰 발행 후 데이터베이스에 저장되지 않는 이슈
//     processNewAccount(SignUpForm signUpForm) 메소드를 실행할때는 이미 트랜젝션이 종료된 상태
//     따라서 newAccount.generateEmailCheckToken() 는 준영속 상태의 객체에 데이터를 변경한것이다.
//     해당 메소드에 @Transactional 애노테이션을 추가하여 영속상태를 유지시켜주면 변경 내용이 반영되어
//     데이터베이스에 업데이트가 발생하게 된다.
//     테스트 코드를 작성하지 않아 확인되지 않은 문제

@Service
@RequiredArgsConstructor
public class AccountService {
    //TODO 2021.01.09 - 8.회원가입 리팩토링 및 테스트
    //     기존 Controller 에서 사용하는 의존성을 Service Layer 로 이동
    //     Controller 의 의존성을 AccountService 만을 받게 변경

    private final AccountRepository accountRepository;
    private final JavaMailSender javaMailSender;
    private final PasswordEncoder passwordEncoder;

    //private final AuthenticationManager authenticationManager;

    //TODO 2021.01.09 - 8.회원가입 리팩토링 및 테스트
    //     Controller Layer 에서 사용할 메소드만을 public 접근제어자로 공개
    //
    //TODO 2021.01.11 12.회원가입 가입 완료 후 자동 로그인
    //     자동 로그인 기능을 구현하기 위해 반환 타입을 Account 객체로 변경
    //     AccountController 에서 회원가입 로직을 처리한 후 Account 객체를 반환받아
    //     login(Account account) [자동 로그인] 로직 실행
    @Transactional
    public Account processNewAccount(SignUpForm signUpForm) {
        Account newAccount = saveNewAccount(signUpForm);
        //TODO 이메일 전송에 필요한 토큰 발행
        newAccount.generateEmailCheckToken();
        sendSignUpConfirmEmail(newAccount);
        return newAccount;
    }

    //TODO 2021.01.09 - 8.회원가입 리팩토링 및 테스트
    //     리팩토링 : 회원가입을 담당하는 기능을 추출하여 메소드로 분리
    //     Controller Layer 에서 알고있지 않아도 됨으로 private 접근제 어자로 비공개
    private Account saveNewAccount(SignUpForm signUpForm) {
        Account account = Account.createAccount(signUpForm.getNickname(),
                signUpForm.getEmail(), passwordEncoder.encode(signUpForm.getPassword()));

        //TODO 뷰에서 받은 데이터 저장
        Account newAccount = accountRepository.save(account);
        return newAccount;
    }

    //TODO 2021.01.09 - 8.회원가입 리팩토링 및 테스트
    //     리팩토링 : 이메일 전송을 담당하는 기능을 추출하여 메소드로 분리
    //     Controller Layer 에서 알고있지 않아도 됨으로 private 접근제어자로 비공개
    private void sendSignUpConfirmEmail(Account newAccount) {
        //TODO 이메일 전송
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(newAccount.getEmail()); //TODO 이메일을 받을 사람
        mailMessage.setSubject("스터디올래, 회원 가입 인증");
        mailMessage.setText("/check-email-token?token=" + newAccount.getEmailCheckToken() +
                "&email=" + newAccount.getEmail());

        javaMailSender.send(mailMessage);
    }

    //TODO 2021.01.11 12.회원가입 가입 완료 후 자동 로그인
    //     자동 로그인 비즈니스 로직
    public void login(Account account) {
        //TODO 2021.01.11 12.회원가입 가입 완료 후 자동 로그인
        //     본래는 AuthenticationManager 가 하는 일을 비즈니스 로직에서 구현
        //     결과는 동일
        //     이와 같이 사용하는 이유는 아래 정석적으로 인증하는 방법에 명시
        List<SimpleGrantedAuthority> authorities = new ArrayList<>(Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                account.getNickname(),
                account.getPassword(),
                authorities);
        SecurityContextHolder.getContext().setAuthentication(token);

        //TODO 2021.01.11 12.회원가입 가입 완료 후 자동 로그인
        //     정석적으로 인증하는 방법
        //     AuthenticationManager 사용
        //     폼에서 받은 데이터를 넣어 인증된 객체를 토큰에 넣어줘야된다.
        //     정석적으로 인증하는 방법을 사용하지 못하는 이유 :
        //          password 를 평문으로 입력해야하는데 데이터베이스에 password 를 평문으로 저장하지 않는다.
        //          웹 브라우저에서 password 평문을 받는 경우도 있지만 그렇지 않는 경우도 있기 때문에 사용 불가능
//        UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken(
//                username, password);
//        Authentication authenticate = authenticationManager.authenticate(token);
//        SecurityContext context = SecurityContextHolder.getContext();
//        context.setAuthentication(authenticate);

    }
}
