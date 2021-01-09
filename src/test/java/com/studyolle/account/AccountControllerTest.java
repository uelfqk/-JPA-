package com.studyolle.account;

import com.studyolle.domain.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//TODO 컨트롤러부터 아래 모든 클래스 테스트

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    AccountRepository accountRepository;

    //TODO 이메일 전송 테스트에 사용 - Mock 객체 사용
    @MockBean
    JavaMailSender javaMailSender;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원 가입 화면 보이는지 테스트")
    void signUpFormTest() throws Exception {
        mockMvc.perform(get("/sign-up"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"));

    //TODO    내 코드 - 테스트 실패 : content() 메서드는 html 본문을 가져오는 메서드임으로 view 의 이름이 "account/sign-up" 인지
    //                검증하는데 부적학함 - view().name("account/sign-up") 으로 수정하여 테스트 완성
    //                .andExpect(content().string("account/sign-up"));
    }

    @Test
    @DisplayName("회원 가입 화면에서 모델이 전달됬는지 테스트")
    void signUpFormV2Test() throws Exception {
        mockMvc.perform(get("/sign-up"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"))
                .andExpect(model().attributeExists("signUpForm"));
    }

    //TODO 2021.01.09 - 8.회원가입 리팩토링 및 테스트
    //     status() 가 403 을 반환
    //     이유 : 스프링 시큐리티가 authorizeRequests 만 설정해놓은 상태
    //           CSRF (Cross-site Request Forgery) - 타 사이트에서 공격하는 사이트를 대상으로 폼 데이터를 보내는 것이다.
    //           예 - 은행 계좌 이체 데이터를 은행 사이트가 아니라 타 사이트에서 보내는 것
    //     방지 : 이런것을 방지하기 위해서 스프링 시큐리티가 CSRF 토큰이라는것을 자동으로 사용
    //            Thymeleaf template 로 만들면 타임리프 스프링 시큐리티, 스프링 MVC 가 조합이 되어 CSRF 기능을 지원해준다.
    //     사용 : 웹 브라우저에서 서버로 보내는 데이터에 CSRF 토큰을 hidden 으로 담아서 같이 전송
    //           이 토큰 값을 보고 내가 만들어준 폼에서 만들어준 데이터이니 받아도 된다.
    //     오류 : 이 토큰값이 다르거나 없는 경우 안전하지 않은 요청으로 받아들여 http status 403 을 반환한다.
    //     적용 : 보내는 요청에 " with(csrf()) " 포함하여 전송 > 테스트 코드일때
    @Test
    @DisplayName("회원 가입 처리 - 입력값 오류")
    void signUpSubmit_with_wrong_input_Test() throws Exception {
        mockMvc.perform(post("/sign-up")
            .param("nickname", "youngbin")
            .param("email", "email..")
            .param("password", "12345")
            .with(csrf())) //TODO 적용 : 보내는 요청에 " with(csrf()) " 포함하여 전송 > 테스트 코드일때
            .andExpect(status().isOk())
            .andExpect(view().name("account/sign-up"));

    }

    //TODO 2021.01.09 - 8.회원가입 리팩토링 및 테스트
    @Test
    @DisplayName("회원 가입 처리 - 입력값 정상")
    void signUpSubmit_with_wrong_correct_test() throws Exception {
        MvcResult result = mockMvc.perform(post("/sign-up")
                .param("nickname", "youngbin")
                .param("email", "yb@email.com")
                .param("password", "12345678")
                .with(csrf()))
                .andExpect(status().is3xxRedirection()) //TODO 상태 값이 리다이렉트
                .andExpect(view().name("redirect:/")) //TODO 반환되는 URL
                .andReturn();

        //TODO 요청에 매개변수 얻기 - nickname
        String nickname = result.getRequest().getParameter("nickname");
        //TODO 요청에 매개변수 얻기 - email
        String email = result.getRequest().getParameter("email");

        //TODO 얻어온 매개변수 출력
        System.out.println("nickname = " + nickname);
        System.out.println("email = " + email);

        assertThat(accountRepository.existsByNickname(nickname)).isTrue();
        assertThat(accountRepository.existsByEmail(email)).isTrue();

        //TODO 이메일 전송 테스트
        //     메일의 내용까지 확인하는것은 너무 깊게 들어가는것이며
        //     추후에 메일의 내용이 변경될수 있다.
        // -----------------------------------------------------------------
        //     아무런 객체를 넣고 메일이 전송됬는지 확인
        //     JavaMailSender 는 개발자가 인터페이스만 관리하고 외부의 서비스를 이용
        //     그렇기 때문에 Mock 객체를 이용해 테스트
       then(javaMailSender).should().send(any(SimpleMailMessage.class));
    }

    //TODO 2021.01.10 - 9.회원가입 패스워드 인코딩
    //     bcrypt 알고리즘을 이용하여 입력된 평문을 해싱한 결과 테스트
    @Test
    @DisplayName("회원가입 - 비밀번호 인코딩 테스트")
    void password_encode_test() throws Exception {
        mockMvc.perform(post("/sign-up")
                .param("nickname", "youngbin")
                .param("email", "yb@email.com")
                .param("password", "12345678")
                .with(csrf()))
                .andExpect(status().is3xxRedirection()) //TODO 상태 값이 리다이렉트
                .andExpect(view().name("redirect:/")); //TODO 반환되는 URL

        Account findAccount = accountRepository.findByEmail("yb@email.com");

        assertThat(findAccount).isNotNull();
        assertThat(findAccount.getPassword()).isNotEqualTo("12345678");
    }

    //TODO 2021.01.10 - 9.회원가입 패스워드 인코딩
    //     입력받은 평문을 해싱하여 데이터베이스에 저장한 해시와 입력받은 평문 과 데이베이스의 해시값을 이용해
    //     일치하는지 확인
    @Test
    @DisplayName("회원가입 - 비밀번호 인코딩 일치 테스트")
    void password_encode_match_test() throws Exception {
        mockMvc.perform(post("/sign-up")
                .param("nickname", "youngbin")
                .param("email", "yb@email.com")
                .param("password", "12345678")
                .with(csrf()))
                .andExpect(status().is3xxRedirection()) //TODO 상태 값이 리다이렉트
                .andExpect(view().name("redirect:/")); //TODO 반환되는 URL

        Account findAccount = accountRepository.findByEmail("yb@email.com");

        boolean mathPassword = passwordEncoder.matches( "12345678", findAccount.getPassword());

        assertThat(findAccount).isNotNull();
        assertThat(mathPassword).isTrue();
    }
}