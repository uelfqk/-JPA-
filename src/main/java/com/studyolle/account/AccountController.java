package com.studyolle.account;

import com.studyolle.account.form.SignUpForm;
import com.studyolle.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequiredArgsConstructor
public class AccountController {

    private final SignUpFormValidator signUpFormValidator;
    private final AccountService accountService;
    private final AccountRepository accountRepository;

    //TODO 2021.01.08 - 6.회원 가입 폼 서브밋 검증
    //     InitBinder 를 이용하여 value 로 지정한 이름에 해당하는 객체를
    //     해당 객체를 받을때 303 검증 코드도 실행이 되고 Custom Validator 로 지정한 검증도 수행한다.
    @InitBinder("signUpForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(signUpFormValidator);
    }

    //TODO 2021.01.08 - 6.회원 가입 폼 서브밋 검증
    @GetMapping("/sign-up")
    public String newFrom(Model model) {
        model.addAttribute("signUpForm", new SignUpForm());
        return "account/sign-up";
    }

    //TODO 2021.01.08 - 7.회원 가입 폼 서브밋 처리
    @PostMapping("/sign-up")
    public String signUpSubmit(@Valid @ModelAttribute SignUpForm signUpForm, Errors errors) {
        //TODO JSR303 검증
        if (errors.hasErrors()) {
            return "account/sign-up";
        }

        Account account = accountService.processNewAccount(signUpForm);
        accountService.login(account);
        //TODO 회원 가입 처리
        return "redirect:/";
    }

    //TODO 2021.01.10 - 10.회원가입 인증 메일 확인
    //     이메일 인증처리에 사용 메소드
    //     GET http://localhost:8080/check-email-token?token=${token}&email=${email}
    //     웹브라우저에서 전달되는 url 처리
    //     @RequestParam - 생략 가능
    @GetMapping("/check-email-token")
    public String checkEmailToken(@RequestParam String token,
                                  @RequestParam String email,
                                  Model model) {
        Account account = accountRepository.findByEmail(email);
        String viewName = "account/checked-email";

        //TODO 이메일로 조회하기때문에 해당 엔티티가 null 이면 이메일이 잘못되었다는 의미
        if(account == null) {
            model.addAttribute("error", "wrong.email");
            return viewName;
        }

        //TODO 이메일을 발송할때 발행한 토큰과 브라우저에서 전송한 토큰이 다르면
        //     토큰이 잘못되었다는 의미
        if(!account.isValidEmailToken(token)) {
            model.addAttribute("error", "wrong.token");
            return viewName;
        }

        accountService.completeSignUp(account);
        //TODO View 에 출력할 내용을 model 에 담아 전달
        //     이메일을 확인했습니다. *{n} 번째 회원, *{nickname} 님 가입을 축하합니다.
        model.addAttribute("numberOfUser", accountRepository.count());
        model.addAttribute("nickname", account.getNickname());
        return viewName;
    }

    //TODO 2021.01.13 16.가입확인 이메일 재전송
    //     1. 인증하지 않은 경우 계정 인증 이메일을 확인하세요. 클릭시
    //     2. 인증된 사용자 정보를 참조하여 화면 변경
    @GetMapping("/check-email")
    public String checkEmail(@CurrentUser Account account, Model model) {
        model.addAttribute("email", account.getEmail());
        return "account/check-email";
    }

    //TODO 2021.01.13 16.가입확인 이메일 재전송
    //     1. 인증 이메일 재전송은 현재시간에서 1시간 뺀 시간과 이메일 전송 토큰을 생성한 시간과 비교하여
    //     2. 1시간마다 재전송 하는 이유
    //      1). 의도적으로 이메일을 계속 전송하여 서비스를 망가트릴 가능성이 있기때문에 이와같은 제약조건 설정
    //     3. 결과가 true 이면 이메일 재전송 - 리다렉트 사용 ( 새로고침으로 인해 이메일이 계속 전송될 수 있기때문에 )
    //              false 이면 error 정보를 담아 화면 전환
    @GetMapping("/resend-confirm-email")
    public String reSendEmail(@CurrentUser Account account, Model model) {
        if(!account.canSendConfirmEmail()) {
            model.addAttribute("error", "인증 이메일은 1시간에 한번만 전송할 수 있습니다.");
            model.addAttribute("email", account.getEmail());
            return "account/check-email";
        }

        accountService.sendSignUpConfirmEmail(account);
        return "redirect:/";
    }

    //TODO 2021.01.15 22.프로필 뷰
    //     1. 프로필 뷰에 접근하는 핸들러
    //     2. 프로필 정보가 있을때와 없을때 보여줄 메시지가 다름
    //     3. 현재 유자가 프로필을 수정할 수 있는 권한이 있는지 판단 필요
    @GetMapping("/profile/{nickname}")
    public String viewProfile(@PathVariable String nickname, Model model,
                              @CurrentUser Account account) {
        //TODO 1. 현재 인증된 정보 principal ( @CurrentUser Account account ) 을 이용해
        //        Url path 의 nickname 으로 해당 유저 조회
        //     2. 해당 유저가 존재하지 않으면 예외에 메시지를 담아서 반환
        //     3. 해당 유저가 존재하면 뷰에서 사용할 유저 정보, 해당 유저가 이 계정의 주인이 맞는지에 대한 정보를
        //        model 에 담아 반환
        Account findAccount = accountRepository.findByNickname(nickname);
        if (findAccount == null) {
            new IllegalArgumentException(nickname + "에 해당하는 사용자가 없습니다.");
        }
        model.addAttribute("account", findAccount);
        model.addAttribute("isOwner", findAccount.equals(account));

        return "account/profile";
    }

    //TODO 2021.01.18 33.패스워드를 잊어버렸습니다.
    //     1. 이메일 로그인 폼 요청 핸들러
    @GetMapping("/email-login")
    public String withOutPasswordEmailLoginView() {
        return "account/email-login";
    }

    //TODO 2021.01.18 33.패스워드를 잊어버렸습니다.
    //     1. 폼에서 입력된 이메일을 받아 이메일을 다시 전송하는 핸들러
    @PostMapping("/email-login")
    public String sendEmailLoginLink(@RequestParam String email, Model model,
                                     RedirectAttributes attributes) {
        Account findAccount = accountRepository.findByEmail(email);

        //TODO 2021.01.18 33.패스워드를 잊어버렸습니다.
        //     1. 이메일로 유저정보를 조회한 결과가 없으면 유효한 이메일이 아니라는 의미
        if(findAccount == null) {
            model.addAttribute("error", "유효한 이메일 주소가 아닙니다.");
            return "account/email-login";
        }

        //TODO 2021.01.18 33.패스워드를 잊어버렸습니다.
        //     1. 이메일을 너무 자주 보내게되면 이슈가 발생할 가능성이 있음으로 1시간에 한번만
        //        이메일 전송이 가능하도록 처리
        if(!findAccount.canSendConfirmEmail()) {
            model.addAttribute("error", "이메일 로그인은 1시간 뒤에 사용할 수 있습니다.");
            return "account/email-login";
        }

        accountService.sendLoginLink(findAccount);
        attributes.addFlashAttribute("message", "인증 이메일을 발송하였습니다.");
        return "redirect:/email-login";
    }

    //TODO 2021.01.18 33.패스워드를 잊어버렸습니다.
    //     1. 이메일로 로그인한 유저에게 패스워드를 변경하라는 폼 요청을 처리하는 핸들러
    @GetMapping("/login-by-email")
    public String loginByEmail(@RequestParam String token,
                               @RequestParam String email, Model model) {
        Account findAccount = accountRepository.findByEmail(email);

        //TODO 2021.01.18 33.패스워드를 잊어버렸습니다.
        //     1. 이메일로 조회한 유저가 없거나 이메일 토큰이 유효하지 않은 경우 로그인 실패처리
        if(findAccount == null || !findAccount.isValidEmailToken(token)) {
            model.addAttribute("error", "로그인 할 수 없습니다.");
            return "account/logged-in-by-email";
        }

        //TODO 2021.01.18 33.패스워드를 잊어버렸습니다.
        //     1. 유저 로그인 처리
        accountService.login(findAccount);
        return "account/logged-in-by-email";
    }
}
