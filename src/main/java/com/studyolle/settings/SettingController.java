package com.studyolle.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyolle.account.AccountService;
import com.studyolle.account.CurrentUser;
import com.studyolle.domain.Account;
import com.studyolle.domain.Tag;
import com.studyolle.domain.Zone;
import com.studyolle.tag.TagRepository;
import com.studyolle.zone.ZoneRepository;
import com.studyolle.settings.form.*;
import com.studyolle.settings.validator.NicknameFormValidator;
import com.studyolle.settings.validator.PasswordFormValidator;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class SettingController {

    private final AccountService accountService;
    private final ModelMapper modelMapper;
    private final NicknameFormValidator nicknameFormValidator;
    private final ObjectMapper objectMapper;
    private final TagRepository tagRepository;
    private final ZoneRepository zoneRepository;

    //TODO 2021.01.17 28. 패스워드 수정
    //     1. 비밀번호 검증 Validator 를 WebDataBinder 에 등록
    //     2. 스프링 빈으로 등록하지 않았음으로 new operation 을 이용해 등록
    @InitBinder("passwordForm")
    public void initPasswordFormBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(new PasswordFormValidator());
    }

    @InitBinder("nicknameForm")
    public void initNicknameFormBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(nicknameFormValidator);
    }

    //TODO 2021.01.16 25.프로필 수정
    //     1. 프로필 수정화면으로 이동 요청을 처리하는 핸들러
    //     2. 인증된 유저의 정보 @CurrentUser Account account 와
    //        뷰에서 사용할 Form 데이터에 유저정보를 저장하여 전달
    @GetMapping("/settings/profile")
    public String profileUpdateForm(@CurrentUser Account account, Model model) {
        model.addAttribute("account", account);
        model.addAttribute("profile", modelMapper.map(account, Profile.class));
        return "settings/profile";
    }

    //TODO 2021.01.16 25.프로필 수정
    //     1. 프로필 정보 수정 요청을 처리하는 핸들러
    //     2. 폼에서 넘어오는 데이터를 validation 할때는 반드시
    //        @Valid  @ModelAttribute Profile profile, Errors errors 순서로 매개변수 정의
    //     3. 수정된 정보를 데이터베이스에 업데이트하는 행위는 AccountService 에 위임
    //     4. POST 요청은 반드시 리다이랙트 처리
    @PostMapping("/settings/profile")
    public String updateProfile(@Valid @ModelAttribute Profile profile, Errors errors,
                                @CurrentUser Account account, Model model,
                                RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            model.addAttribute("account", account);
            return "settings/profile";
        }

        accountService.updateProfile(account, profile);
        //TODO 2021.01.16 25.프로필 수정
        //     1. 리다이랙트 시킬때 한번쓰고 더이상 사용하지 않는 데이터 - Model 에 자동으로 들어가게된다.
        //      1). attributes.addFlashAttribute("message", "프로필을 수정하였습니다.");
        attributes.addFlashAttribute("message", "프로필을 수정하였습니다.");
        return "redirect:/" + "settings/profile";
    }

    @GetMapping("/settings/password")
    public String updatePasswordForm(@CurrentUser Account account, Model model) {
        model.addAttribute("account", account);
        model.addAttribute("passwordForm", new PasswordForm());
        return "settings/password";
    }

    //TODO 2021.01.17 28. 패스워드 수정
    @PostMapping("/settings/password")
    public String updatePassword(@Valid @ModelAttribute PasswordForm passwordForm, Errors errors,
                                 @CurrentUser Account account, Model model, RedirectAttributes attributes) {
        if(errors.hasErrors()) {
            model.addAttribute("account", account);
            return "settings/password";
        }

        accountService.updatePassword(account, passwordForm.getNewPassword());
        attributes.addFlashAttribute("message", "비밀번호를 변경했습니다.");
        return "redirect:/" + "settings/password";
    }

    //TODO 2021.01.17 28. 패스워드 수정
    //     1. 강의 외 개별 검증로직 구현
    @PostMapping("/settings/password/mylogic")
    public String updatePasswordMyLogic(@Valid @ModelAttribute PasswordForm passwordForm, Errors errors,
                                 @CurrentUser Account account, Model model, RedirectAttributes attributes) {
        if(errors.hasErrors()) {
            model.addAttribute("account", account);
            return "settings/password";
        }
        //TODO 2021.01.17 28. 패스워드 수정
        //     1. 입력한 새 비밀번호가 일치하지 않음을 검증하는 로직을 PasswordForm 객체에 위임하여 코드 작성
        //     2. 해당 검증 로직은 Validator 로 구현하는 것이 더 좋은 코드로 판단
        if(!passwordForm.isEqualsPassword()) {
            model.addAttribute("account", account);
            model.addAttribute("message", "비밀번호가 일치하지 않습니다.");
            return "settings/password";
        }

        //TODO 2021.01.17 28. 패스워드 수정
        //     1. 현재 사용중인 비밀번호와 새로운 비밀번호가 동일하면 비밀번호 변경이 되지 않도록 구현
        //        AccountService Layer 에는 현재 비밀번호와 새로운 비밀번호에 대한 데이터를 모두 받을 수 있음으로
        //        해당 서비스에 위임하여 처리하는것이 옳다고 판단
        //     2. 해당 검증로직을 Validator 로 구현하고자하면 현재 유저의 패스워드를 얻어와야하는데 구현방법 생각 중
        if(!accountService.updatePasswordMyLogic(account, passwordForm.getNewPassword())) {
            model.addAttribute("account", account);
            model.addAttribute("message", "기존 비밀번호와 동일하게 변경할 수 없습니다.");
            return "settings/password";
        }

        attributes.addFlashAttribute("message", "비밀번호를 변경했습니다.");
        return "redirect:/" + "settings/password";
    }

    //TODO 2021.01.17 30.알림 설정
    @GetMapping("/settings/notifications")
    public String updateNotificationsForm(@CurrentUser Account account, Model model) {
        model.addAttribute("account", account);

        //TODO 2021.01.17 31.ModelMapper 적용
        //     1. Notifications 는 스프링 빈이 아니기 때문에 의존성을 주입 받을 수 없다.
        //     2. Notifications 생성을 더이상 해당 객체에서 하지 않고 Controller Layer 로 가져와
        //        ModelMapper 를 주입받은 후 map 메소드 사용
        //     3. 사용 방법
        //      1). Notifications notifications = modelMapper.map(account, Notifications.class);
        //     4. 동작 방식
        //      1). Destination 에 해당하는 객체를 생성하고 해당 프로퍼티를 Source 의 값으로 복사
        //      2). Source : account, Destination : Notifications 의 프로퍼티  
        model.addAttribute("notifications", modelMapper.map(account, Notifications.class));
        return "settings/notifications";
    }

    //TODO 2021.01.17 30.알림 설정
    @PostMapping("/settings/notifications")
    public String updateNotifications(@Valid @ModelAttribute Notifications notifications, Errors errors,
                                      @CurrentUser Account account, Model model, RedirectAttributes attributes) {
        if(errors.hasErrors()) {
            model.addAttribute("account", account);
            return "settings/notifications";
        }

        accountService.updateNotifications(account, notifications);
        attributes.addFlashAttribute("message", "알림 설정이 수정되었습니다.");
        return "redirect:/" + "settings/notifications";
    }

    //TODO 2021.01.17 32.닉네임 수정
    //     1. 닉네임 수정 폼 요청 처리 핸들러
    @GetMapping("/settings/account")
    public String updateNicknameForm(@CurrentUser Account account, Model model) {
        model.addAttribute("account", account);
        model.addAttribute("nicknameForm", modelMapper.map(account, NicknameForm.class));
        return "settings/account";
    }

    //TODO 2021.01.17 32.닉네임 수정
    //     1. 닉네임 수정 요청 처리 핸들러
    @PostMapping("/settings/account")
    public String updateNickname(@Valid @ModelAttribute NicknameForm nicknameForm, Errors errors,
                                        @CurrentUser Account account, Model model, RedirectAttributes attributes) {
        if(errors.hasErrors()) {
            model.addAttribute("account", account);
            return "settings/account";
        }

        accountService.updateNickname(account, nicknameForm.getNickname());
        attributes.addFlashAttribute("message", "닉네임이 수정되었습니다.");
        return "redirect:/" + "settings/account";
    }

    //TODO 2021.01.19 36.관심 주제 등록 뷰
    //     1. 태그 입력 폼 요청 처리 핸들러
    //     2. 기존에 등록한 태그를 유저에게 보여줄 수 있도록 유저가 등록한 태그 조회 후 
    //        태그의 이름을 가지고 리스트로 변환환 후 model 에 담아 함께 전달
    @GetMapping("/settings/tags")
    public String updateTags(@CurrentUser Account account, Model model) throws JsonProcessingException {
        model.addAttribute("account", account);


        //TODO 2021.01.20 37.관심 주제 조회

        //TODO 2021.01.19 36.관심 주제 등록 뷰
        //     1. Tag 의 Title 을 이용해 리스트로 변환
        //     2. stream Api (map, collect(Collectors.toList()) 사용
        List<String> tags = accountService.getTags(account);
        System.out.println("=======================================================");

        model.addAttribute("tags", tags);

        //TODO 2021.01.20 39. 관심주제 자동완성
        List<String> allTags = tagRepository.findAll().stream()
                .map(t -> t.getTitle())
                .collect(Collectors.toList());

        //TODO 2021.01.20 39. 관심주제 자동완성
        model.addAttribute("whitelist", objectMapper.writeValueAsString(allTags));
        return "settings/tags";
    }

    //TODO 2021.01.19 36.관심 주제 등록 뷰
    //     1. 폼에서 Ajax 로 전달된 태그를 조회 후 조회된 태그가 없는 경우 데이터베이스에 저장 - 서비스로 위임
    @PostMapping("/settings/tags/add")
    @ResponseBody
    public ResponseEntity addTag(@CurrentUser Account account, @RequestBody TagForm tagForm) {
        String title = tagForm.getTagTitle();

        Tag tag = tagRepository.findByTitle(title);

        if(tag == null) {
            tag = tagRepository.save(Tag.createTag(title));
        }

        //TODO 2021.01.19 36.관심 주제 등록 뷰
        //     1. 폼에서 Ajax 로 전달된 태그로 조회
        accountService.addTag(account, tag);

        return ResponseEntity.ok().build();
    }

    //TODO 2021.01.19 38.관심 주제 삭제
    //     1. 폼에서 Ajax 로 전달된 태그를 조회 후 조회된 태그가 없는 경우 BadRequest
    //     2. 조회 결과가 있는 경우 해당 태그 삭제 - 서비스로 위임
    @PostMapping("/settings/tags/remove")
    @ResponseBody
    public ResponseEntity removeTag(@CurrentUser Account account, @RequestBody TagForm tagForm) {
        String title = tagForm.getTagTitle();
        //AccountTag accountTag = accountTagRepository.findTag(account.getId(), tagForm.getTagTitle());

        Tag tag = tagRepository.findByTitle(title);
        if(tag == null) {
            return ResponseEntity.badRequest().build();
        }

        //TODO 2021.01.19 38.관심 주제 삭제
        //     1. 폼에서 Ajax 로 전달된 태그로 조회
        accountService.removeTag(account, tag);

        return ResponseEntity.ok().build();
    }

    //TODO 2021.01.24 43.지역 정보 추가,삭제 / 테스트
    //     1. 지역정보 폼 요청 처리 핸들러
    //     2. 유저가 등록한 지역정보를 조회하여 폼으로 전달
    //      1). List<String> 형식 - Java8 의 stream Api 사용 stream().map().collect()
    //     3. ZoneService 의 @PostConstruct 에서 등록한 지역정보를 화이트 리스트로 json 형태로 변환하여 폼에 전달
    //      1). List<String> 형식 - Java8 의 stream Api 사용 stream().map().collect()
    @GetMapping("/settings/zones")
    public String updateZonesForm(@CurrentUser Account account, Model model) throws JsonProcessingException {
        model.addAttribute("account", account);

        List<String> zoneList = accountService.getZones(account);

        List<String> whiteList = zoneRepository.findAll().stream()
                .map(z -> z.toString())
                .collect(Collectors.toList());

        model.addAttribute("zones", zoneList);
        model.addAttribute("whitelist", objectMapper.writeValueAsString(whiteList));

        return "settings/zones";
    }

    //TODO 2021.01.24 43.지역 정보 추가,삭제 / 테스트
    //     1. 계정에 지역정보 추가
    //      1). Account(add 요청을 한 유저)
    //      2). AccountZone (유저와 지역정보의 연결점) - Account 객체에 AccountZone 을 추가
    //       -. AccountZone 객체에는 Account 의 PK 와 Zone 의 PK FK 로 매핑
    //      3). Zone (생성된 데이터)
    //     2. 폼에서 전달받은 본문의 json 데이터를 파싱하고 city 데이터와 province 데이터를 이용해
    //        지역정보 데이터베이스에 해당 데이터가 있는지 조회 및 확인
    //     3. 조회 정보가 없다면 잘못된 요청으로 badRequest 반환
    //     4. 조회 결과를 가지고 AccountZone insert 수행
    @PostMapping("/settings/zones/add")
    @ResponseBody
    public ResponseEntity addAccountZone(@CurrentUser Account account, @RequestBody ZoneForm zoneForm) {
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCity(), zoneForm.getProvince());

        if (zone == null) {
            return ResponseEntity.badRequest().build();
        }

        accountService.addAccountZone(account, zone);
        return ResponseEntity.ok().build();
    }


    //TODO 2021.01.24 43.지역 정보 추가,삭제 / 테스트
    //     1. 계정에 지역정보 삭제
    //      1). Account(remove 요청을 한 유저)
    //      2). AccountZone (유저와 지역정보의 연결점) - Account 객체에 AccountZone 을 삭제
    //      3). Zone (생성된 데이터)
    //     2. 폼에서 전달받은 본문의 json 데이터를 파싱하고 city 데이터와 province 데이터를 이용해
    //        지역정보 데이터베이스에 해당 데이터가 있는지 조회 및 확인
    //     3. 조회 정보가 없다면 잘못된 요청으로 badRequest 반환
    //     4. 조회 결과를 가지고 AccountZone remove 수행 - Zone 객체의 데이터를 삭제하는것이 아님
    @PostMapping("/settings/zones/remove")
    @ResponseBody
    public ResponseEntity removeAccountZone(@CurrentUser Account account, @RequestBody ZoneForm zoneForm) {
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCity(), zoneForm.getProvince());

        if(zone == null) {
            ResponseEntity.badRequest().build();
        }

        accountService.removeAccountZone(account, zone);
        return ResponseEntity.ok().build();
    }
}
